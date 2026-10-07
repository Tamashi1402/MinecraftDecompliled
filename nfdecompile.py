#!/usr/bin/env python3
"""NeoForge decompiler - console tool.

Pulls a NeoForge version, deobfuscates Minecraft and decompiles EVERYTHING
(NeoForge + all Minecraft classes) into versions/<neoforge-version>/ as a
plain folder database of .java files:

    versions/26.3.0.56-beta/
        net/minecraft/...
        net/neoforged/...
        com/mojang/...
        _meta.json

Usage:
    python nfdecompile.py                 interactive version picker
    python nfdecompile.py run <version>    decompile a specific version
    python nfdecompile.py versions         list available NeoForge versions

Best output is produced with Java 21+ (uses NeoFormRuntime, the same
pipeline the NeoForge MDK uses). With Java 17 it falls back to official
mappings + SpecialSource remap + Vineflower slice decompile.
"""
from __future__ import annotations

import glob
import json
import os
import re
import shutil
import subprocess
import sys
import time
import urllib.request
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent
VERSIONS = ROOT / "versions"
CACHE = ROOT / "tools" / "cache"
DATA = ROOT / "tools" / "data"
NFRT_HOME = ROOT / "tools" / "nfrt-home"

VINEFLOWER = CACHE / "vineflower-1.12.0.jar"
SPECIALSOURCE = CACHE / "SpecialSource-1.11.4-shaded.jar"
NFRT = CACHE / "neoform-runtime-2.0.31-all.jar"

VINEFLOWER_URL = "https://repo1.maven.org/maven2/org/vineflower/vineflower/1.12.0/vineflower-1.12.0.jar"
SPECIALSOURCE_URL = "https://repo1.maven.org/maven2/net/md-5/SpecialSource/1.11.4/SpecialSource-1.11.4-shaded.jar"
NFRT_URL = "https://maven.neoforged.net/releases/net/neoforged/neoform-runtime/2.0.31/neoform-runtime-2.0.31-all.jar"
NF_META = "https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml"
NF_MAVEN = "https://maven.neoforged.net/releases/net/neoforged/neoforge"
MOJANG_MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"

UA = {"User-Agent": "nfdecompile/1.1"}
STATUS_PATH = DATA / "status.json"
VER_RE = re.compile(r"^[0-9A-Za-z.+_-]+$")


def log(msg: str) -> None:
    print(f"[{time.strftime('%H:%M:%S')}] {msg}", flush=True)


def write_status(**fields: object) -> None:
    DATA.mkdir(parents=True, exist_ok=True)
    current: dict = {}
    if STATUS_PATH.exists():
        try:
            current = json.loads(STATUS_PATH.read_text())
        except json.JSONDecodeError:
            current = {}
    current.update(fields)
    current["files"] = count_java(Path(current["outDir"])) if current.get("outDir") else current.get("files", 0)
    tmp = STATUS_PATH.with_suffix(".tmp")
    tmp.write_text(json.dumps(current, indent=2))
    tmp.replace(STATUS_PATH)


def count_java(folder: Path) -> int:
    if not folder.is_dir():
        return 0
    n = 0
    for _root, _dirs, files in os.walk(folder):
        n += sum(1 for f in files if f.endswith(".java"))
    return n


def http_get(url: str, dest: Path | None = None) -> bytes:
    req = urllib.request.Request(url, headers=UA)
    with urllib.request.urlopen(req, timeout=180) as resp:
        data = resp.read()
    if dest is not None:
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_bytes(data)
    return data


def ensure_tool(path: Path, url: str) -> None:
    if path.exists() and path.stat().st_size > 10_000:
        log(f"tool ready {path.name}")
        return
    log(f"download {path.name}")
    http_get(url, path)
    log(f"saved {path.name} ({path.stat().st_size} bytes)")


