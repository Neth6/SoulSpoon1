#!/usr/bin/env python3
"""Conecta el casino: lo inicializa en SoulSpoon (antes de ImportedContent), registra su renderer
y agrega los nombres. Se puede correr varias veces. Uso (raiz del repo): python tools/aplicar_casino.py"""
import json, os, re, sys

ROOT = os.getcwd()
J = os.path.join(ROOT, "src", "main", "java", "neth6", "soulspoon")
CJ = os.path.join(ROOT, "src", "client", "java", "neth6", "soulspoon", "client")
LANG = os.path.join(ROOT, "src", "main", "resources", "assets", "soulspoon", "lang")

def rd(p):
    with open(p, encoding="utf-8") as f: return f.read()
def wr(p, t):
    with open(p, "w", encoding="utf-8", newline="\n") as f: f.write(t)

# 1) SoulSpoon.java
p = os.path.join(J, "SoulSpoon.java")
s = rd(p)
if "ModCasino.initialize()" not in s:
    if "import neth6.soulspoon.casino.ModCasino;" not in s:
        s = s.replace("import net.fabricmc.api.ModInitializer;", "import net.fabricmc.api.ModInitializer;\nimport neth6.soulspoon.casino.ModCasino;", 1)
    if "ImportedContent.initialize();" in s:
        s = s.replace("        ImportedContent.initialize();", "        ModCasino.initialize();\n        ImportedContent.initialize();", 1)
    else:
        s = re.sub(r"(\n\s*LOGGER\.info\()", r"\n        ModCasino.initialize();\1", s, count=1)
    wr(p, s); print("SoulSpoon: ModCasino.initialize() agregado")
else:
    print("SoulSpoon: ya estaba")

# 2) Renderer (busca ModEntityRenderers en el cliente)
rp = None
for dp, _, fs in os.walk(os.path.join(ROOT, "src")):
    if "ModEntityRenderers.java" in fs: rp = os.path.join(dp, "ModEntityRenderers.java")
if rp is None:
    sys.exit("No encuentro ModEntityRenderers.java")
r = rd(rp)
if "ModCasino.CASINO" not in r:
    r = r.replace("import neth6.soulspoon.entity.ModEntities;", "import neth6.soulspoon.entity.ModEntities;\nimport neth6.soulspoon.casino.ModCasino;", 1)
    marker = re.search(r"(\n\s*EntityRendererRegistry\.register\(ModEntities\.[A-Z_]+,)", r)
    add = "\n        EntityRendererRegistry.register(ModCasino.CASINO,\n                ctx -> new ModGeoRenderer<>(ctx, \"casino\", 0.8f, 1.0f));"
    if marker:
        r = r[:marker.start()] + add + r[marker.start():]
    else:
        r = re.sub(r"(public static void register\(\) \{)", r"\1" + add, r, count=1)
    wr(rp, r); print("Renderer del casino agregado en", os.path.relpath(rp, ROOT))
else:
    print("Renderer: ya estaba")

# 3) Textos
add = {"es_es": {"entity.soulspoon.casino": "Casino", "item.soulspoon.casino_spawn_egg": "Huevo de Casino"},
       "en_us": {"entity.soulspoon.casino": "Casino", "item.soulspoon.casino_spawn_egg": "Casino Spawn Egg"}}
for code, d in add.items():
    lp = os.path.join(LANG, code + ".json")
    cur = json.loads(rd(lp)) if os.path.exists(lp) else {}
    new = {k: v for k, v in d.items() if k not in cur}
    if new:
        cur.update(new); wr(lp, json.dumps(cur, indent=2, ensure_ascii=False)); print("lang", code, "+", len(new))
# 3b) Cliente: receptor de la tabla del casino
cp = os.path.join(CJ, "SoulSpoonClient.java")
if os.path.exists(cp):
    c = rd(cp)
    if "CasinoClient.initialize()" not in c:
        c2 = c.replace("        ModEntityRenderers.register();", "        ModEntityRenderers.register();\n        CasinoClient.initialize();", 1)
        if c2 == c: sys.exit("No pude enganchar CasinoClient en SoulSpoonClient.java (agrega CasinoClient.initialize(); a mano)")
        wr(cp, c2); print("SoulSpoonClient: CasinoClient.initialize() agregado")
    else:
        print("SoulSpoonClient: ya estaba")
else:
    sys.exit("No encuentro SoulSpoonClient.java")

# 4) Mixin de proteccion contra explosiones
mp = None
for dp, _, fs in os.walk(os.path.join(ROOT, "src", "main", "resources")):
    for f in fs:
        if f.endswith(".mixins.json") and "client" not in f: mp = os.path.join(dp, f)
if mp:
    m = json.loads(rd(mp))
    if "ExplosionProtectMixin" not in m.get("mixins", []):
        m.setdefault("mixins", []).append("ExplosionProtectMixin")
        wr(mp, json.dumps(m, indent="\t")); print("Mixin de explosiones registrado en", os.path.basename(mp))
    else:
        print("Mixin de explosiones: ya estaba")
else:
    print("AVISO: no encuentro el archivo .mixins.json; agrega 'ExplosionProtectMixin' a mano")

print("\nListo. Prueba: /give @s soulspoon:casino_spawn_egg, click derecho al casino con deditas.\nShift + clic izquierdo al casino = tabla de premios (admins editan). /casino 0|1, /casino precio, /casino reset")
