#!/usr/bin/env python3
"""Conecta Subastas/Vitrina, Candados, Marcas, Minimapa e Intercambio. Se puede correr varias veces.
Requiere haber instalado antes soulspoon-hermandad-boveda.zip (Marcas: este zip reemplaza MarcasPayload.java por la version completa).
Uso (raiz del repo): python tools/aplicar_social.py"""
import json, os, sys

ROOT = os.getcwd()
J = os.path.join(ROOT, "src", "main", "java", "neth6", "soulspoon")
CJ = os.path.join(ROOT, "src", "client", "java", "neth6", "soulspoon", "client")
RES = os.path.join(ROOT, "src", "main", "resources")
RA = os.path.join(RES, "assets", "soulspoon")

def rd(p):
    with open(p, encoding="utf-8") as f: return f.read()
def wr(p, t):
    with open(p, "w", encoding="utf-8", newline="\n") as f: f.write(t)

p = os.path.join(J, "SoulSpoon.java"); s = rd(p)
if "ModSocial.initialize()" not in s:
    m = "        ImportedContent.initialize();"
    if m not in s: sys.exit("Agrega ModSocial.initialize(); antes de ImportedContent.initialize(); a mano")
    s = s.replace(m, "        ModSocial.initialize();\n" + m, 1); wr(p, s); print("SoulSpoon: listo")
else: print("SoulSpoon: ya estaba")

cp = os.path.join(CJ, "SoulSpoonClient.java"); c = rd(cp)
if "SocialClient.initialize()" not in c:
    c2 = c.replace("        ModEntityRenderers.register();", "        ModEntityRenderers.register();\n        SocialClient.initialize();", 1)
    if c2 == c: sys.exit("Agrega SocialClient.initialize(); en SoulSpoonClient a mano")
    wr(cp, c2); print("SoulSpoonClient: listo")
else: print("SoulSpoonClient: ya estaba")

def add_mixin(path, names, key):
    if not os.path.exists(path): print("AVISO: no existe", path); return
    j = json.loads(rd(path)); lst = j.setdefault(key, [])
    new = [n for n in names if n not in lst]
    if new: lst.extend(new); wr(path, json.dumps(j, indent="\t")); print("mixins +", new)
add_mixin(os.path.join(RES, "soulspoon.mixins.json"), ["HopperBlockEntityMixin", "ChestBlockMixin", "ExplosionCandadoMixin"], "mixins")
add_mixin(os.path.join(ROOT, "src", "client", "resources", "soulspoon.client.mixins.json"), ["ChestRendererMixin"], "client")

LANG = os.path.join(RA, "lang")
add = {"es_es": {"block.soulspoon.vitrina": "Vitrina", "block.soulspoon.puesto_intercambio": "Puesto de Intercambio",
                 "item.soulspoon.candado": "Candado", "item.soulspoon.llave_candado": "Llave de Candado",
                 "container.soulspoon.subastas": "Subastas"},
       "en_us": {"block.soulspoon.vitrina": "Display Case", "block.soulspoon.puesto_intercambio": "Trading Post",
                 "item.soulspoon.candado": "Padlock", "item.soulspoon.llave_candado": "Padlock Key",
                 "container.soulspoon.subastas": "Auctions"}}
for code, d in add.items():
    lp = os.path.join(LANG, code + ".json")
    cur = json.loads(rd(lp)) if os.path.exists(lp) else {}
    new = {k: v for k, v in d.items() if k not in cur}
    if new: cur.update(new); wr(lp, json.dumps(cur, indent=2, ensure_ascii=False)); print("lang", code, "+", len(new))
print("\nListo. Comandos: /ah (subastas), /marca..., /minimapa...")
