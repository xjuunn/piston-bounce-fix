[简体中文](README_zh.md)

# Piston Bounce Fix

A server-side Fabric mod for Minecraft 26.3 that fixes the vanilla bug where a piston fails to launch players standing on a slime block.

## What it fixes

On Minecraft 26.3, a player standing on a slime block that is pushed by a piston is not launched. The player stays on the block instead of bouncing.

The bug only occurs on multiplayer servers, and it triggers more often on connections with higher latency. Singleplayer is not affected.

## Requirements

- Minecraft 26.3
- Fabric Loader 0.19.5 or newer
- Fabric API

## Installation

Server:

1. Download the `piston-bounce-fix` jar.
2. Place it in the `mods` folder of your server.
3. Restart the server.

Client:

No installation is needed. The mod runs on the server only, so players can connect with an unmodified vanilla or Fabric client.