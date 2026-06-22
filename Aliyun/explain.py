import numpy as np
import pandas as pd
import torch
import pickle
from config import Config
from models.sgc_rec import SGC_Rec, HGTEncoder
from data_utils import build_sequences

cfg = Config()
device = cfg.DEVICE

# 温度缩放参数（校准概率，降低极端值）
TEMPERATURE = 1.362   # 根据 calibrate_temperature.py 计算结果设置

# 加载图数据（直接使用 torch.load，不加 safe_globals）
data = torch.load(cfg.GRAPH_PT, map_location=device, weights_only=False)

with open(cfg.MAPPINGS_PKL, "rb") as f:
    mappings = pickle.load(f)
user_encoder = mappings["user_encoder"]
post_encoder = mappings["post_encoder"]
tag_encoder = mappings["tag_encoder"]
df_posts = pd.read_csv(cfg.POSTS_CSV)
df_inter = pd.read_csv(cfg.INTERACTIONS_CSV)

# 构建序列（用于推理）
seq_dict = build_sequences(df_inter, user_encoder, post_encoder, max_len=cfg.SEQ_LEN)

# 邻接表（用于解释）
user_post_edges = data["user", "interact", "post"].edge_index.cpu().numpy()
user_to_posts = {}
for i in range(user_post_edges.shape[1]):
    u, p = user_post_edges[0, i], user_post_edges[1, i]
    user_to_posts.setdefault(u, []).append(p)

post_tag_edges = data["post", "has_tag", "tag"].edge_index.cpu().numpy()
post_to_tags = {}
for i in range(post_tag_edges.shape[1]):
    p, t = post_tag_edges[0, i], post_tag_edges[1, i]
    post_to_tags.setdefault(p, []).append(t)

# 关键修复：num_posts 必须与训练时一致（+1），并传递 padding_idx
num_posts_total = len(post_encoder.classes_)
padding_idx = num_posts_total   # 训练时用这个作为填充索引

in_dim_dict = {nt: data[nt].x.size(1) for nt in data.node_types}
graph_encoder = HGTEncoder(in_dim_dict, cfg.HIDDEN_DIM, cfg.OUT_DIM, data.metadata(), cfg.NUM_HEADS).to(device)
model = SGC_Rec(
    num_users=len(user_encoder.classes_),
    num_posts=num_posts_total + 1,          # 加上 padding token
    num_tags=len(tag_encoder.classes_),
    num_positions=20,
    num_comments=len(mappings["comment_encoder"].classes_),
    seq_emb_dim=cfg.OUT_DIM,
    graph_hidden_dim=cfg.HIDDEN_DIM,
    graph_out_dim=cfg.OUT_DIM,
    seq_len=cfg.SEQ_LEN,
    num_tasks=cfg.NUM_TASKS,
    expert_dim=cfg.EXPERT_DIM,
    num_experts=cfg.NUM_EXPERTS,
    padding_idx=padding_idx                # 传递填充索引
).to(device)
model.set_graph_encoder(graph_encoder)
model.load_state_dict(torch.load(cfg.BEST_MODEL, map_location=device))
model.eval()
data = data.to(device)

with torch.no_grad():
    post_embeddings = model.graph_encoder(data.x_dict, data.edge_index_dict)["post"].cpu().numpy()

def get_user_interest_vector(user_idx):
    hist = user_to_posts.get(user_idx, [])
    if not hist:
        return None
    return np.mean([post_embeddings[p] for p in hist], axis=0)

