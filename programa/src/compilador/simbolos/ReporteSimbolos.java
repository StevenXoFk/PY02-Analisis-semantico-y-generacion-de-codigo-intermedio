package compilador.simbolos;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Escribe el archivo salida/<nombre>_simbolos.txt con todas las tablas de símbolos.
 * @Entrada: ruta del archivo, filas de constantes y filas de palabras reservadas.
 * @Salida: archivo con cada tabla (nombre, nivel, padre y sus símbolos), la tabla de
 *          constantes y la de palabras reservadas.
 * @Restricciones: se llama después de GestorTablas.finalizar().
 * @Objetivo: mostrar las tablas de símbolos que armó el compilador.
 */
public class ReporteSimbolos {

    private static final String FILA = "%-22s %-11s %-8s %-6s %-8s %s";

    /**
     * Escribe el reporte completo.
     * @Entrada: ruta, constantes {valor, tipo, línea} y reservadas {lexema, token, línea}.
     * @Salida: el archivo escrito.
     */
    public static void escribir(Path ruta, List<String[]> constantes, List<String[]> reservadas) throws IOException {
        if (ruta.getParent() != null) {
            Files.createDirectories(ruta.getParent());
        }
        try (BufferedWriter w = Files.newBufferedWriter(ruta, StandardCharsets.UTF_8)) {
            w.write("TABLAS DE SÍMBOLOS");
            w.newLine();
            if (!GestorTablas.isCompleta()) {
                w.write("Nota: hubo un error de sintaxis que no dejó terminar el análisis, las tablas pueden estar incompletas.");
                w.newLine();
            }
            w.newLine();
            if (GestorTablas.getGlobal() != null) {
                escribirTabla(w, GestorTablas.getGlobal());
            }

            w.write("=".repeat(80));
            w.newLine();
            w.write("TABLA DE CONSTANTES");
            w.newLine();
            w.write(String.format("%-30s %-8s %s", "Valor", "Tipo", "Línea"));
            w.newLine();
            for (String[] c : constantes) {
                w.write(String.format("%-30s %-8s %s", c[0], c[1], c[2]));
                w.newLine();
            }
            w.newLine();

            w.write("=".repeat(80));
            w.newLine();
            w.write("TABLA DE PALABRAS RESERVADAS");
            w.newLine();
            w.write(String.format("%-30s %-12s %s", "Lexema", "Token", "Línea"));
            w.newLine();
            for (String[] r : reservadas) {
                w.write(String.format("%-30s %-12s %s", r[0], r[1], r[2]));
                w.newLine();
            }
        }
    }

    /**
     * Escribe una tabla y después sus hijas (recorrido en preorden).
     * @Entrada: escritor y tabla.
     * @Salida: encabezado de la tabla y una fila por símbolo.
     */
    private static void escribirTabla(BufferedWriter w, TablaSimbolos tabla) throws IOException {
        String encabezado = "Tabla: " + tabla.getNombre() + " (nivel " + tabla.getNivel();
        if (tabla.getPadre() != null) {
            encabezado += ", padre: " + tabla.getPadre().getNombre();
        }
        w.write("=".repeat(80));
        w.newLine();
        w.write(encabezado + ")");
        w.newLine();

        if (tabla.getSimbolos().isEmpty()) {
            w.write("sin declaraciones");
            w.newLine();
        } else {
            w.write(String.format(FILA, "Lexema", "Categoría", "Tipo", "Línea", "Columna", "Información"));
            w.newLine();
            for (Simbolo s : tabla.getSimbolos()) {
                String categoria = s.getCategoria().name().toLowerCase();
                w.write(String.format(FILA, s.getLexema(), categoria, s.getTipo(),
                        s.getLinea(), s.getColumna(), s.getInfo()));
                w.newLine();
            }
        }
        w.newLine();

        for (TablaSimbolos hija : tabla.getHijos()) {
            escribirTabla(w, hija);
        }
    }
}
