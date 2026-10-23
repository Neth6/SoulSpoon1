#!/usr/bin/env python3
"""
Convierte un .bbmodel de Blockbench (formato "Free", UV por cara) en los 3 archivos que
necesita GeckoLib:

    assets/soulspoon/geo/entity/<id>.geo.json
    assets/soulspoon/animations/entity/<id>.animation.json   (idle + walk generadas)
    assets/soulspoon/textures/entity/<id>.png                (la textura incrustada)

Uso:
    python tools/importar_bbmodel.py <archivo.bbmodel> <id_mob> <carpeta_resources> [estilo]

    estilo (opcional): cuadrupedo | bipedo | humanoide | creeper | fantasma | acuatico
                       Si se omite se adivina por el nombre de los huesos.

Las convenciones son las mismas que usa Blockbench al exportar "Bedrock Geometry":
X espejado, rotaciones [-x, -y, z]. Si un dia prefieres exportar tu mismo desde Blockbench
(File > Export > Export Bedrock Geometry), puedes reemplazar el .geo.json sin tocar nada mas
siempre que los huesos conserven los nombres.
"""
import base64
import json
import math
import os
import sys


def r(v):
    v = round(float(v), 4)
    return 0 if v == 0 else v


# --------------------------------------------------------------------------- geometria

def cube_to_geo(e, tex_id):
    frm, to = e["from"], e["to"]
    size = [to[i] - frm[i] for i in range(3)]
    c = {
        "origin": [r(-to[0]), r(frm[1]), r(frm[2])],
        "size": [r(s) for s in size],
    }
    if e.get("inflate"):
        c["inflate"] = r(e["inflate"])
    rot = e.get("rotation") or [0, 0, 0]
    if any(abs(v) > 1e-6 for v in rot):
        o = e.get("origin", [0, 0, 0])
        c["pivot"] = [r(-o[0]), r(o[1]), r(o[2])]
        c["rotation"] = [r(-rot[0]), r(-rot[1]), r(rot[2])]
    uv = {}
    for name, f in e["faces"].items():
        if f.get("texture") is None:
            continue
        u = f["uv"]
        uv[name] = {"uv": [r(u[0]), r(u[1])], "uv_size": [r(u[2] - u[0]), r(u[3] - u[1])]}
    c["uv"] = uv
    return c


def build_bones(data, regroup=None):
    els = {e["uuid"]: e for e in data["elements"]}
    groups = {g["uuid"]: g for g in data["groups"]}
    bones = []
    used_tex = set()
    seen = {}

    def unique(name):
        n = seen.get(name, 0)
        seen[name] = n + 1
        return name if n == 0 else "%s_%d" % (name, n + 1)

    def bone_for(g, parent):
        name = unique(g["name"])
        o = g["origin"]
        rot = g.get("rotation") or [0, 0, 0]
        b = {"name": name, "pivot": [r(-o[0]), r(o[1]), r(o[2])]}
        if parent:
            b["parent"] = parent
        if any(abs(v) > 1e-6 for v in rot):
            b["rotation"] = [r(-rot[0]), r(-rot[1]), r(rot[2])]
        b["cubes"] = []
        bones.append(b)
        return b

    def walk(node, parent_bone):
        if isinstance(node, str):
            e = els[node]
            for f in e["faces"].values():
                if f.get("texture") is not None:
                    used_tex.add(f["texture"])
            parent_bone["cubes"].append(cube_to_geo(e, 0))
            return
        g = groups[node["uuid"]]
        b = bone_for(g, parent_bone["name"] if parent_bone else None)
        for ch in node["children"]:
            walk(ch, b)

    root = {"name": "root", "pivot": [0, 0, 0], "cubes": []}
    top_level_cubes = [n for n in data["outliner"] if isinstance(n, str)]
    has_loose = bool(top_level_cubes)
    if has_loose:
        bones.append(root)
    for n in data["outliner"]:
        walk(n, root if has_loose else None)
    bones = [b for b in bones if b["cubes"] or any(x.get("parent") == b["name"] for x in bones)]
    for b in bones:
        if not b["cubes"]:
            del b["cubes"]
    return bones, used_tex


