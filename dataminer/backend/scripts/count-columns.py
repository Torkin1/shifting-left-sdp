#!/usr/bin/env python

import csv
import sys

def count_columns_in_csv(file_path):
    """
    Counts the number of columns in the first row of a CSV file.
    """
    try:
        with open(file_path, mode='r', newline='', encoding='utf-8') as csv_file:
            reader = csv.reader(csv_file)
            # Get the first row to determine the number of columns
            header = next(reader, None)
            return len(header) if header else 0
    except FileNotFoundError:
        print(f"Error: The file '{file_path}' does not exist.")
        sys.exit(1)
    except Exception as e:
        print(f"An error occurred: {e}")
        sys.exit(1)

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python count_columns.py <path_to_csv>")
        sys.exit(1)
    
    file_path = sys.argv[1]
    column_count = count_columns_in_csv(file_path)
    print(f"The CSV file has {column_count} columns.")
