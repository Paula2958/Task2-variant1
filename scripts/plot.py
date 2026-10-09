import csv
import statistics
from collections import defaultdict
from pathlib import Path

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

ROOT = Path(__file__).resolve().parents[1]
RESULTS = ROOT / "results"
FIGURES = ROOT / "figures"

RESULTS.mkdir(exist_ok=True)
FIGURES.mkdir(exist_ok=True)


with (RESULTS / "raw.csv").open(
    newline="", encoding="utf-8"
) as file:
    rows = list(csv.DictReader(file))

if not rows:
    raise ValueError("No benchmark results found")


groups = defaultdict(list)
metadata = {}

for row in rows:
    key = (
        int(row["n"]),
        float(row["p"]),
        row["representation"],
        row["operation"]
    )

    groups[key].append(
        float(row["ns_per_query"])
    )
    metadata[key] = row

summary = []

for key, measurements in sorted(groups.items()):
    n, p, representation, operation = key
    original = metadata[key]

    summary.append({
        "n": n,
        "p": p,
        "representation": representation,
        "operation": operation,
        "median_ns_per_query":
            statistics.median(measurements),
        "estimated_bytes":
            int(original["estimated_bytes"]),
        "edges": int(original["edges"])
    })


with (RESULTS / "summary.csv").open(
    "w", newline="", encoding="utf-8"
) as file:

    writer = csv.DictWriter(
        file,
        fieldnames=[
            "n", "p", "representation", "operation",
            "median_ns_per_query", "estimated_bytes",
            "edges"
        ]
    )

    writer.writeheader()
    writer.writerows(summary)


experiments = [
    (
        "memory",
        "memory.pdf",
        "Estimated Retained Memory",
        "Estimated memory (MiB)"
    ),
    (
        "edge",
        "edge_time.pdf",
        "Edge-Existence Query Performance",
        "Median time per query (ns)"
    ),
    (
        "neighbors",
        "neighbor_time.pdf",
        "Neighbor Enumeration Performance",
        "Median time per query (ns)"
    )
]

for metric, filename, title, ylabel in experiments:

    fig, axes = plt.subplots(
        1, 2,
        figsize=(9.2, 3.5)
    )

    for ax, probability in zip(
        axes, [0.005, 0.20]
    ):

        for representation in ["list", "matrix"]:

            selected = [
                row for row in summary
                if row["p"] == probability
                and row["representation"] == representation
                and (
                    row["operation"] == "edge"
                    if metric == "memory"
                    else row["operation"] == metric
                )
            ]

            selected.sort(
                key=lambda row: row["n"]
            )

            sizes = [
                row["n"] for row in selected
            ]

            if metric == "memory":
                values = [
                    row["estimated_bytes"] / (1024 ** 2)
                    for row in selected
                ]
            else:
                values = [
                    row["median_ns_per_query"]
                    for row in selected
                ]

            label = (
                "Adjacency list"
                if representation == "list"
                else "Packed matrix"
            )

            ax.plot(
                sizes,
                values,
                marker="o",
                linewidth=1.8,
                markersize=4,
                label=label
            )

        name = (
            "Sparse"
            if probability == 0.005
            else "Dense"
        )

        ax.set_title(
            f"{name} (p = {probability:g})"
        )

        ax.set_xlabel("Number of vertices")
        ax.set_ylabel(ylabel)
        ax.grid(alpha=0.25)
        ax.legend(fontsize=8)

    fig.suptitle(
        title,
        fontsize=12,
        fontweight="bold"
    )

    fig.tight_layout()

    fig.savefig(
        FIGURES / filename,
        bbox_inches="tight"
    )

    plt.close(fig)

    print(f"Generated: figures/{filename}")

print("Generated: results/summary.csv")
