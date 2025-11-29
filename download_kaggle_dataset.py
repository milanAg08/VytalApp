"""
Script to download and use real medical symptom datasets from Kaggle
Requires kaggle API credentials (kaggle.json in ~/.kaggle/)
"""

import os
import json
import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder

def download_kaggle_dataset(dataset_name, download_path='./datasets'):
    """
    Download a dataset from Kaggle
    
    Args:
        dataset_name: Format 'username/dataset-name'
        download_path: Where to save the dataset
    """
    try:
        import kaggle
        from kaggle.api.kaggle_api_extended import KaggleApi
        
        api = KaggleApi()
        api.authenticate()
        
        os.makedirs(download_path, exist_ok=True)
        api.dataset_download_files(dataset_name, path=download_path, unzip=True)
        
        print(f"Dataset {dataset_name} downloaded successfully to {download_path}")
        return True
    except Exception as e:
        print(f"Error downloading dataset: {e}")
        print("Make sure you have kaggle installed: pip install kaggle")
        print("And have kaggle.json in ~/.kaggle/ directory")
        return False

def load_symptom2disease_dataset(dataset_path='./datasets'):
    """
    Load and process symptom2disease dataset
    
    This function maps common disease names to our 7 disease patterns
    """
    # Common mappings from real diseases to our patterns
    disease_mappings = {
        # Diabetes-related
        'diabetes': 'Diabetes Risk Pattern',
        'diabetes mellitus': 'Diabetes Risk Pattern',
        'type 2 diabetes': 'Diabetes Risk Pattern',
        'type 1 diabetes': 'Diabetes Risk Pattern',
        
        # Hypertension/Heart-related
        'hypertension': 'Hypertension/Heart Risk Pattern',
        'high blood pressure': 'Hypertension/Heart Risk Pattern',
        'heart disease': 'Hypertension/Heart Risk Pattern',
        'cardiovascular disease': 'Hypertension/Heart Risk Pattern',
        'coronary artery disease': 'Hypertension/Heart Risk Pattern',
        
        # Thyroid-related
        'hypothyroidism': 'Thyroid Imbalance Pattern',
        'hyperthyroidism': 'Thyroid Imbalance Pattern',
        'thyroid disorder': 'Thyroid Imbalance Pattern',
        'goiter': 'Thyroid Imbalance Pattern',
        
        # Asthma/Respiratory
        'asthma': 'Asthma/Respiratory Pattern',
        'copd': 'Asthma/Respiratory Pattern',
        'chronic obstructive pulmonary disease': 'Asthma/Respiratory Pattern',
        'bronchitis': 'Asthma/Respiratory Pattern',
        
        # Arthritis
        'arthritis': 'Arthritis/Joint Pain Pattern',
        'rheumatoid arthritis': 'Arthritis/Joint Pain Pattern',
        'osteoarthritis': 'Arthritis/Joint Pain Pattern',
        'joint pain': 'Arthritis/Joint Pain Pattern',
        
        # Obesity/Metabolic
        'obesity': 'Obesity/Metabolic Risk Pattern',
        'metabolic syndrome': 'Obesity/Metabolic Risk Pattern',
        'high cholesterol': 'Obesity/Metabolic Risk Pattern',
        
        # Stress/Fatigue
        'anxiety': 'Stress/Fatigue Pattern',
        'depression': 'Stress/Fatigue Pattern',
        'chronic fatigue': 'Stress/Fatigue Pattern',
        'stress': 'Stress/Fatigue Pattern',
    }
    
    # Try to load the dataset
    csv_files = [f for f in os.listdir(dataset_path) if f.endswith('.csv')]
    
    if not csv_files:
        print(f"No CSV files found in {dataset_path}")
        return None, None
    
    # Load the first CSV file
    df = pd.read_csv(os.path.join(dataset_path, csv_files[0]))
    
    print(f"Loaded dataset with {len(df)} rows")
    print(f"Columns: {df.columns.tolist()}")
    
    # Try to identify symptom and disease columns
    symptom_col = None
    disease_col = None
    
    for col in df.columns:
        col_lower = col.lower()
        if 'symptom' in col_lower or 'text' in col_lower or 'description' in col_lower:
            symptom_col = col
        if 'disease' in col_lower or 'condition' in col_lower or 'label' in col_lower:
            disease_col = col
    
    if not symptom_col or not disease_col:
        print("Could not identify symptom and disease columns")
        print("Please check the dataset structure")
        return None, None
    
    # Map diseases to our patterns
    texts = []
    labels = []
    
    for idx, row in df.iterrows():
        symptom_text = str(row[symptom_col]).lower()
        disease_name = str(row[disease_col]).lower()
        
        # Try to map disease to our pattern
        mapped_label = None
        for disease_key, pattern in disease_mappings.items():
            if disease_key in disease_name:
                mapped_label = pattern
                break
        
        if mapped_label:
            texts.append(symptom_text)
            labels.append(mapped_label)
    
    print(f"\nMapped {len(texts)} examples to our disease patterns")
    label_counts = {}
    for label in labels:
        label_counts[label] = label_counts.get(label, 0) + 1
    
    print("Distribution:")
    for label, count in sorted(label_counts.items()):
        print(f"  {label}: {count}")
    
    return texts, labels

if __name__ == "__main__":
    # Example usage:
    # 1. Download dataset from Kaggle
    # dataset_name = "itachi9604/disease-symptom-description-dataset"
    # download_kaggle_dataset(dataset_name)
    
    # 2. Load and process
    # texts, labels = load_symptom2disease_dataset()
    
    print("This script helps download and process Kaggle medical datasets")
    print("To use:")
    print("1. Install kaggle: pip install kaggle")
    print("2. Get API credentials from https://www.kaggle.com/account")
    print("3. Place kaggle.json in ~/.kaggle/ directory")
    print("4. Call download_kaggle_dataset() and load_symptom2disease_dataset()")

