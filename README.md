# Proyecto #2 — Análisis Semántico y Generación de Código Intermedio

**Curso:** Compiladores e Intérpretes GR 60  
**Profesor:** Allan Rodríguez Dávila  
**Institución:** Instituto Tecnológico de Costa Rica  
**Semestre:** II Semestre, 2026  

**Integrantes:**
- Angelo Piedra Castro — 2024101262
- Elian Trejos Quirós — 2024143262

La documentación completa (manual, pruebas con capturas, diseño, análisis de resultados, justificación de la validación semántica y bitácora) está en `documentacion/Documentacion_Proyecto_2.docx`.

---

## Descripción

Un grupo de desarrolladores quiere crear un lenguaje imperativo ligero para configurar chips de sistemas empotrados. En el Proyecto 1 se construyeron el analizador léxico (JFlex) y el analizador sintáctico (CUP). El Proyecto 2 agrega las dos fases siguientes sobre esa misma gramática:

- **Análisis semántico:** comprueba que el programa tenga sentido además de estar bien escrito. El lenguaje tiene tipado explícito y fuerte, así que se validan los tipos de cada operación, asignación, llamada y retorno, que los nombres estén declarados antes de usarse, que no se repitan en el mismo alcance y que `return` y `break` estén en lugares válidos.
- **Generación de código intermedio:** traduce el programa a código de tres direcciones con el mismo significado y lo escribe en un archivo.

Además, se conservan y corrigen los alcances del Proyecto 1, se indica si el archivo puede ser generado considerando gramática, sintaxis y semántica, y los errores léxicos, sintácticos y semánticos se manejan con recuperación en modo pánico (se reporta el error con su línea y el análisis continúa).

---

## Requisitos

| Herramienta | Versión | Notas |
|---|---|---|
| JDK | 17 o superior | `java -version` y `javac -version` deben mostrar la misma versión |
| JFlex | 1.9.1 (`jflex-full-1.9.1.jar`) | Incluido en `programa/lib/` |
| CUP | 11b (`java-cup-11b.jar`, `java-cup-11b-runtime.jar`) | Incluidos en `programa/lib/` |
| Sistema operativo | Windows | `build.bat` y `run.bat` son scripts de Windows |

No hace falta descargar ninguna librería. Los archivos fuente deben estar guardados en UTF-8, porque el lenguaje usa símbolos Unicode (`Ͱ`, `»`, `є:`, `λ`, etc.).

---

## Estructura del proyecto

```
info.txt
documentacion/
└── Documentacion_Proyecto_2.docx
programa/
├── lib/            jflex-full-1.9.1.jar, java-cup-11b.jar, java-cup-11b-runtime.jar
├── spec/           Lexer.flex, Parser.cup
├── src/compilador/
│   ├── Main.java, ErrorReporter.java, TokenReporter.java, ReportingLexer.java
│   ├── atributos/  Expr, Codigo, Bloque, Rama
│   ├── simbolos/   Simbolo, TablaSimbolos, GestorTablas, ReporteSimbolos
│   ├── semantico/  Semantico
│   ├── tac/        GeneradorTAC
│   └── generated/  Lexer, Parser, sym  (los genera build.bat)
├── tests/          archivos de prueba (.txt)
├── salida/         reportes generados al ejecutar
├── build.bat
└── run.bat
```

---

## Compilación

Desde la raíz del proyecto:

```powershell
cd .\programa\
.\build.bat
```

El script hace tres pasos:

1. Genera `Parser.java` y `sym.java` con CUP.
2. Genera `Lexer.java` con JFlex.
3. Compila todo el código Java en la carpeta `bin`.

Si todo sale bien termina con `Compilacion exitosa`. CUP reporta 150 producciones, 260 estados y 0 conflictos.

## Ejecución

Desde la carpeta `programa`:

```powershell
.\run.bat tests\programa_prueba.txt
```

El programa analiza el archivo, muestra los errores en la consola y escribe los reportes en `programa/salida/`. Termina con código de salida 0 si el archivo no tiene errores y 1 si tiene errores.

### Salidas generadas

Para un archivo `<nombre>.txt` se generan en `programa/salida/`:

| Archivo | Contenido |
|---|---|
| `<nombre>_tokens.txt` | Todos los tokens: línea, columna, token, lexema, tabla de símbolos a la que pertenece e información almacenada |
| `<nombre>_simbolos.txt` | Una tabla de símbolos por alcance (global, cada función, principal, if, elif, else, while, for), más la tabla de constantes y la de palabras reservadas |
| `<nombre>_errores.txt` | Los errores léxicos, sintácticos y semánticos, ordenados por línea y columna |
| `<nombre>_tac.txt` | El código intermedio de tres direcciones. Solo se genera si el archivo no tiene errores |

