package compilador.simbolos;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * Construye las tablas de símbolos desde las acciones del parser (Parser.cup), de forma
 * procedural, y genera el reporte de tablas.
 *
 *
 * @Entrada: llamadas desde las acciones del parser (declarar, cerrarBloque, nombrar,
 *           cerrarFor, cerrarFuncion, cerrarPrincipal, cerrarGlobal) con las posiciones
 *           (left = línea, right = columna) de los tokens.
 * @Salida: árbol de tablas de símbolos con raíz en la tabla global; resolución de cada ID a su
 *          tabla (resolver); archivo de reporte <nombre>_simbolos.txt (escribirReporte).
 * @Restricciones:
 * - Estado estático: llamar reiniciar() antes de analizar cada archivo.
 * - Si el parse no termina (error no recuperable), cerrarGlobal() mete en la global todo lo que
 *   quedó pendiente y el reporte indica que las tablas son parciales.
 * @Objetivo: Implementar la tabla de símbolos.
 */
public final class GestorTablas {

    private static final Deque<Object> pendientes = new ArrayDeque<>();
    private static TablaSimbolos global = null;
    private static boolean completa = false;

    private static final String FORMATO_FILA = "%-24s %-11s %-8s %-6s %-6s %s";

    private GestorTablas() { }

    /**
     * Deja el gestor listo para analizar un archivo nuevo.
     * @Entrada: Ninguna.
     * @Salida: pila de pendientes vacía y sin tabla global.
     */
    public static void reiniciar() {
        pendientes.clear();
        global = null;
        completa = false;
    }

    /**
     * Registra una declaración (variable, arreglo, parámetro o función) como pendiente.
     * @Entrada: símbolo creado en la acción de la producción de declaración.
     * @Salida: el símbolo queda en la pila hasta que se cierre su alcance.
     */
    public static void declarar(Simbolo s) {
        if (s != null) {
            pendientes.push(s);
        }
    }

    /**
     * Cierra un bloque "¿: ... :?" y arma su tabla.
     * @Entrada: línea/columna de "¿:" y de ":?".
     * @Salida: tabla del bloque (con nombre provisional "bloque"); queda pendiente para el
     *          alcance que la contiene.
     */
    public static TablaSimbolos cerrarBloque(int lineaAbre, int colAbre, int lineaCierra, int colCierra) {
        long inicio = TablaSimbolos.posicion(lineaAbre, colAbre);
        TablaSimbolos t = new TablaSimbolos("bloque", inicio, TablaSimbolos.posicion(lineaCierra, colCierra));
        t.agregarContenido(sacarDesde(inicio));
        pendientes.push(t);
        return t;
    }

    /**
     * Le pone nombre a la tabla de un bloque según la estructura que lo usa.
     * @Entrada: tabla devuelta por el bloque, palabra de la estructura ("if", "elif", "else",
     *           "while") y posición de esa palabra reservada.
     * @Salida: la tabla queda nombrada, por ejemplo "while (L28:C5)".
     */
    public static void nombrar(TablaSimbolos t, String estructura, int linea, int columna) {
        if (t != null) {
            t.setNombre(estructura + " (L" + linea + ":C" + columna + ")");
        }
    }

    /**
     * Cierra el alcance de un for: su tabla incluye la variable declarada en el encabezado.
     * @Entrada: tabla del bloque del for y posición de la palabra "for".
     * @Salida: la tabla se nombra "for (L..)", su rango empieza en "for" y absorbe los
     *          pendientes del encabezado.
     */
    public static void cerrarFor(TablaSimbolos t, int linea, int columna) {
        extender(t, linea, columna);
        nombrar(t, "for", linea, columna);
    }

    /**
     * Cierra una función: parámetros y cuerpo forman un solo alcance, y la función se declara
     * en el alcance global.
     * @Entrada: tabla del bloque del cuerpo, nombre y posición del ID de la función, tipo de
     *           retorno, lista de parámetros (Simbolo) y posición de "є:".
     * @Salida: tabla "función <nombre>" con los parámetros al inicio; símbolo FUNCION pendiente.
     */
    public static void cerrarFuncion(TablaSimbolos t, String nombre, int linea, int columna,
                                     String tipoRetorno, List<Simbolo> parametros,
                                     int lineaParen, int colParen) {
        extender(t, lineaParen, colParen);
        if (t != null) {
            t.setNombre("función " + nombre);
        }
        Simbolo f = new Simbolo(nombre, tipoRetorno, linea, columna);
        f.setCategoria(Simbolo.Categoria.FUNCION);
        f.setParametros(parametros != null ? parametros : new ArrayList<>());
        declarar(f);
    }

