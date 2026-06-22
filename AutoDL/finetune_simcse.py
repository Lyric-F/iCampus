# finetune_simcse.py
import os
import torch
import torch.nn.functional as F
import torch.optim as optim
from torch.utils.data import DataLoader, Dataset
from tqdm import tqdm
from sklearn.model_selection import train_test_split
from models.simcse_simple import SimCSE

# ========== 自定义字符分词器（与预训练一致） ==========
class CharTokenizer:
    def __init__(self):
        self.char_list = [chr(i) for i in range(0x4E00, 0x9FFF+1)]
        self.char_list += list("，。！？；：“”‘’、—…·abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789")
        self.vocab = {ch: idx+1 for idx, ch in enumerate(self.char_list)}
        self.vocab['[PAD]'] = 0
        self.vocab_size = len(self.vocab)

    def encode(self, text, max_len=None):
        ids = [self.vocab.get(ch, 0) for ch in text]
        if max_len:
            ids = ids[:max_len]
        return ids

    def batch_encode(self, texts, max_len, padding=True):
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
BATCH_SIZE = 256          # 微调数据少，可用较小 batch
EPOCHS = 3                # 3 轮足够
LR = 1e-5                 # 微调学习率
TEMPERATURE = 0.05
MAX_LEN = 128
PRETRAIN_DATA = "./finetune_sentences.txt"       # 微调数据
PRETRAIN_MODEL = "./semantic_model/saved_model/simcse.pth"  # 预训练模型
SAVE_DIR = "./semantic_model/saved_model"
FINAL_MODEL_NAME = "simcse_finetuned.pth"

VAL_RATIO = 0.01          # 从微调数据中分 1% 作为验证集

# 模型结构参数（必须与预训练一致）
HIDDEN_DIM = 256
NUM_LAYERS = 4
NUM_HEADS = 8
INTERMEDIATE_DIM = 512
DROPOUT = 0.1
POOLING = 'mean'

DEVICE = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
print(f"使用设备: {DEVICE}")

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
    encoded = tokenizer.batch_encode(batch, max_len=max_len, padding=True)
    input_ids = torch.tensor(encoded, dtype=torch.long)
    attention_mask = (input_ids != 0).bool()
    return {'input_ids': input_ids, 'attention_mask': attention_mask}

def evaluate_loss(model, dataloader, device):
    model.eval()
    total_loss = 0
    with torch.no_grad():
        for batch in dataloader:
            input_ids = batch['input_ids'].to(device)
            attention_mask = batch['attention_mask'].to(device)
            emb1 = model(input_ids, attention_mask)
            emb2 = model(input_ids, attention_mask)
            emb1 = F.normalize(emb1, p=2, dim=1)
            emb2 = F.normalize(emb2, p=2, dim=1)
            sim = torch.matmul(emb1, emb2.T) / TEMPERATURE
            labels = torch.arange(sim.size(0), device=device)
            loss = F.cross_entropy(sim, labels)
            total_loss += loss.item()
    return total_loss / len(dataloader)

def train():
    tokenizer = CharTokenizer()
    vocab_size = tokenizer.vocab_size

    with open(PRETRAIN_DATA, 'r', encoding='utf-8') as f:
        all_texts = [line.strip() for line in f if line.strip()]
    print(f"总句子数: {len(all_texts)}")

    # 划分训练/验证
    train_texts, val_texts = train_test_split(all_texts, test_size=VAL_RATIO, random_state=42)
    print(f"训练集: {len(train_texts)} 句, 验证集: {len(val_texts)} 句")

    # 创建 DataLoader
    train_dataset = TextDataset(train_texts, tokenizer, MAX_LEN)
    val_dataset = TextDataset(val_texts, tokenizer, MAX_LEN)

    train_loader = DataLoader(train_dataset, batch_size=BATCH_SIZE, shuffle=True,
                              collate_fn=lambda x: collate_fn(x, tokenizer, MAX_LEN),
                              num_workers=4, pin_memory=True)
    val_loader = DataLoader(val_dataset, batch_size=BATCH_SIZE, shuffle=False,
                            collate_fn=lambda x: collate_fn(x, tokenizer, MAX_LEN),
                            num_workers=4, pin_memory=True)

    # 创建模型（架构与预训练一致）
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

    # 加载预训练权重
    if os.path.exists(PRETRAIN_MODEL):
        state_dict = torch.load(PRETRAIN_MODEL, map_location=DEVICE)
        model.load_state_dict(state_dict)
        print(f"加载预训练模型: {PRETRAIN_MODEL}")
    else:
        print("警告：预训练模型不存在，将从零开始训练！")

    optimizer = optim.AdamW(model.parameters(), lr=LR)

    best_val_loss = float('inf')
    for epoch in range(EPOCHS):
        model.train()
        total_loss = 0
        progress_bar = tqdm(train_loader, desc=f"Epoch {epoch+1}/{EPOCHS}")

        for batch in progress_bar:
            input_ids = batch['input_ids'].to(DEVICE)
            attention_mask = batch['attention_mask'].to(DEVICE)

            optimizer.zero_grad()
            emb1 = model(input_ids, attention_mask)
            emb2 = model(input_ids, attention_mask)
            emb1 = F.normalize(emb1, p=2, dim=1)
            emb2 = F.normalize(emb2, p=2, dim=1)
            sim = torch.matmul(emb1, emb2.T) / TEMPERATURE
            labels = torch.arange(sim.size(0), device=DEVICE)
            loss = F.cross_entropy(sim, labels)

            loss.backward()
            optimizer.step()

            total_loss += loss.item()
            progress_bar.set_postfix(loss=f"{loss.item():.4f}")

        avg_loss = total_loss / len(train_loader)
        val_loss = evaluate_loss(model, val_loader, DEVICE)
        print(f"Epoch {epoch+1} | Train Loss: {avg_loss:.4f} | Val Loss: {val_loss:.4f}")

        if val_loss < best_val_loss:
            best_val_loss = val_loss
            torch.save(model.state_dict(), os.path.join(SAVE_DIR, FINAL_MODEL_NAME))
            print(f"  -> 保存最佳微调模型 (val_loss={val_loss:.4f})")

    print("微调完成！")

if __name__ == "__main__":
    train()