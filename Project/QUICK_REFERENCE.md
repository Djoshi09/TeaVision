# 🍵 HYBRID TEA CLASSIFICATION MODEL - QUICK REFERENCE

## Model Overview

```
Hybrid Ensemble Model
├─ Backbone 1: EfficientNetV2S (384×384)
├─ Backbone 2: MobileNetV3Small (224×224)
└─ Output: 18 Tea Classes
```

---

## Notebook Cells Structure

| Cell # | Purpose                          | Output                        |
| ------ | -------------------------------- | ----------------------------- |
| 1      | Environment setup                | TF version, GPU check         |
| 2      | Import dependencies              | Library versions              |
| 3-5    | Data loading & preprocessing     | Dataset splits, class weights |
| 6-8    | Model architecture & training    | Trained model + checkpoints   |
| 9-10   | Model export & state loading     | Keras + TFLite models         |
| 11     | Confusion matrix                 | Raw metrics                   |
| **14** | 📊 **Training curves dashboard** | 4-subplot accuracy/loss/F1    |
| **15** | 📷 **Prediction examples**       | 12 sample predictions         |
| **16** | 🔄 **Confusion analysis**        | Top pairs + heatmap           |
| **17** | 🏗️ **Architecture summary**      | Model structure + params      |
| **18** | 📈 **Dataset statistics**        | Distribution + confidence     |
| **19** | 📋 **Comprehensive report**      | Full metrics table            |
| **20** | ⚠️ **Failure analysis**          | Edge cases + calibration      |

---

## 🎯 Visualization Cells (14-20)

### Cell 14: Training Performance Dashboard

```
[Accuracy Curve] [Loss Curve]
[Per-Class Recall] [Per-Class F1]
```

### Cell 15: Prediction Examples

```
12 tea leaf images in 3×4 grid
Each shows: True Label | Predicted Label | Confidence
```

### Cell 16: Confusion Analysis

```
Top 10 confusion pairs bar chart
Normalized confusion matrix heatmap
```

### Cell 17: Model Architecture

```
Full model summary with parameters
Backbone configuration table
```

### Cell 18: Dataset & Statistics

```
Train/Val distribution
Confidence histogram
Precision-recall scatter
Class support chart
```

### Cell 19: Comprehensive Report

```
Configuration table
Performance metrics table
Model paths and explanation
```

### Cell 20: Failure Cases

```
Confidence distribution comparison
Most misclassified classes
Per-class accuracy breakdown
Error rate calibration curve
```

---

## 🚀 Quick Start

### Train the Model

```python
# Run cells 1-9 in order
# Expected time: 30-60 minutes (GPU recommended)
# Output: Models saved in tea_training_state/hybrid_ensemble/
```

### Analyze Results

```python
# Run cells 10-20
# View all visualizations and metrics
# Identify problematic tea pairs
```

### Use Trained Model

```python
import tensorflow as tf
import numpy as np

model = tf.keras.models.load_model(
    'tea_training_state/hybrid_ensemble/tea_model_best.keras'
)

# Prepare image
img = tf.image.resize(image, (384, 384))
img = tf.cast(img, tf.float32)

# Predict
prediction = model(img[tf.newaxis, ...])
class_idx = np.argmax(prediction[0])
confidence = np.max(prediction[0])

print(f"Tea: {class_names[class_idx]} ({confidence:.2%})")
```

---

## 📊 Expected Metrics

| Metric          | Typical Range |
| --------------- | ------------- |
| Train Accuracy  | 85-95%        |
| Val Accuracy    | 80-90%        |
| Macro Precision | 0.80-0.90     |
| Macro Recall    | 0.80-0.90     |
| Macro F1-Score  | 0.80-0.90     |

---

## 🔧 Training Configuration

```python
HYBRID_SPEC = {
    "name": "hybrid_ensemble",
    "backbones": ["EfficientNetV2S", "MobileNetV3Small"],
    "img_size": (384, 384),
    "batch_size": 8,
    "branch_units": 256,
    "head_units": 256,
    "dropout": 0.3,
    "initial_lr": 1e-3,
    "finetune_lr": 1e-5,
    "epochs_head": 10,
    "epochs_finetune": 5,
    "unfreeze_last": 20,
}
```

---

## 📁 Output Files

```
tea_training_state/hybrid_ensemble/
├── tea_model_best.keras          # ⭐ Best model (use this)
├── tea_model_final.keras         # Final epoch model
├── hybrid_ensemble_export.keras   # Exported format
├── hybrid_ensemble.tflite        # Mobile version
└── analysis_state.joblib         # Training state backup
```

---

## 18 Tea Classes

1. Green Tea
2. Black Tea
3. Oolong Tea
4. Chamomile Tea
5. Peppermint Tea
6. Ginger Tea
7. Hibiscus Tea
8. Rooibos Tea
9. Lavender Tea
10. Matcha Tea
11. Chai Tea
12. Turmeric Tea
13. Rosehip Tea
14. Blueberry Tea
15. Raspberry Tea
16. Kukicha Tea
17. Genmaicha Tea
18. Lemon Tea

---

## 💡 Tips

✅ GPU speeds up training 10-50x vs CPU
✅ Start with lower batch size if out of memory
✅ Check Cell 18 output to find confused tea pairs
✅ Use Cell 20 to identify model weaknesses
✅ Export to TFLite for mobile deployment
✅ Save joblib state for resumable training

---

## ⚠️ Common Issues

| Issue                   | Solution                                             |
| ----------------------- | ---------------------------------------------------- |
| Out of memory           | Reduce batch_size to 4, reduce img_size to (256,256) |
| Slow training           | Use GPU, increase batch_size                         |
| Low accuracy            | Increase epochs, check data quality, reduce dropout  |
| Model not found         | Verify tea_training_state/ folder exists             |
| TFLite conversion fails | Check model is compatible, update TensorFlow         |

---

## 📚 Related Files

- `HYBRID_MODEL_DOCUMENTATION.md` - Full technical documentation
- `IMPLEMENTATION_SUMMARY.md` - Complete implementation details
- `createmodelehybrid.ipynb` - The actual notebook

---

**Last Updated: May 2, 2026**
**Status: ✅ Ready for Training**
