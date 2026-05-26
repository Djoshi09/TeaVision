# Hybrid Tea Classification Model - Documentation

## Overview

A hybrid deep learning model combining **EfficientNetV2S** and **MobileNetV3Small** backbones for classifying 18 different types of tea. The model uses a dual-branch architecture with feature fusion for improved accuracy and robustness.

---

## Model Architecture

### Dual-Branch Design

```
Input (384×384×3)
    ↓
Data Augmentation (Flip, Rotation, Zoom, Contrast)
    ├─────────────────┬─────────────────┐
    ↓                 ↓
Branch 1:          Branch 2:
EfficientNetV2S    MobileNetV3Small
(384×384)          (224×224)
    ↓                 ↓
Global Avg Pool    Global Avg Pool
    ↓                 ↓
Dense 256 ReLU     Dense 256 ReLU
    ↓                 ↓
Dropout 0.3        Dropout 0.3
    └─────────────────┴─────────────────┘
            ↓
      Concatenate (512 dims)
            ↓
      Dense 256 ReLU
            ↓
      Dropout 0.3
            ↓
      Dense 18 (Softmax) → Tea Class
```

### Key Components

- **Backbones**: Pre-trained on ImageNet, frozen initially
- **Branch Units**: 256 neurons per branch projection
- **Fusion Head**: 256 neurons in final dense layer
- **Classes**: 18 tea types
- **Total Parameters**: ~29.5M (trainable initially ≈ 1.2M)

---

## Training Configuration

### Hyperparameters

| Parameter               | Value                   |
| ----------------------- | ----------------------- |
| Batch Size              | 8                       |
| Initial Learning Rate   | 1e-3                    |
| Fine-tune Learning Rate | 1e-5                    |
| Epochs (Head Training)  | 10                      |
| Epochs (Fine-tuning)    | 5                       |
| Dropout Rate            | 0.3                     |
| Unfreeze Layers         | Last 20 backbone layers |

### Data Augmentation

- Random Horizontal Flip (50%)
- Random Rotation (±12°)
- Random Zoom (±15%)
- Random Contrast (±20%)

### Callbacks

- **Early Stopping**: Patience=4 (monitor val_accuracy)
- **ReduceLROnPlateau**: Factor=0.5, Patience=2, Min LR=1e-6
- **ModelCheckpoint**: Save best model by val_accuracy

### Class Balancing

Weighted loss based on inverse class frequency to handle imbalanced training data.

---

## Dataset

### Tea Classes (18 total)

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

### Train/Validation Split

- **Test Size**: 20%
- **Stratified**: Ensures class representation in both sets
- **Seed**: 123 (reproducibility)

---

## Notebook Cells & Visualizations

### Cell 1: Environment Setup

- TensorFlow/NumPy versions
- GPU configuration check
- Mixed precision settings (if GPU available)

### Cell 2: Imports & Dependencies

- Core ML libraries: TensorFlow, scikit-learn
- Data handling: Pandas, NumPy
- Visualization: Matplotlib, Seaborn
- Utilities: Joblib, Pathlib

### Cell 3: Dataset Loading

- Load all tea class folders
- Create stratified train/val split
- Validation coverage check per class

### Cell 4: Data Processing

- Image loading and resizing
- Normalization (float32)
- Dataset batching and prefetching
- Per-class validation coverage

### Cell 5: Class Weights

- Calculate class imbalance weights
- Store for weighted loss computation

### Cell 6: Model Architecture

- `build_backbone()`: Load pre-trained models
- `make_dataset()`: Create tf.data pipelines
- `build_hybrid_model()`: Assemble dual-branch architecture
- `train_hybrid_model()`: 2-phase training (frozen→fine-tune)

### Cell 7: Model Configuration

- Define `HYBRID_SPEC` with all hyperparameters
- Backbone sizes, learning rates, epochs

### Cell 8: Training Execution

- Train hybrid model (phases 1 & 2)
- Save best and final models
- Persist state via joblib for resumable training

### Cell 9: State Persistence

- Verify models saved successfully
- Display checkpoint paths

### Cell 10: State Restoration

- Load saved training state from joblib
- Rebuild datasets from persisted paths
- Load best or final model for analysis

### Cell 11: Training Summary

- Accuracy/Loss curves with matplotlib
- Top confusions analysis
- Per-class performance metrics

### Cell 12: Model Export

- Save Keras model in export format

### Cell 13: TFLite Conversion

- Quantize for mobile/embedded deployment
- Save `.tflite` model file

### **VISUALIZATION CELL 14: Detailed Training Curves** ⭐

Shows 4 subplots:

1. **Train vs Val Accuracy** - with phase transition line
2. **Train vs Val Loss** - normalized plots
3. **Per-Class Recall** - sorted with values
4. **Per-Class F1-Score** - color-coded (green: >0.8, orange: 0.6-0.8, red: <0.6)

### **VISUALIZATION CELL 15: Prediction Examples** ⭐

- 12 random validation samples (3×4 grid)
- True label, Predicted label, Confidence score
- Green title = correct, Red title = incorrect
- Actual image display

### **VISUALIZATION CELL 16: Confusion Analysis** ⭐

- Top 10 confusion pairs bar chart (directed: true→predicted)
- Normalized confusion matrix heatmap
- Color intensity shows recall per class

### **VISUALIZATION CELL 17: Model Architecture Summary** ⭐

- Model summary with nested layer expansion
- Parameter counts: Total, Trainable, Non-trainable
- Backbone configuration table
- Model name and class count

### **VISUALIZATION CELL 18: Dataset & Prediction Analysis** ⭐

- **Train/Val Distribution**: Class balance in both sets
- **Confidence Distribution**: Histogram with mean line
- **Precision-Recall Scatter**: Per-class trade-off analysis
- **Class Support**: Validation samples per class

