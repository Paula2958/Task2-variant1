
import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Big Data - Variant 1
 * Exploring Graph Representations
 *
 * Compares adjacency lists and packed adjacency matrices
 * on identical synthetic undirected social networks.
 */
public final class GraphBenchmark {

    private static volatile long sink = 0;

    private static final int[] SIZES = {
        250, 500, 1000, 2000, 4000
    };

    private static final double[] PROBABILITIES = {
        0.005, 0.20
    };

    private static final int QUERIES = 10000;
    private static final int WARMUPS = 2;
    private static final int TRIALS = 5;

    interface Graph {
        boolean connected(int u, int v);
        int enumerateNeighbors(int u);
        long estimatedBytes();
    }

    static final class AdjacencyList implements Graph {

        private final int[][] adjacency;
        private final long memoryEstimate;

        AdjacencyList(int n, int[] from, int[] to) {

            int[] degree = new int[n];

            for (int i = 0; i < from.length; i++) {
                degree[from[i]]++;
                degree[to[i]]++;
            }

            adjacency = new int[n][];
            long bytes = 16L + 4L * n;

            for (int u = 0; u < n; u++) {
                adjacency[u] = new int[degree[u]];
                bytes += 16L + 4L * degree[u];
            }

            int[] position = new int[n];

            for (int i = 0; i < from.length; i++) {
                adjacency[from[i]][position[from[i]]++] =
                    to[i];

                adjacency[to[i]][position[to[i]]++] =
                    from[i];
            }

            memoryEstimate = bytes;
        }

