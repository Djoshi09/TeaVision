# ==========================
# ULTIMATE DATASET BUILDER (NO TRAINING)
# ==========================

import os
import uuid
import time
import random
import requests
import cv2
import numpy as np
from ddgs import DDGS
from icrawler.builtin import BingImageCrawler
from PIL import Image
import imagehash
import torch
from transformers import CLIPProcessor, CLIPModel

# ==========================
# CONFIG
# ==========================

DATASET_DIR = r"C:\Users\deept\Fpxnet Dropbox\xafolo xafolo\shareit\course\Deepu\tea project\advanced images"
MAX_PER_QUERY = 25
MAX_PER_TEA_DOWNLOAD = 500
MIN_KEEP_AFTER_CLEAN = 300
MIN_RESOLUTION = 256

device = "cuda" if torch.cuda.is_available() else "cpu"

clip_model = CLIPModel.from_pretrained("openai/clip-vit-base-patch32").to(device)
clip_processor = CLIPProcessor.from_pretrained("openai/clip-vit-base-patch32")

# ==========================
# SAFE REQUEST
# ==========================


def safe_request(url):
    try:
        return requests.get(url, headers={"User-Agent": "Mozilla/5.0"}, timeout=5)
    except:
        return None


# ==========================
# DOWNLOAD
# ==========================


def image_count(folder):
    return len(
        [f for f in os.listdir(folder) if os.path.isfile(os.path.join(folder, f))]
    )


def download_ddgs(query, folder):
    if image_count(folder) >= MAX_PER_TEA_DOWNLOAD:
        return

    for _ in range(3):
        try:
            with DDGS() as ddgs:
                results = ddgs.images(query, max_results=MAX_PER_QUERY)

                for r in results:
                    if image_count(folder) >= MAX_PER_TEA_DOWNLOAD:
                        return

                    try:
                        res = safe_request(r["image"])
                        if res and res.status_code == 200:
                            path = os.path.join(folder, f"{uuid.uuid4()}.jpg")
                            with open(path, "wb") as f:
                                f.write(res.content)
                    except:
                        continue

            time.sleep(random.uniform(2, 5))  # ✅ outside inner try

        except Exception as e:
            print("DDGS error:", e)
            time.sleep(5)


def download_bing(query, folder):
    remaining = MAX_PER_TEA_DOWNLOAD - image_count(folder)
    if remaining <= 0:
        return

    crawler = BingImageCrawler(storage={"root_dir": folder})
    crawler.crawl(keyword=query, max_num=min(MAX_PER_QUERY, remaining))


# ==========================
# QUALITY FILTERS
# ==========================


def is_blurry(img):
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    return cv2.Laplacian(gray, cv2.CV_64F).var() < 40


def has_good_lighting(img):
    brightness = img.mean()
    return 30 < brightness < 230


def remove_text_heavy(img):
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    edges = cv2.Canny(gray, 50, 150)
    return edges.mean() < 85


def color_filter(img, tea_class):
    avg = img.mean(axis=(0, 1))  # BGR
    b, g, r = avg

    if tea_class == "green tea":
        return g >= r * 0.9 and g >= b * 0.9

    if tea_class in {"masala chai", "chai tea"}:
        return r > g

    return True


# ==========================
# CLIP FILTERS
# ==========================


def clip_predict(image, labels):
    inputs = clip_processor(
        text=labels, images=image, return_tensors="pt", padding=True
    ).to(device)
    outputs = clip_model(**inputs)
    return outputs.logits_per_image.softmax(dim=1)[0]


def is_real(image):
    labels = ["real photo", "cartoon", "illustration", "ai generated"]
    probs = clip_predict(image, labels)
    return probs[0] > 0.45


def is_tea(image):
    labels = ["tea in cup", "chai", "cup of tea", "no tea", "person", "street"]
    probs = clip_predict(image, labels)
    return sum(probs[:3]) > sum(probs[3:]) - 0.05


