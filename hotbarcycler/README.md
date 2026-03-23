# Hotbar Cycler — Fabric Mod for Minecraft 1.21.10

Cycle through all four rows of your player inventory as if each one were a hotbar, using two assignable keybindings.

---

## How It Works

Your inventory has 36 "main" slots arranged in 4 rows of 9:

```
[ Row 3 ]  slots 27-35   ← virtual hotbar 4
[ Row 2 ]  slots 18-26   ← virtual hotbar 3
[ Row 1 ]  slots  9-17   ← virtual hotbar 2
[Hotbar ]  slots  0-8    ← virtual hotbar 1  (active by default)
```

Pressing **Next Hotbar Row** swaps the current hotbar with the next row (wraps around).  
Pressing **Previous Hotbar Row** swaps back.

The items **physically swap** — the server inventory is updated — so the correct items appear in your hotbar slot bar and can be used immediately.

An action-bar message ("Hotbar Row X / 4") briefly confirms which row is active each time you cycle.

---

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for Minecraft **1.21.10**.
2. Download [Fabric API](https://modrinth.com/mod/fabric-api) for 1.21.10 and place it in your `mods/` folder.
3. Build this mod (`./gradlew build`) and place the resulting `.jar` from `build/libs/` into your `mods/` folder.
4. Launch Minecraft.

---

## Keybinding Setup

1. Open **Options → Controls → Key Binds**.
2. Search for **"Hotbar Cycler"**.
3. Assign **Next Hotbar Row** and **Previous Hotbar Row** to your preferred keys.

Both bindings are **unbound by default**.

---

## Building from Source

```bash
git clone <this-repo>
cd hotbarcycler
./gradlew build
# Output: build/libs/hotbarcycler-1.0.0.jar
```

Requires JDK 21+.

---

## Compatibility

- Minecraft: **1.21.10**
- Loader: **Fabric** ≥ 0.14.0
- Fabric API: required

Works in **singleplayer** and on **multiplayer servers** that have the mod installed.

---

## Technical Notes

**Why does the server also need this mod for multiplayer?**  
Inventory swaps must happen server-side so they persist and sync correctly. The client sends a lightweight C2S packet (`hotbarcycler:cycle`) containing the target slot row; the server performs the 9-slot swap and syncs the result back.

**Will my items be safe if I log out mid-cycle?**  
Yes. The swap is a real inventory modification, so the current state is always saved correctly.
