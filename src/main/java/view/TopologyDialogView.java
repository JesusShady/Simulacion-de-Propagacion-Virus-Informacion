package view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Separator;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import model.TopologyGenerator.TopologyType;

import java.util.HashMap;
import java.util.Map;


public class TopologyDialogView {
      // ═══════════════════════════════════════════════════════════════
    //  CONSTANTES
    // ═══════════════════════════════════════════════════════════════

    private static final double DIALOG_WIDTH   = 620;
    private static final double DIALOG_HEIGHT  = 580;
    private static final double CARD_SIZE      = 160;
    private static final double PREVIEW_SIZE   = 80;
    private static final double CARD_RADIUS    = 12;

    // ═══════════════════════════════════════════════════════════════
    //  COMPONENTES
    // ═══════════════════════════════════════════════════════════════

    private final Stage dialog;
    private final VBox  root;

    /** Tipo de topología seleccionada */
    private TopologyType selectedType;

    /** Número de nodos seleccionado */
    private int selectedNodeCount;

    /** ¿El usuario confirmó la generación? */
    private boolean confirmed;

    // ── Cards ─────────────────────────────────────────────────────
    private final Map<TopologyType, VBox> cards;
    private VBox activeCard;

    // ── Configuración ─────────────────────────────────────────────
    private Slider sliderNodes;
    private Label  lblNodeCount;
    private Label  lblDescription;
    private Label  lblRange;

    // ── Botones ───────────────────────────────────────────────────
    private Button btnGenerate;
    private Button btnCancel;

    // ═══════════════════════════════════════════════════════════════
    //  CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════

    /**
     * Crea el diálogo de topologías.
     *
     * @param owner la ventana padre (para centrar el diálogo)
     */
    public TopologyDialogView(Stage owner) {
        this.cards         = new HashMap<>();
        this.selectedType  = TopologyType.STAR; // Default
        this.selectedNodeCount = 15;
        this.confirmed     = false;

        // Crear el stage modal
        dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setResizable(false);

        // Construir contenido
        root = new VBox(0);
        root.setPrefSize(DIALOG_WIDTH, DIALOG_HEIGHT);
        root.setStyle(String.format(
            "-fx-background-color: %s; " +
            "-fx-background-radius: 16; " +
            "-fx-border-color: %s; " +
            "-fx-border-radius: 16; " +
            "-fx-border-width: 1;",
            MainView.BG_SECONDARY, MainView.BORDER_COLOR
        ));

        // Sombra del diálogo completo
        DropShadow dialogShadow = new DropShadow();
        dialogShadow.setColor(Color.BLACK.deriveColor(0, 1, 1, 0.6));
        dialogShadow.setRadius(30);
        dialogShadow.setSpread(0.1);
        root.setEffect(dialogShadow);

        buildHeader();
        buildCardGrid();
        buildConfiguration();
        buildFooter();

        // Escena
        StackPane sceneRoot = new StackPane(root);
        sceneRoot.setStyle("-fx-background-color: transparent;");
        Scene scene = new Scene(sceneRoot, DIALOG_WIDTH + 40, DIALOG_HEIGHT + 40);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);

        // Seleccionar la primera card por defecto
        selectCard(TopologyType.STAR);

        // Cerrar con ESC
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                confirmed = false;
                dialog.close();
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════
    //  HEADER
    // ═══════════════════════════════════════════════════════════════

