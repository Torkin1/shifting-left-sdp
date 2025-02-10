#!/usr/bin/env python

import pandas as pd
import os

OUTPUT_DIR = os.path.join("..", "data", "output", "measurements", "nlp4re-replaced")

# Function to replace values in csv1 with values from csv2 for specified columns
def replace_values_from_csv(csv1_path, csv2_path, columns_to_replace):
    # Load both CSVs into DataFrames
    df1 = pd.read_csv(csv1_path)
    df2 = pd.read_csv(csv2_path)
    
    # Replace values in the specified columns in df1 with values from df2
    for column in columns_to_replace:
        if column in df1.columns and column in df2.columns:
            df1[column] = df2[column]
    
    # Save the modified df1 to a new CSV
    file_name = os.path.basename(csv1_path)
    df1.to_csv(os.path.join(OUTPUT_DIR, file_name), index=False)
    print(f"{file_name} updated successfully using {os.path.basename(csv2_path)} values.")

# Function to replace values with "?" in specified columns
def replace_with_question_mark(csv_path, columns_to_mask):
    # Load the CSV into a DataFrame
    df = pd.read_csv(csv_path)
    
    # Replace values in the specified columns with "?"
    for column in columns_to_mask:
        if column in df.columns:
            df[column] = "?"
    
    # Save the modified DataFrame to a new CSV
    file_name = os.path.basename(csv_path)
    df.to_csv(os.path.join(OUTPUT_DIR, file_name), index=False)
    print(f"{file_name} updated with '?' in specified columns.")

# List of columns to replace from CSV2 into CSV1
columns_to_replace = [
    "Intrinsic:nlp4re_description-DA_ACT", "Internal_Temperature:nlp4re_sentiment-CM_ONS", 
    "Internal_Temperature:nlp4re_sentiment-IT_POL", "Internal_Temperature:nlp4re_sentiment-IT_SUB", 
    "Intrinsic:nlp4re_description-DA_OPT", "Intrinsic:nlp4re_description-EX_SBJ", 
    "Intrinsic:nlp4re_description-EX_RDS", "Intrinsic:nlp4re_description-EX_ACD", 
    "Intrinsic:nlp4re_description-EX_CNS", "Intrinsic:nlp4re_description-EX_ENT", 
    "Intrinsic:nlp4re_description-DA_WKP", "Intrinsic:nlp4re_description-EX_ICP", 
    "Intrinsic:nlp4re_description-DA_RKL", "Intrinsic:nlp4re_description-DA_IMP", 
    "Internal_Temperature:nlp4re_sentiment-CM_NNS", "Intrinsic:nlp4re_description-DA_CND", 
    "Intrinsic:nlp4re_description-EX_DIR", "Intrinsic:nlp4re_description-EX_AMG", 
    "Intrinsic:nlp4re_description-EX_VRB", "Internal_Temperature:nlp4re_sentiment-CM_PNS", 
    "Intrinsic:nlp4re_description-DA_INC", "Intrinsic:nlp4re_description-DA_CNT", 
    "Intrinsic:nlp4re_description-DA_SRC"
]

# List of columns to replace values with "?"
columns_to_mask = [
    "Internal_Temperature:nlp4re_sentiment-CM_ONS", 
    "Internal_Temperature:nlp4re_sentiment-IT_POL", 
    "Internal_Temperature:nlp4re_sentiment-IT_SUB", 
    "Internal_Temperature:nlp4re_sentiment-CM_NNS", 
    "Internal_Temperature:nlp4re_sentiment-CM_PNS"
]

if __name__ == "__main__":

    if not os.path.isdir(OUTPUT_DIR):
        os.makedirs(OUTPUT_DIR)

    replace_values_from_csv('/home/daniele/Documenti/Universita/magistrale/TESI/shifting-left-sdp/dataminer/backend/data/output/measurements/preprocessed/apachejit_HBASE_OneSecondAfterLastCommitDate_T.csv', '/home/daniele/Documenti/Universita/magistrale/TESI/shifting-left-sdp/dataminer/backend/data/output/measurements/preprocessed/apachejit_HBASE_OneSecondBeforeFirstCommitDate_T.csv', columns_to_replace)
    replace_values_from_csv('/home/daniele/Documenti/Universita/magistrale/TESI/shifting-left-sdp/dataminer/backend/data/output/measurements/preprocessed/apachejit_HBASE_OneSecondBeforeFirstAssignmentDate_T.csv', '/home/daniele/Documenti/Universita/magistrale/TESI/shifting-left-sdp/dataminer/backend/data/output/measurements/preprocessed/apachejit_HBASE_OneSecondBeforeFirstCommitDate_T.csv', columns_to_replace)
    replace_values_from_csv('/home/daniele/Documenti/Universita/magistrale/TESI/shifting-left-sdp/dataminer/backend/data/output/measurements/preprocessed/apachejit_HBASE_OneSecondBeforeFirstCommitDate_T.csv', '/home/daniele/Documenti/Universita/magistrale/TESI/shifting-left-sdp/dataminer/backend/data/output/measurements/preprocessed/apachejit_HBASE_OneSecondBeforeFirstCommitDate_T.csv', columns_to_replace)
    replace_values_from_csv('/home/daniele/Documenti/Universita/magistrale/TESI/shifting-left-sdp/dataminer/backend/data/output/measurements/preprocessed/apachejit_HIVE_OneSecondAfterLastCommitDate_T.csv', '/home/daniele/Documenti/Universita/magistrale/TESI/shifting-left-sdp/dataminer/backend/data/output/measurements/preprocessed/apachejit_HIVE_OneSecondBeforeFirstCommitDate_T.csv', columns_to_replace)
    replace_values_from_csv('/home/daniele/Documenti/Universita/magistrale/TESI/shifting-left-sdp/dataminer/backend/data/output/measurements/preprocessed/apachejit_HIVE_OneSecondBeforeFirstAssignmentDate_T.csv', '/home/daniele/Documenti/Universita/magistrale/TESI/shifting-left-sdp/dataminer/backend/data/output/measurements/preprocessed/apachejit_HIVE_OneSecondBeforeFirstCommitDate_T.csv', columns_to_replace)
    replace_values_from_csv('/home/daniele/Documenti/Universita/magistrale/TESI/shifting-left-sdp/dataminer/backend/data/output/measurements/preprocessed/apachejit_HIVE_OneSecondBeforeFirstCommitDate_T.csv', '/home/daniele/Documenti/Universita/magistrale/TESI/shifting-left-sdp/dataminer/backend/data/output/measurements/preprocessed/apachejit_HIVE_OneSecondBeforeFirstCommitDate_T.csv', columns_to_replace)

    replace_with_question_mark('/home/daniele/Documenti/Universita/magistrale/TESI/shifting-left-sdp/dataminer/backend/data/output/measurements/nlp4re-replaced/apachejit_HBASE_OneSecondBeforeFirstAssignmentDate_T.csv', columns_to_mask)
    replace_with_question_mark('/home/daniele/Documenti/Universita/magistrale/TESI/shifting-left-sdp/dataminer/backend/data/output/measurements/nlp4re-replaced/apachejit_HIVE_OneSecondBeforeFirstAssignmentDate_T.csv', columns_to_mask)

