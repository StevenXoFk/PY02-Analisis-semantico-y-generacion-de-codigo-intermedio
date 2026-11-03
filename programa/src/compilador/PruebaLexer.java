package compilador;

import compilador.generated.Lexer;
import compilador.generated.sym;
import java_cup.runtime.Symbol;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/**
 * Clase de prueba para el lexer generado por JFlex.
 * Permite analizar un archivo fuente y mostrar los tokens generados por el lexer.
 * @Entrada: Ruta del archivo fuente a analizar.
 * @Salida: Lista de tokens generados por el lexer, mostrados en consola.
 * @Restricciones:
 * - El archivo fuente debe existir y ser accesible.
 * - El lexer debe estar correctamente generado y disponible en el classpath.
 * @Objetivo: Probar el funcionamiento del lexer generado por JFlex y verificar que los tokens se generen correctamente a partir de un archivo fuente.
 */
public class PruebaLexer {
    public static void main(String[] args) throws Exception {
        Reader lector = new BufferedReader(new InputStreamReader(
            new FileInputStream(args[0]), StandardCharsets.UTF_8
        ));

        Lexer lexer = new Lexer(lector);
        Symbol t = lexer.next_token();

        while (t.sym != sym.EOF) {
            System.out.printf("%4d:%-4d %-18s %s%n", 
            t.left, t.right, sym.terminalNames[t.sym], t.value);

            t = lexer.next_token();
        }

        ErrorReporter.mostrarErrores();
    }
}
