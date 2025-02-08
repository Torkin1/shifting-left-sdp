#! /usr/bin/env python
# 
# Denormalize the preprocessed data by reversing the log transformation.

import csv
import math
import os

INPUT_DIR = os.path.join("..", "data", "output", "measurements", "preprocessed")
OUTPUT_DIR = os.path.join("..", "data", "output", "measurements", "denormalized")

def reverse_log_csv(input_file, output_file):
    with open(input_file, mode='r', newline='', encoding='utf-8') as infile:
        reader = csv.reader(infile)
        data = []
        for row in reader:
            new_row = []
            for value in row:
                try:
                    num = float(value)
                    original_value = 10 ** num  # Reverse log base 10
                    new_row.append(original_value)
                except ValueError:
                    new_row.append(value)  # Non-numeric data remains unchanged
            data.append(new_row)

    with open(output_file, mode='w', newline='', encoding='utf-8') as outfile:
        writer = csv.writer(outfile)
        writer.writerows(data)

if __name__ == "__main__":
    input_files = os.listdir(INPUT_DIR)
    if not os.path.isdir(OUTPUT_DIR):
        os.makedirs(OUTPUT_DIR)
    for input_file in input_files:
        input_file_path = os.path.join(INPUT_DIR, input_file)
        output_file_path = os.path.join(OUTPUT_DIR, input_file)
        reverse_log_csv(input_file_path, output_file_path)