En la consola se imprime el veredicto:

- Sin errores: `El archivo se pudo ser generado con éxito`, con la ruta de los tokens, las tablas y el código intermedio.
- Con errores: `El archivo no puede ser generado por la gramática` y el total de errores.

Cada error indica su tipo, línea y columna, por ejemplo:

```
ERROR SEMÁNTICO en la línea 2, columna 14: La variable 'no_declarada' no está declarada
```

---

## Uso del lenguaje

| Elemento | Símbolo | Elemento | Símbolo |
|---|---|---|---|
| Bloque | `¿: … :?` | Asignación | `Ͱ` |
| Paréntesis | `є: … :э` | Fin de sentencia | `»` |
| Índice / operando relacional | `ʃ: … :ʅ` | Y / O / Negación | `λ` / `θ` / `Σ` |
| Comentario de línea | `\|` | Comentario de bloque | `¡ … !` |
| Declaración | `val <tipo> <id>` | Potencia / módulo | `pot` / `mod` |

Ejemplo de programa válido:

```
val float notas ʃ: 3 :ʅ Ͱ ¿: 1.5, 2.0, 3.5 :? »

int suma є: int a, int b :э ¿:
    return a + b »
:?

void principal є: :э ¿:
    val int x Ͱ 0 »
    val int y Ͱ 0 »
    read є: x :э »
    y Ͱ suma є: x + 1, x * 2 :э »
    notas ʃ: 0 :ʅ Ͱ 4.5 »
    write є: y :э »
    write є: notas ʃ: 1 :ʅ :э »
:?
```

---

## Pruebas

Los casos están en `programa/tests/`:

| Archivo | Qué prueba | Resultado esperado |
|---|---|---|
| `programa_prueba.txt` | Programa completo (sistema de notas) con globales, arreglos, funciones, if/elif/else, while, read y write | Sin errores; genera tokens, tablas y `_tac.txt` |
| `error_lexico.txt` | Caracteres que no pertenecen al lenguaje (`@`, `#`, `$`, `%`) | 4 errores léxicos (líneas 2 a 5); el análisis continúa después de cada uno |
| `error_sintactico.txt` | Una sentencia sin `»` al final | 1 error sintáctico en la línea 4; el parser se recupera y sigue |
| `error_semantico.txt` | Uso de una variable que no fue declarada | 1 error semántico en la línea 2 |

Cuando hay errores no se genera `_tac.txt`; solo `_tokens.txt`, `_simbolos.txt` y `_errores.txt`. Las capturas de cada ejecución están en la sección 2 de la documentación.

---

## Diseño del programa

### Arquitectura

El analizador semántico y el generador de código no recorren un árbol aparte: los llaman las acciones de las producciones de `Parser.cup` (traducción dirigida por la sintaxis).

```
archivo .txt (UTF-8)
   │
   ▼
Lexer (JFlex) ──► ReportingLexer ──► Parser (CUP)
                  registra tokens     │  en cada producción:
                                      │    1. GestorTablas  (tablas de símbolos)
                                      │    2. Semantico     (valida y calcula el tipo)
                                      │    3. GeneradorTAC  (arma el código)
                                      ▼
             salida/: _tokens.txt, _simbolos.txt, _errores.txt, _tac.txt
```

### Análisis en dos pasadas

`Main` analiza el archivo dos veces con el mismo lexer y parser:

- **Pasada 1:** solo arma el árbol completo de tablas de símbolos. No valida nada y sus errores se descartan, porque se vuelven a encontrar en la pasada 2.
- **Pasada 2:** análisis completo. `Semantico.iniciar(tablaGlobal)` recibe el árbol de la pasada 1 y con él se validan todos los usos. Al final, `Semantico.validarDeclaraciones()` hace las revisiones que necesitan el árbol entero (redeclaraciones, funciones sin return, funciones que retornan string).

CUP es un parser ascendente (bottom-up): reduce primero lo más interno, así que cuando procesa una sentencia todavía no cerró el bloque, la función ni el ciclo que la contienen. Con una sola pasada no se podría saber si existe una función llamada en `principal` que está declarada más abajo, a qué función pertenece un `return` ni si un `break` está dentro de un ciclo. Con el árbol de la pasada 1, cada uso se resuelve por su posición: `tablaMasInterna(posición)` encuentra el alcance del uso y `buscarAntesDe(nombre, posición)` busca la declaración visible desde ahí hacia afuera. Las posiciones se guardan como `línea * 100000 + columna` para compararlas fácilmente.