def counterfactual_explanation(user_idx, post_idx):
    hist = user_to_posts.get(user_idx, [])
    if not hist:
        return "暂无历史行为。"
    post_vec = post_embeddings[post_idx]
    orig_vec = get_user_interest_vector(user_idx)
    if orig_vec is None:
        return "无法计算兴趣向量。"
    orig_sim = np.dot(orig_vec, post_vec) / (np.linalg.norm(orig_vec)*np.linalg.norm(post_vec)+1e-8)
    best_effect = 0
    best_post = None
    for hp in hist:
        other = [p for p in hist if p != hp]
        if not other:
            continue
        new_vec = np.mean([post_embeddings[p] for p in other], axis=0)
        new_sim = np.dot(new_vec, post_vec) / (np.linalg.norm(new_vec)*np.linalg.norm(post_vec)+1e-8)
        effect = orig_sim - new_sim
        if effect > best_effect:
            best_effect = effect
            best_post = hp
    if best_post is not None and best_effect > 0.05:
        hp_id = post_encoder.inverse_transform([best_post])[0]
        hp_title = df_posts[df_posts["post_id"] == hp_id]["title"].values[0]
        return f"如果您没有浏览过《{hp_title}》，兴趣相似度会下降约{best_effect:.3f}。"
    return "推荐主要基于长期兴趣。"

def get_meta_path_explanation(user_idx, post_idx):
    if user_idx not in user_to_posts:
        return "暂无历史交互。"
    hist = user_to_posts[user_idx]
    if post_idx not in post_to_tags:
        return "该帖子无标签。"
    post_tags = set(post_to_tags[post_idx])
    tag_hist = {}
    for hp in hist:
        if hp in post_to_tags:
            for t in post_to_tags[hp]:
                tag_hist.setdefault(t, []).append(hp)
    common = [t for t in post_tags if t in tag_hist]
    if not common:
        return "无共同标签。"
    best_tag = max(common, key=lambda t: len(tag_hist[t]))
    tag_name = tag_encoder.inverse_transform([best_tag])[0]
    ex_posts = tag_hist[best_tag][:2]
    ex_titles = []
    for hp in ex_posts:
        hp_id = post_encoder.inverse_transform([hp])[0]
        ex_titles.append(df_posts[df_posts["post_id"] == hp_id]["title"].values[0])
    if len(ex_titles) == 1:
        return f"因为你喜欢《{ex_titles[0]}》（含“{tag_name}”标签），推荐此帖。"
    else:
        return f"因为你喜欢{', '.join([f'《{t}》' for t in ex_titles])}（含“{tag_name}”标签），推荐此帖。"

def explain_recommendation(user_id, top_k=5):
    if user_id not in user_encoder.classes_:
        return []
    user_idx = user_encoder.transform([user_id])[0]
    seq = seq_dict.get(user_id, [])
    # 使用 padding_idx 填充
    seq_tensor = torch.full((1, cfg.SEQ_LEN), padding_idx, dtype=torch.long, device=device)
    seq_mask = torch.zeros(1, cfg.SEQ_LEN, dtype=torch.bool, device=device)
    if seq:
        seq = seq[-cfg.SEQ_LEN:]
        seq_tensor[0, -len(seq):] = torch.tensor(seq, device=device)
        seq_mask[0, -len(seq):] = True

    num_posts = data["post"].x.size(0)
    all_post = torch.arange(num_posts, device=device)
    user_tensor = torch.tensor([user_idx] * num_posts, device=device)
    seq_tensor = seq_tensor.repeat(num_posts, 1)
    seq_mask = seq_mask.repeat(num_posts, 1)

    with torch.no_grad():
        logits, _ = model(data.x_dict, data.edge_index_dict, user_tensor, all_post,
                          seq_tensor, seq_mask, propensity=None)
        # 温度缩放
        calibrated_probs = torch.sigmoid(logits / TEMPERATURE)
        scores = calibrated_probs[:, 0].cpu().numpy()

    top_idx = np.argsort(scores)[-top_k:][::-1]
    recs = []
    for idx in top_idx:
        post_id = post_encoder.inverse_transform([idx])[0]
        row = df_posts[df_posts["post_id"] == post_id].iloc[0]
        meta = get_meta_path_explanation(user_idx, idx)
        cf = counterfactual_explanation(user_idx, idx)
        recs.append({
            "post_id": int(post_id),
            "title": row["title"],
            "score": float(scores[idx]),
            "explanation": f"{meta} {cf}",
            "tags": row["tags"].split(",")
        })
    return recs