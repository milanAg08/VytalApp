# Training the Chronic Risk Classifier Model

## Installation

Install the required packages:

```bash
pip install numpy tensorflow scikit-learn
```

Or use the requirements file:
```bash
pip install -r requirements.txt
```

**Note:** TensorFlow is a large package (~330MB) and may take several minutes to download and install.

## Training

Run the training script:

```bash
python train_model.py
```

The script will:
1. Load the existing vocabulary from `app/src/main/assets/models/vocab.json`
2. Create a comprehensive dataset based on real medical symptom patterns
3. Train a neural network model
4. Convert the model to TensorFlow Lite format
5. Save the model to `app/src/main/assets/models/chronic_risk_classifier.tflite`

## Dataset

The training dataset is based on real medical symptom patterns from medical literature:
- **Diabetes Risk Pattern**: increased thirst, frequent urination, unexplained weight loss, increased hunger, fatigue, blurry vision
- **Hypertension/Heart Risk Pattern**: chest pain, shortness of breath, dizziness, palpitations, high blood pressure
- **Thyroid Imbalance Pattern**: weight gain, fatigue, cold intolerance, hair loss, dry skin, mood changes
- **Asthma/Respiratory Pattern**: shortness of breath, wheezing, chest tightness, coughing
- **Arthritis/Joint Pain Pattern**: joint pain, stiffness, swelling, difficulty moving
- **Obesity/Metabolic Risk Pattern**: weight gain, difficulty losing weight, high blood pressure, high cholesterol
- **Stress/Fatigue Pattern**: fatigue, stress, anxiety, headache, sleep problems

## Model Architecture

- Input: Sequence of 20 tokens (matching app's preprocessing)
- Embedding layer: 32 dimensions
- LSTM layer: 64 units
- Dense layers with dropout for regularization
- Output: 7 classes (logits, app applies softmax)

## Testing

After training, the script will test the model with diabetes symptoms:
```
"increased thirst frequent urination unexplained weight loss increased hunger fatigue blurry vision"
```

Expected output: "Diabetes Risk Pattern" with high confidence.

