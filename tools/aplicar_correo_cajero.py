#!/usr/bin/env python3
"""Conecta cajero + banco + correo + entrega de mision. Se puede correr varias veces.
Uso (raiz del repo): python tools/aplicar_correo_cajero.py"""
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
if "ModCajero.initialize()" not in s:
    imp = "import net.fabricmc.api.ModInitializer;"
    s = s.replace(imp, imp + "\nimport neth6.soulspoon.banco.ModCajero;\nimport neth6.soulspoon.correo.ModCorreo;", 1)
    call = "        ModCajero.initialize();\n        ModCorreo.initialize();\n"
    if "        ModCasino.initialize();" in s:
        s = s.replace("        ModCasino.initialize();", call + "        ModCasino.initialize();", 1)
    elif "        ImportedContent.initialize();" in s:
        s = s.replace("        ImportedContent.initialize();", call + "        ImportedContent.initialize();", 1)
    else:
        sys.exit("No encuentro donde insertar en SoulSpoon.java (agrega ModCajero.initialize(); ModCorreo.initialize(); antes de ImportedContent a mano)")
    wr(p, s); print("SoulSpoon: cajero y correo agregados")
else:
    print("SoulSpoon: ya estaba")

# 2) Cliente
cp = os.path.join(CJ, "SoulSpoonClient.java")
c = rd(cp)
if "CajeroCorreoClient.initialize()" not in c:
    c2 = c.replace("        ModEntityRenderers.register();", "        ModEntityRenderers.register();\n        CajeroCorreoClient.initialize();", 1)
    if c2 == c: sys.exit("No pude enganchar el cliente (agrega CajeroCorreoClient.initialize(); en SoulSpoonClient a mano)")
    wr(cp, c2); print("SoulSpoonClient: listo")
else:
    print("SoulSpoonClient: ya estaba")

# 3) Textos
add = {"es_es": {"block.soulspoon.cajero": "Cajero", "block.soulspoon.mensajero": "Buzón",
                 "block.soulspoon.entrega_mision": "Entrega de Misión"},
       "en_us": {"block.soulspoon.cajero": "ATM", "block.soulspoon.mensajero": "Mailbox",
                 "block.soulspoon.entrega_mision": "Mission Delivery"}}
for code, d in add.items():
    lp = os.path.join(LANG, code + ".json")
    cur = json.loads(rd(lp)) if os.path.exists(lp) else {}
    new = {k: v for k, v in d.items() if k not in cur}
    if new:
        cur.update(new); wr(lp, json.dumps(cur, indent=2, ensure_ascii=False)); print("lang", code, "+", len(new))

# 4) Texturas de GUI: copia el sobre si no lo tienes (no pisa nada)
import shutil
gui = os.path.join(ROOT, "src", "main", "resources", "assets", "soulspoon", "textures", "gui")
os.makedirs(gui, exist_ok=True)
src = os.path.join(ROOT, "tools", "gui_extra", "sobre.png")
if os.path.exists(src) and not os.path.exists(os.path.join(gui, "sobre.png")):
    shutil.copyfile(src, os.path.join(gui, "sobre.png")); print("copiado textures/gui/sobre.png")
for f in ("dedita_azul.png", "dedita_verde.png", "dedita_roja.png"):
    if not os.path.exists(os.path.join(gui, f)):
        print("AVISO: falta textures/gui/" + f + " (viene en el zip de HUD de deditas; sin eso las monedas se ven como cuadro morado)")
print("\nListo. Ver LEEME.txt")
