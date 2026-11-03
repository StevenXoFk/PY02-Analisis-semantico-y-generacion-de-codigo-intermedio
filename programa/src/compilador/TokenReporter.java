package compilador;
import compilador.generated.sym;
import java_cup.runtime.Symbol;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Clase que permite registrar los tokens generados por el lexer y escribirlos en un archivo.
 * Implementa AutoCloseable para asegurar el cierre del BufferedWriter al finalizar su uso.
 * @Entrada: Ruta del archivo donde se registrarán los tokens.
 * @Salida: Tokens generados por el lexer, registrados en el archivo especificado.
 * @Restricciones:
 * - La ruta del archivo debe ser válida y accesible para escritura.
 * - El lexer debe estar correctamente generado y disponible en el classpath.
 * @Objetivo: Proporcionar un mecanismo para registrar los tokens generados por el lexer
 * y almacenarlos en un archivo para su análisis posterior.
 */
public class TokenReporter implements AutoCloseable {
    private final BufferedWriter writer;
    private static final String FORMATO = "%-8s%-10s%-25s%-35s%-20s%-30s%n";

    public TokenReporter(Path ruta) throws IOException {
        Path carpeta = ruta.getParent();
        if (carpeta != null) {
            Files.createDirectories(carpeta);
        }
        writer = Files.newBufferedWriter(ruta, StandardCharsets.UTF_8);
        writer.write(String.format(FORMATO,"Linea", "Columna", "Token", "Lexema", "Tabla", "Info"));
    }

    /**
     * Registra un token generado por el lexer en el archivo.
     * @Entrada: Token generado por el lexer.
     * @Salida: Se escribe el token en el archivo especificado.
     */
    public void register(Symbol token) throws IOException {
        if (token == null || token.sym == sym.EOF) {
            return;
        }

        String nombreToken = sym.terminalNames[token.sym];
        String lexema = token.value != null ? token.value.toString() : "";
        lexema = escape(lexema);

        String tabla = clasificarTabla(token.sym);
        String info = construirInfo(token.sym, lexema);
        
        writer.write(String.format(FORMATO, token.left, token.right, nombreToken, lexema, tabla, info));
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
     * Construye información adicional sobre el token, como su tipo y valor.
     * @Entrada: Código del token y su lexema.
     * @Salida: Información adicional sobre el token.
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
                return "Reservadas";

            case sym.ID:
                return "Identificadores";

            case sym.LIT_INT:
            case sym.LIT_FLOAT:
            case sym.LIT_CHAR:
            case sym.LIT_STRING:
            case sym.TRUE:
            case sym.FALSE:
                return "Constantes";

            default:
                return "ninguna";
        }
    }
    
    /**
     * Construye información adicional sobre el token, como su tipo y valor.
     * @Entrada: Código del token y su lexema.
     * @Salida: Información adicional sobre el token.
     */
    private String construirInfo(int symCode, String lexema) {
        switch (symCode) {
            case sym.LIT_INT:
                return "valor=" + lexema + ", tipo=int";
            case sym.LIT_FLOAT:
                return "valor=" + lexema + ", tipo=float";
            case sym.LIT_CHAR:
                return "valor=" + lexema + ", tipo=char";
            case sym.LIT_STRING:
                return "valor=" + lexema + ", tipo=string";
            case sym.TRUE:
            case sym.FALSE:
                return "valor=" + lexema + ", tipo=bool";
            default:
                return "";
        }
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
