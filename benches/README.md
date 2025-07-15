## Benchmarking Multi-Vector HNSW

The code for the benchmarks is in [src/benchmark](../src/benchmark/java/io/github/habedi/mvhnsw/bench) directory.
Check out [BenchmarkCLI.java](../src/benchmark/java/io/github/habedi/mvhnsw/bench/BenchmarkCLI.java) for default parameters and options.
The benchmarks primarily measure average build time and search time (in milliseconds) as well as recall@k (with k=100) for a given dataset.

Execute `make bench-run BENCHMARK_DATASET=<dataset_name>` to start the benchmarks for a specified dataset.
At the moment, `<dataset_name>` can be one of `se_cs_768`, `se_ds_768`, or `se_p_768`.

The commands below will run the benchmarks for all three datasets.
(They must be run inside the root directory of the project.)

```shell
make bench-run BENCHMARK_DATASET=se_cs_768 ARGS="--ef-search=100"
make bench-run BENCHMARK_DATASET=se_ds_768 ARGS="--ef-search=100"
make bench-run BENCHMARK_DATASET=se_p_768 ARGS="--ef-search=100"
```

> [!NOTE]
> These benchmarks are mainly to verify the implementation's correctness and measure the raw throughput of the library on a local machine.
> In more realistic scenarios, you would typically use the library with your own datasets and measure performance based on your specific use
> case.

### Datasets

To run the benchmarks, you need to download the datasets available from the link below and put them inside
`benches/multi-vector-hnsw-datasets` directory.

- [habedi/multi-vector-hnsw-datasets](https://huggingface.co/datasets/habedi/multi-vector-hnsw-datasets)

Alternatively, you can use [huggingface-cli](https://huggingface.co/docs/huggingface_hub/en/guides/cli) to download the datasets.

```shell
huggingface-cli download habedi/multi-vector-hnsw-datasets --repo-type dataset \
  --local-dir multi-vector-hnsw-datasets
```

Note that the command must be run inside this directory (`benches`).

> [!NOTE]
> You can also use `make bench-data` to download the benchmark datasets automatically.
> However, you need to have [huggingface-cli](https://huggingface.co/docs/huggingface_hub/en/guides/cli) installed.
> You can set up a Python environment with `huggingface-cli` using the provided [pyproject.toml](../pyproject.toml) file.
