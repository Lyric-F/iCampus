import torch
import torch.nn as nn
import torch.nn.functional as F
from torch_geometric.nn import HGTConv


class SequentialEncoder(nn.Module):
    """
    用户行为序列编码器（Transformer）
    """
    def __init__(self, num_posts, emb_dim, hidden_dim, num_layers=2, num_heads=4, max_len=20):
        super().__init__()
        self.item_emb = nn.Embedding(num_posts, emb_dim)
        self.pos_emb = nn.Embedding(max_len, emb_dim)
        encoder_layer = nn.TransformerEncoderLayer(
            d_model=emb_dim, nhead=num_heads, dim_feedforward=hidden_dim,
            batch_first=True, activation='gelu'
        )
        self.transformer = nn.TransformerEncoder(encoder_layer, num_layers=num_layers)

    def forward(self, seq_ids, seq_mask):
        B, L = seq_ids.shape
        pos_ids = torch.arange(L, device=seq_ids.device).unsqueeze(0).expand(B, -1)
        emb = self.item_emb(seq_ids) + self.pos_emb(pos_ids)
        emb = self.transformer(emb, src_key_padding_mask=~seq_mask)
        pooled = emb.mean(dim=1)
        return pooled


class HGTEncoder(nn.Module):
    """异构图编码器（HGT）"""
    def __init__(self, in_dim_dict, hidden_dim, out_dim, metadata, num_heads=4):
        super().__init__()
        self.lin_dict = nn.ModuleDict()
        for node_type, in_dim in in_dim_dict.items():
            self.lin_dict[node_type] = nn.Linear(in_dim, hidden_dim)
        self.hgt1 = HGTConv(hidden_dim, hidden_dim, metadata, num_heads)
        self.hgt2 = HGTConv(hidden_dim, out_dim, metadata, num_heads)
        self.dropout = nn.Dropout(0.2)

    def forward(self, x_dict, edge_index_dict):
        x_dict = {nt: self.lin_dict[nt](x) for nt, x in x_dict.items()}
        x_dict = self.hgt1(x_dict, edge_index_dict)
        x_dict = {k: F.elu(x) for k, x in x_dict.items()}
        x_dict = self.hgt2(x_dict, edge_index_dict)
        x_dict = {k: self.dropout(x) for k, x in x_dict.items()}
        return x_dict


class PLE(nn.Module):
    """渐进式分层提取（简化版）"""
    def __init__(self, input_dim, num_experts, num_tasks, expert_dim):
        super().__init__()
        self.shared_experts = nn.ModuleList([nn.Linear(input_dim, expert_dim) for _ in range(num_experts)])
        self.task_experts = nn.ModuleList([
            nn.ModuleList([nn.Linear(input_dim, expert_dim) for _ in range(num_experts)])
            for _ in range(num_tasks)
        ])
        self.task_gates = nn.ModuleList([nn.Linear(input_dim, num_experts) for _ in range(num_tasks)])
        self.task_layers = nn.ModuleList([nn.Linear(expert_dim, 1) for _ in range(num_tasks)])
        self.dropout = nn.Dropout(0.2)

    def forward(self, x):
        x = self.dropout(x)
        shared_out = torch.stack([expert(x) for expert in self.shared_experts], dim=1)
        task_outputs = []
        for i, (task_experts, gate, task_layer) in enumerate(zip(self.task_experts, self.task_gates, self.task_layers)):
            task_out = torch.stack([expert(x) for expert in task_experts], dim=1)
            gate_weights = F.softmax(gate(x), dim=1)
            weighted = torch.einsum('be,bed->bd', gate_weights, task_out + shared_out)
            task_outputs.append(task_layer(weighted))
        return torch.stack(task_outputs, dim=1)


class CausalGate(nn.Module):
    """因果门控"""
    def __init__(self, dim):
        super().__init__()
        self.W = nn.Linear(dim, dim, bias=False)  # 预留，可用于扩展

    def forward(self, user_emb, post_emb, propensity):
        # 处理 propensity 为 None 的情况（负样本时）
        if propensity is None:
            gate = torch.ones(user_emb.size(0), 1, device=user_emb.device)
        else:
            gate = (1 - propensity).unsqueeze(-1)
        score = (user_emb * post_emb).sum(dim=-1, keepdim=True)
        output = gate * score
        return output


class SGC_Rec(nn.Module):
    """
    序列+图+因果门控推荐模型（带可训练用户嵌入）
    """
    def __init__(self, num_users, num_posts, num_tags, num_positions, num_comments,
                 seq_emb_dim, graph_hidden_dim, graph_out_dim, seq_len, num_tasks,
                 expert_dim, num_experts=3, user_emb_dim=None, padding_idx=None):
        super().__init__()
        # 可训练的用户嵌入
        if user_emb_dim is None:
            user_emb_dim = graph_out_dim
        self.user_embedding = nn.Embedding(num_users, user_emb_dim)

        # 序列编码器
        self.seq_encoder = SequentialEncoder(
            num_posts, seq_emb_dim, seq_emb_dim * 4,
            num_layers=2, num_heads=4, max_len=seq_len
        )

        # 投影层：将拼接后的用户表示（user_emb_dim + seq_emb_dim）映射到与帖子图特征相同的维度
        self.user_proj = nn.Linear(user_emb_dim + seq_emb_dim, graph_out_dim)

        # 图编码器（外部设置）
        self.graph_encoder = None

        # 因果门控
        self.causal_gate = CausalGate(graph_out_dim)

        # PLE 多任务预测器（输入维度 = 原始用户拼接 + 帖子图 + 门控输出）
        ple_input_dim = (user_emb_dim + seq_emb_dim) + graph_out_dim + 1
        self.ple = PLE(ple_input_dim, num_experts, num_tasks, expert_dim)

        # 记录维度
        self.seq_emb_dim = seq_emb_dim
        self.graph_out_dim = graph_out_dim
        self.user_emb_dim = user_emb_dim
        self.num_tasks = num_tasks

    def set_graph_encoder(self, graph_encoder):
        self.graph_encoder = graph_encoder

    def forward(self, x_dict, edge_index_dict, user_ids, post_ids, seq_ids, seq_mask, propensity=None):
        # 图编码
        x_dict = self.graph_encoder(x_dict, edge_index_dict)
        post_graph = x_dict["post"][post_ids]                     # [B, graph_out_dim]

        # 序列编码
        user_seq = self.seq_encoder(seq_ids, seq_mask)            # [B, seq_emb_dim]

        # 可训练用户嵌入
        user_graph = self.user_embedding(user_ids)                # [B, user_emb_dim]

        # 融合用户表示（拼接）
        user_rep = torch.cat([user_graph, user_seq], dim=-1)      # [B, user_emb_dim + seq_emb_dim]

        # 投影到与帖子图相同维度，用于因果门控
        user_rep_proj = self.user_proj(user_rep)                  # [B, graph_out_dim]

        # 因果门控
        score = self.causal_gate(user_rep_proj, post_graph, propensity)   # [B, 1]

        # 多任务预测（使用原始 user_rep 以保留完整信息）
        combined = torch.cat([user_rep, post_graph, score], dim=-1)       # [B, D_user + D_post + 1]
        logits = self.ple(combined)                                        # [B, num_tasks, 1]

        return logits.squeeze(-1), x_dict