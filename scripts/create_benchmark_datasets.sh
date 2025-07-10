#!/bin/bash

# This script runs the ground truth generation for all specified datasets
# by loading the raw Parquet files from the habedi/multi-vector-search-datasets repo.

set -e # Exit immediately if a command fails.

PYTHON_SCRIPT="scripts/create_ground_truth.py"
DATASET_REPO="habedi/multi-vector-search-datasets"
OUTPUT_DIR="benches/data" # Define output directory here
SAMPLE_SIZE=1000 # Define sample size here
K_VALUE=100 # Define K here


# Check if the python script exists
if [ ! -f "$PYTHON_SCRIPT" ]; then
    echo "Error: Python script '$PYTHON_SCRIPT' not found."
    exit 1
fi

# Create the output directory
echo "Ensuring output directory '$OUTPUT_DIR' exists..."
mkdir -p "$OUTPUT_DIR"

echo "--- Starting Ground Truth Generation for All Datasets (k=$K_VALUE) ---"

# --- Stack Exchange Datasets ---
STACK_EXCHANGE_FILES=("se_cs_768.parquet" "se_ds_768.parquet" "se_p_768.parquet")

for file in "${STACK_EXCHANGE_FILES[@]}"; do
    echo -e "\nProcessing Stack Exchange file: $file"
    echo "======================================================"
    python3 "$PYTHON_SCRIPT" \
        --dataset_name "$DATASET_REPO" \
        --data_file "$file" \
        --id_column "id" \
        --sample_size $SAMPLE_SIZE \
        --k $K_VALUE \
        --output_dir "$OUTPUT_DIR"
done

## --- Flickr8k Dataset ---
#FLICKR_FILE="flickr8k_768.parquet"
#echo -e "\nProcessing Flickr8k file: $FLICKR_FILE"
#echo "======================================================"
#python3 "$PYTHON_SCRIPT" \
#    --dataset_name "$DATASET_REPO" \
#    --data_file "$FLICKR_FILE" \
#    --id_column "image" \
#    --sample_size $SAMPLE_SIZE \
#    --k $K_VALUE \
#    --output_dir "$OUTPUT_DIR"

echo -e "\n--- All tasks completed successfully! ---"
