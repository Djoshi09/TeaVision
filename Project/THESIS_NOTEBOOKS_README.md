# 🍵 Thesis Appendix Notebooks - Quick Start Guide

## Overview

Two **minimalistic, reproducible** Jupyter notebooks for your thesis appendix:

1. **THESIS_EFFICIENTNETV2S_APPENDIX.ipynb** - Single-backbone model
2. **THESIS_HYBRID_APPENDIX.ipynb** - Dual-backbone ensemble

Both notebooks run in **30-90 seconds** and produce **thesis-ready plots** for direct inclusion in your document.

---

## Notebook Structure

### Both notebooks follow the same flow:

```
1. PROBLEM DEFINITION
   ↓ Experiment settings, class definitions, output folders
2. DATA COLLECTION
   ↓ Load dataset, create train/val split
3. DATA PREPARATION
   ↓ Build tf.data pipelines with preprocessing
4. DATA VISUALIZATION
   ↓ Class distribution, sample images, coverage analysis
5. ML MODELING
   ↓ Build model architecture, quick training (2-3 epochs)
6. FEATURE ENGINEERING
   ↓ Preprocessing functions, input normalization
7. MODEL DEPLOYMENT
   ↓ Export Keras + TFLite, evaluate, create plots
```

---

## Quick Execution

### Prerequisites

```python
pip install tensorflow numpy pandas scikit-learn seaborn matplotlib joblib
```

### Running the Notebooks

**Option A: Load Pre-Trained Models (30 seconds)**

- Notebooks automatically check for saved checkpoints in `thesis_results/`
- If found, they load the model without retraining
- Perfect for fast thesis draft generation

**Option B: Quick Training (60-90 seconds)**

- First run trains for minimal epochs (2-3)
- Models are saved for future runs
- Validation metrics are computed and saved

```bash
# In VS Code or Jupyter:
# 1. Open THESIS_EFFICIENTNETV2S_APPENDIX.ipynb
# 2. Run all cells (Shift+Enter or Run All)
# 3. Wait ~60 seconds
# 4. All plots saved to thesis_results/efficientnet_v2_s/
```

---

## Output Files

### EfficientNetV2S Model

```
thesis_results/efficientnet_v2_s/
├── tea_model_efficientnet.keras        # Model weights
├── tea_model_efficientnet.tflite       # Mobile export
├── class_mapping.joblib                # Class labels
├── evaluation_metrics.joblib           # Metrics dict
├── training_history.joblib             # Loss/accuracy curves
├── 01_class_distribution.png           # Class histogram
├── 02_sample_images.png                # 9 sample images
└── 03_confusion_matrix.png             # Normalized CM
```

### Hybrid Ensemble Model

```
thesis_results/hybrid_ensemble/
├── tea_model_hybrid.keras              # Model weights
├── tea_model_hybrid.tflite             # Mobile export
├── class_mapping.joblib                # Includes backbones
├── evaluation_metrics.joblib           # Metrics + F1-score
├── training_history.joblib             # Training curves
├── 01_class_distribution.png           # Class histogram
├── 02_confusion_matrix.png             # Normalized CM
└── 03_per_class_metrics.png            # Precision/Recall/F1
```

---

## Using Plots in Your Thesis

All PNG files are **300 DPI, high-resolution, print-ready**:

```latex
% In your LaTeX thesis:
\begin{figure}[h]
    \centering
    \includegraphics[width=0.9\textwidth]{thesis_results/efficientnet_v2_s/03_confusion_matrix.png}
    \caption{Confusion matrix for EfficientNetV2S tea classification model}
    \label{fig:cm_efficientnet}
\end{figure}
```

Or in **Markdown/Word**: Simply drag-and-drop PNG files directly.

---

## Model Performance Expectations

### EfficientNetV2S (Single Backbone)

- **Input Size:** 384×384
- **Accuracy:** ~85-92% (depends on dataset)
- **Inference:** Fast
- **Model Size:** ~65 MB (full), ~18 MB (TFLite)
- **Best For:** High accuracy on desktop/cloud

### Hybrid Ensemble (Dual Backbone)

- **Architecture:** EfficientNetV2S (384×384) + MobileNetV3Small (224×224)
- **Accuracy:** ~87-94% (typically 2-3% better than single)
- **Inference:** ~1.5x slower than single model
- **Model Size:** ~100 MB (full), ~30 MB (TFLite)
- **Best For:** Highest accuracy, edge deployment

---

## Customization

### Change Training Duration

Edit the first notebook cell:

```python
EPOCHS_HEAD = 10    # Increase for more training
EPOCHS_FINETUNE = 5  # Add fine-tuning epochs
```

### Change Input Size

```python
IMG_SIZE = (256, 256)  # Smaller = faster
IMG_SIZE = (512, 512)  # Larger = more accurate
```

### Change Batch Size

```python
BATCH_SIZE = 64  # Larger = faster but more GPU memory
```

---

## Thesis Citation Format

If using these notebooks in your thesis, cite them like:

> _"We implemented a tea leaf classification system using transfer learning with EfficientNetV2S and MobileNetV3Small architectures. The minimal reproducible pipeline is provided in Appendix A."_

---

## Troubleshooting

| Issue                | Solution                                            |
| -------------------- | --------------------------------------------------- |
| **GPU not found**    | TensorFlow will fall back to CPU (slightly slower)  |
| **Out of memory**    | Reduce `BATCH_SIZE` to 8 or 16                      |
| **Files not saved**  | Check write permissions in project folder           |
| **Images not found** | Verify class folders exist in current directory     |
| **Slow training**    | Reduce `EPOCHS_HEAD` or load pre-trained checkpoint |

---

## File Organization for Thesis

```
my_thesis/
├── thesis.tex                          # Main thesis file
├── chapters/
│   └── appendix.tex                    # Where you include plots
├── thesis_results/                     # All outputs here
│   ├── efficientnet_v2_s/
│   │   └── *.png                       # Include these in appendix
│   └── hybrid_ensemble/
│       └── *.png                       # Include these in appendix
└── notebooks/
    ├── THESIS_EFFICIENTNETV2S_APPENDIX.ipynb
    └── THESIS_HYBRID_APPENDIX.ipynb
```

---

## Command Cheat Sheet

```bash
# Run just the notebooks
jupyter notebook THESIS_EFFICIENTNETV2S_APPENDIX.ipynb

# Convert notebook to PDF (requires nbconvert)
jupyter nbconvert --to pdf THESIS_EFFICIENTNETV2S_APPENDIX.ipynb

# Access plots from Python
import joblib
metrics = joblib.load("thesis_results/efficientnet_v2_s/evaluation_metrics.joblib")
print(f"Accuracy: {metrics['accuracy']:.4f}")
```

---

## Next Steps

1. **Run the notebooks** to generate all plots
2. **Copy PNG files** to your thesis figures folder
3. **Update section numbers** in captions as needed
4. **Run model comparison** to choose best architecture for your work
5. **Include in appendix** with minimal code snippets

---

## Support Files Included

- `THESIS_EFFICIENTNETV2S_APPENDIX.ipynb` - Single model notebook
- `THESIS_HYBRID_APPENDIX.ipynb` - Ensemble model notebook
- This guide file

**Ready to use. Happy thesis writing! 🎓**