def is_correct(image, tea_class):
    labels = [f"{tea_class} tea", "green tea", "black tea", "milk tea"]
    probs = clip_predict(image, labels)

    return probs[0] > max(probs[1:]) - 0.05


# ==========================
# DUPLICATES
# ==========================


def remove_duplicates(folder, min_keep=0):
    hashes = []
    for file in os.listdir(folder):
        if min_keep and image_count(folder) <= min_keep:
            break

        path = os.path.join(folder, file)
        try:
            img = Image.open(path)
            h = imagehash.phash(img)

            for existing in hashes:
                if abs(h - existing) < 5:
                    if not min_keep or image_count(folder) > min_keep:
                        os.remove(path)
                    break
            else:
                hashes.append(h)
        except:
            if not min_keep or image_count(folder) > min_keep:
                os.remove(path)


# ==========================
# MAIN FILTER PIPELINE
# ==========================


def clean_folder(folder, tea_class):
    if image_count(folder) <= MIN_KEEP_AFTER_CLEAN:
        print(f"ℹ️ {tea_class}: skip clean to keep >= {MIN_KEEP_AFTER_CLEAN} images")
        return

    for file in os.listdir(folder):
        if image_count(folder) <= MIN_KEEP_AFTER_CLEAN:
            break

        path = os.path.join(folder, file)

        try:
            img = cv2.imread(path)

            if img is None:
                os.remove(path)
                continue

            h, w, _ = img.shape
            if h < MIN_RESOLUTION or w < MIN_RESOLUTION:
                os.remove(path)
                continue

            if is_blurry(img):
                os.remove(path)
                continue

            if not has_good_lighting(img):
                os.remove(path)
                continue

            if not remove_text_heavy(img):
                os.remove(path)
                continue

            if not color_filter(img, tea_class):
                os.remove(path)
                continue

            pil_img = Image.fromarray(cv2.cvtColor(img, cv2.COLOR_BGR2RGB))

            if not is_real(pil_img):
                os.remove(path)
                continue

            if not is_tea(pil_img):
                os.remove(path)
                continue

            if not is_correct(pil_img, tea_class):
                os.remove(path)
                continue

        except:
            os.remove(path)

    remove_duplicates(folder, min_keep=MIN_KEEP_AFTER_CLEAN)


# ==========================
# QUERY SET
# ==========================


def get_queries(tea):
    return [
        f"{tea} different tea cup",
        f"{tea} close up tea",
        f"{tea} top view tea",
        f"{tea} tea bags",
        f"{tea} tea leaves",
        f"{tea} tea different lightening",
        f"{tea} tea on table",
        f"{tea} asthetic",
        f"{tea} tea different angles",
    ]


# ==========================
# BUILD DATASET
# ==========================


def build_dataset(tea):
    print(f"\n🚀 {tea}")

    folder = os.path.join(DATASET_DIR, tea.replace(" ", "_"))
    os.makedirs(folder, exist_ok=True)

    for q in get_queries(tea):
        if image_count(folder) >= MAX_PER_TEA_DOWNLOAD:
            break

        print("🔍", q)
        download_ddgs(q, folder)
        download_bing(q, folder)

    clean_folder(folder, tea)

    print(f"✅ {tea}: {len(os.listdir(folder))} images")


# ==========================
# RUN
# ==========================

if __name__ == "__main__":

    teas = [
        "green tea",
        "black tea",
        "oolong tea",
        "chamomile tea",
        "peppermint tea",
        "ginger tea",
        "hibiscus tea",
        "rooibos tea",
        "lavender tea",
        "matcha tea",
        "chai tea",
        "turmeric tea",
        "rosehip tea",
        "blueberry tea",
        "raspberry tea",
        "kukicha tea",
        "genmaicha tea",
        "lemon tea",
    ]

    for t in teas:
        build_dataset(t)

    print("\n🎯 DATASET READY (BEST QUALITY)")
