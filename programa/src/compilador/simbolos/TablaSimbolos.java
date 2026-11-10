package compilador.simbolos;

import java.util.ArrayList;
import java.util.List;

/**
 * Tabla de símbolos de un alcance. 
 * @Entrada: nombre del alcance y su rango en el código fuente (inicio y fin).
 * @Salida: la tabla con sus símbolos y sus tablas hijas.
 * @Restricciones: las posiciones se guardan como línea * 100000 + columna (ver posicion()).
 * @Objetivo: guardar los identificadores de cada alcance y permitir buscarlos.
 */
public class TablaSimbolos {

    public enum Clase { GLOBAL, FUNCION, PRINCIPAL, IF, ELIF, ELSE, WHILE, FOR, BLOQUE }
    public static final long FIN_ARCHIVO = Long.MAX_VALUE;

    private String nombre;
    private Clase clase = Clase.BLOQUE;
    private long inicio;
    private long fin;
    private TablaSimbolos padre = null;
    private List<TablaSimbolos> hijos = new ArrayList<>();
    private List<Simbolo> simbolos = new ArrayList<>();

    public TablaSimbolos(String nombre, long inicio, long fin) {
        this.nombre = nombre;
        this.inicio = inicio;
        this.fin = fin;
    }

    /**
     * Convierte línea y columna en un solo número para comparar posiciones fácilmente.
     * @Entrada: línea y columna.
     * @Salida: línea * 100000 + columna.
     * @Restricciones: supone líneas de menos de 100000 caracteres.
     */
    public static long posicion(int linea, int columna) {
        return linea * 100000L + columna;
    }

    /**
     * Agrega un símbolo a esta tabla. Si ya había uno con el mismo nombre en esta misma tabla,
     * el nuevo se marca como repetido.
     * @Entrada: símbolo declarado en este alcance.
     * @Salida: el símbolo queda guardado en la tabla.
     */
    public void agregar(Simbolo s) {
        Simbolo anterior = buscarLocal(s.getLexema());
        if (anterior != null) {
            s.marcarRedeclarada(anterior);
        }
        s.setAlcance(this);
        simbolos.add(s);
    }

    /**
     * Agrega una tabla hija (un alcance que está dentro de este).
     * @Entrada: tabla hija.
     * @Salida: la hija queda guardada y esta tabla queda como su padre.
     */
    public void agregarHija(TablaSimbolos hija) {
        hija.padre = this;
        hijos.add(hija);
    }

    /**
     * Busca un nombre solo en esta tabla.
     * @Entrada: nombre a buscar.
     * @Salida: la primera declaración con ese nombre, o null si no está.
     */
    public Simbolo buscarLocal(String lexema) {
        for (Simbolo s : simbolos) {
            if (s.getLexema().equals(lexema)) {
                return s;
            }
        }
        return null;
    }

    /**
     * Busca un nombre desde esta tabla hacia afuera, pero solo acepta declaraciones que estén
     * antes de la posición donde se usa el nombre. Las funciones y principal se aceptan
     * siempre, porque se pueden llamar aunque estén declaradas más abajo.
     * @Entrada: nombre y posición donde se usa.
     * @Salida: la declaración que se ve desde esa posición, o null si no hay.
     */
    public Simbolo buscarAntesDe(String lexema, long pos) {
        TablaSimbolos tabla = this;
        while (tabla != null) {
            for (Simbolo s : tabla.simbolos) {
                if (s.getLexema().equals(lexema)) {
                    boolean esFuncion = s.getCategoria() == Simbolo.Categoria.FUNCION
                            || s.getCategoria() == Simbolo.Categoria.PRINCIPAL;
                    if (esFuncion || s.getPosicion() < pos) {
                        return s;
                    }
                }
            }
            tabla = tabla.padre;
        }
        return null;
    }

    /**
     * Sube por los padres hasta encontrar una tabla de alguna de las clases dadas.
     * Sirve para saber si un break está dentro de un ciclo o a qué función pertenece un return.
     * @Entrada: una o varias clases (por ejemplo WHILE, FOR).
     * @Salida: la primera tabla que cumpla, o null.
     */
    public TablaSimbolos ancestroDeClase(Clase... clases) {
        TablaSimbolos tabla = this;
        while (tabla != null) {
            for (Clase c : clases) {
                if (tabla.clase == c) {
                    return tabla;
                }
            }
            tabla = tabla.padre;
        }
        return null;
    }

    /**
     * Indica si una posición está dentro del rango de esta tabla.
     * @Entrada: posición.
     * @Salida: true si inicio <= pos <= fin.
     */
    public boolean contiene(long pos) {
        return pos >= inicio && pos <= fin;
    }

    /**
     * Busca la tabla más interna (esta o alguna hija) que contiene una posición.
     * @Entrada: posición.
     * @Salida: la tabla más anidada que la contiene, o null si esta tabla no la contiene.
     */
    public TablaSimbolos tablaMasInterna(long pos) {
        if (!contiene(pos)) {
            return null;
        }
        for (TablaSimbolos hija : hijos) {
            TablaSimbolos encontrada = hija.tablaMasInterna(pos);
            if (encontrada != null) {
                return encontrada;
            }
        }
        return this;
    }

    /**
     * Nivel de anidamiento de la tabla.
     * @Entrada: ninguna.
     * @Salida: 0 para la global, 1 para sus hijas y así sucesivamente.
     */
    public int getNivel() {
        int nivel = 0;
        TablaSimbolos tabla = padre;
        while (tabla != null) {
            nivel++;
            tabla = tabla.padre;
        }
        return nivel;
    }

    public String getNombre() { return nombre; }
    public Clase getClase() { return clase; }
    public long getInicio() { return inicio; }
    public long getFin() { return fin; }
    public TablaSimbolos getPadre() { return padre; }
    public List<TablaSimbolos> getHijos() { return hijos; }
    public List<Simbolo> getSimbolos() { return simbolos; }

    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setClase(Clase clase) { this.clase = clase; }
    public void setInicio(long inicio) { this.inicio = inicio; }
}
