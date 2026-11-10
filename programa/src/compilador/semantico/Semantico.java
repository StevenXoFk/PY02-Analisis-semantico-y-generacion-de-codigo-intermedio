package compilador.semantico;

import compilador.ErrorReporter;
import compilador.atributos.Expr;
import compilador.generated.sym;
import compilador.simbolos.Simbolo;
import compilador.simbolos.TablaSimbolos;
import java_cup.runtime.Symbol;

import java.util.ArrayList;
import java.util.List;

/**
 * Reglas del análisis semántico. Las acciones de Parser.cup llaman a estos métodos antes de
 * llamar a GeneradorTAC: aquí se valida y se calcula el tipo; GeneradorTAC solo genera código.
 *
 * Se trabaja en dos pasadas: la pasada 1 arma el árbol de tablas de símbolos completo
 * y la pasada 2 lo recibe en iniciar(). Con ese árbol se resuelve, para cualquier posición del
 * fuente, en qué alcance está, qué nombres son visibles, a qué función pertenece un return y si
 * un break está dentro de un ciclo, aunque el parser bottom-up todavía no haya cerrado esos
 * alcances.
 *
 * @Entrada: atributos de las expresiones, nombres y posiciones (línea, columna) que pasan
 *           las acciones del parser.
 * @Salida: el tipo resultante de cada expresión y los errores
 *          semánticos reportados en ErrorReporter.semantico(msj, columna, línea).
 * @Restricciones:
 * - En la pasada 1 no valida ni reporta nada: esSegundaPasada() es false.
 * - Si algún operando ya es de tipo "error" (o null por una producción error), el resultado es
 *   "error" y no se reporta otro error (evita errores en cascada).
 * @Objetivo: Implementar la comprobación de tipos, unicidad, declaración antes de uso, llamadas,
 *            return y break.
 */
public final class Semantico {

    private static TablaSimbolos tablaGlobal = null;

    // Funciones que tienen al menos un return (se llena en la pasada 2)
    private static List<Simbolo> funcionesConReturn = new ArrayList<>();

    private Semantico() { }

    /**
     * Prepara la pasada 2 con el árbol de tablas de la pasada 1.
     * @Entrada: tabla global de la pasada 1.
     * @Salida: la semántica queda activa.
     */
    public static void iniciar(TablaSimbolos global) {
        tablaGlobal = global;
        funcionesConReturn.clear();
    }

    /**
     * Deja la semántica inactiva para la pasada 1.
     * @Entrada: Ninguna.
     * @Salida: sin tabla global; ningún método valida.
     */
    public static void reiniciar() {
        tablaGlobal = null;
        funcionesConReturn.clear();
    }

    /**
     * Indica si se está en la pasada 2.
     * @Entrada: Ninguna.
     * @Salida: true si ya se tiene la tabla global de la pasada 1.
     */
    public static boolean esSegundaPasada() {
        return tablaGlobal != null;
    }

    /**
     * Resuelve un identificador usado en una expresión.
     * @Entrada: nombre y posición del uso.
     * @Salida: su declaración visible (buscarAntesDe en el alcance más interno), o null.
     * @Restricciones: reporta error si no está declarada antes del uso o si es una función.
     */
    public static Simbolo usarVariable(String nombre, int linea, int columna) {
        if (!esSegundaPasada()) {
            return null;
        }
        // Alcance donde está el uso
        long posicion = TablaSimbolos.posicion(linea, columna);
        TablaSimbolos alcance = tablaGlobal.tablaMasInterna(posicion);
        if (alcance == null) {
            alcance = tablaGlobal;
        }

        Simbolo simbolo = alcance.buscarAntesDe(nombre, posicion);

        // No declarada (o declarada después del uso)
        if (simbolo == null) {
            ErrorReporter.semantico("La variable '" + nombre + "' no está declarada", columna, linea);
            return null;
        }

        // Una función no se puede usar como variable
        Simbolo.Categoria categoria = simbolo.getCategoria();
        boolean esFuncion = categoria == Simbolo.Categoria.FUNCION
                || categoria == Simbolo.Categoria.PRINCIPAL;
        if (esFuncion) {
            ErrorReporter.semantico("'" + nombre + "' es una función, no una variable", columna, linea);
            return null;
        }

        return simbolo;
    }

