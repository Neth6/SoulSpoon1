
#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Convierte las animaciones GIF y PNG en recursos de fuente bitmap
para el mod SoulSpoon de Minecraft.

Uso desde la carpeta del proyecto:
python tools/gif_to_wheel.py /e/spoon/arte_ruleta/textures src/main/resources --max-frames 255 --color-frames 255 --frame-delay 1
"""

import argparse
import json
import os
import re
import shutil
import sys

from PIL import Image, ImageSequence

COLORS = {
    "rojo": "roja",
    "morado": "morada",
    "verde": "verde",
    "amarillo": "amarilla",
    "cian": "celeste",
    "azul": "azul",
    "naranja": "naranja",
}

FINAL_SLOT = 255
MAX_ANIMS = 24


def natural(s):
    return [
        int(t) if t.isdigit() else t.lower()
        for t in re.split(r"(\d+)", s)
    ]


def fit(im, size):
    """Centra el contenido visible en un lienzo fijo y conserva la transparencia."""
    im = im.convert("RGBA")

    # Obtener los límites del contenido visible
    bbox = im.getchannel("A").getbbox()

    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))

    if bbox is None:
        return canvas

    # Recortar el contenido transparente
    im = im.crop(bbox)

    # Escalar sin deformar
    w, h = im.size
    scale = min(size / w, size / h)
    new_w = max(1, round(w * scale))
    new_h = max(1, round(h * scale))

    im = im.resize(
        (new_w, new_h),
        Image.Resampling.LANCZOS
    )

    # Centrar el contenido
    x = (size - new_w) // 2
    y = (size - new_h) // 2

    canvas.alpha_composite(im, (x, y))
    return canvas

    scale = min(size / max(w, h), 1.0)
    new_w = max(1, round(w * scale))
    new_h = max(1, round(h * scale))

    return im.resize(
        (new_w, new_h),
        Image.Resampling.LANCZOS
    )


def pick(n, k):
    """Selecciona fotogramas distribuidos a lo largo de la animacion."""
    if n <= k:
        return list(range(n))

    if k <= 1:
        return [n - 1]

    return sorted({
        round(i * (n - 1) / (k - 1))
        for i in range(k)
    })



def load_frames(folder, max_frames, frame_delay):
    """
    Devuelve los fotogramas, la demora en ticks y la cantidad original.
    Omite fotogramas completamente transparentes al principio y al final.
    """
    gif_path = os.path.join(folder, "animation.gif")

    if os.path.exists(gif_path):
        with Image.open(gif_path) as gif:
            total = getattr(gif, "n_frames", 1)
            raw = []

            for i in range(total):
                gif.seek(i)
                raw.append(gif.convert("RGBA").copy())

            # Eliminar solamente los fotogramas totalmente transparentes
            # al principio y al final de la animación.
            start = 0
            end = len(raw)

            while start < end and raw[start].getchannel("A").getbbox() is None:
                start += 1

            while end > start and raw[end - 1].getchannel("A").getbbox() is None:
                end -= 1

            raw = raw[start:end]

            if not raw:
                return [], frame_delay, total

            indices = pick(len(raw), max_frames)
            return [raw[i] for i in indices], frame_delay, total

    pngs = sorted(
        [f for f in os.listdir(folder) if f.lower().endswith(".png")],
        key=natural
    )

    if not pngs:
        return [], frame_delay, 0

    indices = pick(len(pngs), max_frames)
    frames = []

    for i in indices:
        with Image.open(os.path.join(folder, pngs[i])) as im:
            frames.append(im.convert("RGBA").copy())

    return frames, frame_delay, len(pngs)


    pngs = sorted(
        [
            f for f in os.listdir(folder)
            if f.lower().endswith(".png")
        ],
        key=natural
    )

    if not pngs:
        return [], frame_delay, 0

    selected = pick(len(pngs), max_frames)
    frames = []

    for filename_index in selected:
        path = os.path.join(folder, pngs[filename_index])

        with Image.open(path) as im:
            frames.append(im.convert("RGBA").copy())

    return frames, frame_delay, len(pngs)


def main():
    parser = argparse.ArgumentParser()

    parser.add_argument("art")
    parser.add_argument("resources")

    parser.add_argument(
        "--height",
        type=int,
        default=56,
        help="Altura visual de la animacion en Minecraft"
    )

    parser.add_argument(
        "--size",
        type=int,
        default=254,
        help="Resolucion maxima de cada imagen"
    )

    parser.add_argument(
        "--ascent",
        type=int,
        default=None,
        help="Posicion vertical de la imagen en la fuente"
    )

    parser.add_argument(
        "--max-frames",
        type=int,
        default=255,
        help="Maximo de fotogramas para animaciones adicionales"
    )

    parser.add_argument(
        "--color-frames",
        type=int,
        default=255,
        help="Maximo de fotogramas para las ruletas de colores"
    )

    parser.add_argument(
        "--frame-delay",
        type=int,
        default=1,
        help="Ticks por fotograma; 1 tick equivale a 20 FPS"
    )

    parser.add_argument("--loops", type=int, default=1)

    parser.add_argument(
        "--final-seconds",
        type=float,
        default=1.5,
        help="Segundos que permanece el fotograma final"
    )

    args = parser.parse_args()

    if args.height < 1:
        parser.error("--height debe ser mayor que cero")

    if args.size < 1:
        parser.error("--size debe ser mayor que cero")

    if args.frame_delay < 1:
        parser.error("--frame-delay debe ser mayor que cero")

    if args.max_frames < 1 or args.color_frames < 1:
        parser.error("Los limites de fotogramas deben ser mayores que cero")

    args.size = min(args.size, 254)
    args.max_frames = min(args.max_frames, 255)
    args.color_frames = min(args.color_frames, 255)

    ascent = (
        args.ascent
        if args.ascent is not None
        else args.height // 2 - 3
    )

    base = os.path.join(
        args.resources,
        "assets",
        "soulspoon"
    )

    tex = os.path.join(base, "textures", "wheel")
    font_dir = os.path.join(base, "font")

    os.makedirs(base, exist_ok=True)
    os.makedirs(font_dir, exist_ok=True)

    names = []

    for slot, (color, folder) in enumerate(COLORS.items()):
        names.append((color, folder, slot))

    extras = sorted(
        [
            directory
            for directory in os.listdir(args.art)
            if os.path.isdir(os.path.join(args.art, directory))
            and directory not in COLORS.values()
        ],
        key=natural
    )

    for i, directory in enumerate(extras):
        names.append((directory, directory, len(COLORS) + i))

    if len(names) > MAX_ANIMS:
        sys.exit(
            "Demasiadas animaciones: maximo %d." % MAX_ANIMS
        )

    shutil.rmtree(tex, ignore_errors=True)
    os.makedirs(tex, exist_ok=True)

    providers = []
    animations = {}

    for name, folder, slot in names:
        path = os.path.join(args.art, folder)

        if not os.path.isdir(path):
            continue

        limit = (
            args.color_frames
            if name in COLORS
            else args.max_frames
        )

        frames, delay, original_count = load_frames(
            path,
            limit,
            args.frame_delay
        )

        if not frames:
            print(f"{name}: no se encontraron imagenes.")
            continue

        def glyph(n):
            return chr(0xE000 + slot * 256 + n)

        count = len(frames)
        is_static = count == 1

        if not is_static:
            for n, frame in enumerate(frames):
                filename = f"{name}_{n:02d}.png"
                frame_path = os.path.join(tex, filename)

                fit(frame, args.size).save(frame_path)

                providers.append({
                    "type": "bitmap",
                    "file": f"soulspoon:wheel/{filename}",
                    "ascent": ascent,
                    "height": args.height,
                    "chars": [glyph(n)]
                })

        final_filename = f"{name}_final.png"
        final_path = os.path.join(tex, final_filename)

        fit(frames[-1], args.size).save(final_path)

        providers.append({
            "type": "bitmap",
            "file": f"soulspoon:wheel/{final_filename}",
            "ascent": ascent,
            "height": args.height,
            "chars": [glyph(FINAL_SLOT)]
        })

        animations[name] = {
            "slot": slot,
            "base": 0xE000 + slot * 256,
            "final_glyph": 0xE000 + slot * 256 + FINAL_SLOT,
            "frames": 0 if is_static else count,
            "delay": delay,
            "loops": args.loops,
            "final": True,
            "final_ticks": int(args.final_seconds * 20)
        }

        print(
            f"{name:10s} slot {slot:2d}: "
            f"{count} seleccionados de {original_count} originales"
        )

    font_path = os.path.join(font_dir, "wheel.json")
    anim_path = os.path.join(base, "wheel_anim.json")

    with open(font_path, "w", encoding="utf-8") as file:
        json.dump(
            {"providers": providers},
            file,
            indent=2,
            ensure_ascii=False
        )

    with open(anim_path, "w", encoding="utf-8") as file:
        json.dump(
            animations,
            file,
            indent=2,
            ensure_ascii=False
        )

    print()
    print(f"Listo: {len(animations)} animaciones.")
    print(f"Imagenes generadas: {len(providers)}.")
    print("Recursos guardados en:", base)


if __name__ == "__main__":
    main()
