#!/usr/bin/env python3

import json
import sys

def count_json_objects(input_file):
    # Read the input JSON file
    with open(input_file, 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    # Ensure the data is a list
    if isinstance(data, list):
        print(f"{input_file}: Number of objects in the array: {len(data)}")
    else:
        print(f"{input_file}: Invalid JSON format: Expected an array.")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python script.py <input_json_file1> <input_json_file2> ...")
        sys.exit(1)
    
    for input_file in sys.argv[1:]:
        count_json_objects(input_file)
