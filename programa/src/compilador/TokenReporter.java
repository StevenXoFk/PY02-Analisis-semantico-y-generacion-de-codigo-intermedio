package compilador;

import compilador.generated.sym;
import compilador.simbolos.GestorTablas;
import compilador.simbolos.Simbolo;
import compilador.simbolos.TablaSimbolos;
import java_cup.runtime.Symbol;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Guarda los tokens que lee el lexer y al final escribe el archivo de tokens, indicando para
 * cada token en qué tabla de símbolos va y qué información se guarda.
 * El archivo se escribe después del parse porque hasta ese momento no existen las tablas.
 * @Entrada: tokens del lexer (los manda ReportingLexer) y la ruta del archivo.
 * @Salida: archivo con las columnas Linea, Columna, Token, Lexema, Tabla e Info.
 * @Restricciones: escribir() se llama después de GestorTablas.finalizar().
 * @Objetivo: guardar los tokens y escribir el reporte de tokens con la información de las tablas de símbolos.
 */
public class TokenReporter {
    private static final String FORMATO = "%-8s%-10s%-18s%-30s%-30s%s%n";

    private List<Symbol> tokens = new ArrayList<>();

    /**
     * Guarda un token leído por el lexer.
     * @Entrada: token.
     * @Salida: el token queda en la lista (el EOF no se guarda).
     */
    public void register(Symbol token) {
        if (token != null && token.sym != sym.EOF) {
            tokens.add(token);
        }
    }

    /**
     * Escribe el archivo de tokens.
     * @Entrada: ruta del archivo.
     * @Salida: una línea por token.
     */
    public void escribir(Path ruta) throws IOException {
        if (ruta.getParent() != null) {
            Files.createDirectories(ruta.getParent());
        }
        try (BufferedWriter w = Files.newBufferedWriter(ruta, StandardCharsets.UTF_8)) {
            w.write(String.format(FORMATO, "Linea", "Columna", "Token", "Lexema", "Tabla", "Info"));
            w.write(String.format(FORMATO, "------", "-------", "-----", "------", "-----", "----"));
            for (Symbol t : tokens) {
                String lexema = lexema(t);
                String tabla;
                String info;
                if (t.sym == sym.ID) {
                    String[] datos = datosIdentificador(lexema, t.left, t.right);
                    tabla = datos[0];
                    info = datos[1];
                } else if (tipoLiteral(t.sym) != null) {
                    tabla = "constantes";
                    info = "valor " + lexema + ", tipo " + tipoLiteral(t.sym);
                } else if (esReservada(t.sym)) {
                    tabla = "reservadas";
                    info = "palabra reservada";
                } else {
                    tabla = "ninguna";
                    info = "operador/delimitador";
                }
                w.write(String.format(FORMATO, t.left, t.right, sym.terminalNames[t.sym], lexema, tabla, info));
            }
        }
    }

    /**
     * Busca en qué tabla queda un identificador y si es su declaración o un uso.
     * @Entrada: lexema, línea y columna del token.
     * @Salida: {nombre de la tabla, información}.
     */
    private String[] datosIdentificador(String lexema, int linea, int columna) {
        TablaSimbolos global = GestorTablas.getGlobal();
        if (global == null) {
            return new String[] {"identificadores", ""};
        }
        long pos = TablaSimbolos.posicion(linea, columna);
        TablaSimbolos tabla = global.tablaMasInterna(pos);

        for (Simbolo s : tabla.getSimbolos()) {
            if (s.getPosicion() == pos) {
                String categoria = s.getCategoria().name().toLowerCase();
                return new String[] {tabla.getNombre(), "declaración de " + categoria + " " + s.getTipo()};
            }
        }
        
        Simbolo s = tabla.buscarAntesDe(lexema, pos);
        if (s == null) {
            return new String[] {tabla.getNombre(), "uso sin declaración"};
        }
        String categoria = s.getCategoria().name().toLowerCase();
        return new String[] {tabla.getNombre(), "uso de " + categoria + " " + s.getTipo()
                + " declarada en " + s.getAlcance().getNombre() + " (línea " + s.getLinea() + ")"};
    }

    /**
     * Filas de la tabla de constantes, cada literal una sola vez.
     * @Entrada: ninguna.
     * @Salida: lista de {valor, tipo, línea donde aparece primero}.
     */
    public List<String[]> filasConstantes() {
        List<String[]> filas = new ArrayList<>();
        List<String> vistos = new ArrayList<>();
        for (Symbol t : tokens) {
            String tipo = tipoLiteral(t.sym);
            String lexema = lexema(t);
            if (tipo != null && !vistos.contains(tipo + " " + lexema)) {
                vistos.add(tipo + " " + lexema);
                filas.add(new String[] {lexema, tipo, String.valueOf(t.left)});
            }
        }
        return filas;
    }

    /**
     * Filas de la tabla de palabras reservadas que aparecen en el programa.
     * @Entrada: ninguna.
     * @Salida: lista de {lexema, token, línea donde aparece primero}.
     */
    public List<String[]> filasReservadas() {
        List<String[]> filas = new ArrayList<>();
        List<String> vistos = new ArrayList<>();
        for (Symbol t : tokens) {
            String lexema = lexema(t);
            if (esReservada(t.sym) && !vistos.contains(lexema)) {
                vistos.add(lexema);
                filas.add(new String[] {lexema, sym.terminalNames[t.sym], String.valueOf(t.left)});
            }
        }
        return filas;
    }

    /**
     * Texto del token, con los saltos de línea y tabs escritos como \n y \t.
     * @Entrada: token.
     * @Salida: lexema listo para escribirse en una sola línea.
     */
    private String lexema(Symbol t) {
        if (t.value == null) {
            return "";
        }
        return t.value.toString().replace("\n", "\\n").replace("\t", "\\t").replace("\r", "\\r");
    }

    /**
     * Indica si un token es palabra reservada.
     * @Entrada: código del token.
     * @Salida: true si es palabra reservada o tipo de dato.
     */
    private boolean esReservada(int codigo) {
        switch (codigo) {
            case sym.VAL: case sym.IF: case sym.ELIF: case sym.ELSE: case sym.WHILE: case sym.FOR:
            case sym.RETURN: case sym.BREAK: case sym.READ: case sym.WRITE: case sym.PRINCIPAL:
            case sym.VOID: case sym.INT: case sym.FLOAT: case sym.BOOL: case sym.CHAR: case sym.STRING:
                return true;
            default:
                return false;
        }
    }

    /**
     * Tipo de un literal.
     * @Entrada: código del token.
     * @Salida: "int", "float", "char", "string", "bool" o null si no es literal.
     */
    private String tipoLiteral(int codigo) {
        switch (codigo) {
            case sym.LIT_INT: return "int";
            case sym.LIT_FLOAT: return "float";
            case sym.LIT_CHAR: return "char";
            case sym.LIT_STRING: return "string";
            case sym.TRUE: case sym.FALSE: return "bool";
            default: return null;
        }
    }
}
