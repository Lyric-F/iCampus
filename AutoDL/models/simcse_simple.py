# models/simcse_simple.py
import torch
import torch.nn as nn
import torch.nn.functional as F
from .simple_transformer import SimplifiedTransformerEncoder

class SimCSE(nn.Module):
    """
    无监督 SimCSE 模型，使用自研 Transformer 编码器。
    """
    def __init__(self, vocab_size, max_len=128, hidden_dim=256, num_layers=4,
                 num_heads=8, intermediate_dim=512, dropout=0.1, pooling='mean'):
        super().__init__()
        self.encoder = SimplifiedTransformerEncoder(
            vocab_size=vocab_size,
            max_len=max_len,
            hidden_dim=hidden_dim,
            num_layers=num_layers,
            num_heads=num_heads,
            intermediate_dim=intermediate_dim,
            dropout=dropout
        )
        self.pooling = pooling
        self.hidden_dim = hidden_dim

    def forward(self, input_ids, attention_mask):
        """
        input_ids: [batch, seq_len]
        attention_mask: [batch, seq_len], True for valid tokens
        """
        last_hidden = self.encoder(input_ids, attention_mask)  # [B, L, D]
        if self.pooling == 'cls':
            emb = last_hidden[:, 0, :]
        elif self.pooling == 'mean':
            # 均值池化，忽略 padding
            mask_expanded = attention_mask.unsqueeze(-1).float()
            sum_emb = (last_hidden * mask_expanded).sum(dim=1)
            sum_mask = mask_expanded.sum(dim=1).clamp(min=1e-9)
            emb = sum_emb / sum_mask
        else:
            raise ValueError("pooling must be 'cls' or 'mean'")
        return emb

    def encode(self, input_ids, attention_mask):
        """推理时使用，与 forward 相同"""
        return self.forward(input_ids, attention_mask)