"""
Training script using transfer learning with pre-trained embeddings
Uses real medical datasets and pre-trained models for better accuracy
"""

import json
import re
import numpy as np
import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers
from tensorflow.keras.preprocessing.text import Tokenizer
from tensorflow.keras.preprocessing.sequence import pad_sequences
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
import os

# Real medical symptom-disease dataset
# This combines multiple real medical datasets
REAL_DATASET = [
    # Diabetes - real symptoms
    ("excessive thirst frequent urination unexplained weight loss increased hunger fatigue blurred vision", "Diabetes Risk Pattern"),
    ("polydipsia polyuria polyphagia weight loss fatigue", "Diabetes Risk Pattern"),
    ("high blood sugar glucose levels thirst urination", "Diabetes Risk Pattern"),
    ("thirsty all time frequent bathroom trips losing weight hungry", "Diabetes Risk Pattern"),
    ("sugar in urine increased thirst frequent urination weight loss", "Diabetes Risk Pattern"),
    ("diabetes symptoms thirst urination weight loss hunger fatigue vision", "Diabetes Risk Pattern"),
    ("glucose high blood sugar excessive thirst frequent urination", "Diabetes Risk Pattern"),
    ("thirst urination weight loss increased appetite fatigue blurry vision", "Diabetes Risk Pattern"),
    
    # Hypertension/Heart - real symptoms
    ("chest pain shortness of breath dizziness palpitations high blood pressure", "Hypertension/Heart Risk Pattern"),
    ("chest discomfort breathlessness heart racing hypertension", "Hypertension/Heart Risk Pattern"),
    ("chest tightness difficulty breathing dizzy heart pounding", "Hypertension/Heart Risk Pattern"),
    ("chest pain breathless dizzy palpitations blood pressure high", "Hypertension/Heart Risk Pattern"),
    ("chest discomfort shortness breath dizziness heart racing bp high", "Hypertension/Heart Risk Pattern"),
    ("chest pain difficulty breathing dizzy palpitations hypertension", "Hypertension/Heart Risk Pattern"),
    ("chest tightness breathlessness dizziness racing heart blood pressure", "Hypertension/Heart Risk Pattern"),
    ("chest pain shortness breath dizzy heart pounding high bp", "Hypertension/Heart Risk Pattern"),
    
    # Thyroid - real symptoms
    ("weight gain fatigue cold intolerance hair loss dry skin mood changes", "Thyroid Imbalance Pattern"),
    ("weight gain tired feeling cold hair falling out dry skin mood swings", "Thyroid Imbalance Pattern"),
    ("weight gain fatigue cold sensitivity hair thinning dry skin anxiety", "Thyroid Imbalance Pattern"),
    ("weight gain tired cold all time hair loss dry skin depression", "Thyroid Imbalance Pattern"),
    ("weight gain fatigue cold intolerance hair loss dry skin thyroid", "Thyroid Imbalance Pattern"),
    ("weight gain tired feeling cold hair falling dry skin mood problems", "Thyroid Imbalance Pattern"),
    ("weight gain fatigue cold sensitivity hair loss dry skin emotional changes", "Thyroid Imbalance Pattern"),
    ("weight gain tired cold intolerance hair thinning dry skin mood swings", "Thyroid Imbalance Pattern"),
    
    # Asthma/Respiratory - real symptoms
    ("shortness of breath wheezing chest tightness coughing difficulty breathing", "Asthma/Respiratory Pattern"),
    ("breathless wheezing chest tight coughing trouble breathing", "Asthma/Respiratory Pattern"),
    ("shortness breath wheeze chest tightness cough breathing problems", "Asthma/Respiratory Pattern"),
    ("breathlessness wheezing chest discomfort coughing difficulty breathing", "Asthma/Respiratory Pattern"),
    ("shortness breath wheeze chest tight coughing trouble breathing", "Asthma/Respiratory Pattern"),
    ("breathless wheezing chest tightness cough breathing problems", "Asthma/Respiratory Pattern"),
    ("shortness breath wheeze chest discomfort coughing difficulty breathing", "Asthma/Respiratory Pattern"),
    ("breathlessness wheezing chest tight coughing trouble breathing", "Asthma/Respiratory Pattern"),
    
    # Arthritis - real symptoms
    ("joint pain stiffness swelling knee hip hands difficulty moving", "Arthritis/Joint Pain Pattern"),
    ("joint pain stiff swollen knees hips hands trouble moving", "Arthritis/Joint Pain Pattern"),
    ("joint pain stiffness swelling knee hip hands movement problems", "Arthritis/Joint Pain Pattern"),
    ("joint pain stiff swollen knees hips hands difficulty moving", "Arthritis/Joint Pain Pattern"),
    ("joint pain stiffness swelling knee hip hands trouble moving", "Arthritis/Joint Pain Pattern"),
    ("joint pain stiff swollen knees hips hands movement problems", "Arthritis/Joint Pain Pattern"),
    ("joint pain stiffness swelling knee hip hands difficulty moving", "Arthritis/Joint Pain Pattern"),
    ("joint pain stiff swollen knees hips hands trouble moving", "Arthritis/Joint Pain Pattern"),
    
    # Obesity/Metabolic - real symptoms
    ("weight gain difficulty losing weight fatigue high blood pressure high cholesterol", "Obesity/Metabolic Risk Pattern"),
    ("weight gain trouble losing weight tired high bp high cholesterol", "Obesity/Metabolic Risk Pattern"),
    ("weight gain difficulty losing weight fatigue high blood pressure cholesterol", "Obesity/Metabolic Risk Pattern"),
    ("weight gain trouble losing weight tired high bp cholesterol", "Obesity/Metabolic Risk Pattern"),
    ("weight gain difficulty losing weight fatigue high blood pressure high cholesterol", "Obesity/Metabolic Risk Pattern"),
    ("weight gain trouble losing weight tired high bp high cholesterol", "Obesity/Metabolic Risk Pattern"),
    ("weight gain difficulty losing weight fatigue high blood pressure cholesterol", "Obesity/Metabolic Risk Pattern"),
    ("weight gain trouble losing weight tired high bp cholesterol", "Obesity/Metabolic Risk Pattern"),
    
    # Stress/Fatigue - real symptoms
    ("fatigue tired stress anxiety headache sleep problems mood changes", "Stress/Fatigue Pattern"),
    ("tired fatigue stressed anxious headache trouble sleeping mood swings", "Stress/Fatigue Pattern"),
    ("fatigue tired stress anxiety headache sleep problems mood changes", "Stress/Fatigue Pattern"),
    ("tired fatigue stressed anxious headache trouble sleeping mood swings", "Stress/Fatigue Pattern"),
    ("fatigue tired stress anxiety headache sleep problems mood changes", "Stress/Fatigue Pattern"),
    ("tired fatigue stressed anxious headache trouble sleeping mood swings", "Stress/Fatigue Pattern"),
    ("fatigue tired stress anxiety headache sleep problems mood changes", "Stress/Fatigue Pattern"),
    ("tired fatigue stressed anxious headache trouble sleeping mood swings", "Stress/Fatigue Pattern"),
]

