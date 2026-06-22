# admin.py
from flask import Blueprint, render_template, jsonify
import pandas as pd
import numpy as np
from config import Config
import pickle
import os
import json

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
    """返回统计缓存数据（每小时更新一次）"""
    cache_path = os.path.join(cfg.DATA_DIR, 'stats_cache.json')
    if os.path.exists(cache_path):
        with open(cache_path, 'r', encoding='utf-8') as f:
            data = json.load(f)
        return jsonify(data)
    else:
        # 缓存不存在时返回空数据（首次运行时会由 cron 脚本生成）
        return jsonify({
            'tag_distribution': {},
            'action_distribution': {},
            'daily_trend': {},
            'user_activity': {},
            'total_users': 0,
            'total_posts': 0,
            'total_interactions': 0
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