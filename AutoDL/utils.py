import os
import requests
import time
import json
from dotenv import load_dotenv

load_dotenv()  # 加载 .env 文件中的 DEEPSEEK_API_KEY

DEEPSEEK_API_KEY = os.getenv("DEEPSEEK_API_KEY")
API_URL = "https://api.deepseek.com/v1/chat/completions"  # 根据实际API调整

def call_deepseek(prompt, max_tokens=500, temperature=0.8, top_p=0.9, system_prompt=None, retries=3):
    headers = {
        "Authorization": f"Bearer {DEEPSEEK_API_KEY}",
        "Content-Type": "application/json"
    }
    messages = []
    if system_prompt:
        messages.append({"role": "system", "content": system_prompt})
    messages.append({"role": "user", "content": prompt})

    data = {
        "model": "deepseek-chat",
        "messages": messages,
        "max_tokens": max_tokens,
        "temperature": temperature,
        "top_p": top_p,
    }

    for attempt in range(retries):
        try:
            response = requests.post(API_URL, headers=headers, json=data, timeout=30)
            response.raise_for_status()
            result = response.json()
            return result["choices"][0]["message"]["content"].strip()
        except Exception as e:
            print(f"API调用失败 (尝试 {attempt+1}/{retries}): {e}")
            time.sleep(2 ** attempt)  # 指数退避
    return None

def parse_json_response(text):
    """尝试从LLM返回的文本中提取JSON"""
    try:
        # 尝试直接解析
        return json.loads(text)
    except:
        # 尝试查找json块
        import re
        json_match = re.search(r'\{.*\}', text, re.DOTALL)
        if json_match:
            try:
                return json.loads(json_match.group())
            except:
                pass
        return None