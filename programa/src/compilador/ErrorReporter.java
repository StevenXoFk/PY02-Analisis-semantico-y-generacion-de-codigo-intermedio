package compilador;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Acumula y reporta errores de análisis léxico, sintáctico y semántico.
 * Los cuales los errores se almacenan en una lista y se pueden mostrar en consola.
 * .
 * @Entrada: mensaje de error, línea y columna donde ocurrió el error.
 * @Salida: lista de errores acumulados y mostrados en consola.
 * 
 * @Restricciones: 
 * - El orden de parámetros para los métodos de reporte de errores debe ser: mensaje, columna, línea.
 * - Los errores se deben mostrar en orden ascendente por línea y columna.
 * 
 * @Objetivo: Poder proporcionar un mecanismo para acumular y reportar los errores
 * que ocurren durante el análisis léxico, sintáctico y semántico de un archivo fuente.
 */

public class ErrorReporter {
    public static class ErrorAnalisis {
        public final String mensaje;
        public final int linea;
        public final int columna;
        public final String tipo;

        public ErrorAnalisis(String mensaje, int linea, int columna, String tipo) {
            this.mensaje = mensaje;
            this.linea = linea;
            this.columna = columna;
            this.tipo = tipo;
        }

        @Override
        public String toString() {
            return String.format("%s: %s en la línea %d, columna %d", tipo, mensaje, linea, columna);
        }
    }

    private static final List<ErrorAnalisis> errores = new ArrayList<>();

    /**
     * Registra un error léxico.
     * @Entrada: mensaje de error, columna y línea donde ocurrió el error.
     * @Salida: Se agrega un error léxico a la lista de errores.
     */
    public static void lexico(String msj, int columna, int linea) {
        errores.add(new ErrorAnalisis(msj, linea, columna, "ERROR LÉXICO"));
    }

    /**
     * Registra un error sintáctico.
     * @Entrada: mensaje de error, columna y línea donde ocurrió el error.
     * @Salida: Se agrega un error sintáctico a la lista de errores.
     */
    public static void sintactico(String msj, int columna, int linea) {
        errores.add(new ErrorAnalisis(msj, linea, columna, "ERROR SINTÁCTICO"));
    }

    /**
     * Registra un error semántico.
     * @Entrada: mensaje de error, columna y línea donde ocurrió el error.
     * @Salida: Se agrega un error semántico a la lista de errores.
     */
    public static void semantico(String msj, int columna, int linea) {
        errores.add(new ErrorAnalisis(msj, linea, columna, "ERROR SEMÁNTICO"));
    }

    /**
     * Verifica si hay errores acumulados.
     * @Entrada: Ninguna.
     * @Salida: true si hay errores, false si no hay errores.
    */
    public static boolean hayErrores() {
        return !errores.isEmpty();
    }

    /**
     * Obtiene la lista de errores acumulados.
     * @Entrada: Ninguna.
     * @Salida: Lista de errores acumulados, ordenada por línea y columna.
     */
    public static List<ErrorAnalisis> getErrores() {
        List<ErrorAnalisis> copia = new ArrayList<>(errores);
        copia.sort(Comparator.comparingInt((ErrorAnalisis e) -> e.linea).thenComparingInt(e -> e.columna));
        return copia;
    }

    /**
     * Muestra los errores acumulados en consola.
     * @Entrada: Ninguna.
     * @Salida: Se imprimen los errores en consola.
     */
    public static void mostrarErrores() {
        if (errores.isEmpty()) {
            System.out.println("No se encontraron errores.");
            return;
        }
        System.out.println("Se encontraron " + errores.size() + " error(es):");
        for (ErrorAnalisis error : getErrores()) {
            System.out.println(error);
        }
    }

    /**
     * Guarda los errores acumulados en un archivo.
     * @Entrada: Ruta del archivo donde se guardarán los errores.
     * @Salida: Se escriben los errores en el archivo especificado.
     */
    public static void guardarErrores(Path ruta) throws IOException {
        Path carpeta = ruta.getParent();
        if (carpeta != null) {
            Files.createDirectories(carpeta);
        }
        try (BufferedWriter writer = Files.newBufferedWriter(ruta, StandardCharsets.UTF_8)) {
            if (errores.isEmpty()) {
                writer.write("No se encontraron errores.");
                writer.newLine();
                return;
            }
            writer.write("Se encontraron " + errores.size() + " error(es):");
            writer.newLine();
            for (ErrorAnalisis error : getErrores()) {
                writer.write(error.toString());
                writer.newLine();
            }
        }
    }

    /**
     * Obtiene el total de errores acumulados.
     * @Entrada: Ninguna.
     * @Salida: Total de errores acumulados.
     */
    public static int totalErrores() {
        return errores.size();
    }

    /**
     * Limpia la lista de errores acumulados.
     * @Entrada: Ninguna.
     * @Salida: Se limpia la lista de errores.
     */
    public static void limpiarErrores() {
        errores.clear();
    }
}