    /**
     * Tipo de un símbolo resuelto.
     * @Entrada: símbolo (puede ser null).
     * @Salida: su tipo, o "error" si no hay símbolo.
     */
    public static String tipoDe(Simbolo s) {
        return s == null ? Expr.ERROR : s.getTipo();
    }

    /**
     * Uso de una variable suelta como valor (en una expresión o en write), es decir,
     * un ID sin ʃ: :ʅ. Si el ID va con ʃ: :ʅ, el parser usa acceso_arreglo y llama a acceso().
     * @Entrada: nombre y posición del uso.
     * @Salida: tipo de la variable o "error".
     * @Restricciones: un arreglo sin ʃ: :ʅ no se puede usar como valor, solo sus elementos.
     */
    public static String valorDeVariable(String nombre, int linea, int columna) {
        // usarVariable ya reporta si no existe o si es una función
        Simbolo variable = usarVariable(nombre, linea, columna);
        if (variable == null) {
            return Expr.ERROR;
        }

        // Un arreglo sin ʃ: :ʅ no es un valor
        if (variable.getCategoria() == Simbolo.Categoria.ARREGLO) {
            ErrorReporter.semantico("'" + nombre + "' es un arreglo; use un elemento con " + nombre + " ʃ: indice :ʅ", columna, linea);
            return Expr.ERROR;
        }
        return variable.getTipo();
    }

    /**
     * Tipo de un literal.
     * @Entrada: tipo del literal ("int", "float", "char", "string", "bool").
     * @Salida: el mismo tipo.
     */
    public static String literal(String tipo) {
        return tipo;
    }

    /**
     * Operación binaria: aritmética, relacional o
     * lógica.
     * @Entrada: operandos, operador canónico y posición del operador.
     * @Salida: tipo del resultado o "error".
     * @Restricciones: si un operando ya es "error" no se reporta otro error.
     */
    public static String binaria(Expr a, String op, Expr b, int linea, int columna) {
        if (Expr.esError(a) || Expr.esError(b)) {
            return Expr.ERROR;
        }
        switch (op) {
            // Relacionales
            case "<":
            case "<=":
            case ">":
            case ">=":
            case "==":
            case "!=":
                return relacional(a.getTipo(), op, b.getTipo(), linea, columna);

            // Lógicos: λ (and), θ (or)
            case "λ":
            case "θ":
                return logica(a.getTipo(), op, b.getTipo(), linea, columna);

            // Aritméticos: + - * / // mod pot
            default:
                return aritmetica(a.getTipo(), op, b.getTipo(), linea, columna);
        }
    }

    /**
     * Operación relacional: < <= > >= == !=.
     * @Entrada: tipo de cada operando, operador y posición del operador.
     * @Salida: "bool" o "error".
     * @Restricciones: tipado estricto, no se mezclan int y float.
     *                 < <= > >= solo comparan dos int o dos float;
     *                 == != además aceptan bool y char, pero no string (Anexo I, punto j).
     */
    private static String relacional(String tipoA, String op, String tipoB, int linea, int columna) {
        if (!esSegundaPasada()) {
            return Expr.ERROR;
        }
        // Toda comparación necesita los dos lados del mismo tipo
        if (!tipoA.equals(tipoB)) {
            ErrorReporter.semantico("El operador '" + op + "' no puede comparar " + tipoA + " con " + tipoB, columna, linea);
            return Expr.ERROR;
        }

        // < <= > >= además solo con números
        boolean esOrden = op.equals("<") || op.equals("<=") || op.equals(">") || op.equals(">=");
        boolean esNumero = tipoA.equals("int") || tipoA.equals("float");
        if (esOrden && !esNumero) {
            ErrorReporter.semantico("El operador '" + op + "' solo compara números, se recibió " + tipoA, columna, linea);
            return Expr.ERROR;
        }

        // == != además aceptan bool y char, pero no string
        boolean esBoolOChar = tipoA.equals("bool") || tipoA.equals("char");
        if (!esOrden && !esNumero && !esBoolOChar) {
            ErrorReporter.semantico("El operador '" + op + "' solo compara int, float, bool o char, se recibió " + tipoA, columna, linea);
            return Expr.ERROR;
        }

        return "bool";
    }

