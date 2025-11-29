"""
Training script for chronic disease risk classifier
Uses real medical symptom patterns based on medical literature
"""

import json
import re
import numpy as np
import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers
from collections import Counter
from sklearn.model_selection import train_test_split
import os

# Real medical symptom patterns based on medical literature
# These are based on actual clinical presentations of these conditions

DATASET = [
    # Diabetes Risk Pattern - based on classic diabetes symptoms
    ("increased thirst frequent urination unexplained weight loss increased hunger fatigue blurry vision", "Diabetes Risk Pattern"),
    ("excessive thirst urinating more often weight loss despite eating more extreme hunger", "Diabetes Risk Pattern"),
    ("thirsty all the time frequent bathroom trips losing weight without trying very hungry tired", "Diabetes Risk Pattern"),
    ("polydipsia polyuria weight loss polyphagia fatigue blurred vision", "Diabetes Risk Pattern"),
    ("high blood sugar glucose levels increased thirst frequent urination", "Diabetes Risk Pattern"),
    ("thirsty urination weight loss hunger fatigue vision problems", "Diabetes Risk Pattern"),
    ("diabetes symptoms sugar in urine excessive thirst frequent urination", "Diabetes Risk Pattern"),
    ("increased thirst frequent urination weight loss increased appetite fatigue", "Diabetes Risk Pattern"),
    ("thirst urination weight loss hunger tired blurry vision", "Diabetes Risk Pattern"),
    ("excessive thirst frequent urination unexplained weight loss extreme hunger fatigue", "Diabetes Risk Pattern"),
    ("thirsty urinating weight loss hungry tired vision blur", "Diabetes Risk Pattern"),
    ("increased thirst frequent urination weight loss increased hunger fatigue blurry vision", "Diabetes Risk Pattern"),
    ("thirst urination loss weight hunger fatigue vision", "Diabetes Risk Pattern"),
    ("sugar glucose high blood increased thirst frequent urination", "Diabetes Risk Pattern"),
    ("diabetes risk thirst urination weight loss hunger fatigue", "Diabetes Risk Pattern"),
    ("excessive thirst frequent urination weight loss hunger fatigue blurry vision", "Diabetes Risk Pattern"),
    ("thirsty urinating more losing weight hungry tired blurry vision", "Diabetes Risk Pattern"),
    ("increased thirst frequent urination unexplained weight loss increased hunger", "Diabetes Risk Pattern"),
    ("thirst urination weight loss hunger fatigue vision problems", "Diabetes Risk Pattern"),
    ("polydipsia polyuria weight loss polyphagia fatigue blurred vision", "Diabetes Risk Pattern"),
    ("high glucose sugar increased thirst frequent urination weight loss", "Diabetes Risk Pattern"),
    ("thirst urination weight loss hunger fatigue blurry vision", "Diabetes Risk Pattern"),
    ("excessive thirst frequent urination weight loss increased hunger fatigue", "Diabetes Risk Pattern"),
    ("thirsty urinating weight loss hungry tired vision blur", "Diabetes Risk Pattern"),
    ("increased thirst frequent urination weight loss hunger fatigue blurry vision", "Diabetes Risk Pattern"),
    
    # Hypertension/Heart Risk Pattern - based on cardiovascular symptoms
    ("chest pain shortness of breath dizziness palpitations high blood pressure", "Hypertension/Heart Risk Pattern"),
    ("chest pain breathless dizzy heart racing high bp pressure", "Hypertension/Heart Risk Pattern"),
    ("chest discomfort shortness breath dizziness heart palpitations hypertension", "Hypertension/Heart Risk Pattern"),
    ("chest pain difficulty breathing dizzy palpitations blood pressure high", "Hypertension/Heart Risk Pattern"),
    ("chest tightness breathlessness dizziness racing heart high blood pressure", "Hypertension/Heart Risk Pattern"),
    ("chest pain shortness breath dizzy heart pounding hypertension", "Hypertension/Heart Risk Pattern"),
    ("chest discomfort breathing problems dizziness palpitations bp high", "Hypertension/Heart Risk Pattern"),
    ("chest pain breathless dizzy heart racing blood pressure", "Hypertension/Heart Risk Pattern"),
    ("chest tightness shortness breath dizziness palpitations hypertension", "Hypertension/Heart Risk Pattern"),
    ("chest pain difficulty breathing dizzy heart racing high bp", "Hypertension/Heart Risk Pattern"),
    ("chest discomfort breathlessness dizziness palpitations blood pressure", "Hypertension/Heart Risk Pattern"),
    ("chest pain shortness breath dizzy heart pounding high pressure", "Hypertension/Heart Risk Pattern"),
    ("chest tightness breathing problems dizziness racing heart hypertension", "Hypertension/Heart Risk Pattern"),
    ("chest pain breathless dizzy palpitations high blood pressure", "Hypertension/Heart Risk Pattern"),
    ("chest discomfort shortness breath dizziness heart racing bp high", "Hypertension/Heart Risk Pattern"),
    ("chest pain difficulty breathing dizzy palpitations hypertension", "Hypertension/Heart Risk Pattern"),
    ("chest tightness breathlessness dizziness heart pounding blood pressure", "Hypertension/Heart Risk Pattern"),
    ("chest pain shortness breath dizzy racing heart high bp", "Hypertension/Heart Risk Pattern"),
    ("chest discomfort breathing problems dizziness palpitations pressure", "Hypertension/Heart Risk Pattern"),
    ("chest pain breathless dizzy heart racing hypertension", "Hypertension/Heart Risk Pattern"),
    ("chest tightness shortness breath dizziness palpitations high pressure", "Hypertension/Heart Risk Pattern"),
    ("chest pain difficulty breathing dizzy heart pounding bp", "Hypertension/Heart Risk Pattern"),
    ("chest discomfort breathlessness dizziness racing heart blood pressure", "Hypertension/Heart Risk Pattern"),
    ("chest pain shortness breath dizzy palpitations hypertension", "Hypertension/Heart Risk Pattern"),
    ("chest tightness breathing problems dizziness heart racing high bp", "Hypertension/Heart Risk Pattern"),
    
    # Thyroid Imbalance Pattern - based on hypo/hyperthyroidism symptoms
    ("weight gain fatigue cold intolerance hair loss dry skin mood changes", "Thyroid Imbalance Pattern"),
    ("weight gain tired cold all the time hair falling out dry skin mood swings", "Thyroid Imbalance Pattern"),
    ("weight gain fatigue feeling cold hair loss dry skin anxiety depression", "Thyroid Imbalance Pattern"),
    ("weight gain tired cold intolerance hair thinning dry skin mood changes", "Thyroid Imbalance Pattern"),
    ("weight gain fatigue cold sensitivity hair loss dry skin emotional changes", "Thyroid Imbalance Pattern"),
    ("weight gain tired feeling cold hair falling dry skin mood problems", "Thyroid Imbalance Pattern"),
    ("weight gain fatigue cold intolerance hair loss dry skin thyroid symptoms", "Thyroid Imbalance Pattern"),
    ("weight gain tired cold all time hair loss dry skin mood swings", "Thyroid Imbalance Pattern"),
    ("weight gain fatigue cold sensitivity hair thinning dry skin mood changes", "Thyroid Imbalance Pattern"),
    ("weight gain tired cold intolerance hair falling dry skin anxiety", "Thyroid Imbalance Pattern"),
    ("weight gain fatigue feeling cold hair loss dry skin depression mood", "Thyroid Imbalance Pattern"),
    ("weight gain tired cold sensitivity hair loss dry skin emotional changes", "Thyroid Imbalance Pattern"),
    ("weight gain fatigue cold all time hair thinning dry skin mood problems", "Thyroid Imbalance Pattern"),
    ("weight gain tired cold intolerance hair falling dry skin thyroid", "Thyroid Imbalance Pattern"),
    ("weight gain fatigue cold sensitivity hair loss dry skin mood swings", "Thyroid Imbalance Pattern"),
    ("weight gain tired feeling cold hair loss dry skin mood changes", "Thyroid Imbalance Pattern"),
    ("weight gain fatigue cold intolerance hair thinning dry skin anxiety", "Thyroid Imbalance Pattern"),
    ("weight gain tired cold sensitivity hair falling dry skin depression", "Thyroid Imbalance Pattern"),
    ("weight gain fatigue cold all time hair loss dry skin mood problems", "Thyroid Imbalance Pattern"),
    ("weight gain tired cold intolerance hair loss dry skin emotional changes", "Thyroid Imbalance Pattern"),
    ("weight gain fatigue cold sensitivity hair thinning dry skin mood swings", "Thyroid Imbalance Pattern"),
    ("weight gain tired feeling cold hair falling dry skin mood changes", "Thyroid Imbalance Pattern"),
    ("weight gain fatigue cold intolerance hair loss dry skin thyroid symptoms", "Thyroid Imbalance Pattern"),
    ("weight gain tired cold sensitivity hair loss dry skin mood problems", "Thyroid Imbalance Pattern"),
    ("weight gain fatigue cold all time hair thinning dry skin anxiety", "Thyroid Imbalance Pattern"),
    
    # Asthma/Respiratory Pattern - based on respiratory symptoms
    ("shortness of breath wheezing chest tightness coughing difficulty breathing", "Asthma/Respiratory Pattern"),
    ("breathless wheezing chest tight coughing trouble breathing", "Asthma/Respiratory Pattern"),
    ("shortness breath wheeze chest tightness cough breathing problems", "Asthma/Respiratory Pattern"),
    ("breathlessness wheezing chest discomfort coughing difficulty breathing", "Asthma/Respiratory Pattern"),
    ("shortness breath wheeze chest tight coughing trouble breathing", "Asthma/Respiratory Pattern"),
    ("breathless wheezing chest tightness cough breathing problems", "Asthma/Respiratory Pattern"),
    ("shortness breath wheeze chest discomfort coughing difficulty breathing", "Asthma/Respiratory Pattern"),
    ("breathlessness wheezing chest tight coughing trouble breathing", "Asthma/Respiratory Pattern"),
    ("shortness breath wheeze chest tightness cough breathing problems", "Asthma/Respiratory Pattern"),
    ("breathless wheezing chest discomfort coughing difficulty breathing", "Asthma/Respiratory Pattern"),
    ("shortness breath wheeze chest tight coughing trouble breathing", "Asthma/Respiratory Pattern"),
    ("breathlessness wheezing chest tightness cough breathing problems", "Asthma/Respiratory Pattern"),
    ("shortness breath wheeze chest discomfort coughing difficulty breathing", "Asthma/Respiratory Pattern"),
    ("breathless wheezing chest tight coughing trouble breathing", "Asthma/Respiratory Pattern"),
    ("shortness breath wheeze chest tightness cough breathing problems", "Asthma/Respiratory Pattern"),
    ("breathlessness wheezing chest discomfort coughing difficulty breathing", "Asthma/Respiratory Pattern"),
    ("shortness breath wheeze chest tight coughing trouble breathing", "Asthma/Respiratory Pattern"),
    ("breathless wheezing chest tightness cough breathing problems", "Asthma/Respiratory Pattern"),
    ("shortness breath wheeze chest discomfort coughing difficulty breathing", "Asthma/Respiratory Pattern"),
    ("breathlessness wheezing chest tight coughing trouble breathing", "Asthma/Respiratory Pattern"),
    ("shortness breath wheeze chest tightness cough breathing problems", "Asthma/Respiratory Pattern"),
    ("breathless wheezing chest discomfort coughing difficulty breathing", "Asthma/Respiratory Pattern"),
    ("shortness breath wheeze chest tight coughing trouble breathing", "Asthma/Respiratory Pattern"),
    ("breathlessness wheezing chest tightness cough breathing problems", "Asthma/Respiratory Pattern"),
    ("shortness breath wheeze chest discomfort coughing difficulty breathing", "Asthma/Respiratory Pattern"),
    
    # Arthritis/Joint Pain Pattern - based on joint symptoms
    ("joint pain stiffness swelling knee hip hands difficulty moving", "Arthritis/Joint Pain Pattern"),
    ("joint pain stiff swollen knees hips hands trouble moving", "Arthritis/Joint Pain Pattern"),
    ("joint pain stiffness swelling knee hip hands movement problems", "Arthritis/Joint Pain Pattern"),
    ("joint pain stiff swollen knees hips hands difficulty moving", "Arthritis/Joint Pain Pattern"),
    ("joint pain stiffness swelling knee hip hands trouble moving", "Arthritis/Joint Pain Pattern"),
    ("joint pain stiff swollen knees hips hands movement problems", "Arthritis/Joint Pain Pattern"),
    ("joint pain stiffness swelling knee hip hands difficulty moving", "Arthritis/Joint Pain Pattern"),
    ("joint pain stiff swollen knees hips hands trouble moving", "Arthritis/Joint Pain Pattern"),
    ("joint pain stiffness swelling knee hip hands movement problems", "Arthritis/Joint Pain Pattern"),
    ("joint pain stiff swollen knees hips hands difficulty moving", "Arthritis/Joint Pain Pattern"),
    ("joint pain stiffness swelling knee hip hands trouble moving", "Arthritis/Joint Pain Pattern"),
    ("joint pain stiff swollen knees hips hands movement problems", "Arthritis/Joint Pain Pattern"),
    ("joint pain stiffness swelling knee hip hands difficulty moving", "Arthritis/Joint Pain Pattern"),
    ("joint pain stiff swollen knees hips hands trouble moving", "Arthritis/Joint Pain Pattern"),
    ("joint pain stiffness swelling knee hip hands movement problems", "Arthritis/Joint Pain Pattern"),
    ("joint pain stiff swollen knees hips hands difficulty moving", "Arthritis/Joint Pain Pattern"),
    ("joint pain stiffness swelling knee hip hands trouble moving", "Arthritis/Joint Pain Pattern"),
    ("joint pain stiff swollen knees hips hands movement problems", "Arthritis/Joint Pain Pattern"),
    ("joint pain stiffness swelling knee hip hands difficulty moving", "Arthritis/Joint Pain Pattern"),
    ("joint pain stiff swollen knees hips hands trouble moving", "Arthritis/Joint Pain Pattern"),
    ("joint pain stiffness swelling knee hip hands movement problems", "Arthritis/Joint Pain Pattern"),
    ("joint pain stiff swollen knees hips hands difficulty moving", "Arthritis/Joint Pain Pattern"),
    ("joint pain stiffness swelling knee hip hands trouble moving", "Arthritis/Joint Pain Pattern"),
    ("joint pain stiff swollen knees hips hands movement problems", "Arthritis/Joint Pain Pattern"),
    ("joint pain stiffness swelling knee hip hands difficulty moving", "Arthritis/Joint Pain Pattern"),
    
    # Obesity/Metabolic Risk Pattern - based on metabolic syndrome symptoms
    ("weight gain difficulty losing weight fatigue high blood pressure high cholesterol", "Obesity/Metabolic Risk Pattern"),
    ("weight gain trouble losing weight tired high bp high cholesterol", "Obesity/Metabolic Risk Pattern"),
    ("weight gain difficulty losing weight fatigue high blood pressure cholesterol", "Obesity/Metabolic Risk Pattern"),
    ("weight gain trouble losing weight tired high bp cholesterol", "Obesity/Metabolic Risk Pattern"),
    ("weight gain difficulty losing weight fatigue high blood pressure high cholesterol", "Obesity/Metabolic Risk Pattern"),
    ("weight gain trouble losing weight tired high bp high cholesterol", "Obesity/Metabolic Risk Pattern"),
    ("weight gain difficulty losing weight fatigue high blood pressure cholesterol", "Obesity/Metabolic Risk Pattern"),
    ("weight gain trouble losing weight tired high bp cholesterol", "Obesity/Metabolic Risk Pattern"),
    ("weight gain difficulty losing weight fatigue high blood pressure high cholesterol", "Obesity/Metabolic Risk Pattern"),
    ("weight gain trouble losing weight tired high bp high cholesterol", "Obesity/Metabolic Risk Pattern"),
    ("weight gain difficulty losing weight fatigue high blood pressure cholesterol", "Obesity/Metabolic Risk Pattern"),
    ("weight gain trouble losing weight tired high bp cholesterol", "Obesity/Metabolic Risk Pattern"),
    ("weight gain difficulty losing weight fatigue high blood pressure high cholesterol", "Obesity/Metabolic Risk Pattern"),
    ("weight gain trouble losing weight tired high bp high cholesterol", "Obesity/Metabolic Risk Pattern"),
    ("weight gain difficulty losing weight fatigue high blood pressure cholesterol", "Obesity/Metabolic Risk Pattern"),
    ("weight gain trouble losing weight tired high bp cholesterol", "Obesity/Metabolic Risk Pattern"),
    ("weight gain difficulty losing weight fatigue high blood pressure high cholesterol", "Obesity/Metabolic Risk Pattern"),
    ("weight gain trouble losing weight tired high bp high cholesterol", "Obesity/Metabolic Risk Pattern"),
    ("weight gain difficulty losing weight fatigue high blood pressure cholesterol", "Obesity/Metabolic Risk Pattern"),
    ("weight gain trouble losing weight tired high bp cholesterol", "Obesity/Metabolic Risk Pattern"),
    ("weight gain difficulty losing weight fatigue high blood pressure high cholesterol", "Obesity/Metabolic Risk Pattern"),
    ("weight gain trouble losing weight tired high bp high cholesterol", "Obesity/Metabolic Risk Pattern"),
    ("weight gain difficulty losing weight fatigue high blood pressure cholesterol", "Obesity/Metabolic Risk Pattern"),
    ("weight gain trouble losing weight tired high bp cholesterol", "Obesity/Metabolic Risk Pattern"),
    ("weight gain difficulty losing weight fatigue high blood pressure high cholesterol", "Obesity/Metabolic Risk Pattern"),
    
    # Stress/Fatigue Pattern - based on stress and fatigue symptoms
    ("fatigue tired stress anxiety headache sleep problems mood changes", "Stress/Fatigue Pattern"),
    ("tired fatigue stressed anxious headache trouble sleeping mood swings", "Stress/Fatigue Pattern"),
    ("fatigue tired stress anxiety headache sleep problems mood changes", "Stress/Fatigue Pattern"),
    ("tired fatigue stressed anxious headache trouble sleeping mood swings", "Stress/Fatigue Pattern"),
    ("fatigue tired stress anxiety headache sleep problems mood changes", "Stress/Fatigue Pattern"),
    ("tired fatigue stressed anxious headache trouble sleeping mood swings", "Stress/Fatigue Pattern"),
    ("fatigue tired stress anxiety headache sleep problems mood changes", "Stress/Fatigue Pattern"),
    ("tired fatigue stressed anxious headache trouble sleeping mood swings", "Stress/Fatigue Pattern"),
    ("fatigue tired stress anxiety headache sleep problems mood changes", "Stress/Fatigue Pattern"),
    ("tired fatigue stressed anxious headache trouble sleeping mood swings", "Stress/Fatigue Pattern"),
    ("fatigue tired stress anxiety headache sleep problems mood changes", "Stress/Fatigue Pattern"),
    ("tired fatigue stressed anxious headache trouble sleeping mood swings", "Stress/Fatigue Pattern"),
    ("fatigue tired stress anxiety headache sleep problems mood changes", "Stress/Fatigue Pattern"),
    ("tired fatigue stressed anxious headache trouble sleeping mood swings", "Stress/Fatigue Pattern"),
    ("fatigue tired stress anxiety headache sleep problems mood changes", "Stress/Fatigue Pattern"),
    ("tired fatigue stressed anxious headache trouble sleeping mood swings", "Stress/Fatigue Pattern"),
    ("fatigue tired stress anxiety headache sleep problems mood changes", "Stress/Fatigue Pattern"),
    ("tired fatigue stressed anxious headache trouble sleeping mood swings", "Stress/Fatigue Pattern"),
    ("fatigue tired stress anxiety headache sleep problems mood changes", "Stress/Fatigue Pattern"),
    ("tired fatigue stressed anxious headache trouble sleeping mood swings", "Stress/Fatigue Pattern"),
    ("fatigue tired stress anxiety headache sleep problems mood changes", "Stress/Fatigue Pattern"),
    ("tired fatigue stressed anxious headache trouble sleeping mood swings", "Stress/Fatigue Pattern"),
    ("fatigue tired stress anxiety headache sleep problems mood changes", "Stress/Fatigue Pattern"),
    ("tired fatigue stressed anxious headache trouble sleeping mood swings", "Stress/Fatigue Pattern"),
    ("fatigue tired stress anxiety headache sleep problems mood changes", "Stress/Fatigue Pattern"),
]

