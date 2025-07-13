## Benchmark Datasets

You can download the datasets used in the benchmarks from Hugging Face.

- [habedi/multi-vector-hnsw-datasets](https://huggingface.co/datasets/habedi/multi-vector-hnsw-datasets)

Make sure that the downloaded datasets are stored in the `benches/data` directory.

### Using Hugging Face CLI Client

You can use [huggingface-cli](https://huggingface.co/docs/huggingface_hub/en/guides/cli) to download the datasets.

```shell
huggingface-cli download habedi/multi-vector-hnsw-datasets --repo-type dataset \
  --local-dir multi-vector-hnsw-datasets
```

The command must be run inside this directory (`benches`).

For convenience, you can use the [`pyproject.toml`](../pyproject.toml) file to set up a Python environment with the
required dependencies using [Poetry](https://python-poetry.org/), [`uv`](https://docs.astral.sh/uv/),
or any other modern Python environment manager.
