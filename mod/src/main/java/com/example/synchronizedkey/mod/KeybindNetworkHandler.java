package com.example.synchronizedkey.mod;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Gestiona la comunicación de red entre el mod y el plugin Spigot.
 * Recibe la configuración de teclas del servidor (JSON array) y
 * envía las pulsaciones de teclas de vuelta (JSON object).
 */
public class KeybindNetworkHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("SynchronizedKey-Red");

    /** Referencia al gestor de teclas dinámicas */
    private final DynamicKeybindManager gestorTeclas;

    /**
     * Constructor del gestor de red.
     *
     * @param gestorTeclas Instancia del gestor de teclas dinámicas
     */
    public KeybindNetworkHandler(DynamicKeybindManager gestorTeclas) {
        this.gestorTeclas = gestorTeclas;
    }

    /**
     * Procesa un paquete recibido del servidor por el canal teclas_pro:main.
     * El servidor envía un JSON array con las teclas a registrar:
     * [{"id": "...", "label": "...", "default_key": N}, ...]
     *
     * Este método se ejecuta en el hilo de red de Netty, por lo que
     * delegamos el trabajo al hilo del cliente para seguridad.
     *
     * @param payload Payload con los bytes del JSON
     * @param context Contexto de Fabric Networking
     */
    public void recibirPaquete(KeybindPayload payload, ClientPlayNetworking.Context context) {
        String jsonString = payload.comoTexto();
        LOGGER.info("Paquete de sincronización recibido del servidor: {} bytes", jsonString.length());

        // Parsear el JSON en el hilo de red (operación segura, sin acceso a MC)
        List<KeybindData> listaAcciones;
        try {
            listaAcciones = parsearJsonAcciones(jsonString);
        } catch (JsonSyntaxException e) {
            LOGGER.error("JSON inválido recibido del servidor: {}", jsonString, e);
            return;
        }

        if (listaAcciones.isEmpty()) {
            LOGGER.warn("El servidor envió una lista de acciones vacía.");
            return;
        }

        LOGGER.info("Acciones parseadas correctamente: {}", listaAcciones.size());

        // Obtener el nombre del servidor para la categoría de teclas
        String nombreServidor = obtenerNombreServidor();

        // Ejecutar el registro de teclas en el hilo del cliente (obligatorio)
        MinecraftClient.getInstance().execute(() -> {
            gestorTeclas.registrarTeclasDinamicas(listaAcciones, nombreServidor);
        });
    }

    /**
     * Parsea el JSON array del servidor a una lista de KeybindData.
     *
     * @param jsonString JSON en formato: [{"id":"...", "label":"...",
     *                   "default_key":N}, ...]
     * @return Lista de acciones parseadas
     * @throws JsonSyntaxException Si el JSON es inválido
     */
    private List<KeybindData> parsearJsonAcciones(String jsonString) {
        List<KeybindData> acciones = new ArrayList<>();

        JsonArray arrayJson = JsonParser.parseString(jsonString).getAsJsonArray();

        for (JsonElement elemento : arrayJson) {
            JsonObject objeto = elemento.getAsJsonObject();

            String id = objeto.get("id").getAsString();
            String label = objeto.get("label").getAsString();
            int defaultKey = objeto.get("default_key").getAsInt();

            acciones.add(new KeybindData(id, label, defaultKey));
        }

        return acciones;
    }

    /**
     * Envía una acción de tecla presionada al servidor.
     * Formato enviado: {"action_id": "..."}
     *
     * Se asegura de enviar en el hilo del cliente para evitar crasheos.
     *
     * @param actionId ID de la acción cuya tecla fue presionada
     */
    public void enviarAccionAlServidor(String actionId) {
        // Construir el JSON con GSON
        JsonObject jsonObjeto = new JsonObject();
        jsonObjeto.addProperty("action_id", actionId);
        String jsonString = jsonObjeto.toString();

        LOGGER.info("Enviando acción al servidor: {}", jsonString);

        // Ejecutar el envío en el hilo del cliente
        MinecraftClient.getInstance().execute(() -> {
            try {
                ClientPlayNetworking.send(KeybindPayload.desdeTexto(jsonString));
            } catch (Exception e) {
                LOGGER.error("Error al enviar paquete al servidor: {}", e.getMessage());
            }
        });
    }

    /**
     * Obtiene el nombre del servidor actual para usarlo en la categoría de teclas.
     * Usa la dirección del servidor o "Desconocido" si no está disponible.
     *
     * @return Nombre o dirección del servidor
     */
    private String obtenerNombreServidor() {
        MinecraftClient cliente = MinecraftClient.getInstance();
        if (cliente.getCurrentServerEntry() != null) {
            String nombre = cliente.getCurrentServerEntry().name;
            if (nombre != null && !nombre.isEmpty()) {
                return nombre;
            }
            return cliente.getCurrentServerEntry().address;
        }
        return "Servidor";
    }
}