### **VISUALIZATION CELL 19: Comprehensive Report** ⭐

- Model configuration table (architecture, backbones, resolution, parameters)
- Performance metrics table (accuracy, loss, precision, recall, F1-score)
- Saved model paths
- Hybrid model explanation and benefits

### **VISUALIZATION CELL 20: Failure Case Analysis** ⭐

- Correct predictions with low confidence (edge cases)
- Incorrect predictions with high confidence (overconfident)
- Confidence distribution comparison (correct vs incorrect)
- Most frequently misclassified classes
- Per-class accuracy with color coding
- Error rate by confidence bin (calibration curve)

---

## Performance Metrics

### Validation Set Evaluation

All metrics computed on held-out validation set with confusion matrix:

| Metric        | Description                              |
| ------------- | ---------------------------------------- |
| **Accuracy**  | Correct predictions / Total samples      |
| **Precision** | TP / (TP + FP) per class, macro averaged |
| **Recall**    | TP / (TP + FN) per class, macro averaged |
| **F1-Score**  | 2×(Precision×Recall)/(Precision+Recall)  |

### Per-Class Metrics

- Individual recall, precision, F1 for each tea type
- Identifies best/worst performing classes
- Support count (number of validation samples per class)

---

## Model Outputs & Exports

### Saved Models

```
tea_training_state/hybrid_ensemble/
├── tea_model_best.keras          # Best model by val_accuracy
├── tea_model_final.keras         # Final model after all epochs
├── hybrid_ensemble_export.keras   # Exported Keras model
├── hybrid_ensemble.tflite        # Quantized TFLite model
└── analysis_state.joblib         # Resumable training state
```

### TFLite Model

- **Format**: FlatBuffers (`.tflite`)
- **Optimization**: Default quantization
- **Size**: Significantly reduced (suitable for mobile)
- **Inference**: Optimized for edge devices

### Joblib State

Persists for training resumption:

- Class names and indices
- Dataset paths and labels
- Training history (accuracy, loss)
- Fine-tuning history
- Model configuration
- Learning rates and epochs

---

## Hybrid Model Advantages

### Why Two Backbones?

1. **EfficientNetV2S (384×384)**
   - Higher resolution input
   - Excellent accuracy-to-efficiency trade-off
   - Better for fine-grained tea classification

2. **MobileNetV3Small (224×224)**
   - Efficient architecture (fewer parameters)
   - Fast inference
   - Captures complementary low-resolution features

### Ensemble Benefits

- **Complementary representations**: Different input resolutions capture different patterns
- **Robustness**: Individual backbone weaknesses mitigated by the other
- **Generalization**: Fusion reduces overfitting
- **Multi-scale features**: Leverages both detailed and global context

---

## Usage Guide

### Running the Notebook

1. **Environment Setup**
   - Run Cell 1: Check TensorFlow version and GPU availability
   - Run Cell 2: Load all dependencies

2. **Data Preparation**
   - Run Cell 3: Load and verify dataset
   - Run Cell 4: Create training/validation splits
   - Run Cell 5: Calculate class weights

3. **Model Training**
   - Run Cell 6-7: Define architecture and config
   - Run Cell 8: Train hybrid model (takes ~30-60 minutes depending on GPU)
   - Run Cell 9: Verify saved models

4. **Analysis & Visualization**
   - Run Cell 10: Restore training state
   - Run Cell 11-20: Generate comprehensive visualizations
   - Review all plots and metrics

### Inference on New Images

```python
# Load trained model
model = tf.keras.models.load_model('tea_training_state/hybrid_ensemble/tea_model_best.keras')

# Prepare image
img = tf.image.resize(image, (384, 384))
img = tf.cast(img, tf.float32)
img = img[tf.newaxis, ...]  # Add batch dimension

# Predict
prediction = model.predict(img)
class_idx = np.argmax(prediction[0])
confidence = np.max(prediction[0])
tea_type = class_names[class_idx]

print(f"Predicted: {tea_type} ({confidence:.2%} confidence)")
```

---

## Troubleshooting

### Memory Issues

- Reduce batch size from 8 to 4
- Reduce image size from (384, 384) to (256, 256)
- Use gradient checkpointing if available

### Training Too Slow

- Use GPU (CUDA/cuDNN)
- Increase batch size if VRAM allows
- Reduce number of augmentations

### Poor Validation Accuracy

- Check class imbalance (run Cell 11)
- Increase fine-tuning epochs
- Reduce dropout if underfitting
- Verify data preprocessing in Cell 4

### Model Path Not Found

- Ensure notebooks are in correct directory
- Check `tea_training_state/` folder exists
- Verify model saved successfully (check Cell 9 output)

---

## Key References

- **EfficientNetV2**: Tan et al., "EfficientNetV2: Smaller Models and Faster Training" (ICML 2021)
- **MobileNetV3**: Howard et al., "Searching for MobileNetV3" (ICCV 2019)
- **Data Augmentation**: RandAugment, AutoAugment techniques
- **Transfer Learning**: Fine-tuning and layer freezing strategies

---

## Author Notes

This hybrid model was designed specifically for the tea classification task with 18 classes. The dual-branch architecture provides:

✅ **High Accuracy** through ensemble learning
✅ **Efficiency** by combining models with complementary strengths  
✅ **Robustness** through diverse feature representations
✅ **Reproducibility** via joblib state persistence
✅ **Mobile Deployment** with TFLite export

All code is optimized for GPU acceleration with mixed precision training and includes comprehensive error analysis.

---

_Generated: May 2, 2026_
_Notebook: createmodelehybrid.ipynb_
_Tea Classification Dataset: 18 Classes_
