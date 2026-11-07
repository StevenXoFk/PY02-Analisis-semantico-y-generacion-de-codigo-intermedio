package compilador;

import compilador.atributos.Codigo;
import compilador.generated.Lexer;
import compilador.generated.Parser;
import compilador.semantico.Semantico;
import compilador.simbolos.GestorTablas;
import compilador.simbolos.ReporteSimbolos;
import compilador.simbolos.TablaSimbolos;
import compilador.tac.GeneradorTAC;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import java_cup.runtime.Scanner;
import java_cup.runtime.Symbol;

/**
 * Punto de entrada del compilador: analiza un archivo fuente en dos pasadas y genera los reportes.
 * - Pasada 1: lexer + parser solo para armar el árbol completo de tablas de símbolos. Sus errores
 *   se descartan (se vuelven a encontrar en la pasada 2).
 * - Pasada 2: análisis completo (tokens, errores léxicos, sintácticos y semánticos, tablas y TAC).
 *   La semántica recibe el árbol de la pasada 1 para resolver nombres, llamadas a funciones
 *   declaradas más abajo, return y break, aunque el parser bottom-up aún no haya cerrado esos
 *   alcances.
 * @Entrada: ruta del archivo fuente (UTF-8).
 * @Salida: salida/<nombre>_tokens.txt, _simbolos.txt, _errores.txt, _tac.txt (solo si no hubo
 *          errores) y el veredicto en consola; código de salida 0 sin errores, 1 con errores.
 * @Restricciones: el archivo debe existir; las clases estáticas (ErrorReporter, GestorTablas,
 *                 Semantico, GeneradorTAC) se reinician antes de cada pasada.
 * @Objetivo: Orquestar el análisis léxico, sintáctico y semántico y la generación de código
 *            intermedio.
 */
public class Main {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Uso: run.sh <ruta_archivo_fuente>");
            System.exit(2);
        }

        File archivoFuente = new File(args[0]);
        if (!archivoFuente.exists() || !archivoFuente.isFile()) {
            System.err.println("El archivo fuente no existe o no es un archivo: " + args[0]);
            System.exit(2);
        }

        String nombreBase = archivoFuente.getName().replaceFirst("[.][^.]+$", "");
        Path rutaTokens  = Path.of("salida", nombreBase + "_tokens.txt");
        Path rutaErrores = Path.of("salida", nombreBase + "_errores.txt");
        Path rutaSimbolos = Path.of("salida", nombreBase + "_simbolos.txt");
        Path rutaTac = Path.of("salida", nombreBase + "_tac.txt");

        System.out.println("Analizando: " + archivoFuente.getPath());
        System.out.println();

        // pass 1
        ErrorReporter.limpiarErrores();
        GestorTablas.reiniciar();
        Semantico.reiniciar();
        GeneradorTAC.reiniciar();
        analizar(archivoFuente, null);
        TablaSimbolos tablaGlobal = GestorTablas.finalizar();

        // pass 2
        ErrorReporter.limpiarErrores();
        GestorTablas.reiniciar();
        Semantico.iniciar(tablaGlobal);
        GeneradorTAC.reiniciar();
        TokenReporter tokenReporter = new TokenReporter();
        Codigo programa = analizar(archivoFuente, tokenReporter);
        Semantico.validarDeclaraciones();

        GestorTablas.finalizar();
        try {
            tokenReporter.escribir(rutaTokens);
            ReporteSimbolos.escribir(rutaSimbolos, tokenReporter.filasConstantes(),
                    tokenReporter.filasReservadas());
        } catch (Exception e) {
            System.err.println("Error al guardar tokens o tablas de símbolos: " + e.getMessage());
        }

        ErrorReporter.mostrarErrores();
        try {
            ErrorReporter.guardarErrores(rutaErrores);
        } catch (Exception e) {
            System.err.println("Error al guardar los errores en el archivo: " + e.getMessage());
        }

        System.out.println();

        if (ErrorReporter.hayErrores()) {
            System.out.println("El archivo no puede ser generado por la gramática. (mirar errores en " + rutaErrores.toString() + ")");
            System.out.println("Total de errores: " + ErrorReporter.getErrores().size());
            System.exit(1);
        } else {
            try {
                GeneradorTAC.escribirArchivo(rutaTac, programa);
            } catch (Exception e) {
                System.err.println("Error al guardar el código intermedio: " + e.getMessage());
            }
            System.out.println("El archivo se pudo ser generado con éxito, (archivo de tokens en " + rutaTokens.toString() + ")");
            System.out.println("Tablas de símbolos en " + rutaSimbolos.toString());
            System.out.println("Código intermedio en " + rutaTac.toString());
            System.exit(0);
        }
    }

    /**
     * Ejecuta el lexer y el parser sobre el archivo una vez.
     * @Entrada: archivo fuente y TokenReporter (null en la pasada 1, donde no se registran tokens).
     * @Salida: el código TAC del programa (RESULT de "programa"), o null si el parse no terminó.
     * @Restricciones: las excepciones del parse se reportan como error sintáctico.
     */
    private static Codigo analizar(File archivoFuente, TokenReporter tokenReporter) {
        try (Reader lector = new BufferedReader(new InputStreamReader(
                new FileInputStream(archivoFuente), StandardCharsets.UTF_8))) {

            Lexer lexer = new Lexer(lector);
            Scanner scanner = tokenReporter != null ? new ReportingLexer(lexer, tokenReporter) : lexer;
            @SuppressWarnings("deprecation")
            Parser parser = new Parser(scanner);
            Symbol resultado = parser.parse();
            return resultado != null && resultado.value instanceof Codigo ? (Codigo) resultado.value : null;

        } catch (Exception e) {
            ErrorReporter.sintactico(
                    "Excepción durante el análisis: " + e.getMessage(), 0, 0);
        } catch (Throwable t) {
            ErrorReporter.sintactico(
                    "Error fatal durante el análisis: " + t.getMessage(), 0, 0);
        }
        return null;
    }
}
