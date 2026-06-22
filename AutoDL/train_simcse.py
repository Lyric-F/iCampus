# train_simcse.py
import os
import torch
import torch.nn as nn
import torch.nn.functional as F
import torch.optim as optim
from torch.utils.data import DataLoader, Dataset
from tqdm import tqdm
import time
from sklearn.model_selection import train_test_split

# 导入自研模型
from models.simcse_simple import SimCSE

# ========== 自定义字符分词器 ==========
class CharTokenizer:
    """完全自研的字符级分词器，不依赖任何外部模型"""
    def __init__(self):
        # 基本中文字符范围：CJK统一表意文字（4E00-9FFF）
        self.char_list = [chr(i) for i in range(0x4E00, 0x9FFF+1)]
        # 添加常见标点、数字、英文字母
        self.char_list += list("，。！？；：“”‘’、—…·abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789")
        self.vocab = {ch: idx+1 for idx, ch in enumerate(self.char_list)}  # 0 留给 padding
        self.vocab['[PAD]'] = 0
        self.vocab_size = len(self.vocab)

    def encode(self, text, max_len=None):
        """将文本转换为 token ids，可选截断"""
        ids = [self.vocab.get(ch, 0) for ch in text]  # 未知字符映射为 0
        if max_len:
            ids = ids[:max_len]
        return ids

    def batch_encode(self, texts, max_len, padding=True):
        """批量编码，自动 padding"""
        batch_ids = [self.encode(t, max_len) for t in texts]
        if padding:
            max_len_actual = max(len(ids) for ids in batch_ids) if max_len is None else max_len
            padded = []
            for ids in batch_ids:
                if len(ids) < max_len_actual:
                    padded.append(ids + [0] * (max_len_actual - len(ids)))
                else:
                    padded.append(ids[:max_len_actual])
            return padded
        return batch_ids

# ========== 配置参数 ==========
BATCH_SIZE = 8192                # 根据显存调整
EPOCHS = 10                      # 从头训练适当增加轮数
LR = 1e-4                        # 学习率（建议使用 warmup）
TEMPERATURE = 0.05
MAX_LEN = 128
PRETRAIN_DATA = "./sentences.txt"   # 请根据实际路径修改
SAVE_DIR = "./semantic_model/saved_model"
BEST_MODEL_NAME = "simcse_best.pth"
FINAL_MODEL_NAME = "simcse.pth"

# 验证集比例
VAL_RATIO = 0.01

# 模型结构参数
HIDDEN_DIM = 256
NUM_LAYERS = 4
NUM_HEADS = 8
INTERMEDIATE_DIM = 512
DROPOUT = 0.1
POOLING = 'mean'  # 推荐 mean

# 硬件与加速
DEVICE = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
USE_AMP = True                   # 混合精度
USE_TF32 = True                  # 若 GPU 支持
USE_COMPILE = False              # PyTorch 2.0+ 编译，可选

if USE_TF32 and DEVICE.type == 'cuda':
    torch.backends.cuda.matmul.allow_tf32 = True
    torch.backends.cudnn.allow_tf32 = True
    print("启用 TF32 加速")

print(f"使用设备: {DEVICE}")

# 创建保存目录
os.makedirs(SAVE_DIR, exist_ok=True)

# ========== 数据集 ==========
class TextDataset(Dataset):
    def __init__(self, texts, tokenizer, max_len):
        self.tokenizer = tokenizer
        self.max_len = max_len
        self.texts = texts
        print(f"加载 {len(self.texts)} 条句子")

    def __len__(self):
        return len(self.texts)

    def __getitem__(self, idx):
        return self.texts[idx]

def collate_fn(batch, tokenizer, max_len):
    """批量编码，返回 input_ids 和 bool 类型的 attention_mask"""
    encoded = tokenizer.batch_encode(batch, max_len=max_len, padding=True)
    input_ids = torch.tensor(encoded, dtype=torch.long)
    attention_mask = (input_ids != 0).bool()   # 布尔型 mask
    return {'input_ids': input_ids, 'attention_mask': attention_mask}

# ========== 验证函数 ==========
def evaluate_loss(model, dataloader, device):
    model.eval()
    total_loss = 0
    with torch.no_grad():
        for batch in dataloader:
            input_ids = batch['input_ids'].to(device)
            attention_mask = batch['attention_mask'].to(device)
            emb1 = model(input_ids, attention_mask)
            emb2 = model(input_ids, attention_mask)  # dropout 产生不同视图
            emb1 = F.normalize(emb1, p=2, dim=1)
            emb2 = F.normalize(emb2, p=2, dim=1)
            sim = torch.matmul(emb1, emb2.T) / TEMPERATURE
            labels = torch.arange(sim.size(0), device=device)
            loss = F.cross_entropy(sim, labels)
            total_loss += loss.item()
    return total_loss / len(dataloader)

