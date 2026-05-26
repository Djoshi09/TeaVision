# Hybrid Model Architecture Visualization

## High-Level Architecture

```
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃                  Input Image (384×384×3)             ┃
┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
                            ↓
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃            Data Augmentation Layer                    ┃
┃  (Flip, Rotate, Zoom, Contrast)                      ┃
┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
                            ↓
      ┏━━━━━━━━━━━━━━━━━━━━┻━━━━━━━━━━━━━━━━━━━┓
      ↓                                         ↓
┏─────────────────────┐             ┏─────────────────────┐
│   BRANCH 1          │             │   BRANCH 2          │
│ EfficientNetV2S     │             │ MobileNetV3Small    │
├─────────────────────┤             ├─────────────────────┤
│ Input: 384×384×3    │             │ Input: 224×224×3    │
│ Backbone (frozen)   │             │ Backbone (frozen)   │
│ Output: 1024 feats  │             │ Output: 576 feats   │
└─────────────────────┘             └─────────────────────┘
      ↓                                         ↓
┏─────────────────────┐             ┏─────────────────────┐
│ Global Avg Pool     │             │ Global Avg Pool     │
│ Output: 1024        │             │ Output: 576         │
└─────────────────────┘             └─────────────────────┘
      ↓                                         ↓
┏─────────────────────┐             ┏─────────────────────┐
│ Dense(256) + ReLU   │             │ Dense(256) + ReLU   │
│ Dropout(0.3)        │             │ Dropout(0.3)        │
│ Output: 256         │             │ Output: 256         │
└─────────────────────┘             └─────────────────────┘
      ↓                                         ↓
      └─────────────────────┬───────────────────┘
                            ↓
                  ┏─────────────────────┐
                  │ Concatenate         │
                  │ Input: 256+256=512  │
                  │ Output: 512         │
                  └─────────────────────┘
                            ↓
                  ┏─────────────────────┐
                  │ Dense(256) + ReLU   │
                  │ Output: 256         │
                  └─────────────────────┘
                            ↓
                  ┏─────────────────────┐
                  │ Dropout(0.3)        │
                  │ Output: 256         │
                  └─────────────────────┘
                            ↓
                  ┏─────────────────────┐
                  │ Dense(18) + Softmax │
                  │ Output: 18 logits   │
                  └─────────────────────┘
                            ↓
            ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
            ┃  18 Tea Classes (Softmax)  ┃
            ┃  [class_0, ..., class_17]  ┃
            ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
```

---

## Training Process

### Phase 1: Head Training (Epochs 1-10)

```
Frozen EfficientNetV2S ─┐
                        ├─→ Train Only Head
Frozen MobileNetV3Small ─┘

Learning Rate: 1e-3
Optimizer: Adam
Loss: Sparse Categorical Crossentropy
Metrics: Accuracy
```

### Phase 2: Fine-tuning (Epochs 11-15)

```
EfficientNetV2S (last 20 layers trainable) ─┐
                                            ├─→ Train Head + Unfrozen Layers
MobileNetV3Small (last 20 layers trainable)─┘

Learning Rate: 1e-5 (reduced)
Optimizer: Adam
Loss: Sparse Categorical Crossentropy
Metrics: Accuracy
```

---

## Data Flow

### Training Data Pipeline

```
Raw Tea Images (Dataset Folders)
         ↓
Load Image Files
         ↓
Stratified Train/Val Split (80/20)
         ↓
tf.data.Dataset Creation
         ↓
  ┏─────────────┓    ┏──────────────┓
  │ Train Set   │    │ Val Set      │
  ├─────────────┤    ├──────────────┤
  │ Shuffle     │    │ No Shuffle   │
  │ Augment     │    │ No Augment   │
  │ Resize      │    │ Resize       │
  │ Batch       │    │ Batch        │
  │ Prefetch    │    │ Prefetch     │
  └─────────────┘    └──────────────┘
         ↓                  ↓
   Training Loop      Validation Loop
```

---

## Model Specifications

### Parameters Breakdown

```
Total Parameters: ~29.5M

Branch 1 (EfficientNetV2S):
├─ Backbone weights: ~21.5M (frozen)
└─ Projection layer: ~262K

Branch 2 (MobileNetV3Small):
├─ Backbone weights: ~2.5M (frozen)
└─ Projection layer: ~131K

Fusion Head:
├─ Dense(256): ~262K
├─ Dense(18): ~4.6K
└─ Total: ~266K

Fine-tuning Trainable:
├─ ~20 layers per backbone (unfrozen during phase 2)
└─ Head layers (always trainable)
```

### Input/Output Specifications

