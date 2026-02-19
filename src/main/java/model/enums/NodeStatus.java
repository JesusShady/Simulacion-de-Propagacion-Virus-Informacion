package model.enums;

import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;


public enum NodeStatus{

    SUSCEPTIBLE(
        "Susceptible",
        "Nodo sano, vulnerable a infeccion",
        Color.web("#B0BEC5"),           // Gris azulado elegante
        Color.web("#78909C"),           // Gris azulado oscuro (borde)
        Color.web("#CFD8DC"),           // Gris claro (hover/glow)
        "⬡",                            // Icono hexagono vacío
        0
    ),
    INFECTED(
        "Infectado",
        "Nodo activamente propagando el virus",
        Color.web("#FF1744"),
        Color.web("#D50000"),
        Color.web("#FF8A80"),
        "⬢",                            // Hexagono lleno
        1
    ),
    RECOVERED(
        "Recuperado",
        "Nodo que superó la infección, ahora inmune",
        Color.web("#00E676"),           // Verde neon
        Color.web("#00C853"),           // Verde oscuro (borde)
        Color.web("#B9F6CA"),           // Verde claro (glow)
        "◈",                            // Diamante con punto
        2
    ),

    QUARANTINED(
        "En Cuarentena",
        "Nodo aislado manualmente — bloquea la propagación",
        Color.web("#00E5FF"),           // Cyan electrico
        Color.web("#00B8D4"),           // Cyan oscuro (borde)
        Color.web("#84FFFF"),           // Cyan claro (glow)
        "⊘",                            // Simbolo de prohibido
        3
    ),

    PATIENT_ZERO(
        "Paciente Cero",
        "Punto de origen de la infección",
        Color.web("#D500F9"),           // Magenta/Purpura vibrante
        Color.web("#AA00FF"),           // Purpura oscuro (borde)
        Color.web("#EA80FC"),           // Purpura claro (glow pulsante)
        "☣",                            // Símbolo de biohazard
        4
    );

//----campos

private final String displayName;
private final String description;
private final Color primaryColor;
private final Color borderColor;
private final Color glowColor;
private final String icon;
private final int ordinalIndex;

//---constructor
 NodeStatus(String displayName, String description,
               Color primaryColor, Color borderColor, Color glowColor,
               String icon, int ordinalIndex) {
        this.displayName   = displayName;
        this.description   = description;
        this.primaryColor  = primaryColor;
        this.borderColor   = borderColor;
        this.glowColor     = glowColor;
        this.icon          = icon;
        this.ordinalIndex  = ordinalIndex;
    }

//---getters

    public String getDisplayName()  { return displayName;  }
    public String getDescription()  { return description;  }
    public Color  getPrimaryColor() { return primaryColor;  }
    public Color  getBorderColor()  { return borderColor;   }
    public Color  getGlowColor()    { return glowColor;     }
    public String getIcon()         { return icon;          }
    public int    getOrdinalIndex() { return ordinalIndex;  }

//---Metodos de utilidad

/*
Genera un color radiante tipo neon
*/
public RadialGradient toRadialGradient(){
    return new RadialGradient(
            0, 0,              // focusAngle, focusDistance
            0.5, 0.5,         // centerX, centerY (proporcional)
            0.55,              // radius (proporcional)
            true,              // proportional
            CycleMethod.NO_CYCLE,
            new Stop(0.0, glowColor),           // Centro: color brillante
            new Stop(0.5, primaryColor),         // Medio: color principal
            new Stop(1.0, borderColor)           // Borde: color oscuro
        );
}

/*
retorna el color glow como un string hexadecimal
*/
public String toHex(){
    return String.format("#%02X%02X%02X",
            (int) (primaryColor.getRed()   * 255),
            (int) (primaryColor.getGreen() * 255),
            (int) (primaryColor.getBlue()  * 255)
        );
}
/*
 Genera la regla CSS de GraphStream para este estado.
Incluye efectos visuales avanzados: sombras, bordes, tamaños
*/
public String toGraphStreamCSS(int size) {
        return String.format(
            "fill-color: %s; " +
            "stroke-mode: plain; " +
            "stroke-color: %s; " +
            "stroke-width: 2px; " +
            "size: %dpx; " +
            "shadow-mode: gradient-radial; " +
            "shadow-color: %s; " +
            "shadow-width: 8px; " +
            "shadow-offset: 0px, 0px; " +
            "text-color: white; " +
            "text-size: 14px; " +
            "text-style: bold; " +
            "text-alignment: under; " +
            "text-background-mode: rounded-box; " +
            "text-background-color: rgba(0,0,0,180); " +
            "text-padding: 4px;",
            toHex(), borderToHex(), size, getGlowHex()
        );
    }

 public String borderToHex() {
        return String.format("#%02X%02X%02X",
            (int) (borderColor.getRed()   * 255),
            (int) (borderColor.getGreen() * 255),
            (int) (borderColor.getBlue()  * 255)
        );
    }

    public String getGlowHex() {
        return String.format("#%02X%02X%02X",
                (int) (glowColor.getRed()   * 255),
                (int) (glowColor.getGreen() * 255),
                (int) (glowColor.getBlue()  * 255)
        );
    }


    /**
     * Determina si este estado permite que el nodo propague la infección.
     * Solo los nodos INFECTED y PATIENT_ZERO pueden contagiar.
     *
     * @return true si el nodo en este estado puede infectar vecinos
     */

  public boolean canSpread() {
        return this == INFECTED || this == PATIENT_ZERO;
    }

      /**
     * Determina si este estado permite que el nodo sea infectado.
     * Solo los nodos SUSCEPTIBLE pueden ser infectados.
     * Los nodos en QUARANTINED, RECOVERED o ya INFECTED no pueden.
     *
     * @return true si el nodo puede recibir la infección
     */
    public boolean canBeInfected() {
        return this == SUSCEPTIBLE;
    }

    /**
     * Determina si el nodo está activo en la simulacion.
     * Un nodo en cuarentena está "fuera de juego".
     *
     * @return true si el nodo participa activamente en la propagación
     */
    public boolean isActive() {
        return this != QUARANTINED;
    }

      /**
     * Retorna el siguiente estado lógico en la transición SIR.
     *   SUSCEPTIBLE  → INFECTED     (cuando BFS lo alcanza)
     *   INFECTED     → RECOVERED    (después de N iteraciones)
     *   PATIENT_ZERO → RECOVERED    (después de N iteraciones)
     *   RECOVERED    → RECOVERED    (estado final)
     *   QUARANTINED  → QUARANTINED  (estado bloqueado)
     *
     * @return el siguiente NodeStatus en la cadena de transición
     */

    public NodeStatus nextState() {
        return switch (this) {
            case SUSCEPTIBLE  -> INFECTED;
            case INFECTED     -> RECOVERED;
            case PATIENT_ZERO -> RECOVERED;
            case RECOVERED    -> RECOVERED;
            case QUARANTINED  -> QUARANTINED;
        };
    }

    @Override
    public String toString(){
        return String.format("%s [%s] %s", icon, displayName, description);
    }

}