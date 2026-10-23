#!/usr/bin/env python3
"""Conecta los cofres (Cofre Protegido y Cofre de Huesos). Se puede correr varias veces.
Uso (raiz del repo): python tools/aplicar_cofres.py"""
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
if "ModCofres.initialize()" not in s:
    imp = "import net.fabricmc.api.ModInitializer;"
    s = s.replace(imp, imp + "\nimport neth6.soulspoon.cofres.ModCofres;", 1)
    for marker in ("        ModCajero.initialize();", "        ModCasino.initialize();", "        ImportedContent.initialize();"):
        if marker in s:
            s = s.replace(marker, "        ModCofres.initialize();\n" + marker, 1); break
    else:
        sys.exit("No encuentro donde insertar ModCofres.initialize(); agregalo a mano antes de ImportedContent.initialize();")
    wr(p, s); print("SoulSpoon: cofres agregados")
else:
    print("SoulSpoon: ya estaba")

cp = os.path.join(CJ, "SoulSpoonClient.java")
c = rd(cp)
if "CofresClient.initialize()" not in c:
    c2 = c.replace("        ModEntityRenderers.register();", "        ModEntityRenderers.register();\n        CofresClient.initialize();", 1)
    if c2 == c: sys.exit("No pude enganchar el cliente (agrega CofresClient.initialize(); en SoulSpoonClient a mano)")
    wr(cp, c2); print("SoulSpoonClient: listo")
else:
    print("SoulSpoonClient: ya estaba")

# mallas de los cofres (assets/soulspoon/modelos/*.json), sin pisar
dst = os.path.join(RES, "modelos"); os.makedirs(dst, exist_ok=True)
src = os.path.join(ROOT, "tools", "cofres_modelos")
for f in sorted(os.listdir(src)):
    if not os.path.exists(os.path.join(dst, f)):
        shutil.copyfile(os.path.join(src, f), os.path.join(dst, f)); print("copiado modelos/" + f)

LANG = os.path.join(RES, "lang")
add = {"es_es": {"block.soulspoon.cofre_protegido": "Cofre Protegido", "block.soulspoon.cofre_huesos": "Cofre de Huesos"},
       "en_us": {"block.soulspoon.cofre_protegido": "Protected Chest", "block.soulspoon.cofre_huesos": "Bone Chest"}}
for code, d in add.items():
    lp = os.path.join(LANG, code + ".json")
    cur = json.loads(rd(lp)) if os.path.exists(lp) else {}
    new = {k: v for k, v in d.items() if k not in cur}
    if new:
        cur.update(new); wr(lp, json.dumps(cur, indent=2, ensure_ascii=False)); print("lang", code, "+", len(new))
for need in ("textures/entity/blanco.png",):
    if not os.path.exists(os.path.join(RES, need)):
        print("AVISO: falta", need)
print("\nListo.")
