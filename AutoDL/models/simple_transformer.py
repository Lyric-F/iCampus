# models/simple_transformer.py
import torch
import torch.nn as nn
import math

class SimplifiedTransformerEncoder(nn.Module):
    """
    简化版 Transformer 编码器，用于句向量生成。
    可配置层数、隐藏维度、注意力头数等。
    """
    def __init__(self, vocab_size, max_len=128, hidden_dim=256, num_layers=4,
                 num_heads=8, intermediate_dim=512, dropout=0.1):
        super().__init__()
        self.token_embedding = nn.Embedding(vocab_size, hidden_dim)
        self.position_embedding = nn.Embedding(max_len, hidden_dim)
        self.dropout = nn.Dropout(dropout)

        # Transformer 编码器层
        encoder_layer = nn.TransformerEncoderLayer(
            d_model=hidden_dim,
            nhead=num_heads,
            dim_feedforward=intermediate_dim,
            dropout=dropout,
            batch_first=True,
            activation='gelu'
        )
        self.transformer = nn.TransformerEncoder(encoder_layer, num_layers=num_layers)
        self.layer_norm = nn.LayerNorm(hidden_dim)

        # 初始化参数
        self._init_weights()

    def _init_weights(self):
        for p in self.parameters():
            if p.dim() > 1:
                nn.init.xavier_uniform_(p)

    def forward(self, input_ids, attention_mask):
        """
        input_ids: [batch, seq_len]
        attention_mask: [batch, seq_len], True 为有效位置
        """
        seq_len = input_ids.size(1)
        positions = torch.arange(seq_len, device=input_ids.device).unsqueeze(0)  # [1, L]
        # 位置嵌入
        pos_emb = self.position_embedding(positions)  # [1, L, D]
        # Token嵌入
        token_emb = self.token_embedding(input_ids)   # [B, L, D]
        x = token_emb + pos_emb
        x = self.dropout(x)

        # Transformer 需要 padding mask，且 mask 为 True 的位置被忽略
        # 注意：Transformer 中 src_key_padding_mask 为 True 的位置会被 mask
        # 这里 attention_mask 为 True 表示有效位置，所以需要取反
        padding_mask = ~attention_mask  # [B, L]

        x = self.transformer(x, src_key_padding_mask=padding_mask)
        x = self.layer_norm(x)
        return x