#!/usr/bin/env python

import csv
import sys

FEATURE_FAMILY_SEPARATOR = ":"
FEATURE_VARIANT_SEPARATOR = "-"

def has_family(feature):
    return FEATURE_FAMILY_SEPARATOR in feature

def count_columns_in_csv(file_path):
    """
    Counts the number of columns in the first row of a CSV file.
    """
    try:
        with open(file_path, mode='r', newline='', encoding='utf-8') as csv_file:
            counts = {}
            nlp4re_counts = {}
            reader = csv.reader(csv_file)
            # Get the first row to determine the number of columns
            header = next(reader, None)
            counts["all"] = len(header) if header else 0

            # group by feature family
            for feature in header:
                if has_family(feature):
                    family = feature.split(FEATURE_FAMILY_SEPARATOR)[0]
                else:
                    family = "Target"
                if family not in counts:
                    counts[family] = 0
                counts[family] += 1

                if "nlp4re" in feature:
                    feature = feature.split(FEATURE_FAMILY_SEPARATOR)[1].split(FEATURE_VARIANT_SEPARATOR)[0]
                    if feature not in nlp4re_counts:
                        nlp4re_counts[feature] = 0
                    nlp4re_counts[feature] += 1    
            return counts, nlp4re_counts

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
    counts, nlp4re_counts = count_columns_in_csv(file_path)
    print(f"counts: {counts}")
    print(f"nlp4re_counts: {nlp4re_counts}")