    /**
     * Operación lógica: λ (and), θ (or).
     * @Entrada: tipo de cada operando, operador y posición del operador.
     * @Salida: "bool" o "error".
     * @Restricciones: los dos operandos deben ser bool.
     */
    private static String logica(String tipoA, String op, String tipoB, int linea, int columna) {
        if (!esSegundaPasada()) {
            return Expr.ERROR;
        }
        // Los dos lados deben ser bool
        if (!tipoA.equals("bool") || !tipoB.equals("bool")) {
            ErrorReporter.semantico("El operador '" + op + "' solo acepta bool, se recibió " + tipoA + " y " + tipoB, columna, linea);
            return Expr.ERROR;
        }
        return "bool";
    }

    /**
     * Operación aritmética: + - * / // mod pot.
     * @Entrada: tipo de cada operando, operador y posición del operador.
     * @Salida: tipo del resultado o "error".
     * @Restricciones: tipado estricto, no se mezclan int y float.
     *                 + - * aceptan dos int o dos float; / igual pero siempre da float;
     *                 // mod pot solo aceptan dos int.
     */
    private static String aritmetica(String tipoA, String op, String tipoB, int linea, int columna) {
        if (!esSegundaPasada()) {
            return Expr.ERROR;
        }
        boolean ambosInt = "int".equals(tipoA) && "int".equals(tipoB);
        boolean ambosFloat = "float".equals(tipoA) && "float".equals(tipoB);

        // División entera, módulo y potencia: solo int
        if (op.equals("//") || op.equals("mod") || op.equals("pot")) {
            if (ambosInt) {
                return "int";
            }
            ErrorReporter.semantico("El operador '" + op + "' solo acepta int, se recibió " + tipoA + " y " + tipoB, columna, linea);
            return Expr.ERROR;
        }

        // + - * /: dos int o dos float
        if (!ambosInt && !ambosFloat) {
            ErrorReporter.semantico("El operador '" + op + "' no se puede aplicar a " + tipoA + " y " + tipoB, columna, linea);
            return Expr.ERROR;
        }

        // La división decimal siempre da float
        if (op.equals("/")) {
            return "float";
        }
        return tipoA;
    }

    /**
     * Operación unaria: negativo de un literal o negación lógica.
     * @Entrada: operador, operando y posición del operador.
     * @Salida: tipo del resultado o "error".
     * @Restricciones: Σ solo acepta bool; el negativo no se valida porque la gramática
     *                 solo lo permite sobre literales numéricos.
     */
    public static String unaria(String op, Expr a, int linea, int columna) {
        if (Expr.esError(a)) {
            return Expr.ERROR;
        }
        // Negativo: mismo tipo del literal
        if (op.equals("-")) {
            return a.getTipo();
        }

        // Negación Σ: solo bool
        if (!esSegundaPasada()) {
            return Expr.ERROR;
        }
        if (!a.getTipo().equals("bool")) {
            ErrorReporter.semantico("El operador 'Σ' solo acepta bool, se recibió " + a.getTipo(), columna, linea);
            return Expr.ERROR;
        }
        return "bool";
    }

