import torch
import torch.nn as nn
import torch.nn.functional as F
import numpy as np
import pandas as pd
import pickle
import os
from sklearn.metrics import roc_auc_score
from sklearn.model_selection import train_test_split
from config import Config
from data_utils import build_graph, build_sequences
from models.sgc_rec import SGC_Rec, HGTEncoder
from models.propensity_model import PropensityModel

# ==================== 加速设置 ====================
torch.backends.cuda.matmul.allow_tf32 = True
torch.backends.cudnn.allow_tf32 = True

cfg = Config()
# 关闭所有辅助损失，仅保留简单 BCE 以稳定训练
cfg.LAMBDA_SSL = 0.0
cfg.LAMBDA_NEG = 0.0

VALIDATION_INTERVAL = 10
USE_COMPILE = False

# ==================== 1. 构建图与编码器 ====================
data, (user_encoder, post_encoder, tag_encoder, pos_encoder, comment_encoder) = build_graph()
data = data.to(cfg.DEVICE)

# ==================== 调试：打印节点数量和边索引范围 ====================
print("\n=== 节点数量 ===")
for nt in data.node_types:
    print(f"{nt}: {data[nt].x.size(0)}")
print("\n=== 边索引范围 ===")
for (src, rel, dst), edge_index in data.edge_index_dict.items():
    if edge_index.numel() > 0:
        print(f"{src}->{rel}->{dst}: {edge_index.size(1)} edges, min={edge_index.min().item()}, max={edge_index.max().item()}")
    else:
        print(f"{src}->{rel}->{dst}: 0 edges (empty)")

# ==================== 2. 加载交互数据与序列 ====================
df_inter = pd.read_csv(cfg.INTERACTIONS_CSV)
seq_dict = build_sequences(df_inter, user_encoder, post_encoder, max_len=cfg.SEQ_LEN)

# ==================== 3. 加载行为映射 ====================
with open(cfg.ACTION_MAP_PKL, "rb") as f:
    action_map = pickle.load(f)

# ==================== 4. 构建训练样本（分层抽样）====================
exposure_edges = data["user", "interact", "post"].edge_index
num_edges = exposure_edges.size(1)

# 生成标签
all_labels = []
for i in range(num_edges):
    u = exposure_edges[0, i].item()
    p = exposure_edges[1, i].item()
    user_str = user_encoder.inverse_transform([u])[0]
    post_id_val = post_encoder.inverse_transform([p])[0]
    action = action_map.get((user_str, post_id_val), "view")
    label = 1 if action == "like" else 0
    all_labels.append(label)
all_labels = torch.tensor(all_labels, dtype=torch.float, device=cfg.DEVICE).unsqueeze(1)

pos_tensor = data["user", "interact", "post"].pos

# 检查正样本数量
num_pos = (all_labels == 1).sum().item()
print(f"正样本数量: {num_pos} / {num_edges}")
if num_pos == 0:
    raise ValueError("数据中没有正样本（like），模型无法学习。请确保交互数据中包含点赞记录。")

# 分层划分训练/验证集
labels_np = all_labels.cpu().numpy().flatten()
train_idx, val_idx = train_test_split(
    np.arange(num_edges),
    test_size=cfg.VAL_RATIO,
    stratify=labels_np,
    random_state=42
)
train_idx = torch.tensor(train_idx, device=cfg.DEVICE)
val_idx = torch.tensor(val_idx, device=cfg.DEVICE)

train_user_ids = exposure_edges[0, train_idx]
train_post_ids = exposure_edges[1, train_idx]
train_labels = all_labels[train_idx]
train_pos = pos_tensor[train_idx]

val_user_ids = exposure_edges[0, val_idx]
val_post_ids = exposure_edges[1, val_idx]
val_labels = all_labels[val_idx]
val_pos = pos_tensor[val_idx]

# ==================== 5. 构建序列数据 ====================
num_posts_total = len(post_encoder.classes_)
padding_idx = num_posts_total

def get_seq(user_id):
    user_str = user_encoder.inverse_transform([user_id])[0]
    seq = seq_dict.get(user_str, [])
    if len(seq) < cfg.SEQ_LEN:
        seq = [padding_idx] * (cfg.SEQ_LEN - len(seq)) + seq
    else:
        seq = seq[-cfg.SEQ_LEN:]
    return seq

def make_seq_tensor(seq_list):
    seq_tensor = torch.tensor(seq_list, dtype=torch.long, device=cfg.DEVICE)
    mask = (seq_tensor != padding_idx)
    return seq_tensor, mask

