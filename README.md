
# Exploring Graph Representations

Big Data — Graph Exercises, Variant 1

## Project overview

This project compares adjacency lists and packed-bit
adjacency matrices in synthetic social networks.

The goal is to look at how graph length and density
have an impact on garage requirements and question performance.

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

Edge-lifestyles queries require scanning the
supply vertex's buddies.

### Packed adjacency matrices

Implemented using Java BitSet[].

Memory complexity: O(V²) bits in the worst case.

Edge-existence queries use direct bit access.

## Benchmark methodology

Each graph configuration makes use of:

- 10,000 shared random queries
- 2 heat-up passes
- five recorded dimension trials
- System.NanoTime() timing
- Median execution-time aggregation

Correctness tests compare vertex levels and
2,000 sampled facet-existence queries.

Graph technology and representation construction
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

Adjacency lists keep away from storing absent edges,
making them attractive for sparse networks.

Packed matrices assist consistent-time area checks
and may be space-aggressive in dense networks.

The realistic performance depends on graph
density, implementation, and query workload.


## Limitations

Memory values are analytical storage estimates,
not direct measurements of retained heap memory.

The timing benchmark is a lightweight Java
microbenchmark in preference to a JMH benchmark.

The generated graphs do no longer reproduce all
structural residences of actual social networks.

Execution instances may additionally vary throughout machines.

## References

Cormen et al. (2022).
Introduction to Algorithms, 4th edition.

Erdős and Rényi (1959).
On Random Graphs I.

Oracle Java SE 17.
BitSet API documentation.
