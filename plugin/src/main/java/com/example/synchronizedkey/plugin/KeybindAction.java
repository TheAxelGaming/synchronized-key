package com.example.synchronizedkey.plugin;

/**
 * Modelo de datos que representa una acción de keybind configurada en el
 * servidor.
 * Cada acción tiene un identificador único, una etiqueta visible para el
 * jugador,
 * una tecla por defecto (código GLFW), el comando a ejecutar y un permiso
 * opcional.
 */
public class KeybindAction {

    /** Identificador único de la acción (ej: "abrir_menu") */
    private final String id;

    /** Nombre visible de la tecla en el menú de controles del cliente */
    private final String label;

    /** Código de tecla GLFW por defecto (ej: 77 para la tecla M) */
    private final int defaultKey;

    /** Comando que se ejecutará como el jugador al presionar la tecla (sin /) */
    private final String comando;

    /** Permiso requerido para ejecutar la acción (puede ser null o vacío) */
    private final String permiso;

    /**
     * Constructor completo de KeybindAction.
     *
     * @param id         Identificador único de la acción
     * @param label      Nombre visible para el jugador
     * @param defaultKey Código GLFW de la tecla por defecto
     * @param comando    Comando a ejecutar (sin la barra /)
     * @param permiso    Permiso requerido (null o vacío = sin restricción)
     */
    public KeybindAction(String id, String label, int defaultKey, String comando, String permiso) {
        this.id = id;
        this.label = label;
        this.defaultKey = defaultKey;
        this.comando = comando;
        this.permiso = permiso;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public int getDefaultKey() {
        return defaultKey;
    }

    public String getComando() {
        return comando;
    }

    public String getPermiso() {
        return permiso;
    }

    /**
     * Verifica si esta acción requiere un permiso específico.
     *
     * @return true si el permiso no es null ni vacío
     */
    public boolean requierePermiso() {
        return permiso != null && !permiso.isEmpty();
    }
}
