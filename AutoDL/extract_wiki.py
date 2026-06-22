import bz2
import mwxml
import mwparserfromhell
import json
from pathlib import Path

INPUT_FILE = "zhwiki-latest-pages-articles.xml.bz2"
OUTPUT_DIR = Path("wiki_text_json")
OUTPUT_DIR.mkdir(exist_ok=True)

def clean_wikitext(wikitext):
    """去除维基标记，返回纯文本"""
    try:
        parsed = mwparserfromhell.parse(wikitext)
        return parsed.strip_code().strip()
    except:
        return ""

def process_dump():
    # 使用 bz2.open 解压读取
    with bz2.open(INPUT_FILE, "rb") as f:
        dump = mwxml.Dump.from_file(f)
        for page in dump:
            if page.namespace != 0:
                continue
            for revision in page:
                wikitext = revision.text
                if not wikitext:
                    continue
                plain_text = clean_wikitext(wikitext)
                if len(plain_text) < 50:
                    continue
                record = {
                    "id": page.id,
                    "title": page.title,
                    "text": plain_text
                }
                subdir = OUTPUT_DIR / str(page.id % 100)
                subdir.mkdir(exist_ok=True)
                out_file = subdir / f"{page.id}.json"
                with open(out_file, "w", encoding="utf-8") as out_f:
                    json.dump(record, out_f, ensure_ascii=False)
                print(f"Saved: {page.id} - {page.title}")

if __name__ == "__main__":
    process_dump()