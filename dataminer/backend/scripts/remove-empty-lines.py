#!/usr/bin/env python

import sys

def remove_empty_lines(input_file, output_file):
    try:
        with open(input_file, 'r') as infile, open(output_file, 'w') as outfile:
            for line in infile:
                if line.strip():  # Check if the line is not empty
                    outfile.write(line)
        print(f"Empty lines removed. Cleaned file saved to: {output_file}")
    except FileNotFoundError:
        print(f"Error: File not found: {input_file}")
    except Exception as e:
        print(f"An error occurred: {e}")

if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Usage: python remove_empty_lines.py <input_file> <output_file>")
    else:
        input_file = sys.argv[1]
        output_file = sys.argv[2]
        remove_empty_lines(input_file, output_file)
