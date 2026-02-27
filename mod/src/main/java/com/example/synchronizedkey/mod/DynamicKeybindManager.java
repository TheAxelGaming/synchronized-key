package com.example.synchronizedkey.mod;

import com.example.synchronizedkey.mod.mixin.GameOptionsAccessor;
import com.example.synchronizedkey.mod.mixin.KeyBindingAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Gestiona el registro y limpieza de teclas dinámicas en Minecraft.
 * Las teclas aparecen en el menú de controles nativo bajo la categoría
 * "Servidor: [nombre]", y se limpian al desconectarse para evitar
 * conflictos con otros servidores.
 *
 * Utiliza lógica DIFERENCIAL para que al recibir un nuevo paquete de
 * sincronización (ej: /sk reload), se reutilicen los KeyBinding existentes
 * y se conserven las teclas personalizadas por el jugador.
 */
public class DynamicKeybindManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("SynchronizedKey-Teclas");

    /**
     * Mapa de teclas dinámicas activas: actionId -> KeyBinding.
     * Se usa para detectar pulsaciones y para limpieza.
     * Los objetos KeyBinding se reutilizan entre reloads para
     * preservar las asignaciones personalizadas del jugador.
     */
    private final Map<String, KeyBinding> dynamicKeyMap = new HashMap<>();

    /**
     * Nombre de la categoría dinámica actual (ej: "Servidor: MiServer").
     * Se guarda para poder eliminarla del mapa de orden al limpiar.
     */
    private String categoriaActual = null;

    /**
     * Prioridad alta para que las categorías dinámicas aparezcan al final
     * del menú de controles, después de las categorías vanilla.
     */
    private static final int CATEGORIA_ORDEN_PRIORIDAD = 100;

    /**
     * Registra las teclas dinámicas recibidas del servidor usando lógica
     * DIFERENCIAL. En lugar de borrar todo y recrear:
     * - Reutiliza KeyBindings existentes (conserva teclas personalizadas).
     * - Crea nuevos KeyBindings solo para acciones nuevas.
     * - Elimina KeyBindings de acciones que ya no envía el servidor.
     *
     * @param acciones   Lista de acciones con sus teclas
     * @param serverName Nombre del servidor (para la categoría)
     */
    public void registrarTeclasDinamicas(List<KeybindData> acciones, String serverName) {
        String categoria = "Servidor: " + serverName;
        categoriaActual = categoria;

        // Registrar la categoría en el mapa de orden para evitar NPE
        // al ordenar categorías (compatibilidad con el mod Controlling)
        registrarOrdenCategoria(categoria);

        // Construir un set con los IDs recibidos del servidor
        Set<String> idsRecibidos = new HashSet<>();
        for (KeybindData accion : acciones) {
            idsRecibidos.add(accion.id());
        }

        // --- Paso 1: Registrar nuevas teclas y reutilizar existentes ---
        List<KeyBinding> nuevasTeclas = new ArrayList<>();
        for (KeybindData accion : acciones) {
            if (dynamicKeyMap.containsKey(accion.id())) {
                // La tecla ya existe → reutilizar el objeto KeyBinding existente.
                // Esto preserva la tecla que el jugador haya configurado manualmente.
                LOGGER.info("Tecla '{}' ya existe, reutilizando (conserva config de usuario)",
                        accion.id());
            } else {
                // Acción nueva → crear un nuevo KeyBinding
                KeyBinding tecla = new KeyBinding(
                        accion.label(), // Se muestra como nombre de la tecla
                        InputUtil.Type.KEYSYM,
                        accion.defaultKey(), // Código GLFW por defecto
                        categoria // Categoría en el menú de controles
                );

                dynamicKeyMap.put(accion.id(), tecla);
                nuevasTeclas.add(tecla);
                LOGGER.info("Tecla dinámica NUEVA registrada: '{}' -> {} (GLFW: {})",
                        accion.id(), accion.label(), accion.defaultKey());
            }
        }

        // --- Paso 2: Eliminar teclas obsoletas (ya no vienen del servidor) ---
        List<KeyBinding> teclasEliminadas = new ArrayList<>();
        Iterator<Map.Entry<String, KeyBinding>> iterador = dynamicKeyMap.entrySet().iterator();
        while (iterador.hasNext()) {
            Map.Entry<String, KeyBinding> entrada = iterador.next();
            if (!idsRecibidos.contains(entrada.getKey())) {
                teclasEliminadas.add(entrada.getValue());
                iterador.remove();
                LOGGER.info("Tecla obsoleta eliminada: '{}'", entrada.getKey());
            }
        }

        // --- Paso 3: Actualizar allKeys solo si hubo cambios ---
        if (!nuevasTeclas.isEmpty() || !teclasEliminadas.isEmpty()) {
            actualizarAllKeys(nuevasTeclas, teclasEliminadas);
            LOGGER.info("allKeys actualizado: +{} nuevas, -{} eliminadas",
                    nuevasTeclas.size(), teclasEliminadas.size());
        } else {
            LOGGER.info("Sin cambios en teclas dinámicas, allKeys intacto.");
        }

        // Recalcular el mapa interno de teclas por código
        KeyBinding.updateKeysByCode();

        LOGGER.info("Total de teclas dinámicas activas: {} (categoría: '{}')",
                dynamicKeyMap.size(), categoria);
    }

    /**
     * Limpia todas las teclas dinámicas registradas.
     * Se llama al desconectarse del servidor para evitar conflictos
     * con otros servidores. NO se llama durante un reload.
     */
    public void limpiarTeclasDinamicas() {
        if (dynamicKeyMap.isEmpty()) {
            return;
        }

        LOGGER.info("Limpiando {} teclas dinámicas...", dynamicKeyMap.size());

        // Quitar las teclas dinámicas del array allKeys
        removerDeAllKeys();

        // Quitar la categoría dinámica del mapa de orden
        desregistrarOrdenCategoria();

        // Limpiar nuestro mapa interno
        dynamicKeyMap.clear();

        // Recalcular el mapa interno de teclas por código
        KeyBinding.updateKeysByCode();

        LOGGER.info("Teclas dinámicas limpiadas correctamente.");
    }

    /**
     * Devuelve el mapa de teclas dinámicas activas.
     * Usado por KeybindInputHandler para detectar pulsaciones.
     *
     * @return Mapa actionId -> KeyBinding (solo lectura conceptual)
     */
    public Map<String, KeyBinding> obtenerDynamicKeyMap() {
        return dynamicKeyMap;
    }

    /**
     * Verifica si hay teclas dinámicas activas.
     *
     * @return true si hay al menos una tecla registrada
     */
    public boolean tieneTeclasActivas() {
        return !dynamicKeyMap.isEmpty();
    }

    /**
     * Actualiza el array allKeys de GameOptions de forma diferencial.
     * Elimina las teclas obsoletas y añade las nuevas, sin tocar las existentes.
     *
     * @param nuevas     Lista de KeyBindings nuevos a añadir
     * @param eliminadas Lista de KeyBindings obsoletos a quitar
     */
    private void actualizarAllKeys(List<KeyBinding> nuevas, List<KeyBinding> eliminadas) {
        MinecraftClient cliente = MinecraftClient.getInstance();
        if (cliente.options == null) {
            return;
        }

        GameOptions opciones = cliente.options;
        KeyBinding[] teclasActuales = opciones.allKeys;

        // Crear un set de las eliminadas para búsqueda rápida
        Set<KeyBinding> setEliminadas = new HashSet<>(eliminadas);

        // Filtrar las eliminadas del array actual
        List<KeyBinding> listaResultante = new ArrayList<>();
        for (KeyBinding tecla : teclasActuales) {
            if (!setEliminadas.contains(tecla)) {
                listaResultante.add(tecla);
            }
        }

        // Añadir las nuevas al final
        listaResultante.addAll(nuevas);

        // Reemplazar el array en GameOptions usando el Accessor Mixin
        ((GameOptionsAccessor) opciones).setAllKeys(
                listaResultante.toArray(new KeyBinding[0]));
    }

    /**
     * Remueve las teclas dinámicas del array allKeys de GameOptions.
     * Filtra el array para quedarse solo con las teclas no dinámicas.
     */
    private void removerDeAllKeys() {
        MinecraftClient cliente = MinecraftClient.getInstance();
        if (cliente.options == null) {
            return;
        }

        GameOptions opciones = cliente.options;
        KeyBinding[] teclasActuales = opciones.allKeys;

        // Filtrar las teclas dinámicas del array
        KeyBinding[] arrayFiltrado = Arrays.stream(teclasActuales)
                .filter(tecla -> !dynamicKeyMap.containsValue(tecla))
                .toArray(KeyBinding[]::new);

        // Reemplazar el array en GameOptions usando el Accessor Mixin
        ((GameOptionsAccessor) opciones).setAllKeys(arrayFiltrado);
    }

    /**
     * Registra la categoría dinámica en el mapa estático CATEGORY_ORDER_MAP
     * de KeyBinding. Esto evita NullPointerException cuando mods como
     * Controlling intentan ordenar las categorías en el menú de controles.
     *
     * Si el mapa es inmutable (ej: ImmutableMap de Guava), se reemplaza
     * por un HashMap mutable usando el setter del Accessor Mixin.
     *
     * @param categoria Nombre de la categoría (ej: "Servidor: MiServer")
     */
    private void registrarOrdenCategoria(String categoria) {
        try {
            Map<String, Integer> mapaOrden = KeyBindingAccessor.getCategoryOrderMap();

            if (mapaOrden == null) {
                LOGGER.warn("CATEGORY_ORDER_MAP es null, no se puede registrar la categoría.");
                return;
            }

            // Si ya contiene la categoría, no hacer nada
            if (mapaOrden.containsKey(categoria)) {
                LOGGER.info("Categoría '{}' ya existe en CATEGORY_ORDER_MAP, omitiendo registro.", categoria);
                return;
            }

            // Intentar insertar directamente (funciona si el mapa es mutable)
            try {
                mapaOrden.put(categoria, CATEGORIA_ORDEN_PRIORIDAD);
                LOGGER.info("Categoría '{}' registrada en CATEGORY_ORDER_MAP con prioridad {}",
                        categoria, CATEGORIA_ORDEN_PRIORIDAD);
            } catch (UnsupportedOperationException e) {
                // El mapa es inmutable → crear uno nuevo mutable con las entradas existentes
                LOGGER.info("CATEGORY_ORDER_MAP es inmutable, reemplazando con mapa mutable...");
                Map<String, Integer> nuevoMapa = new HashMap<>(mapaOrden);
                nuevoMapa.put(categoria, CATEGORIA_ORDEN_PRIORIDAD);
                KeyBindingAccessor.setCategoryOrderMap(nuevoMapa);
                LOGGER.info("Categoría '{}' registrada tras reemplazar CATEGORY_ORDER_MAP (prioridad: {})",
                        categoria, CATEGORIA_ORDEN_PRIORIDAD);
            }
        } catch (Exception e) {
            LOGGER.warn("No se pudo registrar la categoría en CATEGORY_ORDER_MAP: {}", e.getMessage());
        }
    }

    /**
     * Elimina la categoría dinámica del mapa estático CATEGORY_ORDER_MAP
     * al limpiar las teclas, para no dejar entradas huérfanas.
     */
    private void desregistrarOrdenCategoria() {
        if (categoriaActual == null) {
            return;
        }

        try {
            Map<String, Integer> mapaOrden = KeyBindingAccessor.getCategoryOrderMap();
            if (mapaOrden != null) {
                mapaOrden.remove(categoriaActual);
                LOGGER.info("Categoría '{}' eliminada de CATEGORY_ORDER_MAP", categoriaActual);
            }
        } catch (Exception e) {
            LOGGER.warn("No se pudo eliminar la categoría de CATEGORY_ORDER_MAP: {}", e.getMessage());
        }

        categoriaActual = null;
    }
}
