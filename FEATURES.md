# HellasAudio

HellasAudio handles the full lifecycle of custom MP3 music discs for the Hellas network. Players can upload their own
tracks, staff can curate those discs via commands, and the mod streams the audio from server storage back to listening
clients. It focuses purely on distribution and metadata—the actual playback is delegated to the Hellas audio pipeline
(HellasControl and other suite components).

## Feature Overview
- **Client upload workflow** – Players can run `/hellas audio upload <disc_id> <path>` to send MP3 files directly from
their client to the server. The client helper validates the file, applies size caps, and manages upload/cache folders.
- **Server-side storage & metadata** – Uploaded discs are written to `${.minecraft}/hellasaudio/discs` and mirrored in a
JSON metadata file so display names, uploader UUIDs, and timestamps survive restarts.
- **Custom disc item** – `CustomDiscItem` stores the disc identifier inside NBT, exposes informative tooltips, and tells
`MusicDiscManager` to broadcast the associated audio when used.
- **Staff tooling and permissions** – `/hellas audio` commands let authorized users list, rename, remove, give, and play
discs. Permission checks fall back to vanilla op levels but integrate with LuckPerms when present.
- **Network streaming** – `DiscUploadMessage` and `DiscPlaybackMessage` move MP3 payloads between clients and the server.
Cached files are stored locally and announced through chat so that the existing Hellas audio renderer can pick them up.

## Technical Overview
- **Entry point (`HellasAudio`)** – Registers common/client setup hooks, enforces entitlement through HellasControl, and
initializes the deferred item registry plus networking.
- **Client package** – Contains `HellasAudioClient` for file handling, `ClientCommandHandler` for intercepting upload
commands before they hit chat, and `ClientDiscLibrary` as a cache and notification layer for received discs.
- **Server package** – `MusicDiscManager` owns all metadata, file storage, and playback logic, while
`ServerEventHandler`/`MusicDiscCommands` wire the Brigadier command tree. Optional permission support lives in
`server.permission`.
- **Networking** – `NetworkHandler` defines a SimpleChannel used by the two packet types in `network.message`. Those
packets are symmetrical (upload vs. playback) and are registered during common setup.

## Extension Points
- **Adding new moderation or playback commands** – Extend `MusicDiscCommands` or create additional literals under the
existing `/hellas audio` tree. `attachAudioCommands` shows how to combine permission checks, suggestions, and handlers.
- **Granting discs from other systems** – Call `MusicDiscManager.createDiscItem(String)` to build a `CustomDiscItem`
with the correct NBT tags, then give it to a player via `ItemHandlerHelper`.
- **Integrating alternative audio renderers** – Hook into `ClientDiscLibrary.queuePlayback` or the cached files it
stores. Each entry maps disc IDs to the resolved `Path`, making it straightforward to hand the MP3 to another pipeline.
- **Adding upload rules** – `MusicDiscManager.handleUpload` centralizes validation. Additional checks (duration limits,
file signatures, etc.) can be inserted there so every upload path shares the same guard rails.

## Dependencies & Environment
- Minecraft **1.16.5** and Forge **36.2.42** (see `gradle.properties`).
- Requires **HellasControl** for entitlement/licensing checks; the mod refuses to start if it is missing.
- Optional integration with **LuckPerms** to mirror server-side permission nodes. Without LuckPerms the mod falls back to
standard operator levels.
- Other Hellas mods can depend on the exposed `CustomDiscItem` and command hooks but no additional runtime deps exist in
this repository.

## Migration Notes
- The networking layer is tightly coupled to Forge's `SimpleChannel` API and raw packet buffers. Future Minecraft or
NeoForge updates that alter networking will require revisiting `NetworkHandler` and the encode/decode helpers in
`network.message`.
- Music storage is written directly to the filesystem using `FMLPaths.GAMEDIR`. Server directory layout changes or
sandboxed environments will need updates in `MusicDiscManager`.
- Permission checks rely on LuckPerms' current API surface through reflection. Should LuckPerms rename its provider
classes, `PermissionHandler` will need to be updated to match.
- Client workflows assume access to the game directory to create `hellasaudio/uploads` and `hellasaudio/cache`. If that
access disappears (e.g., due to launcher sandboxing), the logic inside `HellasAudioClient` will need rework.