    private void buildHeader() {
        // Título
        Label icon = new Label("🌐");
        icon.setFont(Font.font("Segoe UI Emoji", 22));
        icon.setEffect(new Glow(0.6));

        Label title = new Label("GENERAR TOPOLOGÍA DE RED");
        title.setFont(Font.font("Consolas", FontWeight.BOLD, 16));
        title.setTextFill(Color.web(MainView.ACCENT_CYAN));
        title.setEffect(new Glow(0.3));

        HBox titleBox = new HBox(10, icon, title);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        titleBox.setPadding(new Insets(16, 20, 8, 20));

        // Botón cerrar (X)
        Button btnClose = new Button("✕");
        btnClose.setFont(Font.font("Consolas", FontWeight.BOLD, 14));
        btnClose.setTextFill(Color.web(MainView.TEXT_MUTED));
        btnClose.setStyle(String.format(
            "-fx-background-color: transparent; " +
            "-fx-cursor: hand;"
        ));
        btnClose.setOnMouseEntered(e ->
            btnClose.setTextFill(Color.web(MainView.ACCENT_RED)));
        btnClose.setOnMouseExited(e ->
            btnClose.setTextFill(Color.web(MainView.TEXT_MUTED)));
        btnClose.setOnAction(e -> {
            confirmed = false;
            dialog.close();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(titleBox, spacer, btnClose);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(0, 16, 0, 0));

        Separator sep = new Separator();
        sep.setStyle(String.format("-fx-background-color: %s;", MainView.BORDER_COLOR));

        root.getChildren().addAll(header, sep);
    }

    // ═══════════════════════════════════════════════════════════════
    //  GRID DE CARDS
    // ═══════════════════════════════════════════════════════════════

    private void buildCardGrid() {
        FlowPane grid = new FlowPane(12, 12);
        grid.setAlignment(Pos.CENTER);
        grid.setPadding(new Insets(16, 20, 12, 20));

        for (TopologyType type : TopologyType.values()) {
            VBox card = createTopologyCard(type);
            cards.put(type, card);
            grid.getChildren().add(card);
        }

        root.getChildren().add(grid);
    }

    /**
     * Crea una card seleccionable para un tipo de topología.
     */
    private VBox createTopologyCard(TopologyType type) {
        VBox card = new VBox(4);
        card.setPrefSize(CARD_SIZE, CARD_SIZE);
        card.setMaxSize(CARD_SIZE, CARD_SIZE);
        card.setMinSize(CARD_SIZE, CARD_SIZE);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(8));
        card.setCursor(javafx.scene.Cursor.HAND);

        // Estilo base
        applyCardStyle(card, false);

        // ── Ícono ─────────────────────────────────────────────────
        Label iconLabel = new Label(type.getIcon());
        iconLabel.setFont(Font.font("Segoe UI Emoji", 24));

        // ── Nombre ────────────────────────────────────────────────
        Label nameLabel = new Label(type.getDisplayName());
        nameLabel.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
        nameLabel.setTextFill(Color.web(MainView.TEXT_PRIMARY));

        // ── Preview mini del grafo ────────────────────────────────
        Canvas preview = new Canvas(PREVIEW_SIZE, PREVIEW_SIZE);
        drawTopologyPreview(preview.getGraphicsContext2D(), type, PREVIEW_SIZE);

        // ── Rango de nodos ────────────────────────────────────────
        Label rangeLabel = new Label(type.getMinNodes() + "-" + type.getMaxNodes() + " nodos");
        rangeLabel.setFont(Font.font("Consolas", 9));
        rangeLabel.setTextFill(Color.web(MainView.TEXT_MUTED));

        card.getChildren().addAll(iconLabel, nameLabel, preview, rangeLabel);

        // ── Eventos ───────────────────────────────────────────────
        card.setOnMouseEntered(e -> {
            if (selectedType != type) {
                applyCardHoverStyle(card);
            }
        });

        card.setOnMouseExited(e -> {
            if (selectedType != type) {
                applyCardStyle(card, false);
            }
        });

        card.setOnMouseClicked(e -> selectCard(type));

        return card;
    }

