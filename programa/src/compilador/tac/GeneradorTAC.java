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
 * Genera el código de tres direcciones (TAC)
 * Las acciones de Parser.cup lo llaman después de Semantico. Cada método recibe el código de
 * los hijos y devuelve el del padre: así el código se arma de abajo hacia arriba, igual que
 * el parser.
 *
 * Reglas que sigue:
 *   - todo identificador y todo literal se copia primero a un temporal (t1 = a, t2 = 5);
 *   - cada operación produce un temporal nuevo (t3 = t1 + t2);
 *   - las condiciones se traducen en "if t goto" y "goto";
 *   - cada estructura de control tiene etiquetas propias numeradas (if_true_1, while_2...).
 *
 * @Entrada: Expr y Codigo de los hijos, nombres y tipos (ya validados por Semantico).
 * @Salida: Expr (expresiones) o Codigo (sentencias) y al final el archivo _tac.txt.
 * @Restricciones:
 *   - No valida nada: el archivo solo se escribe si el programa no tiene errores.
 *   - En la pasada 1 también se ejecuta, por eso ningún método falla si recibe null.
 * @Objetivo: generar el código intermedio del programa.
 */
public class GeneradorTAC {
    public static final String MARCA_BREAK = "goto __break__";

    private static int temporales = 0;
    private static int etiquetas = 0;

    /**
     * Reinicia los contadores al empezar cada pasada.
     * @Entrada: ninguna.
     * @Salida: el próximo temporal es t1 y la próxima estructura usa el número 1.
     */
    public static void reiniciar() {
        temporales = 0;
        etiquetas = 0;
    }

    /**
     * Crea un temporal nuevo.
     * @Entrada: ninguna.
     * @Salida: "t1", "t2", ...
     */
    public static String nuevoTemporal() {
        temporales++;
        return "t" + temporales;
    }

    /**
     * Da un número nuevo para las etiquetas de una estructura. Todas las etiquetas de un mismo
     * if o ciclo usan el mismo número (if_true_1, if_false_1, if_end_1).
     * @Entrada: ninguna.
     * @Salida: 1, 2, 3, ...
     */
    public static int nuevoNumero() {
        etiquetas++;
        return etiquetas;
    }

    /**
     * Línea de instrucción: va con 4 espacios de sangría.
     * @Entrada: la instrucción, por ejemplo "t1 = a".
     * @Salida: "    t1 = a".
     */
    private static String instr(String texto) {
        return "    " + texto;
    }

    /**
     * Línea de etiqueta: sin sangría y terminada en dos puntos.
     * @Entrada: nombre de la etiqueta, por ejemplo "while_1".
     * @Salida: "while_1:".
     */
    private static String etiqueta(String nombre) {
        return nombre + ":";
    }

    /**
     * Código de una expresión, sin fallar si es null.
     * @Entrada: expresión.
     * @Salida: su código, o un código vacío.
     */
    private static Codigo codigoDe(Expr e) {
        if (e == null) {
            return Codigo.vacio();
        }
        return e.getCodigo();
    }

    /**
     * Temporal donde quedó el valor de una expresión, sin fallar si es null.
     * @Entrada: expresión.
     * @Salida: su lugar (t1, t2...), o "?" si no tiene.
     */
    private static String lugarDe(Expr e) {
        if (e == null || e.getLugar() == null) {
            return "?";
        }
        return e.getLugar();
    }

    /**
     * Cambia los operadores mod y pot por los símbolos que usa el anexo.
     * @Entrada: operador canónico.
     * @Salida: el operador como se escribe en el TAC.
     */
    private static String operadorTAC(String op) {
        if (op.equals("mod")) {
            return "%";
        }
        if (op.equals("pot")) {
            return "^";
        }
        return op;
    }

    // ---------------------------------------------------------------------------------
    // Expresiones
    // ---------------------------------------------------------------------------------

    /**
     * Literal: t1 = 5
     * @Entrada: texto del literal, tipo y posición.
     * @Salida: Expr con el temporal que guarda el literal.
     */
    public static Expr literal(String valor, String tipo, int linea, int columna) {
        Expr e = new Expr(tipo, linea, columna);
        String t = nuevoTemporal();
        e.setCodigo(Codigo.vacio().agregar(instr(t + " = " + valor)));
        e.setLugar(t);
        return e;
    }

    /**
     * Uso de una variable: t1 = x
     * @Entrada: nombre, tipo y posición.
     * @Salida: Expr con el temporal que guarda el valor de la variable.
     */
    public static Expr variable(String nombre, String tipo, int linea, int columna) {
        Expr e = new Expr(tipo, linea, columna);
        String t = nuevoTemporal();
        e.setCodigo(Codigo.vacio().agregar(instr(t + " = " + nombre)));
        e.setLugar(t);
        e.setNombre(nombre);
        return e;
    }

