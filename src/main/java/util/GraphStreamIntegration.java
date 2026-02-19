package util;

import model.GraphModel;
import model.GraphModel.EdgeRecord;
import model.NodeData;
import model.enums.NodeStatus;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.layout.springbox.implementations.SpringBox;
import org.graphstream.ui.layout.springbox.implementations.LinLog;

import java.util.HashMap;
import java.util.Map;

public class GraphStreamIntegration {

    private static final String GS_GRAPH_ID = "BFS_Propagation_Graph";
    private static final int LAYOUT_ITERATIONS = 200;
    private static final double PADDING = 60.0;

    private static final String GS_STYLESHEET =
            "graph { fill-color: #0A0E14; padding: 50px; } " +
                    "node { fill-color: #B0BEC5; stroke-mode: plain; stroke-color: #78909C; " +
                    "       stroke-width: 2px; size: 30px; text-color: white; text-size: 12px; } " +
                    "node.susceptible { fill-color: #B0BEC5; } " +
                    "node.infected { fill-color: #FF1744; size: 35px; } " +
                    "node.recovered { fill-color: #00E676; } " +
                    "node.quarantined { fill-color: #00E5FF; } " +
                    "node.patient_zero { fill-color: #D500F9; size: 40px; } " +
                    "edge { fill-color: #30363D; size: 2px; } " +
                    "edge.active { fill-color: #FF9100; } " +
                    "edge.disabled { fill-color: #FF174444; size: 1px; }";

    private final GraphModel graphModel;
    private Graph gsGraph;
    private boolean synced;

    public GraphStreamIntegration(GraphModel graphModel) {
        this.graphModel = graphModel;
        this.gsGraph    = null;
        this.synced     = false;
    }

    // ═══════════════════════════════════════════════════════════════
    //  ENUM
    // ═══════════════════════════════════════════════════════════════

    public enum LayoutType {
        SPRING_BOX("SpringBox", "Layout basado en fuerzas."),
        LIN_LOG("LinLog", "Layout logarítmico para clusters.");

        private final String name;
        private final String description;

        LayoutType(String name, String description) {
            this.name = name;
            this.description = description;
        }

        public String getName()        { return name; }
        public String getDescription() { return description; }
    }

    // ═══════════════════════════════════════════════════════════════
    //  SYNC: GraphModel → GraphStream
    // ═══════════════════════════════════════════════════════════════

    public Graph syncToGraphStream() {
        gsGraph = new SingleGraph(GS_GRAPH_ID);
        gsGraph.setAttribute("ui.stylesheet", GS_STYLESHEET);
        gsGraph.setAutoCreate(false);
        gsGraph.setStrict(false);

        for (NodeData nodeData : graphModel.getAllNodes()) {
            Node gsNode = gsGraph.addNode(nodeData.getId());
            gsNode.setAttribute("xy", nodeData.getPosX(), nodeData.getPosY());
            gsNode.setAttribute("ui.label", nodeData.getLabel());
            gsNode.setAttribute("ui.class", getGSCssClass(nodeData));
            gsNode.setAttribute("depth", nodeData.getDepthLevel());
            gsNode.setAttribute("degree", nodeData.getDegree());
            gsNode.setAttribute("status", nodeData.getStatus().name());
        }

        for (EdgeRecord edgeRecord : graphModel.getAllEdges()) {
            try {
                Edge gsEdge = gsGraph.addEdge(
                        edgeRecord.id(),
                        edgeRecord.sourceId(),
                        edgeRecord.targetId(),
                        false
                );
                if (graphModel.getSimulationState().isEdgeDisabled(edgeRecord.id())) {
                    gsEdge.setAttribute("ui.class", "disabled");
                }
            } catch (Exception e) {
                System.err.println("[GSIntegration] Error arista: " + e.getMessage());
            }
        }

        synced = true;
        System.out.println("[GSIntegration] Sincronizado: "
                + gsGraph.getNodeCount() + " nodos, "
                + gsGraph.getEdgeCount() + " aristas");
        return gsGraph;
    }

    private String getGSCssClass(NodeData node) {
        String cssClass = switch (node.getStatus()) {
            case SUSCEPTIBLE  -> "susceptible";
            case INFECTED     -> "infected";
            case RECOVERED    -> "recovered";
            case QUARANTINED  -> "quarantined";
            case PATIENT_ZERO -> "patient_zero";
        };
        if (node.isSuperSpreader()) {
            cssClass += ", super_spreader";
        }
        return cssClass;
    }

    // ═══════════════════════════════════════════════════════════════
    //  POSICIONES: GraphStream → GraphModel
    // ══════════════════════════════════════════════════════════════���

