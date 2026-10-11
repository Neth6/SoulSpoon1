#!/usr/bin/env python3
"""
Genera los modelos y texturas de los mobs de la Parte 4 (y sus huevos).
    python tools/make_mobs.py src/client/java src/main/resources [--preview preview.png]

Salida:
  src/client/java/neth6/soulspoon/client/ModelDefs.java   (modelos en codigo, uno por forma)
  assets/soulspoon/textures/entity/<mob>.png           (textura 128x128 de cada mob)
  assets/soulspoon/textures/item + models/item         (huevos e items nuevos)
Para cambiar un mob: edita MODELS (formas) o MOBS (colores) y vuelve a ejecutarlo.
Coordenadas: y hacia abajo, el suelo esta en y=24 (igual que los modelos de vanilla).
"""
import json, os, random, sys
from PIL import Image, ImageDraw

def rgb(h): return tuple(int(h[i:i+2], 16) for i in (0, 2, 4))
def mul(c, f): return tuple(max(0, min(255, int(x * f))) for x in c)

def P(name, box, pos=(0, 0, 0), color="main", rot=(0, 0, 0), face=None):
    return dict(name=name, box=box, pos=pos, color=color, rot=rot, face=face or {})

# ---------------------------------------------------------------- formas
MODELS = {
 "reaper": [
    P("head", (-4,-8,-4,8,8,8), (0,0,0), "dark", face={"eyes": "eye"}),
    P("hood", (-5,-10,-5,10,4,10), (0,0,0), "main"),
    P("body", (-4,0,-2,8,12,4), (0,0,0), "main"),
    P("skirt", (-5,12,-3,10,12,6), (0,0,0), "main"),
    P("arm_l", (-1,-2,-2,4,12,4), (5,2,0), "main"),
    P("hand_r", (-3,-2,-2,4,12,4), (-5,2,0), "main", rot=(-1.1,0,0)),
    P("scythe_handle", (-1,-14,-1,2,26,2), (-6,10,-6), "extra"),
    P("scythe_blade", (-1,-14,-10,2,3,10), (-6,10,-6), "accent"),
 ],
 "medula": [
    P("head", (-4,-8,-4,8,8,8), (0,0,0), "main", face={"eyes": "eye", "mouth": "dark"}),
    P("body", (-4,0,-2,8,12,4), (0,0,0), "main"),
    P("rib1", (-5,1,-3,10,2,6), (0,0,0), "extra"),
    P("rib2", (-5,5,-3,10,2,6), (0,0,0), "extra"),
    P("rib3", (-5,9,-3,10,2,6), (0,0,0), "extra"),
    P("spine1", (-1,0,2,2,3,3), (0,0,0), "accent"),
    P("spine2", (-1,4,2,2,3,3), (0,0,0), "accent"),
    P("spine3", (-1,8,2,2,3,3), (0,0,0), "accent"),
    P("arm_l", (-1,-2,-1,3,12,3), (5,2,0), "main"),
    P("arm_r", (-2,-2,-1,3,12,3), (-5,2,0), "main"),
    P("leg_l", (-1,0,-1,3,12,3), (2,12,0), "main"),
    P("leg_r", (-2,0,-1,3,12,3), (-2,12,0), "main"),
 ],
 "cerebro": [
    P("body", (-4,-6,-4,8,6,8), (0,21,0), "main", face={"eyes": "eye"}),
    P("lobe", (-3,-9,-3,6,3,6), (0,21,0), "extra"),
    P("lobe2", (-1,-11,-2,2,2,4), (0,21,0), "accent"),
    P("leg_fl", (-1,0,-1,2,3,2), (3,21,-2), "dark"),
    P("leg_fr", (-1,0,-1,2,3,2), (-3,21,-2), "dark"),
    P("leg_bl", (-1,0,-1,2,3,2), (3,21,2), "dark"),
    P("leg_br", (-1,0,-1,2,3,2), (-3,21,2), "dark"),
 ],
 "ghost": [
    P("head", (-4,-8,-4,8,8,8), (0,8,0), "main", face={"eyes": "eye", "mouth": "dark"}),
    P("body", (-3,0,-2,6,10,4), (0,8,0), "main"),
    P("tail1", (-2,0,-2,4,6,4), (0,18,0), "extra"),
    P("tail2", (-1,0,-1,2,6,2), (0,24,0), "accent", rot=(0.0,0,0)),
    P("arm_l", (0,-1,-1,2,12,2), (3,9,0), "extra"),
    P("arm_r", (-2,-1,-1,2,12,2), (-3,9,0), "extra"),
 ],
 "dactylo": [
    P("body", (-3,-3,-8,6,6,16), (0,12,0), "main"),
    P("belly", (-2,3,-7,4,1,14), (0,12,0), "extra"),
    P("neck", (-2,-2,-5,4,4,5), (0,12,-8), "main"),
    P("head", (-2,-3,-7,4,4,6), (0,10,-12), "main", face={"eyes": "eye"}),
    P("beak", (-1,-1,-17,2,2,10), (0,10,-12), "accent"),
    P("crest", (-1,-5,-2,2,3,7), (0,10,-12), "dark"),
    P("wing_l", (0,0,-5,20,1,12), (3,10,0), "dark"),
    P("wing_r", (-20,0,-5,20,1,12), (-3,10,0), "dark"),
    P("tail", (-1,-1,0,2,2,10), (0,12,8), "dark"),
 ],
 "kragtops": [
    P("body", (-6,0,-9,12,10,18), (0,10,0), "main"),
    P("back", (-4,-2,-6,8,2,12), (0,10,0), "extra"),
    P("head", (-5,-5,-8,10,10,8), (0,14,-9), "main", face={"eyes": "eye"}),
    P("frill", (-8,-9,0,16,14,2), (0,14,-9), "extra"),
    P("horn_c", (-1,0,-13,2,2,5), (0,14,-9), "accent"),
    P("horn_l", (3,-6,-12,2,2,5), (0,14,-9), "accent"),
    P("horn_r", (-5,-6,-12,2,2,5), (0,14,-9), "accent"),
    P("leg_fl", (-2,0,-2,4,6,4), (4,18,-6), "dark"),
    P("leg_fr", (-2,0,-2,4,6,4), (-4,18,-6), "dark"),
    P("leg_bl", (-2,0,-2,4,6,4), (4,18,6), "dark"),
    P("leg_br", (-2,0,-2,4,6,4), (-4,18,6), "dark"),
    P("tail", (-1,0,0,2,2,8), (0,12,9), "dark"),
 ],
 "visceral": [
    P("head", (-4,-8,-4,8,8,8), (0,6,0), "main", face={"eyes": "eye", "mouth": "dark"}),
    P("body", (-4,0,-2,8,12,4), (0,6,0), "main"),
    P("organ1", (-3,2,-3,6,3,2), (0,6,0), "accent"),
    P("organ2", (-2,6,-3,4,3,2), (0,6,0), "extra"),
    P("organ3", (-5,3,-1,2,5,3), (0,6,0), "extra"),
    P("leg_fl", (-2,0,-2,4,6,4), (2,18,-4), "dark"),
    P("leg_fr", (-2,0,-2,4,6,4), (-2,18,-4), "dark"),
    P("leg_bl", (-2,0,-2,4,6,4), (2,18,4), "dark"),
    P("leg_br", (-2,0,-2,4,6,4), (-2,18,4), "dark"),
 ],
 "ojo": [
    P("eye", (-5,-5,-5,10,10,10), (0,14,0), "main", face={"iris": "eye"}),
    P("veins", (-5,-2,-5,10,1,10), (0,14,0), "accent"),
    P("wing_l", (0,0,-3,8,1,6), (5,12,0), "dark"),
    P("wing_r", (-8,0,-3,8,1,6), (-5,12,0), "dark"),
    P("tendril1", (-1,0,-1,2,7,2), (-2,19,1), "extra"),
    P("tendril2", (-1,0,-1,2,6,2), (2,19,-1), "extra"),
    P("tendril3", (-1,0,-1,2,8,2), (0,19,2), "extra"),
 ],
}

