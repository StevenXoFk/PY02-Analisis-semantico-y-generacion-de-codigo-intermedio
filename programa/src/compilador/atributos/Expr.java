package compilador.atributos;

/**
 * Atributos sintetizados de una expresión: los usa la semántica (tipo) y el TAC (lugar y código).
 * Es el RESULT de todos los no terminales de expresión en Parser.cup.
 * @Entrada: tipo, lugar, código que lo calcula y posición.
 * @Salida: objeto que el padre combina con los de otras expresiones.
 * @Restricciones:
 * - tipo es "int", "float", "bool", "char", "string" o "error".
 * - Una expresión de tipo "error" no vuelve a reportar errores (evita errores en cascada).
 * - nombre solo se llena cuando la expresión es una variable (ID) o un acceso a arreglo;
 *   indice solo en el acceso a arreglo.
 * @Objetivo: Ser el contrato entre Semantico y GeneradorTAC para las expresiones.
 */
public class Expr {

    public static final String ERROR = "error";

    private String tipo;
    private String lugar;
    private Codigo codigo;
    private final int linea;
    private final int columna;
    private String nombre;
    private Expr indice;

    /**
     * Crea una expresión sin lugar ni código.
     * @Entrada: tipo y posición del primer token de la expresión.
     * @Salida: Expr nueva con código vacío.
     */
    public Expr(String tipo, int linea, int columna) {
        this.tipo = tipo;
        this.linea = linea;
        this.columna = columna;
        this.codigo = Codigo.vacio();
    }

    /**
     * Crea una expresión de tipo error.
     * @Entrada: posición donde ocurrió.
     * @Salida: Expr de tipo "error" con código vacío.
     */
    public static Expr error(int linea, int columna) {
        return new Expr(ERROR, linea, columna);
    }

    /**
     * Indica si una expresión es inválida (null por una producción error, o de tipo error).
     * @Entrada: expresión, posiblemente null.
     * @Salida: true si no debe usarse para validar ni generar código.
     */
    public static boolean esError(Expr e) {
        return e == null || ERROR.equals(e.tipo);
    }

    public String getTipo() { return tipo; }
    public String getLugar() { return lugar; }
    public Codigo getCodigo() { return codigo; }
    public int getLinea() { return linea; }
    public int getColumna() { return columna; }
    public String getNombre() { return nombre; }
    public Expr getIndice() { return indice; }

    public void setTipo(String tipo) { this.tipo = tipo; }
    public void setIndice(Expr indice) { this.indice = indice; }
    public void setLugar(String lugar) { this.lugar = lugar; }
    public void setCodigo(Codigo codigo) { this.codigo = codigo != null ? codigo : Codigo.vacio(); }
    public void setNombre(String nombre) { this.nombre = nombre; }
}
