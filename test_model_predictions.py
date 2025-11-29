"""
Quick test script to check model predictions for different symptom types
"""
import json
import re
import numpy as np
import tensorflow as tf

# Load vocab and labels
with open('app/src/main/assets/models/vocab.json', 'r') as f:
    vocab = json.load(f)

with open('app/src/main/assets/models/labels.json', 'r') as f:
    labels = json.load(f)

# Load model
interpreter = tf.lite.Interpreter(model_path='app/src/main/assets/models/chronic_risk_classifier.tflite')
interpreter.allocate_tensors()

input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()

def tokenize(text, vocab, seq_len=20):
    tokens = re.sub(r'[^a-z0-9_\s]', ' ', text.lower()).split()
    tokens = [t for t in tokens if t.strip()]
    ids = [vocab.get(token, vocab.get("[UNK]", 1)) for token in tokens]
    if len(ids) >= seq_len:
        ids = ids[:seq_len]
    else:
        ids = ids + [vocab.get("[PAD]", 0)] * (seq_len - len(ids))
    return ids

# Test cases
test_cases = [
    ("increased thirst frequent urination unexplained weight loss", "Diabetes"),
    ("chest pain shortness of breath dizziness palpitations", "Hypertension"),
    ("weight gain fatigue cold intolerance hair loss", "Thyroid"),
    ("shortness of breath wheezing chest tightness coughing", "Asthma"),
    ("joint pain stiffness swelling knee hip", "Arthritis"),
    ("weight gain difficulty losing weight high cholesterol", "Obesity"),
    ("fatigue stress anxiety headache sleep problems", "Stress"),
]

print("Testing current model predictions:\n")
for text, expected in test_cases:
    tokens = np.array([tokenize(text, vocab, seq_len=20)], dtype=np.int32)
    
    # Check input type
    input_dtype = input_details[0]['dtype']
    if input_dtype == np.float32:
        tokens = tokens.astype(np.float32)
    
    interpreter.set_tensor(input_details[0]['index'], tokens)
    interpreter.invoke()
    output = interpreter.get_tensor(output_details[0]['index'])
    
    # Apply softmax
    exp_output = np.exp(output[0])
    probs = exp_output / np.sum(exp_output)
    
    predicted_idx = np.argmax(probs)
    predicted_label = labels[predicted_idx]
    confidence = probs[predicted_idx]
    
    print(f"Input: {text[:50]}...")
    print(f"Expected: {expected}")
    print(f"Predicted: {predicted_label} ({confidence:.1%})")
    print(f"All: {[(labels[i], f'{probs[i]:.1%}') for i in np.argsort(probs)[-3:][::-1]]}")
    print()

