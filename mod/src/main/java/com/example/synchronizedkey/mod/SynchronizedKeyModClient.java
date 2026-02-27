package com.example.synchronizedkey.mod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Punto de entrada del mod SynchronizedKey en el lado del cliente.
 * Registra el canal de red, los eventos de tick y la limpieza al desconectarse.
 *
 * Flujo principal:
 * 1. El servidor envía la lista de teclas por el canal teclas_pro:main
 * 2. El mod parsea el JSON y registra KeyBindings dinámicos
 * 3. En cada tick se detectan pulsaciones y se envían al servidor
 * 4. Al desconectarse, se limpian las teclas dinámicas
 */
public class SynchronizedKeyModClient implements ClientModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("SynchronizedKey-Client");

    @Override
    public void onInitializeClient() {
        LOGGER.info("Inicializando SynchronizedKey Mod (cliente)...");

        // Crear las instancias de los gestores
        DynamicKeybindManager gestorTeclas = new DynamicKeybindManager();
        KeybindNetworkHandler gestorRed = new KeybindNetworkHandler(gestorTeclas);
        KeybindInputHandler gestorInput = new KeybindInputHandler(gestorTeclas, gestorRed);

        // Paso 1: Registrar los tipos de payload para el canal teclas_pro:main
        // S2C = servidor → cliente (recibir configuración de teclas)
        // C2S = cliente → servidor (enviar action_id de tecla presionada)
        PayloadTypeRegistry.playS2C().register(KeybindPayload.ID, KeybindPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(KeybindPayload.ID, KeybindPayload.CODEC);

        // Paso 2: Registrar el receptor global de paquetes del servidor
        ClientPlayNetworking.registerGlobalReceiver(
                KeybindPayload.ID,
                gestorRed::recibirPaquete);

        // Paso 3: Registrar el callback de tick del cliente para detectar pulsaciones
        ClientTickEvents.END_CLIENT_TICK.register(gestorInput::onClientTick);

        // Paso 4: Registrar la limpieza al desconectarse del servidor
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            LOGGER.info("Desconectado del servidor. Limpiando teclas dinámicas...");
            // Ejecutar en el hilo del cliente para seguridad
            client.execute(gestorTeclas::limpiarTeclasDinamicas);
        });

        LOGGER.info("SynchronizedKey Mod (cliente) inicializado correctamente.");
    }
}
