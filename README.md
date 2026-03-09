<div align="center">

# ☣ BFS VIRUS PROPAGATION SIMULATOR

### Simulador Visual de Propagación de Virus usando BFS + Modelo SIR

<br>

```
     ██████╗ ███████╗███████╗    ██╗   ██╗██╗██████╗ ██╗   ██╗███████╗
     ██╔══██╗██╔════╝██╔════╝    ██║   ██║██║██╔══██╗██║   ██║██╔════╝
     ██████╔╝█████╗  ███████╗    ██║   ██║██║██████╔╝██║   ██║███████╗
     ██╔══██╗██╔══╝  ╚════██║    ╚██╗ ██╔╝██║██╔══██╗██║   ██║╚════██║
     ██████╔╝██║     ███████║     ╚████╔╝ ██║██║  ██║╚██████╔╝███████║
     ╚═════╝ ╚═╝     ��══════╝      ╚═══╝  ╚═╝╚═╝  ╚═╝ ╚═════╝ ╚══════╝

          P  R  O  P  A  G  A  T  I  O  N     S  I  M  U  L  A  T  O  R
```

<br>

[![Java](https://img.shields.io/badge/Java-21+-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-007ACC?style=for-the-badge&logo=java&logoColor=white)](https://openjfx.io/)
[![GraphStream](https://img.shields.io/badge/GraphStream-2.0-00C853?style=for-the-badge)](http://graphstream-project.org/)
[![Maven](https://img.shields.io/badge/Maven-3.8+-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge)](LICENSE)

<br>

**Visualiza, experimenta y comprende cómo un virus se propaga a través de una red**
**usando el algoritmo BFS y el modelo epidemiológico SIR**

<br>

[📖 Guía de Usuario](#-guía-de-usuario) •
[🚀 Inicio Rápido](#-inicio-rápido) •
[✨ Funcionalidades](#-funcionalidades) •
[🏗️ Arquitectura](#️-arquitectura)

</div>

---

<br>

## 🎯 ¿Qué es esto?

**BFS Virus Propagation Simulator** es una aplicación de escritorio que simula visualmente cómo un virus se propaga a través de una red de personas usando:

- 🔍 **BFS (Breadth-First Search)** — El virus se expande nivel por nivel desde los pacientes cero
- 🦠 **Modelo SIR** — Cada persona pasa por estados: **S**usceptible → **I**nfectado → **R**ecuperado
- 🎮 **Controles interactivos** — Crea redes, configura parámetros, y observa la propagación en tiempo real

```
                    ┌─────────────┐
                    │  SUSCEPTIBLE │  Sano, puede ser
                    │     (S)     │  infectado
                    └──────┬──────┘
                           │
                    contagio (BFS)
                           │
                    ┌──────▼──────┐
                    │  INFECTADO  │  Propagando el
                    │     (I)     │  virus activamente
                    └──────┬──────┘
                           │
                  recuperación (N iter)
                           │
                    ┌──────▼──────┐
                    │ RECUPERADO  │  Inmune, no puede
                    │     (R)     │  ser reinfectado
                    └─────────────┘
```

<br>

## 🚀 Inicio Rápido

### Prerrequisitos

| Herramienta | Versión mínima |
|-------------|---------------|
| ☕ Java JDK | 21+           |
| 📦 Maven   | 3.8+          |

### Clonar y ejecutar

```bash
# Clonar el repositorio
git clone https://github.com/JesusShady/SimulacionPropagacion.git

# Entrar al directorio
cd SimulacionPropagacion

# Compilar y ejecutar
mvn clean javafx:run
```

> 💡 **¡Eso es todo!** Maven descarga las dependencias automáticamente.

### Verificar Java

```bash
java --version
# Debe mostrar: java 21.x.x o superior
```

<br>

## ✨ Funcionalidades

### 🎨 11 Funcionalidades Principales

<table>
<tr>
<td width="50%">

#### 🖱️ 1. Lienzo Interactivo
Crea nodos con clic, arrástralos para moverlos,
y conecta nodos arrastrando entre ellos.

#### 🌐 2. Topologías Predefinidas
6 tipos de redes automáticas: Estrella, Malla,
Libre de Escala, Anillo, Árbol y Aleatoria.

#### 👑 3. Súper Propagador
Identifica automáticamente el nodo más peligroso
de la red (mayor centralidad de grado).

#### ☣️ 4. Múltiples Pacientes Cero
Selecciona uno o varios nodos como origen de
la infección para comparar escenarios.

#### ⊘ 5. Cuarentena + Cortafuegos
Aísla nodos y desactiva aristas para bloquear
la propagación estratégicamente.

#### 🎲 6. Propagación Probabilística
Tasa de contagio ajustable (0-100%).
No todos los contactos resultan en infección.

</td>
<td width="50%">

#### 🎮 7. Control Granular
Controles estilo reproductor de video:
Play, Pausa, Paso a paso, Adelante, Atrás.

#### 🌡️ 8. Mapa de Calor BFS
Colores por profundidad: Rojo (origen) →
Naranja → Amarillo → Verde → Cyan (lejano).

#### 🔄 9. Modelo SIR Completo
Susceptible → Infectado → Recuperado con
tiempo de recuperación configurable.

#### 📊 10. Dashboard en Tiempo Real
Gráfica SIR, barras de progreso, info del nodo
seleccionado, y cola BFS en vivo.

#### 📁 11. Exportación CSV/TXT
Exporta resultados tabulares (CSV) o reportes
legibles (TXT) para análisis externo.

</td>
</tr>
</table>

<br>

### 🌐 Topologías de Red

```
   ⭐ ESTRELLA          ◻️ MALLA            🌐 LIBRE ESCALA
                                          
      ⬡                ⬡──⬡──⬡              ⬡──⬡
     /│\               │  │  │              / │
   ⬡──●──⬡            ⬡──⬡──⬡          ⬡──●──⬡──⬡
     \│/               │  │  │              \ │
      ⬡                ⬡──⬡──⬡              ⬡──⬡──⬡
                                          

   🔄 ANILLO            🌳 ÁRBOL            🎲 ALEATORIA
                                          
   ⬡──⬡──⬡               ⬡               ⬡──⬡   ⬡
   │     │              / \              │ X     │
   ⬡     ⬡            ⬡   ⬡             ⬡──⬡──⬡
   │     │            /\ /\             
   ⬡──⬡──⬡          ⬡ ⬡ ⬡ ⬡            
```

<br>

### 🎮 Controles de Reproducción

```
┌────────────────────────────────────────────────────────────────────┐
│                                                                    │
│  ┌──────────┐   ⏮  ⏪  ▶/⏸  ⏩  ⏭    ──●── Velocidad            │
│  │▶ SIMULAR │                                                      │
│  └──────────┘   Iter: 3/15             ──●── Contagio: 70%        │
│                                                                    │
│                 ═══●═══════════         [📁 CSV] [📄 TXT]         │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

<br>

## 📊 Dashboard

```
┌─────────────────────────────┐
│  📊  ESTADÍSTICAS            │
│                              │
│  S ████████████░░░░░  78%   │
│  I ██████░░░░░░░░░░░  15%   │
│  R ██░░░░░░░░░░░░░░░   5%   │
│  Q █░░░░░░░░░░░░░░░░   2%   │
│                              │
├──────────────────────────────┤
│  📈  GRÁFICA SIR             │
│                              │
│  100%│╲S                     │
│      │  ╲     ╱── R         │
│   50%│   ╲   ╱               │
│      │    ╲╱ I               │
│    0%│────────── Q           │
│      └──────────→            │
│                              │
├──────────────────────────────┤
│  🔍  NODO SELECCIONADO       │
│                              │
│  ID:          N7             │
│  Estado:      ⬢ Infectado   │
│  Profundidad: Nivel 2       │
│  Infectado:   por N3        │
│  Propagó a:   3 nodos       │
│                              │
├──────────────────────────────┤
│  📋  COLA BFS                │
│  [N12] [N15] [N18] [N21]   │
└──────────────────────────────┘
```

<br>

## 🏗️ Arquitectura

### Patrón MVC (Model-View-Controller)

```
  ┌─────────────────────────────────────────────────────────────��
  │                        VISTA (View)                         │
  │                                                             │
  │  MainView ─── GraphCanvasView ─── ControlPanelView         │
  │                    │                                        │
  │  StatsDashboardView ─── TopologyDialogView                  │
  │                                                             │
  └──────────────────────────┬──────────────────────────────────┘
                             │ eventos
                             ▼
  ┌─────────────────────────────────────────────────────────────┐
  │                    CONTROLADOR (Controller)                  │
  │                                                             │
  │  MainController ─── GraphController                         │
  │        │                                                    │
  │  SimulationController ─── ExportController                  │
  │                                                             │
  └──────────────────────────┬──────────────────────────────────┘
                             │ acciones
                             ▼
  ┌─────────────────────────────────────────────────────────────┐
  │                      MODELO (Model)                          │
  │                                                             │
  │  GraphModel ─── SimulationState ─── NodeData                │
  │        │                                                    │
  │  TopologyGenerator ─── NodeStatus (enum)                    │
  │                                                             │
  └──────────────────────────┬──────────────────────────────────┘
                             │
                             ▼
  ┌─────────────────────────────────────────────────────────────┐
  │                    UTILIDADES (Util)                          │
  │                                                             │
  │              GraphStreamIntegration                          │
  │         (Puente JavaFX ↔ GraphStream)                       │
  │                                                             │
  └─────────────────────────────────────────────────────────────┘
```

### Estructura de Archivos

```
SimulacionPropagacion/
│
├── 📄 pom.xml                          # Configuración Maven
├── 📖 README.md                        # Este archivo
├── 📖 GUIA_DE_USUARIO.md              # Guía completa
│
└── src/main/java/
    │
    ├── 🚀 main/
    │   └── Main.java                   # Punto de entrada
    │
    ├── 📦 model/                       # Lógica de negocio
    │   ├── enums/
    │   │   └── NodeStatus.java         # Estados SIR (enum)
    │   ├── NodeData.java               # Datos de cada nodo
    │   ├── GraphModel.java             # Grafo + motor BFS
    │   ├── SimulationState.java        # Estado de simulación
    │   └── TopologyGenerator.java      # Generador de redes
    │
    ├── 🎨 view/                        # Interfaz gráfica
    │   ├── MainView.java               # Layout principal
    │   ├── GraphCanvasView.java        # Canvas interactivo
    │   ├── ControlPanelView.java       # Controles de playback
    │   ├── StatsDashboardView.java     # Dashboard estadísticas
    │   └── TopologyDialogView.java     # Diálogo de topologías
    │
    ├── 🎮 controller/                  # Lógica de control
    │   ├── MainController.java         # Orquestador principal
    │   ├── GraphController.java        # Eventos del canvas
    │   ├── SimulationController.java   # Control de simulación
    │   └── ExportController.java       # Exportación CSV/TXT
    │
    └── 🔧 util/                        # Utilidades
        └── GraphStreamIntegration.java # Puente con GraphStream
```

<br>

## 🛠️ Tecnologías

<table>
<tr>
<td align="center" width="25%">

### ☕ Java 21+
Lenguaje principal con
features modernos:
records, switch expressions,
pattern matching

</td>
<td align="center" width="25%">

### 🎨 JavaFX 21
Framework GUI con
Canvas 2D, controles
personalizados y
bindings reactivos

</td>
<td align="center" width="25%">

### 📊 GraphStream 2.0
Librería de grafos para
layouts automáticos,
métricas de centralidad
y algoritmos

</td>
<td align="center" width="25%">

### 📦 Maven
Build system para
gestión de dependencias,
compilación y ejecución

</td>
</tr>
</table>

<br>

## 🎮 Guía Rápida de Uso

### Paso 1: Crear la Red

```
  Opción A: Manual                    Opción B: Automática
  ────────────────                    ──────────────────────

  1. Clic "⬡ Crear Nodo"             1. Clic "🌐 Topologías"
  2. Clic en el canvas                2. Selecciona un tipo
  3. Clic "⟷ Crear Arista"           3. Ajusta nodos (slider)
  4. Arrastra nodo → nodo             4. Clic "✨ Generar Red"
```

### Paso 2: Configurar

```
  1. Clic "☣ Paciente Cero" → clic en nodo(s) de origen
  2. (Opcional) Clic "⊘ Cuarentena" → aislar nodos
  3. (Opcional) Clic "🔥 Cortafuegos" → bloquear aristas
  4. Ajustar: Tasa de contagio │ Recuperación │ Velocidad
```

### Paso 3: Simular

```
  1. Clic "▶ SIMULAR"
  2. Observar la propagación en tiempo real
  3. Usar controles: ⏮ ⏪ ▶/⏸ ⏩ ⏭
  4. Analizar el dashboard
  5. Exportar: 📁 CSV │ 📄 TXT
```

<br>

## 🧪 Experimentos Sugeridos

### 🔬 Experimento 1: Efecto del Súper Propagador

```
  Red: Estrella (20 nodos)
  P0:  Nodo central (hub) vs Nodo periférico
  
  Observar: ¿Qué tan rápido se propaga
  cuando el hub es el paciente cero
  vs un nodo con solo 1 conexión?
```

### 🔬 Experimento 2: Cuarentena Estratégica

```
  Red: Libre de Escala (30 nodos)
  P0:  Cualquier nodo
  
  Prueba A: Sin cuarentena
  Prueba B: Hub en cuarentena
  Prueba C: 3 nodos aleatorios en cuarentena
  
  Comparar: % de infección final en cada caso
```

### 🔬 Experimento 3: Tasa de Contagio

```
  Red: Malla 5x5 (25 nodos)
  P0:  Nodo central
  
  Prueba A: Contagio 100%
  Prueba B: Contagio 50%
  Prueba C: Contagio 20%
  
  Comparar: Velocidad de propagación y alcance
```

### 🔬 Experimento 4: Cortafuegos

```
  Red: Anillo (15 nodos)
  P0:  Cualquier nodo
  
  Prueba A: Sin cortafuegos
  Prueba B: 1 cortafuegos (divide el anillo)
  
  Observar: ¿El cortafuegos detiene
  completamente la propagación en una dirección?
```

<br>

## 📈 Exportación de Datos

### CSV (para análisis)

```csv
Iteracion,NodoID,Estado,Profundidad,InfectadoPor,Infectados,PorcentajeInfeccion
0,N0,Paciente Cero,0,N/A,1,4.00
1,N1,Infectado,1,N0,5,20.00
1,N3,Infectado,1,N0,5,20.00
2,N0,Infectado,0,N/A,9,36.00
```

### TXT (para reportes)

```
══════════════════════════════════════
  REPORTE DE SIMULACIÓN BFS
══════════════════════════════════════

  Total iteraciones: 15
  Tasa de contagio:  70%
  Pacientes cero:    [N0]

  ── Iteración 1 ──
    N0 → N1 (nivel 1)
    N0 → N3 (nivel 1)
    N0 → N5 [BLOQUEADO]
  [S:22 I:3 R:0 Q:0]
```

<br>

## 🐛 Solución de Problemas

<details>
<summary><b>❌ Error: "Module javafx.controls not found"</b></summary>

Elimina el archivo `module-info.java` si existe:
```
src/main/java/module-info.java  ← ELIMINAR
```
Luego ejecuta con Maven:
```bash
mvn clean javafx:run
```
</details>

<details>
<summary><b>❌ Error: "ClassNotFoundException: main.Main"</b></summary>

Verifica la estructura de directorios:
```
src/main/java/main/Main.java  ← Main.java debe estar aquí
```
Marca `src/main/java` como **Sources Root** en IntelliJ.
</details>

<details>
<summary><b>❌ La simulación no avanza</b></summary>

- ¿Seleccionaste paciente(s) cero? ☣
- ¿El paciente cero tiene aristas (conexiones)?
- ¿La tasa de contagio es mayor que 0%?
- ¿Los vecinos no están todos en cuarentena?
</details>

<details>
<summary><b>❌ Los nodos se ven fuera del canvas</b></summary>

- Redimensiona la ventana
- Genera una nueva topología (se adapta al tamaño)
- Arrastra los nodos de vuelta al área visible
</details>

<br>

## 📚 Fundamento Teórico

### Algoritmo BFS (Breadth-First Search)

```
  BFS usa una COLA (FIFO) para explorar el grafo nivel por nivel:

  Cola: [N0]
  ──────────────────────────────────────────
  Iter 0:  Procesar N0 → infectar vecinos N1, N3, N5
  Cola: [N1, N3, N5]
  ──────────────────────────────────────────
  Iter 1:  Procesar N1 → infectar N2, N4
           Procesar N3 → infectar N6
           Procesar N5 → ya infectado (skip)
  Cola: [N2, N4, N6]
  ──────────────────────────────────────────
  Iter 2:  Procesar N2 → sin vecinos nuevos
           Procesar N4 → infectar N7
           Procesar N6 → infectar N8
  Cola: [N7, N8]
  ──────────────────────────────────────────
  ...continúa hasta que la cola esté vacía
```

### Modelo SIR

```
  dS/dt = -β * S * I / N
  dI/dt = β * S * I / N - γ * I
  dR/dt = γ * I

  Donde:
    β = tasa de contagio (configurable)
    γ = tasa de recuperación (1 / tiempo_recuperación)
    N = población total
```

### Centralidad de Grado

```
  C(v) = grado(v) / (N - 1)

  El nodo con mayor C(v) es el "Súper Propagador".
  Es el nodo más peligroso si se infecta.
```

<br>

## 🤝 Contribuir

¿Encontraste un bug o quieres agregar una funcionalidad?

1. 🍴 Fork el repositorio
2. 🌿 Crea una rama: `git checkout -b feature/nueva-funcionalidad`
3. ✍️ Haz tus cambios y commit: `git commit -m "Agrega X"`
4. 📤 Push: `git push origin feature/nueva-funcionalidad`
5. 🔄 Abre un Pull Request

<br>

## 📚 Documentación

<table>
<tr>
<td align="center" width="50%">

### 📖 Guía de Usuario

Tutorial completo paso a paso con
capturas, diagramas y ejemplos.

**[Abrir Guía →](docs/GUIA_USUARIO.md)**

</td>
<td align="center" width="50%">

## 📄 Licencia

Este proyecto está bajo la Licencia MIT. Ver [LICENSE](LICENSE) para más detalles.

</table>
<br>



<div align="center">

### Programa realizado por JesusShady

**Universidad Nacional Experimental de Guayana (UNEG)**
Semestre IV — Técnicas de Programación 3

<br>

```
  ☣ "En epidemiología, como en la vida,
     las conexiones lo cambian todo." ☣
```

<br>

⭐ **Si te fue útil, deja una estrella en el repositorio** ⭐

</div>
