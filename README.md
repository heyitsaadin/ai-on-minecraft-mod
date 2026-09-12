# AI on Minecraft

A Fabric mod for Minecraft 26.2 that adds an AI companion to your in-game chat — no commands needed to talk to it, it just responds when you type. It remembers recent conversation and reacts on its own to advancements, low health, and notable mob kills.

## Commands

| Command | Description |
|---|---|
| `/aion config` | Opens the in-game settings screen (AI provider, endpoint/model/API key, and which ambient events it reacts to). |

There's no separate command to talk to the AI — just type normally in chat and it replies.

## Settings (`/aion config`)

- **AI provider** — `Free (built-in)` uses the mod's hosted proxy with no key required. The two `Custom` options let you point the mod at your own OpenAI-compatible endpoint or an NVIDIA NIM endpoint.
- **Custom endpoint URL / model / API key** — only used when the provider above is set to one of the Custom options; ignored on Free.
- **React to advancements / low health / notable mob kills** — toggle which ambient events trigger an unprompted reply from the AI.
- **Low health threshold** — the health fraction (0.05–0.95) below which the mod considers the player "low health."

Settings are saved to `config/aionminecraft.json`.

## Requirements

- Minecraft 26.2
- Fabric Loader 0.19.5+
- Fabric API 0.160.0+26.2
- Cloth Config 26.2.155 (bundled as a dependency, pulled in automatically)
