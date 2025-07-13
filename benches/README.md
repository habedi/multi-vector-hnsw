## Benchmarking Multi-Vector HNSW

The implementation of benchmarks are in [`src/benchmark`](../src/benchmark/java/io/github/habedi/mvhnsw/bench) directory.
The benchmarks primarily measure average build time and search time (in milliseconds) and recall@k (with k=100) for given dataset.

## Datasets

To run the benchmarks, you need to download the datasets used in the benchmarks and put them inside the `benches` directory.
You can download the datasets used in the benchmarks from Hugging Face via the following link:

- [habedi/multi-vector-hnsw-datasets](https://huggingface.co/datasets/habedi/multi-vector-hnsw-datasets)

### Using Hugging Face CLI Client

Alternatively, You can use [huggingface-cli](https://huggingface.co/docs/huggingface_hub/en/guides/cli) to download the datasets.

```shell
huggingface-cli download habedi/multi-vector-hnsw-datasets --repo-type dataset \
  --local-dir multi-vector-hnsw-datasets
```

Note that the command must be run inside this directory (`benches`).

For convenience, you can use the [`pyproject.toml`](../pyproject.toml) file to set up a Python environment with the
required dependencies using [Poetry](https://python-poetry.org/), [`uv`](https://docs.astral.sh/uv/),
or any other modern Python environment manager.
