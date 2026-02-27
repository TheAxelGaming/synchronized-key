package com.example.synchronizedkey.plugin;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Listener que se activa cuando un jugador se une al servidor.
 * Espera 2 segundos (40 ticks) antes de enviar la configuración de teclas
 * al cliente, dando tiempo para que el mod del cliente se inicialice.
 */
public class PlayerJoinListener implements Listener {

    /** Referencia al plugin principal para acceder a las acciones y al scheduler */
    private final SynchronizedKeyPlugin plugin;

    /** Canal de comunicación plugin-mod */
    private static final String CANAL = "teclas_pro:main";

    /** Delay en ticks antes de enviar el paquete (40 ticks = 2 segundos) */
    private static final long DELAY_TICKS = 40L;

    /**
     * Constructor del listener.
     *
     * @param plugin Instancia del plugin principal
     */
    public PlayerJoinListener(SynchronizedKeyPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Maneja el evento de unión de un jugador.
     * Programa una tarea con delay de 40 ticks para enviar
     * la configuración de keybinds al cliente.
     *
     * @param evento Evento de unión del jugador
     */
    @EventHandler
    public void alUnirseJugador(PlayerJoinEvent evento) {
        Player jugador = evento.getPlayer();

        // Programar envío con delay de 2 segundos para dar tiempo al cliente
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // Verificar que el jugador sigue conectado después del delay
            if (!jugador.isOnline()) {
                return;
            }

            enviarConfiguracionTeclas(jugador);
        }, DELAY_TICKS);
    }

    /**
     * Construye el JSON de acciones usando el método centralizado del plugin
     * y lo envía al cliente por el canal registrado.
     *
     * @param jugador Jugador al que se le envía la configuración
     */
    private void enviarConfiguracionTeclas(Player jugador) {
        // Usar el método centralizado del plugin para construir el JSON
        byte[] datos = plugin.construirJsonAcciones();

        // No enviar si no hay acciones configuradas
        if (datos == null) {
            plugin.getLogger().warning("No hay acciones de keybind configuradas para enviar.");
            return;
        }

        // Enviar el paquete al cliente por el canal registrado
        jugador.sendPluginMessage(plugin, CANAL, datos);

        plugin.getLogger().info(
                "Configuración de teclas enviada a " + jugador.getName()
                        + " (" + plugin.obtenerAcciones().size() + " acciones)");
    }
}