### Scanner (JFlex) — `spec/Lexer.flex`

- Directivas: `%class Lexer`, `%public`, `%unicode`, `%cup`, `%line`, `%column`, `%xstate COMENTARIO`.
- **Comentarios de bloque con `%xstate`:** se usa un estado **exclusivo**. Mientras se está dentro de `¡ … !` solo aplican las reglas del estado `COMENTARIO`, así que ningún texto del comentario se reconoce como token. Con `%state` (inclusivo) también seguirían activas las reglas generales, y una palabra como `while` dentro del comentario podría reconocerse. Si el archivo termina con el comentario abierto, se reporta un error léxico con la línea donde empezó.
- Palabras reservadas antes que los identificadores, y patrones de varios caracteres antes que los de uno (`++` antes de `+`, `//` antes de `/`), porque JFlex resuelve los empates por el orden de las reglas.
- Identificadores con clases explícitas `[_a-zA-Z]`, para que `λ`, `θ` y `Σ` no se reconozcan como identificadores.
- La regla comodín `[^]` reporta cualquier carácter no reconocido con su línea y columna, y el análisis continúa.
- El signo negativo no forma parte del literal: lo maneja el parser como operador unario.

### Parser (CUP) — `spec/Parser.cup`

150 producciones, 260 estados y 0 conflictos. Cada producción con significado semántico llama, en este orden:

1. a `GestorTablas` si declara algo o cierra un alcance;
2. a `Semantico`, que valida y devuelve el tipo del resultado;
3. a `GeneradorTAC`, que recibe ese tipo y arma el código.

Todas las acciones van al final de las producciones. No se usan acciones a mitad de producción, porque en esta gramática generan conflictos (por ejemplo, entre `bloque` y su alternativa de error). Los no terminales tienen tipo: las expresiones devuelven `Expr` (tipo, temporal y código), las sentencias devuelven `Codigo` y los bloques devuelven `Bloque` (su tabla y su código).

La precedencia, de mayor a menor (`pot` > `+ -` > `* / //` > `mod`), se refleja en la estructura de la gramática con un no terminal por nivel, sin la producción genérica `expr ::= expr op expr`:

```
expr_modulo → expr_multiplicativa → expr_aditiva → expr_potencia
```

`expr_potencia` asocia por la derecha; los demás niveles por la izquierda.

### Correcciones del Proyecto 1

Según la revisión del Proyecto 1 se corrigió:

- Tabla de símbolos por alcance: cada token indica la tabla real a la que pertenece (global, funcion f, principal, if, while…) y se genera el reporte `_simbolos.txt`.
- Arreglos con tamaño (`val int datos ʃ: 10 :ʅ »`) y creación con valores iniciales `¿: … :?`.
- `principal` puede ir antes que otras funciones, y las funciones pueden declararse en cualquier orden.
- `write` solo acepta literales o variables (Anexo I, punto p).
- `for` con la forma `є: inicio » condición » actualización :э`.
- Relacionales y lógicas: anidación en `==` y `!=`, encadenamiento de lógicas y negación con `Σ`, también dentro de condiciones.
- Bloques anidados sueltos dentro de otros bloques.
- `return` siempre con expresión, y funciones que retornan char.

### Tablas de símbolos

Hay una tabla por alcance, organizada como un árbol: `global` es la raíz, sus hijas son las funciones y `principal`, y dentro de ellas están las tablas de if, elif, else, while, for y los bloques sueltos.

- Los parámetros y el cuerpo de una función forman un solo alcance, que empieza en el `є:` de la función.
- La variable declarada en el encabezado de un `for` pertenece al alcance del `for`.
- Las funciones se guardan en la tabla global con su tipo de retorno y la lista de sus parámetros.

`GestorTablas` no usa una pila: anota cada declaración y cada alcance (con su rango de inicio y fin en el código) a medida que el parser los reduce, y al terminar `programa` arma el árbol ubicando cada declaración en el alcance más interno que la contiene. Si en una tabla ya existe el mismo nombre, el símbolo nuevo se guarda marcado como redeclarado y la semántica lo reporta como error de unicidad.

### Analizador semántico

La clase `Semantico` tiene un método por cada construcción del lenguaje (`binaria`, `unaria`, `asignacion`, `llamada`, `retorno`, `romper`, `leer`, `escribir`, etc.). Cada método:

