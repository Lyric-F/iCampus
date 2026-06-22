#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import os
import sys
import json
import pymysql
import pandas as pd
from datetime import datetime

DB_CONFIG = {
    'host': '127.0.0.1',
    'user': 'root',
    'password': '123456!Qwerty',
    'database': 'campus_db',
    'charset': 'utf8mb4'
}

CACHE_PATH = '/opt/app/iCampus_rec/data/stats_cache.json'

def get_db_connection():
    return pymysql.connect(**DB_CONFIG)

def update_stats():
    conn = get_db_connection()
    try:
        # 1. 总用户数
        with conn.cursor() as cur:
            cur.execute("SELECT COUNT(*) FROM user")
            total_users = cur.fetchone()[0]

        # 2. 总帖子数
        with conn.cursor() as cur:
            cur.execute("SELECT COUNT(*) FROM post")
            total_posts = cur.fetchone()[0]

        # 3. 总交互数
        with conn.cursor() as cur:
            cur.execute("SELECT COUNT(*) FROM like_dislike")
            total_interactions = cur.fetchone()[0]

        # 4. 交互行为分布
        with conn.cursor() as cur:
            cur.execute("SELECT type, COUNT(*) FROM like_dislike GROUP BY type")
            rows = cur.fetchall()
            action_distribution = {row[0]: row[1] for row in rows}

        # 5. 每日交互趋势
        with conn.cursor() as cur:
            cur.execute("""
                SELECT DATE(create_time) as date, COUNT(*) 
                FROM like_dislike 
                WHERE create_time IS NOT NULL
                GROUP BY DATE(create_time)
                ORDER BY date
            """)
            rows = cur.fetchall()
            daily_trend = {str(row[0]): row[1] for row in rows}

        # 6. 热门帖子分类 Top 10（使用 type 字段）
        with conn.cursor() as cur:
            cur.execute("""
                SELECT type, COUNT(*) as cnt
                FROM post
                WHERE type IS NOT NULL AND type != ''
                GROUP BY type
                ORDER BY cnt DESC
                LIMIT 10
            """)
            rows = cur.fetchall()
            tag_counts = {row[0]: row[1] for row in rows}

        # 7. 用户活跃度分布
        with conn.cursor() as cur:
            cur.execute("""
                SELECT student_id, COUNT(*) as cnt
                FROM like_dislike
                GROUP BY student_id
            """)
            rows = cur.fetchall()
            user_activity = [row[1] for row in rows]
            bins = [0, 10, 50, 100, 200, 500, 1000]
            labels = ['0-10', '11-50', '51-100', '101-200', '201-500', '501+']
            if user_activity:
                activity_series = pd.Series(user_activity)
                user_activity_cut = pd.cut(activity_series, bins=bins, labels=labels, right=False).value_counts().to_dict()
            else:
                user_activity_cut = {label: 0 for label in labels}
            for label in labels:
                if label not in user_activity_cut:
                    user_activity_cut[label] = 0

        stats_data = {
            'tag_distribution': tag_counts,
            'action_distribution': action_distribution,
            'daily_trend': daily_trend,
            'user_activity': user_activity_cut,
            'total_users': total_users,
            'total_posts': total_posts,
            'total_interactions': total_interactions
        }

        with open(CACHE_PATH, 'w', encoding='utf-8') as f:
            json.dump(stats_data, f, ensure_ascii=False, indent=2)

        print(f"[{datetime.now()}] 统计缓存更新成功")
    except Exception as e:
        print(f"[{datetime.now()}] 更新失败: {e}")
    finally:
        conn.close()

if __name__ == '__main__':
    update_stats()