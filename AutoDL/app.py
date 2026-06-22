from flask import Flask, request, jsonify
from explain import explain_recommendation
import pickle
from config import Config
from admin import bp as admin_bp  # 导入管理员后台蓝图

app = Flask(__name__)
cfg = Config()

# 注册管理员后台路由（/admin）
app.register_blueprint(admin_bp)

# 加载映射（用于校验用户）
with open(cfg.MAPPINGS_PKL, "rb") as f:
    mappings = pickle.load(f)
user_encoder = mappings["user_encoder"]

@app.route('/recommend', methods=['POST'])
def recommend():
    """推荐接口"""
    data = request.get_json()
    user_id = data.get('user_id')
    top_k = data.get('top_k', 5)
    if user_id not in user_encoder.classes_:
        return jsonify({"error": "user not found"}), 404
    recs = explain_recommendation(user_id, top_k=top_k)
    return jsonify(recs)

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)