#!/usr/bin/env python3

import csv
import os
import sys

OUTPUT_DIR = os.path.join("..", "data", "output", "measurements", "filtered")

def parse_csv_and_get_set(csv_file: str, column_name: str) -> set:
    """
    Parses the CSV file and retrieves all distinct values from the specified column as a set.
    """
    values_set = set()
    with open(csv_file, mode='r', newline='', encoding='utf-8') as file:
        reader = csv.DictReader(file)
        for row in reader:
            values_set.add(row[column_name])
    return values_set


def filter_csv_by_set(input_csv: str, output_csv: str, filter_set: set, key_column: str):
    """
    Filters the input CSV, removing rows where the key_column's value is not present in the filter_set.
    Writes the filtered rows to the output CSV.
    """
    with open(input_csv, mode='r', newline='', encoding='utf-8') as infile, \
            open(output_csv, mode='w', newline='', encoding='utf-8') as outfile:

        reader = csv.DictReader(infile)
        writer = csv.DictWriter(outfile, fieldnames=reader.fieldnames)

        writer.writeheader()
        for row in reader:
            if row[key_column] in filter_set:
                writer.writerow(row)


def main():
    # Define your file paths
    requirement_csv = '/home/daniele/Documenti/Universita/laurea_magistrale/TESI/shifting-left-sdp/dataminer/backend/src/main/resources/nlp4re/issue-beans_OneSecondAfterLastCommitDate.csv'
    inputs_csv = [input for input in sys.argv[1:]]

    # Parse the requirement file to get the Requirement ID set
    requirement_set = parse_csv_and_get_set(requirement_csv, "Requirement ID")

    # Filter the input file based on the Requirement ID set
    for input_csv in inputs_csv:
        output_csv = os.path.join(OUTPUT_DIR, f"{os.path.basename(input_csv)}_filtered.csv")
        filter_csv_by_set(input_csv, output_csv, requirement_set, "key")


if __name__ == "__main__":
    if not os.path.isdir(OUTPUT_DIR):
        os.makedirs(OUTPUT_DIR)
    main()
