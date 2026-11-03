# Proyecto #1 — Análisis Léxico y Sintáctico

**Curso:** Compiladores e Intérpretes GR 60  
**Profesor:** Allan Rodríguez Dávila  
**Institución:** Instituto Tecnológico de Costa Rica  
**Semestre:** II Semestre, 2026  

**Integrantes:**
- Angelo Piedra Castro — 2024101262
- Elian Trejos Quirós — 2024143262

---

## Descripción

Este proyecto implementa las dos primeras fases de un compilador para un lenguaje imperativo ligero orientado a la configuración de chips:

1. **Análisis léxico** con JFlex.
2. **Análisis sintáctico** con CUP.

El programa lee un archivo fuente en UTF-8, reconoce los tokens, valida la sintaxis, reporta los errores encontrados (con línea y columna) y emite un veredicto final.

---

## Requisitos

| Herramienta | Versión sugerida | Notas |
|---|---|---|
| JDK | 17 o superior | Verificar con `java -version` y `javac -version` |
| JFlex | 1.9.x (`jflex-full-1.9.1.jar`) | Incluido en `lib/` |
| CUP | 11b (`java-cup-11b.jar` y `java-cup-11b-runtime.jar`) | Incluidos en `lib/` |
| Sistema operativo | Windows | Los scripts están probados en Windows |

Los jars de JFlex y CUP están incluidos en `lib/`, por lo que no es necesario descargarlos.

---

## Estructura del proyecto
```
Proyecto #1
│
├── info.txt
│
├── documentacion/
│   ├── Documentacion_Proyecto1.docx
│   └── bitacora.txt
│
└── programa/
    │
    ├── lib/
    │   ├── jflex-full-1.9.1.jar
    │   ├── java-cup-11b.jar
    │   └── java-cup-11b-runtime.jar
    │
    ├── spec/
    │   ├── Lexer.flex
    │   └── Parser.cup
    │
    ├── src/
    │   └── compilador/
    │       ├── generated/
    │       │   ├── Lexer.java
    │       │   ├── Parser.java
    │       │   └── sym.java
    │       ├── ErrorReporter.java
    │       ├── Main.java
    │       ├── ReportingLexer.java
    │       ├── TokenReporter.java
    │       └── PruebaLexer.java
    │
    ├── tests/
    │   ├── validos/
    │   ├── errores_lexicos/
    │   ├── errores_sintacticos/
    │   └── mixtos/
    │
    ├── salida/            (se genera al ejecutar)
    ├── build.bat
    └── run.bat
```
---
## Compilación

Abre PowerShell en la raíz del proyecto y ejecuta:

```powershell
cd .\programa\
.\build.bat
```
El script realiza tres pasos:

1. Genera el parser con CUP (produce Parser.java y sym.java).

2. Genera el scanner con JFlex (produce Lexer.java).

3. Compila todo el código Java.

Si la compilación es exitosa, verás el mensaje ```Compilacion exitosa```.

## Ejecución

Desde la carpeta `programa/`, ejecuta:

```powershell
.\run.bat <nombre>.txt
```

Por ejemplo:

```powershell
.\run.bat tests\programa.txt
```

### Salidas generadas

Para cada archivo analizado `<nombre>.txt` se generan los siguientes resultados.

**1. `programa/salida/<nombre>_tokens.txt`:** tabla de tokens reconocidos con las siguientes columnas:

| Columna | Descripción |
|---|---|
| Línea | Línea donde aparece el token |
| Columna | Columna donde aparece el token |
| Token | Nombre del terminal |
| Lexema | Texto reconocido |
| Tabla | Tabla de símbolos a la que pertenece |
| Info | Información almacenada |

**2. `programa/salida/<nombre>_errores.txt`:** lista de errores léxicos y sintácticos, ordenados por línea y columna.

**3. Consola:** se imprimen los errores encontrados y un veredicto final:

