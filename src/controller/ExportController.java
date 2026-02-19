package controller;

import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.GraphModel;
import model.SimulationState;
import view.MainView;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ════��═══════════════════════════════════════════════════════════════════
 *  CONTROLADOR DE EXPORTACIÓN — CSV / TXT
 * ════════════════════════════════════════════════════════════════════════
 *
 *  Maneja la exportación de los resultados de la simulación:
 *
 *  ┌─────��────────────────────────────────────────────────────────────┐
 *  │                                                                  │
 *  │   📁 CSV — Datos tabulares para análisis en Excel/Python         │
 *  │   ├── Una fila por nodo por iteración                           │
 *  │   ├── Columnas: Iter, ID, Estado, Profundidad, InfectadoPor    │
 *  │   └── Estadísticas agregadas por iteración                     │
 *  │                                                                  │
 *  │   📄 TXT — Reporte legible para documentación                   │
 *  │   ├── Header con parámetros de simulación                      │
 *  │   ├── Detalle iteración por iteración                          │
 *  │   ├── Eventos de propagación (quién → quién)                   │
 *  │   └── Estadísticas SIR por iteración                           │
 *  │                                                                  │
 *  │   Flujo:                                                        │
 *  │   Botón Export → Validar datos → FileChooser → Escribir → OK   │
 *  │                                                                  │
 *  └──────────────────────────────────────────────────────────────────┘
 *
 *  @author AikoCol27
 *  @version 1.0
 */
public class ExportController {

    // ═══════════════════════════════════════════════════════════════
    //  CONSTANTES
    // ═══════════════════════════════════════════════════════════════

    /** Formato para timestamps en nombres de archivo */
    private static final DateTimeFormatter FILE_TS_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    /** Nombre base para archivos exportados */
    private static final String BASE_FILENAME = "BFS_Simulation";

    // ══════════════════════════════════��════════════════════════════
    //  REFERENCIAS
    // ═══════════════════════════════════════════════════════════════

    private final GraphModel model;
    private final MainView   view;

    /** Último directorio usado (para recordar la ubicación) */
    private File lastDirectory;

    // ═══════════════════════════════════════════════════════════════
    //  CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════

    public ExportController(GraphModel model, MainView view) {
        this.model         = model;
        this.view          = view;
        this.lastDirectory = null;
    }

    // ═══════════════════════════════════════════════════════════════
    //  EXPORTAR A CSV
    // ═══════════════════════════════════════════════════════════════

