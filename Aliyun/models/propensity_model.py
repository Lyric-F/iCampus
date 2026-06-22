import torch
import torch.nn as nn

class PropensityModel(nn.Module):
    def __init__(self, user_dim, post_dim, pos_dim, hidden_dim=64):
        super().__init__()
        self.fc = nn.Sequential(
            nn.Linear(user_dim + post_dim + pos_dim, hidden_dim),
            nn.ReLU(),
            nn.Dropout(0.2),
            nn.Linear(hidden_dim, 1),
            nn.Sigmoid()
        )
    def forward(self, user_emb, post_emb, pos_emb):
        x = torch.cat([user_emb, post_emb, pos_emb], dim=-1)
        return self.fc(x).squeeze(-1)