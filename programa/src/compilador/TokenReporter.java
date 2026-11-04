package compilador;
import compilador.generated.sym;
import compilador.simbolos.GestorTablas;
import java_cup.runtime.Symbol;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Clase que registra los tokens generados por el lexer y los escribe en un archivo, indicando
 * para cada token en cuál tabla de símbolos va y qué información se almacena.
 * Los tokens se guardan en memoria mientras se analiza y el archivo se escribe DESPUÉS del
 * parse, porque el alcance de cada identificador solo se conoce cuando el parser ya armó las
 * tablas de símbolos (GestorTablas).
 * @Entrada: Tokens generados por el lexer (vía ReportingLexer) y, al escribir, la ruta del archivo.
 * @Salida: Archivo de tokens con columnas Linea, Columna, Token, Lexema, Tabla, Info; además
 *          las filas de la tabla de constantes y de la tabla de palabras reservadas.
 * @Restricciones:
 * - La ruta del archivo debe ser válida y accesible para escritura.
 * - escribir() debe llamarse después de GestorTablas.finalizar() para que los ID tengan su tabla.
 * @Objetivo: Por cada token indicar su tabla de
 *            símbolos y la información que se almacena.
 */
public class TokenReporter {
    private static final String FORMATO = "%-8s%-10s%-18s%-30s%-30s%s%n";
    private static class TokenLeido {
        final int sym;
        final int linea;
        final int columna;
        final String lexema;

        TokenLeido(int sym, int linea, int columna, String lexema) {
            this.sym = sym;
            this.linea = linea;
            this.columna = columna;
            this.lexema = lexema;
        }
    }

    private final List<TokenLeido> tokens = new ArrayList<>();

    /**
     * Registra un token generado por el lexer (solo lo guarda en memoria).
     * @Entrada: Token generado por el lexer.
     * @Salida: El token queda en la lista para escribirse después del parse.
     */
    public void register(Symbol token) {
        if (token == null || token.sym == sym.EOF) {
            return;
        }
        String lexema = token.value != null ? token.value.toString() : "";
        tokens.add(new TokenLeido(token.sym, token.left, token.right, lexema));
    }

    /**
     * Escribe el archivo de tokens con la tabla y la información de cada uno.
     * @Entrada: Ruta del archivo.
     * @Salida: Archivo de tokens; los ID se resuelven con GestorTablas.resolver().
     */
    public void escribir(Path ruta) throws IOException {
        Path carpeta = ruta.getParent();
        if (carpeta != null) {
            Files.createDirectories(carpeta);
        }
        try (BufferedWriter writer = Files.newBufferedWriter(ruta, StandardCharsets.UTF_8)) {
            writer.write(String.format(FORMATO, "Linea", "Columna", "Token", "Lexema", "Tabla", "Info"));
            for (TokenLeido t : tokens) {
                String tabla;
                String info;
                if (t.sym == sym.ID) {
                    String[] r = GestorTablas.resolver(t.lexema, t.linea, t.columna);
                    tabla = r[0];
                    info = r[1];
                } else {
                    tabla = clasificarTabla(t.sym);
                    info = construirInfo(t.sym, escape(t.lexema));
                }
                writer.write(String.format(FORMATO, t.linea, t.columna, sym.terminalNames[t.sym],
                        escape(t.lexema), tabla, info));
            }
        }
    }

    /**
     * Filas de la tabla de constantes: cada literal distinto una sola vez.
     * @Entrada: Ninguna.
     * @Salida: filas {valor, tipo, primera posición, apariciones}.
     */
    public List<String[]> filasConstantes() {
        Map<String, String[]> filas = new LinkedHashMap<>();
        Map<String, Integer> conteo = new LinkedHashMap<>();
        for (TokenLeido t : tokens) {
            String tipo = tipoLiteral(t.sym);
            if (tipo == null) continue;
            String clave = tipo + "|" + t.lexema;
            filas.putIfAbsent(clave, new String[]{escape(t.lexema), tipo, "L" + t.linea + ":C" + t.columna, ""});
            conteo.merge(clave, 1, Integer::sum);
        }
        for (Map.Entry<String, String[]> e : filas.entrySet()) {
            e.getValue()[3] = String.valueOf(conteo.get(e.getKey()));
        }
        return new ArrayList<>(filas.values());
    }

