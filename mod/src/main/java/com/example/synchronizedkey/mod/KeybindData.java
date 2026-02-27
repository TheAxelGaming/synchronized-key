package com.example.synchronizedkey.mod;

/**
 * Modelo de datos que representa una acción de keybind recibida del servidor.
 * Contiene la información necesaria para crear un KeyBinding dinámico.
 *
 * @param id         Identificador único de la acción (ej: "abrir_menu")
 * @param label      Nombre visible para el jugador (ej: "Abrir Menú")
 * @param defaultKey Código de tecla GLFW por defecto (ej: 77 para M)
 */
public record KeybindData(String id, String label, int defaultKey) {
}
