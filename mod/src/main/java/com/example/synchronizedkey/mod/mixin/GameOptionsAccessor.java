package com.example.synchronizedkey.mod.mixin;

import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor Mixin para poder modificar el campo final allKeys de GameOptions.
 * Esto permite añadir y quitar teclas dinámicas en tiempo de ejecución.
 */
@Mixin(GameOptions.class)
public interface GameOptionsAccessor {

    /**
     * Obtiene el array actual de teclas.
     */
    @Accessor("allKeys")
    KeyBinding[] getAllKeys();

    /**
     * Permite asignar un nuevo array a allKeys, saltando la restricción 'final'.
     */
    @Mutable
    @Accessor("allKeys")
    void setAllKeys(KeyBinding[] allKeys);
}
