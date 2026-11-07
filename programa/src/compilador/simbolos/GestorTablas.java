package compilador.simbolos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Arma el árbol de tablas de símbolos a partir de las acciones de Parser.cup.
 *
 * @Entrada: llamadas desde las acciones del parser con las posiciones de los tokens.
 * @Salida: la tabla global, que es la raíz del árbol de tablas.
 * @Restricciones: hay que llamar reiniciar() antes de analizar cada archivo.
 * @Objetivo: construir las tablas de símbolos por alcance.
 */
public class GestorTablas {

    private static List<Simbolo> declaraciones = new ArrayList<>();
    private static List<TablaSimbolos> alcances = new ArrayList<>();
    private static TablaSimbolos global = null;
    private static boolean completa = false;

    /**
     * Deja todo vacío para analizar un archivo nuevo.
     * @Entrada: ninguna.
     * @Salida: listas vacías y sin tabla global.
     */
    public static void reiniciar() {
        declaraciones.clear();
        alcances.clear();
        global = null;
        completa = false;
    }

    /**
     * Anota una declaración (variable, arreglo, parámetro o función).
     * @Entrada: el símbolo declarado.
     * @Salida: el símbolo queda en la lista de declaraciones.
     */
    public static void declarar(Simbolo s) {
        if (s != null) {
            declaraciones.add(s);
        }
    }

    /**
     * Anota un bloque ¿: ... :? como un alcance nuevo.
     * @Entrada: línea y columna de ¿: y de :?.
     * @Salida: la tabla del bloque, con nombre "bloque" hasta que se sepa de qué estructura es.
     */
    public static TablaSimbolos cerrarBloque(int lineaAbre, int colAbre, int lineaCierra, int colCierra) {
        long inicio = TablaSimbolos.posicion(lineaAbre, colAbre);
        long fin = TablaSimbolos.posicion(lineaCierra, colCierra);
        TablaSimbolos tabla = new TablaSimbolos("bloque", inicio, fin);
        alcances.add(tabla);
        return tabla;
    }

    /**
     * Le pone nombre y clase a la tabla de un if, elif, else o while.
     * @Entrada: tabla del bloque, palabra de la estructura y su posición.
     * @Salida: la tabla queda con nombre, por ejemplo "while línea 23".
     */
    public static void nombrar(TablaSimbolos tabla, String estructura, int linea, int columna) {
        tabla.setNombre(estructura + " línea " + linea);
        tabla.setClase(TablaSimbolos.Clase.valueOf(estructura.toUpperCase()));
    }

    /**
     * Cierra un for. Su alcance empieza en la palabra for, para que la variable del
     * encabezado (for є: val int i ...) quede dentro del for.
     * @Entrada: tabla del bloque del for y posición de la palabra for.
     * @Salida: la tabla queda como alcance del for.
     */
    public static void cerrarFor(TablaSimbolos tabla, int linea, int columna) {
        tabla.setInicio(TablaSimbolos.posicion(linea, columna));
        nombrar(tabla, "for", linea, columna);
    }

    /**
     * Cierra una función. Su alcance empieza en є: para que los parámetros queden en la
     * misma tabla que el cuerpo. La función se declara con su firma.
     * @Entrada: tabla del cuerpo, nombre, posición del nombre, tipo de retorno, parámetros y
     *           posición de є:.
     * @Salida: la tabla queda como "función <nombre>" y se anota el símbolo de la función.
     */
    public static void cerrarFuncion(TablaSimbolos tabla, String nombre, int linea, int columna,
                                     String tipoRetorno, List<Simbolo> parametros,
                                     int lineaParen, int colParen) {
        tabla.setInicio(TablaSimbolos.posicion(lineaParen, colParen));
        tabla.setNombre("funcion " + nombre);
        tabla.setClase(TablaSimbolos.Clase.FUNCION);

        Simbolo funcion = new Simbolo(nombre, tipoRetorno, linea, columna);
        funcion.setCategoria(Simbolo.Categoria.FUNCION);
        funcion.setParametros(parametros);
        declarar(funcion);
    }

    /**
     * Cierra la función principal.
     * @Entrada: tabla del cuerpo, posición de "principal" y de є:.
     * @Salida: la tabla queda como "principal" y se anota su símbolo (void, sin parámetros).
     */
    public static void cerrarPrincipal(TablaSimbolos tabla, int linea, int columna, int lineaParen, int colParen) {
        tabla.setInicio(TablaSimbolos.posicion(lineaParen, colParen));
        tabla.setNombre("principal");
        tabla.setClase(TablaSimbolos.Clase.PRINCIPAL);

        Simbolo principal = new Simbolo("principal", "void", linea, columna);
        principal.setCategoria(Simbolo.Categoria.PRINCIPAL);
        declarar(principal);
    }

    /**
     * Arma el árbol de tablas con lo anotado. Se llama al terminar "programa".
     * @Entrada: ninguna.
     * @Salida: la tabla global; si ya estaba armada devuelve la misma.
     */
    public static TablaSimbolos cerrarGlobal() {
        if (global != null) {
            return global;
        }
        completa = true;
        global = new TablaSimbolos("global", 0, TablaSimbolos.FIN_ARCHIVO);
        global.setClase(TablaSimbolos.Clase.GLOBAL);

        alcances.sort(Comparator.comparingLong(TablaSimbolos::getInicio));
        for (TablaSimbolos tabla : alcances) {
            global.tablaMasInterna(tabla.getInicio()).agregarHija(tabla);
        }

        declaraciones.sort(Comparator.comparingLong(Simbolo::getPosicion));
        for (Simbolo s : declaraciones) {
            global.tablaMasInterna(s.getPosicion()).agregar(s);
        }
        return global;
    }

    /**
     * La usa Main después del parse. Si hubo un error que no dejó llegar a "programa",
     * arma el árbol con lo que se alcanzó a anotar y lo marca como incompleto.
     * @Entrada: ninguna.
     * @Salida: la tabla global.
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
}