    /**
     * Operación binaria: código de a, código de b, t3 = t1 op t2
     * @Entrada: operandos, operador, tipo del resultado y posición.
     * @Salida: Expr con el temporal del resultado.
     */
    public static Expr binaria(Expr a, String op, Expr b, String tipo, int linea, int columna) {
        Expr e = new Expr(tipo, linea, columna);
        String t = nuevoTemporal();
        Codigo c = Codigo.vacio();
        c.agregar(codigoDe(a));
        c.agregar(codigoDe(b));
        c.agregar(instr(t + " = " + lugarDe(a) + " " + operadorTAC(op) + " " + lugarDe(b)));
        e.setCodigo(c);
        e.setLugar(t);
        return e;
    }

    /**
     * Operación unaria: t2 = -t1  o  t4 = Σ t3
     * @Entrada: operador ("-" o "Σ"), operando, tipo y posición.
     * @Salida: Expr con el temporal del resultado.
     */
    public static Expr unaria(String op, Expr a, String tipo, int linea, int columna) {
        Expr e = new Expr(tipo, linea, columna);
        String t = nuevoTemporal();
        Codigo c = Codigo.vacio();
        c.agregar(codigoDe(a));
        if (op.equals("-")) {
            c.agregar(instr(t + " = -" + lugarDe(a)));
        } else {
            c.agregar(instr(t + " = " + op + " " + lugarDe(a)));
        }
        e.setCodigo(c);
        e.setLugar(t);
        return e;
    }

    /**
     * Incremento o decremento posfijo (x++ o x--):
     *   t1 = x
     *   t2 = 1
     *   t3 = t1 + t2
     *   x = t3
     * Como es posfijo, el valor de la expresión es el de antes (t1).
     * @Entrada: nombre de la variable, operador ("++" o "--"), tipo y posición.
     * @Salida: Expr cuyo lugar es el valor anterior de la variable.
     */
    public static Expr incremento(String nombre, String op, String tipo, int linea, int columna) {
        Expr e = new Expr(tipo, linea, columna);
        String anterior = nuevoTemporal();
        String uno = nuevoTemporal();
        String nuevo = nuevoTemporal();
        String operador = op.equals("++") ? "+" : "-";
        Codigo c = Codigo.vacio();
        c.agregar(instr(anterior + " = " + nombre));
        c.agregar(instr(uno + " = 1"));
        c.agregar(instr(nuevo + " = " + anterior + " " + operador + " " + uno));
        c.agregar(instr(nombre + " = " + nuevo));
        e.setCodigo(c);
        e.setLugar(anterior);
        e.setNombre(nombre);
        return e;
    }

    /**
     * Lectura de un arreglo: código del índice, t2 = a[t1]
     * Guarda también el nombre y el índice, por si se usa como destino (a[i] Ͱ x).
     * @Entrada: nombre del arreglo, índice, tipo del elemento y posición.
     * @Salida: Expr con el temporal que guarda el elemento.
     */
    public static Expr acceso(String nombre, Expr indice, String tipo, int linea, int columna) {
        Expr e = new Expr(tipo, linea, columna);
        String t = nuevoTemporal();
        Codigo c = Codigo.vacio();
        c.agregar(codigoDe(indice));
        c.agregar(instr(t + " = " + nombre + "[" + lugarDe(indice) + "]"));
        e.setCodigo(c);
        e.setLugar(t);
        e.setNombre(nombre);
        e.setIndice(indice);
        return e;
    }

    /**
     * Llamada a función: cada argumento se calcula y luego se pasa con param;
     * al final t = call f, n
     * @Entrada: nombre de la función, argumentos en orden, tipo de retorno y posición.
     * @Salida: Expr con el temporal que guarda el valor retornado.
     */
    public static Expr llamada(String nombre, List<Expr> argumentos, String tipo, int linea, int columna) {
        Expr e = new Expr(tipo, linea, columna);
        Codigo c = Codigo.vacio();
        int cantidad = 0;
        if (argumentos != null) {
            for (Expr arg : argumentos) {
                c.agregar(codigoDe(arg));
                c.agregar(instr("param " + lugarDe(arg)));
                cantidad++;
            }
        }
        String t = nuevoTemporal();
        c.agregar(instr(t + " = call " + nombre + ", " + cantidad));
        e.setCodigo(c);
        e.setLugar(t);
        return e;
    }

    // ---------------------------------------------------------------------------------
    // Sentencias
    // ---------------------------------------------------------------------------------

