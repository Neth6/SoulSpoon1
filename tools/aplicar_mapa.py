#!/usr/bin/env python3
"""Mapa a pantalla completa con la tecla M. Requiere soulspoon-hermandad-boveda.zip y soulspoon-social.zip instalados.
Uso (raiz del repo): python tools/aplicar_mapa.py"""
import json, os, sys
ROOT = os.getcwd()
CJ = os.path.join(ROOT, "src", "client", "java", "neth6", "soulspoon", "client")
LANG = os.path.join(ROOT, "src", "main", "resources", "assets", "soulspoon", "lang")
def rd(p):
    with open(p, encoding="utf-8") as f: return f.read()
def wr(p, t):
    with open(p, "w", encoding="utf-8", newline="\n") as f: f.write(t)
cp = os.path.join(CJ, "SoulSpoonClient.java"); c = rd(cp)
if "MapaClient.initialize()" not in c:
    c2 = c.replace("        ModEntityRenderers.register();", "        ModEntityRenderers.register();\n        MapaClient.initialize();", 1)
    if c2 == c: sys.exit("Agrega MapaClient.initialize(); en SoulSpoonClient a mano")
    wr(cp, c2); print("SoulSpoonClient: listo")
else: print("SoulSpoonClient: ya estaba")
for code, txt in (("es_es", "Abrir mapa"), ("en_us", "Open map")):
    lp = os.path.join(LANG, code + ".json")
    cur = json.loads(rd(lp)) if os.path.exists(lp) else {}
    if "key.soulspoon.mapa" not in cur:
        cur["key.soulspoon.mapa"] = txt; wr(lp, json.dumps(cur, indent=2, ensure_ascii=False)); print("lang", code)
print("Listo. Tecla M (cambiable en Controles). Si usas Xaero's, desactiva su tecla M.")