```
Input:
  Shape: (batch_size, 384, 384, 3)
  Range: [0, 255] (uint8) or [0, 1] (float32)

Output:
  Shape: (batch_size, 18)
  Range: [0, 1] (softmax probabilities)
  Classes: 18 tea types
```

---

## Inference Architecture

```
Input Image
     ↓
Resize to 384×384
     ↓
Normalize to [0, 1]
     ↓
Forward Pass through Hybrid Model
     ↓
Output: 18 Probabilities
     ↓
argmax → Class Index
     ↓
class_names[index] → Tea Type
```

---

## Callback Architecture

### Early Stopping

```
Monitor: val_accuracy
Patience: 4 epochs
Action: Restore best weights if no improvement
```

### Learning Rate Reduction

```
Monitor: val_loss
Trigger: No improvement for 2 epochs
Reduction: LR *= 0.5
Min LR: 1e-6
```

### Model Checkpoint

```
Monitor: val_accuracy
Save: Best model only
Path: tea_training_state/hybrid_ensemble/tea_model_best.keras
```

---

## Loss Function & Optimization

### Loss Function

```python
tf.keras.losses.SparseCategoricalCrossentropy()

Formula: -Σ log(p_true_class)
where p_true_class = softmax output for true class
```

### Optimizer

```python
tf.keras.optimizers.Adam(learning_rate=lr)

Phase 1: lr = 1e-3
Phase 2: lr = 1e-5
```

### Class Weighting

```
weight[class_i] = total_samples / (num_classes × count[class_i])

Purpose: Balance loss for imbalanced classes
Effect: Rare classes contribute more to gradient updates
```

---

## Feature Extraction Pipeline

### Branch 1: EfficientNetV2S Path

```
Input (384×384×3)
  ↓
Norm Layer (ImageNet stats)
  ↓
Stem Layers (96 filters)
  ↓
8× MBConv Blocks (96-256 filters)
  ↓
Final Conv (1280 filters)
  ↓
Global Avg Pool (1280 → 1024 dims)
  ↓
Dense Projection (1024 → 256 dims)
```

### Branch 2: MobileNetV3Small Path

```
Input (224×224×3)
  ↓
Norm Layer (ImageNet stats)
  ↓
Conv 2D (3×3, 16 filters)
  ↓
7× Bottleneck Residual Blocks (16-576 filters)
  ↓
Conv 2D (1×1, 1024 filters)
  ↓
Global Avg Pool (1024 → 576 dims)
  ↓
Dense Projection (576 → 256 dims)
```

### Fusion Path

```
Concatenate Branches (256 + 256 = 512)
  ↓
Dense(256) + ReLU + Dropout(0.3)
  ↓
Dense(18) + Softmax
  ↓
Output: Tea Class Probabilities
```

---

## Evaluation Pipeline

```
Validation Dataset
     ↓
Forward Pass (no augmentation)
     ↓
Collect Predictions
     ↓
Compute Confusion Matrix
     ↓
Calculate Metrics:
  • Per-class Precision
  • Per-class Recall
  • Per-class F1-Score
  • Macro Averages
     ↓
Generate Visualizations:
  • Accuracy/Loss Curves
  • Confusion Matrix Heatmap
  • Per-class Metrics Charts
  • Prediction Examples
  • Failure Case Analysis
```

---

## Export Architecture

### Keras Model Export

```
Model → .keras format (HDF5-like)
├─ Architecture (JSON)
├─ Weights (numpy arrays)
└─ Training config
```

### TFLite Conversion

```
Keras Model
  ↓
TFLiteConverter
  ↓
Apply Optimizations (DEFAULT)
  ├─ Quantization (float32 → int8)
  ├─ Operator Fusion
  └─ Constant Folding
  ↓
FlatBuffers Format (.tflite)
  ↓
Mobile Deployment Ready
```

---

## Monitoring & Metrics

### Training Metrics

- **Accuracy**: Correct predictions / Total predictions
- **Loss**: Sparse Categorical Crossentropy

### Validation Metrics

- **Per-class Precision**: TP / (TP + FP)
- **Per-class Recall**: TP / (TP + FN)
- **Per-class F1-Score**: Harmonic mean of Precision & Recall
- **Macro Averages**: Mean of per-class metrics

### Model Selection Criteria

- **Best Model**: Highest val_accuracy
- **Early Stopping**: No improvement for 4 epochs
- **Learning Rate**: Reduced by 0.5× if val_loss plateaus

---

This comprehensive architecture ensures:
✅ **Dual-perspective feature learning** (high & low res)
✅ **Efficient training** (transfer learning + freezing)
✅ **Robust performance** (ensemble benefits)
✅ **Production ready** (Keras + TFLite exports)
✅ **Complete monitoring** (metrics & visualizations)