def maven_jar(group_path: str, artifact: str, version: str, classifier: str | None = None) -> str:
    name = f"{artifact}-{version}" + (f"-{classifier}" if classifier else "") + ".jar"
    return f"{group_path}/{version}/{name}"


# ---------------------------------------------------------------- java setup

def find_java() -> str | None:
    exe = shutil.which("java")
    if exe:
        return exe
    if os.name == "nt":
        if os.environ.get("JAVA_HOME"):
            cand = Path(os.environ["JAVA_HOME"]) / "bin" / "java.exe"
            if cand.exists():
                return str(cand)
        pats = [
            r"C:\Program Files\Eclipse Adoptium\jdk-21*\bin\java.exe",
            r"C:\Program Files\Java\jdk-21*\bin\java.exe",
            r"C:\Program Files\Eclipse Adoptium\jdk-17*\bin\java.exe",
            r"C:\Program Files\Java\jdk-17*\bin\java.exe",
            r"C:\Program Files\Microsoft\jdk-21*\bin\java.exe",
        ]
        for pat in pats:
            hits = glob.glob(pat)
            if hits:
                return sorted(hits)[-1]
    return None


JAVA = None  # set in cmd_run


def run_java(args: list[str], cwd: Path | None = None) -> int:
    log("+ java " + " ".join(os.path.basename(a) if i == 1 else a for i, a in enumerate(args[1:8])) + (" …" if len(args) > 8 else ""))
    proc = subprocess.Popen(
        args,
        cwd=str(cwd) if cwd else None,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        bufsize=1,
    )
    assert proc.stdout is not None
    for line in proc.stdout:
        print(line.rstrip(), flush=True)
    return proc.wait()


def java_major() -> int:
    if JAVA is None:
        return 0
    p = subprocess.run([JAVA, "-version"], capture_output=True, text=True)
    text = p.stderr + p.stdout
    m = re.search(r'version "(\d+)', text)
    return int(m.group(1)) if m else 0


# ------------------------------------------------------------- decompression

def extract_java_sources(jar: Path, dest: Path) -> int:
    n = 0
    with zipfile.ZipFile(jar) as zf:
        for name in zf.namelist():
            if not name.endswith(".java") or name.startswith("META-INF/"):
                continue
            target = dest / name
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(zf.read(name))
            n += 1
            if n % 400 == 0:
                log(f"  extracted {n} java files…")
    return n


def jar_has_prefix(jar: Path, prefix: str) -> bool:
    with zipfile.ZipFile(jar) as zf:
        return any(n.startswith(prefix) and n.endswith(".class") for n in zf.namelist())


def mc_version_from_neoform(mcp: str) -> str:
    ver = mcp.split(":")[-1].split("@")[0]
    if ver.startswith("1."):
        return ver.split("-")[0]
    if re.match(r"^\d+\.\d+-\d+$", ver):
        return ver.rsplit("-", 1)[0]
    return ver.split("-")[0]


# -------------------------------------------------------------- version list

def load_userdev_config(version: str) -> dict:
    dest = CACHE / f"neoforge-{version}-userdev.jar"
    url = maven_jar(NF_MAVEN, "neoforge", version, "userdev")
    log(f"userdev {url}")
    if not dest.exists():
        http_get(url, dest)
    with zipfile.ZipFile(dest) as zf:
        cfg = json.loads(zf.read("config.json"))
    return cfg


def fetch_mc_client(mc_version: str) -> tuple[Path, Path | None]:
    log(f"mojang manifest for {mc_version}")
    manifest = json.loads(http_get(MOJANG_MANIFEST))
    entry = next((v for v in manifest["versions"] if v["id"] == mc_version), None)
    if entry is None:
        entry = next((v for v in manifest["versions"] if v["id"].startswith(mc_version)), None)
    if entry is None:
        raise SystemExit(f"Minecraft {mc_version} not in Mojang manifest")
    meta = json.loads(http_get(entry["url"]))
    downloads = meta["downloads"]
    client_path = CACHE / f"mc-{mc_version}-client.jar"
    if not client_path.exists():
        log(f"minecraft client {downloads['client']['url']}")
        http_get(downloads["client"]["url"], client_path)
    mappings_path = None
    if "client_mappings" in downloads:
        mappings_path = CACHE / f"mc-{mc_version}-client.txt"
        if not mappings_path.exists():
            log("official mappings")
            http_get(downloads["client_mappings"]["url"], mappings_path)
    return client_path, mappings_path