        @Override
        public boolean connected(int u, int v) {

            for (int neighbor : adjacency[u]) {
                if (neighbor == v) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public int enumerateNeighbors(int u) {

            int checksum = 0;
            int count = 0;

            for (int neighbor : adjacency[u]) {
                checksum ^= neighbor + 1;
                count++;
            }

            sink ^= checksum;
            return count;
        }

        @Override
        public long estimatedBytes() {
            return memoryEstimate;
        }
    }


    static final class PackedMatrix implements Graph {

        private final BitSet[] adjacency;
        private final long memoryEstimate;

        PackedMatrix(int n, int[] from, int[] to) {

            adjacency = new BitSet[n];

            for (int u = 0; u < n; u++) {
                adjacency[u] = new BitSet(n);
            }

            for (int i = 0; i < from.length; i++) {
                adjacency[from[i]].set(to[i]);
                adjacency[to[i]].set(from[i]);
            }

            // Approximate storage for a fully allocated
            // packed matrix under HotSpot assumptions.
            memoryEstimate =
                16L + 4L * n
                + n * (40L + 8L * ((n + 63L) / 64));
        }

        @Override
        public boolean connected(int u, int v) {
            return adjacency[u].get(v);
        }

        @Override
        public int enumerateNeighbors(int u) {

            int checksum = 0;
            int count = 0;

            for (
                int v = adjacency[u].nextSetBit(0);
                v >= 0;
                v = adjacency[u].nextSetBit(v + 1)
            ) {
                checksum ^= v + 1;
                count++;
            }

            sink ^= checksum;
            return count;
        }

        @Override
        public long estimatedBytes() {
            return memoryEstimate;
        }
    }


    static final class EdgeSet {

        final int[] from;
        final int[] to;

        EdgeSet(int[] from, int[] to) {
            this.from = from;
            this.to = to;
        }
    }


    static EdgeSet generateGraph(
        int n,
        double probability,
        long seed
    ) {

        Random random = new Random(seed);

        int capacity = Math.max(
            16,
            (int) Math.ceil(
                probability * n * (n - 1) / 2 * 1.15
            )
        );

        int[] from = new int[capacity];
        int[] to = new int[capacity];

        int edges = 0;

        for (int u = 0; u < n; u++) {

            for (int v = u + 1; v < n; v++) {

                if (random.nextDouble() < probability) {

                    if (edges == from.length) {

                        int newCapacity =
                            from.length
                            + from.length / 2 + 1;

                        from = Arrays.copyOf(
                            from, newCapacity
                        );

                        to = Arrays.copyOf(
                            to, newCapacity
                        );
                    }

                    from[edges] = u;
                    to[edges] = v;
                    edges++;
                }
            }
        }

        return new EdgeSet(
            Arrays.copyOf(from, edges),
            Arrays.copyOf(to, edges)
        );
    }

 
    static void validate(
        Graph list,
        Graph matrix,
        int n,
        long seed
    ) {

        Random random = new Random(seed);

        // Verify degree agreement for every vertex.
        for (int u = 0; u < n; u++) {

            int listDegree = list.enumerateNeighbors(u);
            int matrixDegree = matrix.enumerateNeighbors(u);

            if (listDegree != matrixDegree) {
                throw new AssertionError(
                    "Degree mismatch at vertex " + u
                );
            }
        }

        // Verify sampled connectivity queries.
        for (int i = 0; i < 2000; i++) {

            int u = random.nextInt(n);
            int v = random.nextInt(n);

            if (list.connected(u, v)
                    != matrix.connected(u, v)) {

                throw new AssertionError(
                    "Edge mismatch: " + u + ", " + v
                );
            }
        }
    }


    static long benchmark(
        Graph graph,
        String operation,
        int[] sources,
        int[] targets
    ) {

        long checksum = 0;

        long start = System.nanoTime();

        for (int i = 0; i < sources.length; i++) {

            if (operation.equals("edge")) {

                checksum += graph.connected(
                    sources[i], targets[i]
                ) ? 1 : 0;

            } else {

                checksum += graph.enumerateNeighbors(
                    sources[i]
                );
            }
        }

        long elapsed = System.nanoTime() - start;

        sink ^= checksum;

        return elapsed;
    }


    public static void main(String[] args)
            throws IOException {

        Files.createDirectories(
            Path.of("results")
        );

        Path outputPath = Path.of("results/raw.csv");

        try (PrintWriter output = new PrintWriter(
                Files.newBufferedWriter(outputPath))) {

            output.println(
                "n,p,edges,representation,operation,"
                + "trial,queries,total_ns,ns_per_query,"
                + "estimated_bytes"
            );

            for (int n : SIZES) {

                for (double p : PROBABILITIES) {

                    long seed =
                        1234567L
                        + n * 31L
                        + Math.round(p * 1000);

                    EdgeSet edges = generateGraph(
                        n, p, seed
                    );

                    Graph list = new AdjacencyList(
                        n, edges.from, edges.to
                    );

                    Graph matrix = new PackedMatrix(
                        n, edges.from, edges.to
                    );

                    validate(
                        list, matrix, n, seed + 1
                    );

                    Random random = new Random(
                        seed + 2
                    );

                    int[] sources = new int[QUERIES];
                    int[] targets = new int[QUERIES];

                    for (int i = 0; i < QUERIES; i++) {
                        sources[i] = random.nextInt(n);
                        targets[i] = random.nextInt(n);
                    }

                    String[] operations = {
                        "edge", "neighbors"
                    };

                    for (String operation : operations) {

                        
                        for (int w = 0; w < WARMUPS; w++) {

                            benchmark(
                                list, operation,
                                sources, targets
                            );

                            benchmark(
                                matrix, operation,
                                sources, targets
                            );
                        }

                        
                        for (int trial = 0;
                             trial < TRIALS;
                             trial++) {

                            Graph[] order =
                                trial % 2 == 0
                                ? new Graph[]{list, matrix}
                                : new Graph[]{matrix, list};

                            for (Graph graph : order) {

                                long elapsed = benchmark(
                                    graph,
                                    operation,
                                    sources,
                                    targets
                                );

                                String representation =
                                    graph == list
                                    ? "list" : "matrix";

                                output.printf(
                                    Locale.ROOT,
                                    "%d,%.3f,%d,%s,%s,%d,"
                                    + "%d,%d,%.6f,%d%n",
                                    n,
                                    p,
                                    edges.from.length,
                                    representation,
                                    operation,
                                    trial,
                                    QUERIES,
                                    elapsed,
                                    elapsed / (double) QUERIES,
                                    graph.estimatedBytes()
                                );
                            }
                        }
                    }

                    System.out.printf(
                        Locale.ROOT,
                        "n=%d p=%.3f edges=%d: validated%n",
                        n, p, edges.from.length
                    );
                }
            }
        }

        System.out.println(
            "Benchmark completed: results/raw.csv"
        );

        System.out.println("Checksum: " + sink);
    }
}
