# models/char_tokenizer.py
class CharTokenizer:
    """简单的字符级分词器，将每个字符映射为整数索引"""
    def __init__(self, chars=None):
        if chars is None:
            # 基本中文字符范围：CJK统一表意文字（4E00-9FFF）
            self.char_list = [chr(i) for i in range(0x4E00, 0x9FFF+1)]
            # 添加常见标点、数字、英文字母
            self.char_list += list("，。！？；：“”‘’、—…·abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789")
        else:
            self.char_list = chars
        self.vocab = {ch: idx+1 for idx, ch in enumerate(self.char_list)}  # 0 留给 padding
        self.vocab['[PAD]'] = 0
        self.vocab_size = len(self.vocab)

    def encode(self, text, max_len=None):
        """将文本转换为 token ids，可选截断"""
        ids = [self.vocab.get(ch, 0) for ch in text]  # 未知字符映射为 0
        if max_len:
            ids = ids[:max_len]
        return ids

    def batch_encode(self, texts, max_len, padding=True):
        """批量编码，自动 padding"""
        batch_ids = [self.encode(t, max_len) for t in texts]
        if padding:
            max_len_actual = max(len(ids) for ids in batch_ids)
            if max_len:
                max_len_actual = max_len
            padded = []
            for ids in batch_ids:
                padded.append(ids + [0] * (max_len_actual - len(ids)))
            return padded
        return batch_ids

    def decode(self, ids):
        return ''.join([self.char_list[id-1] if id>0 else '' for id in ids])