    /**
     * Expresión usada como sentencia (por ejemplo i++ » o una llamada suelta).
     * @Entrada: expresión.
     * @Salida: su código.
     */
    public static Codigo sentenciaExpr(Expr e) {
        return Codigo.vacio().agregar(codigoDe(e));
    }

    /**
     * Asignación: código del valor, x = t
     * @Entrada: nombre del destino y valor.
     * @Salida: código de la asignación.
     */
    public static Codigo asignacion(String nombre, Expr valor) {
        Codigo c = Codigo.vacio();
        c.agregar(codigoDe(valor));
        c.agregar(instr(nombre + " = " + lugarDe(valor)));
        return c;
    }

    /**
     * Asignación a un elemento: código del valor, código del índice, a[ti] = tv
     * @Entrada: destino (con nombre e índice) y valor.
     * @Salida: código de la asignación.
     */
    public static Codigo asignacionArreglo(Expr destino, Expr valor) {
        Codigo c = Codigo.vacio();
        if (destino == null) {
            return c;
        }
        c.agregar(codigoDe(valor));
        c.agregar(codigoDe(destino.getIndice()));
        c.agregar(instr(destino.getNombre() + "[" + lugarDe(destino.getIndice()) + "] = " + lugarDe(valor)));
        return c;
    }

    /**
     * Creación de variable. Con valor es igual a una asignación; sin valor no genera código.
     * @Entrada: símbolo declarado y valor (o null).
     * @Salida: código de la inicialización.
     */
    public static Codigo declaracion(Simbolo simbolo, Expr valor) {
        if (simbolo == null || valor == null) {
            return Codigo.vacio();
        }
        return asignacion(simbolo.getLexema(), valor);
    }

    /**
     * Creación de arreglo con valores: por cada valor k,
     *   código del valor
     *   tk = k
     *   a[tk] = tv
     * Sin valores no genera código.
     * @Entrada: símbolo del arreglo y valores iniciales.
     * @Salida: código de la inicialización.
     */
    public static Codigo creacionArreglo(Simbolo simbolo, List<Expr> valores) {
        Codigo c = Codigo.vacio();
        if (simbolo == null || valores == null) {
            return c;
        }
        for (int k = 0; k < valores.size(); k++) {
            Expr valor = valores.get(k);
            c.agregar(codigoDe(valor));
            String posicion = nuevoTemporal();
            c.agregar(instr(posicion + " = " + k));
            c.agregar(instr(simbolo.getLexema() + "[" + posicion + "] = " + lugarDe(valor)));
        }
        return c;
    }

    /**
     * if:
     * @Entrada: condición y cuerpo del if, ramas elif en orden y cuerpo del else (o null).
     * @Salida: código completo del if.
     */
    public static Codigo si(Expr condicion, Codigo entonces, List<Rama> ramas, Codigo sino) {
        int n = nuevoNumero();
        String fin = "if_end_" + n;
        Codigo c = Codigo.vacio();

        c.agregar(codigoDe(condicion));
        c.agregar(instr("if " + lugarDe(condicion) + " goto if_true_" + n));
        c.agregar(instr("goto if_false_" + n));
        c.agregar(etiqueta("if_true_" + n));
        c.agregar(entonces);
        c.agregar(instr("goto " + fin));
        c.agregar(etiqueta("if_false_" + n));

        if (ramas != null) {
            for (Rama rama : ramas) {
                int m = nuevoNumero();
                c.agregar(codigoDe(rama.getCondicion()));
                c.agregar(instr("if " + lugarDe(rama.getCondicion()) + " goto elif_true_" + m));
                c.agregar(instr("goto elif_false_" + m));
                c.agregar(etiqueta("elif_true_" + m));
                c.agregar(rama.getCuerpo());
                c.agregar(instr("goto " + fin));
                c.agregar(etiqueta("elif_false_" + m));
            }
        }

        c.agregar(sino);
        c.agregar(etiqueta(fin));
        return c;
    }

    /**
     * while:
     * @Entrada: condición y cuerpo.
     * @Salida: código completo del ciclo.
     */
    public static Codigo mientras(Expr condicion, Codigo cuerpo) {
        int n = nuevoNumero();
        String inicio = "while_" + n;
        String fin = "while_end_" + n;
        Codigo c = Codigo.vacio();

        c.agregar(etiqueta(inicio));
        c.agregar(codigoDe(condicion));
        c.agregar(instr("if " + lugarDe(condicion) + " goto while_body_" + n));
        c.agregar(instr("goto " + fin));
        c.agregar(etiqueta("while_body_" + n));
        if (cuerpo != null) {
            cuerpo.reemplazar(instr(MARCA_BREAK), instr("goto " + fin));
            c.agregar(cuerpo);
        }
        c.agregar(instr("goto " + inicio));
        c.agregar(etiqueta(fin));
        return c;
    }

