import os
import sys
sys.path.insert(0, '/opt/app/iCampus_rec/data')
import numpy as np
import pandas as pd
from tqdm import tqdm
sys.path.append('..')
from config import Config

cfg = Config()

def generate_embeddings(texts, desc="生成向量"):
    """
    生成文本向量（优先使用语义模型，失败时用随机向量）
    自动处理 NaN，然后降维到 TARGET_DIM
    """
    # 尝试加载语义模型
    try:
        from semantic_model.inference import SemanticEncoder
        encoder = SemanticEncoder(model_path=cfg.SEMANTIC_MODEL_DIR)
        embeddings = encoder.encode(texts, batch_size=128)
        print(f"{desc}：语义模型加载成功，原始向量形状 {embeddings.shape}")
    except Exception as e:
        print(f"{desc}：语义模型加载失败，使用随机向量: {e}")
        embeddings = np.random.randn(len(texts), cfg.SEMANTIC_EMB_DIM).astype(np.float32)

    # ----- 处理 NaN（如果存在）-----
    if np.isnan(embeddings).any():
        nan_rows = np.where(np.isnan(embeddings).any(axis=1))[0]
        print(f"{desc}：发现 {len(nan_rows)} 个 NaN 样本，将用随机向量替换")
        for idx in nan_rows:
            # 用随机向量替换
            embeddings[idx] = np.random.randn(embeddings.shape[1])
        # 再次检查，确保没有 NaN 残留
        if np.isnan(embeddings).any():
            print(f"{desc}：警告，替换后仍有 NaN，强制全部替换")
            embeddings = np.random.randn(*embeddings.shape)

    n_samples, n_features = embeddings.shape

    # ----- 降维处理 -----
    if n_features > cfg.TARGET_DIM:
        if n_samples < cfg.TARGET_DIM:
            print(f"{desc}：样本数 {n_samples} 小于目标维度 {cfg.TARGET_DIM}，直接截断或补零")
            # 如果特征维度 > 目标维度，截断；否则补零（但这里 n_features > TARGET_DIM，所以只会截断）
            embeddings = embeddings[:, :cfg.TARGET_DIM]
        else:
            from sklearn.decomposition import PCA
            pca = PCA(n_components=cfg.TARGET_DIM)
            embeddings = pca.fit_transform(embeddings)
    elif n_features < cfg.TARGET_DIM:
        pad = cfg.TARGET_DIM - n_features
        embeddings = np.pad(embeddings, ((0,0),(0,pad)), mode='constant')
    # 如果 n_features == TARGET_DIM，什么也不做

    return embeddings.astype(np.float32)

def generate_post_embeddings():
    df = pd.read_csv(cfg.POSTS_CSV)
    texts = []
    for _, row in df.iterrows():
        text = f"标题：{row['title']}\n内容：{row['content']}\n标签：{row['tags']}"
        texts.append(text)
    print(f"共 {len(texts)} 篇帖子")
    embeddings = generate_embeddings(texts, desc="生成帖子向量")
    np.save(cfg.POST_EMB_NPY, embeddings)
    print(f"帖子向量保存至{cfg.POST_EMB_NPY}，形状{embeddings.shape}")

def generate_comment_embeddings():
    if not os.path.exists(cfg.COMMENTS_CSV):
        print("评论文件不存在，跳过评论向量生成")
        return
    df = pd.read_csv(cfg.COMMENTS_CSV)
    # 过滤空内容或 NaN 的评论
    df = df.dropna(subset=['content'])
    df = df[df['content'].str.strip() != '']
    texts = df["content"].tolist()
    print(f"共 {len(texts)} 条有效评论（已过滤空内容）")
    if len(texts) == 0:
        print("没有有效评论，生成空向量文件")
        np.save(cfg.COMMENT_EMB_NPY, np.array([]))
        return
    embeddings = generate_embeddings(texts, desc="生成评论向量")
    np.save(cfg.COMMENT_EMB_NPY, embeddings)
    print(f"评论向量保存至{cfg.COMMENT_EMB_NPY}，形状{embeddings.shape}")

if __name__ == "__main__":
    generate_post_embeddings()
    generate_comment_embeddings()