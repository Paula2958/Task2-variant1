
#!/usr/bin/env bash

set -euo pipefail

# Run from the repository root.
cd "$(dirname "$0")/.."

mkdir -p build results figures

echo "====================================="
echo "Big Data - Graph Representations"
echo "====================================="

echo "[1/4] Compiling Java..."

javac -d build src/GraphBenchmark.java

echo "[2/4] Executing experiments..."

java -cp build GraphBenchmark

echo "[3/4] Generating CSV summaries and figures..."

python3 scripts/plot.py

echo "[4/4] Compiling LaTeX report..."

if command -v pdflatex >/dev/null 2>&1; then

    (
        cd docs

        pdflatex \
            -interaction=nonstopmode \
            -halt-on-error \
            report.tex >/dev/null

        pdflatex \
            -interaction=nonstopmode \
            -halt-on-error \
            report.tex >/dev/null
    )

    echo "Generated: docs/report.pdf"

else
    echo "pdflatex not found."
    echo "Compile docs/report.tex using Overleaf."
fi

echo "====================================="
echo "Experiment completed."
echo "====================================="