    public void applyPositionsToModel(double canvasWidth, double canvasHeight) {
        if (gsGraph == null || !synced) return;

        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;

        for (Node gsNode : gsGraph) {
            double[] pos = getNodeXY(gsNode);
            if (pos == null) continue;
            minX = Math.min(minX, pos[0]);
            maxX = Math.max(maxX, pos[0]);
            minY = Math.min(minY, pos[1]);
            maxY = Math.max(maxY, pos[1]);
        }

        double gsWidth  = Math.max(maxX - minX, 1);
        double gsHeight = Math.max(maxY - minY, 1);
        double usableW = canvasWidth  - 2 * PADDING;
        double usableH = canvasHeight - 2 * PADDING;
        double scale = Math.min(usableW / gsWidth, usableH / gsHeight);
        double offsetX = PADDING + (usableW - gsWidth * scale) / 2;
        double offsetY = PADDING + (usableH - gsHeight * scale) / 2;

        for (Node gsNode : gsGraph) {
            double[] pos = getNodeXY(gsNode);
            if (pos == null) continue;
            double canvasX = offsetX + (pos[0] - minX) * scale;
            double canvasY = offsetY + (pos[1] - minY) * scale;
            NodeData nodeData = graphModel.getNode(gsNode.getId());
            if (nodeData != null) {
                nodeData.setPosX(canvasX);
                nodeData.setPosY(canvasY);
            }
        }
    }

    /**
     * Obtiene posición XY de un nodo de GraphStream con casting seguro.
     */
    private double[] getNodeXY(Node node) {
        // Intentar "xy"
        Object xyRaw = node.getAttribute("xy");
        if (xyRaw instanceof Object[]) {
            Object[] xy = (Object[]) xyRaw;
            if (xy.length >= 2 && xy[0] instanceof Number && xy[1] instanceof Number) {
                return new double[]{
                        ((Number) xy[0]).doubleValue(),
                        ((Number) xy[1]).doubleValue()
                };
            }
        }

        // Intentar "xyz"
        Object xyzRaw = node.getAttribute("xyz");
        if (xyzRaw instanceof Object[]) {
            Object[] xyz = (Object[]) xyzRaw;
            if (xyz.length >= 2 && xyz[0] instanceof Number && xyz[1] instanceof Number) {
                return new double[]{
                        ((Number) xyz[0]).doubleValue(),
                        ((Number) xyz[1]).doubleValue()
                };
            }
        }

        // Intentar x, y individuales
        Object xObj = node.getAttribute("x");
        Object yObj = node.getAttribute("y");
        if (xObj instanceof Number && yObj instanceof Number) {
            return new double[]{
                    ((Number) xObj).doubleValue(),
                    ((Number) yObj).doubleValue()
            };
        }

        return null;
    }

    // ═══════════════════════════════════════════════════════════════
    //  LAYOUTS
    // ═══════════════════════════════════════════════════════════════

    public void applyLayout(LayoutType layoutType, double canvasWidth,
                            double canvasHeight, int iterations) {
        syncToGraphStream();
        if (gsGraph.getNodeCount() == 0) return;

        int iters = iterations > 0 ? iterations : LAYOUT_ITERATIONS;

        switch (layoutType) {
            case SPRING_BOX -> applySpringBoxLayout(iters);
            case LIN_LOG    -> applyLinLogLayout(iters);
        }

        applyPositionsToModel(canvasWidth, canvasHeight);
    }

    private void applySpringBoxLayout(int iterations) {
        SpringBox layout = new SpringBox(false);
        layout.addSink(gsGraph);
        gsGraph.addSink(layout);
        layout.setForce(0.5);
        layout.setQuality(1.0);
        for (int i = 0; i < iterations; i++) layout.compute();
        extractLayoutPositions();
        layout.removeSink(gsGraph);
        gsGraph.removeSink(layout);
        layout.clear();
    }

    private void applyLinLogLayout(int iterations) {
        LinLog layout = new LinLog(false);
        layout.addSink(gsGraph);
        gsGraph.addSink(layout);
        layout.setForce(3.0);
        layout.setQuality(1.0);
        for (int i = 0; i < iterations; i++) layout.compute();
        extractLayoutPositions();
        layout.removeSink(gsGraph);
        gsGraph.removeSink(layout);
        layout.clear();
    }

