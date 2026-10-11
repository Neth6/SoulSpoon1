#!/usr/bin/env python3
"""
Convierte la carpeta de arte de la ruleta en recursos del mod.

Uso (desde la carpeta del proyecto):
    pip install pillow
    python tools/gif_to_wheel.py /e/spoon/arte_ruleta/textures src/main/resources [--height 32] [--max-frames 40]

Cada subcarpeta de la carpeta de arte es una animacion. Puede traer:
    animation.gif                  (se usa primero) o
    frame_0000.png, frame_0001.png ... (fotogramas sueltos, en orden alfabetico)
    un solo PNG                    (imagen fija)

Carpetas que son colores de la ruleta:
    roja -> rojo, morada -> morado, verde -> verde, amarilla -> amarillo,
    celeste -> cian, azul -> azul, naranja -> naranja
Las demas carpetas (nutria, reviil, rosa, death, muerte, gui...) se convierten en
animaciones extra con el nombre de su carpeta.
"""
import argparse, json, os, re, shutil, sys
from PIL import Image, ImageSequence

COLORS = {"rojo": "roja", "morado": "morada", "verde": "verde", "amarillo": "amarilla",
          "cian": "celeste", "azul": "azul", "naranja": "naranja"}   # orden = WheelColor
FINAL_SLOT = 63

def natural(s):
    return [int(t) if t.isdigit() else t.lower() for t in re.split(r"(\d+)", s)]

def fit(im, size):
    """Reescala al maximo 'size' px (Minecraft no admite letras de mas de ~254 px)."""
    im = im.convert("RGBA")
    w, h = im.size
    k = size / max(w, h)
    return im.resize((max(1, round(w * k)), max(1, round(h * k))), Image.LANCZOS)

def load_frames(folder, max_frames, frame_delay):
    # frame_delay = ticks por fotograma, fijo (1 tick = 20 fps)
    """Devuelve (lista de imagenes, delay en ticks)."""
    gif = os.path.join(folder, "animation.gif")
    if os.path.exists(gif):
        g = Image.open(gif)
        raw = [(fr.copy(), fr.info.get("duration", 100)) for fr in ImageSequence.Iterator(g)]
        step = max(1, -(-len(raw) // max_frames))
        picked = raw[::step][:max_frames]
        return [f for f, _ in picked], frame_delay
    pngs = sorted([f for f in os.listdir(folder) if f.lower().endswith(".png")], key=natural)
    if not pngs:
        return [], frame_delay
    step = max(1, -(-len(pngs) // max_frames))
    picked = pngs[::step][:max_frames]
    return [Image.open(os.path.join(folder, f)) for f in picked], frame_delay

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("art"); ap.add_argument("resources")
    ap.add_argument("--height", type=int, default=56, help="tamano en pantalla (sube = mas grande)")
    ap.add_argument("--size", type=int, default=254, help="resolucion de cada imagen en px (max 254)")
    ap.add_argument("--ascent", type=int, default=None)
    ap.add_argument("--max-frames", type=int, default=60, help="fotogramas por animacion (mas = dura mas)")
    ap.add_argument("--frame-delay", type=int, default=1, help="ticks por fotograma (1 = rapido)")
    ap.add_argument("--loops", type=int, default=1)
    ap.add_argument("--final-seconds", type=float, default=1.5, help="cuanto se queda el ultimo fotograma")
    a = ap.parse_args()
    ascent = a.ascent if a.ascent is not None else a.height // 2 - 3   # centrado en pantalla
    a.size = min(a.size, 254)

    base = os.path.join(a.resources, "assets", "soulspoon")
    tex = os.path.join(base, "textures", "wheel")
    shutil.rmtree(tex, ignore_errors=True)
    os.makedirs(tex); os.makedirs(os.path.join(base, "font"), exist_ok=True)

    names = []                                   # (nombre, carpeta, slot)
    for slot, (color, folder) in enumerate(COLORS.items()):
        names.append((color, folder, slot))
    extras = sorted(d for d in os.listdir(a.art)
                    if os.path.isdir(os.path.join(a.art, d)) and d not in COLORS.values())
    for i, d in enumerate(extras):
        names.append((d, d, len(COLORS) + i))
    if len(names) > 90:
        sys.exit("Demasiadas animaciones (maximo 90)")

    providers, anim = [], {}
    for name, folder, slot in names:
        path = os.path.join(a.art, folder)
        if not os.path.isdir(path):
            continue
        frames, delay = load_frames(path, a.max_frames, a.frame_delay)
        if not frames:
            continue
        static = len(frames) == 1
        def glyph(n): return chr(0xE000 + slot * 64 + n)
        count = 0
        if not static:
            for n, fr in enumerate(frames):
                fit(fr, a.size).save(os.path.join(tex, f"{name}_{n:02d}.png"))
                providers.append({"type": "bitmap", "file": f"soulspoon:wheel/{name}_{n:02d}.png",
                                  "ascent": ascent, "height": a.height, "chars": [glyph(n)]})
            count = len(frames)
        # el ultimo fotograma (o la imagen fija) se queda en pantalla al terminar
        fit(frames[-1], a.size).save(os.path.join(tex, f"{name}_final.png"))
        providers.append({"type": "bitmap", "file": f"soulspoon:wheel/{name}_final.png",
                          "ascent": ascent, "height": a.height, "chars": [glyph(FINAL_SLOT)]})
        anim[name] = {"slot": slot, "frames": count, "delay": delay, "loops": a.loops,
                      "final": True, "final_ticks": int(a.final_seconds * 20)}
        print(f"{name:10s} slot {slot:2d}: {count} fotogramas, {delay} ticks c/u")

    json.dump({"providers": providers}, open(os.path.join(base, "font", "wheel.json"), "w"), indent=1)
    json.dump(anim, open(os.path.join(base, "wheel_anim.json"), "w"), indent=1)
    print(f"Listo: {len(anim)} animaciones, {len(providers)} imagenes.")

main()