    /**
     * Incremento o decremento posfijo.
     * @Entrada: nombre de la variable, operador y posición.
     * @Salida: tipo de la variable o "error".
     * @Restricciones: solo variables int o float; no arreglos completos ni funciones.
     */
    public static String incremento(String nombre, String op, int linea, int columna) {
        // usarVariable ya reporta si no existe o si es una función
        Simbolo variable = usarVariable(nombre, linea, columna);
        if (variable == null) {
            return Expr.ERROR;
        }

        // No se aplica a un arreglo completo
        if (variable.getCategoria() == Simbolo.Categoria.ARREGLO) {
            ErrorReporter.semantico("'" + nombre + "' es un arreglo; '" + op + "' solo se aplica a variables", columna, linea);
            return Expr.ERROR;
        }

        // Solo int o float
        String tipo = variable.getTipo();
        if (!tipo.equals("int") && !tipo.equals("float")) {
            ErrorReporter.semantico("El operador '" + op + "' solo se aplica a int o float, '" + nombre + "' es " + tipo, columna, linea);
            return Expr.ERROR;
        }
        return tipo;
    }

    /**
     * Acceso a un elemento de arreglo a[i].
     * @Entrada: nombre del arreglo, expresión del índice y posición.
     * @Salida: tipo del elemento o "error".
     * @Restricciones: el nombre debe ser un arreglo y el índice int.
     */
    public static String acceso(String nombre, Expr indice, int linea, int columna) {
        // Si no existe o es una función, usarVariable ya reportó el error y devuelve null:
        // aquí solo se sale, sin reportar otra vez
        Simbolo arreglo = usarVariable(nombre, linea, columna);
        if (arreglo == null) {
            return Expr.ERROR;
        }

        // Solo los arreglos tienen elementos
        if (arreglo.getCategoria() != Simbolo.Categoria.ARREGLO) {
            ErrorReporter.semantico("'" + nombre + "' no es un arreglo", columna, linea);
            return Expr.ERROR;
        }

        // El índice ya traía error: se ignora
        if (Expr.esError(indice)) {
            return Expr.ERROR;
        }

        // El índice debe ser int
        if (!indice.getTipo().equals("int")) {
            ErrorReporter.semantico("El índice de '" + nombre + "' debe ser int, se recibió " + indice.getTipo(), columna, linea);
            return Expr.ERROR;
        }
        return arreglo.getTipo();
    }

    /**
     * Llamada a función.
     * @Entrada: nombre, argumentos en orden y posición.
     * @Salida: tipo de retorno de la función o "error".
     * @Restricciones: la función debe existir y recibir la misma cantidad de argumentos, en el
     *                 mismo orden y del mismo tipo que sus parámetros. principal no se puede
     *                 llamar porque es palabra reservada (error sintáctico).
     */
    public static String llamada(String nombre, List<Expr> argumentos, int linea, int columna) {
        if (!esSegundaPasada()) {
            return Expr.ERROR;
        }
        // Las funciones son visibles en todo el archivo: se buscan en la global
        Simbolo funcion = tablaGlobal.buscarLocal(nombre);
        if (funcion == null) {
            ErrorReporter.semantico("La función '" + nombre + "' no está declarada", columna, linea);
            return Expr.ERROR;
        }

        // Una variable global no se puede llamar
        if (funcion.getCategoria() != Simbolo.Categoria.FUNCION) {
            ErrorReporter.semantico("'" + nombre + "' no es una función", columna, linea);
            return Expr.ERROR;
        }

        // Misma cantidad de argumentos que de parámetros
        List<Simbolo> parametros = funcion.getParametros();
        if (argumentos.size() != parametros.size()) {
            ErrorReporter.semantico("'" + nombre + "' espera " + parametros.size() + " argumento(s), se dieron " + argumentos.size(), columna, linea);
            return funcion.getTipo();
        }

        // Cada argumento del tipo de su parámetro, en el mismo orden
        for (int i = 0; i < argumentos.size(); i++) {
            Expr argumento = argumentos.get(i);
            Simbolo parametro = parametros.get(i);

            // El argumento ya traía error: se salta
            if (Expr.esError(argumento)) {
                continue;
            }
            // Debe ser del tipo de su parámetro
            if (!compatible(parametro.getTipo(), argumento.getTipo())) {
                ErrorReporter.semantico("El argumento " + (i + 1) + " de '" + nombre + "' debe ser " + parametro.getTipo() + ", se recibió " + argumento.getTipo(), argumento.getColumna(), argumento.getLinea());
            }
        }
        return funcion.getTipo();
    }