    /**
     * Filas de la tabla de palabras reservadas que aparecen en el fuente.
     * @Entrada: Ninguna.
     * @Salida: filas {lexema, token, primera posición, apariciones}.
     */
    public List<String[]> filasReservadas() {
        Map<String, String[]> filas = new LinkedHashMap<>();
        Map<String, Integer> conteo = new LinkedHashMap<>();
        for (TokenLeido t : tokens) {
            if (!"reservadas".equals(clasificarTabla(t.sym))) continue;
            filas.putIfAbsent(t.lexema, new String[]{t.lexema, sym.terminalNames[t.sym],
                    "L" + t.linea + ":C" + t.columna, ""});
            conteo.merge(t.lexema, 1, Integer::sum);
        }
        for (Map.Entry<String, String[]> e : filas.entrySet()) {
            e.getValue()[3] = String.valueOf(conteo.get(e.getKey()));
        }
        return new ArrayList<>(filas.values());
    }

    /**
     * Escapa caracteres especiales en el lexema para su correcta visualización.
     * @Entrada: Lexema a escapar.
     * @Salida: Lexema con caracteres especiales escapados.
     */
    private String escape(String lexema) {
        return lexema.replace("\n", "\\n")
                     .replace("\t", "\\t")
                     .replace("\r", "\\r");
    }

    /**
     * Indica la tabla de un token que no es identificador.
     * @Entrada: Código del token.
     * @Salida: "reservadas", "constantes" o "ninguna" (operadores y delimitadores no se guardan).
     */
    private String clasificarTabla(int symCode) {
        switch (symCode) {
            case sym.VAL:
            case sym.IF:
            case sym.ELIF:
            case sym.ELSE:
            case sym.WHILE:
            case sym.FOR:
            case sym.RETURN:
            case sym.BREAK:
            case sym.READ:
            case sym.WRITE:
            case sym.PRINCIPAL:
            case sym.VOID:
            case sym.INT:
            case sym.FLOAT:
            case sym.BOOL:
            case sym.CHAR:
            case sym.STRING:
                return "reservadas";

            case sym.LIT_INT:
            case sym.LIT_FLOAT:
            case sym.LIT_CHAR:
            case sym.LIT_STRING:
            case sym.TRUE:
            case sym.FALSE:
                return "constantes";

            default:
                return "ninguna";
        }
    }

    /**
     * Tipo de un literal.
     * @Entrada: Código del token.
     * @Salida: "int", "float", "char", "string", "bool" o null si no es literal.
     */
    private String tipoLiteral(int symCode) {
        switch (symCode) {
            case sym.LIT_INT: return "int";
            case sym.LIT_FLOAT: return "float";
            case sym.LIT_CHAR: return "char";
            case sym.LIT_STRING: return "string";
            case sym.TRUE:
            case sym.FALSE: return "bool";
            default: return null;
        }
    }

    /**
     * Construye la información que se almacena para un token que no es identificador.
     * @Entrada: Código del token y su lexema.
     * @Salida: valor y tipo para literales; "palabra reservada" u "operador/delimitador (no se almacena)".
     */
    private String construirInfo(int symCode, String lexema) {
        String tipo = tipoLiteral(symCode);
        if (tipo != null) {
            return "valor=" + lexema + ", tipo=" + tipo;
        }
        if ("reservadas".equals(clasificarTabla(symCode))) {
            return "palabra reservada";
        }
        return "operador/delimitador (no se almacena)";
    }
}
