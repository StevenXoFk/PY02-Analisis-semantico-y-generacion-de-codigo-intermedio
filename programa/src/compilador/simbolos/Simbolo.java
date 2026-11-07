package compilador.simbolos;

import java.util.ArrayList;
import java.util.List;

/**
 * Un identificador declarado en el programa (variable, arreglo, parámetro o función).
 * @Entrada: lexema, tipo, línea y columna donde se declaró.
 * @Salida: los datos del identificador que se guardan en la tabla de símbolos.
 * @Restricciones: la dimensión solo se usa en arreglos y los parámetros solo en funciones.
 * @Objetivo: guardar la información de cada identificador.
 */
public class Simbolo {

    public enum Categoria { VARIABLE, ARREGLO, PARAMETRO, FUNCION, PRINCIPAL }

    private String lexema;
    private String tipo;
    private int linea;
    private int columna;
    private Categoria categoria = Categoria.VARIABLE;
    private boolean inicializada = false;
    private int dimension = 0;
    private List<Simbolo> parametros = new ArrayList<>();
    private boolean redeclarada = false;
    private Simbolo primeraDeclaracion = null;
    private TablaSimbolos alcance = null;

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
    public void setAlcance(TablaSimbolos alcance) { this.alcance = alcance; }

    /**
     * Marca el símbolo como repetido dentro de su mismo alcance.
     * @Entrada: la primera declaración con el mismo nombre.
     * @Salida: el símbolo queda marcado.
     */
    public void marcarRedeclarada(Simbolo primera) {
        redeclarada = true;
        primeraDeclaracion = primera;
    }

    /**
     * Posicion del símbolo como un solo número, para poder compararla.
     * @Entrada: ninguna.
     * @Salida: línea * 100000 + columna.
     */
    public long getPosicion() {
        return TablaSimbolos.posicion(linea, columna);
    }

    /**
     * Texto con la información extra del símbolo según su categoria.
     * @Entrada: ninguna.
     * @Salida: por ejemplo "inicializada", "tamano 5" o "retorna int, parámetros: int a".
     */
    public String getInfo() {
        String info = "";
        if (categoria == Categoria.VARIABLE) {
            if (inicializada) {
                info = "inicializada";
            } else {
                info = "sin inicializar";
            }
        } else if (categoria == Categoria.ARREGLO) {
            info = "tamaño " + dimension;
        } else if (categoria == Categoria.FUNCION || categoria == Categoria.PRINCIPAL) {
            info = "retorna " + tipo + ", parámetros: ";
            if (parametros.isEmpty()) {
                info += "ninguno";
            }
            for (int i = 0; i < parametros.size(); i++) {
                if (i > 0) {
                    info += ", ";
                }
                info += parametros.get(i).getTipo() + " " + parametros.get(i).getLexema();
            }
        }
        if (redeclarada) {
            info += " (repetida, primera en línea " + primeraDeclaracion.getLinea() + ")";
        }
        return info;
    }
}
