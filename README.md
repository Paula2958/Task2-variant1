
# Exploring Graph Representations

Big Data — Graph Exercises, Variant 1

## Project overview

This project compares adjacency lists and packed-bit
adjacency matrices in synthetic social networks.

The goal is to study how graph size and density
influence storage requirements and query efficiency.

## Experimental configuration

Graphs are generated using the undirected
Erdős–Rényi G(n,p) model without self-loops.

Graph sizes:

- 250 vertices
- 500 vertices
- 1,000 vertices
- 2,000 vertices
- 4,000 vertices

Edge probabilities:

- p = 0.005: relatively sparse
- p = 0.20: dense

Both representations receive identical edge sets.

## Implementations

### Adjacency lists

Implemented using primitive Java int[][] arrays.

Memory complexity: O(V + E).

Edge-existence queries require scanning the
source vertex's neighbors.

### Packed adjacency matrices

Implemented using Java BitSet[].

Memory complexity: O(V²) bits in the worst case.

Edge-existence queries use direct bit access.

## Benchmark methodology

Each graph configuration uses:

- 10,000 shared random queries
- 2 warm-up passes
- 5 recorded measurement trials
- System.nanoTime() timing
- Median execution-time aggregation

Correctness checks compare vertex degrees and
2,000 sampled edge-existence queries.

Graph generation and representation construction
are excluded from query timings.

## Requirements

- Java JDK 17 or later
- Python 3
- Matplotlib
- pdfLaTeX (optional)

Install the Python dependency:

    python3 -m pip install matplotlib

## Reproducing the experiments

From the repository root:

    bash scripts/run.sh

The script compiles Java, runs the experiments,
generates CSV files, creates the figures, and
compiles the report if pdfLaTeX is available.

## Output files

### Results

results/raw.csv
results/summary.csv

### Figures

figures/memory.pdf
figures/edge_time.pdf
figures/neighbor_time.pdf

### Report

docs/report.tex
docs/report.pdf

## Interpretation

Adjacency lists avoid storing absent edges,
making them attractive for sparse networks.

Packed matrices support constant-time edge tests
and may be space-competitive in dense networks.

The practical performance depends on graph
density, implementation, and query workload.

## Limitations

Memory values are analytical storage estimates,
not direct measurements of retained heap memory.

The timing benchmark is a lightweight Java
microbenchmark rather than a JMH benchmark.

The generated graphs do not reproduce all
structural properties of real social networks.

Execution times may differ across machines.

## References

Cormen et al. (2022).
Introduction to Algorithms, 4th edition.

Erdős and Rényi (1959).
On Random Graphs I.

Oracle Java SE 17.
BitSet API documentation.
