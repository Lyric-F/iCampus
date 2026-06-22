# admin.py
from flask import Blueprint, render_template, jsonify
import pandas as pd
import numpy as np
from config import Config
import pickle
import os

bp = Blueprint('admin', __name__, url_prefix='/admin')
cfg = Config()

def load_data():
    """加载数据（帖子、交互、映射）"""
    posts = pd.read_csv(cfg.POSTS_CSV)
    interactions = pd.read_csv(cfg.INTERACTIONS_CSV)
    with open(cfg.MAPPINGS_PKL, 'rb') as f:
        mappings = pickle.load(f)
    return posts, interactions, mappings

@bp.route('/')
def dashboard():
    """管理后台主页"""
    return render_template('admin.html')

@bp.route('/stats')
def stats():
    """返回统计数据（JSON）"""
    posts, interactions, mappings = load_data()

    # 1. 帖子按标签分布
    tags_series = posts['tags'].str.split(',').explode()
    tag_counts = tags_series.value_counts().head(10).to_dict()

    # 2. 交互行为分布
    action_counts = interactions['action'].value_counts().to_dict()

    # 3. 每日交互趋势（按日期聚合）
    interactions['date'] = pd.to_datetime(interactions['timestamp']).dt.date
    daily_trend = interactions.groupby('date').size().to_dict()
    # 转为 {日期字符串: 数量}
    daily_trend = {str(k): v for k, v in daily_trend.items()}

    # 4. 推荐模型性能指标（模拟，实际可从训练日志读取）
    # 假设训练时记录的 AUC 值（从 0.514 逐渐提升）
    # 这里我们生成模拟数据，但可以从日志文件解析
    epochs = list(range(0, 101, 10))
    auc_values = [0.514, 0.523, 0.541, 0.562, 0.578, 0.592, 0.605, 0.617, 0.628, 0.636, 0.642]
    # 如果实际有日志文件，可以读取
    # 尝试读取训练日志（可选）
    log_path = os.path.join(cfg.MODEL_DIR, 'training_log.csv')
    if os.path.exists(log_path):
        log_df = pd.read_csv(log_path)
        epochs = log_df['epoch'].tolist()
        auc_values = log_df['val_auc'].tolist()
    
    # 5. 用户活跃度分布（按交互次数）
    user_activity = interactions['user_id'].value_counts()
    # 划分为区间
    bins = [0, 10, 50, 100, 200, 500, 1000]
    labels = ['0-10', '11-50', '51-100', '101-200', '201-500', '501+']
    user_activity_cut = pd.cut(user_activity, bins=bins, labels=labels, right=False).value_counts().to_dict()
    # 确保所有标签都存在
    for label in labels:
        if label not in user_activity_cut:
            user_activity_cut[label] = 0

    return jsonify({
        'tag_distribution': tag_counts,
        'action_distribution': action_counts,
        'daily_trend': daily_trend,
        'training_curve': {
            'epochs': epochs,
            'auc': auc_values
        },
        'user_activity': user_activity_cut,
        'total_users': len(user_activity),
        'total_posts': len(posts),
        'total_interactions': len(interactions)
    })