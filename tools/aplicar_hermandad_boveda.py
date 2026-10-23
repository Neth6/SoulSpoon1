#!/usr/bin/env python3
"""Conecta Hermandades + Boveda. Se puede correr varias veces.
Requiere haber instalado antes soulspoon-correo-cajero.zip (usa EstiloCajero y Banco).
Uso (raiz del repo): python tools/aplicar_hermandad_boveda.py"""
import json, os, shutil, sys

ROOT = os.getcwd()
J = os.path.join(ROOT, "src", "main", "java", "neth6", "soulspoon")
CJ = os.path.join(ROOT, "src", "client", "java", "neth6", "soulspoon", "client")
RES = os.path.join(ROOT, "src", "main", "resources", "assets", "soulspoon")

def rd(p):
    with open(p, encoding="utf-8") as f: return f.read()
def wr(p, t):
    with open(p, "w", encoding="utf-8", newline="\n") as f: f.write(t)

p = os.path.join(J, "SoulSpoon.java")
s = rd(p)
if "ModHermandad.initialize()" not in s:
    imp = "import net.fabricmc.api.ModInitializer;"
    s = s.replace(imp, imp + "\nimport neth6.soulspoon.boveda.ModBoveda;\nimport neth6.soulspoon.hermandad.ModHermandad;", 1)
    m = "        ImportedContent.initialize();"
    if m not in s: sys.exit("No encuentro ImportedContent.initialize(); agrega ModHermandad.initialize(); y ModBoveda.initialize(); antes a mano")
    s = s.replace(m, "        ModHermandad.initialize();\n        ModBoveda.initialize();\n" + m, 1)
    wr(p, s); print("SoulSpoon: hermandad + boveda agregados")
else:
    print("SoulSpoon: ya estaba")

cp = os.path.join(CJ, "SoulSpoonClient.java")
c = rd(cp)
if "HermandadClient.initialize()" not in c:
    c2 = c.replace("        ModEntityRenderers.register();", "        ModEntityRenderers.register();\n        HermandadClient.initialize();", 1)
    if c2 == c: sys.exit("Agrega HermandadClient.initialize(); en SoulSpoonClient a mano")
    wr(cp, c2); print("SoulSpoonClient: listo")
else:
    print("SoulSpoonClient: ya estaba")

LANG = os.path.join(RES, "lang")
add = {"es_es": {"item.soulspoon.manuscrito_hermandad": "Manuscrito de Hermandad",
                 "item.soulspoon.manuscrito_hermandad_admin": "Manuscrito de Hermandad [Admin]",
                 "block.soulspoon.boveda": "Bóveda de Hermandad",
                 "key.soulspoon.hermandad": "Interfaz de Hermandad", "category.soulspoon": "SoulSpoon"},
       "en_us": {"item.soulspoon.manuscrito_hermandad": "Brotherhood Manuscript",
                 "item.soulspoon.manuscrito_hermandad_admin": "Brotherhood Manuscript [Admin]",
                 "block.soulspoon.boveda": "Brotherhood Vault",
                 "key.soulspoon.hermandad": "Brotherhood menu", "category.soulspoon": "SoulSpoon"}}
for code, d in add.items():
    lp = os.path.join(LANG, code + ".json")
    cur = json.loads(rd(lp)) if os.path.exists(lp) else {}
    new = {k: v for k, v in d.items() if k not in cur}
    if new:
        cur.update(new); wr(lp, json.dumps(cur, indent=2, ensure_ascii=False)); print("lang", code, "+", len(new))

# textura del manuscrito sellado: si falta, copia la normal para que no salga el cuadro rosa
tx = os.path.join(RES, "textures", "item")
a, b = os.path.join(tx, "manuscrito_hermandad.png"), os.path.join(tx, "manuscrito_hermandad_sellado.png")
if os.path.exists(a) and not os.path.exists(b):
    shutil.copyfile(a, b); print("copiado manuscrito_hermandad_sellado.png (provisional, reemplazalo con tu arte)")
for need in ("textures/entity/boveda.png", "textures/item/manuscrito_hermandad.png"):
    if not os.path.exists(os.path.join(RES, need)):
        print("AVISO: falta", need)
if not os.path.exists(os.path.join(CJ, "cajero", "EstiloCajero.java")):
    print("AVISO: falta client/cajero/EstiloCajero.java -> instala primero soulspoon-correo-cajero.zip")
print("\nListo.")