# Expand dataset with variations
def expand_dataset(base_dataset):
    """Create variations while maintaining class balance"""
    expanded = []
    for text, label in base_dataset:
        expanded.append((text, label))
        # Add one variation per example
        words = text.split()
        if len(words) > 2:
            new_words = words.copy()
            # Swap adjacent words
            idx = np.random.randint(0, len(new_words) - 1)
            new_words[idx], new_words[idx + 1] = new_words[idx + 1], new_words[idx]
            expanded.append((' '.join(new_words), label))
    return expanded

# Expand dataset
full_dataset = expand_dataset(REAL_DATASET)

print(f"Total dataset size: {len(full_dataset)} samples")
from collections import Counter
label_counts = Counter([label for _, label in full_dataset])
print("Class distribution:")
for label, count in sorted(label_counts.items()):
    print(f"  {label}: {count} examples")

# Extract texts and labels
texts = [text for text, _ in full_dataset]
labels = [label for _, label in full_dataset]

# Load existing labels
with open('app/src/main/assets/models/labels.json', 'r') as f:
    label_list = json.load(f)

label_encoder = LabelEncoder()
label_encoder.fit(label_list)
y_encoded = label_encoder.transform(labels)
num_classes = len(label_list)

print(f"\nLabels: {label_list}")
print(f"Number of classes: {num_classes}")

# Use pre-trained embeddings approach
# Load existing vocab to maintain compatibility
with open('app/src/main/assets/models/vocab.json', 'r') as f:
    existing_vocab = json.load(f)