    /**
     * Cierra la función principal.
     * @Entrada: tabla del bloque, posición de "principal" y de "є:".
     * @Salida: tabla "principal"; símbolo PRINCIPAL (void, sin parámetros) pendiente.
     */
    public static void cerrarPrincipal(TablaSimbolos t, int linea, int columna, int lineaParen, int colParen) {
        extender(t, lineaParen, colParen);
        if (t != null) {
            t.setNombre("principal");
        }
        Simbolo p = new Simbolo("principal", "void", linea, columna);
        p.setCategoria(Simbolo.Categoria.PRINCIPAL);
        declarar(p);
    }

    /**
     * Arma la tabla global con todo lo que queda pendiente. Se llama desde la acción de
     * "programa" (parse completo) y desde Main después del parse (por si no terminó).
     * @Entrada: Ninguna.
     * @Salida: tabla global (raíz del árbol); llamadas repetidas devuelven la misma tabla.
     */
    public static TablaSimbolos cerrarGlobal() {
        if (global == null) {
            completa = true;
            global = new TablaSimbolos("global", 0, TablaSimbolos.FIN_ARCHIVO);
            global.agregarContenido(sacarDesde(Long.MIN_VALUE));
        }
        return global;
    }

    /**
     * Versión usada por Main: si el parser no llegó a "programa", arma la global marcándola
     * como parcial.
     * @Entrada: Ninguna.
     * @Salida: tabla global.
     */
    public static TablaSimbolos finalizar() {
        if (global == null) {
            cerrarGlobal();
            completa = false;
        }
        return global;
    }

    public static TablaSimbolos getGlobal() { return global; }
    public static boolean isCompleta() { return completa; }

    /**
     * Extiende el inicio de la tabla que está en el tope de la pila hasta una posición anterior
     * y absorbe los pendientes que están entre esa posición y el bloque (parámetros o variable
     * del encabezado del for).
     * @Entrada: tabla del bloque (debe ser el tope de la pila) y nueva posición de inicio.
     * @Salida: tabla extendida y devuelta al tope de la pila.
     */
    private static void extender(TablaSimbolos t, int linea, int columna) {
        if (t == null || pendientes.peek() != t) {
            return;
        }
        pendientes.pop();
        long inicio = TablaSimbolos.posicion(linea, columna);
        t.anteponer(sacarDesde(inicio));
        t.setInicio(inicio);
        pendientes.push(t);
    }

    /**
     * Saca de la pila los pendientes cuya posición es >= desde.
     * @Entrada: posición codificada.
     * @Salida: lista de pendientes en orden del fuente.
     */
    private static List<Object> sacarDesde(long desde) {
        List<Object> sacados = new ArrayList<>();
        while (!pendientes.isEmpty() && posicionDe(pendientes.peek()) >= desde) {
            sacados.add(pendientes.pop());
        }
        Collections.reverse(sacados);
        return sacados;
    }

    private static long posicionDe(Object o) {
        if (o instanceof Simbolo) {
            return ((Simbolo) o).getPosicion();
        }
        return ((TablaSimbolos) o).getInicio();
    }

    /**
     * Determina en qué tabla va un token ID y qué información le corresponde.
     * @Entrada: lexema del ID, línea y columna del token.
     * @Salida: arreglo {tabla, info}. La tabla es el alcance más interno donde aparece el token;
     *          la info dice si es declaración o uso (y a qué declaración se refiere).
     */
    public static String[] resolver(String lexema, int linea, int columna) {
        if (global == null) {
            return new String[]{"Identificadores", ""};
        }
        long pos = TablaSimbolos.posicion(linea, columna);
        TablaSimbolos t = global.tablaMasInterna(pos);
        if (t == null) {
            t = global;
        }
        Simbolo decl = t.declaradoEn(pos);
        if (decl == null && t != global) {
            decl = global.declaradoEn(pos);
            if (decl != null) {
                t = global;
            }
        }
        if (decl != null) {
            return new String[]{t.getNombre(), "declaración · " + decl.getDescripcionCorta()
                    + (decl.isRedeclarada() ? " · REDECLARADA" : "")};
        }
        Simbolo s = t.buscar(lexema);
        if (s == null) {
            return new String[]{t.getNombre(), "uso sin declaración visible"};
        }
        String info = "uso → " + s.getDescripcionCorta() + " de '" + s.getAlcance().getNombre()
                + "' (L" + s.getLinea() + ":C" + s.getColumna() + ")";
        if (s.getPosicion() > pos && s.getCategoria() != Simbolo.Categoria.FUNCION) {
            info += " · declarada después del uso";
        }
        return new String[]{t.getNombre(), info};
    }

