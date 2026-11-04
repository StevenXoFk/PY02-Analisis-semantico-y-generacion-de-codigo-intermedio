package compilador.simbolos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tabla de símbolos de un alcance (global, función, if, elif, else, while o for).
 * @Entrada: nombre del alcance y su rango en el fuente (desde/hasta línea:columna); luego
 *           los símbolos y tablas hijas que GestorTablas le va agregando.
 * @Salida: Estructura consultable con buscarLocal(), buscar() y tablaMasInterna().
 * @Restricciones:
 * - Los símbolos se guardan en orden de declaración.
 * - El nivel se calcula a partir del padre (global = 0).
 * @Objetivo: Implementar la tabla de símbolos por alcance.
 */
public class TablaSimbolos {
    public static final long FIN_ARCHIVO = Long.MAX_VALUE;

    private String nombre;
    private TablaSimbolos padre;
    private final List<TablaSimbolos> hijos = new ArrayList<>();
    private final List<Simbolo> simbolos = new ArrayList<>();
    private final Map<String, Simbolo> primeros = new LinkedHashMap<>();
    private long inicio;
    private long fin;

    /**
     * Crea una tabla vacía sin padre.
     * @Entrada: nombre del alcance, posición de inicio y posición de fin (ver posicion()).
     * @Salida: Tabla nueva.
     */
    public TablaSimbolos(String nombre, long inicio, long fin) {
        this.nombre = nombre;
        this.inicio = inicio;
        this.fin = fin;
    }

    /**
     * Codifica una posición del fuente en un número comparable.
     * @Entrada: línea y columna (base 1).
     * @Salida: línea * 100000 + columna.
     * @Restricciones: supone líneas de menos de 100000 columnas.
     */
    public static long posicion(int linea, int columna) {
        return (long) linea * 100000L + columna;
    }

    /**
     * Convierte una posición codificada a texto "L<linea>:C<columna>".
     * @Entrada: posición codificada.
     * @Salida: texto legible, o "fin del archivo".
     */
    public static String textoPosicion(long pos) {
        if (pos == FIN_ARCHIVO) {
            return "fin del archivo";
        }
        return "L" + (pos / 100000L) + ":C" + (pos % 100000L);
    }

    /**
     * Agrega un símbolo al final de la tabla.
     * @Entrada: símbolo declarado en este alcance.
     * @Salida: el símbolo queda en la tabla; si el nombre ya existía en ESTA tabla se marca
     *          como redeclarado (se conservan ambas entradas).
     */
    public void agregar(Simbolo s) {
        Simbolo previo = primeros.get(s.getLexema());
        if (previo != null) {
            s.marcarRedeclarada(previo);
        } else {
            primeros.put(s.getLexema(), s);
        }
        s.setAlcance(this);
        simbolos.add(s);
    }

    /**
     * Agrega una tabla hija (alcance anidado) y la enlaza con esta como padre.
     * @Entrada: tabla del alcance anidado.
     * @Salida: la hija queda en la lista de hijos y su padre es esta tabla.
     */
    public void agregarHija(TablaSimbolos hija) {
        hija.padre = this;
        hijos.add(hija);
    }

    /**
     * Inserta, antes del contenido actual, símbolos y tablas que aparecen antes en el fuente
     * (parámetros de una función o la variable del encabezado de un for).
     * @Entrada: lista en orden del fuente con objetos Simbolo o TablaSimbolos.
     * @Salida: la tabla queda con el prefijo seguido de su contenido anterior, recalculando
     *          las marcas de redeclaración.
     */
    void anteponer(List<Object> prefijo) {
        if (prefijo.isEmpty()) {
            return;
        }
        List<Simbolo> viejosSimbolos = new ArrayList<>(simbolos);
        List<TablaSimbolos> viejosHijos = new ArrayList<>(hijos);
        simbolos.clear();
        hijos.clear();
        primeros.clear();
        agregarContenido(prefijo);
        for (Simbolo s : viejosSimbolos) {
            agregar(s);
        }
        for (TablaSimbolos h : viejosHijos) {
            agregarHija(h);
        }
    }

    /**
     * Agrega a la tabla una lista mixta de símbolos y tablas hijas.
     * @Entrada: lista en orden del fuente con objetos Simbolo o TablaSimbolos.
     * @Salida: cada Simbolo se agrega como símbolo y cada TablaSimbolos como hija.
     */
    void agregarContenido(List<Object> contenido) {
        for (Object o : contenido) {
            if (o instanceof Simbolo) {
                agregar((Simbolo) o);
            } else if (o instanceof TablaSimbolos) {
                agregarHija((TablaSimbolos) o);
            }
        }
    }

    /**
     * Busca un nombre solo en esta tabla.
     * @Entrada: lexema.
     * @Salida: su primera declaración en este alcance, o null.
     */
    public Simbolo buscarLocal(String lexema) {
        return primeros.get(lexema);
    }

    /**
     * Busca un nombre en esta tabla y luego en los padres (bloque anidado más cercano).
     * @Entrada: lexema.
     * @Salida: la declaración visible más cercana, o null si no hay ninguna.
     */
    public Simbolo buscar(String lexema) {
        for (TablaSimbolos t = this; t != null; t = t.padre) {
            Simbolo s = t.buscarLocal(lexema);
            if (s != null) {
                return s;
            }
        }
        return null;
    }

    /**
     * Busca el símbolo declarado exactamente en una posición dentro de esta tabla.
     * @Entrada: posición codificada del token ID.
     * @Salida: el símbolo cuya declaración está en esa posición, o null.
     */
    public Simbolo declaradoEn(long pos) {
        for (Simbolo s : simbolos) {
            if (s.getPosicion() == pos) {
                return s;
            }
        }
        return null;
    }

    /**
     * Indica si una posición cae dentro del rango de esta tabla.
     * @Entrada: posición codificada.
     * @Salida: true si inicio <= pos <= fin.
     */
    public boolean contiene(long pos) {
        return pos >= inicio && pos <= fin;
    }

    /**
     * Encuentra la tabla más anidada (esta o una descendiente) que contiene la posición.
     * @Entrada: posición codificada de un token.
     * @Salida: la tabla más interna que la contiene, o null si ni esta tabla la contiene.
     */
    public TablaSimbolos tablaMasInterna(long pos) {
        if (!contiene(pos)) {
            return null;
        }
        for (TablaSimbolos h : hijos) {
            TablaSimbolos t = h.tablaMasInterna(pos);
            if (t != null) {
                return t;
            }
        }
        return this;
    }

    /**
     * Nivel de anidamiento.
     * @Entrada: Ninguna.
     * @Salida: 0 para la global, 1 para sus hijas, etc.
     */
    public int getNivel() {
        int nivel = 0;
        for (TablaSimbolos t = padre; t != null; t = t.padre) {
            nivel++;
        }
        return nivel;
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public TablaSimbolos getPadre() { return padre; }
    public List<TablaSimbolos> getHijos() { return Collections.unmodifiableList(hijos); }
    public List<Simbolo> getSimbolos() { return Collections.unmodifiableList(simbolos); }
    public long getInicio() { return inicio; }
    public long getFin() { return fin; }
    void setInicio(long inicio) { this.inicio = inicio; }
}
