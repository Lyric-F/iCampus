import os
import sys
import numpy as np
import pandas as pd
from tqdm import tqdm
sys.path.append('..')
from config import Config

cfg = Config()

def generate_embeddings(texts, desc="生成向量"):
    """使用语义模型生成向量"""
    # 尝试加载训练好的模型
    try:
        from semantic_model.inference import SemanticEncoder
        encoder = SemanticEncoder(model_path=cfg.SEMANTIC_MODEL_DIR)
        embeddings = encoder.encode(texts, batch_size=128)
    except Exception as e:
        print(f"语义模型加载失败，使用随机向量: {e}")
        embeddings = np.random.randn(len(texts), cfg.SEMANTIC_EMB_DIM).astype(np.float32)

    # 降维到目标维度
    if embeddings.shape[1] > cfg.TARGET_DIM:
        from sklearn.decomposition import PCA
        pca = PCA(n_components=cfg.TARGET_DIM)
        embeddings = pca.fit_transform(embeddings)
    elif embeddings.shape[1] < cfg.TARGET_DIM:
        # 补零
        pad = cfg.TARGET_DIM - embeddings.shape[1]
        embeddings = np.pad(embeddings, ((0,0),(0,pad)), mode='constant')
    return embeddings.astype(np.float32)

def generate_post_embeddings():
    df = pd.read_csv(cfg.POSTS_CSV)
    texts = []
    for _, row in df.iterrows():
        text = f"标题：{row['title']}\n内容：{row['content']}\n标签：{row['tags']}"
        texts.append(text)
    embeddings = generate_embeddings(texts, desc="生成帖子向量")
    np.save(cfg.POST_EMB_NPY, embeddings)
    print(f"帖子向量保存至{cfg.POST_EMB_NPY}，形状{embeddings.shape}")

def generate_comment_embeddings():
    df = pd.read_csv(cfg.COMMENTS_CSV)
    texts = df["content"].tolist()
    embeddings = generate_embeddings(texts, desc="生成评论向量")
    np.save(cfg.COMMENT_EMB_NPY, embeddings)
    print(f"评论向量保存至{cfg.COMMENT_EMB_NPY}，形状{embeddings.shape}")

if __name__ == "__main__":
    generate_post_embeddings()
    generate_comment_embeddings()