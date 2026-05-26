# Tea Classification — Presentation Draft

---

## Slide 1: Title
- Project: Tea type classification from images
- Presenter: (Your name)

**Speaker notes:** Short intro — goal: build small on-device classifier for multiple tea varieties.

---

## Slide 2: Dataset
- Organized in class folders (e.g., `green_tea`, `matcha_tea`, `oolong_tea`, ...)
- Labels file: `tea_labels.txt`

**Speaker notes:** Mention image counts per class if needed; dataset cleaning steps done in notebooks.

---

## Slide 3: Models & Exports
- Models trained: EfficientNetV2-S, MobileNetV3-small, Hybrid ensemble
- Key artifacts: `tea_model_best.keras`, `tea_model_final.keras`, `efficientnet_v2_s.tflite`, `hybrid_ensemble.tflite`

**Speaker notes:** Briefly explain why these models were chosen (accuracy vs size tradeoff) and mention ensemble idea.

---

## Slide 4: Results & Training State
- Training histories and comparisons in `tea_training_state/` (`history.json`, model comparison CSVs)
- Metrics to show: accuracy, validation accuracy, loss, confusion matrix, and model sizes

**Speaker notes:** Show plots (accuracy/loss curves) and confusion matrix for the best model.

---

## Slide 5: Demo & How to Run
- Quick demo: use `inspect_tflite.py` to inspect or run a TFLite model on sample images.

Example command:
```bash
python inspect_tflite.py --model path/to/tea_model.tflite --image images/sample.jpg
```

**Speaker notes:** One live demo slide showing sample prediction and inference time.

---

## Slide 6: Libraries Used (detected) & Next Steps
- Detected libraries used in repository:
  - `tensorflow`
  - `numpy`
  - `opencv-python` (imported as `cv2`)
  - `Pillow` (imported as `PIL`)
  - `imagehash`
  - `torch` (PyTorch)
  - `transformers` (Hugging Face)
  - `requests`
  - `joblib`
  - standard libs: `os`, `sys`, `time`, `random`, `uuid`
  - `ddgs` (appears imported — may be a local/custom module)

**Next steps:**
- Pick 4–6 key figures (accuracy plot, confusion matrix, sample predictions, model size table).
- I can add the plots into slides and export to PPTX if you want.

---