    /**
     * Selecciona una card y actualiza toda la UI.
     */
    private void selectCard(TopologyType type) {
        // Desactivar card anterior
        if (activeCard != null) {
            applyCardStyle(activeCard, false);
        }

        selectedType = type;
        activeCard = cards.get(type);

        // Activar nueva card
        applyCardStyle(activeCard, true);

        // Actualizar slider de nodos
        sliderNodes.setMin(type.getMinNodes());
        sliderNodes.setMax(type.getMaxNodes());
        selectedNodeCount = (int) Math.min(
            Math.max(selectedNodeCount, type.getMinNodes()),
            type.getMaxNodes()
        );
        sliderNodes.setValue(selectedNodeCount);

        // Actualizar descripción
        lblDescription.setText(type.getDescription());

        // Actualizar rango
        lblRange.setText(String.format("Rango: %d — %d nodos",
            type.getMinNodes(), type.getMaxNodes()));
    }

    /**
     * Estilo base de una card (seleccionada o no).
     */
    private void applyCardStyle(VBox card, boolean selected) {
        if (selected) {
            card.setStyle(String.format(
                "-fx-background-color: %s; " +
                "-fx-background-radius: %f; " +
                "-fx-border-color: %s; " +
                "-fx-border-radius: %f; " +
                "-fx-border-width: 2;",
                MainView.BG_TERTIARY, CARD_RADIUS,
                MainView.ACCENT_CYAN, CARD_RADIUS
            ));

            DropShadow glow = new DropShadow();
            glow.setColor(Color.web(MainView.ACCENT_CYAN, 0.4));
            glow.setRadius(18);
            glow.setSpread(0.15);
            card.setEffect(glow);
        } else {
            card.setStyle(String.format(
                "-fx-background-color: %s; " +
                "-fx-background-radius: %f; " +
                "-fx-border-color: %s; " +
                "-fx-border-radius: %f; " +
                "-fx-border-width: 1;",
                MainView.BG_TERTIARY, CARD_RADIUS,
                MainView.BORDER_COLOR, CARD_RADIUS
            ));
            card.setEffect(null);
        }
    }

    /**
     * Estilo hover de una card no seleccionada.
     */
    private void applyCardHoverStyle(VBox card) {
        card.setStyle(String.format(
            "-fx-background-color: %s; " +
            "-fx-background-radius: %f; " +
            "-fx-border-color: %s; " +
            "-fx-border-radius: %f; " +
            "-fx-border-width: 1;",
            MainView.BG_HOVER, CARD_RADIUS,
            MainView.TEXT_MUTED, CARD_RADIUS
        ));

        DropShadow hoverGlow = new DropShadow();
        hoverGlow.setColor(Color.web(MainView.TEXT_MUTED, 0.2));
        hoverGlow.setRadius(10);
        card.setEffect(hoverGlow);
    }

    // ═══════════════════════════════════════════════════════════════
    //  PREVIEW DE TOPOLOGÍAS (Mini Canvas)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Dibuja una representación miniatura de cada topología.
     */
    private void drawTopologyPreview(GraphicsContext gc, TopologyType type, double size) {
        // Fondo
        gc.setFill(Color.web(MainView.BG_PRIMARY, 0.6));
        gc.fillRoundRect(0, 0, size, size, 8, 8);

        double cx = size / 2;
        double cy = size / 2;
        double r  = size * 0.35;
        double nodeR = 4;

        gc.setStroke(Color.web(MainView.ACCENT_CYAN, 0.4));
        gc.setLineWidth(1);
        gc.setFill(Color.web(MainView.ACCENT_CYAN, 0.8));

        switch (type) {
            case STAR -> drawStarPreview(gc, cx, cy, r, nodeR);
            case MESH -> drawMeshPreview(gc, cx, cy, r, nodeR);
            case SCALE_FREE -> drawScaleFreePreview(gc, cx, cy, r, nodeR);
            case RING -> drawRingPreview(gc, cx, cy, r, nodeR);
            case TREE -> drawTreePreview(gc, cx, cy, r, nodeR);
            case RANDOM -> drawRandomPreview(gc, cx, cy, r, nodeR);
        }
    }

