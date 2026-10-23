#!/usr/bin/env python3
"""
Genera lo que falta de los 4 tiers de armadura a partir de los iconos de item:
  - textures/models/armor/<tier>_layer_1.png / _layer_2.png  (la armadura PUESTA, 64x32, formato vanilla)
      El pixelart del icono se dibuja en el frente (casco, peto, pantalones, botas) y el resto de caras
      se rellena con la paleta del icono, asi la armadura se ve completa en el juego.
  - models/item/<pieza>.json
  - data/soulspoon/recipe/<pieza>_mejora.json  (mesa de herreria: plantilla + pieza base + material)
Uso: python tools/make_armor.py src/main/resources [--keep]
  --keep  no pisa las texturas puestas que ya existan (para cuando dibujes las tuyas).
"""
import json, os, random, sys
import numpy as np
from PIL import Image

ARGS = [a for a in sys.argv[1:] if not a.startswith("--")]
KEEP = "--keep" in sys.argv
RES = ARGS[0] if ARGS else "src/main/resources"
ASSETS = os.path.join(RES, "assets", "soulspoon")
DATA = os.path.join(RES, "data", "soulspoon")
ITEM_TEX = os.path.join(ASSETS, "textures", "item")

TIERS = {
    "primitiva":  {"addition": "soulspoon:esencia_roja",    "base": "netherite"},
    "glebanoide": {"addition": "minecraft:nether_star",     "base": "primitiva"},
    "cedronica":  {"addition": "soulspoon:cristal_cedronico", "base": "netherite"},
    "limbo":      {"addition": "soulspoon:pluma_limbo",     "base": "netherite"},
}
PIECES = [("casco", "helmet"), ("peto", "chestplate"), ("pantalones", "leggings"), ("botas", "boots")]


def icon(piece, tier):
    return Image.open(os.path.join(ITEM_TEX, "%s_%s.png" % (piece, tier))).convert("RGBA")


def bbox_crop(img):
    a = np.array(img)[:, :, 3]
    ys, xs = np.where(a > 0)
    return img.crop((xs.min(), ys.min(), xs.max() + 1, ys.max() + 1))


def scaled(img, w, h):
    """Reduce/estira un icono a w x h conservando colores y bordes duros (alfa 0/255)."""
    a = np.array(img).astype(float)
    m = (a[:, :, 3] > 0).astype(float)
    prem = Image.fromarray((a[:, :, :3] * m[:, :, None]).astype("uint8"))
    mm = Image.fromarray((m * 255).astype("uint8"))
    prem = np.array(prem.resize((w, h), Image.BOX)).astype(float)
    mm = np.array(mm.resize((w, h), Image.BOX)).astype(float) / 255
    col = np.where(mm[:, :, None] > 0, prem / np.maximum(mm[:, :, None], 1e-3), 0).clip(0, 255)
    out = np.zeros((h, w, 4), dtype="uint8")
    out[:, :, :3] = col.astype("uint8")
    out[:, :, 3] = np.where(mm > 0.4, 255, 0)
    return Image.fromarray(out)


def palette(tier):
    img = icon("peto", tier)
    px = [p[:3] for p in img.getdata() if p[3] > 200]
    px.sort(key=lambda c: sum(c))
    n = len(px)
    return px[int(n * 0.05)], px[int(n * 0.30)], px[int(n * 0.55)], px[int(n * 0.85)]


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def base_face(img, x, y, w, h, pal, rnd, shade=0.0):
    dark, mid_d, mid, light = pal
    for j in range(h):
        for i in range(w):
            c = lerp(mid, mid_d, 0.15 + 0.5 * j / max(1, h - 1))
            r = rnd.random()
            if r < 0.20:
                c = lerp(c, light, 0.35)
            elif r < 0.34:
                c = lerp(c, dark, 0.35)
            if shade:
                c = lerp(c, dark, shade)
            img.putpixel((x + i, y + j), c + (255,))


def art_face(img, x, y, w, h, art, rows=None):
    """Pega el pixelart (icono reducido a w x h) sobre una cara; solo sus pixeles opacos."""
    s = scaled(art, w, h)
    for j in range(h):
        if rows is not None and j not in rows:
            continue
        for i in range(w):
            p = s.getpixel((i, j))
            if p[3] > 0:
                img.putpixel((x + i, y + j), p)


