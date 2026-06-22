import os
import pandas as pd
import numpy as np
import random
import time
from datetime import datetime, timedelta
from tqdm import tqdm
import sys
from concurrent.futures import ThreadPoolExecutor, as_completed
import threading

sys.path.append('..')
from config import Config
from utils import call_deepseek, parse_json_response

cfg = Config()

AVG_COMMENTS_PER_POST = 3
AVG_LIKES_PER_COMMENT = 5

# 全局共享的用户列表（在生成前加载）
user_ids = None

def generate_comment(post_content):
    """根据帖子内容生成一条评论"""
    system_prompt = "你是一个校园论坛的普通学生，回复帖子时语气自然、真实。"
    prompt = f"""
请根据以下帖子内容，写一条回复评论。
帖子内容：{post_content}

要求：
- 评论要像真实学生之间的交流，可以表达赞同、提问、分享类似经历等
- 长度在20-80字之间，不要太长
- 直接返回评论内容，不要加引号或其他标记
"""
    response = call_deepseek(prompt, max_tokens=150, temperature=0.8,
                             system_prompt=system_prompt, retries=2)
    # 请求后稍作停顿，避免单个线程过快
    time.sleep(random.uniform(0.3, 0.8))
    return response if response else "谢谢分享！"

def process_post(row, user_ids, comment_id_lock, comment_id_counter):
    """为单篇帖子生成所有评论，返回评论列表"""
    local_comments = []
    post_id = row["post_id"]
    pub_time = datetime.strptime(row["publish_time"], "%Y-%m-%d %H:%M:%S")
    num_comments = np.random.poisson(AVG_COMMENTS_PER_POST)
    for _ in range(num_comments):
        user_id = random.choice(user_ids)
        delta = timedelta(days=random.randint(0,7), hours=random.randint(0,23))
        timestamp = (pub_time + delta).strftime("%Y-%m-%d %H:%M:%S")
        content = generate_comment(row["content"])
        with comment_id_lock:
            cid = comment_id_counter[0]
            comment_id_counter[0] += 1
        local_comments.append({
            "comment_id": cid,
            "user_id": user_id,
            "post_id": post_id,
            "content": content,
            "timestamp": timestamp
        })
    return local_comments

def generate_comments():
    global user_ids

    # 加载训练集帖子
    posts_path = "data/posts_train.csv"
    if not os.path.exists(posts_path):
        raise FileNotFoundError(f"训练集帖子文件不存在: {posts_path}")
    df_posts = pd.read_csv(posts_path)

    # 加载交互数据获取用户列表
    if not os.path.exists(cfg.INTERACTIONS_CSV):
        raise FileNotFoundError(f"交互数据文件不存在: {cfg.INTERACTIONS_CSV}")
    df_inter = pd.read_csv(cfg.INTERACTIONS_CSV)
    user_ids = df_inter["user_id"].unique().tolist()

    # 线程安全计数器
    comment_id_counter = [1]   # 用列表实现可变整数
    comment_id_lock = threading.Lock()

    comments = []

    # 使用线程池并发处理帖子
    max_workers = 6   # 可根据 API 限流情况调整（建议 5~8）
    with ThreadPoolExecutor(max_workers=max_workers) as executor:
        futures = {executor.submit(process_post, row, user_ids, comment_id_lock, comment_id_counter): row for _, row in df_posts.iterrows()}
        for future in tqdm(as_completed(futures), total=len(df_posts), desc="生成评论"):
            try:
                comments.extend(future.result())
            except Exception as e:
                print(f"帖子处理失败: {e}")

    # 保存评论
    df_comments = pd.DataFrame(comments)
    df_comments.to_csv(cfg.COMMENTS_CSV, index=False)
    print(f"评论数据保存至{cfg.COMMENTS_CSV}")

    # 生成评论点赞（与之前相同，单线程即可，量不大）
    comment_likes = []
    for _, row in tqdm(df_comments.iterrows(), total=len(df_comments), desc="生成评论点赞"):
        comment_id = row["comment_id"]
        author = row["user_id"]
        num_likes = np.random.poisson(AVG_LIKES_PER_COMMENT)
        other_users = [u for u in user_ids if u != author]
        if len(other_users) >= num_likes:
            liked_by = random.sample(other_users, num_likes)
        else:
            liked_by = other_users
        for liker in liked_by:
            comment_likes.append({
                "user_id": liker,
                "comment_id": comment_id,
                "timestamp": row["timestamp"]
            })
    df_likes = pd.DataFrame(comment_likes)
    df_likes.to_csv(cfg.COMMENT_LIKES_CSV, index=False)
    print(f"评论点赞数据保存至{cfg.COMMENT_LIKES_CSV}")

if __name__ == "__main__":
    generate_comments()