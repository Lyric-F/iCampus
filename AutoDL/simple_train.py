import torch
import torch.nn as nn
import pandas as pd
import numpy as np
from sklearn.metrics import roc_auc_score
from torch.utils.data import DataLoader, Dataset
from sklearn.model_selection import train_test_split
from config import Config

cfg = Config()

# 加载交互数据
df = pd.read_csv(cfg.INTERACTIONS_CSV)
df['label'] = (df['action'] == 'like').astype(int)

# 构建用户/帖子映射
user2idx = {u:i for i,u in enumerate(df['user_id'].unique())}
post2idx = {p:i for i,p in enumerate(df['post_id'].unique())}
df['user_idx'] = df['user_id'].map(user2idx)
df['post_idx'] = df['post_id'].map(post2idx)

# 平衡采样：正负样本1:1
pos = df[df['label']==1]
neg = df[df['label']==0].sample(len(pos), random_state=42)
df_balanced = pd.concat([pos, neg])

train_df, val_df = train_test_split(df_balanced, test_size=0.2, random_state=42)

class SimpleDataset(Dataset):
    def __init__(self, df):
        self.users = df['user_idx'].values
        self.posts = df['post_idx'].values
        self.labels = df['label'].values
    def __len__(self):
        return len(self.labels)
    def __getitem__(self, idx):
        return self.users[idx], self.posts[idx], self.labels[idx]

train_loader = DataLoader(SimpleDataset(train_df), batch_size=1024, shuffle=True)
val_loader = DataLoader(SimpleDataset(val_df), batch_size=1024)

class SimpleModel(nn.Module):
    def __init__(self, num_users, num_posts, emb_dim=32):
        super().__init__()
        self.user_emb = nn.Embedding(num_users, emb_dim)
        self.post_emb = nn.Embedding(num_posts, emb_dim)
    def forward(self, u, p):
        return (self.user_emb(u) * self.post_emb(p)).sum(dim=1, keepdim=True)

model = SimpleModel(len(user2idx), len(post2idx)).cuda()
optimizer = torch.optim.Adam(model.parameters(), lr=1e-3)
criterion = nn.BCEWithLogitsLoss()

for epoch in range(10):
    model.train()
    for u, p, y in train_loader:
        u = u.cuda(); p = p.cuda(); y = y.float().cuda().unsqueeze(1)
        optimizer.zero_grad()
        loss = criterion(model(u, p), y)
        loss.backward()
        optimizer.step()
    # 验证
    model.eval()
    preds, trues = [], []
    with torch.no_grad():
        for u, p, y in val_loader:
            u = u.cuda(); p = p.cuda()
            preds.append(torch.sigmoid(model(u, p)).cpu().numpy())
            trues.append(y.numpy())
    auc = roc_auc_score(np.concatenate(trues), np.concatenate(preds))
    print(f"Epoch {epoch}, AUC: {auc:.4f}")