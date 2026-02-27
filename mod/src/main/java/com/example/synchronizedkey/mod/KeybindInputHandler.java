package com.example.synchronizedkey.mod;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Detecta las pulsaciones de teclas dinámicas en cada tick del cliente.
 * Cuando se detecta una pulsación, envía el action_id correspondiente
 * al servidor a través del KeybindNetworkHandler.
 *
 * Se registra como callback de END_CLIENT_TICK en Fabric API.
 */
public class KeybindInputHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("SynchronizedKey-Input");

    /** Referencia al gestor de teclas para obtener las teclas activas */
    private final DynamicKeybindManager gestorTeclas;

    /** Referencia al gestor de red para enviar acciones al servidor */
    private final KeybindNetworkHandler gestorRed;

    /**
     * Constructor del gestor de input.
     *
     * @param gestorTeclas Gestor de teclas dinámicas
     * @param gestorRed    Gestor de comunicación de red
     */
    public KeybindInputHandler(DynamicKeybindManager gestorTeclas, KeybindNetworkHandler gestorRed) {
        this.gestorTeclas = gestorTeclas;
        this.gestorRed = gestorRed;
    }

    /**
     * Callback ejecutado al final de cada tick del cliente.
     * Comprueba si alguna tecla dinámica fue presionada y, de ser así,
     * envía la acción correspondiente al servidor.
     *
     * Solo procesa teclas si:
     * - Hay teclas dinámicas activas
     * - El jugador está en un mundo (conectado a un servidor)
     * - No está en una pantalla/menú abierto
     *
     * @param cliente Instancia del cliente de Minecraft
     */
    public void onClientTick(MinecraftClient cliente) {
        // No procesar si no hay teclas dinámicas activas
        if (!gestorTeclas.tieneTeclasActivas()) {
            return;
        }

        // No procesar si el jugador no está en un mundo
        if (cliente.player == null || cliente.world == null) {
            return;
        }

        // Iterar sobre las teclas dinámicas y comprobar pulsaciones
        Map<String, KeyBinding> mapaTeclas = gestorTeclas.obtenerDynamicKeyMap();

        for (Map.Entry<String, KeyBinding> entrada : mapaTeclas.entrySet()) {
            String actionId = entrada.getKey();
            KeyBinding tecla = entrada.getValue();

            // wasPressed() consume la pulsación (evita envíos duplicados)
            while (tecla.wasPressed()) {
                LOGGER.debug("Tecla presionada: '{}' (action_id: {})", tecla.getTranslationKey(), actionId);
                gestorRed.enviarAccionAlServidor(actionId);
            }
        }
    }
}
