#!/usr/bin/env python3

import json
import sys
import os

def split_json_by_measurement(input_file):
    # Define the valid measurementDateName values
    valid_dates = [
        "OneSecondBeforeFirstAssignmentDate",
        "OneSecondBeforeFirstCommitDate",
        "OneSecondAfterLastCommitDate"
    ]
    
    # Read the input JSON file
    with open(input_file, 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    # Create a dictionary to store filtered data
    filtered_data = {key: [] for key in valid_dates}
    
    # Categorize JSON objects
    for obj in data:
        measurement_date = obj.get("measurementDateName")
        if measurement_date in valid_dates:
            filtered_data[measurement_date].append(obj)
    
    # Write to separate files
    base_name = os.path.splitext(input_file)[0]
    for key, value in filtered_data.items():
        output_file = f"{base_name}_{key}.json"
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(value, f, indent=4)
        print(f"Created: {output_file}")

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python script.py <input_json_file>")
        sys.exit(1)
    
    input_file = sys.argv[1]
    split_json_by_measurement(input_file)
