package com.example.synchronizedkey.plugin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.nio.charset.StandardCharsets;

/**
 * Receptor de mensajes del canal teclas_pro:main.
 * Recibe el action_id enviado desde el mod del cliente cuando el jugador
 * presiona una tecla dinámica. Valida la existencia de la acción,
 * verifica permisos y ejecuta el comando correspondiente.
 */
public class KeybindMessageListener implements PluginMessageListener {

    /** Referencia al plugin principal para acceder al mapa de acciones */
    private final SynchronizedKeyPlugin plugin;

    /**
     * Constructor del receptor de mensajes.
     *
     * @param plugin Instancia del plugin principal
     */
    public KeybindMessageListener(SynchronizedKeyPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Método invocado al recibir un mensaje por el canal registrado.
     * Procesa el JSON recibido del cliente con formato: {"action_id": "..."}
     *
     * Flujo de validación:
     * 1. Parsear el JSON y extraer action_id
     * 2. Verificar que la acción existe en la configuración
     * 3. Verificar que el jugador tiene el permiso requerido (si lo hay)
     * 4. Ejecutar el comando como el jugador
     *
     * @param canal   Canal por el que se recibió el mensaje
     * @param jugador Jugador que envió el mensaje
     * @param datos   Bytes del mensaje (JSON codificado en UTF-8)
     */
    @Override
    public void onPluginMessageReceived(String canal, Player jugador, byte[] datos) {
        // Decodificar los bytes a String UTF-8
        String jsonString = new String(datos, StandardCharsets.UTF_8);

        // Intentar parsear el JSON recibido
        String actionId;
        try {
            JsonObject jsonObjeto = JsonParser.parseString(jsonString).getAsJsonObject();
            actionId = jsonObjeto.get("action_id").getAsString();
        } catch (JsonSyntaxException | NullPointerException | IllegalStateException e) {
            plugin.getLogger().warning(
                    "JSON inválido recibido de " + jugador.getName() + ": " + jsonString);
            return;
        }

        // Validar que el action_id no esté vacío
        if (actionId == null || actionId.isEmpty()) {
            plugin.getLogger().warning(
                    "action_id vacío recibido de " + jugador.getName());
            return;
        }

        // Buscar la acción en el mapa de acciones configuradas
        KeybindAction accion = plugin.obtenerAccionPorId(actionId);

        if (accion == null) {
            // SEGURIDAD: El action_id no existe en la configuración del servidor.
            // Esto podría indicar un cliente manipulado intentando ejecutar acciones no
            // autorizadas.
            plugin.getLogger().warning(
                    "Acción desconocida '" + actionId + "' recibida de " + jugador.getName()
                            + ". Solicitud rechazada.");
            return;
        }

        // Verificar permisos si la acción los requiere
        if (accion.requierePermiso()) {
            if (!jugador.hasPermission(accion.getPermiso())) {
                plugin.getLogger().info(
                        "Jugador " + jugador.getName() + " no tiene permiso '"
                                + accion.getPermiso() + "' para la acción '" + actionId + "'.");
                return;
            }
        }

        // Ejecutar el comando como el jugador
        // Se usa performCommand que ejecuta el comando como si el jugador lo hubiera
        // escrito
        String comando = accion.getComando();

        plugin.getLogger().info(
                "Ejecutando acción '" + actionId + "' para " + jugador.getName()
                        + " -> /" + comando);

        // Ejecutar en el hilo principal del servidor para seguridad con la API de
        // Bukkit
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            jugador.performCommand(comando);
        });
    }
}