    /**
     * Asignación a una variable simple.
     * @Entrada: nombre del destino, valor y posición del destino.
     * @Salida: errores reportados si no son compatibles.
     * @Restricciones: el destino debe estar declarado, no ser arreglo ni función, y el valor
     *                 debe ser del mismo tipo.
     */
    public static void asignacion(String nombre, Expr valor, int linea, int columna) {
        if (!esSegundaPasada()) {
            return;
        }
        // usarVariable ya reporta si no existe o si es una función
        Simbolo destino = usarVariable(nombre, linea, columna);
        if (destino == null || Expr.esError(valor)) {
            return;
        }

        // Un arreglo completo no se asigna, solo sus elementos
        if (destino.getCategoria() == Simbolo.Categoria.ARREGLO) {
            ErrorReporter.semantico("'" + nombre + "' es un arreglo; asigne a un elemento con " + nombre + " ʃ: indice :ʅ", columna, linea);
            return;
        }

        if (!compatible(destino.getTipo(), valor.getTipo())) {
            ErrorReporter.semantico("No se puede asignar un valor de tipo " + valor.getTipo() + " a '" + nombre + "' de tipo " + destino.getTipo(), columna, linea);
        }
    }

    /**
     * Asignación a un elemento de arreglo.
     * @Entrada: destino (Expr del acceso, con nombre e índice), valor y posición.
     * @Salida: errores reportados si no son compatibles.
     * @Restricciones: el acceso (arreglo e índice) ya lo validó acceso().
     */
    public static void asignacionArreglo(Expr destino, Expr valor, int linea, int columna) {
        // Pasada 1, o el acceso o el valor ya tenían error: se ignora
        if (!esSegundaPasada() || Expr.esError(destino) || Expr.esError(valor)) {
            return;
        }
        if (!compatible(destino.getTipo(), valor.getTipo())) {
            ErrorReporter.semantico("No se puede asignar un valor de tipo " + valor.getTipo() + " a un elemento de '" + destino.getNombre() + "' de tipo " + destino.getTipo(), columna, linea);
        }
    }

    /**
     * Creación de variable con o sin valor inicial.
     * @Entrada: símbolo declarado y valor (null si no tiene).
     * @Salida: errores reportados si el valor no es compatible.
     * @Restricciones: si no hay valor o el valor ya tiene error, no se reporta nada.
     */
    public static void declaracion(Simbolo simbolo, Expr valor) {
        if (!esSegundaPasada() || valor == null || Expr.esError(valor)) {
            return;
        }
        if (!compatible(simbolo.getTipo(), valor.getTipo())) {
            ErrorReporter.semantico("No se puede asignar un valor de tipo " + valor.getTipo() + " a '" + simbolo.getLexema() + "' de tipo " + simbolo.getTipo(), simbolo.getColumna(), simbolo.getLinea());
        }
    }

    /**
     * Creación de arreglo.
     * @Entrada: símbolo del arreglo (con su dimensión) y valores iniciales (lista vacía si no hay).
     * @Salida: errores reportados (tamaño, cantidad o tipo de valores).
     * @Restricciones: el tamaño debe ser mayor que 0; si hay valores, deben ser exactamente
     *                 tantos como el tamaño y del mismo tipo del arreglo.
     */
    public static void creacionArreglo(Simbolo simbolo, List<Expr> valores) {
        if (!esSegundaPasada()) {
            return;
        }
        String nombre = simbolo.getLexema();
        int tamano = simbolo.getDimension();

        // Tamaño mayor que 0
        if (tamano <= 0) {
            ErrorReporter.semantico("El tamaño de '" + nombre + "' debe ser mayor que 0", simbolo.getColumna(), simbolo.getLinea());
            return;
        }

        // Sin valores iniciales no hay más que revisar
        if (valores.isEmpty()) {
            return;
        }

        // Tantos valores como el tamaño
        if (valores.size() != tamano) {
            ErrorReporter.semantico("'" + nombre + "' tiene tamaño " + tamano + " pero se dieron " + valores.size() + " valores", simbolo.getColumna(), simbolo.getLinea());
            return;
        }

        // Cada valor del tipo del arreglo
        for (Expr valor : valores) {
            if (!compatible(simbolo.getTipo(), valor.getTipo())) {
                ErrorReporter.semantico("'" + nombre + "' es de tipo " + simbolo.getTipo() + ", no acepta el valor de tipo " + valor.getTipo(), valor.getColumna(), valor.getLinea());
            }
        }
    }