- no hace nada en la pasada 1;
- si un operando ya viene con tipo `error`, devuelve `error` sin reportar nada nuevo;
- si encuentra un problema, lo reporta con `ErrorReporter.semantico(mensaje, columna, línea)` y devuelve `error`;
- si todo está bien, devuelve el tipo del resultado.

### Generador de código intermedio

`GeneradorTAC` genera código de tres direcciones siguiendo el anexo 13. Cada método recibe el código de los hijos y devuelve el del padre, así que el código se arma de abajo hacia arriba, igual que el parser.

| Construcción | Código generado |
|---|---|
| Literal o variable | Se copia a un temporal: `t1 = 5`, `t2 = a` |
| Operación binaria | Un temporal nuevo por operación: `t3 = t1 + t2`; `mod` se escribe `%` y `pot` se escribe `^` |
| Asignación | `x = t3` |
| Arreglos | Escritura `notas[t4] = t1`, lectura `t25 = notas[t24]` |
| if / elif / else | `if t goto if_true_n`, `goto if_false_n`, etiquetas `elif_true_m`, `elif_false_m` e `if_end_n` |
| while | Etiquetas `while_n`, `while_body_n` y `while_end_n` |
| for | Inicio, etiquetas `for_n` y `for_body_n`, la actualización al final del cuerpo y `for_end_n` |
| break | `goto` a la etiqueta de fin del ciclo más cercano |
| Función | Etiqueta `tipo_nombre:`, `param_tipo nombre` por cada parámetro, cuerpo y `tipo_nombre_end` |
| Llamada | `param t` por cada argumento y `t = call f, n` (n = cantidad de argumentos) |
| read / write | `t = call read, 0` y luego `x = t`; `param t` y `call write, 1` |
| return | `return t` |

Las etiquetas de una misma estructura comparten número (`if_true_1`, `if_false_1`, `if_end_1`) y los temporales son `t1, t2, …` en todo el programa. Para `break`, el generador emite una marca provisional y el `while` o `for` que lo contiene la reemplaza por el salto a su propia etiqueta de fin.

### Recuperación en modo pánico

- `syntax_error(Symbol)` reporta el error con línea y columna; `unrecovered_syntax_error(Symbol)` reporta los errores no recuperables.
- Producciones `error` que sincronizan con `»` o con el cierre de un bloque: en `lista_sentencias`, `bloque`, `elemento_programa` (globales y encabezados de función) y `condicion`.
- Los errores léxicos se reportan y el lexer sigue con el siguiente carácter; los semánticos se reportan y el análisis continúa con el tipo `error`.

### Clases Java

| Clase | Responsabilidad |
|---|---|
| `Main` | Valida la ruta, ejecuta las dos pasadas, escribe los reportes y muestra el veredicto |
| `ErrorReporter` | Acumula los errores léxicos, sintácticos y semánticos, los ordena por línea y columna, los muestra y los guarda |
| `ReportingLexer` | Envuelve el lexer de JFlex y registra cada token en `TokenReporter` antes de entregarlo al parser |
| `TokenReporter` | Escribe el archivo de tokens con la tabla y la información de cada token |
| `PruebaLexer` | Utilidad para probar el lexer por separado |
| `atributos.Expr`, `Codigo`, `Bloque`, `Rama` | Lo que viaja en `RESULT` entre las producciones: tipo, temporal y código |
| `simbolos.Simbolo`, `TablaSimbolos` | Un identificador declarado y la tabla de un alcance |
| `simbolos.GestorTablas` | Arma el árbol de tablas a partir de las acciones del parser |
| `simbolos.ReporteSimbolos` | Escribe el archivo `_simbolos.txt` |
| `semantico.Semantico` | Reglas del análisis semántico |
| `tac.GeneradorTAC` | Generación del código de tres direcciones y escritura de `_tac.txt` |

---

## Validación semántica

- **Dónde:** dentro de las acciones de `Parser.cup`, con atributos sintetizados. Cada expresión devuelve un `Expr` con su tipo y el padre usa los tipos de sus hijos para validar y calcular el suyo. Todas las reglas están en la clase `Semantico`; las acciones solo llaman métodos.
- **Tipado explícito y fuerte:** toda la compatibilidad pasa por un solo método, `compatible(destino, origen)`, que exige tipos iguales. No hay conversión automática de int a float en asignaciones, argumentos, returns ni operaciones.
- **Sin errores en cascada:** una expresión con error pasa a tener tipo `error`, y cualquier operación que lo reciba no reporta nada nuevo. Las producciones de error del parser devuelven `null`, que también se trata como `error`.
- **Gramática o semántica:** si una regla depende solo de la forma del código, va en la gramática (un único `principal`, negativo solo sobre literales, `write` solo con literales o variables, arreglos solo de int o float, `return` siempre con valor); si depende de tipos o declaraciones, va en la semántica.

