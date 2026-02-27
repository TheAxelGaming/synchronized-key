package com.example.synchronizedkey.mod;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Punto de entrada principal (común) del mod SynchronizedKey.
 * Este entrypoint se ejecuta tanto en cliente como en servidor.
 * La lógica real del mod está en SynchronizedKeyModClient (solo cliente).
 */
public class SynchronizedKeyMod implements ModInitializer {

    /** Identificador del mod usado en logs y registros */
    public static final String MOD_ID = "synchronizedkey";

    /** Logger compartido del mod */
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // La lógica principal está en SynchronizedKeyModClient (entrypoint client)
        LOGGER.info("SynchronizedKey Mod cargado.");
    }
}
