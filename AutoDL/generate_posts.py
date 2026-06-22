import os
import pandas as pd
import random
from datetime import datetime, timedelta
from tqdm import tqdm
import sys
sys.path.append('..')
from config import Config
from utils import call_deepseek, parse_json_response

cfg = Config()

def random_publish_time():
    """随机生成2026年1月1日至2026年3月31日之间的时间"""
    start = datetime(2026, 1, 1)
    end = datetime(2026, 3, 31)
    delta = (end - start).days
    random_days = random.randint(0, delta)
    random_hours = random.randint(0, 23)
    random_minutes = random.randint(0, 59)
    random_seconds = random.randint(0, 59)
    result = start + timedelta(days=random_days, hours=random_hours, minutes=random_minutes, seconds=random_seconds)
    return result.strftime("%Y-%m-%d %H:%M:%S")

def default_post(post_id):
    """返回一个默认帖子，用于API失败时"""
    return {
        "post_id": post_id,
        "title": f"校园生活分享{post_id}",
        "content": "今天在校园里发现了一处美丽的小角落，分享给大家看看。",
        "tags": "校园生活",
        "publish_time": random_publish_time()
    }

# 主题池
TOPICS = [
    "学习求助", "校园活动", "失物招领", "二手交易", "校园风景",
    "吐槽树洞", "干货分享", "组队找搭子", "情感交流", "社团招新",
    "考研交流", "求职实习", "竞赛组队", "宿舍生活", "食堂美食",
    "选课攻略", "讲座分享", "表白交友", "运动健身", "校园新闻",
    "外卖推荐", "交通出行", "防骗指南", "期末备考", "奖学金"
]

def generate_one_post(post_id):
    topic = random.choice(TOPICS)
    
    # 角色设定（可以用系统消息）
    system_prompt = "你是一个活跃在校园论坛的大学生，喜欢分享真实经历和感受。"
    
    # 添加few-shot示例（可选）
    example = """
例如，一篇好的帖子：
标题：食堂新出的麻辣香锅也太好吃了吧！
正文：今天中午去食堂发现新开了麻辣香锅窗口，我点了一份中辣的，加了肥牛、金针菇和藕片，没想到这么入味！辣度刚刚好，吃得我满头大汗，太爽了。强烈推荐大家去尝尝，位置在一楼最里面。
标签：美食,校园生活,推荐
"""
    prompt = f"""
请生成一篇校园论坛帖子，主题是“{topic}”。

要求：
- 标题要吸引人，能引起同学共鸣
- 正文要具体、真实，包含细节（时间、地点、人物、感受等），不少于100字
- 语言自然，像真实学生发的帖子，可以带一点口语化
- 标签用逗号分隔，从候选池中选择2-3个：学习、活动、求助、失物、二手、风景、吐槽、干货、组队、情感、美食、校园生活、社团、考研、求职、实习、竞赛、宿舍、选课、讲座、表白、运动、新闻、外卖、交通、防骗、期末、奖学金
- 发布时间在2026年1月1日至2026年3月31日之间

请直接返回JSON格式，不要包含其他说明：
{{
    "title": "...",
    "content": "...",
    "tags": "tag1,tag2",
    "publish_time": "2026-03-15 14:30:00"
}}

{example}
"""
    response = call_deepseek(prompt, max_tokens=1000, temperature=0.9, system_prompt=system_prompt, retries=3)
    if response:
        data = parse_json_response(response)
        if data and all(k in data for k in ("title", "content", "tags")):
            # 验证时间格式
            try:
                datetime.strptime(data["publish_time"], "%Y-%m-%d %H:%M:%S")
            except:
                data["publish_time"] = random_publish_time()
            return {
                "post_id": post_id,
                "title": data["title"],
                "content": data["content"],
                "tags": data["tags"],
                "publish_time": data["publish_time"]
            }
    # 失败时返回默认内容（也可从模板库随机取）
    return default_post(post_id)

def generate_posts(num_posts=5000, output_file=None):
    if output_file is None:
        output_file = cfg.POSTS_CSV   # 默认配置路径
    posts = []
    # 使用 tqdm 显示进度
    for i in tqdm(range(1, num_posts+1), desc="生成帖子"):
        posts.append(generate_one_post(i))
        # 修改休眠策略：每 10 篇休眠一次，随机 0.8~1.5 秒，降低并发压力
        if i % 10 == 0:
            import time
            time.sleep(random.uniform(0.8, 1.5))
    df = pd.DataFrame(posts)
    df.to_csv(output_file, index=False)
    print(f"生成{num_posts}篇帖子，保存至{output_file}")

if __name__ == "__main__":
    # 使用示例：生成 6000 篇训练帖子
    generate_posts(6000, "data/posts_train.csv")
    # 若要生成微调帖子，改为 generate_posts(5000, "data/posts_finetune.csv")