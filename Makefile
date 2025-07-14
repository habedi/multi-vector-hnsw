# Use the Maven wrapper if it exists, otherwise fall back to a system-wide mvn
MVN := $(if $(wildcard ./mvnw),./mvnw,mvn)
SHELL := /bin/bash

# Default log level, can be overridden from the command line
LOG_LEVEL ?= warn

# Benchmark data directory
BENCHMARK_DATA_DIR ?= benches/multi-vector-hnsw-datasets

# Default benchmark dataset
BENCHMARK_DATASET ?= se_p_768

# Default target executed when 'make' is run without arguments
.DEFAULT_GOAL := help

# Phony targets don't represent files
.PHONY: help build package publish test format format-check lint clean setup-hooks \
 test-hooks bench-data bench-jar bench-run

help: ## Show this help message
	@echo "Usage: make <target>"
	@echo ""
	@echo "Targets:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' Makefile | \
	awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2}'

build: ## Run the full Maven build lifecycle (compile, check, test, and package)
	@echo "Building project and running all checks..."
	@$(MVN) -B verify

package: ## Compile and package the library into a JAR file
	@echo "Packaging project into JAR file..."
	@$(MVN) -B package

publish: ## Deploys the release artifacts to Maven Central
	@echo "Deploying to Maven Central..."
	@$(MVN) -B deploy -P release

test: ## Run tests (e.g., make test LOG_LEVEL=debug)
	@echo "Running tests with log level: $(LOG_LEVEL)..."
	@$(MVN) -B verify -Dmv.hnsw.log.level=$(LOG_LEVEL)

format: ## Format Java source files
	@echo "Formatting source code..."
	@$(MVN) -B spotless:apply

format-check: ## Check code formatting without applying changes
	@echo "Checking code formatting..."
	@$(MVN) -B spotless:check

lint: ## Check code style
	@echo "Checking code style..."
	@$(MVN) -B checkstyle:check

clean: ## Remove all build artifacts
	@echo "Cleaning project..."
	@$(MVN) -B clean

setup-hooks: ## Set up pre-commit hooks
	@echo "Setting up pre-commit hooks..."
	@if ! command -v pre-commit &> /dev/null; then \
	   echo "pre-commit not found. Please install it using 'pip install pre-commit'"; \
	   exit 1; \
	fi
	@pre-commit install --install-hooks

test-hooks: ## Test pre-commit hooks on all files
	@echo "Testing pre-commit hooks..."
	@./scripts/test_precommit_hooks.sh

bench-data: ## Download the benchmark datasets
	@echo "Downloading the datasets used for benchmarks..."
	@$(SHELL) scripts/download_benchmark_datasets.sh

bench-jar: ## Build the benchmark JAR file
	@echo "Building benchmark JAR file..."
	@$(MVN) clean package -Pbenchmark

bench-run: bench-data bench-jar ## Run benchmarks (e.g., make bench-run LOG_LEVEL=debug)
	@echo "Running benchmarks with log level $(LOG_LEVEL)..."
	@java --add-modules jdk.incubator.vector -Dmv.hnsw.log.level=$(LOG_LEVEL) \
	-jar target/benchmarks.jar --dataset $(BENCHMARK_DATASET) --data-path $(BENCHMARK_DATA_DIR) \
	--profiler "stack"