- Con errores: `El archivo no puede ser generado por la gramática.` (revisar `salida/<nombre>_errores.txt`).
- Sin errores: `El archivo se pudo generar con éxito.` (archivo de tokens en `salida/<nombre>_tokens.txt`).

---

## Pruebas

Los casos de prueba se encuentran en `programa/tests/`:

| Carpeta | Contenido |
|---|---|
| `validos/` | Programas que la gramática debe aceptar |
| `errores_lexicos/` | Programas con caracteres no reconocidos, strings o comentarios sin cerrar |
| `errores_sintacticos/` | Programas con errores de sintaxis |
| `mixtos/` | Programas con errores léxicos y sintácticos combinados |

Ejemplo de ejecución sobre un caso válido:

```powershell
.\run.bat tests\validos\<archivo>.txt
```

---

## Diseño del programa

### Scanner (JFlex) — `spec/Lexer.flex`

- Directivas: `%class Lexer`, `%public`, `%unicode`, `%cup`, `%line`, `%column`, `%state COMENTARIO`.
- Método auxiliar `symbol(int type)` y macros agrupadas por categoría.

Decisiones de diseño:

- **Orden de las reglas:** los patrones de 2 o más caracteres van antes que los de 1 (`++` antes de `+`, `//` antes de `/`, `<=` antes de `<`).
- **Palabras reservadas antes que identificadores**, para que `while` no se reconozca como `id`.
- **Identificadores con `[a-zA-Z_]`:** clases explícitas para evitar que caracteres como `λ`, `θ`, `Σ` se reconozcan como identificadores.
- **Comentarios de bloque con estado:** `%state COMENTARIO` consume el contenido hasta el cierre. Si se llega a EOF con el comentario abierto, se reporta un error léxico con la línea de apertura.
- **Recuperación de errores léxicos:** la regla comodín `[^]` reporta el carácter no reconocido (con línea y columna) y continúa. Un string sin cerrar se reporta como error léxico porque no coincide con `litStr`.
- **Literales:** enteros, flotantes, char, string y booleanos. El signo negativo no forma parte del literal; se resuelve en el parser como operador unario.

### Parser (CUP) — `spec/Parser.cup`

- Bloque `parser code {: :}` con `syntax_error` y `unrecovered_syntax_error`.
- Declaración de terminales, no terminales y producciones.

Decisiones de diseño:

- **Precedencia en la estructura, no con `precedence`:** como se prohíbe la producción genérica `expr ::= expr op expr`, se usa un no terminal por nivel: `expr_modulo → expr_multiplicativa → expr_aditiva → expr_potencia → expr_negativo → expr_primaria`.
- **Separación de `tipo_arrays` y `tipo_no_arreglo`:** elimina un conflicto reduce/reduce (con un solo `tipo_var`, CUP no podía decidir si `val int x` era variable simple o arreglo con un token de anticipación).
- **Recuperación en modo pánico:** `lista_sentencias` incluye `error FIN_SENTENCIA lista_sentencias` y `bloque` incluye `OPEN_LLAVES error CLOSE_LLAVES`, de modo que el parser continúa y reporta los errores de las líneas siguientes.

### Clases Java (`src/compilador/`)

| Clase | Responsabilidad |
|---|---|
| `Main` | Valida la ruta, lee el archivo en UTF-8, crea `Lexer`, `ReportingLexer` y `Parser`, ejecuta `parser.parse()` dentro de `try/catch`, guarda los errores y emite el veredicto. |
| `ErrorReporter` | Acumula errores léxicos, sintácticos y semánticos; los ordena por línea y columna; los imprime en consola y los guarda en archivo UTF-8; devuelve el total. |
| `TokenReporter` | Escribe el archivo de tokens (6 columnas) y clasifica cada token en una tabla de símbolos: *Reservadas*, *Identificadores*, *Constantes* (literales numéricos, char, string y booleanos) o *Ninguna* (operadores, delimitadores, EOF). |
| `ReportingLexer` | Envuelve el lexer generado por JFlex y registra cada token en el `TokenReporter` antes de entregarlo al parser. |
| `PruebaLexer` | Utilidad para probar el analizador léxico de forma aislada. |