    private void drawStarPreview(GraphicsContext gc, double cx, double cy,
                                  double r, double nodeR) {
        // Centro
        gc.setFill(Color.web(MainView.ACCENT_MAGENTA, 0.9));
        gc.fillOval(cx - nodeR, cy - nodeR, nodeR * 2, nodeR * 2);

        // Periféricos
        int n = 6;
        gc.setFill(Color.web(MainView.ACCENT_CYAN, 0.8));
        for (int i = 0; i < n; i++) {
            double angle = (2 * Math.PI * i) / n - Math.PI / 2;
            double px = cx + r * Math.cos(angle);
            double py = cy + r * Math.sin(angle);
            gc.strokeLine(cx, cy, px, py);
            gc.fillOval(px - nodeR, py - nodeR, nodeR * 2, nodeR * 2);
        }
    }

    private void drawMeshPreview(GraphicsContext gc, double cx, double cy,
                                  double r, double nodeR) {
        int rows = 3, cols = 3;
        double spacing = r * 0.8;
        double startX = cx - spacing;
        double startY = cy - spacing;

        double[][] positions = new double[rows * cols][2];
        int idx = 0;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                double px = startX + col * spacing;
                double py = startY + row * spacing;
                positions[idx++] = new double[]{px, py};
            }
        }

        // Aristas horizontales y verticales
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int i = row * cols + col;
                if (col + 1 < cols) {
                    int j = row * cols + (col + 1);
                    gc.strokeLine(positions[i][0], positions[i][1],
                                  positions[j][0], positions[j][1]);
                }
                if (row + 1 < rows) {
                    int j = (row + 1) * cols + col;
                    gc.strokeLine(positions[i][0], positions[i][1],
                                  positions[j][0], positions[j][1]);
                }
            }
        }

        // Nodos
        for (double[] pos : positions) {
            gc.fillOval(pos[0] - nodeR, pos[1] - nodeR, nodeR * 2, nodeR * 2);
        }
    }

    private void drawScaleFreePreview(GraphicsContext gc, double cx, double cy,
                                       double r, double nodeR) {
        // Hub grande
        gc.setFill(Color.web(MainView.ACCENT_MAGENTA, 0.9));
        gc.fillOval(cx - nodeR * 1.5, cy - nodeR * 1.5, nodeR * 3, nodeR * 3);

        // Nodos distribuidos irregularmente (algunos clusters)
        double[][] nodes = {
            {cx - r * 0.7, cy - r * 0.4},
            {cx + r * 0.6, cy - r * 0.6},
            {cx - r * 0.3, cy + r * 0.7},
            {cx + r * 0.8, cy + r * 0.3},
            {cx - r * 0.9, cy + r * 0.2},
            {cx + r * 0.2, cy - r * 0.9},
            {cx + r * 0.5, cy + r * 0.8},
        };

        gc.setFill(Color.web(MainView.ACCENT_CYAN, 0.8));
        for (double[] pos : nodes) {
            gc.strokeLine(cx, cy, pos[0], pos[1]);
            gc.fillOval(pos[0] - nodeR, pos[1] - nodeR, nodeR * 2, nodeR * 2);
        }

        // Algunas conexiones entre periféricos
        gc.setStroke(Color.web(MainView.ACCENT_CYAN, 0.2));
        gc.strokeLine(nodes[0][0], nodes[0][1], nodes[4][0], nodes[4][1]);
        gc.strokeLine(nodes[1][0], nodes[1][1], nodes[5][0], nodes[5][1]);
        gc.strokeLine(nodes[3][0], nodes[3][1], nodes[6][0], nodes[6][1]);
        gc.setStroke(Color.web(MainView.ACCENT_CYAN, 0.4));
    }

    private void drawRingPreview(GraphicsContext gc, double cx, double cy,
                                  double r, double nodeR) {
        int n = 8;
        double[][] positions = new double[n][2];

        for (int i = 0; i < n; i++) {
            double angle = (2 * Math.PI * i) / n - Math.PI / 2;
            positions[i][0] = cx + r * Math.cos(angle);
            positions[i][1] = cy + r * Math.sin(angle);
        }

        // Aristas del anillo
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            gc.strokeLine(positions[i][0], positions[i][1],
                          positions[j][0], positions[j][1]);
        }

        // Nodos
        for (double[] pos : positions) {
            gc.fillOval(pos[0] - nodeR, pos[1] - nodeR, nodeR * 2, nodeR * 2);
        }
    }

    private void drawTreePreview(GraphicsContext gc, double cx, double cy,
                                  double r, double nodeR) {
        // Raíz
        double rootX = cx, rootY = cy - r * 0.8;
        gc.setFill(Color.web(MainView.ACCENT_MAGENTA, 0.9));
        gc.fillOval(rootX - nodeR * 1.3, rootY - nodeR * 1.3,
                    nodeR * 2.6, nodeR * 2.6);

        // Nivel 1
        double[][] level1 = {
            {cx - r * 0.7, cy},
            {cx,           cy},
            {cx + r * 0.7, cy}
        };

        gc.setFill(Color.web(MainView.ACCENT_CYAN, 0.8));
        for (double[] pos : level1) {
            gc.strokeLine(rootX, rootY, pos[0], pos[1]);
            gc.fillOval(pos[0] - nodeR, pos[1] - nodeR, nodeR * 2, nodeR * 2);
        }

        // Nivel 2 (solo bajo primer y tercer hijo)
        double[][] level2 = {
            {cx - r * 0.95, cy + r * 0.7},
            {cx - r * 0.45, cy + r * 0.7},
            {cx + r * 0.45, cy + r * 0.7},
            {cx + r * 0.95, cy + r * 0.7},
        };

        for (int i = 0; i < level2.length; i++) {
            int parent = (i < 2) ? 0 : 2;
            gc.strokeLine(level1[parent][0], level1[parent][1],
                          level2[i][0], level2[i][1]);
            gc.fillOval(level2[i][0] - nodeR, level2[i][1] - nodeR,
                        nodeR * 2, nodeR * 2);
        }
    }

    private void drawRandomPreview(GraphicsContext gc, double cx, double cy,
                                    double r, double nodeR) {
        // Posiciones pseudo-aleatorias (fijas para consistencia)
        double[][] nodes = {
            {cx - r * 0.6, cy - r * 0.7},
            {cx + r * 0.4, cy - r * 0.5},
            {cx - r * 0.8, cy + r * 0.1},
            {cx + r * 0.9, cy - r * 0.1},
            {cx - r * 0.2, cy + r * 0.6},
            {cx + r * 0.6, cy + r * 0.7},
            {cx,           cy},
        };

        // Aristas aleatorias
        int[][] edgePairs = {
            {0, 1}, {0, 2}, {1, 3}, {2, 4}, {3, 5},
            {4, 6}, {5, 6}, {0, 6}, {1, 6}, {2, 6}
        };

        for (int[] pair : edgePairs) {
            gc.strokeLine(nodes[pair[0]][0], nodes[pair[0]][1],
                          nodes[pair[1]][0], nodes[pair[1]][1]);
        }

        for (double[] pos : nodes) {
            gc.fillOval(pos[0] - nodeR, pos[1] - nodeR, nodeR * 2, nodeR * 2);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  CONFIGURACIÓN (Slider de nodos)
    // ═══════════════════════════════════════════════════════════════

    private void buildConfiguration() {
        Separator sep = new Separator();
        sep.setStyle(String.format("-fx-background-color: %s;", MainView.BORDER_COLOR));

        // ── Slider de nodos ───────────────────────────────────────
        Label lblNodesTitle = new Label("NODOS");
        lblNodesTitle.setFont(Font.font("Consolas", FontWeight.BOLD, 10));
        lblNodesTitle.setTextFill(Color.web(MainView.TEXT_MUTED));

        lblNodeCount = new Label("15");
        lblNodeCount.setFont(Font.font("Consolas", FontWeight.BOLD, 20));
        lblNodeCount.setTextFill(Color.web(MainView.ACCENT_CYAN));
        lblNodeCount.setEffect(new Glow(0.3));

        sliderNodes = new Slider(5, 50, 15);
        sliderNodes.setPrefWidth(300);
        sliderNodes.setBlockIncrement(1);
        sliderNodes.setMajorTickUnit(5);
        sliderNodes.setMinorTickCount(4);
        sliderNodes.setSnapToTicks(true);
        sliderNodes.setShowTickMarks(true);

        sliderNodes.setStyle(String.format(
            "-fx-control-inner-background: %s; " +
            "-fx-accent: %s;",
            MainView.BG_TERTIARY, MainView.ACCENT_CYAN
        ));

        // Estilizar cuando el skin esté listo
        sliderNodes.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            if (newSkin != null) {
                sliderNodes.lookup(".track").setStyle(String.format(
                    "-fx-background-color: %s; " +
                    "-fx-background-radius: 4; " +
                    "-fx-pref-height: 6;",
                    MainView.BG_TERTIARY
                ));
                sliderNodes.lookup(".thumb").setStyle(String.format(
                    "-fx-background-color: %s; " +
                    "-fx-background-radius: 10; " +
                    "-fx-pref-width: 20; " +
                    "-fx-pref-height: 20; " +
                    "-fx-effect: dropshadow(gaussian, %s, 10, 0.3, 0, 0);",
                    MainView.ACCENT_CYAN, MainView.ACCENT_CYAN
                ));
            }
        });

        sliderNodes.valueProperty().addListener((obs, old, newVal) -> {
            selectedNodeCount = newVal.intValue();
            lblNodeCount.setText(String.valueOf(selectedNodeCount));
        });

        lblRange = new Label("Rango: 5 — 50 nodos");
        lblRange.setFont(Font.font("Consolas", 9));
        lblRange.setTextFill(Color.web(MainView.TEXT_MUTED));

        HBox sliderRow = new HBox(12, lblNodesTitle, sliderNodes, lblNodeCount);
        sliderRow.setAlignment(Pos.CENTER);

        // ── Descripción ────���──────────────────────────────────────
        lblDescription = new Label(TopologyType.STAR.getDescription());
        lblDescription.setFont(Font.font("Consolas", 11));
        lblDescription.setTextFill(Color.web(MainView.TEXT_SECONDARY));
        lblDescription.setWrapText(true);
        lblDescription.setMaxWidth(DIALOG_WIDTH - 60);
        lblDescription.setPadding(new Insets(4, 0, 4, 0));

        VBox configBox = new VBox(8, sliderRow, lblRange, lblDescription);
        configBox.setAlignment(Pos.CENTER);
        configBox.setPadding(new Insets(12, 20, 8, 20));

        root.getChildren().addAll(sep, configBox);
    }

    // ═══════════════════════════════════════════════════════════════
    //  FOOTER (Botones de acción)
    // ═══════════════════════════════════════════════════════════════

    private void buildFooter() {
        Separator sep = new Separator();
        sep.setStyle(String.format("-fx-background-color: %s;", MainView.BORDER_COLOR));

        // ── Botón Cancelar ────────────────────────────────────────
        btnCancel = new Button("Cancelar");
        btnCancel.setPrefHeight(36);
        btnCancel.setPrefWidth(100);
        btnCancel.setFont(Font.font("Consolas", 12));
        btnCancel.setTextFill(Color.web(MainView.TEXT_SECONDARY));
        btnCancel.setCursor(javafx.scene.Cursor.HAND);

        String cancelBaseStyle = String.format(
            "-fx-background-color: %s; " +
            "-fx-background-radius: %f; " +
            "-fx-border-color: %s; " +
            "-fx-border-radius: %f; " +
            "-fx-border-width: 1;",
            MainView.BG_TERTIARY, MainView.CORNER_RADIUS,
            MainView.BORDER_COLOR, MainView.CORNER_RADIUS
        );
        btnCancel.setStyle(cancelBaseStyle);

        btnCancel.setOnMouseEntered(e -> {
            btnCancel.setStyle(String.format(
                "-fx-background-color: %s; " +
                "-fx-background-radius: %f; " +
                "-fx-border-color: %s; " +
                "-fx-border-radius: %f; " +
                "-fx-border-width: 1;",
                MainView.BG_HOVER, MainView.CORNER_RADIUS,
                MainView.TEXT_MUTED, MainView.CORNER_RADIUS
            ));
        });
        btnCancel.setOnMouseExited(e -> btnCancel.setStyle(cancelBaseStyle));

        btnCancel.setOnAction(e -> {
            confirmed = false;
            dialog.close();
        });

        // ── Botón Generar ─────────────────────────────────────────
        btnGenerate = new Button("✨  Generar Red");
        btnGenerate.setPrefHeight(36);
        btnGenerate.setPrefWidth(160);
        btnGenerate.setFont(Font.font("Consolas", FontWeight.BOLD, 13));
        btnGenerate.setTextFill(Color.web(MainView.BG_PRIMARY));
        btnGenerate.setCursor(javafx.scene.Cursor.HAND);

        String genBaseStyle = String.format(
            "-fx-background-color: linear-gradient(to right, %s, %s); " +
            "-fx-background-radius: %f;",
            MainView.ACCENT_CYAN, MainView.ACCENT_MAGENTA,
            MainView.CORNER_RADIUS
        );
        btnGenerate.setStyle(genBaseStyle);

        DropShadow genGlow = new DropShadow();
        genGlow.setColor(Color.web(MainView.ACCENT_CYAN, 0.5));
        genGlow.setRadius(15);
        genGlow.setSpread(0.1);
        btnGenerate.setEffect(genGlow);

        btnGenerate.setOnMouseEntered(e -> {
            btnGenerate.setStyle(String.format(
                "-fx-background-color: linear-gradient(to right, %s, %s); " +
                "-fx-background-radius: %f;",
                MainView.ACCENT_MAGENTA, MainView.ACCENT_CYAN,
                MainView.CORNER_RADIUS
            ));
            genGlow.setRadius(22);
            genGlow.setSpread(0.2);
        });

        btnGenerate.setOnMouseExited(e -> {
            btnGenerate.setStyle(genBaseStyle);
            genGlow.setRadius(15);
            genGlow.setSpread(0.1);
        });

        btnGenerate.setOnAction(e -> {
            confirmed = true;
            dialog.close();
        });

        // ── Layout ────────────────────────────────────────────────
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox footer = new HBox(12, spacer, btnCancel, btnGenerate);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(12, 20, 16, 20));

        root.getChildren().addAll(sep, footer);
    }

    // ═══════════════════════════════════════════════════════════════
    //  API PÚBLICA
    // ═══════════════════════════════════════════════════════════════

    /**
     * Muestra el diálogo y bloquea hasta que el usuario confirme o cancele.
     *
     * @return true si el usuario confirmó "Generar Red"
     */
    public boolean showAndWait() {
        confirmed = false;
        dialog.showAndWait();
        return confirmed;
    }

    /**
     * @return el tipo de topología seleccionado
     */
    public TopologyType getSelectedType() {
        return selectedType;
    }

    /**
     * @return el número de nodos seleccionado
     */
    public int getSelectedNodeCount() {
        return selectedNodeCount;
    }

    /**
     * @return true si el usuario confirmó la generación
     */
    public boolean isConfirmed() {
        return confirmed;
    }

    /**
     * @return referencia al Stage del diálogo (para posicionamiento)
     */
    public Stage getDialog() {
        return dialog;
    }
}