train_seq_raw = [get_seq(u.item()) for u in train_user_ids]
train_seq, train_seq_mask = make_seq_tensor(train_seq_raw)

val_seq_raw = [get_seq(u.item()) for u in val_user_ids]
val_seq, val_seq_mask = make_seq_tensor(val_seq_raw)

# ==================== 6. 模型初始化 ====================
in_dim_dict = {nt: data[nt].x.size(1) for nt in data.node_types}
graph_encoder = HGTEncoder(in_dim_dict, cfg.HIDDEN_DIM, cfg.OUT_DIM,
                           data.metadata(), cfg.NUM_HEADS).to(cfg.DEVICE)

# 处理 comment_encoder 可能为 None 的情况
num_comments = len(comment_encoder.classes_) if comment_encoder else 0

model = SGC_Rec(
    num_users=len(user_encoder.classes_),
    num_posts=num_posts_total + 1,
    num_tags=len(tag_encoder.classes_),
    num_positions=len(pos_encoder.classes_),
    num_comments=num_comments,
    seq_emb_dim=cfg.OUT_DIM,
    graph_hidden_dim=cfg.HIDDEN_DIM,
    graph_out_dim=cfg.OUT_DIM,
    seq_len=cfg.SEQ_LEN,
    num_tasks=cfg.NUM_TASKS,
    expert_dim=cfg.EXPERT_DIM,
    num_experts=cfg.NUM_EXPERTS,
    padding_idx=padding_idx
).to(cfg.DEVICE)
model.set_graph_encoder(graph_encoder)

# ==================== 加载预训练权重（部分匹配） ====================
if os.path.exists(cfg.BEST_MODEL):
    print(f"正在加载预训练权重: {cfg.BEST_MODEL}")
    old_state_dict = torch.load(cfg.BEST_MODEL, map_location='cpu')
    model_state_dict = model.state_dict()
    filtered_state_dict = {}
    for k, v in old_state_dict.items():
        if k in model_state_dict and v.shape == model_state_dict[k].shape:
            filtered_state_dict[k] = v
        else:
            print(f"跳过参数 {k}，形状不匹配: 旧 {v.shape} vs 新 {model_state_dict.get(k, 'None')}")
    model.load_state_dict(filtered_state_dict, strict=False)
    print(f"成功加载 {len(filtered_state_dict)} 个匹配的参数")
else:
    print("未找到预训练权重，将从头训练")

if USE_COMPILE and hasattr(torch, 'compile'):
    print("编译模型...")
    model = torch.compile(model, mode="reduce-overhead")
    graph_encoder = torch.compile(graph_encoder, mode="reduce-overhead")

# ==================== 7. 倾向性模型预训练 ====================
# 由于数据量小，我们可以跳过倾向性模型，直接使用 None 作为 propensity
# 但保留代码以保持完整性
propensity_model = PropensityModel(
    user_dim=data["user"].x.size(1),
    post_dim=data["post"].x.size(1),
    pos_dim=data["position"].x.size(1)
).to(cfg.DEVICE)
if USE_COMPILE and hasattr(torch, 'compile'):
    propensity_model = torch.compile(propensity_model, mode="reduce-overhead")

print("预训练倾向性模型...")
optimizer_prop = torch.optim.Adam(propensity_model.parameters(), lr=1e-3)
loss_fn = nn.BCELoss()
neg_ratio = 5

for epoch in range(30):
    # 如果正样本太少，调整采样大小
    max_pos = min(len(train_user_ids), cfg.BATCH_SIZE * 10)
    pos_indices = torch.randperm(len(train_user_ids))[:max_pos]
    pos_u = train_user_ids[pos_indices]
    pos_p = train_post_ids[pos_indices]
    pos_pos = train_pos[pos_indices]

    neg_u = torch.randint(0, data["user"].x.size(0), (len(pos_u) * neg_ratio,), device=cfg.DEVICE)
    neg_p = torch.randint(0, data["post"].x.size(0), (len(pos_u) * neg_ratio,), device=cfg.DEVICE)
    neg_pos = torch.randint(0, data["position"].x.size(0), (len(pos_u) * neg_ratio,), device=cfg.DEVICE)

    user_feat = torch.cat([data["user"].x[pos_u], data["user"].x[neg_u]], dim=0)
    post_feat = torch.cat([data["post"].x[pos_p], data["post"].x[neg_p]], dim=0)
    pos_feat = torch.cat([data["position"].x[pos_pos], data["position"].x[neg_pos]], dim=0)
    labels = torch.cat([torch.ones(len(pos_u)), torch.zeros(len(pos_u) * neg_ratio)], dim=0).to(cfg.DEVICE)

    optimizer_prop.zero_grad()
    pred = propensity_model(user_feat, post_feat, pos_feat)
    loss = loss_fn(pred, labels)
    loss.backward()
    optimizer_prop.step()

    if epoch % 10 == 0:
        print(f"Propensity epoch {epoch}, loss={loss.item():.4f}")

