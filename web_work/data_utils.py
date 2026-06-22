import torch
import numpy as np
import pandas as pd
from torch_geometric.data import HeteroData
from sklearn.preprocessing import LabelEncoder
from sklearn.feature_extraction.text import TfidfVectorizer
import pickle
import datetime
from config import Config

cfg = Config()

def build_graph():
    """构建异构图，返回 HeteroData 和映射"""
    # 加载数据
    df_posts = pd.read_csv(cfg.POSTS_CSV)
    df_inter = pd.read_csv(cfg.INTERACTIONS_CSV)
    df_comments = pd.read_csv(cfg.COMMENTS_CSV)
    df_comment_likes = pd.read_csv(cfg.COMMENT_LIKES_CSV)
    post_emb = np.load(cfg.POST_EMB_NPY)
    comment_emb = np.load(cfg.COMMENT_EMB_NPY)

    # ---------- 清洗：删除第二行表头（如果存在） ----------
    for df in [df_inter, df_comments, df_comment_likes]:
        if df.iloc[0,0] == df.columns[0] and len(df) > 1:
            df.drop(index=0, inplace=True)
            df.reset_index(drop=True, inplace=True)

    # ---------- 编码器 ----------
    # 用户编码
    user_encoder = LabelEncoder()
    valid_users = df_inter["user_id"].dropna().astype(str).unique()
    user_encoder.fit(valid_users)
    user_map = {str(u): i for i, u in enumerate(user_encoder.classes_)}
    num_users = len(user_map)

    # 帖子编码
    post_encoder = LabelEncoder()
    valid_posts = df_posts["post_id"].dropna().astype(str).unique()
    post_encoder.fit(valid_posts)
    post_map = {str(p): i for i, p in enumerate(post_encoder.classes_)}
    num_posts = len(post_map)

    # 标签编码
    all_tags = set()
    for tags in df_posts["tags"].dropna().str.split(","):
        all_tags.update(tags)
    tag_encoder = LabelEncoder()
    tag_encoder.fit(list(all_tags))
    tag_map = {str(t): i for i, t in enumerate(tag_encoder.classes_)}
    num_tags = len(tag_map)

    # 位置编码（1~20）
    pos_encoder = LabelEncoder()
    pos_encoder.fit(range(1, 21))
    pos_map = {str(p): i for i, p in enumerate(pos_encoder.classes_)}
    num_positions = len(pos_map)

    # 评论编码
    comment_encoder = LabelEncoder()
    valid_comments = df_comments["comment_id"].dropna().astype(str).unique()
    comment_encoder.fit(valid_comments)
    comment_map = {str(c): i for i, c in enumerate(comment_encoder.classes_)}
    num_comments = len(comment_map)

    # ---------- 过滤外键，只保留有效数据 ----------
    def filter_by_key(df, col, key_map):
        if col not in df.columns:
            return df
        return df[df[col].astype(str).isin(key_map.keys())]

    df_inter = filter_by_key(df_inter, "user_id", user_map)
    df_inter = filter_by_key(df_inter, "post_id", post_map)
    df_comments = filter_by_key(df_comments, "user_id", user_map)
    df_comments = filter_by_key(df_comments, "post_id", post_map)
    df_comments = filter_by_key(df_comments, "comment_id", comment_map)
    df_comment_likes = filter_by_key(df_comment_likes, "user_id", user_map)
    df_comment_likes = filter_by_key(df_comment_likes, "comment_id", comment_map)

    # ---------- 节点特征 ----------
    # 为用户生成有意义的特征（基于历史标签的 TF‑IDF）
    user_ids = user_encoder.classes_
    user_tag_texts = []
    for uid in user_ids:
        user_posts = df_inter[df_inter['user_id'].astype(str) == str(uid)]['post_id'].tolist()
        tags = set()
        for pid in user_posts:
            # 获取帖子标签，处理可能的 NaN
            post_tags_series = df_posts[df_posts['post_id'].astype(str) == str(pid)]['tags']
            if not post_tags_series.empty:
                post_tags = post_tags_series.iloc[0]
                if pd.notna(post_tags):
                    for t in post_tags.split(','):
                        tags.add(t.strip())
        if tags:
            user_tag_texts.append(' '.join(tags))
        else:
            user_tag_texts.append('')   # 无交互用户用空字符串
    # TF-IDF 向量化，维度由 config 决定
    vectorizer = TfidfVectorizer(max_features=cfg.USER_FEAT_DIM, token_pattern=r'(?u)\b\w+\b')
    user_tfidf = vectorizer.fit_transform(user_tag_texts).toarray()
    user_x = torch.tensor(user_tfidf, dtype=torch.float)
    print(f"用户特征维度: {user_x.shape[1]} (基于标签 TF-IDF)")

    # 帖子特征：语义向量 + 标签 one‑hot
    post_tag_onehot = []
    for _, row in df_posts.iterrows():
        tags = row["tags"]
        if pd.isna(tags):
            first_tag = ""
        else:
            first_tag = tags.split(",")[0] if "," in tags else tags
        tag_idx = tag_map.get(str(first_tag), 0)
        onehot = torch.zeros(num_tags)
        onehot[tag_idx] = 1.0
        post_tag_onehot.append(onehot)
    post_tag_onehot = torch.stack(post_tag_onehot, dim=0)
    post_x = torch.cat([torch.tensor(post_emb, dtype=torch.float), post_tag_onehot], dim=1)
    print(f"帖子特征维度: {post_x.shape[1]}")

    tag_x = torch.eye(num_tags, dtype=torch.float)
    pos_x = torch.eye(num_positions, dtype=torch.float)
    comment_x = torch.tensor(comment_emb, dtype=torch.float)

    # ---------- 时间衰减函数 ----------
    def time_decay(timestamp, half_life_days=7, ref_time=None):
        if ref_time is None:
            ref_time = datetime.datetime.now()
        try:
            ts = datetime.datetime.strptime(timestamp, "%Y-%m-%d %H:%M:%S")
        except:
            ts = ref_time
        days = (ref_time - ts).days
        return 0.5 ** (days / half_life_days)

    # 收集所有时间戳（过滤掉非日期字符串）
    def safe_timestamps(series):
        valid = []
        for val in series:
            if isinstance(val, str) and len(val) >= 19 and val[4]=='-' and val[7]=='-':
                try:
                    datetime.datetime.strptime(val, "%Y-%m-%d %H:%M:%S")
                    valid.append(val)
                except:
                    pass
        return valid

    all_timestamps = []
    if len(df_inter) > 0:
        all_timestamps.extend(safe_timestamps(df_inter["timestamp"]))
    if len(df_comments) > 0:
        all_timestamps.extend(safe_timestamps(df_comments["timestamp"]))
    if len(df_comment_likes) > 0:
        all_timestamps.extend(safe_timestamps(df_comment_likes["timestamp"]))
    if not all_timestamps:
        ref_time = datetime.datetime.now()
    else:
        max_ts = max(all_timestamps)
        ref_time = datetime.datetime.strptime(max_ts, "%Y-%m-%d %H:%M:%S")

    # ---------- 构建边 ----------
    user_post_edges_list = []
    edge_weights = []
    edge_positions = []
    edge_timestamps = []
    for _, row in df_inter.iterrows():
        u = user_map.get(str(row["user_id"]))
        p = post_map.get(str(row["post_id"]))
        pos = pos_map.get(str(row["position"]))
        if u is None or p is None or pos is None:
            continue
        user_post_edges_list.append([u, p])
        edge_positions.append(pos)
        weight = row["weight"] * time_decay(row["timestamp"], half_life_days=7, ref_time=ref_time)
        edge_weights.append(weight)
        try:
            ts = datetime.datetime.strptime(row["timestamp"], "%Y-%m-%d %H:%M:%S")
            edge_timestamps.append(ts.timestamp())
        except:
            edge_timestamps.append(0.0)
    if user_post_edges_list:
        user_post_edges = torch.tensor(user_post_edges_list, dtype=torch.long).t().contiguous()
    else:
        user_post_edges = torch.empty(2, 0, dtype=torch.long)
    edge_weights = torch.tensor(edge_weights, dtype=torch.float) if edge_weights else torch.empty(0, dtype=torch.float)
    edge_positions = torch.tensor(edge_positions, dtype=torch.long) if edge_positions else torch.empty(0, dtype=torch.long)
    edge_timestamps = torch.tensor(edge_timestamps, dtype=torch.float) if edge_timestamps else torch.empty(0, dtype=torch.float)

    # 用户-位置边
    user_pos_edges_list = []
    for _, row in df_inter.iterrows():
        u = user_map.get(str(row["user_id"]))
        pos = pos_map.get(str(row["position"]))
        if u is None or pos is None:
            continue
        user_pos_edges_list.append([u, pos])
    if user_pos_edges_list:
        user_pos_edges = torch.tensor(user_pos_edges_list, dtype=torch.long).t().contiguous()
    else:
        user_pos_edges = torch.empty(2, 0, dtype=torch.long)

    # 帖子-标签边
    post_tag_edges_list = []
    for p_id, tags in zip(df_posts["post_id"].astype(str), df_posts["tags"].fillna("").str.split(",")):
        p = post_map.get(p_id)
        if p is None:
            continue
        for tag in tags:
            if tag.strip():
                t = tag_map.get(str(tag))
                if t is not None:
                    post_tag_edges_list.append([p, t])
    if post_tag_edges_list:
        post_tag_edges = torch.tensor(post_tag_edges_list, dtype=torch.long).t().contiguous()
    else:
        post_tag_edges = torch.empty(2, 0, dtype=torch.long)

    # 用户-评论边
    user_comment_edges_list = []
    for _, row in df_comments.iterrows():
        u = user_map.get(str(row["user_id"]))
        c = comment_map.get(str(row["comment_id"]))
        if u is None or c is None:
            continue
        user_comment_edges_list.append([u, c])
    if user_comment_edges_list:
        user_comment_edges = torch.tensor(user_comment_edges_list, dtype=torch.long).t().contiguous()
    else:
        user_comment_edges = torch.empty(2, 0, dtype=torch.long)

    # 评论-帖子边
    comment_post_edges_list = []
    for _, row in df_comments.iterrows():
        c = comment_map.get(str(row["comment_id"]))
        p = post_map.get(str(row["post_id"]))
        if c is None or p is None:
            continue
        comment_post_edges_list.append([c, p])
    if comment_post_edges_list:
        comment_post_edges = torch.tensor(comment_post_edges_list, dtype=torch.long).t().contiguous()
    else:
        comment_post_edges = torch.empty(2, 0, dtype=torch.long)

    # 用户-评论点赞边
    user_comment_like_edges_list = []
    for _, row in df_comment_likes.iterrows():
        u = user_map.get(str(row["user_id"]))
        c = comment_map.get(str(row["comment_id"]))
        if u is None or c is None:
            continue
        user_comment_like_edges_list.append([u, c])
    if user_comment_like_edges_list:
        user_comment_like_edges = torch.tensor(user_comment_like_edges_list, dtype=torch.long).t().contiguous()
    else:
        user_comment_like_edges = torch.empty(2, 0, dtype=torch.long)

    # ----- 构建 HeteroData -----
    data = HeteroData()
    data["user"].x = user_x
    data["post"].x = post_x
    data["tag"].x = tag_x
    data["position"].x = pos_x
    data["comment"].x = comment_x

    # 添加正向边
    data["user", "interact", "post"].edge_index = user_post_edges
    if edge_weights.numel() > 0:
        data["user", "interact", "post"].edge_weight = edge_weights
        data["user", "interact", "post"].pos = edge_positions
        data["user", "interact", "post"].timestamp = edge_timestamps

    data["user", "exposed_at", "position"].edge_index = user_pos_edges
    data["post", "has_tag", "tag"].edge_index = post_tag_edges
    data["user", "publish_comment", "comment"].edge_index = user_comment_edges
    data["comment", "belongs_to", "post"].edge_index = comment_post_edges
    data["user", "like_comment", "comment"].edge_index = user_comment_like_edges

    # 反向边
    edges_to_reverse = [
        ("user", "interact", "post"), ("user", "exposed_at", "position"),
        ("post", "has_tag", "tag"), ("user", "publish_comment", "comment"),
        ("comment", "belongs_to", "post"), ("user", "like_comment", "comment")
    ]
    for src, rel, dst in edges_to_reverse:
        rev_rel = f"rev_{rel}"
        edge_index = data[src, rel, dst].edge_index
        data[dst, rev_rel, src].edge_index = edge_index.flip(0)
        if hasattr(data[src, rel, dst], "edge_weight"):
            data[dst, rev_rel, src].edge_weight = data[src, rel, dst].edge_weight
        if hasattr(data[src, rel, dst], "timestamp"):
            data[dst, rev_rel, src].timestamp = data[src, rel, dst].timestamp

    # 保存图与映射
    torch.save(data, cfg.GRAPH_PT)
    with open(cfg.MAPPINGS_PKL, "wb") as f:
        pickle.dump({
            "user_map": user_map, "post_map": post_map, "tag_map": tag_map,
            "pos_map": pos_map, "comment_map": comment_map,
            "user_encoder": user_encoder, "post_encoder": post_encoder,
            "tag_encoder": tag_encoder, "pos_encoder": pos_encoder,
            "comment_encoder": comment_encoder
        }, f)
    print("图构建完成")
    return data, (user_encoder, post_encoder, tag_encoder, pos_encoder, comment_encoder)


def build_sequences(df_inter, user_encoder, post_encoder, max_len=20):
    """构建用户行为序列（按时间排序）"""
    df_inter_sorted = df_inter.sort_values(['user_id', 'timestamp'])
    seq_dict = {}
    for user_id, group in df_inter_sorted.groupby('user_id'):
        posts = group['post_id'].tolist()
        post_indices = [post_encoder.transform([str(p)])[0] for p in posts]
        seq_dict[user_id] = post_indices[-max_len:]
    return seq_dict