# ========== 主训练函数 ==========
def train():
    # 初始化自定义分词器
    tokenizer = CharTokenizer()
    vocab_size = tokenizer.vocab_size
    print(f"词表大小: {vocab_size}")

    # 加载语料
    with open(PRETRAIN_DATA, 'r', encoding='utf-8') as f:
        all_texts = [line.strip() for line in f if line.strip()]
    print(f"总句子数: {len(all_texts)}")

    # 划分训练/验证
    train_texts, val_texts = train_test_split(all_texts, test_size=VAL_RATIO, random_state=42)
    print(f"训练集: {len(train_texts)} 句, 验证集: {len(val_texts)} 句")

    # 创建 DataLoader
    train_dataset = TextDataset(train_texts, tokenizer, MAX_LEN)
    val_dataset = TextDataset(val_texts, tokenizer, MAX_LEN)

    num_workers = min(16, os.cpu_count())
    train_loader = DataLoader(
        train_dataset, batch_size=BATCH_SIZE, shuffle=True,
        num_workers=num_workers, collate_fn=lambda x: collate_fn(x, tokenizer, MAX_LEN),
        pin_memory=True, prefetch_factor=2, persistent_workers=True
    )
    val_loader = DataLoader(
        val_dataset, batch_size=BATCH_SIZE, shuffle=False,
        num_workers=num_workers, collate_fn=lambda x: collate_fn(x, tokenizer, MAX_LEN),
        pin_memory=True
    )

    # 初始化模型（从头训练）
    model = SimCSE(
        vocab_size=vocab_size,
        max_len=MAX_LEN,
        hidden_dim=HIDDEN_DIM,
        num_layers=NUM_LAYERS,
        num_heads=NUM_HEADS,
        intermediate_dim=INTERMEDIATE_DIM,
        dropout=DROPOUT,
        pooling=POOLING
    ).to(DEVICE)

    # 可选编译加速
    if USE_COMPILE and hasattr(torch, 'compile'):
        print("使用 torch.compile 优化模型")
        model = torch.compile(model, mode="reduce-overhead")

    # 优化器与混合精度（使用新版 API）
    optimizer = optim.AdamW(model.parameters(), lr=LR)
    scaler = torch.amp.GradScaler('cuda', enabled=USE_AMP)

    # 学习率预热调度器（可选）
    total_steps = len(train_loader) * EPOCHS
    warmup_steps = int(0.1 * total_steps)
    scheduler = optim.lr_scheduler.LambdaLR(
        optimizer,
        lambda step: min(1.0, step / warmup_steps) if warmup_steps > 0 else 1.0
    )

    best_val_loss = float('inf')
    start_time = time.time()

    for epoch in range(EPOCHS):
        model.train()
        total_loss = 0
        epoch_start = time.time()
        progress_bar = tqdm(train_loader, desc=f"Epoch {epoch+1}/{EPOCHS}")

        for step, batch in enumerate(progress_bar):
            input_ids = batch['input_ids'].to(DEVICE, non_blocking=True)
            attention_mask = batch['attention_mask'].to(DEVICE, non_blocking=True)

            optimizer.zero_grad()

            with torch.amp.autocast('cuda', enabled=USE_AMP):
                emb1 = model(input_ids, attention_mask)
                emb2 = model(input_ids, attention_mask)
                emb1 = F.normalize(emb1, p=2, dim=1)
                emb2 = F.normalize(emb2, p=2, dim=1)
                sim = torch.matmul(emb1, emb2.T) / TEMPERATURE
                labels = torch.arange(sim.size(0), device=DEVICE)
                loss = F.cross_entropy(sim, labels)

            scaler.scale(loss).backward()
            scaler.step(optimizer)
            scaler.update()
            scheduler.step()  # 更新学习率

            total_loss += loss.item()
            progress_bar.set_postfix(loss=f"{loss.item():.4f}")

        avg_loss = total_loss / len(train_loader)
        epoch_time = time.time() - epoch_start

        # 验证
        val_loss = evaluate_loss(model, val_loader, DEVICE)
        print(f"Epoch {epoch+1} | Train Loss: {avg_loss:.4f} | Val Loss: {val_loss:.4f} | 耗时: {epoch_time:.2f}s")

        # 保存最佳模型
        if val_loss < best_val_loss:
            best_val_loss = val_loss
            torch.save(model.state_dict(), os.path.join(SAVE_DIR, BEST_MODEL_NAME))
            print(f"  -> 保存最佳模型 (val_loss={val_loss:.4f})")

    # 保存最终模型
    torch.save(model.state_dict(), os.path.join(SAVE_DIR, FINAL_MODEL_NAME))
    total_time = time.time() - start_time
    print(f"训练完成！总耗时: {total_time:.2f} 秒 ({total_time/3600:.2f} 小时)")
    print(f"最佳验证损失: {best_val_loss:.4f}")

if __name__ == "__main__":
    train()