propensity_model.eval()

# ==================== 8. 主模型训练（简化版，仅用 BCE） ====================
optimizer = torch.optim.Adam(model.parameters(), lr=cfg.LR)

# 直接使用 data.edge_index_dict（已确保所有边非空）
edge_index_dict = data.edge_index_dict

best_val_auc = 0.0
batch_size = min(cfg.BATCH_SIZE, len(train_user_ids))  # 防止 batch 大于样本数

log_path = os.path.join(cfg.MODEL_DIR, 'training_log.csv')
if not os.path.exists(log_path):
    with open(log_path, 'w') as f:
        f.write("epoch,val_auc\n")

for epoch in range(cfg.EPOCHS):
    model.train()
    total_loss = 0.0
    perm = torch.randperm(len(train_user_ids), device=cfg.DEVICE)

    for i in range(0, len(perm), batch_size):
        idx = perm[i:i+batch_size]
        u_batch = train_user_ids[idx]
        p_batch = train_post_ids[idx]
        y_batch = train_labels[idx]
        seq_batch = train_seq[idx]
        seq_mask_batch = train_seq_mask[idx]

        optimizer.zero_grad()

        # 前向传播，不使用倾向性模型（避免额外计算）
        logits, _ = model(
            data.x_dict, edge_index_dict,
            u_batch, p_batch, seq_batch, seq_mask_batch, propensity=None
        )

        loss = F.binary_cross_entropy_with_logits(logits, y_batch)

        loss.backward()
        torch.nn.utils.clip_grad_norm_(model.parameters(), max_norm=1.0)
        optimizer.step()

        total_loss += loss.item() * len(u_batch)

    # 验证
    if epoch % VALIDATION_INTERVAL == 0:
        print(f"验证集正样本数: {val_labels.sum().item()}")
        model.eval()
        val_aucs = []
        with torch.no_grad():
            for i in range(0, len(val_user_ids), batch_size):
                u_batch = val_user_ids[i:i+batch_size]
                p_batch = val_post_ids[i:i+batch_size]
                y_batch = val_labels[i:i+batch_size]
                seq_batch = val_seq[i:i+batch_size]
                seq_mask_batch = val_seq_mask[i:i+batch_size]

                logits, _ = model(
                    data.x_dict, edge_index_dict,
                    u_batch, p_batch, seq_batch, seq_mask_batch, propensity=None
                )
                probs = torch.sigmoid(logits).cpu().numpy()
                y_true_np = y_batch.cpu().numpy()
                for t in range(cfg.NUM_TASKS):
                    if y_true_np[:, t].sum() > 0:
                        auc = roc_auc_score(y_true_np[:, t], probs[:, t])
                        val_aucs.append(auc)
        avg_auc = np.mean(val_aucs) if val_aucs else 0.0
        print(f"Epoch {epoch}, loss={total_loss/len(perm):.4f}, val AUC={avg_auc:.4f}")

        if avg_auc > best_val_auc:
            best_val_auc = avg_auc
            torch.save(model.state_dict(), cfg.BEST_MODEL)
            print(f"  -> Best model saved (AUC={avg_auc:.4f})")

        with open(log_path, 'a') as f:
            f.write(f"{epoch},{avg_auc}\n")

# ==================== 9. 保存最终嵌入 ====================
# 直接使用训练过程中保存的最佳模型，无需再加载旧模型
if os.path.exists(cfg.BEST_MODEL):
    print("加载最佳模型用于生成嵌入...")
    model.load_state_dict(torch.load(cfg.BEST_MODEL, map_location='cpu'))
else:
    print("警告：未找到最佳模型，使用当前模型生成嵌入")

model.eval()
with torch.no_grad():
    final_emb = model.graph_encoder(data.x_dict, edge_index_dict)
    user_emb = final_emb["user"].cpu().numpy()
    post_emb = final_emb["post"].cpu().numpy()
    np.save(cfg.USER_EMB_NPY, user_emb)
    np.save(cfg.POST_EMB_NPY_FINAL, post_emb)

print("训练完成。")