package compilador.tac;

import compilador.atributos.Codigo;
import compilador.atributos.Expr;
import compilador.atributos.Rama;
import compilador.simbolos.Simbolo;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Generador de código de tres direcciones por traducción dirigida por la sintaxis.
 *
 * @Entrada: Expr y Codigo de los hijos, nombres, tipos ya calculados por Semantico.
 * @Salida: Expr (expresiones: lugar + código) o Codigo (sentencias); el archivo TAC final.
 * @Restricciones:
 * - Supone que tipos, declaraciones y alcances ya se validaron: no vuelve a validar.
 * - Si recibe una Expr de tipo "error" o null, devuelve código vacío.
 * - Los operadores se reciben en forma canónica;
 *   mod se escribe % y pot se escribe ^ en el TAC.
 * @Objetivo: Generar el archivo de código intermedio.
 */
public final class GeneradorTAC {

    /** Línea provisional que deja un break; mientras/para la reemplazan por su etiqueta de salida. */
    public static final String MARCA_BREAK = "goto __break__";

    private static int temporales = 0;
    private static int etiquetas = 0;

    private GeneradorTAC() { }

    /**
     * Reinicia los contadores cuando empieza cada pasada.
     * @Entrada: Ninguna.
     * @Salida: el próximo temporal es t1 y la próxima etiqueta termina en _1.
     */
    public static void reiniciar() {
        temporales = 0;
        etiquetas = 0;
    }

    /**
     * Crea un temporal nuevo.
     * @Entrada: Ninguna.
     * @Salida: "t1", "t2", ...
     */
    public static String nuevoTemporal() {
        return "t" + (++temporales);
    }

    /**
     * Crea una etiqueta única.
     * @Entrada: nombre base.
     * @Salida: base + "_" + número.
     */
    public static String nuevaEtiqueta(String base) {
        return base + "_" + (++etiquetas);
    }

    // ===================== Expresiones====================

    /**
     * Literal
     * @Entrada: lexema del literal, tipo y posición.
     * @Salida: Expr con el temporal y su código.
     */
    public static Expr literal(String valor, String tipo, int linea, int columna) {
        return new Expr(tipo, linea, columna);
    }

    /**
     * Uso de variable
     * @Entrada: nombre, tipo y posición.
     * @Salida: Expr con el temporal, su código y el nombre de la variable.
     */
    public static Expr variable(String nombre, String tipo, int linea, int columna) {
        Expr e = new Expr(tipo, linea, columna);
        e.setNombre(nombre);
        return e;
    }

    /**
     * Operación binaria
     * @Entrada: operandos, operador canónico, tipo resultante y posición del operador.
     * @Salida: Expr con el temporal del resultado.
     */
    public static Expr binaria(Expr a, String op, Expr b, String tipo, int linea, int columna) {
        return new Expr(tipo, linea, columna);
    }

    /**
     * Operación unaria
     * @Entrada: operador, operando, tipo y posición.
     * @Salida: Expr con el temporal del resultado.
     */
    public static Expr unaria(String op, Expr a, String tipo, int linea, int columna) {
        return new Expr(tipo, linea, columna);
    }

    /**
     * Incremento/decremento
     * @Entrada: nombre, operador, tipo y posición.
     * @Salida: Expr con el temporal del valor.
     */
    public static Expr incremento(String nombre, String op, String tipo, int linea, int columna) {
        Expr e = new Expr(tipo, linea, columna);
        e.setNombre(nombre);
        return e;
    }

    /**
     * Acceso a arreglo
     * @Entrada: nombre, índice, tipo del elemento y posición.
     * @Salida: Expr con el temporal, el nombre y el índice
     */
    public static Expr acceso(String nombre, Expr indice, String tipo, int linea, int columna) {
        Expr e = new Expr(tipo, linea, columna);
        e.setNombre(nombre);
        e.setIndice(indice);
        return e;
    }

    /**
     * Llamada a función
     * @Entrada: nombre, argumentos en orden, tipo de retorno y posición.
     * @Salida: Expr con el temporal del valor retornado.
     */
    public static Expr llamada(String nombre, List<Expr> argumentos, String tipo, int linea, int columna) {
        return new Expr(tipo, linea, columna);
    }

    // ===================== Sentencias =====================

    /**
     * Expresión usada como sentencia
     * @Entrada: expresión.
     * @Salida: su código.
     */
    public static Codigo sentenciaExpr(Expr e) {
        return Codigo.vacio();
    }