# ---------------------------------------------------------------- mobs (modelo + colores)
MOBS = {
 "reaper_pequeno":    ("reaper",   dict(main="1b1b24", dark="0c0c12", accent="c9ccd6", eye="ff2a2a", extra="5a3a1e")),
 "medula_osea":       ("medula",   dict(main="d9d2bd", dark="2a2420", accent="a89f84", eye="7a0a0a", extra="efe9d6")),
 "cerebro_rojo":      ("cerebro",  dict(main="b3262e", dark="5a1218", accent="e86a73", eye="fff1a8", extra="d4414a")),
 "cerebro_amarillo":  ("cerebro",  dict(main="e0c020", dark="8a6d00", accent="fff08a", eye="3a2a00", extra="f0d84a")),
 "fantasma_rojo":     ("ghost",    dict(main="c42b2b", dark="4a0a0a", accent="ff8a8a", eye="ffee88", extra="e24a4a")),
 "fantasma_amarillo": ("ghost",    dict(main="e8d24a", dark="6a5a10", accent="fff6a8", eye="2a2000", extra="f2e07a")),
 "dactylo_adulto":    ("dactylo",  dict(main="6a4f9a", dark="3c2c60", accent="e8d6a8", eye="ffd23a", extra="b8a2e0")),
 "kragtops":          ("kragtops", dict(main="8a7a55", dark="4e4630", accent="efe6c8", eye="1a1208", extra="a8603a")),
 "creeper_visceral":  ("visceral", dict(main="b86a6a", dark="4a1a1a", accent="e23a4a", eye="1a0000", extra="d88a8a")),
 "ojo_volador":       ("ojo",      dict(main="f2f2ea", dark="3a2a4a", accent="d83a3a", eye="6a2ad0", extra="8a5a8a")),
}
ITEMS = ["esencia_roja", "manzana_ambar", "altavoz", "linterna"]
EGG = {k: (v[1]["main"], v[1]["accent"]) for k, v in MOBS.items()}
TEX = 128

