package compilador.atributos;

/**
 * Una rama elif: su condición y el código de su bloque.
 * @Entrada: la condición (Expr) y el código del bloque.
 * @Salida: RESULT de sent_elif; lista_elif junta varias en orden.
 * @Restricciones: la condición puede ser de tipo error (GeneradorTAC la ignora).
 * @Objetivo: Pasar las ramas elif a GeneradorTAC.si para armar los saltos del if completo.
 */
public class Rama {

    private final Expr condicion;
    private final Codigo cuerpo;

    public Rama(Expr condicion, Codigo cuerpo) {
        this.condicion = condicion;
        this.cuerpo = cuerpo != null ? cuerpo : Codigo.vacio();
    }

    public Expr getCondicion() { return condicion; }
    public Codigo getCuerpo() { return cuerpo; }
}