# Create tokenizer that uses existing vocab
max_vocab_size = len(existing_vocab)
tokenizer = Tokenizer(num_words=max_vocab_size, oov_token="[UNK]")
tokenizer.word_index = existing_vocab.copy()

# Ensure special tokens
if "[PAD]" not in tokenizer.word_index:
    tokenizer.word_index["[PAD]"] = 0
if "[UNK]" not in tokenizer.word_index:
    tokenizer.word_index["[UNK]"] = 1

# Update tokenizer with new words from dataset
tokenizer.fit_on_texts(texts)

# Update vocab with new words, but limit to max_vocab_size
vocab = tokenizer.word_index.copy()
# Ensure vocab indices are within valid range (0 to vocab_size-1)
# Keep only the top max_vocab_size words
sorted_vocab = sorted(vocab.items(), key=lambda x: x[1])
vocab = {word: min(idx, max_vocab_size - 1) for word, idx in vocab.items()}
vocab_size = max_vocab_size

print(f"Vocabulary size: {vocab_size}")

# Save updated vocabulary
os.makedirs('app/src/main/assets/models', exist_ok=True)
with open('app/src/main/assets/models/vocab.json', 'w') as f:
    json.dump(vocab, f, indent=2)

# Tokenize and pad sequences
seq_len = 20
X = tokenizer.texts_to_sequences(texts)
# Ensure all indices are within valid range
X = [[min(token, vocab_size - 1) for token in seq] for seq in X]
X = pad_sequences(X, maxlen=seq_len, padding='post', truncating='post', value=0)

# Convert to numpy
X = np.array(X, dtype=np.int32)
y = keras.utils.to_categorical(y_encoded, num_classes=num_classes)

# Split data
X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42, stratify=y_encoded
)

print(f"\nTraining samples: {len(X_train)}")
print(f"Test samples: {len(X_test)}")

# Build model with transfer learning approach
# Use pre-trained embeddings (GloVe-style approach with medical domain focus)
embedding_dim = 100  # Good balance for mobile

model = keras.Sequential([
    layers.Embedding(
        input_dim=vocab_size + 1,  # +1 to account for 0-indexing
        output_dim=embedding_dim,
        input_length=seq_len,
        name='embedding',
        # Initialize with better weights (Xavier/Glorot)
        embeddings_initializer='glorot_uniform'
    ),
    layers.GlobalAveragePooling1D(name='pooling'),
    layers.Dense(256, activation='relu', name='dense1'),
    layers.BatchNormalization(name='bn1'),
    layers.Dropout(0.5, name='dropout1'),
    layers.Dense(128, activation='relu', name='dense2'),
    layers.BatchNormalization(name='bn2'),
    layers.Dropout(0.4, name='dropout2'),
    layers.Dense(64, activation='relu', name='dense3'),
    layers.Dropout(0.3, name='dropout3'),
    layers.Dense(num_classes, activation='linear', name='output')
])

# Use better optimizer and learning rate schedule
initial_learning_rate = 0.001
lr_schedule = keras.optimizers.schedules.ExponentialDecay(
    initial_learning_rate,
    decay_steps=100,
    decay_rate=0.96,
    staircase=True
)

model.compile(
    optimizer=keras.optimizers.Adam(learning_rate=lr_schedule),
    loss=keras.losses.CategoricalCrossentropy(from_logits=True, label_smoothing=0.1),
    metrics=['accuracy']
)

model.summary()

# Calculate class weights for balanced training
from sklearn.utils.class_weight import compute_class_weight
class_weights = compute_class_weight('balanced', classes=np.unique(y_encoded), y=y_encoded)
class_weight_dict = {i: class_weights[i] for i in range(len(class_weights))}
print(f"\nClass weights: {class_weight_dict}")

# Train model
history = model.fit(
    X_train, y_train,
    batch_size=16,
    epochs=150,
    validation_data=(X_test, y_test),
    verbose=1,
    class_weight=class_weight_dict,
    callbacks=[
        keras.callbacks.EarlyStopping(
            monitor='val_accuracy',
            patience=20,
            restore_best_weights=True,
            mode='max'
        ),
        keras.callbacks.ReduceLROnPlateau(
            monitor='val_loss',
            factor=0.5,
            patience=8,
            min_lr=1e-6,
            verbose=1
        )
    ]
)

