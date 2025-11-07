# HellasAudio Permissions

When LuckPerms is present, HellasAudio exposes granular permission nodes so you can delegate disc management without promoting players to server operator status.

| Node | Default | Description |
| ---- | ------- | ----------- |
| `hellasaudio.upload` | OPs (`permission level >= 2`) | Allows a player to upload custom MP3 tracks to the server library via `/hellas audio upload` or the client-side shortcuts. |
| `hellasaudio.command.play` | OPs (`permission level >= 2`) | Authorizes `/hellas audio play …`, letting staff trigger playback for specific players or the whole server without needing a physical disc. |
| `hellasaudio.command.list` | OPs (`permission level >= 2`) | Grants access to `/hellas audio list`, exposing the stored disc catalog and metadata. |
| `hellasaudio.command.name` | OPs (`permission level >= 2`) | Permits `/hellas audio name …` so moderators can rename discs after upload. |
| `hellasaudio.command.remove` | OPs (`permission level >= 2`) | Required for `/hellas audio remove …`, removing an entry from the library and deleting its assets. |
| `hellasaudio.command.give` | OPs (`permission level >= 2`) | Controls `/hellas audio give …` for handing out custom disc items tagged with the appropriate metadata. |

If LuckPerms is not installed, the mod falls back to vanilla permission levels. Operators with permission level 2 or higher can always perform these actions, while non-operators cannot.
