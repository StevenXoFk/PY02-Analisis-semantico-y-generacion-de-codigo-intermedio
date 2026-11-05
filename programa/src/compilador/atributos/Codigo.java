package compilador.atributos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Secuencia de instrucciones de código de tres direcciones.
 * Es el atributo sintetizado "code" de la traducción dirigida por la sintaxis: cada sentencia
 * devuelve su Codigo y el padre concatena el de sus hijos.
 * @Entrada: líneas de TAC (instrucciones o etiquetas) y otros Codigo para concatenar.
 * @Salida: lista ordenada de líneas, lista para escribirse en el archivo TAC.
 * @Restricciones:
 * - Las etiquetas se guardan terminadas en ":" y sin sangría; las instrucciones las indenta
 *   GeneradorTAC al escribir el archivo.
 * - Nunca contiene null: agregar(null) no hace nada.
 * @Objetivo: Transportar el código generado de abajo hacia arriba en el parser sin usar
 *            acciones a mitad de producción.
 */
public class Codigo {

    private final List<String> lineas = new ArrayList<>();

    /**
     * Crea un código vacío.
     * @Entrada: Ninguna.
     * @Salida: Codigo sin líneas.
     */
    public static Codigo vacio() {
        return new Codigo();
    }

    /**
     * Agrega una línea al final.
     * @Entrada: instrucción o etiqueta TAC.
     * @Salida: este mismo Codigo (permite encadenar llamadas).
     */
    public Codigo agregar(String linea) {
        if (linea != null) {
            lineas.add(linea);
        }
        return this;
    }

    /**
     * Agrega al final todas las líneas de otro código.
     * @Entrada: otro Codigo (puede ser null).
     * @Salida: este mismo Codigo.
     */
    public Codigo agregar(Codigo otro) {
        if (otro != null) {
            lineas.addAll(otro.lineas);
        }
        return this;
    }

    /**
     * Reemplaza todas las apariciones exactas de una línea por otra.
     * @Entrada: línea a buscar y línea nueva.
     * @Salida: este mismo Codigo.
     */
    public Codigo reemplazar(String buscada, String nueva) {
        for (int i = 0; i < lineas.size(); i++) {
            if (lineas.get(i).equals(buscada)) {
                lineas.set(i, nueva);
            }
        }
        return this;
    }

    public List<String> getLineas() {
        return Collections.unmodifiableList(lineas);
    }

    public boolean estaVacio() {
        return lineas.isEmpty();
    }
}