    private void extractLayoutPositions() {
        for (Node gsNode : gsGraph) {
            double[] pos = getNodeXY(gsNode);
            if (pos != null) {
                gsNode.setAttribute("xy", pos[0], pos[1]);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  MÉTRICAS
    // ═══════════════════════════════════════════════════════════════

    public Map<String, Double> computeBetweennessCentrality() {
        Map<String, Double> centrality = new HashMap<>();
        if (!ensureSynced()) return centrality;

        try {
            org.graphstream.algorithm.BetweennessCentrality bc =
                    new org.graphstream.algorithm.BetweennessCentrality();
            bc.init(gsGraph);
            bc.compute();
            for (Node node : gsGraph) {
                Object cbRaw = node.getAttribute("Cb");
                double cb = (cbRaw instanceof Number) ? ((Number) cbRaw).doubleValue() : 0.0;
                centrality.put(node.getId(), cb);
            }
        } catch (Exception e) {
            System.err.println("[GSIntegration] Error betweenness: " + e.getMessage());
        }
        return centrality;
    }

    public int countConnectedComponents() {
        if (!ensureSynced()) return 0;
        try {
            org.graphstream.algorithm.ConnectedComponents cc =
                    new org.graphstream.algorithm.ConnectedComponents();
            cc.init(gsGraph);
            cc.compute();
            return cc.getConnectedComponentsCount();
        } catch (Exception e) {
            return -1;
        }
    }

    public int computeDiameter() {
        if (!ensureSynced()) return -1;
        if (countConnectedComponents() != 1) return -1;

        try {
            org.graphstream.algorithm.APSP apsp =
                    new org.graphstream.algorithm.APSP();
            apsp.init(gsGraph);
            apsp.compute();

            int diameter = 0;
            for (Node src : gsGraph) {
                Object infoRaw = src.getAttribute(
                        org.graphstream.algorithm.APSP.APSPInfo.ATTRIBUTE_NAME);
                if (!(infoRaw instanceof org.graphstream.algorithm.APSP.APSPInfo)) continue;
                org.graphstream.algorithm.APSP.APSPInfo info =
                        (org.graphstream.algorithm.APSP.APSPInfo) infoRaw;

                for (Node dst : gsGraph) {
                    if (src.equals(dst)) continue;
                    double dist = info.getLengthTo(dst.getId());
                    if (dist != Double.POSITIVE_INFINITY && dist > diameter) {
                        diameter = (int) dist;
                    }
                }
            }
            return diameter;
        } catch (Exception e) {
            return -1;
        }
    }

    public double computeAverageClusteringCoefficient() {
        if (!ensureSynced()) return 0.0;
        try {
            double totalCC = 0;
            int validNodes = 0;
            for (Node node : gsGraph) {
                int degree = node.getDegree();
                if (degree < 2) continue;
                int triangles = 0;
                int possibleTriangles = degree * (degree - 1) / 2;
                Node[] neighbors = new Node[degree];
                int idx = 0;
                for (Edge edge : node) {
                    neighbors[idx++] = edge.getOpposite(node);
                }
                for (int i = 0; i < degree; i++) {
                    for (int j = i + 1; j < degree; j++) {
                        if (neighbors[i].hasEdgeBetween(neighbors[j])) {
                            triangles++;
                        }
                    }
                }
                totalCC += possibleTriangles > 0 ? (double) triangles / possibleTriangles : 0.0;
                validNodes++;
            }
            return validNodes > 0 ? totalCC / validNodes : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    public String generateMetricsReport() {
        if (!ensureSynced()) return "Grafo no sincronizado.";
        int components = countConnectedComponents();
        int diameter = computeDiameter();
        double avgClustering = computeAverageClusteringCoefficient();
        double avgDegree = graphModel.getAverageDegree();
        Map<String, Double> betweenness = computeBetweennessCentrality();

        String topNode = "";
        double topVal = 0;
        for (var entry : betweenness.entrySet()) {
            if (entry.getValue() > topVal) {
                topVal = entry.getValue();
                topNode = entry.getKey();
            }
        }

        return String.format(
                "Nodos: %d, Aristas: %d, Grado prom: %.2f, " +
                        "Componentes: %d, Diámetro: %s, Clustering: %.4f, " +
                        "Puente: %s (%.4f)",
                graphModel.getNodeCount(), graphModel.getEdgeCount(), avgDegree,
                components, diameter >= 0 ? String.valueOf(diameter) : "∞",
                avgClustering, topNode, topVal
        );
    }

    // ═══════════════════════════════════════════════════════════════
    //  ESTILOS DINÁMICOS
    // ═══════════════════════════════════════════════════════════════

    public void updateStyles() {
        if (gsGraph == null || !synced) return;
        for (NodeData nodeData : graphModel.getAllNodes()) {
            Node gsNode = gsGraph.getNode(nodeData.getId());
            if (gsNode != null) {
                gsNode.setAttribute("ui.class", getGSCssClass(nodeData));
                gsNode.setAttribute("ui.label", nodeData.getLabel());
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  UTILIDADES
    // ═══════════════════════════════════════════════════════════════

    private boolean ensureSynced() {
        if (gsGraph == null || !synced) syncToGraphStream();
        return gsGraph != null && synced;
    }

    public void invalidateSync() { synced = false; }

    public void cleanup() {
        if (gsGraph != null) { gsGraph.clear(); gsGraph = null; }
        synced = false;
    }

    public Graph getGsGraph()  { return gsGraph; }
    public boolean isSynced()  { return synced; }
}