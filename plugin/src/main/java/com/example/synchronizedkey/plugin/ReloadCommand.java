package com.example.synchronizedkey.plugin;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Ejecutor del comando /sk reload.
 * Recarga el config.yml, reconstruye el JSON de acciones y envía
 * la nueva configuración a todos los jugadores conectados.
 *
 * Permiso requerido: teclaspro.admin
 */
public class ReloadCommand implements CommandExecutor {

    /** Referencia al plugin principal para acceder a métodos de recarga y envío */
    private final SynchronizedKeyPlugin plugin;

    /**
     * Constructor del comando de recarga.
     *
     * @param plugin Instancia del plugin principal
     */
    public ReloadCommand(SynchronizedKeyPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Maneja la ejecución del comando /sk.
     * Subcomandos soportados:
     * - reload: recarga config y sincroniza con todos los jugadores
     *
     * @param sender  Quien ejecuta el comando
     * @param command Objeto del comando
     * @param label   Alias usado
     * @param args    Argumentos del comando
     * @return true si el comando fue procesado
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Verificar que se proporcionó un subcomando
        if (args.length == 0) {
            sender.sendMessage("§e[SynchronizedKey] §fUso: /sk reload");
            return true;
        }

        // Procesar el subcomando "reload"
        if (args[0].equalsIgnoreCase("reload")) {
            ejecutarReload(sender);
            return true;
        }

        // Subcomando desconocido
        sender.sendMessage("§e[SynchronizedKey] §cSubcomando desconocido. Uso: /sk reload");
        return true;
    }

    /**
     * Ejecuta la lógica de recarga:
     * 1. Recarga el archivo config.yml desde disco
     * 2. Reconstruye el mapa de acciones interno
     * 3. Construye el JSON de sincronización
     * 4. Envía el paquete a todos los jugadores conectados
     *
     * El envío se realiza en el hilo principal ya que sendPluginMessage
     * debe ejecutarse en el hilo del servidor de Bukkit.
     *
     * @param sender Quien ejecutó el comando (para enviar mensajes de estado)
     */
    private void ejecutarReload(CommandSender sender) {
        // Paso 1: Recargar el config.yml desde disco
        plugin.reloadConfig();

        // Paso 2: Reconstruir el mapa de acciones con la nueva configuración
        plugin.cargarAccionesDesdeConfig();

        int cantidadAcciones = plugin.obtenerAcciones().size();
        sender.sendMessage("§e[SynchronizedKey] §aConfiguración recargada. §f"
                + cantidadAcciones + " acciones cargadas.");

        // Paso 3: Construir los bytes JSON para enviar
        byte[] datosJson = plugin.construirJsonAcciones();

        if (datosJson == null) {
            sender.sendMessage("§e[SynchronizedKey] §cNo hay acciones configuradas para enviar.");
            return;
        }

        // Paso 4: Enviar a todos los jugadores conectados
        // sendPluginMessage debe ejecutarse en el hilo principal,
        // y este comando ya se ejecuta en el hilo principal.
        int jugadoresEnviados = 0;
        for (Player jugador : Bukkit.getOnlinePlayers()) {
            jugador.sendPluginMessage(plugin, SynchronizedKeyPlugin.CANAL, datosJson);
            jugadoresEnviados++;
        }

        sender.sendMessage("§e[SynchronizedKey] §aSincronización enviada a §f"
                + jugadoresEnviados + " §ajugador(es) conectado(s).");

        plugin.getLogger().info("Reload ejecutado por " + sender.getName()
                + ". Acciones: " + cantidadAcciones
                + ", Jugadores sincronizados: " + jugadoresEnviados);
    }
}
