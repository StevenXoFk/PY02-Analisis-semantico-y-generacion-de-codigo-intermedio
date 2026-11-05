package compilador.atributos;

import compilador.simbolos.TablaSimbolos;

/**
 * Atributos de un bloque "¿: ... :?": su tabla de símbolos y su código TAC.
 * @Entrada: la tabla que arma GestorTablas.cerrarBloque y el código de lista_sentencias.
 * @Salida: RESULT del no terminal bloque.
 * @Restricciones: el código nunca es null (vacío si la lista de sentencias falló).
 * @Objetivo: Permitir que bloque entregue a la vez su alcance (para nombrarlo) y su código.
 */
public class Bloque {

    private final TablaSimbolos tabla;
    private final Codigo codigo;

    public Bloque(TablaSimbolos tabla, Codigo codigo) {
        this.tabla = tabla;
        this.codigo = codigo != null ? codigo : Codigo.vacio();
    }

    public TablaSimbolos getTabla() { return tabla; }
    public Codigo getCodigo() { return codigo; }
}
