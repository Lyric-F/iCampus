# semantic_model/inference.py
import os
import torch
import numpy as np
from models.char_tokenizer import CharTokenizer   # 确保路径正确
from models.simcse_simple import SimCSE
from config import Config

class SemanticEncoder:
    def __init__(self, model_path=Config.SEMANTIC_MODEL_DIR, device=None):
        self.device = device if device else Config.DEVICE
        # 使用自定义字符分词器
        self.tokenizer = CharTokenizer()
        # 模型参数必须与训练时一致
        self.model = SimCSE(
            vocab_size=self.tokenizer.vocab_size,
            max_len=128,
            hidden_dim=256,
            num_layers=4,
            num_heads=8,
            intermediate_dim=512,
            dropout=0.1,
            pooling='mean'
        )
        # 加载微调后的模型
        model_file = os.path.join(model_path, 'simcse_finetuned.pth')
        if not os.path.exists(model_file):
            # 如果没有微调模型，尝试加载预训练模型（回退）
            model_file = os.path.join(model_path, 'simcse.pth')
            print(f"未找到微调模型，使用预训练模型: {model_file}")
        state_dict = torch.load(model_file, map_location=self.device)
        self.model.load_state_dict(state_dict)
        self.model.to(self.device)
        self.model.eval()

    def encode(self, texts, batch_size=128):
        """将文本列表编码为向量矩阵"""
        embeddings = []
        for i in range(0, len(texts), batch_size):
            batch_texts = texts[i:i+batch_size]
            # 使用自定义分词器批量编码
            encoded = self.tokenizer.batch_encode(batch_texts, max_len=128, padding=True)
            input_ids = torch.tensor(encoded, dtype=torch.long).to(self.device)
            attention_mask = (input_ids != 0).bool()
            with torch.no_grad():
                emb = self.model(input_ids, attention_mask)
            embeddings.append(emb.cpu().numpy())
        return np.concatenate(embeddings, axis=0)