# ---------------------------------------------------------------- empaquetado UV
def pack(parts):
    items = []
    for p in parts:
        _, _, _, w, h, d = p["box"]
        items.append((p, 2 * d + 2 * w, d + h))
    items.sort(key=lambda t: -t[2])
    x = y = rowh = 0
    for p, bw, bh in items:
        if x + bw > TEX:
            x, y, rowh = 0, y + rowh, 0
        p["uv"] = (x, y); x += bw; rowh = max(rowh, bh)
    if y + rowh > TEX:
        sys.exit("El modelo no cabe en la textura")

def paint(im, parts, pal, seed):
    rnd = random.Random(seed)
    for p in parts:
        u, v = p["uv"]; _, _, _, w, h, d = p["box"]
        base = rgb(pal[p["color"]])
        faces = {"top": (u+d, v, w, d, 1.15), "bottom": (u+d+w, v, w, d, .65),
                 "right": (u, v+d, d, h, .82), "front": (u+d, v+d, w, h, 1.0),
                 "left": (u+d+w, v+d, d, h, .82), "back": (u+2*d+w, v+d, w, h, .9)}
        for name, (fx, fy, fw, fh, sh) in faces.items():
            for i in range(fw):
                for j in range(fh):
                    f = sh * (0.93 + rnd.random() * 0.14)
                    if i in (0, fw-1) or j in (0, fh-1): f *= 0.88
                    im.putpixel((fx+i, fy+j), mul(base, f) + (255,))
            if name == "front":
                detail(im, fx, fy, fw, fh, p["face"], pal)

