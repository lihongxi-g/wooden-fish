#!/usr/bin/env python3
"""Doki 字体子集化：从项目 Kotlin 源码提取用到的字符，裁剪开源楷书字体。
楷书（霞鹜文楷 LXGW WenKai，OFL 许可）→ res/font/kaiti.ttf
字体来源：
  https://github.com/lxgw/LxgwWenKai/releases/download/v1.520/LXGWWenKai-Regular.ttf
重新生成：python3 scripts/subset_fonts.py
"""
import os, re, sys
from fontTools import subset

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "app", "src", "main", "java")
OUT = os.path.join(ROOT, "app", "src", "main", "res", "font")

def collect_chars():
    chars = set()
    # 常用标点
    chars.update("，。！？；：、·…—《》〈〉「」『』（）【】\"'.,!?;:()[]-–—'’“”")
    # 提取所有 .kt 源码中的字符（CJK + ASCII 可打印）
    for dirpath, _, files in os.walk(SRC):
        for f in files:
            if f.endswith(".kt"):
                text = open(os.path.join(dirpath, f), encoding="utf-8").read()
                for ch in text:
                    o = ord(ch)
                    if 0x4E00 <= o <= 0x9FFF or 0x3400 <= o <= 0x4DBF or 0x20 <= o <= 0x7E:
                        chars.add(ch)
    return "".join(sorted(chars))

def run(font_in, font_out, text):
    opts = subset.Options()
    opts.flavor = None
    opts.retain_gids = False
    opts.notdef_outline = True
    opts.recommended_glyphs = False
    opts.name_IDs = ["*"]
    opts.name_legacy = True
    opts.layout_features = ["*"]
    opts.hinting = False
    opts.drop_tables = ["FFTM", "TSI0", "TSI1", "TSI2", "TSI3", "TSI5", "Gasp", "DSIG", "LTSH", "hdmx", "VDMX", "prep"]
    s = subset.Subsetter(opts)
    fp = fontTools.ttLib.TTFont(font_in)
    s.populate(text=text)
    s.subset(fp)
    os.makedirs(OUT, exist_ok=True)
    fp.save(os.path.join(OUT, font_out))
    print(f"{font_out}: {os.path.getsize(os.path.join(OUT, font_out)) // 1024} KB")

import fontTools.ttLib
if __name__ == "__main__":
    chars = collect_chars()
    print(f"字符集: {len(chars)} 字符")
    run("/tmp/fonts/LXGWWenKai.ttf", "kaiti.ttf", chars)
    # 校验关键字符
    from fontTools.ttLib import TTFont
    cmap = set(TTFont(os.path.join(OUT, "kaiti.ttf")).getBestCmap())
    missing = [c for c in "上上籤雲龍風馬門見上下中Great Fortune" if ord(c) not in cmap]
    print(f"kaiti.ttf 缺失字符: {missing if missing else '无'}")
