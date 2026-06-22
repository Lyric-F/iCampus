# config.py
import torch
import os

class Config:
    # 项目根目录
    BASE_DIR = os.path.dirname(os.path.abspath(__file__))

    # 数据目录
    DATA_DIR = os.path.join(BASE_DIR, "data")
    MODEL_DIR = os.path.join(BASE_DIR, "models")
    SEMANTIC_MODEL_DIR = os.path.join(BASE_DIR, "semantic_model", "saved_model")

    # 数据文件
    POSTS_CSV = os.path.join(DATA_DIR, "posts_train.csv")          # 训练集帖子
    INTERACTIONS_CSV = os.path.join(DATA_DIR, "interactions.csv")
    COMMENTS_CSV = os.path.join(DATA_DIR, "comments.csv")
    COMMENT_LIKES_CSV = os.path.join(DATA_DIR, "comment_likes.csv")
    POST_EMB_NPY = os.path.join(DATA_DIR, "post_embeddings.npy")    # 语义向量
    COMMENT_EMB_NPY = os.path.join(DATA_DIR, "comment_embeddings.npy")
    GRAPH_PT = os.path.join(DATA_DIR, "graph.pt")
    MAPPINGS_PKL = os.path.join(DATA_DIR, "mappings.pkl")
    ACTION_MAP_PKL = os.path.join(DATA_DIR, "action_map.pkl")

    # 模型保存路径
    BEST_MODEL = os.path.join(MODEL_DIR, "best_model.pth")
    USER_EMB_NPY = os.path.join(MODEL_DIR, "user_emb.npy")
    POST_EMB_NPY_FINAL = os.path.join(MODEL_DIR, "post_emb.npy")

    # 语义向量参数
    SEMANTIC_EMB_DIM = 256          # 自研 SimCSE 输出维度
    TARGET_DIM = 128                # 最终降维到128（仅用于后续，不影响图构建）

    # 推荐模型参数
    HIDDEN_DIM = 64
    OUT_DIM = 32
    NUM_HEADS = 2
    NUM_EXPERTS = 3
    NUM_TASKS = 1
    EXPERT_DIM = 64
    SEQ_LEN = 10                     # 从 20 降低到 10，减少稀疏用户的填充
    NUM_LAYERS_TRANS = 2
    NUM_HEADS_TRANS = 4

    # 用户特征维度（TF-IDF 输出维度）
    USER_FEAT_DIM = 32               # 从 16 增加到 32，提高用户区分度

    # 训练参数
    BATCH_SIZE = 256
    LR = 5e-4
    EPOCHS = 30
    VAL_RATIO = 0.2
    NEG_SAMPLE_RATIO = 5
    GRAPH_AUG_DROP = 0.2
    TEMPERATURE = 0.5
    LAMBDA_NEG = 0.1                 # 负采样损失权重（若模型支持）
    LAMBDA_SSL = 0.1                 # 图对比学习权重

    # 设备
    DEVICE = torch.device('cuda' if torch.cuda.is_available() else 'cpu')

    # 创建必要目录
    os.makedirs(DATA_DIR, exist_ok=True)
    os.makedirs(MODEL_DIR, exist_ok=True)
    os.makedirs(SEMANTIC_MODEL_DIR, exist_ok=True)