    /**
     * for:
     * @Entrada: inicio, condición, actualización y cuerpo.
     * @Salida: código completo del ciclo.
     */
    public static Codigo para(Codigo inicio, Expr condicion, Codigo actualizacion, Codigo cuerpo) {
        int n = nuevoNumero();
        String etiquetaFor = "for_" + n;
        String fin = "for_end_" + n;
        Codigo c = Codigo.vacio();

        c.agregar(inicio);
        c.agregar(etiqueta(etiquetaFor));
        c.agregar(codigoDe(condicion));
        c.agregar(instr("if " + lugarDe(condicion) + " goto for_body_" + n));
        c.agregar(instr("goto " + fin));
        c.agregar(etiqueta("for_body_" + n));
        if (cuerpo != null) {
            cuerpo.reemplazar(instr(MARCA_BREAK), instr("goto " + fin));
            c.agregar(cuerpo);
        }
        c.agregar(actualizacion);
        c.agregar(instr("goto " + etiquetaFor));
        c.agregar(etiqueta(fin));
        return c;
    }

    /**
     * break: deja la marca; el ciclo más cercano la cambia por el salto a su salida.
     * @Entrada: ninguna.
     * @Salida: código con la marca.
     */
    public static Codigo romper() {
        return Codigo.vacio().agregar(instr(MARCA_BREAK));
    }

    /**
     * return: código del valor, return t
     * @Entrada: valor retornado.
     * @Salida: código del retorno.
     */
    public static Codigo retorno(Expr valor) {
        Codigo c = Codigo.vacio();
        c.agregar(codigoDe(valor));
        c.agregar(instr("return " + lugarDe(valor)));
        return c;
    }

    /**
     * read: se trata como una llamada sin argumentos.
     *   t1 = call read, 0
     *   x = t1
     * @Entrada: nombre de la variable.
     * @Salida: código de la lectura.
     */
    public static Codigo leer(String nombre) {
        String t = nuevoTemporal();
        Codigo c = Codigo.vacio();
        c.agregar(instr(t + " = call read, 0"));
        c.agregar(instr(nombre + " = " + t));
        return c;
    }

    /**
     * write: se trata como una llamada con un argumento.
     *   código del valor
     *   param t
     *   call write, 1
     * @Entrada: valor a escribir.
     * @Salida: código de la escritura.
     */
    public static Codigo escribir(Expr valor) {
        Codigo c = Codigo.vacio();
        c.agregar(codigoDe(valor));
        c.agregar(instr("param " + lugarDe(valor)));
        c.agregar(instr("call write, 1"));
        return c;
    }

    /**
     * Función:
     *   int_suma:
     *       param_int a
     *       cuerpo
     *   int_suma_end
     * @Entrada: tipo de retorno, nombre, parámetros y cuerpo.
     * @Salida: código completo de la función.
     */
    public static Codigo funcion(String tipo, String nombre, List<Simbolo> parametros, Codigo cuerpo) {
        String base = tipo + "_" + nombre;
        Codigo c = Codigo.vacio();
        c.agregar(etiqueta(base));
        if (parametros != null) {
            for (Simbolo p : parametros) {
                c.agregar(instr("param_" + p.getTipo() + " " + p.getLexema()));
            }
        }
        c.agregar(cuerpo);
        c.agregar(base + "_end");
        return c;
    }

    /**
     * Función principal: void_principal: ... void_principal_end
     * @Entrada: cuerpo.
     * @Salida: código completo de principal.
     */
    public static Codigo principal(Codigo cuerpo) {
        Codigo c = Codigo.vacio();
        c.agregar(etiqueta("void_principal"));
        c.agregar(cuerpo);
        c.agregar("void_principal_end");
        return c;
    }

    /**
     * Programa completo, en el orden del código fuente.
     * @Entrada: código de lo que va antes de principal, de principal y de lo que va después.
     * @Salida: código de todo el programa.
     */
    public static Codigo programa(Codigo antes, Codigo principal, Codigo despues) {
        return Codigo.vacio().agregar(antes).agregar(principal).agregar(despues);
    }

    /**
     * Escribe el TAC en el archivo, una línea por instrucción (las líneas ya traen su formato).
     * @Entrada: ruta del archivo y código del programa.
     * @Salida: archivo UTF-8.
     */
    public static void escribirArchivo(Path ruta, Codigo programa) throws IOException {
        if (ruta.getParent() != null) {
            Files.createDirectories(ruta.getParent());
        }
        try (BufferedWriter w = Files.newBufferedWriter(ruta, StandardCharsets.UTF_8)) {
            if (programa == null) {
                return;
            }
            for (String linea : programa.getLineas()) {
                w.write(linea);
                w.newLine();
            }
        }
    }
}
