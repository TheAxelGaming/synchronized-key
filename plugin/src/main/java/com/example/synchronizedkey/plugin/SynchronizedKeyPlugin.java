package com.example.synchronizedkey.plugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.Messenger;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Clase principal del plugin SynchronizedKey para Spigot/Paper 1.21.
 *
 * Este plugin permite configurar teclas dinámicas en el cliente desde el
 * servidor.
 * Lee las acciones desde config.yml, las envía al mod del cliente cuando un
 * jugador
 * se conecta, y procesa las pulsaciones de teclas recibidas desde el mod.
 *
 * Canal de comunicación: teclas_pro:main
 */
public class SynchronizedKeyPlugin extends JavaPlugin {

    /**
     * Canal de comunicación bidireccional entre plugin y mod (público para
     * ReloadCommand)
     */
    public static final String CANAL = "teclas_pro:main";

    /**
     * Mapa de acciones indexado por su ID para búsquedas rápidas O(1).
     * Se carga desde config.yml al habilitar el plugin.
     */
    private final Map<String, KeybindAction> mapaAcciones = new HashMap<>();

    @Override
    public void onEnable() {
        // Paso 1: Guardar y cargar la configuración por defecto
        saveDefaultConfig();
        cargarAccionesDesdeConfig();

        // Paso 2: Registrar el canal de comunicación con el sistema Messenger de Spigot
        registrarCanal();

        // Paso 3: Registrar los listeners de eventos
        registrarListeners();

        // Paso 4: Registrar el comando /sk
        registrarComandos();

        getLogger().info("SynchronizedKey Plugin habilitado correctamente.");
        getLogger().info("Acciones cargadas: " + mapaAcciones.size());
    }

    @Override
    public void onDisable() {
        // Limpiar el mapa de acciones al deshabilitar
        mapaAcciones.clear();
        getLogger().info("SynchronizedKey Plugin deshabilitado.");
    }

    /**
     * Registra el canal teclas_pro:main para comunicación bidireccional.
     * - Outgoing: el plugin envía la configuración de teclas al cliente.
     * - Incoming: el plugin recibe las pulsaciones de teclas del cliente.
     */
    private void registrarCanal() {
        Messenger messenger = getServer().getMessenger();

        // Registrar canal de salida (servidor → cliente)
        messenger.registerOutgoingPluginChannel(this, CANAL);

        // Registrar canal de entrada (cliente → servidor) con su listener
        KeybindMessageListener receptorMensajes = new KeybindMessageListener(this);
        messenger.registerIncomingPluginChannel(this, CANAL, receptorMensajes);

        getLogger().info("Canal '" + CANAL + "' registrado correctamente.");
    }

    /**
     * Registra los listeners de eventos del plugin.
     */
    private void registrarListeners() {
        getServer().getPluginManager().registerEvents(
                new PlayerJoinListener(this), this);
    }

    /**
     * Registra los comandos del plugin.
     * /sk reload - Recarga la configuración y sincroniza con todos los jugadores.
     */
    private void registrarComandos() {
        PluginCommand comandoSk = getCommand("sk");
        if (comandoSk != null) {
            comandoSk.setExecutor(new ReloadCommand(this));
        } else {
            getLogger().warning("No se pudo registrar el comando /sk. Verifica plugin.yml.");
        }
    }

    /**
     * Carga las acciones de keybind desde el archivo config.yml.
     * Cada acción se almacena en el mapa indexada por su ID.
     *
     * Formato esperado en config.yml:
     * acciones:
     * - id: "abrir_menu"
     * label: "Abrir Menú"
     * default_key: 77
     * comando: "dm open menu_principal"
     * permiso: ""
     */
    public void cargarAccionesDesdeConfig() {
        mapaAcciones.clear();

        // Obtener la lista de acciones desde la configuración
        List<?> listaAcciones = getConfig().getList("acciones");

        if (listaAcciones == null || listaAcciones.isEmpty()) {
            getLogger().warning("No se encontraron acciones en config.yml");
            return;
        }

        // Iterar sobre cada elemento de la lista
        for (Object elemento : listaAcciones) {
            if (elemento instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> datosAccion = (Map<String, Object>) elemento;

                String id = String.valueOf(datosAccion.getOrDefault("id", ""));
                String label = String.valueOf(datosAccion.getOrDefault("label", ""));
                int defaultKey = datosAccion.containsKey("default_key")
                        ? ((Number) datosAccion.get("default_key")).intValue()
                        : 0;
                String comando = String.valueOf(datosAccion.getOrDefault("comando", ""));
                String permiso = String.valueOf(datosAccion.getOrDefault("permiso", ""));

                // Validar que los campos obligatorios no estén vacíos
                if (id.isEmpty() || label.isEmpty() || comando.isEmpty()) {
                    getLogger().warning(
                            "Acción con datos incompletos encontrada en config.yml. "
                                    + "Se requiere: id, label y comando. Saltando...");
                    continue;
                }

                // Crear la acción y almacenarla en el mapa
                KeybindAction accion = new KeybindAction(id, label, defaultKey, comando, permiso);
                mapaAcciones.put(id, accion);

                getLogger().info(
                        "Acción cargada: '" + id + "' -> /" + comando
                                + " (tecla: " + defaultKey + ")");
            }
        }
    }

    /**
     * Obtiene todas las acciones de keybind configuradas.
     *
     * @return Colección inmutable de todas las acciones
     */
    public Collection<KeybindAction> obtenerAcciones() {
        return mapaAcciones.values();
    }

    /**
     * Busca una acción de keybind por su ID.
     *
     * @param id Identificador de la acción
     * @return La acción encontrada, o null si no existe
     */
    public KeybindAction obtenerAccionPorId(String id) {
        return mapaAcciones.get(id);
    }

    /**
     * Construye el JSON de acciones serializado como bytes UTF-8.
     * Reutilizable por PlayerJoinListener y ReloadCommand.
     *
     * Formato: [{"id":"...", "label":"...", "default_key":N}, ...]
     *
     * @return bytes del JSON, o null si no hay acciones configuradas
     */
    public byte[] construirJsonAcciones() {
        Collection<KeybindAction> acciones = obtenerAcciones();

        if (acciones.isEmpty()) {
            return null;
        }

        JsonArray arrayAcciones = new JsonArray();
        for (KeybindAction accion : acciones) {
            JsonObject objetoAccion = new JsonObject();
            objetoAccion.addProperty("id", accion.getId());
            objetoAccion.addProperty("label", accion.getLabel());
            objetoAccion.addProperty("default_key", accion.getDefaultKey());
            arrayAcciones.add(objetoAccion);
        }

        return arrayAcciones.toString().getBytes(StandardCharsets.UTF_8);
    }
}