    /**
     * Escribe el archivo con todas las tablas de símbolos.
     * @Entrada: ruta del archivo, filas de la tabla de constantes y de reservadas
     *           (cada fila: {lexema, tipo/token, primera posición, apariciones}).
     * @Salida: archivo con el árbol de tablas (indentado por nivel), constantes y reservadas.
     */
    public static void escribirReporte(Path ruta, List<String[]> constantes, List<String[]> reservadas)
            throws IOException {
        Path carpeta = ruta.getParent();
        if (carpeta != null) {
            Files.createDirectories(carpeta);
        }
        try (BufferedWriter w = Files.newBufferedWriter(ruta, StandardCharsets.UTF_8)) {
            w.write("==================== TABLAS DE SÍMBOLOS ====================");
            w.newLine();
            if (!completa) {
                w.write("NOTA: el análisis sintáctico no terminó; las tablas son parciales.");
                w.newLine();
            }
            w.newLine();
            if (global != null) {
                escribirTabla(w, global);
            }

            w.write("==================== TABLA DE CONSTANTES ====================");
            w.newLine();
            escribirFilas(w, String.format("%-30s %-10s %-14s %s", "Valor", "Tipo", "Primera vez", "Apariciones"),
                    constantes, "%-30s %-10s %-14s %s");

            w.write("==================== TABLA DE PALABRAS RESERVADAS ====================");
            w.newLine();
            escribirFilas(w, String.format("%-30s %-10s %-14s %s", "Lexema", "Token", "Primera vez", "Apariciones"),
                    reservadas, "%-30s %-10s %-14s %s");
        }
    }

    /**
     * Escribe una tabla y, recursivamente, sus hijas con más indentación.
     * @Entrada: escritor y tabla.
     * @Salida: encabezado de la tabla (nombre, nivel, padre, rango) y una fila por símbolo.
     */
    private static void escribirTabla(BufferedWriter w, TablaSimbolos t) throws IOException {
        String sangria = "    ".repeat(t.getNivel());
        String padre = t.getPadre() == null ? "—" : t.getPadre().getNombre();
        String rango = t.getPadre() == null ? "todo el archivo"
                : TablaSimbolos.textoPosicion(t.getInicio()) + " – " + TablaSimbolos.textoPosicion(t.getFin());
        w.write(sangria + "TABLA: " + t.getNombre() + "   (nivel " + t.getNivel() + ", padre: " + padre
                + ", rango: " + rango + ")");
        w.newLine();
        if (t.getSimbolos().isEmpty()) {
            w.write(sangria + "  (sin declaraciones)");
            w.newLine();
        } else {
            w.write(sangria + "  " + String.format(FORMATO_FILA, "Lexema", "Categoría", "Tipo", "Línea", "Col", "Información"));
            w.newLine();
            for (Simbolo s : t.getSimbolos()) {
                w.write(sangria + "  " + String.format(FORMATO_FILA, s.getLexema(), s.getCategoria(), s.getTipo(),
                        s.getLinea(), s.getColumna(), s.getInformacion()));
                w.newLine();
            }
        }
        w.newLine();
        for (TablaSimbolos h : t.getHijos()) {
            escribirTabla(w, h);
        }
    }

    private static void escribirFilas(BufferedWriter w, String encabezado, List<String[]> filas, String formato)
            throws IOException {
        if (filas.isEmpty()) {
            w.write("  (vacía)");
            w.newLine();
            w.newLine();
            return;
        }
        w.write("  " + encabezado);
        w.newLine();
        for (String[] f : filas) {
            w.write("  " + String.format(formato, (Object[]) f));
            w.newLine();
        }
        w.newLine();
    }
}
