package com.example.synchronizedkey.mod.mixin;

import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * Accessor Mixin para acceder y modificar el mapa estático CATEGORY_ORDER_MAP
 * de
 * KeyBinding.
 * Esto permite registrar categorías dinámicas con una prioridad de orden,
 * evitando NullPointerException al ordenar categorías en el menú de controles.
 *
 * Se incluye un setter @Mutable para poder reemplazar el mapa completo
 * en caso de que sea inmutable (ej: Collections.unmodifiableMap).
 */
@Mixin(KeyBinding.class)
public interface KeyBindingAccessor {

    /**
     * Obtiene el mapa estático de orden de categorías.
     * Clave: nombre de la categoría, Valor: prioridad de orden (entero).
     */
    @Accessor("CATEGORY_ORDER_MAP")
    static Map<String, Integer> getCategoryOrderMap() {
        throw new AssertionError("Mixin no aplicado");
    }

    /**
     * Permite reemplazar el mapa estático CATEGORY_ORDER_MAP completo.
     * Necesario si el mapa original es inmutable y no acepta put().
     *
     * @param mapaOrden Nuevo mapa mutable de orden de categorías
     */
    @Accessor("CATEGORY_ORDER_MAP")
    @Mutable
    static void setCategoryOrderMap(Map<String, Integer> mapaOrden) {
        throw new AssertionError("Mixin no aplicado");
    }
}
