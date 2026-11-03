package compilador;

import compilador.generated.Lexer;
import java_cup.runtime.Symbol;
import java_cup.runtime.Scanner;

/**
 * Clase que envuelve al lexer generado por JFlex para reportar los tokens generados.
 * Permite registrar los tokens generados por el lexer y reportarlos a un TokenReporter.
 * @Entrada: Lexer generado por JFlex y un TokenReporter para registrar los tokens.
 * @Salida: Tokens generados por el lexer, registrados en el TokenReporter.
 * @Restricciones:
 * - El lexer debe estar correctamente generado y disponible en el classpath.
 * - El TokenReporter debe estar correctamente implementado para registrar los tokens.
 * @Objetivo: Proporcionar una capa de reporte de tokens generados por el lexer, permitiendo su registro y análisis posterior.
 */
public class ReportingLexer implements Scanner {
    private final Lexer lexer;
    private final TokenReporter report;

    public ReportingLexer(Lexer lexer, TokenReporter tokenReporter) {
        this.lexer = lexer;
        this.report = tokenReporter;
    }

    /**
     * Obtiene el siguiente token generado por el lexer y lo registra en el TokenReporter.
     * @Entrada: Ninguna.
     * @Salida: El siguiente token generado por el lexer, registrado en el TokenReporter.
     * @Restricciones:
     * - El lexer debe estar correctamente generado y disponible en el classpath.
     * - El TokenReporter debe estar correctamente implementado para registrar los tokens.
     * @Objetivo: Obtener y registrar los tokens generados por el lexer para su análisis posterior.
     */
    @Override
    public Symbol next_token() throws Exception {
        Symbol token = lexer.next_token();
        report.register(token);
        return token;
    }
}