def remap(client: Path, mappings: Path, mapped: Path) -> None:
    if mapped.exists() and mapped.stat().st_mtime >= client.stat().st_mtime:
        log(f"reuse mapped jar {mapped.name}")
        return
    log("remap notch -> official names (SpecialSource)")
    code = run_java([JAVA, "-Xmx1G", "-jar", str(SPECIALSOURCE), "--reverse", "-q",
                     "-i", str(client), "-o", str(mapped), "-m", str(mappings)])
    if code != 0 or not mapped.exists():
        raise SystemExit(f"SpecialSource failed ({code})")


# ------------------------------------------- vineflower fallback (java 17 ok)

def slice_class_jars(src: Path, dest_dir: Path, max_classes: int = 280) -> list[Path]:
    dest_dir.mkdir(parents=True, exist_ok=True)
    buckets: dict[str, list[str]] = {}
    with zipfile.ZipFile(src) as zf:
        names = [n for n in zf.namelist() if n.endswith(".class") and not n.startswith("META-INF/")]
        for name in names:
            pkg = name.rsplit("/", 1)[0] if "/" in name else "_root"
            parts = pkg.split("/")
            key = "/".join(parts[:3]) if len(parts) >= 3 else pkg
            buckets.setdefault(key, []).append(name)

        for _ in range(8):
            oversized = [k for k, items in buckets.items() if len(items) > max_classes]
            if not oversized:
                break
            for key in oversized:
                items = buckets.pop(key)
                depth = key.count("/") + 2
                sub: dict[str, list[str]] = {}
                for name in items:
                    pkg = name.rsplit("/", 1)[0] if "/" in name else "_root"
                    parts = pkg.split("/")
                    nkey = "/".join(parts[: min(depth, len(parts))]) or "_root"
                    sub.setdefault(nkey, []).append(name)
                if len(sub) == 1:
                    buckets[key] = items
                    continue
                for sk, sv in sub.items():
                    buckets.setdefault(sk, []).extend(sv)

        packed: list[list[str]] = []
        small: list[str] = []
        for key, items in sorted(buckets.items(), key=lambda kv: -len(kv[1])):
            if len(items) >= 80:
                packed.append(items)
            else:
                small.extend(items)
                if len(small) >= 220:
                    packed.append(small)
                    small = []
        if small:
            packed.append(small)

        written: list[Path] = []
        for i, items in enumerate(packed):
            outj = dest_dir / f"slice-{i:03d}.jar"
            with zipfile.ZipFile(outj, "w") as o:
                for n in items:
                    o.writestr(n, zf.read(n))
            written.append(outj)
    log(f"split {src.name} into {len(written)} class slices (max ~{max_classes}/slice)")
    return written


def vineflower_one(jar: Path, dest: Path) -> int:
    dest.mkdir(parents=True, exist_ok=True)
    return run_java([
        JAVA, "-Xmx1G", "-jar", str(VINEFLOWER),
        "-dgs=1", "-hdc=0", "-asc=1", "-rsy=1", "-s", "--folder",
        str(jar), str(dest),
    ])