def detail(im, fx, fy, fw, fh, face, pal):
    if "eyes" in face and fw >= 4:
        c = rgb(pal[face["eyes"]]); s = 2 if fw >= 8 else 1
        y = fy + int(fh * 0.4)
        for ex in (fx + fw // 4 - (s > 1), fx + fw - fw // 4 - s + (s > 1)):
            for i in range(s):
                for j in range(s): im.putpixel((ex + i, y + j), c + (255,))
    if "mouth" in face and fw >= 6:
        c = rgb(pal[face["mouth"]]); y = fy + int(fh * 0.72)
        for i in range(fw // 4, fw - fw // 4): im.putpixel((fx + i, y), c + (255,))
    if "iris" in face:
        c = rgb(pal[face["iris"]]); cx, cy = fx + fw / 2 - .5, fy + fh / 2 - .5
        for i in range(fw):
            for j in range(fh):
                dd = ((i + fx - cx) ** 2 + (j + fy - cy) ** 2) ** .5
                if dd <= fw * .36: im.putpixel((fx + i, fy + j), c + (255,))
                if dd <= fw * .16: im.putpixel((fx + i, fy + j), (8, 8, 12, 255))

# ---------------------------------------------------------------- Java
def f(x): return repr(float(x)) + "f"

def java(models):
    out = ["package neth6.soulspoon.client;", "",
           "import net.minecraft.client.model.geom.PartPose;",
           "import net.minecraft.client.model.geom.builders.CubeListBuilder;",
           "import net.minecraft.client.model.geom.builders.LayerDefinition;",
           "import net.minecraft.client.model.geom.builders.MeshDefinition;",
           "import net.minecraft.client.model.geom.builders.PartDefinition;", "",
           "// GENERADO por tools/make_mobs.py: no editar a mano.",
           "public final class ModelDefs {", "    private ModelDefs() {}", ""]
    for name, parts in models.items():
        out.append(f"    public static LayerDefinition {name}() {{")
        out.append("        MeshDefinition mesh = new MeshDefinition();")
        out.append("        PartDefinition root = mesh.getRoot();")
        for p in parts:
            ox, oy, oz, w, h, d = p["box"]; x, y, z = p["pos"]; rx, ry, rz = p["rot"]; u, v = p["uv"]
            pose = (f"PartPose.offsetAndRotation({f(x)}, {f(y)}, {f(z)}, {f(rx)}, {f(ry)}, {f(rz)})"
                    if any(p["rot"]) else f"PartPose.offset({f(x)}, {f(y)}, {f(z)})")
            out.append(f'        root.addOrReplaceChild("{p["name"]}", CubeListBuilder.create().texOffs({u}, {v})'
                       f'.addBox({f(ox)}, {f(oy)}, {f(oz)}, {f(w)}, {f(h)}, {f(d)}), {pose});')
        out.append(f"        return LayerDefinition.create(mesh, {TEX}, {TEX});")
        out.append("    }"); out.append("")
    out.append("}")
    return "\n".join(out) + "\n"

# ---------------------------------------------------------------- items
def egg(base, spot, seed):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    b, s = rgb(base), rgb(spot)
    d.ellipse((3, 1, 12, 14), fill=mul(b, .55) + (255,)); d.ellipse((4, 2, 11, 13), fill=b + (255,))
    for i, (x, y) in enumerate([(6,5),(9,7),(5,9),(10,10),(7,12),(8,3),(11,5)]):
        if (seed >> i) & 1 or i < 3: d.point((x, y), fill=s + (255,))
    for q in ((5,3),(5,4),(6,3)): d.point(q, fill=mul(b, 1.4)[:3] + (255,))
    return im

def item_img(name):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    if name == "esencia_roja":
        d.ellipse((3, 3, 12, 12), fill=(120, 10, 20, 255)); d.ellipse((4, 4, 11, 11), fill=(210, 30, 40, 255))
        d.ellipse((5, 5, 8, 8), fill=(255, 140, 140, 255)); d.point((10, 10), fill=(90, 0, 10, 255))
    elif name == "manzana_ambar":
        d.ellipse((2, 4, 13, 14), fill=(150, 80, 10, 255)); d.ellipse((3, 5, 12, 13), fill=(240, 160, 30, 255))
        d.ellipse((4, 6, 7, 8), fill=(255, 220, 120, 255)); d.rectangle((8, 1, 8, 4), fill=(90, 60, 20, 255))
        d.rectangle((9, 2, 11, 3), fill=(60, 150, 40, 255))
    elif name == "altavoz":
        d.rounded_rectangle((3, 1, 12, 14), radius=1, fill=(30, 30, 38, 255))
        d.ellipse((4, 2, 11, 9), fill=(70, 70, 86, 255)); d.ellipse((6, 4, 9, 7), fill=(20, 20, 26, 255))
        d.ellipse((6, 10, 9, 13), fill=(60, 60, 76, 255))
    elif name == "linterna":
        d.rectangle((6, 6, 9, 14), fill=(60, 60, 70, 255)); d.rectangle((5, 2, 10, 6), fill=(110, 110, 124, 255))
        d.rectangle((6, 1, 9, 2), fill=(255, 245, 160, 255)); d.rectangle((7, 8, 8, 9), fill=(200, 60, 60, 255))
    return im

def model_json(tex):
    return {"parent": "minecraft:item/generated", "textures": {"layer0": f"soulspoon:item/{tex}"}}

# ---------------------------------------------------------------- main
def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    if len(args) < 2: sys.exit(__doc__)
    jroot, rroot = args[0], args[1]
    prev = sys.argv[sys.argv.index("--preview") + 1] if "--preview" in sys.argv else None
    base = os.path.join(rroot, "assets", "soulspoon")
    for sub in ("textures/entity", "textures/item", "models/item"): os.makedirs(os.path.join(base, sub), exist_ok=True)
    for parts in MODELS.values(): pack(parts)

    jdir = os.path.join(jroot, "neth6", "soulspoon", "client"); os.makedirs(jdir, exist_ok=True)
    open(os.path.join(jdir, "ModelDefs.java"), "w", encoding="utf-8").write(java(MODELS))

    sheet = []
    for mob, (model, pal) in MOBS.items():
        im = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))
        paint(im, MODELS[model], pal, sum(map(ord, mob)))
        im.save(os.path.join(base, "textures/entity", mob + ".png"))
        e = egg(*EGG[mob], sum(map(ord, mob))); e.save(os.path.join(base, "textures/item", f"egg_{mob}.png"))
        json.dump(model_json(f"egg_{mob}"), open(os.path.join(base, "models/item", f"egg_{mob}.json"), "w"), indent=2)
        sheet.append((mob, model, pal, e))
    for it in ITEMS:
        item_img(it).save(os.path.join(base, "textures/item", it + ".png"))
        json.dump(model_json(it), open(os.path.join(base, "models/item", it + ".json"), "w"), indent=2)

    if prev:   # vista previa: frente y lado de cada mob (para comprobar proporciones)
        S = 6; W = 400
        pv = Image.new("RGB", (W * 2, 130 * ((len(sheet) + 1) // 2)), (50, 50, 56))
        d = ImageDraw.Draw(pv)
        for n, (mob, model, pal, e) in enumerate(sheet):
            ox, oy = (n % 2) * W, (n // 2) * 130
            d.text((ox + 4, oy + 2), mob, fill=(255, 255, 255))
            for view, cx in (("front", ox + 90), ("side", ox + 270)):
                for p in MODELS[model]:
                    bx, by, bz, w, h, dd = p["box"]; px, py, pz = p["pos"]
                    a0, a1 = (bx + px, bx + px + w) if view == "front" else (-(bz + pz + dd), -(bz + pz))
                    y0, y1 = by + py, by + py + h
                    d.rectangle((cx + a0 * 3 - (0 if view == "front" else 0), oy + 20 + y0 * 3.6,
                                 cx + a1 * 3, oy + 20 + y1 * 3.6), fill=rgb(pal[p["color"]]), outline=(0, 0, 0))
            pv.paste(e, (ox + 340, oy + 20), e)
        pv.save(prev)
    print(f"{len(MODELS)} formas, {len(MOBS)} mobs, {len(ITEMS)} items")

main()
