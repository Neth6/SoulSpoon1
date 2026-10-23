#!/usr/bin/env python3
"""
Importa los DATOS del mod original (recetas, loot tables, tags, tipos de dano, encantamientos...)
al mod SoulSpoon.  Cambia el namespace dedsafio4 -> soulspoon.

  python tools/importar_datos.py            # normal
  python tools/importar_datos.py --simular  # solo muestra que haria
  python tools/importar_datos.py --estructuras  # tambien aplica los tags que quitan estructuras vanilla

Reglas:
 * NO pisa ningun archivo que ya exista.
 * Receta / loot table / encantamiento que use un item que todavia no existe se SALTA y se lista al final
   (cuando agregues el item vuelve a correr el script y se importa).
 * En los tags, las entradas que aun no existen se marcan "required": false para no romper el mundo.
"""
import json, os, re, sys, shutil

ROOT = os.getcwd()
SRC = os.path.join(ROOT, "tools", "datos_importados")
RES = os.path.join(ROOT, "src", "main", "resources")
NS = "soulspoon"
SIMULAR = "--simular" in sys.argv
ESTRUCTURAS = "--estructuras" in sys.argv

def read(p):
    with open(p, encoding="utf-8") as f: return f.read()

def write(p, text):
    if SIMULAR: return
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8", newline="\n") as f: f.write(text)

def ren(text): return text.replace("dedsafio4", NS)

# ---- ids que existen en el mod (items/bloques con modelo de item) --------------------------------
models = os.path.join(RES, "assets", NS, "models", "item")
items = {f[:-5] for f in os.listdir(models) if f.endswith(".json")} if os.path.isdir(models) else set()
if not items:
    sys.exit("No encuentro " + models + " -> corre este script desde la raiz del repo.")

# ---- tipos de dano / tags / recetas que se importan en esta misma corrida ---------------------------
mod = os.path.join(SRC, "mod")
damage = {f[:-5] for f in os.listdir(os.path.join(mod, "damage_type"))}
item_tags = {f[:-5] for f in os.listdir(os.path.join(mod, "tags", "item"))}
enchants = {f[:-5] for f in os.listdir(os.path.join(mod, "enchantment"))}

def refs(text):
    return set(re.findall(r'"#?%s:([a-z0-9_/]+)"' % NS, text))

def faltan(text):
    """ids de item que no existen (se ignoran tags, tipos de dano y encantamientos propios)"""
    out = set()
    for m in re.finditer(r'"(#?)%s:([a-z0-9_/]+)"' % NS, text):
        tag, n = m.group(1), m.group(2)
        if n in items or n in damage or n in enchants or n in item_tags: continue
        ctx = text[max(0, m.start() - 40):m.start()]
        if '"tag"' in ctx or "#" in tag or '"type"' in ctx or "translate" in ctx: continue
        out.add(n)
    return out

stats = {"ok": 0, "ya": 0}
saltados = []

def put(dst, text, etiqueta):
    if os.path.exists(dst):
        stats["ya"] += 1; return
    write(dst, text); stats["ok"] += 1

# ---- recetas ------------------------------------------------------------------------------------
for f in sorted(os.listdir(os.path.join(mod, "recipe"))):
    p = os.path.join(mod, "recipe", f)
    t = ren(read(p))
    d = json.loads(t)
    if d.get("type") == NS + ":tenir_bolsa":
        # receta especial del original -> sin mezcla: bolsa + tinte (pierde el traspaso de contenido)
        d = {"type": "minecraft:crafting_shapeless", "category": "misc",
             "ingredients": d["ingredients"], "result": d["result"]}
        t = json.dumps(d, indent=2)
    miss = faltan(t)
    if miss: saltados.append(("receta " + f, sorted(miss))); continue
    put(os.path.join(RES, "data", NS, "recipe", f), t, f)

# ---- loot tables --------------------------------------------------------------------------------
for sub in ("blocks", "entities"):
    base = os.path.join(mod, "loot_table", sub)
    for f in sorted(os.listdir(base)):
        t = ren(read(os.path.join(base, f)))
        miss = faltan(t)
        if miss: saltados.append(("loot " + sub + "/" + f, sorted(miss))); continue
        put(os.path.join(RES, "data", NS, "loot_table", sub, f), t, f)

# ---- tipos de dano y encantamientos --------------------------------------------------------------
for sub in ("damage_type", "enchantment"):
    for f in sorted(os.listdir(os.path.join(mod, sub))):
        t = ren(read(os.path.join(mod, sub, f)))
        miss = faltan(t)
        if miss: saltados.append((sub + "/" + f, sorted(miss))); continue
        put(os.path.join(RES, "data", NS, sub, f), t, f)