def decompile_jar(jar: Path, out: Path) -> None:
    work = CACHE / "vf-work" / jar.stem
    if work.exists():
        shutil.rmtree(work)
    slices_dir = work / "slices"
    src_dir = work / "src"
    slices = slice_class_jars(jar, slices_dir)
    total = 0
    for i, sl in enumerate(slices, 1):
        piece = src_dir / sl.stem
        log(f"vineflower slice {i}/{len(slices)} {sl.name}")
        code = vineflower_one(sl, piece)
        if code in (-9, 137):
            log(f"  OOM on {sl.name}, skipped")
            continue
        if code != 0:
            log(f"  vineflower exited {code} on {sl.name}")
        copied = 0
        if piece.exists():
            for src in piece.rglob("*.java"):
                rel = src.relative_to(piece)
                dest = out / rel
                dest.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(src, dest)
                copied += 1
        total += copied
        log(f"  +{copied} java (running total {total})")
        write_status(step="minecraft-vineflower")
    log(f"copied {total} decompiled java files into {out}")
    shutil.rmtree(work, ignore_errors=True)


# ------------------------------------------------- nfrt (java 21+, best path)

def try_nfrt(version: str, out: Path) -> bool:
    if java_major() < 21:
        log("nfrt skipped (needs Java 21) - using mappings + vineflower fallback")
        return False
    if not NFRT.exists() or NFRT.stat().st_size < 10_000:
        return False
    src_jar = CACHE / f"nfrt-{version}-sources.jar"
    log("nfrt: full NeoForge pipeline (deobf + patches + decompile), this takes a while")
    NFRT_HOME.mkdir(parents=True, exist_ok=True)
    code = run_java([
        JAVA, "-Xmx4G", "-jar", str(NFRT),
        "run", "--dist", "joined",
        "--neoforge", f"net.neoforged:neoforge:{version}:userdev",
        f"--write-result=gameSourcesWithNeoForge:{src_jar}",
        "--home-dir", str(NFRT_HOME),
        "--disable-cache-maintenance", "--no-emojis",
    ])
    if code != 0 or not src_jar.exists():
        log(f"nfrt failed ({code}), falling back to mappings + vineflower")
        return False
    n = extract_java_sources(src_jar, out)
    log(f"nfrt unpacked {n} sources")
    return n > 0


# ------------------------------------------------------------------ commands

def fetch_version_list() -> list[str]:
    root = ET.fromstring(http_get(NF_META))
    versions = []
    for node in root.findall(".//version"):
        v = (node.text or "").strip()
        if not v or re.fullmatch(r"\d{14}", v) or not re.match(r"^\d+\.\d+", v):
            continue
        versions.append(v)
    versions.reverse()  # newest first
    return versions


def cmd_versions() -> None:
    for v in fetch_version_list():
        print(v)


def pick_interactive() -> str:
    versions = fetch_version_list()
    log(f"{len(versions)} NeoForge versions available. Newest 20:")
    for i, v in enumerate(versions[:20], 1):
        print(f"  {i:2}. {v}")
    raw = input("pick a number (or type a full version like 21.1.65): ").strip()
    if not raw:
        raise SystemExit("no version picked")
    if raw.isdigit():
        idx = int(raw)
        if 1 <= idx <= len(versions):
            return versions[idx - 1]
        raise SystemExit(f"no version number {idx} (1..{min(len(versions), 20)} shown, {len(versions)} total)")
    return raw


