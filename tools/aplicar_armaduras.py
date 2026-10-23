#!/usr/bin/env python3
"""
Cambia los tiers de armadura a Primitiva (tier 1) y Glebanoide (tier 2, mas fuerte).
Borra los archivos viejos de carmesi/radiante y regenera recetas y modelos de item.
    python tools/aplicar_armaduras.py
"""
import glob, os, re, subprocess, sys
RES = os.path.join("src", "main", "resources")
n = 0
for pat in ("assets/soulspoon/textures/item/*_carmesi.png", "assets/soulspoon/textures/item/*_radiante.png",
            "assets/soulspoon/models/item/*_carmesi.json", "assets/soulspoon/models/item/*_radiante.json",
            "assets/soulspoon/textures/models/armor/carmesi_layer_*.png",
            "assets/soulspoon/textures/models/armor/radiante_layer_*.png",
            "data/soulspoon/recipe/*_carmesi_mejora.json", "data/soulspoon/recipe/*_radiante_mejora.json"):
    for f in glob.glob(os.path.join(RES, pat)):
        os.remove(f); n += 1
print("archivos viejos borrados:", n)
# primitiva no trae textura puesta propia: se regenera a partir de su icono
subprocess.call([sys.executable, "tools/make_armor.py", RES])
for f in glob.glob("src/**/*.java", recursive=True):
    s = open(f, encoding="utf-8").read()
    if re.search(r"ModArmor\.(CASCO|PETO|PANTALONES|BOTAS|PLANTILLA)_(CARMESI|RADIANTE)", s):
        print("AVISO: usa nombres viejos de armadura ->", f)
print("Listo. Recarga Gradle y Run.")