Se implementaron 38 validaciones:

| Tema | Validaciones |
|---|---|
| Uso de identificadores | Declarada antes de usarse en un alcance visible; una función no se usa como variable; un arreglo no se usa sin índice |
| Expresiones | `+ - *` entre dos int o dos float; `/` siempre da float; `//`, `mod` y `pot` solo con int; comparaciones entre el mismo tipo; `< <= > >=` solo con números; `== !=` sin string; `λ θ Σ` solo con bool; `++` y `--` solo en variables int o float |
| Arreglos | Solo se indexa un arreglo; índice int; tamaño mayor que 0; tantos valores iniciales como su tamaño; valores del tipo del arreglo |
| Asignaciones y declaraciones | Destino declarado y que no sea función; no se asigna a un arreglo completo; el valor es del tipo del destino o del elemento |
| Funciones | La función existe y es función; misma cantidad y tipo de argumentos, en orden; no retorna string; tiene al menos un `return` |
| Control de flujo | Condiciones bool; actualización del `for` numérica; `principal` sin `return`; `return` del tipo de su función; `break` solo en `while` o `for`; nada después de un `return` o `break` en el mismo bloque |
| Sentencias | Una expresión sola como sentencia solo puede ser llamada, `++` o `--`; `read` en variables int, float o char; `write` de string, int, bool o float |
| Unicidad | No se declara dos veces el mismo nombre en el mismo alcance |

La justificación de cada decisión está en la sección 7 de la documentación.

---

## Librerías usadas

| Librería | Uso |
|---|---|
| JFlex 1.9.1 (`jflex-full-1.9.1.jar`) | Genera el analizador léxico a partir de `Lexer.flex`. Solo se usa al compilar |
| CUP 11b (`java-cup-11b.jar`) | Genera el analizador sintáctico a partir de `Parser.cup`. Solo se usa al compilar |
| CUP runtime (`java-cup-11b-runtime.jar`) | Clases que necesita el parser al ejecutarse (`Symbol`, `Scanner`, `lr_parser`) |
| `java.io` | Lectura del archivo fuente y escritura de los reportes |
| `java.nio.file` (`Path`, `Files`) | Rutas y creación de los archivos de salida |
| `java.nio.charset.StandardCharsets` | Lectura y escritura explícita en UTF-8 |
| `java.util` | Listas y comparadores (por ejemplo, para ordenar los errores) |

No se usa ninguna otra librería externa.

---

## Análisis de resultados

### Objetivos alcanzados

- Se conservaron el análisis léxico y sintáctico del Proyecto 1 y se aplicaron las correcciones de su revisión.
- Tablas de símbolos por alcance, organizadas como árbol, con reporte en archivo.
- Análisis semántico con tipado explícito y fuerte (38 validaciones).
- Errores semánticos con línea y columna, sin errores en cascada, y el análisis continúa.
- Recuperación en modo pánico para los errores léxicos y sintácticos.
- Veredicto final que considera los errores léxicos, sintácticos y semánticos.
- Código de tres direcciones para expresiones, arreglos, estructuras de control, `break`, funciones, llamadas, `read` y `write`, escrito en `_tac.txt`.
- Documentación interna (entradas, salidas, restricciones y objetivo) en todas las clases.

### Objetivos no alcanzados

- Puntos extra de recuperación a nivel de frase, producciones de error o corrección global: no se implementaron como tales; la recuperación es en modo pánico.
- Puntos extra de clases: no se implementaron.
- Exponente positivo en `pot`: se valida que ambos operandos sean int, pero no se rechaza un exponente literal negativo como `3 pot -1`.

### Limitaciones conocidas

- El código intermedio solo se genera cuando el archivo no tiene errores, porque el enunciado pide código "con el mismo sentido semántico" que el fuente.
- No se validan en compilación el índice fuera de rango ni la división entre cero.

---

## Bitácora

La bitácora autogenerada con `git log` está en la sección 8 de la documentación y en `programa/bitacora.txt`.

Repositorio: https://github.com/StevenXoFk/PY02-Analisis-semantico-y-generacion-de-codigo-intermedio
