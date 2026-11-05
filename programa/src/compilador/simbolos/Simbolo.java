package compilador.simbolos;

import java.util.ArrayList;
import java.util.List;

/**
 * Entrada de una tabla de símbolos: guarda la información de un identificador declarado.
 * @Entrada: lexema, tipo y posición (línea, columna) del identificador en su declaración.
 *           La categoría, la dimensión, los parámetros y la inicialización se completan
 *           desde las acciones del parser según la producción que lo declaró.
 * @Salida: Objeto con la información del identificador, consultable por la tabla de símbolos
 *          y por los reportes (archivo de tokens y archivo de tablas de símbolos).
 * @Restricciones:
 * - La posición es la del token ID de la declaración (left = línea, right = columna del lexer).
 * - La dimensión solo aplica a ARREGLO; los parámetros y el tipo de retorno solo a FUNCION/PRINCIPAL.
 * @Objetivo: Representar la información que se almacena por cada identificador en la tabla de
 *            símbolos.
 */
public class Simbolo {

    /** Categoría del identificador según cómo fue declarado. */
    public enum Categoria {
        VARIABLE("variable"),
        ARREGLO("arreglo"),
        PARAMETRO("parámetro"),
        FUNCION("función"),
        PRINCIPAL("principal");

        private final String texto;

        Categoria(String texto) {
            this.texto = texto;
        }

        @Override
        public String toString() {
            return texto;
        }
    }

    private final String lexema;
    private final String tipo;
    private final int linea;
    private final int columna;
    private Categoria categoria = Categoria.VARIABLE;
    private boolean inicializada = false;
    private int dimension = -1;
    private List<Simbolo> parametros = new ArrayList<>();
    private boolean redeclarada = false;
    private Simbolo primeraDeclaracion = null;
    private TablaSimbolos alcance = null;

    /**
     * Crea un símbolo con categoría VARIABLE por defecto.
     * @Entrada: lexema del ID, tipo ("int", "float", ...), línea y columna del ID.
     * @Salida: Símbolo nuevo, todavía sin tabla asignada.
     */
    public Simbolo(String lexema, String tipo, int linea, int columna) {
        this.lexema = lexema;
        this.tipo = tipo;
        this.linea = linea;
        this.columna = columna;
    }

    public String getLexema() { return lexema; }
    public String getTipo() { return tipo; }
    public int getLinea() { return linea; }
    public int getColumna() { return columna; }
    public Categoria getCategoria() { return categoria; }
    public boolean isInicializada() { return inicializada; }
    public int getDimension() { return dimension; }
    public List<Simbolo> getParametros() { return parametros; }
    public boolean isRedeclarada() { return redeclarada; }
    public Simbolo getPrimeraDeclaracion() { return primeraDeclaracion; }
    public TablaSimbolos getAlcance() { return alcance; }

    public void setCategoria(Categoria categoria) { this.categoria = categoria; }
    public void setInicializada(boolean inicializada) { this.inicializada = inicializada; }
    public void setDimension(int dimension) { this.dimension = dimension; }
    public void setParametros(List<Simbolo> parametros) { this.parametros = parametros; }
    void setAlcance(TablaSimbolos alcance) { this.alcance = alcance; }

    /**
     * Marca el símbolo como redeclarado en su mismo alcance.
     * @Entrada: la primera declaración del mismo nombre en esa tabla.
     * @Salida: el símbolo queda marcado y apunta a la primera declaración.
     */
    void marcarRedeclarada(Simbolo primera) {
        this.redeclarada = true;
        this.primeraDeclaracion = primera;
    }

    /**
     * Posición del símbolo codificada en un solo número para compararla con otras posiciones.
     * @Entrada: Ninguna.
     * @Salida: línea * 100000 + columna.
     */
    public long getPosicion() {
        return TablaSimbolos.posicion(linea, columna);
    }

    /**
     * Construye la columna "Información" del reporte de tablas.
     * @Entrada: Ninguna.
     * @Salida: Texto con la información propia de la categoría (inicialización, dimensión,
     *          firma de la función) y la marca de redeclaración si aplica.
     */
    public String getInformacion() {
        StringBuilder sb = new StringBuilder();
        switch (categoria) {
            case VARIABLE:
                sb.append(inicializada ? "inicializada" : "sin inicializar");
                break;
            case ARREGLO:
                sb.append("dimensión ").append(dimension)
                  .append(inicializada ? ", inicializado" : ", sin inicializar");
                break;
            case PARAMETRO:
                sb.append("recibido por valor");
                break;
            case FUNCION:
                sb.append("retorna ").append(tipo).append("; ").append(firmaParametros());
                break;
            case PRINCIPAL:
                sb.append("punto de entrada; ").append(firmaParametros());
                break;
            default:
                break;
        }
        if (redeclarada && primeraDeclaracion != null) {
            sb.append("; REDECLARADA (primera en L").append(primeraDeclaracion.getLinea())
              .append(":C").append(primeraDeclaracion.getColumna()).append(")");
        }
        return sb.toString();
    }

    /**
     * Describe los parámetros de una función.
     * @Entrada: Ninguna.
     * @Salida: "sin parámetros" o "parámetros (int n, float x)".
     */
    public String firmaParametros() {
        if (parametros.isEmpty()) {
            return "sin parámetros";
        }
        StringBuilder sb = new StringBuilder("parámetros (");
        for (int i = 0; i < parametros.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(parametros.get(i).getTipo()).append(' ').append(parametros.get(i).getLexema());
        }
        return sb.append(')').toString();
    }

    /**
     * Descripción corta usada en la columna Info del archivo de tokens.
     * @Entrada: Ninguna.
     * @Salida: Por ejemplo "variable int" o "función float, parámetros (int n)".
     */
    public String getDescripcionCorta() {
        if (categoria == Categoria.FUNCION || categoria == Categoria.PRINCIPAL) {
            return categoria + " " + tipo + ", " + firmaParametros();
        }
        if (categoria == Categoria.ARREGLO) {
            return "arreglo " + tipo + "[" + dimension + "]";
        }
        return categoria + " " + tipo;
    }
}