    /**
     * Exporta los resultados de la simulación a un archivo CSV.
     *
     * Formato del CSV:
     *   Iteracion,NodoID,Label,Estado,Profundidad,InfectadoPor,
     *   Susceptibles,Infectados,Recuperados,Cuarentena,PorcentajeInfeccion
     */
    public void exportCSV() {
        // ── Validar que hay datos ─────────────────────────────────
        if (!validateExportData()) return;

        // ── Mostrar FileChooser ────────────────────────────��──────
        File file = showSaveDialog(
            "Exportar Resultados a CSV",
            "CSV Files",
            "*.csv",
            generateFilename("csv")
        );

        if (file == null) {
            System.out.println("[ExportController] Exportación CSV cancelada.");
            return;
        }

        // ── Generar contenido CSV ─────────────────────────────────
        String csvContent = model.getSimulationState().toCSV();

        // ── Escribir archivo ──────────────────────────────────────
        boolean success = writeToFile(file, csvContent);

        if (success) {
            showSuccessAlert(
                "📁 CSV Exportado",
                "El archivo se guardó exitosamente.",
                file
            );
            System.out.println("[ExportController] ✓ CSV exportado: "
                + file.getAbsolutePath());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  EXPORTAR A TXT
    // ═══════════════════════════════════════════════════════════════

    /**
     * Exporta un reporte legible de la simulación a un archivo TXT.
     *
     * Incluye:
     *   - Parámetros de simulación
     *   - Detalle de cada iteración
     *   - Eventos de propagación
     *   - Estadísticas SIR
     */
    public void exportTXT() {
        // ── Validar que hay datos ─────────────────────────────────
        if (!validateExportData()) return;

        // ── Mostrar FileChooser ───────────────────────────────────
        File file = showSaveDialog(
            "Exportar Reporte a TXT",
            "Text Files",
            "*.txt",
            generateFilename("txt")
        );

        if (file == null) {
            System.out.println("[ExportController] Exportación TXT cancelada.");
            return;
        }

        // ── Generar contenido TXT ─────────────────────────────────
        String txtContent = model.getSimulationState().toTXT();

        // ── Escribir archivo ──────────────────────────────────────
        boolean success = writeToFile(file, txtContent);

        if (success) {
            showSuccessAlert(
                "📄 TXT Exportado",
                "El reporte se guardó exitosamente.",
                file
            );
            System.out.println("[ExportController] ✓ TXT exportado: "
                + file.getAbsolutePath());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  VALIDACIÓN
    // ═════════���═════════════════════════════════════════════════════

    /**
     * Verifica que hay datos de simulación para exportar.
     *
     * @return true si se puede exportar
     */
    private boolean validateExportData() {
        SimulationState simState = model.getSimulationState();

        if (simState.getFullHistory().isEmpty()) {
            showAlert(
                "Sin datos para exportar",
                "No hay resultados de simulación.\n\n"
                + "Ejecuta una simulación primero usando el botón\n"
                + "'▶ SIMULAR' antes de exportar.",
                Alert.AlertType.INFORMATION
            );
            return false;
        }

        if (simState.getTotalIterations() == 0) {
            showAlert(
                "Simulación vacía",
                "La simulación no produjo iteraciones.\n"
                + "Verifica la configuración y vuelve a ejecutar.",
                Alert.AlertType.WARNING
            );
            return false;
        }

        return true;
    }

    // ═══════════════════════════════════════════════════════════════
    //  FILE CHOOSER
    // ═════════════════════════════════════════════════════════���═════

    /**
     * Muestra un FileChooser de guardado con el filtro y nombre especificados.
     *
     * @param title       título de la ventana
     * @param filterName  nombre del filtro (ej. "CSV Files")
     * @param filterExt   extensión del filtro (ej. "*.csv")
     * @param defaultName nombre por defecto del archivo
     * @return File seleccionado, o null si se canceló
     */
    private File showSaveDialog(String title, String filterName,
                                 String filterExt, String defaultName) {
        // Obtener Stage principal
        Stage stage = getStage();
        if (stage == null) return null;

        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);

        // Filtro de extensión
        FileChooser.ExtensionFilter filter =
            new FileChooser.ExtensionFilter(filterName, filterExt);
        chooser.getExtensionFilters().add(filter);
        chooser.setSelectedExtensionFilter(filter);

        // Nombre por defecto
        chooser.setInitialFileName(defaultName);

        // Directorio inicial (recordar el último usado)
        if (lastDirectory != null && lastDirectory.exists()) {
            chooser.setInitialDirectory(lastDirectory);
        } else {
            // Intentar usar el escritorio del usuario
            File desktop = new File(System.getProperty("user.home"), "Desktop");
            if (desktop.exists()) {
                chooser.setInitialDirectory(desktop);
            }
        }

        // Mostrar diálogo nativo
        File selectedFile = chooser.showSaveDialog(stage);

        // Recordar el directorio
        if (selectedFile != null) {
            lastDirectory = selectedFile.getParentFile();
        }

        return selectedFile;
    }

    // ═══════════════════════════════════════════════════════════════
    //  ESCRITURA DE ARCHIVOS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Escribe contenido a un archivo con manejo de errores.
     *
     * @param file    archivo destino
     * @param content contenido a escribir
     * @return true si la escritura fue exitosa
     */
    private boolean writeToFile(File file, String content) {
        try (PrintWriter writer = new PrintWriter(
                new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8))) {

            writer.write(content);
            writer.flush();
            return true;

        } catch (IOException e) {
            System.err.println("[ExportController] Error al escribir archivo: "
                + e.getMessage());

            showAlert(
                "Error de Escritura",
                "No se pudo guardar el archivo:\n\n"
                + file.getAbsolutePath() + "\n\n"
                + "Error: " + e.getMessage() + "\n\n"
                + "Verifica que tienes permisos de escritura\n"
                + "en la ubicación seleccionada.",
                Alert.AlertType.ERROR
            );

            return false;

        } catch (Exception e) {
            System.err.println("[ExportController] Error inesperado: "
                + e.getMessage());

            showAlert(
                "Error Inesperado",
                "Ocurrió un error inesperado al exportar:\n\n"
                + e.getMessage(),
                Alert.AlertType.ERROR
            );

            return false;
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  GENERACIÓN DE NOMBRES DE ARCHIVO
    // ═══════════════════════════════════════════════════════════════

    /**
     * Genera un nombre de archivo con timestamp.
     * Formato: BFS_Simulation_2026-02-19_14-30-00.csv
     *
     * @param extension extensión del archivo (sin punto)
     * @return nombre del archivo
     */
    private String generateFilename(String extension) {
        String timestamp = LocalDateTime.now().format(FILE_TS_FMT);
        SimulationState simState = model.getSimulationState();

        // Incluir info básica en el nombre
        int nodes = model.getNodeCount();
        int contagion = (int) (simState.getContagionRate() * 100);

        return String.format("%s_%s_N%d_C%d.%s",
            BASE_FILENAME,
            timestamp,
            nodes,
            contagion,
            extension
        );
    }

    // ═══════════════════════════════════════════════════════════════
    //  ALERTAS Y FEEDBACK
    // ═══════════════════════════════════════════════════════════════

    /**
     * Muestra una alerta de éxito con información del archivo guardado.
     */
    private void showSuccessAlert(String title, String message, File file) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(message);

        // Información detallada del archivo
        long fileSize = file.length();
        String sizeStr;
        if (fileSize < 1024) {
            sizeStr = fileSize + " bytes";
        } else if (fileSize < 1024 * 1024) {
            sizeStr = String.format("%.1f KB", fileSize / 1024.0);
        } else {
            sizeStr = String.format("%.2f MB", fileSize / (1024.0 * 1024.0));
        }

        SimulationState simState = model.getSimulationState();

        alert.setContentText(String.format(
            "Archivo: %s\n" +
            "Ubicación: %s\n" +
            "Tamaño: %s\n\n" +
            "Contenido:\n" +
            "  • %d iteraciones\n" +
            "  • %d nodos registrados\n" +
            "  • Tasa de contagio: %.0f%%\n" +
            "  • Tiempo de recuperación: %d iter",
            file.getName(),
            file.getParent(),
            sizeStr,
            simState.getTotalIterations(),
            model.getNodeCount(),
            simState.getContagionRate() * 100,
            simState.getRecoveryTime()
        ));

        styleAlert(alert);
        alert.showAndWait();

        // Actualizar status bar
        view.updateStatus("● EXPORTED", MainView.ACCENT_GREEN);
    }

    /**
     * Muestra una alerta genérica.
     */
    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        styleAlert(alert);
        alert.showAndWait();
    }

    /**
     * Aplica estilo dark cyberpunk a las alertas.
     */
    private void styleAlert(Alert alert) {
        var dialogPane = alert.getDialogPane();
        dialogPane.setStyle(String.format(
            "-fx-background-color: %s; " +
            "-fx-font-family: 'Consolas';",
            MainView.BG_SECONDARY
        ));

        try {
            dialogPane.lookup(".content").setStyle(String.format(
                "-fx-text-fill: %s; " +
                "-fx-font-family: 'Consolas'; " +
                "-fx-font-size: 12px;",
                MainView.TEXT_PRIMARY
            ));

            dialogPane.getButtonTypes().forEach(bt -> {
                var button = (javafx.scene.control.Button) dialogPane.lookupButton(bt);
                button.setStyle(String.format(
                    "-fx-background-color: %s; " +
                    "-fx-text-fill: %s; " +
                    "-fx-font-family: 'Consolas'; " +
                    "-fx-background-radius: 6; " +
                    "-fx-border-color: %s; " +
                    "-fx-border-radius: 6; " +
                    "-fx-cursor: hand;",
                    MainView.BG_TERTIARY, MainView.TEXT_PRIMARY, MainView.BORDER_COLOR
                ));
            });
        } catch (Exception e) {
            // Silenciar errores de styling
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  UTILIDADES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Obtiene el Stage principal de la aplicación.
     */
    private Stage getStage() {
        try {
            return (Stage) view.getRoot().getScene().getWindow();
        } catch (Exception e) {
            System.err.println("[ExportController] No se pudo obtener el Stage principal.");
            return null;
        }
    }
}