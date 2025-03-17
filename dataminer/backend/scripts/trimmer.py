#!/usr/bin/env python3

import csv
import os
import sys
from typing import List


def get_csv_length(file_path: str) -> int:
    """
    Get the number of rows in a CSV file (excluding the header).
    """
    with open(file_path, mode='r', newline='', encoding='utf-8') as file:
        reader = csv.reader(file)
        next(reader)  # Skip header
        return sum(1 for _ in reader)


def trim_csv(file_path: str, target_length: int) -> None:
    """
    Trim the CSV file to the specified number of rows (excluding the header).
    """
    with open(file_path, mode='r', newline='', encoding='utf-8') as file:
        reader = list(csv.reader(file))
        header, rows = reader[0], reader[1:]

    trimmed_rows = rows[:target_length]

    output_file = os.path.splitext(file_path)[0] + '_trimmed.csv'
    with open(output_file, mode='w', newline='', encoding='utf-8') as file:
        writer = csv.writer(file)
        writer.writerow(header)
        writer.writerows(trimmed_rows)

    print(f"Trimmed file saved as: {output_file}")


def main(file_paths: List[str]) -> None:
    """
    Main function to process all files and trim them to the same number of rows.
    """
    if not file_paths:
        print("No file paths provided.")
        return

    # Get the number of rows in each file
    file_lengths = {path: get_csv_length(path) for path in file_paths}

    # Determine the shortest file length
    min_length = min(file_lengths.values())

    # Trim each file to the minimum length
    for file_path in file_paths:
        trim_csv(file_path, min_length)


if __name__ == "__main__":
    main(sys.argv[1:])
