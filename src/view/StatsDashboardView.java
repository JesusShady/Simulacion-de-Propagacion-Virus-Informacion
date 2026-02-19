package view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import model.GraphModel;
import model.NodeData;
import model.SimulationState;
import model.SimulationState.IterationStats;
import model.enums.NodeStatus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class StatsDashboardView {  
 // ═══════════════════════════════════════════════════════════════
    //  CONSTANTES
    // ═══════════════════════════════════════════════════════════════

    private static final double PANEL_WIDTH    = MainView.DASHBOARD_WIDTH;
    private static final double BAR_HEIGHT     = 14.0;
    private static final double BAR_RADIUS     = 5.0;
    private static final double CHART_HEIGHT   = 160.0;
    private static final double CHART_PADDING  = 8.0;

    // ═══════════════════════════════════════════════════════════════
    //  COMPONENTES
    // ═══════════════════════════════════════════════════════════════

    private final ScrollPane root;
    private final VBox       content;
    private final GraphModel model;

    // ── Estadísticas SIR ─────────────────────────────────────���────
    private Label lblStatTitle;
    private Label lblSusceptible;
    private Label lblInfected;
    private Label lblRecovered;
    private Label lblQuarantined;
    private Label lblTotalNodes;
    private Label lblInfectionPct;

    // Barras de progreso personalizadas
    private Pane barSusceptible;
    private Pane barInfected;
    private Pane barRecovered;
    private Pane barQuarantined;

    // Contenedores de las barras (para controlar el fill)
    private Region fillSusceptible;
    private Region fillInfected;
    private Region fillRecovered;
    private Region fillQuarantined;

    // ── Gráfica SIR ───────────────────────────────────────────────
    private Canvas  chartCanvas;
    private GraphicsContext chartGC;

    // ── Info del nodo seleccionado ─────────────────────────────────
    private VBox  nodeInfoBox;
    private Label lblNodeId;
    private Label lblNodeLabel;
    private Label lblNodeStatus;
    private Label lblNodeDepth;
    private Label lblNodeInfectedBy;
    private Label lblNodeDegree;
    private Label lblNodeSpreadCount;
    private Label lblNodeDuration;
    private Label lblNoSelection;

    // ── Cola BFS ──────────────────────────────────────────────────
    private VBox    bfsQueueBox;
    private HBox    queueChipsContainer;
    private Label   lblQueueEmpty;

    // ── Historial de stats (para la gráfica) ──────────────────────
    private final List<IterationStats> statsHistory;

    // ═══════════════════════════════════════════════════════════════
    //  CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════

    public StatsDashboardView(GraphModel model) {
        this.model = model;
        this.statsHistory = new ArrayList<>();

        content = new VBox(0);
        content.setPrefWidth(PANEL_WIDTH);
        content.setStyle(String.format(
            "-fx-background-color: %s;",
            MainView.BG_SECONDARY
        ));

        buildStatsSection();
        buildChartSection();
        buildNodeInfoSection();
        buildBFSQueueSection();

        content.getChildren().addAll(
            createSectionWidget("📊  ESTADÍSTICAS", buildStatsContent()),
            createSectionWidget("📈  GRÁFICA SIR",  buildChartContent()),
            createSectionWidget("🔍  NODO SELECCIONADO", nodeInfoBox),
            createSectionWidget("📋  COLA BFS", bfsQueueBox)
        );

        // ScrollPane para cuando el contenido exceda la ventana
        root = new ScrollPane(content);
        root.setFitToWidth(true);
        root.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        root.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        root.setPrefWidth(PANEL_WIDTH);
        root.setStyle(String.format(
            "-fx-background: %s; " +
            "-fx-background-color: %s; " +
            "-fx-border-color: %s transparent transparent transparent; " +
            "-fx-border-width: 0 0 0 1;",
            MainView.BG_SECONDARY, MainView.BG_SECONDARY, MainView.BORDER_COLOR
        ));

        setupBindings();
    }

    // ═══════════════════════════════════════════════════════════════
    //  CONSTRUCCIÓN: Sección de Estadísticas SIR
    // ═══════════════════════════════════════════════════════════════

    private void buildStatsSection() {
        lblSusceptible  = createStatValue("0", MainView.TEXT_SECONDARY);
        lblInfected     = createStatValue("0", MainView.ACCENT_RED);
        lblRecovered    = createStatValue("0", MainView.ACCENT_GREEN);
        lblQuarantined  = createStatValue("0", MainView.ACCENT_CYAN);
        lblTotalNodes   = createStatValue("0", MainView.TEXT_PRIMARY);
        lblInfectionPct = createStatValue("0.0%", MainView.ACCENT_RED);

        // Barras de progreso
        fillSusceptible = createBarFill(MainView.TEXT_SECONDARY);
        fillInfected    = createBarFill(MainView.ACCENT_RED);
        fillRecovered   = createBarFill(MainView.ACCENT_GREEN);
        fillQuarantined = createBarFill(MainView.ACCENT_CYAN);

        barSusceptible = createProgressBar(fillSusceptible);
        barInfected    = createProgressBar(fillInfected);
        barRecovered   = createProgressBar(fillRecovered);
        barQuarantined = createProgressBar(fillQuarantined);
    }

    private VBox buildStatsContent() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(4, 0, 4, 0));

        box.getChildren().addAll(
            createStatRow("S  Susceptible", lblSusceptible, barSusceptible,
                          NodeStatus.SUSCEPTIBLE.getIcon()),
            createStatRow("I  Infectado",   lblInfected,    barInfected,
                          NodeStatus.INFECTED.getIcon()),
            createStatRow("R  Recuperado",  lblRecovered,   barRecovered,
                          NodeStatus.RECOVERED.getIcon()),
            createStatRow("Q  Cuarentena",  lblQuarantined, barQuarantined,
                          NodeStatus.QUARANTINED.getIcon()),
            new Separator() {{
                setStyle(String.format("-fx-background-color: %s;", MainView.BORDER_COLOR));
                setPadding(new Insets(4, 0, 4, 0));
            }},
            createInfoLine("Total nodos:", lblTotalNodes),
            createInfoLine("Infección:",   lblInfectionPct)
        );

        return box;
    }

    /**
     * Crea una fila de estadística: ícono + nombre + valor + barra.
     */
    private VBox createStatRow(String name, Label valueLabel, Pane bar, String icon) {
        // Línea superior: ícono + nombre + valor
        Label nameLabel = new Label(name);
        nameLabel.setFont(Font.font("Consolas", 11));
        nameLabel.setTextFill(Color.web(MainView.TEXT_SECONDARY));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topLine = new HBox(6, nameLabel, spacer, valueLabel);
        topLine.setAlignment(Pos.CENTER_LEFT);

        // Barra debajo
        VBox row = new VBox(3, topLine, bar);
        return row;
    }

    /**
     * Crea una barra de progreso personalizada (fondo + fill).
     */
    private Pane createProgressBar(Region fill) {
        Pane bar = new Pane(fill);
        bar.setPrefHeight(BAR_HEIGHT);
        bar.setMaxHeight(BAR_HEIGHT);
        bar.setStyle(String.format(
            "-fx-background-color: %s; " +
            "-fx-background-radius: %f;",
            MainView.BG_PRIMARY, BAR_RADIUS
        ));

        // Fill empieza en 0%
        fill.setPrefHeight(BAR_HEIGHT);
        fill.setLayoutX(0);
        fill.setLayoutY(0);
        fill.setPrefWidth(0);

        // Actualizar width del fill cuando cambie el tamaño del bar
        bar.widthProperty().addListener((obs, old, w) -> {
            // Se actualizará desde updateStats()
        });

        return bar;
    }

    /**
     * Crea el fill (parte rellena) de una barra de progreso.
     */
    private Region createBarFill(String color) {
        Region fill = new Region();
        fill.setStyle(String.format(
            "-fx-background-color: %s; " +
            "-fx-background-radius: %f;",
            color, BAR_RADIUS
        ));

        // Glow sutil
        DropShadow glow = new DropShadow();
        glow.setColor(Color.web(color, 0.4));
        glow.setRadius(6);
        glow.setSpread(0.1);
        fill.setEffect(glow);

        return fill;
    }

    /**
     * Actualiza una barra de progreso con animación.
     *
     * @param fill       el Region que actúa como relleno
     * @param bar        el Pane contenedor
     * @param percentage porcentaje (0.0 a 1.0)
     */
    private void updateBar(Region fill, Pane bar, double percentage) {
        double maxWidth = bar.getWidth();
        if (maxWidth <= 0) maxWidth = PANEL_WIDTH - 40;
        double targetWidth = maxWidth * Math.max(0, Math.min(1, percentage));
        fill.setPrefWidth(targetWidth);
    }

    // ════════════════════════��══════════════════════════════════════
    //  CONSTRUCCIÓN: Gráfica SIR
    // ═══════════════════════════════════════════════════��═══════════

    private void buildChartSection() {
        chartCanvas = new Canvas(PANEL_WIDTH - 32, CHART_HEIGHT);
        chartGC = chartCanvas.getGraphicsContext2D();
    }

    private Pane buildChartContent() {
        Pane chartContainer = new Pane(chartCanvas);
        chartContainer.setPrefHeight(CHART_HEIGHT + 8);
        chartContainer.setStyle(String.format(
            "-fx-background-color: %s; " +
            "-fx-background-radius: 6;",
            MainView.BG_PRIMARY
        ));

        // Hacer responsive
        chartContainer.widthProperty().addListener((obs, old, w) -> {
            double newW = w.doubleValue() - 4;
            if (newW > 0) {
                chartCanvas.setWidth(newW);
                renderChart();
            }
        });

        // Dibujar gráfica vacía inicial
        renderChart();

        return chartContainer;
    }

    /**
     * Renderiza la gráfica SIR completa.
     * Muestra líneas de S, I, R, Q a lo largo del tiempo.
     */
    public void renderChart() {
        double w = chartCanvas.getWidth();
        double h = chartCanvas.getHeight();

        if (w <= 0 || h <= 0) return;

        // ── Limpiar ───────────────────────────────────────────────
        chartGC.setFill(Color.web(MainView.BG_PRIMARY));
        chartGC.fillRect(0, 0, w, h);

        double pad = CHART_PADDING;
        double plotW = w - pad * 2;
        double plotH = h - pad * 2 - 16; // 16px para labels de eje X

        // ── Ejes ──────────────────────────────────────────────────
        chartGC.setStroke(Color.web(MainView.BORDER_COLOR, 0.5));
        chartGC.setLineWidth(1);
        chartGC.strokeLine(pad, pad, pad, pad + plotH);           // Eje Y
        chartGC.strokeLine(pad, pad + plotH, pad + plotW, pad + plotH); // Eje X

        // ── Líneas de referencia horizontales ─────────────────────
        chartGC.setStroke(Color.web(MainView.BORDER_COLOR, 0.15));
        chartGC.setLineWidth(0.5);
        chartGC.setLineDashes(4, 4);
        for (int pct = 25; pct < 100; pct += 25) {
            double y = pad + plotH - (plotH * pct / 100.0);
            chartGC.strokeLine(pad, y, pad + plotW, y);
        }
        chartGC.setLineDashes();

        // ── Labels del eje Y ──────────────────────���───────────────
        chartGC.setFill(Color.web(MainView.TEXT_MUTED, 0.5));
        chartGC.setFont(Font.font("Consolas", 8));
        chartGC.setTextBaseline(javafx.geometry.VPos.CENTER);
        chartGC.setTextAlign(javafx.scene.text.TextAlignment.RIGHT);
        chartGC.fillText("100%", pad - 3, pad);
        chartGC.fillText("50%",  pad - 3, pad + plotH / 2);
        chartGC.fillText("0%",   pad - 3, pad + plotH);

        // ── Si no hay datos, mostrar mensaje ──────────────────────
        if (statsHistory.isEmpty()) {
            chartGC.setFill(Color.web(MainView.TEXT_MUTED, 0.3));
            chartGC.setFont(Font.font("Consolas", 12));
            chartGC.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
            chartGC.setTextBaseline(javafx.geometry.VPos.CENTER);
            chartGC.fillText("Sin datos", w / 2, h / 2);
            return;
        }

        // ── Dibujar líneas de datos ───────────────────────────────
        int dataPoints = statsHistory.size();
        double xStep = dataPoints > 1 ? plotW / (dataPoints - 1) : plotW;

        // Encontrar el total máximo para normalización
        int maxTotal = statsHistory.stream()
            .mapToInt(IterationStats::totalNodes)
            .max().orElse(1);

        // Dibujar cada serie: S, I, R, Q
        drawDataLine(chartGC, pad, plotH, xStep, maxTotal,
            statsHistory.stream().mapToInt(IterationStats::susceptibleCount).toArray(),
            MainView.TEXT_SECONDARY, "S");

        drawDataLine(chartGC, pad, plotH, xStep, maxTotal,
            statsHistory.stream().mapToInt(IterationStats::infectedCount).toArray(),
            MainView.ACCENT_RED, "I");

        drawDataLine(chartGC, pad, plotH, xStep, maxTotal,
            statsHistory.stream().mapToInt(IterationStats::recoveredCount).toArray(),
            MainView.ACCENT_GREEN, "R");

        drawDataLine(chartGC, pad, plotH, xStep, maxTotal,
            statsHistory.stream().mapToInt(IterationStats::quarantinedCount).toArray(),
            MainView.ACCENT_CYAN, "Q");

        // ── Leyenda ───────────────────────────────────────────────
        drawLegend(chartGC, w, h);

        // ── Label eje X ───────────────────────────────────────────
        chartGC.setFill(Color.web(MainView.TEXT_MUTED, 0.4));
        chartGC.setFont(Font.font("Consolas", 8));
        chartGC.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        chartGC.setTextBaseline(javafx.geometry.VPos.TOP);
        chartGC.fillText("Iteraciones →", w / 2, pad + plotH + 4);
    }

    /**
     * Dibuja una línea de datos en la gráfica con area fill semi-transparente.
     */
    private void drawDataLine(GraphicsContext gc, double pad, double plotH,
                              double xStep, int maxTotal, int[] data,
                              String colorHex, String label) {
        if (data.length == 0) return;

        Color lineColor = Color.web(colorHex);
        Color fillColor = Color.web(colorHex, 0.1);

        // ── Área rellena bajo la curva ────────────────────────────
        gc.setFill(fillColor);
        gc.beginPath();
        gc.moveTo(pad, pad + plotH); // Inicio en eje X

        for (int i = 0; i < data.length; i++) {
            double x = pad + i * xStep;
            double y = pad + plotH - (plotH * (double) data[i] / maxTotal);
            if (i == 0) gc.lineTo(x, y);
            else gc.lineTo(x, y);
        }

        gc.lineTo(pad + (data.length - 1) * xStep, pad + plotH); // Volver al eje X
        gc.closePath();
        gc.fill();

        // ── Línea ─────────────────────────────────────────────────
        gc.setStroke(lineColor);
        gc.setLineWidth(2.0);
        gc.setLineDashes();
        gc.beginPath();

        for (int i = 0; i < data.length; i++) {
            double x = pad + i * xStep;
            double y = pad + plotH - (plotH * (double) data[i] / maxTotal);
            if (i == 0) gc.moveTo(x, y);
            else gc.lineTo(x, y);
        }
        gc.stroke();

        // ── Puntos en cada dato ───────────────────────────────────
        gc.setFill(lineColor);
        for (int i = 0; i < data.length; i++) {
            double x = pad + i * xStep;
            double y = pad + plotH - (plotH * (double) data[i] / maxTotal);
            gc.fillOval(x - 2.5, y - 2.5, 5, 5);
        }
    }

    /**
     * Dibuja la leyenda de la gráfica en la esquina superior derecha.
     */
    private void drawLegend(GraphicsContext gc, double w, double h) {
        double legendX = w - 60;
        double legendY = 14;
        double lineH = 12;

        String[][] items = {
            {"S", MainView.TEXT_SECONDARY},
            {"I", MainView.ACCENT_RED},
            {"R", MainView.ACCENT_GREEN},
            {"Q", MainView.ACCENT_CYAN}
        };

        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 9));
        gc.setTextBaseline(javafx.geometry.VPos.CENTER);
        gc.setTextAlign(javafx.scene.text.TextAlignment.LEFT);

        for (int i = 0; i < items.length; i++) {
            double y = legendY + i * lineH;

            // Indicador de color
            gc.setFill(Color.web(items[i][1]));
            gc.fillOval(legendX, y - 3, 6, 6);

            // Label
            gc.setFill(Color.web(items[i][1], 0.8));
            gc.fillText(items[i][0], legendX + 10, y);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  CONSTRUCCIÓN: Info del Nodo Seleccionado
    // ═══════════════════════════════════════════════════════════════

    private void buildNodeInfoSection() {
        nodeInfoBox = new VBox(6);
        nodeInfoBox.setPadding(new Insets(4, 0, 4, 0));

        lblNodeId         = createInfoValue("—");
        lblNodeLabel      = createInfoValue("—");
        lblNodeStatus     = createInfoValue("—");
        lblNodeDepth      = createInfoValue("—");
        lblNodeInfectedBy = createInfoValue("—");
        lblNodeDegree     = createInfoValue("—");
        lblNodeSpreadCount = createInfoValue("—");
        lblNodeDuration   = createInfoValue("—");

        lblNoSelection = new Label("Selecciona un nodo para ver\nsu información detallada");
        lblNoSelection.setFont(Font.font("Consolas", 11));
        lblNoSelection.setTextFill(Color.web(MainView.TEXT_MUTED, 0.5));
        lblNoSelection.setWrapText(true);
        lblNoSelection.setPadding(new Insets(12, 0, 12, 0));
        lblNoSelection.setAlignment(Pos.CENTER);
        lblNoSelection.setMaxWidth(Double.MAX_VALUE);

        nodeInfoBox.getChildren().add(lblNoSelection);
    }

    /**
     * Actualiza la sección de info con los datos de un nodo.
     *
     * @param node el nodo seleccionado, o null para limpiar
     */
    public void updateNodeInfo(NodeData node) {
        nodeInfoBox.getChildren().clear();

        if (node == null) {
            nodeInfoBox.getChildren().add(lblNoSelection);
            return;
        }

        lblNodeId.setText(node.getId());
        lblNodeLabel.setText(node.getLabel());
        lblNodeDegree.setText(String.valueOf(node.getDegree()));

        // Estado con color
        NodeStatus status = node.getStatus();
        lblNodeStatus.setText(status.getIcon() + " " + status.getDisplayName());
        lblNodeStatus.setTextFill(Color.web(status.toHex()));

        // Profundidad
        int depth = node.getDepthLevel();
        lblNodeDepth.setText(depth >= 0 ? "Nivel " + depth : "N/A");
        if (depth >= 0) {
            lblNodeDepth.setTextFill(GraphCanvasView.getDepthColor(depth));
        } else {
            lblNodeDepth.setTextFill(Color.web(MainView.TEXT_SECONDARY));
        }

        // Infectado por
        String infBy = node.getInfectedBy();
        lblNodeInfectedBy.setText(infBy != null ? infBy : "N/A");

        // Nodos que infectó
        lblNodeSpreadCount.setText(node.getSpreadCount() + " nodos");

        // Duración de infección
        lblNodeDuration.setText(node.getInfectedDuration() + " iteraciones");

        nodeInfoBox.getChildren().addAll(
            createInfoLine("ID:",          lblNodeId),
            createInfoLine("Label:",       lblNodeLabel),
            createInfoLine("Estado:",      lblNodeStatus),
            createInfoLine("Profundidad:", lblNodeDepth),
            createInfoLine("Infectado por:", lblNodeInfectedBy),
            createInfoLine("Conexiones:",  lblNodeDegree),
            createInfoLine("Propagó a:",   lblNodeSpreadCount),
            createInfoLine("Duración:",    lblNodeDuration)
        );
    }

    // ═══════════════════════════════════════════════════════════════
    //  CONSTRUCCIÓN: Cola BFS
    // ═══════════════════════════════════════════════════════════════

    private void buildBFSQueueSection() {
        bfsQueueBox = new VBox(6);
        bfsQueueBox.setPadding(new Insets(4, 0, 4, 0));

        queueChipsContainer = new HBox(4);
        queueChipsContainer.setAlignment(Pos.CENTER_LEFT);
        queueChipsContainer.setWrapText(true);

        lblQueueEmpty = new Label("Cola vacía");
        lblQueueEmpty.setFont(Font.font("Consolas", 11));
        lblQueueEmpty.setTextFill(Color.web(MainView.TEXT_MUTED, 0.5));
        lblQueueEmpty.setPadding(new Insets(8, 0, 8, 0));

        bfsQueueBox.getChildren().add(lblQueueEmpty);
    }

    /**
     * Crea un HBox con wrapping simulado para los chips de la cola BFS.
     */
    private void setWrappingContent(HBox container) {
        container.setStyle("-fx-wrap-text: true;");
    }

    /**
     * Actualiza la visualización de la cola BFS.
     *
     * @param queueIds lista de IDs en la cola actual
     */
    public void updateBFSQueue(List<String> queueIds) {
        bfsQueueBox.getChildren().clear();

        if (queueIds == null || queueIds.isEmpty()) {
            bfsQueueBox.getChildren().add(lblQueueEmpty);
            return;
        }

        // Label de conteo
        Label countLabel = new Label("En cola: " + queueIds.size());
        countLabel.setFont(Font.font("Consolas", 10));
        countLabel.setTextFill(Color.web(MainView.TEXT_SECONDARY));
        bfsQueueBox.getChildren().add(countLabel);

        // Chips de nodos (flow layout simulado con HBoxes)
        HBox currentRow = new HBox(4);
        currentRow.setAlignment(Pos.CENTER_LEFT);
        int chipsPerRow = 4;
        int count = 0;

        for (String nodeId : queueIds) {
            Label chip = createQueueChip(nodeId);
            currentRow.getChildren().add(chip);
            count++;

            if (count % chipsPerRow == 0) {
                bfsQueueBox.getChildren().add(currentRow);
                currentRow = new HBox(4);
                currentRow.setAlignment(Pos.CENTER_LEFT);
            }
        }

        // Agregar última fila si tiene chips
        if (!currentRow.getChildren().isEmpty()) {
            bfsQueueBox.getChildren().add(currentRow);
        }
    }

    /**
     * Crea un chip visual para un nodo en la cola BFS.
     */
    private Label createQueueChip(String nodeId) {
        Label chip = new Label(nodeId);
        chip.setFont(Font.font("Consolas", FontWeight.BOLD, 10));
        chip.setTextFill(Color.web(MainView.ACCENT_RED));
        chip.setPadding(new Insets(2, 8, 2, 8));
        chip.setStyle(String.format(
            "-fx-background-color: %s; " +
            "-fx-background-radius: 10; " +
            "-fx-border-color: %s; " +
            "-fx-border-radius: 10; " +
            "-fx-border-width: 1;",
            MainView.BG_PRIMARY, MainView.ACCENT_RED + "44"
        ));

        // Glow sutil
        DropShadow glow = new DropShadow();
        glow.setColor(Color.web(MainView.ACCENT_RED, 0.25));
        glow.setRadius(6);
        chip.setEffect(glow);

        return chip;
    }

    // ═══════════════════════════════════════════════════════════════
    //  ACTUALIZACIÓN MASIVA DE ESTADÍSTICAS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Actualiza todas las estadísticas del dashboard.
     * Llamado cuando la iteración actual cambia.
     *
     * @param stats las estadísticas de la iteración actual
     */
    public void updateStats(IterationStats stats) {
        if (stats == null) return;

        int total = stats.totalNodes();
        if (total == 0) total = 1; // Evitar división por cero

        // ── Actualizar labels ─────────────────────────────────────
        lblSusceptible.setText(String.valueOf(stats.susceptibleCount()));
        lblInfected.setText(String.valueOf(stats.infectedCount()));
        lblRecovered.setText(String.valueOf(stats.recoveredCount()));
        lblQuarantined.setText(String.valueOf(stats.quarantinedCount()));
        lblTotalNodes.setText(String.valueOf(stats.totalNodes()));
        lblInfectionPct.setText(String.format("%.1f%%", stats.infectionPercentage()));

        // ── Actualizar barras ─────────────────────────────────────
        updateBar(fillSusceptible, barSusceptible, (double) stats.susceptibleCount() / total);
        updateBar(fillInfected,    barInfected,    (double) stats.infectedCount() / total);
        updateBar(fillRecovered,   barRecovered,   (double) stats.recoveredCount() / total);
        updateBar(fillQuarantined, barQuarantined, (double) stats.quarantinedCount() / total);
    }

    /**
     * Agrega un punto al historial de la gráfica y re-renderiza.
     *
     * @param stats estadísticas de la nueva iteración
     */
    public void addChartDataPoint(IterationStats stats) {
        if (stats != null) {
            statsHistory.add(stats);
            renderChart();
        }
    }

    /**
     * Reconstruye toda la gráfica desde el historial completo.
     */
    public void rebuildChart(LinkedHashMap<Integer, IterationStats> timeline) {
        statsHistory.clear();
        statsHistory.addAll(timeline.values());
        renderChart();
    }

    /**
     * Limpia todos los datos del dashboard.
     */
    public void clearAll() {
        statsHistory.clear();

        lblSusceptible.setText("0");
        lblInfected.setText("0");
        lblRecovered.setText("0");
        lblQuarantined.setText("0");
        lblTotalNodes.setText("0");
        lblInfectionPct.setText("0.0%");

        updateBar(fillSusceptible, barSusceptible, 0);
        updateBar(fillInfected,    barInfected,    0);
        updateBar(fillRecovered,   barRecovered,   0);
        updateBar(fillQuarantined, barQuarantined, 0);

        updateNodeInfo(null);
        updateBFSQueue(null);
        renderChart();
    }

    // ═══════════════════════════════════════════════════════════════
    //  BINDINGS CON EL MODELO
    // ═══════════════════════════════════════════════════════════════

    private void setupBindings() {
        SimulationState simState = model.getSimulationState();

        // Actualizar stats cuando cambie la iteración actual
        simState.currentIterationIndexProperty().addListener((obs, old, newVal) -> {
            var snapshot = simState.getCurrentSnapshot();
            if (snapshot != null) {
                updateStats(snapshot.stats());
                updateBFSQueue(snapshot.bfsQueueState());
            }
        });

        // Actualizar info del nodo cuando cambie la selección
        model.selectedNodeProperty().addListener((obs, old, newNode) -> {
            updateNodeInfo(newNode);
        });

        // Actualizar total de nodos
        model.nodeCountProperty().addListener((obs, old, newVal) -> {
            lblTotalNodes.setText(String.valueOf(newVal));
        });
    }

    // ═══════════════════════════════════════════════════════════════
    //  UTILIDADES DE CONSTRUCCIÓN UI
    // ═══════════════════════════════════════════════════════════════

    /**
     * Crea un widget de sección con título y contenido.
     */
    private VBox createSectionWidget(String title, javafx.scene.Node content) {
        // Título
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Consolas", FontWeight.BOLD, 11));
        titleLabel.setTextFill(Color.web(MainView.TEXT_MUTED));
        titleLabel.setPadding(new Insets(12, 12, 4, 12));
        titleLabel.setMaxWidth(Double.MAX_VALUE);

        // Contenido
        VBox wrapper = new VBox(0, titleLabel, new VBox() {{
            getChildren().add(content);
            setPadding(new Insets(4, 12, 8, 12));
        }});

        // Separador inferior
        Separator sep = new Separator();
        sep.setStyle(String.format("-fx-background-color: %s;", MainView.BORDER_COLOR));

        VBox section = new VBox(0, wrapper, sep);
        return section;
    }

    /**
     * Crea una línea de info: "Key:    Value"
     */
    private HBox createInfoLine(String key, Label valueLabel) {
        Label keyLabel = new Label(key);
        keyLabel.setFont(Font.font("Consolas", 10));
        keyLabel.setTextFill(Color.web(MainView.TEXT_MUTED));
        keyLabel.setMinWidth(90);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox line = new HBox(4, keyLabel, spacer, valueLabel);
        line.setAlignment(Pos.CENTER_LEFT);
        return line;
    }

    private Label createStatValue(String text, String color) {
        Label label = new Label(text);
        label.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
        label.setTextFill(Color.web(color));
        return label;
    }

    private Label createInfoValue(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Consolas", FontWeight.NORMAL, 11));
        label.setTextFill(Color.web(MainView.TEXT_PRIMARY));
        return label;
    }

    // ═══════════════════════════════════════════════════════════════
    //  GETTERS
    // ═══════════════════════════════════════════════════════════════

    public ScrollPane getRoot() { return root; }

}