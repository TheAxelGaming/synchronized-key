# 🔑 SynchronizedKey

**Sistema híbrido Plugin + Mod para Minecraft 1.21** que permite configurar teclas dinámicas en el cliente desde el servidor.

Los administradores definen acciones en el `config.yml` del plugin (Spigot/Paper), y los jugadores que tengan el mod (Fabric) recibirán automáticamente esas teclas al conectarse, pudiendo personalizarlas desde el menú de controles nativo de Minecraft.

---

## ✨ Características

- 🎮 **Teclas dinámicas**: Configura keybinds desde el servidor que aparecen en el menú de controles del cliente
- 🔄 **Sincronización automática**: Las teclas se envían al jugador al conectarse (con delay de 2 segundos)
- 🛡️ **Seguridad**: Validación de permisos y verificación de acciones antes de ejecutar comandos
- ♻️ **Recarga en caliente**: Usa `/sk reload` para actualizar la configuración sin reiniciar el servidor
- 🧠 **Persistencia diferencial**: Al hacer reload, se conservan las teclas personalizadas por el jugador
- 🧹 **Limpieza automática**: Al desconectarse, el mod elimina las teclas dinámicas para evitar conflictos con otros servidores
- 📂 **Categoría personalizada**: Las teclas aparecen bajo `Servidor: [nombre]` en el menú de controles

---

## 📦 Estructura del Proyecto

```
synchronized-key/
├── plugin/          → Plugin de Spigot/Paper (lado servidor)
├── mod/             → Mod de Fabric (lado cliente)
├── build.gradle     → Configuración raíz de Gradle
└── settings.gradle  → Multi-módulo Gradle
```

---

## 🔧 Requisitos

| Componente | Versión |
|---|---|
| Minecraft | 1.21 |
| Java | 21 |
| Servidor | Spigot / Paper |
| Cliente | Fabric Loader ≥ 0.16.0 |
| Fabric API | Requerida |

---

## ⚙️ Configuración del Plugin (Servidor)

El archivo `plugin/src/main/resources/config.yml` define las acciones disponibles:

```yaml
acciones:
  - id: "abrir_menu"
    label: "Abrir Menú"
    default_key: 77          # Código GLFW (M)
    comando: "dm open menu_principal"
    permiso: ""              # Vacío = sin restricción

  - id: "abrir_tienda"
    label: "Abrir Tienda"
    default_key: 66          # Código GLFW (B)
    comando: "dm open tienda"
    permiso: ""
```

### Campos de cada acción

| Campo | Descripción |
|---|---|
| `id` | Identificador único (sin espacios) |
| `label` | Nombre visible en el menú de controles del cliente |
| `default_key` | Código de tecla GLFW por defecto |
| `comando` | Comando a ejecutar como el jugador (**sin** la barra `/`) |
| `permiso` | Permiso requerido (dejar `""` para acceso libre) |

### Referencia de teclas GLFW comunes

| Tecla | Código | Tecla | Código |
|---|---|---|---|
| B | 66 | N | 78 |
| G | 71 | P | 80 |
| H | 72 | R | 82 |
| J | 74 | U | 85 |
| K | 75 | V | 86 |
| M | 77 | F1-F12 | 290-301 |

---

## 🕹️ Comandos

| Comando | Permiso | Descripción |
|---|---|---|
| `/sk reload` | `teclaspro.admin` | Recarga la configuración y resincroniza las teclas con todos los jugadores conectados |

---

## 📡 Protocolo de Comunicación

Canal: `teclas_pro:main`

### Servidor → Cliente (Sincronización)

```json
[
  { "id": "abrir_menu", "label": "Abrir Menú", "default_key": 77 },
  { "id": "abrir_tienda", "label": "Abrir Tienda", "default_key": 66 }
]
```

### Cliente → Servidor (Pulsación de tecla)

```json
{ "action_id": "abrir_menu" }
```

### Flujo de validación del servidor

1. Parsear el JSON y extraer `action_id`
2. Verificar que la acción existe en la configuración
3. Verificar que el jugador tiene el permiso requerido
4. Ejecutar el comando como el jugador en el hilo principal

---

## 🛠️ Compilación

```bash
# Compilar todo el proyecto
./gradlew build

# Solo el plugin
./gradlew :plugin:build

# Solo el mod
./gradlew :mod:build
```

Los archivos compilados se generan en:
- **Plugin**: `plugin/build/libs/`
- **Mod**: `mod/build/libs/`

---

## 📥 Instalación

1. **Servidor**: Coloca el `.jar` del plugin en la carpeta `plugins/` de tu servidor Spigot/Paper
2. **Cliente**: Coloca el `.jar` del mod en la carpeta `mods/` del cliente con Fabric + Fabric API
3. Inicia el servidor, edita el `config.yml` generado en `plugins/SynchronizedKey/`, y usa `/sk reload`

---

## 🔄 Flujo de Funcionamiento

```
┌─────────────────┐                          ┌──────────────────┐
│    SERVIDOR      │                          │     CLIENTE      │
│  (Spigot Plugin) │                          │   (Fabric Mod)   │
├─────────────────┤                          ├──────────────────┤
│                  │   PlayerJoinEvent         │                  │
│  Carga config    │ ──── (2s delay) ────────→│  Recibe JSON     │
│  desde YAML      │   JSON con acciones      │  Registra teclas │
│                  │                          │  en controles    │
│                  │                          │                  │
│                  │   Pulsación de tecla      │  Detecta tecla   │
│  Valida acción   │ ←───────────────────────│  presionada      │
│  Verifica permiso│   {"action_id": "..."}   │  Envía action_id │
│  Ejecuta comando │                          │                  │
│                  │                          │                  │
│  /sk reload      │ ──── Resincroniza ─────→│  Actualización   │
│                  │   (diferencial)           │  sin perder      │
│                  │                          │  teclas custom   │
└─────────────────┘                          └──────────────────┘
```

---

## 📝 Licencia

All Rights Reserved

---

## 👤 Autor

**TheAxelGaming**
