#!/usr/bin/env python3
"""Mejora del Limbo: reglas (sin romper/poner bloques, tridente solo ahi, agua que quita corazones),
/limbo devolver|ver y /spawn_limbo. Se puede correr varias veces.
Uso (raiz del repo): python tools/aplicar_limbo.py"""
import json, os, sys
ROOT = os.getcwd()
J = os.path.join(ROOT, "src", "main", "java", "neth6", "soulspoon")
RES = os.path.join(ROOT, "src", "main", "resources")
def rd(p):
    with open(p, encoding="utf-8") as f: return f.read()
def wr(p, t):
    with open(p, "w", encoding="utf-8", newline="\n") as f: f.write(t)

p = os.path.join(J, "SoulSpoon.java"); s = rd(p)
if "Limbo.initialize()" not in s:
    imp = "import net.fabricmc.api.ModInitializer;"
    s = s.replace(imp, imp + "\nimport neth6.soulspoon.world.Limbo;", 1)
    m = "        ImportedContent.initialize();"
    if m not in s: sys.exit("Agrega Limbo.initialize(); antes de ImportedContent.initialize(); a mano")
    s = s.replace(m, "        Limbo.initialize();\n" + m, 1); wr(p, s); print("SoulSpoon: listo")
else: print("SoulSpoon: ya estaba")

# /dimension limbo y los demas usos pasan por el Limbo nuevo (spawn propio, sonido, aviso)
p = os.path.join(J, "world", "Dimensions.java"); d = rd(p)
if "Limbo.mandar(player)" not in d:
    sig = "public static void sendToLimbo(ServerPlayer player) {"
    if sig in d:
        d = d.replace(sig, "public static void sendToLimbo(ServerPlayer player) {\n        Limbo.mandar(player);\n    }\n\n    @SuppressWarnings(\"unused\")\n    private static void sendToLimboAntiguo(ServerPlayer player) {", 1)
        wr(p, d); print("Dimensions: sendToLimbo usa Limbo.mandar")
    else: print("AVISO: no encontre sendToLimbo en Dimensions")
else: print("Dimensions: ya estaba")

mp = os.path.join(RES, "soulspoon.mixins.json")
if os.path.exists(mp):
    j = json.loads(rd(mp)); l = j.setdefault("mixins", [])
    if "TridenteLimboMixin" not in l:
        l.append("TridenteLimboMixin"); wr(mp, json.dumps(j, indent="\t")); print("mixin agregado")
dt = os.path.join(RES, "data", "soulspoon", "damage_type", "agua_limbo.json")
if not os.path.exists(dt): print("AVISO: falta data/soulspoon/damage_type/agua_limbo.json (corre tools/importar_datos.py)")
print("\nListo. Comandos: /limbo ver <jugador>, /limbo devolver [jugadores], /spawn_limbo [x y z|quitar]")