def cmd_run(version: str) -> None:
    global JAVA
    JAVA = find_java()
    if JAVA is None:
        raise SystemExit(
            "Java not found. Install Java 21 (Adoptium: https://adoptium.net)\n"
            "and make sure 'java' is on your PATH (or set JAVA_HOME)."
        )
    major = java_major()
    log(f"java {major} at {JAVA}")

    if not VER_RE.match(version):
        raise SystemExit("invalid version")
    VERSIONS.mkdir(parents=True, exist_ok=True)
    CACHE.mkdir(parents=True, exist_ok=True)
    DATA.mkdir(parents=True, exist_ok=True)
    out = VERSIONS / version
    out.mkdir(parents=True, exist_ok=True)

    write_status(running=True, version=version, step="init", error=None,
                 outDir=str(out), startedAt=time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
                 pid=os.getpid())
    log(f"=== NeoForge {version} ===")
    log(f"output {out}")

    write_status(step="tools")
    ensure_tool(VINEFLOWER, VINEFLOWER_URL)
    ensure_tool(SPECIALSOURCE, SPECIALSOURCE_URL)
    try:
        ensure_tool(NFRT, NFRT_URL)
    except Exception as exc:
        log(f"nfrt download skipped: {exc}")

    write_status(step="userdev")
    cfg = load_userdev_config(version)
    mcp = cfg.get("mcp") or ""
    mc_version = mc_version_from_neoform(mcp) if mcp else ".".join(version.split(".")[:2])
    sources_coord = cfg.get("sources") or f"net.neoforged:neoforge:{version}:sources"
    log(f"minecraft {mc_version}")
    log(f"neoform {mcp}")

    # --- 1) neoforge sources ---
    write_status(step="neoforge-sources", minecraft=mc_version)
    src_name = f"neoforge-{version}-sources.jar"
    src_jar = CACHE / src_name
    src_url = f"{NF_MAVEN}/{version}/{src_name}"
    try:
        if not src_jar.exists():
            log(f"neoforge sources {src_url}")
            http_get(src_url, src_jar)
        n = extract_java_sources(src_jar, out)
        log(f"neoforge sources: {n} files")
        write_status(step="neoforge-sources-done")
    except Exception as exc:
        log(f"neoforge sources jar missing ({exc}); will decompile universal")
        uni_name = f"neoforge-{version}-universal.jar"
        uni_jar = CACHE / uni_name
        uni_url = f"{NF_MAVEN}/{version}/{uni_name}"
        log(f"neoforge universal {uni_url}")
        http_get(uni_url, uni_jar)
        decompile_jar(uni_jar, out)
        write_status(step="neoforge-universal-done")

    # --- 2) minecraft sources ---
    write_status(step="minecraft")
    used_nfrt = False
    try:
        used_nfrt = try_nfrt(version, out)
    except Exception as exc:
        log(f"nfrt error: {exc}")

    if not used_nfrt:
        write_status(step="minecraft-vineflower")
        client, mappings = fetch_mc_client(mc_version)
        target = client
        if not jar_has_prefix(client, "net/minecraft/"):
            if mappings is None:
                raise SystemExit("client jar is obfuscated and no official mappings exist")
            mapped = CACHE / f"mc-{mc_version}-official.jar"
            remap(client, mappings, mapped)
            target = mapped
        else:
            log("client jar already uses official names (no remap)")
        decompile_jar(target, out)

    files = count_java(out)
    (out / "_meta.json").write_text(json.dumps(
        {
            "neoforge": version,
            "minecraft": mc_version,
            "neoform": mcp,
            "sources": sources_coord,
            "files": files,
            "nfrt": used_nfrt,
            "finishedAt": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
        }, indent=2) + "\n")
    write_status(step="done", running=False, error=None)
    log(f"DONE {files} java files -> versions/{version}/")


def main() -> None:
    try:
        if hasattr(sys.stdout, "reconfigure"):
            sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass
    if len(sys.argv) >= 2 and sys.argv[1] in ("versions", "list"):
        cmd_versions()
        return
    if len(sys.argv) >= 3 and sys.argv[1] == "run":
        version = sys.argv[2]
    elif len(sys.argv) == 1 or (len(sys.argv) == 2 and sys.argv[1] == "pick"):
        version = pick_interactive()
    else:
        print(__doc__)
        raise SystemExit(2)
    try:
        cmd_run(version)
    except KeyboardInterrupt:
        write_status(running=False, error="interrupted", step="error")
        log("interrupted - partial output kept, rerun to resume")
        raise SystemExit(130)
    except SystemExit:
        write_status(running=False, error="failed", step="error")
        raise
    except Exception as exc:
        log(f"FATAL {exc}")
        write_status(running=False, error=str(exc), step="error")
        raise


if __name__ == "__main__":
    main()
