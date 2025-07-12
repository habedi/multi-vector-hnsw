import argparse
import json
import numpy as np
import os
from datasets import load_dataset
from tqdm import tqdm


def euclidean_distance(v1, v2):
    return np.linalg.norm(v1 - v2)


def cosine_distance(v1, v2):
    dot_product = np.dot(v1, v2)
    norm_v1 = np.linalg.norm(v1)
    norm_v2 = np.linalg.norm(v2)

    if norm_v1 == 0.0 or norm_v2 == 0.0:
        return 1.0

    similarity = dot_product / (norm_v1 * norm_v2)
    return 1.0 - similarity


def calculate_aggregated_distance(emb1, emb2, weights, distance_func):
    total_dist = 0.0
    for i in range(len(weights)):
        dist = distance_func(emb1[i], emb2[i])
        total_dist += dist * weights[i]
    return round(total_dist, 6)


def validate_and_clean_data(dataset, id_column, embedding_column):
    clean_data = []
    expected_dim = None

    # First pass to find a valid dimension
    for item in dataset:
        emb = item[embedding_column]
        if isinstance(emb, list) and emb and isinstance(emb[0], list) and emb[0]:
            expected_dim = len(emb[0])
            break

    if expected_dim is None:
        raise ValueError("Could not determine a valid embedding dimension from the dataset.")

    print(f"Validating data (expected dimension: {expected_dim})...")
    for item in tqdm(dataset, desc="Validating data"):
        raw_embedding = item[embedding_column]
        item_id = item[id_column]

        is_valid = True
        if not isinstance(raw_embedding, list) or not raw_embedding:
            is_valid = False
        else:
            for v in raw_embedding:
                if not isinstance(v, list) or len(v) != expected_dim:
                    is_valid = False
                    break

        if not is_valid:
            print(f"Skipping item with id '{item_id}' due to inhomogeneous embedding shape.")
            continue

        clean_data.append({"id": item_id, "embedding": raw_embedding})

    return clean_data


def find_ground_truth(clean_test_data, clean_train_data, k):
    ground_truth_results = []

    train_embeddings = [np.array(item["embedding"]) for item in clean_train_data]
    train_ids = [item["id"] for item in clean_train_data]

    if not train_embeddings:
        print("Error: No valid training data found after validation.")
        return []

    num_vectors = len(train_embeddings[0])
    weights = [1.0 / num_vectors] * num_vectors

    print(f"Finding {k} nearest neighbors for {len(clean_test_data)} test items...")
    for test_item in tqdm(clean_test_data, desc="Finding neighbors"):
        test_id = test_item["id"]
        test_emb = np.array(test_item["embedding"])

        euclidean_neighbors = []
        cosine_neighbors = []

        for i in range(len(train_ids)):
            train_id = train_ids[i]
            train_emb = train_embeddings[i]

            dist_euc = calculate_aggregated_distance(test_emb, train_emb, weights,
                                                     euclidean_distance)
            dist_cos = calculate_aggregated_distance(test_emb, train_emb, weights, cosine_distance)

            euclidean_neighbors.append({"id": train_id, "dist": dist_euc})
            cosine_neighbors.append({"id": train_id, "dist": dist_cos})

        euclidean_neighbors.sort(key=lambda x: x["dist"])
        cosine_neighbors.sort(key=lambda x: x["dist"])

        top_k_euc = euclidean_neighbors[:k]
        top_k_cos = cosine_neighbors[:k]

        ground_truth_results.append({
            "id": test_id,
            f"top_{k}_euclidean": {
                "ids": [n['id'] for n in top_k_euc],
                "dists": [n['dist'] for n in top_k_euc]
            },
            f"top_{k}_cosine": {
                "ids": [n['id'] for n in top_k_cos],
                "dists": [n['dist'] for n in top_k_cos]
            }
        })

    return ground_truth_results


def main(args):
    split = "train"
    if args.sample_size:
        split = f"train[:{args.sample_size}]"

    print(f"Loading dataset: {args.dataset_name}, data file: {args.data_file}, split:"
          f" {split}, num samples: {args.sample_size or 'full'}")
    dataset = load_dataset(args.dataset_name, data_files=args.data_file, split=split)

    if len(dataset) < 10:
        print("Error: Sample size is too small to create a train/test split. Minimum is 10.")
        return

    print(f"Loaded {len(dataset)} samples from the dataset.")

    print("Splitting dataset into 90% train and 10% test...")
    split_dataset = dataset.train_test_split(test_size=0.1, seed=42)

    # --- Clean and prepare data ---
    clean_train_data = validate_and_clean_data(split_dataset["train"], args.id_column,
                                               args.embedding_column)
    clean_test_data = validate_and_clean_data(split_dataset["test"], args.id_column,
                                              args.embedding_column)

    # --- Find Neighbors ---
    ground_truth_data = find_ground_truth(
        clean_test_data,
        clean_train_data,
        k=args.k
    )

    # --- Create directories and save files ---
    dataset_folder_name = os.path.splitext(args.data_file)[0]
    full_output_dir = os.path.join(args.output_dir, dataset_folder_name)
    os.makedirs(full_output_dir, exist_ok=True)

    print(f"Saving files to '{full_output_dir}' directory...")

    with open(os.path.join(full_output_dir, "train.json"), "w") as f:
        json.dump(clean_train_data, f)

    with open(os.path.join(full_output_dir, "test.json"), "w") as f:
        json.dump(clean_test_data, f)

    with open(os.path.join(full_output_dir, "neighbours.json"), "w") as f:
        json.dump(ground_truth_data, f, indent=2)

    print("Done.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description="Generate ground truth for multi-vector datasets from raw Parquet files."
    )
    parser.add_argument("--dataset_name", type=str, default="habedi/multi-vector-search-datasets")
    parser.add_argument("--data_file", type=str, required=True)
    parser.add_argument("--id_column", type=str, default="id")
    parser.add_argument("--embedding_column", type=str, default="embedding")
    parser.add_argument("--sample_size", type=int, default=None)
    parser.add_argument("--k", type=int, default=100)
    parser.add_argument("--output_dir", type=str, default="output_data")

    args = parser.parse_args()
    main(args)
