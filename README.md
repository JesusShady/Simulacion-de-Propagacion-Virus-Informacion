
# ☣ BFS VIRUS PROPAGATION SIMULATOR
## Guía Completa de Usuario

---

## 📋 TABLA DE CONTENIDOS

1. [Introducción](#1-introducción)
2. [Requisitos del Sistema](#2-requisitos-del-sistema)
3. [Instalación y Ejecución](#3-instalación-y-ejecución)
4. [Interfaz de Usuario](#4-interfaz-de-usuario)
5. [Crear una Red de Nodos](#5-crear-una-red-de-nodos)
6. [Topologías Predefinidas](#6-topologías-predefinidas)
7. [Configurar la Simulación](#7-configurar-la-simulación)
8. [Ejecutar la Simulación](#8-ejecutar-la-simulación)
9. [Controles de Reproducción](#9-controles-de-reproducción)
10. [Análisis de Resultados](#10-análisis-de-resultados)
11. [Exportar Datos](#11-exportar-datos)
12. [Atajos y Tips](#12-atajos-y-tips)
13. [Glosario](#13-glosario)
14. [Solución de Problemas](#14-solución-de-problemas)

---

## 1. INTRODUCCIÓN

### ¿Qué es BFS Virus Propagation Simulator?

Es una aplicación de escritorio que simula cómo un virus se propaga
a través de una red de personas (nodos) usando el algoritmo
**BFS (Breadth-First Search)** combinado con el **modelo
epidemiológico SIR** (Susceptible → Infectado → Recuperado).

### ¿Para qué sirve?

- 🎓 **Educación**: Entender cómo funciona BFS visualmente
- 🦠 **Epidemiología**: Simular propagación de enfermedades
- 🔬 **Análisis de redes**: Identificar nodos críticos
- 📊 **Experimentación**: Probar efectos de cuarentena y cortafuegos

### Modelo SIR

```
  SUSCEPTIBLE ──────▶ INFECTADO ──────▶ RECUPERADO
      (S)         contagio    (I)    recuperación   (R)
   Sano, puede      Activamente        Inmune,
   ser infectado     propagando         no puede ser
                     el virus           reinfectado
```

---

## 2. REQUISITOS DEL SISTEMA

| Requisito         | Mínimo                          |
|-------------------|---------------------------------|
| Sistema Operativo | Windows 10/11, macOS, Linux     |
| Java              | JDK 21 o superior               |
| RAM               | 4 GB                            |
| Espacio en disco  | 200 MB                          |
| Resolución        | 1280 x 720 (recomendado 1920x1080) |

---

## 3. INSTALACIÓN Y EJECUCIÓN

### Paso 1: Verificar Java

Abre una terminal y ejecuta:

```bash
java --version
```

Deberías ver `java 21` o superior.

### Paso 2: Abrir el proyecto

Abre la carpeta `SimulacionPropagacion` en IntelliJ IDEA.

### Paso 3: Ejecutar

**Opción A — Desde terminal (recomendada):**

```bash
mvn clean javafx:run
```

**Opción B — Desde IntelliJ:**

1. Abre la barra lateral Maven
2. Ejecuta: `Plugins` → `javafx` → `javafx:run`

---

## 4. INTERFAZ DE USUARIO

La aplicación tiene 4 zonas principales:

```
┌──────────────────────────────────────────────────────────────────────┐
│  ☣ BFS VIRUS PROPAGATION SIMULATOR    ● IDLE     ⬡ Nodos: 0       │
├──────────────┬───────────────────────────────────┬───────────────────┤
│              │                                   │                  │
│  📋 TOOLBAR  │        🖥️ CANVAS CENTRAL          │  📊 DASHBOARD   │
│  (izquierda) │        (centro)                   │  (derecha)       │
│              │                                   │                  │
│  Botones de  │        Área donde se dibujan      │  Estadísticas    │
│  acciones    │        los nodos y aristas         │  en tiempo real  │
│              │                                   │                  │
├──────────────┴───────────────────────────────────┴────────���──────────┤
│  🎮 PANEL DE CONTROL (inferior)                                      │
│  [▶ SIMULAR]  [⏮][⏪][▶][⏩][⏭]   Velocidad   Contagio   [Export] │
└──────────────────────────────────────────────────────────────────────┘
```

### 4.1 Header (Superior)

| Elemento            | Descripción                                     |
|---------------------|-------------------------------------------------|
| ☣ Título            | Nombre de la aplicación                         |
| ● Estado            | IDLE / RUNNING / PAUSED / FINISHED              |
| ⬡ Nodos             | Contador de nodos en la red                     |
| ⟷ Aristas           | Contador de aristas (conexiones)                |

### 4.2 Toolbar (Izquierda)

Los botones se organizan en 4 secciones:

#### CREACIÓN DE RED

| Botón               | Función                                         |
|---------------------|-------------------------------------------------|
| ⬡ Crear Nodo        | Activa modo de creación de nodos                |
| ⟷ Crear Arista      | Activa modo de creación de aristas              |
| 🌐 Topologías       | Abre diálogo de generación automática           |

#### SIMULACIÓN

| Botón               | Función                                         |
|---------------------|-------------------------------------------------|
| ☣ Paciente Cero     | Seleccionar nodos de inicio de infección        |
| ⊘ Cuarentena        | Aislar nodos para bloquear propagación          |
| 🔥 Cortafuegos      | Desactivar aristas específicas                  |

#### ANÁLISIS

| Botón               | Función                                         |
|---------------------|-------------------------------------------------|
| 👑 Súper Propagador | Identificar el nodo más peligroso               |

#### ACCIONES

| Botón               | Función                                         |
|---------------------|-------------------------------------------------|
| 🔄 Reset Simulación | Reiniciar simulación (mantiene el grafo)        |
| 🗑 Limpiar Todo     | Eliminar todos los nodos y aristas              |

### 4.3 Canvas (Centro)

El área principal donde interactúas con el grafo visualmente.

### 4.4 Dashboard (Derecha)

| Sección              | Contenido                                      |
|----------------------|-------------------------------------------------|
| 📊 Estadísticas      | Barras de S, I, R, Q con porcentajes           |
| 📈 Gráfica SIR       | Curvas de evolución en tiempo real              |
| 🔍 Nodo Seleccionado | Información detallada del nodo clickeado        |
| 📋 Cola BFS          | Nodos actualmente en la cola del BFS            |

### 4.5 Panel de Control (Inferior)

| Control              | Función                                         |
|----------------------|-------------------------------------------------|
| ▶ SIMULAR            | Iniciar la simulación BFS                       |
| ⏮ ⏪ ▶/⏸ ⏩ ⏭       | Controles de reproducción                       |
| Slider Velocidad     | Ajustar velocidad de reproducción               |
| Slider Contagio      | Tasa de contagio (0% — 100%)                    |
| Spinner Recuperación | Tiempo de recuperación (en iteraciones)         |
| 📁 CSV / 📄 TXT      | Exportar resultados                             |

---

## 5. CREAR UNA RED DE NODOS

### 5.1 Crear Nodos Manualmente

1. Haz clic en **⬡ Crear Nodo** en la toolbar
2. El botón se ilumina en cyan (modo activo)
3. Haz clic en cualquier lugar del canvas
4. Se crea un nodo gris (Susceptible) en esa posición
5. Repite para crear más nodos
6. Haz clic en **⬡ Crear Nodo** otra vez para desactivar el modo

```
   Antes:                  Después:
   ┌─────────────┐         ┌─────────────┐
   │             │         │  ⬡N0  ⬡N1  │
   │   (vacío)   │  ───▶   │        ⬡N2  │
   │             │         │  ⬡N3        │
   └─────────────┘         └─────────────┘
```

### 5.2 Crear Aristas (Conexiones)

1. Haz clic en **⟷ Crear Arista**
2. Haz clic y **arrastra** desde un nodo hacia otro nodo
3. Una línea cyan punteada aparece durante el arrastre
4. Suelta el mouse sobre el nodo destino
5. La arista se crea automáticamente

```
   Antes:                  Después:
   ⬡N0      ⬡N1           ⬡N0 ──── ⬡N1
                    ───▶
   ⬡N2      ⬡N3           ⬡N2 ──── ⬡N3
```

### 5.3 Mover Nodos

- En modo normal (sin ningún botón activo):
  - Haz clic y **arrastra** un nodo para moverlo
  - El cursor cambia a ✊ mientras arrastras

### 5.4 Seleccionar un Nodo

- Haz clic sobre un nodo para seleccionarlo
- El nodo se resalta con borde cyan
- Su información aparece en el dashboard derecho
- Haz clic en el espacio vacío para deseleccionar

### 5.5 Eliminar Nodos y Aristas

- **Clic derecho** sobre un nodo → lo elimina (y todas sus aristas)
- **Clic derecho** sobre una arista → la elimina

---

## 6. TOPOLOGÍAS PREDEFINIDAS

En vez de crear nodos manualmente, puedes generar redes automáticas.

### Cómo usar

1. Haz clic en **🌐 Topologías**
2. Se abre un diálogo con 6 tipos de red
3. Selecciona una card haciendo clic
4. Ajusta el número de nodos con el slider
5. Haz clic en **✨ Generar Red**

### Tipos disponibles

#### ⭐ Estrella

```
        ⬡
       /
  ⬡── ● ──⬡
       \
        ⬡
```

- Un nodo central conectado a todos los demás
- **Ideal para**: Simular redes con servidor central
- **Súper propagador**: El nodo central
- **Rango**: 5 — 50 nodos

#### ◻️ Malla (Grid)

```
  ⬡──���──⬡
  │  │  │
  ⬡──⬡──⬡
  │  │  │
  ⬡──⬡──⬡
```

- Cuadrícula regular con conexiones horizontales y verticales
- **Ideal para**: Simular propagación geográfica
- **Propagación**: Uniforme, onda expansiva
- **Rango**: 4 — 64 nodos

#### 🌐 Libre de Escala (Barabási-Albert)

```
      ⬡──⬡
     / │
  ⬡── ● ──⬡──⬡
     \ │
      ⬡──⬡──⬡
```

- Pocos nodos muy conectados (hubs), muchos poco conectados
- **Ideal para**: Redes sociales reales, Internet
- **Peligro**: Si el hub se infecta, propagación explosiva
- **Rango**: 8 — 100 nodos

#### 🔄 Anillo

```
  ⬡──⬡──⬡
  │        │
  ⬡        ⬡
  │        │
  ⬡──⬡──⬡
```

- Cada nodo conectado solo a sus 2 vecinos
- **Ideal para**: Propagación lineal, cadenas de contagio
- **Propagación**: Lenta, bidireccional
- **Rango**: 5 — 50 nodos

#### 🌳 Árbol

```
        ⬡
       / \
      ⬡   ⬡
     / \ / \
    ⬡  ⬡ ⬡  ⬡
```

- Estructura jerárquica sin ciclos
- **Ideal para**: Organizaciones, cadenas de mando
- **Propagación**: De arriba hacia abajo
- **Rango**: 7 — 40 nodos

#### 🎲 Aleatoria (Erdős-Rényi)

```
  ⬡──⬡   ⬡
  │ X     │
  ⬡──⬡──⬡
```

- Conexiones aleatorias entre nodos
- **Ideal para**: Redes impredecibles
- **Propagación**: Variable, difícil de predecir
- **Rango**: 6 — 50 nodos

---

## 7. CONFIGURAR LA SIMULACIÓN

Antes de ejecutar la simulación, necesitas configurar 3 cosas:

### 7.1 Seleccionar Pacientes Cero ☣ (OBLIGATORIO)

Los pacientes cero son los nodos donde comienza la infección.

1. Haz clic en **☣ Paciente Cero**
2. Haz clic en los nodos que quieres como origen
3. Los nodos cambian a color **magenta/púrpura** con el ícono ☣
4. Puedes seleccionar **múltiples** pacientes cero
5. Haz clic en un paciente cero de nuevo para quitarle el rol
6. Haz clic en **☣ Paciente Cero** otra vez para desactivar el modo

```
   ⬡ ── ☣ ── ⬡        ☣ = Paciente Cero (púrpura)
   │         │          ⬡ = Susceptible (gris)
   ⬡ ── ⬡ ── ⬡
```

### 7.2 Configurar Cuarentena ⊘ (Opcional)

Los nodos en cuarentena no pueden ser infectados ni propagar.

1. Haz clic en **⊘ Cuarentena**
2. Haz clic en los nodos que quieres aislar
3. Los nodos cambian a color **cyan** con el ícono ⊘
4. Haz clic de nuevo en un nodo para quitarle la cuarentena

```
   ⬡ ── ☣ ── ⬡         ⊘ = Cuarentena (cyan)
   │         │           El virus NO puede pasar por ⊘
   ⊘ ── ⬡ ── ⬡
```

### 7.3 Configurar Cortafuegos 🔥 (Opcional)

Los cortafuegos desactivan aristas específicas.

1. Haz clic en **🔥 Cortafuegos**
2. Haz clic **sobre una arista** (línea entre nodos)
3. La arista se vuelve roja punteada con ícono 🔥
4. El virus NO puede viajar por esa arista
5. Haz clic de nuevo para reactivarla

```
   ⬡ ── ☣ ──🔥── ⬡      🔥 = Cortafuegos
   │              │       El virus NO puede cruzar
   ⬡ ──── ⬡ ──── ⬡       esa arista
```

### 7.4 Ajustar Parámetros (Panel inferior)

| Parámetro           | Slider/Spinner | Descripción                     | Default |
|---------------------|----------------|---------------------------------|---------|
| **Tasa de Contagio**| Slider 0-100%  | Probabilidad de infectar        | 100%    |
| **Recuperación**    | Spinner 1-20   | Iteraciones para recuperarse    | 5       |
| **Velocidad**       | Slider         | Velocidad de reproducción       | 0.8s    |

#### Ejemplos de Tasa de Contagio:

- **100%**: Todo vecino susceptible SE infecta (BFS puro)
- **70%**: 70% de probabilidad de infectar a cada vecino
- **30%**: Solo 30% de probabilidad → propagación muy lenta
- **0%**: El virus no se propaga (inútil)

#### Ejemplos de Tiempo de Recuperación:

- **1 iteración**: Se recuperan casi inmediatamente
- **5 iteraciones**: Tiempo moderado (default)
- **15 iteraciones**: Infectados por mucho tiempo → más propagación

---

## 8. EJECUTAR LA SIMULACIÓN

### Paso a paso

1. ✅ Crea o genera una red de nodos
2. ✅ Selecciona al menos un paciente cero
3. ✅ (Opcional) Configura cuarentenas y cortafuegos
4. ✅ Ajusta tasa de contagio y tiempo de recuperación
5. Haz clic en **▶ SIMULAR**

### ¿Qué sucede al simular?

1. **Pre-computación**: El motor BFS calcula TODA la simulación
   de una sola vez (esto es instantáneo)
2. **Reproducción**: La simulación se reproduce como un video
3. **Visualización**: Los nodos cambian de color en tiempo real

### Colores durante la simulación

| Color              | Estado          | Significado                   |
|--------------------|-----------------|-------------------------------|
| ⬡ Gris azulado    | Susceptible     | Sano, puede ser infectado     |
| ☣ Púrpura         | Paciente Cero   | Origen de la infección        |
| ⬢ Rojo brillante  | Infectado       | Propagando activamente        |
| ✓ Verde neón      | Recuperado      | Inmune, ya no se infecta      |
| ⊘ Cyan            | Cuarentena      | Aislado, no participa         |

### Efectos visuales durante la simulación

- **Glow pulsante**: Los nodos infectados emiten un resplandor rojo
- **Partículas viajeras**: Puntos blancos viajan por las aristas activas
- **Mapa de calor**: Números en los nodos muestran profundidad BFS
  - 0 = origen (rojo oscuro)
  - 1 = primer nivel (rojo)
  - 2 = segundo nivel (naranja)
  - 3+ = niveles más profundos (amarillo → verde → cyan)
- **Corona 👑**: El nodo súper propagador tiene una corona dorada

---

## 9. CONTROLES DE REPRODUCCIÓN

Una vez ejecutada la simulación, puedes controlarla como un video:

### Botones de transporte

| Botón | Acción                    | Atajo         |
|-------|---------------------------|---------------|
| ⏮    | Ir al inicio (iteración 0)| —             |
| ⏪    | Un paso atrás             | —             |
| ▶/⏸  | Play / Pausa              | —             |
| ⏩    | Un paso adelante          | —             |
| ⏭    | Ir al final               | —             |

### Slider de Timeline

- Arrastra el slider para saltar a cualquier iteración
- Muestra: `Iteración: 3 / 15`

### Velocidad de reproducción

- Mueve el slider de velocidad:
  - **Izquierda**: Más rápido (0.1s entre pasos)
  - **Derecha**: Más lento (3.0s entre pasos)
  - **Default**: 0.8s

### Re-simular

- Después de una simulación, el botón cambia a **🔄 RE-SIMULAR**
- Puedes cambiar parámetros y volver a ejecutar
- La red se mantiene, solo se resetean los estados

---

## 10. ANÁLISIS DE RESULTADOS

### 10.1 Dashboard en Tiempo Real

Durante la simulación, el dashboard muestra:

#### Barras de progreso SIR

```
  S  Susceptible    ████████████░░░░  78%
  I  Infectado      ██████░░░░░░░░░░  15%
  R  Recuperado     ██░░░░░░░░░░░░░░   5%
  Q  Cuarentena     █░░░░░░░░░░░░░░░   2%
```

Las barras se actualizan en cada iteración con colores neón.

#### Gráfica SIR

```
  100%│╲ S
      │  ╲     ╱── R
   50%│   ╲   ╱
      │    ╲╱ I
    0%│────────────── Q
      └──────────────→
         Iteraciones
```

Muestra la evolución de las 4 curvas a lo largo del tiempo.

#### Info del Nodo Seleccionado

Haz clic en cualquier nodo durante la simulación para ver:

```
  ID:             N7
  Label:          SF7
  Estado:         ⬢ Infectado
  Profundidad:    Nivel 2
  Infectado por:  N3
  Conexiones:     5
  Propagó a:      3 nodos
  Duración:       2 iteraciones
```

#### Cola BFS

Muestra los nodos actualmente en la cola del BFS:

```
  En cola: 6
  [N12] [N15] [N18] [N21] [N24] [N27]
```

### 10.2 Súper Propagador 👑

1. Haz clic en **👑 Súper Propagador**
2. El sistema identifica el nodo con más conexiones
3. Aparece una corona dorada sobre el nodo
4. Se muestra un diálogo con información:

```
  👑 Súper Propagador Identificado

  Nodo:        N0 (Hub)
  Conexiones:  12
  Grado de centralidad: Máximo

  Este nodo es el más peligroso de la red.
  Si se infecta, puede propagar el virus
  al mayor número de vecinos directos.
```

### 10.3 Mapa de Calor por Profundidad

Los números en las esquinas de los nodos muestran a qué
"nivel" del BFS fueron alcanzados:

```
  Profundidad 0: ██ Rojo oscuro  (paciente cero)
  Profundidad 1: ██ Rojo         (primer contacto)
  Profundidad 2: ██ Naranja      (segundo contacto)
  Profundidad 3: ██ Amarillo     (tercer contacto)
  Profundidad 4: ██ Verde        (cuarto contacto)
  Profundidad 5+: ██ Cyan        (contactos lejanos)
```

---

## 11. EXPORTAR DATOS

### 11.1 Exportar a CSV 📁

1. Haz clic en **📁 CSV** en el panel inferior
2. Elige ubicación y nombre del archivo
3. Se genera un CSV con todos los datos

**Formato del CSV:**

```csv
Iteracion,NodoID,Label,Estado,Profundidad,InfectadoPor,Susceptibles,Infectados,Recuperados,Cuarentena,PorcentajeInfeccion
0,N0,Hub,Paciente Cero,0,N/A,24,1,0,0,4.00
0,N1,P1,Susceptible,-1,N/A,24,1,0,0,4.00
1,N1,P1,Infectado,1,N0,20,5,0,0,20.00
```

**Útil para**: Abrir en Excel, Google Sheets, o analizar con Python/R.

### 11.2 Exportar a TXT 📄

1. Haz clic en **📄 TXT** en el panel inferior
2. Elige ubicación y nombre del archivo
3. Se genera un reporte legible

**Formato del TXT:**

```
═══════════════════════════════════════════
  REPORTE DE SIMULACIÓN BFS
═══════════════════════════════════════════

  Inicio:            2026-02-19 14:30:00
  Total iteraciones: 15
  Tasa de contagio:  70%
  Recuperación:      5 iteraciones
  Pacientes cero:    [N0]

──── ITERACIÓN 0 ────────────────────────
  Eventos:
    [ORIGEN] → N0 (nivel 0)
  [S:24 I:1 R:0 Q:0]

──── ITERACIÓN 1 ────────────────────────
  Eventos:
    N0 → N1 (nivel 1)
    N0 → N3 (nivel 1)
    N0 → N5 [BLOQUEADO]
  [S:22 I:3 R:0 Q:0]
```

### Nombre del archivo generado

```
BFS_Simulation_2026-02-19_14-30-00_N25_C70.csv
              │                │     │    │
              Fecha           Hora  Nodos Contagio%
```

---

## 12. ATAJOS Y TIPS

### Interacción con el Mouse

| Acción                    | Resultado                          |
|---------------------------|------------------------------------|
| Clic izquierdo en nodo    | Seleccionar nodo                   |
| Clic izquierdo en vacío   | Deseleccionar / Crear nodo (en modo)|
| Clic derecho en nodo      | **Eliminar** nodo y sus aristas    |
| Clic derecho en arista    | **Eliminar** arista                |
| Arrastrar nodo            | Mover nodo                         |
| Arrastrar nodo a nodo     | Crear arista (en modo arista)      |
| ESC en diálogo            | Cerrar/Cancelar                    |

### Tips Pro 🎯

1. **Empieza con topologías**: Es más rápido que crear nodos uno por uno

2. **Prueba con Estrella primero**: Es la más fácil de entender.
   El nodo central es el súper propagador

3. **Experimenta con contagio**: Prueba la misma red con 100%, 50% y 20%
   para ver cómo cambia la propagación

4. **Usa cuarentena estratégica**: Poner en cuarentena al
   súper propagador reduce drásticamente la infección

5. **Cortafuegos en puentes**: Desactiva aristas que conectan
   clusters para aislar secciones de la red

6. **Paso a paso (⏩)**: Usa el botón de paso adelante para
   entender exactamente qué pasa en cada iteración

7. **Exporta para tu informe**: El CSV es perfecto para gráficas
   en Excel, el TXT para documentación

8. **Múltiples pacientes cero**: Prueba con 1 vs 3 pacientes cero
   para ver la diferencia en velocidad de propagación

---

## 13. GLOSARIO

| Término              | Definición                                         |
|----------------------|----------------------------------------------------|
| **BFS**              | Breadth-First Search. Algoritmo que explora nodos nivel por nivel |
| **Nodo**             | Representa una persona/entidad en la red           |
| **Arista**           | Conexión entre dos nodos (contacto entre personas) |
| **Grafo**            | Estructura de nodos y aristas                      |
| **SIR**              | Modelo epidemiológico: Susceptible-Infectado-Recuperado |
| **Susceptible**      | Nodo sano que puede ser infectado                  |
| **Infectado**        | Nodo que propaga activamente el virus              |
| **Recuperado**       | Nodo inmune tras superar la infección              |
| **Cuarentena**       | Nodo aislado que no participa en la propagación    |
| **Paciente Cero**    | Nodo donde comienza la infección                   |
| **Súper Propagador** | Nodo con más conexiones (mayor centralidad)        |
| **Cortafuegos**      | Arista desactivada que bloquea la propagación      |
| **Topología**        | Forma/estructura de la red                         |
| **Iteración**        | Un paso/ciclo de la simulación BFS                 |
| **Profundidad BFS**  | Distancia (en saltos) desde el paciente cero       |
| **Tasa de contagio** | Probabilidad de infectar a un vecino (0%-100%)     |
| **Tiempo de recuperación** | Iteraciones necesarias para recuperarse       |
| **Cola BFS**         | Lista de nodos pendientes de procesar              |
| **Centralidad**      | Medida de importancia de un nodo en la red         |
| **Componente conexa**| Grupo de nodos conectados entre sí                 |

---

## 14. SOLUCIÓN DE PROBLEMAS

### "No se puede simular"

| Problema                        | Solución                                  |
|---------------------------------|-------------------------------------------|
| "Red vacía"                     | Crea nodos o genera una topología         |
| "Sin paciente cero"             | Selecciona al menos un nodo como P0       |
| "Tasa de contagio 0%"           | Sube el slider de contagio                |
| "Demasiados en cuarentena"      | Quita cuarentena de algunos nodos         |

### "El programa no inicia"

| Problema                        | Solución                                  |
|---------------------------------|-------------------------------------------|
| `ClassNotFoundException`        | Verifica estructura: `src/main/java/`     |
| `Module not found`              | Elimina `module-info.java`                |
| `JavaFX not found`              | Ejecuta con `mvn clean javafx:run`        |

### "La simulación no avanza"

- Verifica que el paciente cero tenga conexiones (aristas)
- Verifica que los vecinos no estén todos en cuarentena
- Verifica que no haya cortafuegos en todas las aristas

### "Los nodos no se ven"

- Redimensiona la ventana
- Genera una nueva topología (se adapta al tamaño del canvas)

---

## CRÉDITOS

```
  ╔══════════════════════════════════════════════╗
  ║   BFS VIRUS PROPAGATION SIMULATOR v1.0      ║
  ║                                              ║
  ║   Desarrollado con ❤️ por JesusShady         ║
  ║                                              ║
  ║   Tecnologías:                               ║
  ║   • Java 21+                                 ║
  ║   • JavaFX 21 (GUI)                          ║
  ║   • GraphStream 2.0 (Algoritmos de grafos)   ║
  ║   • Maven (Build system)                     ║
  ║                                              ║
  ║   Arquitectura: MVC (Model-View-Controller)  ║
  ║   Tema visual: Dark Cyberpunk Neón           ║
  ║                                              ║
  ║   Universidad Nacional Experimental          ║
  ║   de Guayana (UNEG)                          ║
  ║   Semestre IV — Técnicas de Programación 3   ║
  ╚══════════════════════════════════════════════╝
```

---

*Guía de Usuario v1.0 — Febrero 2026*