# ---- tags (se marcan como opcionales las entradas que aun no existen) -----------------------------
ENTIDADES = None
def tag_fix(text, kind):
    d = json.loads(ren(text))
    vals = []
    for v in d.get("values", []):
        if isinstance(v, dict): vals.append(v); continue
        if v.startswith(NS + ":") and not v.startswith("#"):
            n = v.split(":", 1)[1]
            existe = (n in damage) if kind == "damage_type" else (n in items) if kind in ("item", "block") else False
            vals.append(v if existe else {"id": v, "required": False})
        else:
            vals.append(v)
    d["values"] = vals
    return json.dumps(d, indent=2)

def tags_de(root, ns_dest, solo_mod):
    for dp, _, fs in os.walk(os.path.join(root, "tags")):
        for f in fs:
            rel = os.path.relpath(os.path.join(dp, f), root).replace("\\", "/")  # tags/block/x.json
            if rel.startswith("tags/worldgen/biome/has_structure") and not ESTRUCTURAS: 
                saltados.append(("tag " + rel, ["(quita estructuras vanilla: usa --estructuras si lo quieres)"])); continue
            kind = rel.split("/")[1] if rel.startswith("tags/") else ""
            if kind == "worldgen": kind = "worldgen"
            t = tag_fix(read(os.path.join(dp, f)), kind)
            put(os.path.join(RES, "data", ns_dest, rel), t, rel)

tags_de(mod, NS, True)
tags_de(os.path.join(SRC, "minecraft"), "minecraft", False)

# ---- funcion /function soulspoon:kit ---------------------------------------------------------------
fk = os.path.join(mod, "function", "kit.mcfunction")
if os.path.exists(fk):
    put(os.path.join(RES, "data", NS, "function", "kit.mcfunction"), ren(read(fk)), "kit")

# ---- textos (encantamientos y mensajes de muerte) -----------------------------------------------------
LANG = {
 "es_es": {"enchantment.soulspoon.perdicion_de_gleba": "Perdición de Gleba",
           "enchantment.soulspoon.perdicion_del_limbo": "Perdición del Limbo",
           "death.attack.agarre": "%1$s fue atrapado hasta morir",
           "death.attack.agua_limbo": "%1$s se ahogó en el agua del Limbo",
           "death.attack.agua_organos": "%1$s se disolvió en el agua de los Órganos",
           "death.attack.circulo": "%1$s fue consumido por el círculo",
           "death.attack.enderperla": "%1$s murió por una enderperla",
           "death.attack.fantasma_amarillo": "%1$s fue destruido por el fantasma amarillo",
           "death.attack.herobrine": "%1$s fue asesinado por Herobrine",
           "death.attack.magma": "%1$s se quemó en el magma",
           "death.attack.meteorito": "%1$s fue aplastado por un meteorito",
           "death.attack.qumara": "%1$s fue devorado por Qumara"},
 "en_us": {"enchantment.soulspoon.perdicion_de_gleba": "Gleba's Bane",
           "enchantment.soulspoon.perdicion_del_limbo": "Limbo's Bane",
           "death.attack.agarre": "%1$s was grabbed to death",
           "death.attack.agua_limbo": "%1$s drowned in Limbo water",
           "death.attack.agua_organos": "%1$s dissolved in the water of the Organs",
           "death.attack.circulo": "%1$s was consumed by the circle",
           "death.attack.enderperla": "%1$s died from an ender pearl",
           "death.attack.fantasma_amarillo": "%1$s was destroyed by the yellow ghost",
           "death.attack.herobrine": "%1$s was killed by Herobrine",
           "death.attack.magma": "%1$s burned in magma",
           "death.attack.meteorito": "%1$s was crushed by a meteorite",
           "death.attack.qumara": "%1$s was devoured by Qumara"}}
for code, add in LANG.items():
    p = os.path.join(RES, "assets", NS, "lang", code + ".json")
    d = json.loads(read(p)) if os.path.exists(p) else {}
    nuevo = {k: v for k, v in add.items() if k not in d}
    if nuevo:
        d.update(nuevo)
        write(p, json.dumps(d, indent=2, ensure_ascii=False))
        print("lang", code, "+", len(nuevo))

print("\nImportados: %d   Ya existian (no se tocaron): %d   Saltados: %d" % (stats["ok"], stats["ya"], len(saltados)))
for n, m in saltados:
    print("  SALTADO", n, "-> falta:", ", ".join(m))
if SIMULAR: print("(simulacion: no se escribio nada)")
