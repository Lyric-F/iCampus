import os
import pandas as pd
import numpy as np
import random
import pickle
from datetime import datetime, timedelta
from tqdm import tqdm
import sys
sys.path.append('..')
from config import Config

cfg = Config()

NUM_USERS = 5000
AVG_ACTIONS_PER_USER = 150
POS_RATIO = 0.3

INTEREST_TOPICS = ["学习", "活动", "求助", "失物", "二手", "风景", "吐槽", "干货", "组队", "情感"]
NUM_TOPICS = len(INTEREST_TOPICS)

def simulate_exposure():
    raw_weights = [0.1,0.09,0.08,0.07,0.06,0.05,0.05,0.05,0.05,0.04,0.04,0.04,0.03,0.03,0.03,0.03,0.02,0.02,0.02,0.02]
    total = sum(raw_weights)
    weights = [w/total for w in raw_weights]
    return np.random.choice(range(1,21), p=weights)

def generate_interactions():
    posts_path = "data/posts_train.csv"
    if not os.path.exists(posts_path):
        raise FileNotFoundError(f"训练集帖子文件不存在: {posts_path}")
    df_posts = pd.read_csv(posts_path)
    num_posts = len(df_posts)
    print(f"加载 {num_posts} 篇帖子")

    post_publish_time = dict(zip(df_posts["post_id"], df_posts["publish_time"]))

    # 分配帖子主题（第一个标签）
    post_topic = []
    for _, row in df_posts.iterrows():
        tags = row["tags"].split(",")
        tag = tags[0] if tags else "学习"
        if tag in INTEREST_TOPICS:
            post_topic.append(INTEREST_TOPICS.index(tag))
        else:
            post_topic.append(random.randint(0, NUM_TOPICS-1))
    post_topic = np.array(post_topic)

    # 每个用户随机选择2个感兴趣的主题
    user_interests = [np.random.choice(NUM_TOPICS, size=2, replace=False) for _ in range(NUM_USERS)]

    interactions = []
    action_map = {}

    for user_id in tqdm(range(1, NUM_USERS+1), desc="生成交互"):
        interest_set = set(user_interests[user_id-1])
        # 正样本池：帖子主题在 interest_set 中
        pos_pool = [idx for idx, t in enumerate(post_topic) if t in interest_set]
        # 负样本池：帖子主题不在 interest_set 中
        neg_pool = [idx for idx, t in enumerate(post_topic) if t not in interest_set]

        # 如果某个池为空，则从所有帖子中随机（防御）
        if len(pos_pool) == 0:
            pos_pool = list(range(num_posts))
        if len(neg_pool) == 0:
            neg_pool = list(range(num_posts))

        total_actions = np.random.poisson(AVG_ACTIONS_PER_USER)
        pos_count = max(1, int(total_actions * POS_RATIO))
        neg_count = total_actions - pos_count

        # 采样（允许重复）
        pos_indices = np.random.choice(pos_pool, size=pos_count, replace=True)
        neg_indices = np.random.choice(neg_pool, size=neg_count, replace=True)

        for idx in pos_indices:
            post_id = idx + 1
            position = simulate_exposure()
            pub_time = datetime.strptime(post_publish_time[post_id], "%Y-%m-%d %H:%M:%S")
            delta = timedelta(days=random.randint(0,7), hours=random.randint(0,23))
            timestamp = (pub_time + delta).strftime("%Y-%m-%d %H:%M:%S")
            interactions.append({
                "user_id": f"user_{user_id}",
                "post_id": post_id,
                "action": "like",
                "position": position,
                "timestamp": timestamp,
                "weight": 1.0
            })
            action_map[(f"user_{user_id}", post_id)] = "like"

        for idx in neg_indices:
            post_id = idx + 1
            position = simulate_exposure()
            pub_time = datetime.strptime(post_publish_time[post_id], "%Y-%m-%d %H:%M:%S")
            delta = timedelta(days=random.randint(0,7), hours=random.randint(0,23))
            timestamp = (pub_time + delta).strftime("%Y-%m-%d %H:%M:%S")
            interactions.append({
                "user_id": f"user_{user_id}",
                "post_id": post_id,
                "action": "view",
                "position": position,
                "timestamp": timestamp,
                "weight": 0.5
            })
            if (f"user_{user_id}", post_id) not in action_map:
                action_map[(f"user_{user_id}", post_id)] = "view"

    df = pd.DataFrame(interactions)
    df.to_csv(cfg.INTERACTIONS_CSV, index=False)
    with open(cfg.ACTION_MAP_PKL, "wb") as f:
        pickle.dump(action_map, f)
    print(f"交互数据保存至{cfg.INTERACTIONS_CSV}，行为映射保存至{cfg.ACTION_MAP_PKL}")

if __name__ == "__main__":
    generate_interactions()