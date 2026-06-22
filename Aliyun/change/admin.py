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
    posts = pd.read_csv(cfg.POSTS_CSV)
    interactions = pd.read_csv(cfg.INTERACTIONS_CSV)
    with open(cfg.MAPPINGS_PKL, 'rb') as f:
        mappings = pickle.load(f)
    return posts, interactions, mappings

@bp.route('/')
def dashboard():
    return render_template('admin.html')

@bp.route('/stats')
def stats():
    posts, interactions, mappings = load_data()

    tags_series = posts['tags'].str.split(',').explode()
    tag_counts = tags_series.value_counts().head(10).to_dict()
    action_counts = interactions['action'].value_counts().to_dict()
    interactions['date'] = pd.to_datetime(interactions['timestamp']).dt.date
    daily_trend = interactions.groupby('date').size().to_dict()
    daily_trend = {str(k): v for k, v in daily_trend.items()}
    user_activity = interactions['user_id'].value_counts()
    bins = [0, 10, 50, 100, 200, 500, 1000]
    labels = ['0-10', '11-50', '51-100', '101-200', '201-500', '501+']
    user_activity_cut = pd.cut(user_activity, bins=bins, labels=labels, right=False).value_counts().to_dict()
    for label in labels:
        if label not in user_activity_cut:
            user_activity_cut[label] = 0

    return jsonify({
        'tag_distribution': tag_counts,
        'action_distribution': action_counts,
        'daily_trend': daily_trend,
        'user_activity': user_activity_cut,
        'total_users': len(user_activity),
        'total_posts': len(posts),
        'total_interactions': len(interactions)
    })

@bp.route('/training-log')
def training_log():
    """返回最后一次训练的单条曲线（epoch 0,10,...,90）"""
    log_path = os.path.join(cfg.MODEL_DIR, 'training_log.csv')
    if not os.path.exists(log_path):
        return jsonify({'epochs': [], 'aucs': [], 'final_auc': 0})

    df = pd.read_csv(log_path)
    # 取最后10行（最后一次训练，epoch 0,10,...,90）
    last_train = df.tail(10).sort_values('epoch')
    epochs = last_train['epoch'].tolist()
    aucs = last_train['val_auc'].tolist()
    final_auc = aucs[-1] if aucs else 0
    return jsonify({
        'epochs': epochs,
        'aucs': aucs,
        'final_auc': final_auc
    })