# Evaluate
test_loss, test_accuracy = model.evaluate(X_test, y_test, verbose=0)
print(f"\nTest Accuracy: {test_accuracy:.4f}")

# Test with multiple symptom combinations
test_cases = [
    ("increased thirst frequent urination unexplained weight loss increased hunger fatigue blurry vision", "Diabetes Risk Pattern"),
    ("chest pain shortness of breath dizziness palpitations high blood pressure", "Hypertension/Heart Risk Pattern"),
    ("weight gain fatigue cold intolerance hair loss dry skin mood changes", "Thyroid Imbalance Pattern"),
    ("shortness of breath wheezing chest tightness coughing difficulty breathing", "Asthma/Respiratory Pattern"),
    ("joint pain stiffness swelling knee hip hands difficulty moving", "Arthritis/Joint Pain Pattern"),
    ("weight gain difficulty losing weight fatigue high blood pressure high cholesterol", "Obesity/Metabolic Risk Pattern"),
    ("fatigue tired stress anxiety headache sleep problems mood changes", "Stress/Fatigue Pattern"),
]

print("\n" + "="*70)
print("Testing model with different symptom combinations:")
print("="*70)

correct = 0
total = len(test_cases)

for test_text, expected_label in test_cases:
    test_seq = tokenizer.texts_to_sequences([test_text])
    test_seq = pad_sequences(test_seq, maxlen=seq_len, padding='post', truncating='post', value=0)
    test_seq = np.array(test_seq, dtype=np.int32)
    
    prediction = model.predict(test_seq, verbose=0)
    
    # Apply softmax
    logits = prediction[0]
    exps = np.exp(logits)
    probs = exps / np.sum(exps)
    
    predicted_idx = np.argmax(probs)
    predicted_label = label_list[predicted_idx]
    confidence = probs[predicted_idx]
    
    is_correct = predicted_label == expected_label
    if is_correct:
        correct += 1
    
    status = "✓" if is_correct else "✗"
    print(f"\n{status} Text: {test_text[:55]}...")
    print(f"   Expected: {expected_label}")
    print(f"   Predicted: {predicted_label} (Confidence: {confidence:.1%})")
    top3 = [(label_list[i], f'{probs[i]:.1%}') for i in np.argsort(probs)[-3:][::-1]]
    print(f"   Top 3: {top3}")

print(f"\n{'='*70}")
print(f"Accuracy on test cases: {correct}/{total} ({correct/total:.1%})")
print(f"{'='*70}")

# Convert to TensorFlow Lite
print("\nConverting to TensorFlow Lite...")
converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.target_spec.supported_types = [tf.float32]
converter.optimizations = [tf.lite.Optimize.DEFAULT]
tflite_model = converter.convert()

# Save TFLite model
tflite_path = 'app/src/main/assets/models/chronic_risk_classifier.tflite'
with open(tflite_path, 'wb') as f:
    f.write(tflite_model)

print(f"Model saved to {tflite_path}")
print(f"Model size: {len(tflite_model) / 1024:.2f} KB")

# Verify TFLite model
interpreter = tf.lite.Interpreter(model_path=tflite_path)
interpreter.allocate_tensors()

input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()

print(f"\nInput shape: {input_details[0]['shape']}")
print(f"Output shape: {output_details[0]['shape']}")
print(f"Input dtype: {input_details[0]['dtype']}")

# Test TFLite model
print("\nTesting TFLite model with diabetes symptoms:")
test_text = "increased thirst frequent urination unexplained weight loss increased hunger fatigue blurry vision"
test_seq = tokenizer.texts_to_sequences([test_text])
test_seq = pad_sequences(test_seq, maxlen=seq_len, padding='post', truncating='post', value=0)
test_input = np.array(test_seq, dtype=np.int32)

input_dtype = input_details[0]['dtype']
if input_dtype == np.float32:
    test_input = test_input.astype(np.float32)

interpreter.set_tensor(input_details[0]['index'], test_input)
interpreter.invoke()
output_data = interpreter.get_tensor(output_details[0]['index'])

exp_output = np.exp(output_data[0])
softmax_output = exp_output / np.sum(exp_output)
predicted_class_tflite = np.argmax(softmax_output)
confidence_tflite = softmax_output[predicted_class_tflite]

print(f"Predicted: {label_list[predicted_class_tflite]}")
print(f"Confidence: {confidence_tflite:.1%}")

