#! /usr/bin/env python

import sys
import pandas as pd
import os

INPUT_DIR = os.path.join("..", "data", "output", "measurements")
OUTPUT_DIR = os.path.join(INPUT_DIR, "preprocessed")

def preprocess(inputs):

    for input_file in inputs:

        output_file = os.path.join(OUTPUT_DIR, input_file)
        input_file = os.path.join(INPUT_DIR, input_file)

        # List of columns to keep
        # List of columns to keep (updated header name included)
        columns_to_keep = [
            "R2R:buggy_similarity-MaxSimilarity_Levenshtein_Title",
            "R2R:buggy_similarity-AvgSimilarity_TFIDFCosine_Title", 
            "R2R:buggy_similarity-AvgSimilarity_BagOfWords_Cosine_Text"
        ]

        # Read the CSV
        df = pd.read_csv(input_file)

        # Replace whitespaces in column headers with underscores only if header doesn't start with "JIT:"
        df.columns = [
            col if col.startswith("JIT:") else col.replace(" ", "_") for col in df.columns
        ]
        input_file_tokens = input_file.split("_")
        prediction_target = input_file_tokens[-1]

        if prediction_target != "JIT":

            # Filter out columns containing "Similarity" that are not in columns_to_keep
            columns_to_remove = [
                col for col in df.columns if "Similarity" in col and col not in columns_to_keep
            ]
            df_filtered = df.drop(columns=columns_to_remove)

            # Delete the specified column
            df_filtered.drop(columns=["Internal_Temperature:activities-work_items_Count"], errors='ignore', inplace=True)

            # Save the resulting dataframe to a new CSV
            df_filtered.to_csv(output_file, index=False)

        print(f"Processed CSV saved to {output_file}")

if __name__ == "__main__":
    input_files = os.listdir(INPUT_DIR)
    input_files = [input for input in input_files if ".csv" in input]
    if not os.path.isdir(OUTPUT_DIR):
        os.makedirs(OUTPUT_DIR)
    preprocess(input_files)
