# NeoForge Decompiler

Console tool that pulls any NeoForge version and decompiles **everything**
(all Minecraft classes + all NeoForge classes) into a plain folder database
of readable `.java` source files.

```
versions/26.3.0.56-beta/
    net/minecraft/...      <- every Minecraft class, deobfuscated
    net/neoforged/...      <- every NeoForge class (patched sources)
    com/mojang/...         <- Mojang data / brigadier / etc.
    _meta.json             <- what was pulled, how many files
```

## Requirements

- **Java 21+** (recommended) — enables the NeoFormRuntime pipeline, the exact
  same deobfuscate + decompile the NeoForge MDK uses. Get it from
  https://adoptium.net (Temurin 21 JRE/JDK). Just installing it normally and
  leaving "Set JAVA_HOME" checked is enough — the script also looks in the
  usual install folders if `java` is not on PATH.
- **Java 17** works too — it falls back to official Mojang mappings +
  SpecialSource remap + Vineflower slice decompile (same output quality for
  Minecraft, slightly different formatting).
- Python 3.10+ on PATH (Windows: just run `Decompile.bat`).
- ~8 GB free disk + a few GB RAM. First run downloads ~150 MB of tools and
  the Minecraft jar; everything is cached in `tools/cache/` so re-runs and
  other versions are fast.

## Usage

**Windows:** double-click `Decompile.bat` — you get a list of the newest 20
NeoForge versions, pick a number (or type any full version like `21.1.65`).

**Or directly:**

```
python nfdecompile.py              interactive version picker
python nfdecompile.py run 21.1.65  decompile a specific NeoForge version
python nfdecompile.py versions     list all available versions
```

A full Minecraft decompile takes 10-40 minutes depending on your machine
(the NFRT pipeline decompiles ~10k classes). Progress is logged live.
If you interrupt (Ctrl+C), partial output is kept and a re-run resumes from
cache.

## What you get

Everything lands in `versions/<neoforge-version>/`:

| Source | Content |
|---|---|
| NeoForge `sources.jar` | all `net/neoforged/**` classes, pre-patched by NeoForge themselves |
| NFRT `gameSourcesWithNeoForge` (Java 21) | all `net/minecraft/**` classes decompiled with ForgeFlower, **with NeoForge's patches already applied** — exactly what MCreator-style modding tools show |
| fallback (Java 17) | Mojang official mappings remap + Vineflower, per-package slices so big jars don't OOM |

Third-party libraries (Guava, fastutil, LWJGL, …) are *not* decompiled —
those are open-source projects with their own sources; ask if you want a
mode that pulls their source jars too.

## Files

- `nfdecompile.py` — the whole tool (stdlib only, no pip packages needed)
- `Decompile.bat` — Windows launcher (prefers the `py` launcher)
- `decompile.sh` — Linux/macOS launcher
- `tools/` — caches (downloads, jars, status.json) — safe to delete anytime