    /**
     * Condición de if, elif, while o for.
     * @Entrada: expresión de la condición.
     * @Salida: error si no es bool.
     * @Restricciones: el error se ubica en la posición de la expresión.
     */
    public static void condicion(Expr condicion) {
        // Pasada 1, o la condición ya tenía error: se ignora
        if (!esSegundaPasada() || Expr.esError(condicion)) {
            return;
        }
        // Debe ser bool
        if (!condicion.getTipo().equals("bool")) {
            ErrorReporter.semantico("La condición debe ser bool, se recibió " + condicion.getTipo(), condicion.getColumna(), condicion.getLinea());
        }
    }

    /**
     * Expresión usada sola como sentencia, por ejemplo: doble є: 5 :э » o a++ ».
     * @Entrada: la expresión.
     * @Salida: error si la expresión no hace nada (su resultado se pierde).
     * @Restricciones: solo se permiten llamadas a función y ++/-- (Anexo I, punto e: "en algunos
     *                 casos, sólo expresiones sin asignación"). Es la misma regla de Java.
     */
    public static void sentenciaExpresion(Expr expresion) {
        // Pasada 1, o la expresión ya traía error: se ignora
        if (!esSegundaPasada() || Expr.esError(expresion)) {
            return;
        }
        // Solo una llamada o un ++/-- hacen algo por sí solos
        if (!expresion.isLlamadaOIncremento()) {
            ErrorReporter.semantico("La expresión no es una sentencia válida; solo se permiten llamadas a función, ++ o --",
                    expresion.getColumna(), expresion.getLinea());
        }
    }

    /**
     * Tercera sección del for cuando es una expresión (no una asignación).
     * @Entrada: expresión de la actualización.
     * @Salida: error si no es numérica.
     * @Restricciones: acepta operaciones algebraicas (indicación del profesor), por ejemplo
     *                 i++ o i + b * 3; el resultado debe ser int o float. El error se ubica en
     *                 la posición de la expresión.
     */
    public static void actualizacionFor(Expr actualizacion) {
        // Pasada 1, o la expresión ya traía error: se ignora
        if (!esSegundaPasada() || Expr.esError(actualizacion)) {
            return;
        }
        // Debe ser una expresión algebraica: int o float
        String tipo = actualizacion.getTipo();
        if (!tipo.equals("int") && !tipo.equals("float")) {
            ErrorReporter.semantico("La actualización del for debe ser numérica (int o float), se recibió " + tipo,
                    actualizacion.getColumna(), actualizacion.getLinea());
        }
    }

