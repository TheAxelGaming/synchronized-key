package com.example.synchronizedkey.mod;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.nio.charset.StandardCharsets;

/**
 * Payload personalizado para el canal teclas_pro:main.
 * Envuelve datos crudos (bytes UTF-8) para compatibilidad con el
 * plugin Spigot que envía/recibe JSON como bytes planos.
 */
public record KeybindPayload(byte[] datos) implements CustomPayload {

    /** Identificador del canal de comunicación bidireccional */
    public static final Identifier CANAL_ID = Identifier.of("teclas_pro", "main");

    /** ID tipado del payload para registro en Fabric Networking */
    public static final Id<KeybindPayload> ID = new Id<>(CANAL_ID);

    /**
     * Codec que lee/escribe bytes crudos sin prefijo de longitud.
     * Necesario para compatibilidad con Spigot que envía bytes planos.
     */
    public static final PacketCodec<RegistryByteBuf, KeybindPayload> CODEC = new PacketCodec<>() {
        @Override
        public void encode(RegistryByteBuf buf, KeybindPayload payload) {
            // Escribir los bytes crudos al buffer sin prefijo
            buf.writeBytes(payload.datos);
        }

        @Override
        public KeybindPayload decode(RegistryByteBuf buf) {
            // Leer todos los bytes restantes del buffer (datos crudos de Spigot)
            byte[] data = new byte[buf.readableBytes()];
            buf.readBytes(data);
            return new KeybindPayload(data);
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    /**
     * Convierte los bytes del payload a String UTF-8.
     *
     * @return El contenido como cadena de texto
     */
    public String comoTexto() {
        return new String(datos, StandardCharsets.UTF_8);
    }

    /**
     * Crea un KeybindPayload a partir de un String UTF-8.
     *
     * @param texto El texto a convertir
     * @return Nuevo payload con los bytes del texto
     */
    public static KeybindPayload desdeTexto(String texto) {
        return new KeybindPayload(texto.getBytes(StandardCharsets.UTF_8));
    }
}
