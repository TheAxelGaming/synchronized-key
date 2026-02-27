---
trigger: always_on
---

📜 Regla de Proyecto: Sistema de Keybinds Dinámicos (Hybrid Mod-Plugin)
Contexto: Estamos desarrollando un sistema de Minecraft 1.21 que permite configurar teclas en el cliente desde el servidor. El proyecto es multi-módulo (Gradle) con un módulo plugin (Spigot/Paper) y un módulo mod (Fabric).

1. Arquitectura de Comunicación
Canal: El canal de comunicación debe ser teclas_pro:main.

Protocolo: * El Servidor envía al Cliente una lista de objetos JSON: { "id": String, "label": String, "default_key": int }.

El Cliente responde al Servidor con el ID de la acción: { "action_id": String }.

2. Pautas para el Módulo plugin (Spigot)
Gestión de Configuración: Lee las acciones desde un archivo config.yml.

Sincronización: Al unirse un jugador (PlayerJoinEvent), espera 2 segundos y envía el paquete de sincronización con todas las teclas configuradas.

Seguridad: Antes de ejecutar un comando recibido desde el mod, verifica que el action_id existe en la configuración y que el jugador tiene los permisos necesarios.

3. Pautas para el Módulo mod (Fabric)
Registro Dinámico: Utiliza la API de Fabric para registrar KeyBinding en tiempo de ejecución basándote en la lista recibida del servidor.

Interfaz de Usuario: Las teclas deben aparecer en la categoría "Servidor: [Nombre del Server]" dentro del menú de controles nativo de Minecraft.

Persistencia: Si el jugador se desconecta, el mod debe limpiar las teclas dinámicas para evitar conflictos con otros servidores.

4. Estándares de Código
Usa Java 21.

Comenta el código en español para facilitar la gestión.

En el mod, asegúrate de que el envío de paquetes ocurra en el hilo del cliente para evitar crasheos.

Usa nombres de variables descriptivos (ej: dynamicKeyMap en lugar de keys).

5.quiero que por cada build que se compile correctamente cambies la vercion 
p
por ejemplo si estamos en la 1.0.0 si la siguiente build se compilo exitosamente le añadas la sigueinte sea 1.0.1, que balla acendiendo