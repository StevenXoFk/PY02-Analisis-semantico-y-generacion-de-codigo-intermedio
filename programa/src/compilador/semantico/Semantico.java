package compilador.semantico;

import compilador.ErrorReporter;
import compilador.atributos.Expr;
import compilador.simbolos.Simbolo;
import compilador.simbolos.TablaSimbolos;

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

    private Semantico() { }

    /**
     * Prepara la pasada 2 con el árbol de tablas de la pasada 1.
     * @Entrada: tabla global de la pasada 1.
     * @Salida: la semántica queda activa.
     */
    public static void iniciar(TablaSimbolos global) {
        tablaGlobal = global;
    }

    /**
     * Deja la semántica inactiva para la pasada 1.
     * @Entrada: Ninguna.
     * @Salida: sin tabla global; ningún método valida.
     */
    public static void reiniciar() {
        tablaGlobal = null;
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
     * @Restricciones: sábado: el nombre debe ser un arreglo y el índice int.
     */
    public static String acceso(String nombre, Expr indice, int linea, int columna) {
        return tipoDe(usarVariable(nombre, linea, columna));
    }

    /**
     * Llamada a función.
     * @Entrada: nombre, argumentos en orden y posición.
     * @Salida: tipo de retorno de la función o "error".
     * @Restricciones: sábado: existe, mismo número, orden y tipo de argumentos.
     */
    public static String llamada(String nombre, List<Expr> argumentos, int linea, int columna) {
        if (!esSegundaPasada()) {
            return Expr.ERROR;
        }
        // Las funciones son visibles en todo el archivo: se buscan desde la global
        Simbolo f = tablaGlobal.buscarLocal(nombre);
        if (f == null || f.getCategoria() != Simbolo.Categoria.FUNCION) {
            return Expr.ERROR;
        }
        return f.getTipo();
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
     */
    public static void asignacionArreglo(Expr destino, Expr valor, int linea, int columna) {
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
     */
    public static void creacionArreglo(Simbolo simbolo, List<Expr> valores) {
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
     * Tercera sección del for cuando es una expresión (no una asignación).
     * @Entrada: expresión de la actualización.
     * @Salida: error según la decisión pendiente (solo incremento o cualquier expresión).
     */
    public static void actualizacionFor(Expr actualizacion) {
    }

    /**
     * Sentencia return.
     * @Entrada: valor retornado y posición de "return".
     * @Salida: error si no coincide con el tipo de la función que lo contiene o si está en principal.
     */
    public static void retorno(Expr valor, int linea, int columna) {
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
     * Validaciones que se hacen una vez sobre el árbol completo: unicidad.
     * @Entrada: Ninguna (usa la tabla global).
     * @Salida: errores reportados.
     * @Restricciones: el único principal ya lo exige la gramática (error sintáctico).
     */
    public static void validarDeclaraciones() {
        if (!esSegundaPasada()) {
            return;
        }
        revisarTabla(tablaGlobal);
    }

    /**
     * Reporta los símbolos redeclarados de una tabla y de sus tablas hijas.
     * @Entrada: tabla a revisar.
     * @Salida: un error semántico por cada redeclaración.
     */
    private static void revisarTabla(TablaSimbolos tabla) {
        for (Simbolo simbolo : tabla.getSimbolos()) {
            if (simbolo.isRedeclarada()) {
                int lineaPrimera = simbolo.getPrimeraDeclaracion().getLinea();
                ErrorReporter.semantico("Redeclaración de '" + simbolo.getLexema() + "' (primera declaración en la línea " + lineaPrimera + ")", simbolo.getColumna(), simbolo.getLinea());
            }
        }
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
