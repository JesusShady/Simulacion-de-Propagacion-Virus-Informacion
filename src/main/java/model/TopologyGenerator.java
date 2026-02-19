package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public class TopologyGenerator {

    //fuente de aleatoriedad
    private final Random random;


    //constantes de layout

    private static final double PADDING = 80.0;

    private static final double MIN_SPACING = 60.0;

    //-----------ENUMS DE TIPOLOGIA
    
    public enum TopologyType {
        STAR(
            "Estrella",
            "Un nodo central conectado a todos los demás. "
            + "Simula redes con un servidor central o súper propagador.",
            "⭐", 5, 50
        ),
        MESH(
            "Malla",
            "Nodos organizados en una grilla rectangular con conexiones "
            + "a sus vecinos directos. Simula redes urbanas o circuitos.",
            "◻️", 4, 64
        ),
        SCALE_FREE(
            "Libre de Escala",
            "Red generada con el algoritmo Barabási-Albert. Pocos nodos "
            + "tienen muchas conexiones (hubs). Simula redes sociales e internet.",
            "🌐", 8, 100
        ),
        RING(
            "Anillo",
            "Cada nodo conectado exactamente a sus 2 vecinos formando un ciclo. "
            + "Simula redes Token Ring o cadenas de transmisión.",
            "🔄", 5, 50
        ),
        TREE(
            "Árbol",
            "Estructura jerárquica sin ciclos. Cada nodo padre tiene hasta K hijos. "
            + "Simula organigramas o estructuras de archivos.",
            "🌳", 7, 40
        ),
        RANDOM(
            "Aleatoria",
            "Nodos conectados al azar con una probabilidad dada (modelo Erdős-Rényi). "
            + "Simula redes con conexiones impredecibles.",
            "🎲", 6, 50
        );

        private final String displayName;
        private final String description;
        private final String icon;
        private final int    minNodes;
        private final int    maxNodes;

        TopologyType(String displayName, String description,
                     String icon, int minNodes, int maxNodes) {
            this.displayName = displayName;
            this.description = description;
            this.icon        = icon;
            this.minNodes    = minNodes;
            this.maxNodes    = maxNodes;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public String getIcon()        { return icon; }
        public int    getMinNodes()    { return minNodes; }
        public int    getMaxNodes()    { return maxNodes; }

        @Override
        public String toString() {
            return icon + " " + displayName;
        }
    }


    //-----resultaod de generacion


    public record TopologyResult(
        TopologyType type,
        List<NodeInfo> nodes,
        List<EdgeInfo> edges,
        String suggestedHubId
    ){
        public TopologyResult{
            nodes = List.copyOf(nodes);
            edges = List.copyOf(edges);
        }
    }


    //Informacionde un node a crear

    public record NodeInfo(
        String id,
        String label,
        double x,
        double y
    ){}

     /**
     * Información de una arista a crear.
     */
    public record EdgeInfo(
        String sourceId,
        String targetId
    ) {
        public String edgeId() {
            return sourceId + "_" + targetId;
        }
    }

    //CONSTRUCTOR

      public TopologyGenerator() {
        this.random = new Random();
    }

    public TopologyGenerator(long seed) {
        this.random = new Random(seed);
    }

    // METODO PRINCIPAL


    /*Genera una topología del tipo especificado. */

    public TopologyResult generate(TopologyType type, int nodeCount,
                                   double canvasW, double canvasH) {
        // Clamp al rango permitido
        int n = Math.max(type.getMinNodes(), Math.min(nodeCount, type.getMaxNodes()));

        return switch (type) {
            case STAR       -> generateStar(n, canvasW, canvasH);
            case MESH       -> generateMesh(n, canvasW, canvasH);
            case SCALE_FREE -> generateScaleFree(n, canvasW, canvasH);
            case RING       -> generateRing(n, canvasW, canvasH);
            case TREE       -> generateTree(n, canvasW, canvasH);
            case RANDOM     -> generateRandom(n, canvasW, canvasH);
        };
    }


    // ═══════════════════════════════════════════════════════════════
    //  ESTRELLA
    //  Un nodo central con N-1 nodos alrededor
    // ═══════════════════════════════════════════════════════════════

    private TopologyResult generateStar(int n, double cw, double ch) {
        List<NodeInfo> nodes = new ArrayList<>();
        List<EdgeInfo> edges = new ArrayList<>();

        double centerX = cw / 2.0;
        double centerY = ch / 2.0;
        double radius  = Math.min(cw, ch) / 2.0 - PADDING;

        // Nodo central (el hub / súper propagador)
        String hubId = "N0";
        nodes.add(new NodeInfo(hubId, "Hub", centerX, centerY));

        // Nodos periféricos distribuidos en círculo
        int peripherals = n - 1;
        for (int i = 0; i < peripherals; i++) {
            double angle = (2.0 * Math.PI * i) / peripherals - Math.PI / 2.0;
            double x = centerX + radius * Math.cos(angle);
            double y = centerY + radius * Math.sin(angle);

            String nodeId = "N" + (i + 1);
            nodes.add(new NodeInfo(nodeId, "P" + (i + 1), x, y));
            edges.add(new EdgeInfo(hubId, nodeId));
        }

        return new TopologyResult(TopologyType.STAR, nodes, edges, hubId);
    }

    // ═══════════════════════════════════════════════════════════════
    //  MALLA (Grid)
    //  Nodos en una grilla rectangular
    // ═══════════════════════════════════════════════════════════════

    private TopologyResult generateMesh(int n, double cw, double ch) {
        List<NodeInfo> nodes = new ArrayList<>();
        List<EdgeInfo> edges = new ArrayList<>();

        // Calcular dimensiones de la grilla (lo más cuadrada posible)
        int cols = (int) Math.ceil(Math.sqrt(n));
        int rows = (int) Math.ceil((double) n / cols);

        // Espaciado entre nodos
        double usableW = cw - 2 * PADDING;
        double usableH = ch - 2 * PADDING;
        double spacingX = cols > 1 ? usableW / (cols - 1) : 0;
        double spacingY = rows > 1 ? usableH / (rows - 1) : 0;

        // Asegurar spacing mínimo
        spacingX = Math.max(spacingX, MIN_SPACING);
        spacingY = Math.max(spacingY, MIN_SPACING);

        // Centrar la grilla
        double totalW = (cols - 1) * spacingX;
        double totalH = (rows - 1) * spacingY;
        double offsetX = (cw - totalW) / 2.0;
        double offsetY = (ch - totalH) / 2.0;

        // Crear nodos en la grilla
        String[][] grid = new String[rows][cols];
        int count = 0;

        for (int r = 0; r < rows && count < n; r++) {
            for (int c = 0; c < cols && count < n; c++) {
                String nodeId = "N" + count;
                double x = offsetX + c * spacingX;
                double y = offsetY + r * spacingY;

                nodes.add(new NodeInfo(nodeId, "M" + count, x, y));
                grid[r][c] = nodeId;
                count++;
            }
        }

        // Crear aristas (horizontal y vertical)
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c] == null) continue;

                // Conexión derecha
                if (c + 1 < cols && grid[r][c + 1] != null) {
                    edges.add(new EdgeInfo(grid[r][c], grid[r][c + 1]));
                }
                // Conexión abajo
                if (r + 1 < rows && grid[r + 1][c] != null) {
                    edges.add(new EdgeInfo(grid[r][c], grid[r + 1][c]));
                }
            }
        }

        // El nodo central de la malla es el más conectado
        String hubId = grid[rows / 2][cols / 2];

        return new TopologyResult(TopologyType.MESH, nodes, edges, hubId);
    }

    /*/  LIBRE DE ESCALA (Barabási-Albert)
    //  Pocos hubs con muchas conexiones, muchos nodos con pocas */

    private TopologyResult generateScaleFree(int n, double cw, double ch) {
        List<NodeInfo> nodes = new ArrayList<>();
        List<EdgeInfo> edges = new ArrayList<>();

        // Parámetros del modelo BA
        int m0 = 3;  // Nodos iniciales
        int m  = 2;  // Conexiones por nuevo nodo

        // Grados de cada nodo (para preferential attachment)
        Map<String, Integer> degrees = new HashMap<>();

        // ── Paso 1: Crear núcleo inicial completamente conectado ──
        for (int i = 0; i < m0; i++) {
            String nodeId = "N" + i;
            nodes.add(new NodeInfo(nodeId, "SF" + i, 0, 0)); // Posición temporal
            degrees.put(nodeId, 0);
        }

        // Conectar núcleo entre sí
        for (int i = 0; i < m0; i++) {
            for (int j = i + 1; j < m0; j++) {
                String a = "N" + i;
                String b = "N" + j;
                edges.add(new EdgeInfo(a, b));
                degrees.merge(a, 1, Integer::sum);
                degrees.merge(b, 1, Integer::sum);
            }
        }

        // ── Paso 2: Agregar nodos con preferential attachment ─────
        for (int i = m0; i < n; i++) {
            String newNodeId = "N" + i;
            nodes.add(new NodeInfo(newNodeId, "SF" + i, 0, 0));
            degrees.put(newNodeId, 0);

            // Seleccionar m nodos para conectar (preferential attachment)
            List<String> targets = selectByPreferentialAttachment(degrees, m, newNodeId);

            for (String targetId : targets) {
                edges.add(new EdgeInfo(newNodeId, targetId));
                degrees.merge(newNodeId, 1, Integer::sum);
                degrees.merge(targetId, 1, Integer::sum);
            }
        }

        // ── Paso 3: Calcular posiciones usando layout de fuerza simplificado ──
        List<NodeInfo> positionedNodes = applyForceDirectedLayout(nodes, edges, cw, ch);

        // ── Encontrar el hub (mayor grado) ────────────────────────
        String hubId = degrees.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("N0");

        return new TopologyResult(TopologyType.SCALE_FREE, positionedNodes, edges, hubId);
    }

    /*
        Selecciona nodos para conectar usando preferential attachment.
     La probabilidad de seleccionar un nodo es proporcional a su grado.
     */

      private List<String> selectByPreferentialAttachment(
            Map<String, Integer> degrees, int count, String excludeId) {

        List<String> selected = new ArrayList<>();
        List<String> candidates = new ArrayList<>(degrees.keySet());
        candidates.remove(excludeId);

        for (int i = 0; i < count && !candidates.isEmpty(); i++) {
            // Calcular suma total de grados
            int totalDegree = candidates.stream()
                .mapToInt(id -> Math.max(degrees.getOrDefault(id, 0), 1))
                .sum();

            // Ruleta de selección
            double roulette = random.nextDouble() * totalDegree;
            double cumulative = 0;

            String chosen = candidates.get(candidates.size() - 1); // fallback

            for (String candidateId : candidates) {
                cumulative += Math.max(degrees.getOrDefault(candidateId, 0), 1);
                if (cumulative >= roulette) {
                    chosen = candidateId;
                    break;
                }
            }

            selected.add(chosen);
            candidates.remove(chosen); // No duplicar conexiones
        }

        return selected;
    }

    /* ANILLO (RING) 
        CADA NODO CONECTADO A SUS DOS VECINOS FORMANDO UN CIRCULO
    */

         private TopologyResult generateRing(int n, double cw, double ch) {
        List<NodeInfo> nodes = new ArrayList<>();
        List<EdgeInfo> edges = new ArrayList<>();

        double centerX = cw / 2.0;
        double centerY = ch / 2.0;
        double radius  = Math.min(cw, ch) / 2.0 - PADDING;

        for (int i = 0; i < n; i++) {
            double angle = (2.0 * Math.PI * i) / n - Math.PI / 2.0;
            double x = centerX + radius * Math.cos(angle);
            double y = centerY + radius * Math.sin(angle);

            String nodeId = "N" + i;
            nodes.add(new NodeInfo(nodeId, "R" + i, x, y));

            // Conectar con el nodo anterior (cerrar ciclo al final)
            if (i > 0) {
                edges.add(new EdgeInfo("N" + (i - 1), nodeId));
            }
        }

        // Cerrar el anillo
        if (n > 2) {
            edges.add(new EdgeInfo("N" + (n - 1), "N0"));
        }

        // En un anillo todos tienen el mismo grado — no hay hub claro
        return new TopologyResult(TopologyType.RING, nodes, edges, null);
    }

    //arbol

     private TopologyResult generateTree(int n, double cw, double ch) {
        List<NodeInfo> nodes = new ArrayList<>();
        List<EdgeInfo> edges = new ArrayList<>();

        int branchFactor = 3; // Cada nodo tiene hasta 3 hijos

        // Crear nodos usando BFS (nivel por nivel)
        nodes.add(new NodeInfo("N0", "Root", cw / 2.0, PADDING));

        int created = 1;
        int parentIdx = 0;

        while (created < n) {
            String parentId = "N" + parentIdx;

            for (int child = 0; child < branchFactor && created < n; child++) {
                String childId = "N" + created;
                nodes.add(new NodeInfo(childId, "T" + created, 0, 0));
                edges.add(new EdgeInfo(parentId, childId));
                created++;
            }
            parentIdx++;
        }

        // Calcular posiciones jerárquicas
        List<NodeInfo> positioned = applyTreeLayout(nodes, edges, cw, ch, branchFactor);

        // La raíz es siempre el hub en un árbol
        return new TopologyResult(TopologyType.TREE, positioned, edges, "N0");
    }


    private List<NodeInfo> applyTreeLayout(List<NodeInfo> nodes, List<EdgeInfo> edges,
                                           double cw, double ch, int branchFactor) {
        int n = nodes.size();
        if (n == 0) return nodes;

        // Calcular el nivel de cada nodo (BFS desde la raíz)
        Map<String, Integer> levels = new HashMap<>();
        Map<Integer, List<String>> nodesByLevel = new HashMap<>();
        levels.put("N0", 0);
        nodesByLevel.computeIfAbsent(0, k -> new ArrayList<>()).add("N0");

        // Construir mapa de adyacencia (parent → children)
        Map<String, List<String>> children = new HashMap<>();
        for (EdgeInfo edge : edges) {
            children.computeIfAbsent(edge.sourceId(), k -> new ArrayList<>())
                    .add(edge.targetId());
        }

        // BFS para asignar niveles
        List<String> queue = new ArrayList<>();
        queue.add("N0");
        int head = 0;

        while (head < queue.size()) {
            String current = queue.get(head++);
            int currentLevel = levels.get(current);

            List<String> kids = children.getOrDefault(current, Collections.emptyList());
            for (String kid : kids) {
                if (!levels.containsKey(kid)) {
                    levels.put(kid, currentLevel + 1);
                    nodesByLevel.computeIfAbsent(currentLevel + 1, k -> new ArrayList<>())
                                .add(kid);
                    queue.add(kid);
                }
            }
        }

        // Calcular posiciones
        int totalLevels = nodesByLevel.size();
        double levelHeight = (ch - 2 * PADDING) / Math.max(totalLevels - 1, 1);

        Map<String, double[]> positions = new HashMap<>();

        for (var entry : nodesByLevel.entrySet()) {
            int level = entry.getKey();
            List<String> nodesAtLevel = entry.getValue();
            int count = nodesAtLevel.size();

            double y = PADDING + level * levelHeight;
            double spacing = (cw - 2 * PADDING) / Math.max(count + 1, 2);

            for (int i = 0; i < count; i++) {
                double x = PADDING + spacing * (i + 1);
                positions.put(nodesAtLevel.get(i), new double[]{x, y});
            }
        }

        // Reconstruir con posiciones
        List<NodeInfo> result = new ArrayList<>();
        for (NodeInfo node : nodes) {
            double[] pos = positions.getOrDefault(node.id(), new double[]{cw / 2, ch / 2});
            result.add(new NodeInfo(node.id(), node.label(), pos[0], pos[1]));
        }

        return result;
    }


    //ALEATORIA

     private TopologyResult generateRandom(int n, double cw, double ch) {
        List<NodeInfo> nodes = new ArrayList<>();
        List<EdgeInfo> edges = new ArrayList<>();

        // Probabilidad de conexión — calibrada para que el grafo sea conexo
        // pero no demasiado denso. p ≈ ln(n)/n garantiza conexidad con alta prob.
        double p = Math.max(0.15, Math.log(n) / n * 1.5);

        // Crear nodos con posiciones aleatorias (con spacing mínimo)
        double usableW = cw - 2 * PADDING;
        double usableH = ch - 2 * PADDING;

        for (int i = 0; i < n; i++) {
            double x = PADDING + random.nextDouble() * usableW;
            double y = PADDING + random.nextDouble() * usableH;

            nodes.add(new NodeInfo("N" + i, "A" + i, x, y));
        }

        // Crear aristas con probabilidad p
        Map<String, Integer> degrees = new HashMap<>();
        for (int i = 0; i < n; i++) {
            degrees.put("N" + i, 0);
            for (int j = i + 1; j < n; j++) {
                if (random.nextDouble() < p) {
                    edges.add(new EdgeInfo("N" + i, "N" + j));
                    degrees.merge("N" + i, 1, Integer::sum);
                    degrees.merge("N" + j, 1, Integer::sum);
                }
            }
        }

        // Garantizar que no haya nodos aislados (conectar al más cercano)
        for (int i = 0; i < n; i++) {
            if (degrees.getOrDefault("N" + i, 0) == 0) {
                // Encontrar el nodo más cercano
                double minDist = Double.MAX_VALUE;
                int closest = (i + 1) % n;

                for (int j = 0; j < n; j++) {
                    if (j == i) continue;
                    double dx = nodes.get(i).x() - nodes.get(j).x();
                    double dy = nodes.get(i).y() - nodes.get(j).y();
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    if (dist < minDist) {
                        minDist = dist;
                        closest = j;
                    }
                }

                edges.add(new EdgeInfo("N" + i, "N" + closest));
                degrees.merge("N" + i, 1, Integer::sum);
                degrees.merge("N" + closest, 1, Integer::sum);
            }
        }

        // Hub = nodo con mayor grado
        String hubId = degrees.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("N0");

        return new TopologyResult(TopologyType.RANDOM, nodes, edges, hubId);
    }


     // ═══════════════════════════════════════════════════════════════
    //  LAYOUT DE FUERZA DIRIGIDA (Force-Directed Layout)
    //  Posiciona nodos de forma estética usando simulación física
    // ═══════════════════════════════════════════════════════════════


    /**
     * Aplica un algoritmo de layout basado en fuerzas (Fruchterman-Reingold simplificado).
     *
     *   - Fuerza de repulsión: todos los nodos se repelen entre sí
     *                          (como cargas eléctricas del mismo signo)
     *   - Fuerza de atracción: las aristas actúan como resortes
     *                          (mantienen nodos conectados cerc

    */

    private List<NodeInfo> applyForceDirectedLayout(
            List<NodeInfo> nodes, List<EdgeInfo> edges,
            double canvasW, double canvasH) {

        int n = nodes.size();
        if (n <= 1) return nodes;

        // Posiciones mutables para la simulación
        double[] px = new double[n];
        double[] py = new double[n];

        // Inicializar en posiciones circulares (mejor convergencia que aleatorio)
        double cx = canvasW / 2.0;
        double cy = canvasH / 2.0;
        double initRadius = Math.min(canvasW, canvasH) / 3.0;

        for (int i = 0; i < n; i++) {
            double angle = (2.0 * Math.PI * i) / n;
            px[i] = cx + initRadius * Math.cos(angle);
            py[i] = cy + initRadius * Math.sin(angle);
        }

        // Mapa de ID a índice para lookup rápido
        Map<String, Integer> idToIndex = new HashMap<>();
        for (int i = 0; i < n; i++) {
            idToIndex.put(nodes.get(i).id(), i);
        }

        // ── Parámetros del algoritmo ──────────────────────────────
        double area = (canvasW - 2 * PADDING) * (canvasH - 2 * PADDING);
        double k = Math.sqrt(area / n);       // Distancia ideal entre nodos
        double temperature = canvasW / 5.0;    // "Temperatura" inicial (rango de movimiento)
        int iterations = 120;                  // Iteraciones de simulación

        // ── Simulación de fuerzas ─────────────────────────────────
        for (int iter = 0; iter < iterations; iter++) {
            double[] fx = new double[n];
            double[] fy = new double[n];

            // Fuerzas de REPULSIÓN (todos contra todos)
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    double dx = px[i] - px[j];
                    double dy = py[i] - py[j];
                    double dist = Math.max(Math.sqrt(dx * dx + dy * dy), 0.01);

                    // Fuerza repulsiva: k² / d
                    double force = (k * k) / dist;
                    double forceX = (dx / dist) * force;
                    double forceY = (dy / dist) * force;

                    fx[i] += forceX;
                    fy[i] += forceY;
                    fx[j] -= forceX;
                    fy[j] -= forceY;
                }
            }

            // Fuerzas de ATRACCIÓN (solo aristas)
            for (EdgeInfo edge : edges) {
                Integer iIdx = idToIndex.get(edge.sourceId());
                Integer jIdx = idToIndex.get(edge.targetId());
                if (iIdx == null || jIdx == null) continue;

                double dx = px[iIdx] - px[jIdx];
                double dy = py[iIdx] - py[jIdx];
                double dist = Math.max(Math.sqrt(dx * dx + dy * dy), 0.01);

                // Fuerza atractiva: d² / k
                double force = (dist * dist) / k;
                double forceX = (dx / dist) * force;
                double forceY = (dy / dist) * force;

                fx[iIdx] -= forceX;
                fy[iIdx] -= forceY;
                fx[jIdx] += forceX;
                fy[jIdx] += forceY;
            }

            // Aplicar fuerzas (limitadas por temperatura)
            for (int i = 0; i < n; i++) {
                double fMag = Math.sqrt(fx[i] * fx[i] + fy[i] * fy[i]);
                if (fMag > 0) {
                    double limitedF = Math.min(fMag, temperature);
                    px[i] += (fx[i] / fMag) * limitedF;
                    py[i] += (fy[i] / fMag) * limitedF;
                }

                // Mantener dentro de los límites del canvas
                px[i] = Math.max(PADDING, Math.min(canvasW - PADDING, px[i]));
                py[i] = Math.max(PADDING, Math.min(canvasH - PADDING, py[i]));
            }

            // Enfriar (reducir temperatura)
            temperature *= 0.95;
        }

        // Construir resultado con posiciones finales
        List<NodeInfo> result = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            NodeInfo orig = nodes.get(i);
            result.add(new NodeInfo(orig.id(), orig.label(), px[i], py[i]));
        }

        return result;
    }

/*Analiza un conjunto de aristas y determina cuál nodo tiene
     * la mayor centralidad de grado (más conexiones directas).
     *
     * Este es el nodo más "peligroso" — si se infecta, puede
     * propagar el virus al mayor número de vecinos directos. */

    public static String findSuperSpreader(List<EdgeInfo> edges) {
        if (edges == null || edges.isEmpty()) return null;

        Map<String, Integer> degrees = new HashMap<>();

        for (EdgeInfo edge : edges) {
            degrees.merge(edge.sourceId(), 1, Integer::sum);
            degrees.merge(edge.targetId(), 1, Integer::sum);
        }

        return degrees.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }


    public static List<Map.Entry<String, Integer>> getDegreeCentralityRanking(
            List<EdgeInfo> edges, int topN) {

        if (edges == null || edges.isEmpty()) return Collections.emptyList();

        Map<String, Integer> degrees = new HashMap<>();
        for (EdgeInfo edge : edges) {
            degrees.merge(edge.sourceId(), 1, Integer::sum);
            degrees.merge(edge.targetId(), 1, Integer::sum);
        }

        return degrees.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(topN)
            .collect(Collectors.toList());
    }
}