    /**
     * Asignación
     * @Entrada: destino y valor.
     * @Salida: código de la asignación.
     */
    public static Codigo asignacion(String nombre, Expr valor) {
        return Codigo.vacio();
    }

    /**
     * Asignación a arreglo
     * @Entrada: destino (Expr del acceso, con nombre e índice) y valor.
     * @Salida: código de la asignación.
     */
    public static Codigo asignacionArreglo(Expr destino, Expr valor) {
        return Codigo.vacio();
    }

    /**
     * Creación de variable
     * @Entrada: símbolo declarado y valor (o null).
     * @Salida: código de la inicialización.
     */
    public static Codigo declaracion(Simbolo simbolo, Expr valor) {
        return Codigo.vacio();
    }

    /**
     * Creación de arreglo
     * @Entrada: símbolo del arreglo y valores iniciales.
     * @Salida: código de la inicialización.
     */
    public static Codigo creacionArreglo(Simbolo simbolo, List<Expr> valores) {
        return Codigo.vacio();
    }

    /**
     * if / elif / else con etiquetas únicas
     * @Entrada: condición y código del if, ramas elif en orden, código del else (o null).
     * @Salida: código completo de la estructura.
     */
    public static Codigo si(Expr condicion, Codigo entonces, List<Rama> ramas, Codigo sino) {
        return Codigo.vacio();
    }

    /**
     * while
     * @Entrada: condición y código del cuerpo.
     * @Salida: código completo del ciclo.
     */
    public static Codigo mientras(Expr condicion, Codigo cuerpo) {
        return Codigo.vacio();
    }

    /**
     * for
     * @Entrada: código del inicio, condición, código de la actualización y del cuerpo.
     * @Salida: código completo del ciclo.
     */
    public static Codigo para(Codigo inicio, Expr condicion, Codigo actualizacion, Codigo cuerpo) {
        return Codigo.vacio();
    }

    /**
     * break
     * @Entrada: Ninguna.
     * @Salida: código con la marca.
     */
    public static Codigo romper() {
        return Codigo.vacio();
    }

    /**
     * return
     * @Entrada: valor retornado.
     * @Salida: código del retorno.
     */
    public static Codigo retorno(Expr valor) {
        return Codigo.vacio();
    }

    /**
     * read
     * @Entrada: nombre de la variable.
     * @Salida: código de la lectura.
     */
    public static Codigo leer(String nombre) {
        return Codigo.vacio();
    }

    /**
     * write
     * @Entrada: valor a escribir.
     * @Salida: código de la escritura.
     */
    public static Codigo escribir(Expr valor) {
        return Codigo.vacio();
    }

    /**
     * Función
     * @Entrada: tipo de retorno, nombre, parámetros (Simbolo) y código del cuerpo.
     * @Salida: código completo de la función.
     */
    public static Codigo funcion(String tipo, String nombre, List<Simbolo> parametros, Codigo cuerpo) {
        return Codigo.vacio();
    }

    /**
     * Función principal
     * @Entrada: código del cuerpo.
     * @Salida: código completo de principal.
     */
    public static Codigo principal(Codigo cuerpo) {
        return Codigo.vacio();
    }

    /**
     * Programa completo en el orden del fuente.
     * @Entrada: código de los elementos antes de principal, de principal y de los elementos después.
     * @Salida: código de todo el programa.
     */
    public static Codigo programa(Codigo antes, Codigo principal, Codigo despues) {
        return Codigo.vacio().agregar(antes).agregar(principal).agregar(despues);
    }

    /**
     * Escribe el TAC en un archivo: las etiquetas sin sangría y las demás
     * instrucciones con 4 espacios.
     * @Entrada: ruta del archivo y código del programa.
     * @Salida: archivo UTF-8 con una instrucción por línea.
     */
    public static void escribirArchivo(Path ruta, Codigo programa) throws IOException {
        Path carpeta = ruta.getParent();
        if (carpeta != null) {
            Files.createDirectories(carpeta);
        }
        try (BufferedWriter w = Files.newBufferedWriter(ruta, StandardCharsets.UTF_8)) {
            if (programa == null) {
                return;
            }
            for (String linea : programa.getLineas()) {
                w.write(linea.endsWith(":") ? linea : "    " + linea);
                w.newLine();
            }
        }
    }
}