def herobrine_bones(data):
    """Herobrine: en el .bbmodel los nombres de brazos y piernas estan cruzados y los pivotes
    de los grupos no coinciden con los cubos. Se reconstruye un humanoide limpio por posicion."""
    els = {e["name"]: e for e in data["elements"]}
    used = set()
    for e in data["elements"]:
        for f in e["faces"].values():
            if f.get("texture") is not None:
                used.add(f["texture"])
    spec = [
        ("cabeza", "cabeza", None, [0, 24, 0]),
        ("cuerpo", "cuerpo", None, [0, 12, 0]),
        ("brazo_izq", "pierna_izq", None, [-6, 22, 0]),   # cubo en x -8..-4
        ("brazo_der", "brazo_izq", None, [6, 22, 0]),     # cubo en x 4..8
        ("pierna_izq", "brazo_der", None, [-2, 12, 0]),   # cubo en x -4..0 (y 0..12)
        ("pierna_der", "pierna_der", None, [2, 12, 0]),   # cubo en x 0..4 (y 0..12)
    ]
    bones = [{"name": "root", "pivot": [0, 0, 0]}]
    for bone, cube_name, _, piv in spec:
        b = {"name": bone, "parent": "root", "pivot": [r(-piv[0]), r(piv[1]), r(piv[2])],
             "cubes": [cube_to_geo(els[cube_name], 0)]}
        bones.append(b)
    return bones, used


# --------------------------------------------------------------------------- animaciones

def xz(b):
    p = b["pivot"]
    return -p[0], p[2]   # deshacer el espejo


def kf(period, samples, fn):
    out = {}
    for i in range(samples + 1):
        t = period * i / samples
        out["%.3f" % t] = [r(v) for v in fn(2 * math.pi * i / samples)]
    return out


def guess_style(names):
    n = " ".join(names)
    if "FANTASMA" in n.upper() or "cola_1" in names and "brazo_der" in names and "cabeza" not in names:
        return "fantasma"
    if "aleta" in n:
        return "acuatico"
    if "pata_del_izq" in names and "pata_tras_izq" in names and "cuerpo" in names:
        return "creeper"
    if "pierna_izq" in names and "brazo_izq" in names and "cabeza" in names:
        return "humanoide"
    if sum(1 for x in names if x.startswith("pata_")) >= 4:
        return "cuadrupedo"
    return "bipedo"


def make_animations(mob_id, bones, style):
    by = {b["name"]: b for b in bones}
    names = list(by)
    swing_walk = {}
    swing_idle = {}

    def add(table, bone, axis, amp, phase=0.0, per=None):
        if bone in by:
            table.setdefault(bone, []).append((axis, amp, phase))

    tails = sorted(n for n in names if n.startswith("cola_"))
    necks = sorted(n for n in names if n.startswith("cuello_"))

    if style in ("cuadrupedo", "creeper"):
        legs = [n for n in names if n.startswith("pata_") and "_inf_" not in n and "_sup_" not in n]
        amp = 38 if style == "creeper" else 30
        for n in legs:
            x, z = xz(by[n])
            ph = 0.0 if (x > 0) == (z > 0) else math.pi
            add(swing_walk, n, 0, amp, ph)
        for i, n in enumerate(tails):
            add(swing_walk, n, 1, 6, 0.0)
            add(swing_idle, n, 1, 4 + 2 * i, -0.6 * i)
        for i, n in enumerate(necks):
            add(swing_idle, n, 0, 3, -0.5 * i)
            add(swing_walk, n, 0, 3, math.pi + 0.5 * i)
        add(swing_idle, "cabeza", 0, 2, 0.0)
    elif style in ("bipedo", "humanoide"):
        legs = [n for n in names if n.startswith(("pierna", "pata"))
                and not any(k in n for k in ("espinilla", "_inf", "_sup"))]
        arms = [n for n in names if n.startswith("brazo")]
        amp = 35 if style == "humanoide" else 28
        for n in legs:
            x, _ = xz(by[n])
            add(swing_walk, n, 0, amp, 0.0 if x > 0 else math.pi)
        for n in arms:
            x, _ = xz(by[n])
            add(swing_walk, n, 0, amp * 0.8 if style == "humanoide" else 12, math.pi if x > 0 else 0.0)
            add(swing_idle, n, 0, 3, 0.0 if x > 0 else math.pi)
        for i, n in enumerate(tails):
            add(swing_walk, n, 1, 7, -0.5 * i)
            add(swing_idle, n, 1, 4 + 2 * i, -0.6 * i)
        add(swing_idle, "cabeza", 0, 2, 0.0)
    elif style == "acuatico":
        for i, n in enumerate(tails):
            add(swing_idle, n, 0, 6 + 4 * i, -0.7 * i)
            add(swing_walk, n, 0, 10 + 5 * i, -0.7 * i)
        for n in names:
            if n.startswith("aleta_") and "cola" not in n and "dorsal" not in n:
                x, _ = xz(by[n])
                sgn = 1 if x > 0 else -1
                add(swing_idle, n, 2, 8 * sgn, 0.0)
                add(swing_walk, n, 2, 14 * sgn, 0.0)
    elif style == "fantasma":
        arms = [n for n in names if n.startswith(("brazo", "antebrazo"))]
        for n in arms:
            x, _ = xz(by[n])
            sgn = 1 if x > 0 else -1
            amp = 10 if n.startswith("brazo") else 14
            add(swing_idle, n, 2, amp * sgn, 0.0)
            add(swing_walk, n, 2, (amp + 4) * sgn, 0.0)

    def anim(table, period, bob=0.0):
        bones_out = {}
        for bone, items in table.items():
            def fn(t, items=items):
                v = [0.0, 0.0, 0.0]
                for axis, amp, ph in items:
                    v[axis] += amp * math.sin(t + ph)
                return v
            bones_out[bone] = {"rotation": kf(period, 8, fn)}
        if bob:
            bones_out.setdefault(bones[0]["name"], {})["position"] = kf(
                period, 8, lambda t: [0, bob * math.sin(t), 0])
        return {"loop": True, "animation_length": period, "bones": bones_out}

    ghost = style == "fantasma"
    idle = anim(swing_idle, 2.0, 0.5 if ghost else 0.0)
    walk = anim(swing_walk if swing_walk else swing_idle, 1.0 if not ghost else 1.2, 0.5 if ghost else 0.0)
    return {
        "format_version": "1.8.0",
        "animations": {
            "animation.%s.idle" % mob_id: idle,
            "animation.%s.walk" % mob_id: walk,
        },
    }