def box(img, u, v, w, h, d, pal, rnd, front=None, back_shade=0.25, rows=None):
    """Caja vanilla. rows = filas (0..h-1) que se pintan en las caras laterales; None = todas."""
    def region(x, y, fw, fh, shade=0.0, front_art=None, side=True):
        use_rows = rows if (side and rows is not None) else None
        if use_rows is None:
            base_face(img, x, y, fw, fh, pal, rnd, shade)
        else:
            tmp = Image.new("RGBA", (fw, fh), (0, 0, 0, 0))
            base_face(tmp, 0, 0, fw, fh, pal, rnd, shade)
            for j in use_rows:
                for i in range(fw):
                    img.putpixel((x + i, y + j), tmp.getpixel((i, j)))
        if front_art is not None:
            art_face(img, x, y, fw, fh, front_art, use_rows)
    region(u + d, v, w, d, 0.0, side=False)              # arriba
    region(u + d + w, v, w, d, 0.4, side=False)          # abajo
    region(u, v + d, d, h, 0.15)                         # derecha
    region(u + d, v + d, w, h, 0.0, front)               # frente
    region(u + d + w, v + d, d, h, 0.15)                 # izquierda
    region(u + 2 * d + w, v + d, w, h, back_shade)       # espalda


def worn(tier, layer):
    pal = palette(tier)
    rnd = random.Random(sum(ord(ch) for ch in tier) * 7 + layer)
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    casco = bbox_crop(icon("casco", tier))
    peto = bbox_crop(icon("peto", tier))
    pant = bbox_crop(icon("pantalones", tier))
    botas = bbox_crop(icon("botas", tier))
    if layer == 1:
        box(img, 0, 0, 8, 8, 8, pal, rnd, front=casco)                      # casco
        box(img, 16, 16, 8, 12, 4, pal, rnd, front=peto)                    # peto (cuerpo)
        box(img, 40, 16, 4, 12, 4, pal, rnd)                                # mangas
        # botas: mitad izquierda del icono en la parte baja de las piernas (filas 7..11)
        half = botas.crop((0, 0, max(1, botas.width // 2), botas.height))
        box(img, 0, 16, 4, 12, 4, pal, rnd, front=half, rows=range(7, 12))
    else:
        # pantalones: cintura (4 filas del cuerpo) + piernas completas
        box(img, 16, 16, 8, 12, 4, pal, rnd, front=pant, rows=range(0, 4))
        half = pant.crop((0, 0, max(1, pant.width // 2), pant.height))
        box(img, 0, 16, 4, 12, 4, pal, rnd, front=half)
    return img


def main():
    d = os.path.join(ASSETS, "textures", "models", "armor")
    os.makedirs(d, exist_ok=True)
    for tier in TIERS:
        for layer in (1, 2):
            target = os.path.join(d, "%s_layer_%d.png" % (tier, layer))
            if KEEP and os.path.exists(target):
                continue
            worn(tier, layer).save(target)
        for es, en in PIECES:
            name = "%s_%s" % (es, tier)
            mdl = os.path.join(ASSETS, "models", "item")
            os.makedirs(mdl, exist_ok=True)
            json.dump({"parent": "minecraft:item/generated",
                       "textures": {"layer0": "soulspoon:item/" + name}},
                      open(os.path.join(mdl, name + ".json"), "w"), indent=2)
            base = TIERS[tier]["base"]
            base_item = ("minecraft:netherite_" + en) if base == "netherite" else "soulspoon:%s_%s" % (es, base)
            rd = os.path.join(DATA, "recipe")
            os.makedirs(rd, exist_ok=True)
            json.dump({
                "type": "minecraft:smithing_transform",
                "template": {"item": "soulspoon:plantilla_" + tier},
                "base": {"item": base_item},
                "addition": {"item": TIERS[tier]["addition"]},
                "result": {"id": "soulspoon:" + name, "count": 1},
            }, open(os.path.join(rd, name + "_mejora.json"), "w"), indent=2)
        mdl = os.path.join(ASSETS, "models", "item")
        json.dump({"parent": "minecraft:item/generated",
                   "textures": {"layer0": "soulspoon:item/plantilla_" + tier}},
                  open(os.path.join(mdl, "plantilla_%s.json" % tier), "w"), indent=2)
    # items sueltos
    mdl = os.path.join(ASSETS, "models", "item")
    for n in ("linterna", "manzana_ambar", "cristal_cedronico", "pluma_limbo"):
        if os.path.exists(os.path.join(ITEM_TEX, n + ".png")):
            json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "soulspoon:item/" + n}},
                      open(os.path.join(mdl, n + ".json"), "w"), indent=2)
    # armaduras puestas dibujadas por ti: tools/worn_manual/<tier>_layer_1.png / _layer_2.png
    # (64x32) mandan sobre las generadas automaticamente.
    manual = os.path.join(os.path.dirname(os.path.abspath(__file__)), "worn_manual")
    if os.path.isdir(manual):
        import shutil
        for fn in sorted(os.listdir(manual)):
            if fn.endswith(".png"):
                shutil.copy2(os.path.join(manual, fn), os.path.join(d, fn))
                print("  usando tu textura puesta:", fn)
    print("armaduras generadas:", ", ".join(TIERS))


main()