    /**
     * Sentencia return.
     * @Entrada: valor retornado y posición de "return".
     * @Salida: error si no coincide con el tipo de la función que lo contiene o si está en principal.
     * @Restricciones: principal es void, así que no puede tener return (la gramática exige valor).
     */
    public static void retorno(Expr valor, int linea, int columna) {
        if (!esSegundaPasada()) {
            return;
        }

        // 1. Tabla donde está el return
        long posicion = TablaSimbolos.posicion(linea, columna);
        TablaSimbolos alcance = tablaGlobal.tablaMasInterna(posicion);

        // 2. Desde ahí, buscar hacia afuera el cuerpo de la función (o de principal)
        TablaSimbolos cuerpo = alcance.ancestroDeClase(
                TablaSimbolos.Clase.FUNCION,
                TablaSimbolos.Clase.PRINCIPAL);
        // No debería pasar: un return fuera de una función es error sintáctico
        if (cuerpo == null) {
            return;
        }

        // 3. principal es void: no tiene return
        if (cuerpo.getClase() == TablaSimbolos.Clase.PRINCIPAL) {
            ErrorReporter.semantico("principal es void, no puede tener return", columna, linea);
            return;
        }

        // 4. Buscar la función, la última declarada antes de su cuerpo
        Simbolo funcion = null;
        for (Simbolo simbolo : tablaGlobal.getSimbolos()) {
            // Solo funciones
            if (simbolo.getCategoria() != Simbolo.Categoria.FUNCION) {
                continue;
            }
            // Solo las que están antes del cuerpo; la última que cumpla es la buscada
            if (simbolo.getPosicion() < cuerpo.getInicio()) {
                funcion = simbolo;
            }
        }
        // No debería pasar: toda función se declara antes de su cuerpo
        if (funcion == null) {
            return;
        }

        // 5. Anotar que esta función tiene return (aunque el valor tenga error)
        funcionesConReturn.add(funcion);

        // El valor ya traía error: se ignora
        if (Expr.esError(valor)) {
            return;
        }

        // 6. El valor debe ser del tipo de la función
        if (!compatible(funcion.getTipo(), valor.getTipo())) {
            ErrorReporter.semantico("'" + funcion.getLexema() + "' retorna " + funcion.getTipo() + ", pero el return da " + valor.getTipo(), columna, linea);
        }
    }

    /**
     * Sentencia break.
     * @Entrada: posición de "break".
     * @Salida: error si no hay un while o for que lo contenga dentro de la misma función.
     * @Restricciones: las funciones no se anidan, así que al subir desde una función no se
     *                 puede llegar a un ciclo de afuera.
     */
    public static void romper(int linea, int columna) {
        if (!esSegundaPasada()) {
            return;
        }
        // Tabla donde está el break
        long posicion = TablaSimbolos.posicion(linea, columna);
        TablaSimbolos alcance = tablaGlobal.tablaMasInterna(posicion);

        // Desde ahí, buscar hacia afuera un while o un for
        TablaSimbolos ciclo = alcance.ancestroDeClase(
                TablaSimbolos.Clase.WHILE,
                TablaSimbolos.Clase.FOR);

        // Si no hay ningún ciclo alrededor, es error
        if (ciclo == null) {
            ErrorReporter.semantico("break solo puede ir dentro de un while o un for", columna, linea);
        }
    }

    /**
     * Código después de un return o un break, en el mismo bloque.
     * @Entrada: palabra ("return" o "break") y el token que sigue a su ».
     * @Salida: error si después viene otra sentencia (código que nunca se ejecuta).
     * @Restricciones: el parser ya leyó el token siguiente al reducir la sentencia; si no es
     *                 el :? que cierra el bloque, hay otra sentencia en el mismo bloque.
     */
    public static void codigoDespues(String palabra, Symbol siguiente) {
        if (!esSegundaPasada()) {
            return;
        }
        // Lo único que puede seguir es el cierre del bloque (o el fin del archivo)
        if (siguiente.sym == sym.CLOSE_LLAVES || siguiente.sym == sym.EOF) {
            return;
        }
        ErrorReporter.semantico("Código inalcanzable después de " + palabra, siguiente.right, siguiente.left);
    }

    /**
     * Sentencia read.
     * @Entrada: nombre de la variable y posición.
     * @Salida: error si no es una variable int, float o char.
     * @Restricciones: tipos según el Anexo I, punto p.
     */
    public static void leer(String nombre, int linea, int columna) {
        // usarVariable ya reporta si no existe o si es una función
        Simbolo variable = usarVariable(nombre, linea, columna);
        if (variable == null) {
            return;
        }

        // No en un arreglo completo
        if (variable.getCategoria() == Simbolo.Categoria.ARREGLO) {
            ErrorReporter.semantico("'" + nombre + "' es un arreglo; read solo lee en variables", columna, linea);
            return;
        }

        // Solo int, float o char
        String tipo = variable.getTipo();
        boolean sePuedeLeer = tipo.equals("int") || tipo.equals("float") || tipo.equals("char");
        if (!sePuedeLeer) {
            ErrorReporter.semantico("read solo lee int, float o char; '" + nombre + "' es " + tipo, columna, linea);
        }
    }

