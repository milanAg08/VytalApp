# Transfer Learning Training Script

## Overview

The `train_with_transfer_learning.py` script uses an improved training approach with:
- Better regularization (BatchNormalization, higher dropout)
- Label smoothing to prevent overconfidence
- Learning rate scheduling
- Class-balanced training
- Comprehensive testing

## Key Improvements

1. **Better Architecture**:
   - Embedding layer with Glorot initialization
   - BatchNormalization layers for stable training
   - Higher dropout rates (0.5, 0.4, 0.3) to prevent overfitting
   - Multiple dense layers for better feature learning

2. **Improved Training**:
   - Learning rate decay schedule
   - Label smoothing (0.1) to prevent overconfidence
   - Class weights for balanced training
   - Early stopping based on validation accuracy

3. **Better Testing**:
   - Tests all 7 disease patterns
   - Shows top 3 predictions
   - Calculates accuracy on test cases

## Usage

```bash
# Activate virtual environment
venv\Scripts\activate.bat

# Run training
python train_with_transfer_learning.py
```

## Expected Results

- **Test Accuracy**: Should be > 70% on test set
- **Confidence Scores**: Should be > 50% for correct predictions
- **Class Balance**: Model should predict different classes correctly

## Using Real Datasets from Kaggle

To use real datasets from Kaggle:

1. **Install Kaggle API**:
   ```bash
   pip install kaggle
   ```

2. **Get API Credentials**:
   - Go to https://www.kaggle.com/account
   - Create API token
   - Download `kaggle.json`
   - Place in `~/.kaggle/` (or `C:\Users\YourName\.kaggle\` on Windows)

3. **Download Dataset**:
   ```python
   from download_kaggle_dataset import download_kaggle_dataset, load_symptom2disease_dataset
   
   # Download symptom2disease dataset
   download_kaggle_dataset("itachi9604/disease-symptom-description-dataset")
   
   # Load and process
   texts, labels = load_symptom2disease_dataset()
   ```

4. **Popular Medical Datasets**:
   - `itachi9604/disease-symptom-description-dataset` - Disease symptoms
   - `kaushil268/disease-symptom-description-dataset` - Another symptom dataset
   - `prathamtripathi/drug-classification` - Drug classification

## Model Output

The trained model will be saved to:
- `app/src/main/assets/models/chronic_risk_classifier.tflite`

The model:
- Input: Sequence of 20 tokens (int32 or float32)
- Output: 7 logits (applies softmax)
- Size: ~200-600 KB (depending on architecture)

## Troubleshooting

### Low Confidence Scores
- Increase training epochs
- Add more training data
- Reduce dropout rates slightly
- Use larger embedding dimensions

### Wrong Predictions
- Check class distribution (should be balanced)
- Verify vocabulary includes all necessary words
- Increase model capacity
- Use more diverse training examples

### Model Too Large
- Reduce embedding dimensions
- Use fewer dense layers
- Reduce hidden units
- Use quantization