### Librerías usadas

| Librería | Descripción |
|---|---|
| JFlex 1.9.x | Generación del analizador léxico |
| `java-cup-11b.jar` | Generador del analizador sintáctico |
| `java-cup-11b-runtime.jar` | Runtime necesario en ejecución |
| `java.io` | Lectura y escritura de archivos |
| `java.nio` | Manejo de rutas y codificación |
| `java.nio.charset.StandardCharsets` | UTF-8 explícito |
| `java.util` | Listas y comparadores para ordenar errores |

---

## Análisis de resultados

### Objetivos alcanzados

- Reconocimiento de todos los tokens del lenguaje.
- Manejo de comentarios de línea y de bloque.
- Recuperación de errores léxicos (carácter no reconocido, string sin cerrar, comentario de bloque sin cerrar).
- Reporte de errores con línea y columna.
- Gramática LALR(1) sin conflictos.
- Precedencia y asociatividad correctas.
- Forma rígida de expresiones relacionales y lógicas.
- Estructuras de control (`if`, `else`, `elif`, `while`, `for`).
- Arreglos con creación, acceso y modificación.
- Recuperación en modo pánico.
- Archivo de tokens con 6 columnas y tabla de símbolos (reservadas, identificadores, constantes).
- Archivo de errores ordenado por línea y columna.

### Objetivos no alcanzados

- La recuperación a nivel de frase no se implementó.

---

## Precedencia y asociatividad

La precedencia, de mayor a menor, se refleja en la estructura de la gramática:

```
expr_modulo → expr_multiplicativa → expr_aditiva → expr_potencia
```

Cada nivel es un no terminal separado, cumpliendo la prohibición de la producción genérica.

- **`expr_potencia`:** asociatividad derecha.
- **`expr_modulo`, `expr_multiplicativa`, `expr_aditiva`:** asociatividad izquierda.

---

## Diferencias con la gramática de la Tarea I

- **Separación de `tipo_var`:** la Tarea I definía `tipo_var ::= int | float | string | bool | char`. Se dividió en `tipo_arrays` (`int | float`) y `tipo_no_arreglo` (`bool | string | char`) para eliminar el conflicto reduce/reduce entre variable simple y arreglo, sin perder generalidad.
- **Unificación de `operando_rel_orden` y `operando_rel_igualdad`:** ambos eran `expr_modulo` y generaban conflicto reduce/reduce; se unificaron en `operando_rel`.
- **Expansión de `*` y `?`:** las producciones de la Tarea I con `*` y `?` se expandieron a producciones recursivas por la derecha y alternativas, siguiendo el estilo de CUP.
- **Restricción de `operando_logico`:** la Tarea I definía `operando_logico ::= expr_relacional | expr_negacion | lit_bool | id | llamada_funcion`, lo que generaba conflicto reduce/reduce con `expr_primaria`, porque los paréntesis del lenguaje se usan tanto para agrupar expresiones aritméticas como para envolver operandos lógicos. Como el enunciado indica que los operandos son booleanos y la gramática solo los produce mediante relacionales y negaciones, se restringió a `operando_logico ::= expr_relacional | expr_negacion`.

---

## Recuperación en modo pánico

Se implementó con:

- `syntax_error(Symbol)`: reporta el error con línea y columna.
- `unrecovered_syntax_error(Symbol)`: reporta errores no recuperables.
- Producciones `error` en `lista_sentencias` y `bloque`, que sincronizan con `FIN_SENTENCIA` y `CLOSE_LLAVES`.

---

## Bitácora

La bitácora completa está en `documentacion/bitacora.txt` y en el historial de commits del repositorio de GitHub.