    /**
     * Sentencia write.
     * @Entrada: valor a escribir y posición de "write".
     * @Salida: error si el tipo no se puede escribir.
     * @Restricciones: tipos según el Anexo I, punto p (string, int, bool o float; no char).
     */
    public static void escribir(Expr valor, int linea, int columna) {
        if (!esSegundaPasada() || Expr.esError(valor)) {
            return;
        }
        // Solo string, int, bool o float
        String tipo = valor.getTipo();
        boolean sePuedeEscribir = tipo.equals("string") || tipo.equals("int") || tipo.equals("bool") || tipo.equals("float");
        if (!sePuedeEscribir) {
            ErrorReporter.semantico("write no puede escribir un valor de tipo " + tipo, columna, linea);
        }
    }

    /**
     * Validaciones que se hacen una vez sobre el árbol completo: unicidad, funciones string y
     * funciones sin return.
     * @Entrada: Ninguna (usa la tabla global).
     * @Salida: errores reportados.
     * @Restricciones: el único principal ya lo exige la gramática (error sintáctico).
     */
    public static void validarDeclaraciones() {
        if (!esSegundaPasada()) {
            return;
        }
        revisarTabla(tablaGlobal);

        // Toda función debe tener al menos un return
        for (Simbolo simbolo : tablaGlobal.getSimbolos()) {
            if (simbolo.getCategoria() != Simbolo.Categoria.FUNCION) {
                continue;
            }
            if (!funcionesConReturn.contains(simbolo)) {
                ErrorReporter.semantico("La función '" + simbolo.getLexema() + "' debe tener al menos un return", simbolo.getColumna(), simbolo.getLinea());
            }
        }
    }

    /**
     * Reporta los símbolos redeclarados de una tabla y de sus tablas hijas, y las funciones
     * que retornan string.
     * @Entrada: tabla a revisar.
     * @Salida: un error semántico por cada redeclaración y por cada función string.
     * @Restricciones: las funciones solo retornan int, float, char o bool (Anexo I, punto q).
     */
    private static void revisarTabla(TablaSimbolos tabla) {
        // 1. Revisar cada símbolo de esta tabla
        for (Simbolo simbolo : tabla.getSimbolos()) {
            String nombre = simbolo.getLexema();

            // Redeclarado en esta misma tabla (la marca la puso TablaSimbolos.agregar)
            if (simbolo.isRedeclarada()) {
                int lineaPrimera = simbolo.getPrimeraDeclaracion().getLinea();
                ErrorReporter.semantico("Redeclaración de '" + nombre + "' (primera declaración en la línea " + lineaPrimera + ")",
                        simbolo.getColumna(), simbolo.getLinea());
            }

            // Una función no puede retornar string
            if (simbolo.getCategoria() == Simbolo.Categoria.FUNCION && simbolo.getTipo().equals("string")) {
                ErrorReporter.semantico("La función '" + nombre + "' no puede retornar string (solo int, float, char o bool)",
                        simbolo.getColumna(), simbolo.getLinea());
            }
        }

        // 2. Hacer lo mismo con cada tabla hija (la función se llama a sí misma)
        for (TablaSimbolos hija : tabla.getHijos()) {
            revisarTabla(hija);
        }
    }

    /**
     * Regla única de compatibilidad de tipos (asignación, retorno, argumentos).
     * @Entrada: tipo del destino y tipo del valor.
     * @Salida: true si el valor puede guardarse en el destino.
     * @Restricciones: tipado estricto, los tipos deben ser iguales (no hay coerción int a float).
     */
    public static boolean compatible(String destino, String origen) {
        if (destino == null || origen == null) {
            return false;
        }
        return destino.equals(origen);
    }
}