# --------------------------------------------------------------------------- principal

def convert(path, mob_id, res_dir, style=None, quiet=False):
    data = json.load(open(path, encoding="utf-8"))
    if data["meta"].get("box_uv"):
        print("AVISO: %s usa Box UV; se recomienda 'Per-face UV'." % path)
    w, h = data["resolution"]["width"], data["resolution"]["height"]
    if data["name"].upper().startswith("HEROBRINE"):
        bones, used = herobrine_bones(data)
    else:
        bones, used = build_bones(data)
    if len(used) != 1:
        print("AVISO: %s usa varias texturas: %s (se usa la primera)" % (path, used))
    tex_id = sorted(used)[0] if used else 0
    tex = [t for t in data["textures"] if int(t["id"]) == int(tex_id)][0]
    png = base64.b64decode(tex["source"].split(",", 1)[1])

    geo = {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": "geometry.%s" % mob_id,
                "texture_width": w,
                "texture_height": h,
                "visible_bounds_width": 6,
                "visible_bounds_height": 6,
                "visible_bounds_offset": [0, 1.5, 0],
            },
            "bones": bones,
        }],
    }
    style = style or guess_style([b["name"] for b in bones])
    anim = make_animations(mob_id, bones, style)

    out = {
        "geo/entity/%s.geo.json" % mob_id: json.dumps(geo, indent=1),
        "animations/entity/%s.animation.json" % mob_id: json.dumps(anim, indent=1),
    }
    base = os.path.join(res_dir, "assets", "soulspoon")
    for rel, txt in out.items():
        p = os.path.join(base, rel)
        os.makedirs(os.path.dirname(p), exist_ok=True)
        open(p, "w", encoding="utf-8").write(txt)
    p = os.path.join(base, "textures", "entity", mob_id + ".png")
    os.makedirs(os.path.dirname(p), exist_ok=True)
    open(p, "wb").write(png)
    if not quiet:
        cubes = sum(len(b.get("cubes", [])) for b in bones)
        print("OK %-22s huesos=%-3d cubos=%-3d textura=%dx%d estilo=%s"
              % (mob_id, len(bones), cubes, tex["width"], tex["height"], style))
    return style


if __name__ == "__main__":
    if len(sys.argv) < 4:
        print(__doc__)
        sys.exit(1)
    convert(sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4] if len(sys.argv) > 4 else None)
