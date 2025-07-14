## Benchmarking Multi-Vector HNSW

The code for the benchmarks is in [`src/benchmark`](../src/benchmark/java/io/github/habedi/mvhnsw/bench) directory.
The benchmarks primarily measure average build time and search time (in milliseconds) as well as recall@k (with k=100) for a given dataset.

## Datasets

To run the benchmarks, you need to download a few datasets from the link below and put them inside
`benches/multi-vector-hnsw-datasets` directory.

- [habedi/multi-vector-hnsw-datasets](https://huggingface.co/datasets/habedi/multi-vector-hnsw-datasets)

Alternatively, you can use [huggingface-cli](https://huggingface.co/docs/huggingface_hub/en/guides/cli) to download the datasets.

```shell
huggingface-cli download habedi/multi-vector-hnsw-datasets --repo-type dataset \
  --local-dir multi-vector-hnsw-datasets
```

Note that the command must be run inside this directory (`benches`).

For convenience, you can use the [`pyproject.toml`](../pyproject.toml) file to set up a Python environment with the
required dependencies, including `huggingface_hub`.