# Additional variations to increase dataset size
def augment_dataset(base_dataset, num_variations=2):
    """Create variations of the base dataset with more controlled augmentation"""
    augmented = []
    for text, label in base_dataset:
        augmented.append((text, label))
        # Add variations with word substitutions and reordering
        words = text.split()
        for _ in range(num_variations):
            if len(words) > 2:
                # Create variation by swapping adjacent words (more natural)
                new_words = words.copy()
                # Swap 1-2 pairs of adjacent words
                num_swaps = min(2, len(words) // 2)
                swap_indices = np.random.choice(len(words) - 1, size=num_swaps, replace=False)
                for idx in swap_indices:
                    new_words[idx], new_words[idx + 1] = new_words[idx + 1], new_words[idx]
                augmented.append((' '.join(new_words), label))
    return augmented

# Check class distribution before augmentation
from collections import Counter
label_counts = Counter([label for _, label in DATASET])
print("\nBase dataset class distribution:")
for label, count in label_counts.items():
    print(f"  {label}: {count} examples")

# Augment dataset - use fewer variations to prevent overfitting
# Balance the dataset better
full_dataset = []
for text, label in DATASET:
    full_dataset.append((text, label))
    # Add only 1-2 variations per example to keep it balanced
    words = text.split()
    if len(words) > 2:
        # Add one variation with word swap
        new_words = words.copy()
        if len(new_words) > 1:
            idx = np.random.randint(0, len(new_words) - 1)
            new_words[idx], new_words[idx + 1] = new_words[idx + 1], new_words[idx]
        full_dataset.append((' '.join(new_words), label))

# Check class distribution after augmentation
label_counts_after = Counter([label for _, label in full_dataset])
print("\nAfter augmentation class distribution:")
for label, count in sorted(label_counts_after.items()):
    print(f"  {label}: {count} examples")

print(f"Total dataset size: {len(full_dataset)} samples")
print(f"Classes: {set([label for _, label in full_dataset])}")

# Extract texts and labels
texts = [text for text, _ in full_dataset]
labels = [label for _, label in full_dataset]

# Build vocabulary from existing vocab.json and add new words
with open('app/src/main/assets/models/vocab.json', 'r') as f:
    existing_vocab = json.load(f)

# Extract all words from texts
all_words = []
for text in texts:
    words = re.sub(r'[^a-z0-9_\s]', ' ', text.lower()).split()
    all_words.extend(words)

word_counts = Counter(all_words)
vocab_size = len(existing_vocab)

# Add new words to vocabulary if they don't exist
vocab = existing_vocab.copy()
for word in word_counts:
    if word not in vocab and len(word) > 0:
        vocab[word] = vocab_size
        vocab_size += 1

# Ensure [PAD] and [UNK] exist
if "[PAD]" not in vocab:
    vocab["[PAD]"] = 0
if "[UNK]" not in vocab:
    vocab["[UNK]"] = 1

print(f"Vocabulary size: {len(vocab)}")

# Save updated vocabulary
os.makedirs('app/src/main/assets/models', exist_ok=True)
with open('app/src/main/assets/models/vocab.json', 'w') as f:
    json.dump(vocab, f, indent=2)

# Load labels
with open('app/src/main/assets/models/labels.json', 'r') as f:
    label_list = json.load(f)

label_to_idx = {label: idx for idx, label in enumerate(label_list)}
num_classes = len(label_list)

print(f"Labels: {label_list}")

# Tokenize function matching the app's preprocessing
def tokenize(text, vocab, seq_len=20):
    """Tokenize text matching the app's preprocessing"""
    tokens = re.sub(r'[^a-z0-9_\s]', ' ', text.lower()).split()
    tokens = [t for t in tokens if t.strip()]
    
    ids = [vocab.get(token, vocab.get("[UNK]", 1)) for token in tokens]
    
    # Pad or truncate to seq_len
    if len(ids) >= seq_len:
        ids = ids[:seq_len]
    else:
        ids = ids + [vocab.get("[PAD]", 0)] * (seq_len - len(ids))
    
    return ids

# Prepare data
X = np.array([tokenize(text, vocab, seq_len=20) for text in texts], dtype=np.int32)
y = np.array([label_to_idx[label] for label in labels], dtype=np.int32)

# Convert to one-hot
y_onehot = keras.utils.to_categorical(y, num_classes=num_classes)

# Split data
X_train, X_test, y_train, y_test = train_test_split(
    X, y_onehot, test_size=0.2, random_state=42, stratify=y
)

print(f"Training samples: {len(X_train)}")
print(f"Test samples: {len(X_test)}")

# Build model - simpler architecture to prevent overfitting
# Using GlobalAveragePooling instead of LSTM for better TFLite support
model = keras.Sequential([
    layers.Embedding(input_dim=len(vocab), output_dim=64, input_length=20, name='embedding'),
    layers.GlobalAveragePooling1D(name='pooling'),
    layers.Dense(128, activation='relu', name='dense1'),
    layers.Dropout(0.6, name='dropout1'),  # Higher dropout to prevent overfitting
    layers.Dense(64, activation='relu', name='dense2'),
    layers.Dropout(0.5, name='dropout2'),
    layers.Dense(num_classes, activation='linear', name='output')  # Linear for logits (app applies softmax)
])

# Use label smoothing to prevent overconfidence
model.compile(
    optimizer=keras.optimizers.Adam(learning_rate=0.001),
    loss=keras.losses.CategoricalCrossentropy(label_smoothing=0.1),  # Add label smoothing
    metrics=['accuracy']
)

model.summary()

# Train model with class weights to handle any imbalance
from sklearn.utils.class_weight import compute_class_weight
class_weights = compute_class_weight('balanced', classes=np.unique(y), y=y)
class_weight_dict = {i: class_weights[i] for i in range(len(class_weights))}

print(f"\nClass weights: {class_weight_dict}")

history = model.fit(
    X_train, y_train,
    batch_size=16,  # Smaller batch size for better gradient updates
    epochs=100,  # Fewer epochs to prevent overfitting
    validation_data=(X_test, y_test),
    verbose=1,
    class_weight=class_weight_dict,
    callbacks=[
        keras.callbacks.EarlyStopping(monitor='val_accuracy', patience=15, restore_best_weights=True, mode='max'),
        keras.callbacks.ReduceLROnPlateau(monitor='val_loss', factor=0.5, patience=5, min_lr=1e-6, verbose=1)
    ]
)

# Evaluate
test_loss, test_accuracy = model.evaluate(X_test, y_test, verbose=0)
print(f"\nTest Accuracy: {test_accuracy:.4f}")

# Test with multiple different symptom combinations
test_cases = [
    ("increased thirst frequent urination unexplained weight loss increased hunger fatigue blurry vision", "Diabetes Risk Pattern"),
    ("chest pain shortness of breath dizziness palpitations high blood pressure", "Hypertension/Heart Risk Pattern"),
    ("weight gain fatigue cold intolerance hair loss dry skin mood changes", "Thyroid Imbalance Pattern"),
    ("shortness of breath wheezing chest tightness coughing difficulty breathing", "Asthma/Respiratory Pattern"),
    ("joint pain stiffness swelling knee hip hands difficulty moving", "Arthritis/Joint Pain Pattern"),
    ("weight gain difficulty losing weight fatigue high blood pressure high cholesterol", "Obesity/Metabolic Risk Pattern"),
    ("fatigue tired stress anxiety headache sleep problems mood changes", "Stress/Fatigue Pattern"),
]

print("\n" + "="*60)
print("Testing model with different symptom combinations:")
print("="*60)

for test_text, expected_label in test_cases:
    test_tokens = np.array([tokenize(test_text, vocab, seq_len=20)], dtype=np.int32)
    prediction = model.predict(test_tokens, verbose=0)
    
    # Apply softmax
    logits = prediction[0]
    exps = np.exp(logits)
    probs = exps / np.sum(exps)
    
    predicted_class = np.argmax(probs)
    confidence = probs[predicted_class]
    
    print(f"\nText: {test_text[:60]}...")
    print(f"Expected: {expected_label}")
    print(f"Predicted: {label_list[predicted_class]} (Confidence: {confidence:.2%})")
    print(f"Top 3: {[(label_list[i], f'{probs[i]:.2%}') for i in np.argsort(probs)[-3:][::-1]]}")
    
    if label_list[predicted_class] != expected_label:
        print(f"  ⚠️  MISMATCH!")

# Convert to TensorFlow Lite
# Ensure input type is int32 for embeddings
converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.target_spec.supported_types = [tf.float32]
converter.optimizations = [tf.lite.Optimize.DEFAULT]

# Create a representative dataset to help with quantization
def representative_dataset():
    for i in range(10):
        yield [X_train[i:i+1].astype(np.int32)]

# Try to convert with int32 input preservation
try:
    tflite_model = converter.convert()
except Exception as e:
    print(f"Standard conversion failed: {e}")
    # Fallback: convert without optimizations
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    tflite_model = converter.convert()

# Save TFLite model
tflite_path = 'app/src/main/assets/models/chronic_risk_classifier.tflite'
with open(tflite_path, 'wb') as f:
    f.write(tflite_model)

print(f"\nModel saved to {tflite_path}")
print(f"Model size: {len(tflite_model) / 1024:.2f} KB")

# Verify the model works
interpreter = tf.lite.Interpreter(model_path=tflite_path)
interpreter.allocate_tensors()

input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()

print(f"\nInput shape: {input_details[0]['shape']}")
print(f"Output shape: {output_details[0]['shape']}")

# Test TFLite model
# Check input type from model
input_dtype = input_details[0]['dtype']
if input_dtype == np.float32:
    test_input = test_tokens.astype(np.float32)
else:
    test_input = test_tokens.astype(np.int32)
interpreter.set_tensor(input_details[0]['index'], test_input)
interpreter.invoke()
output_data = interpreter.get_tensor(output_details[0]['index'])

# Apply softmax (matching app's logic)
exp_output = np.exp(output_data[0])
softmax_output = exp_output / np.sum(exp_output)
predicted_class_tflite = np.argmax(softmax_output)
confidence_tflite = softmax_output[predicted_class_tflite]

print(f"\nTFLite test prediction:")
print(f"Predicted: {label_list[predicted_class_tflite]}")
print(f"Confidence: {confidence_tflite:.4f}")

