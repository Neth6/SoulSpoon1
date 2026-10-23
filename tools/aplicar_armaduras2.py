#!/usr/bin/env python3
"""
Arregla la armadura puesta (Primitiva y Glebanoide) y agrega Cedronica y Limbo.
Ejecutar desde la raiz del repo:  python tools/aplicar_armaduras2.py
Requiere Pillow y numpy:  python -m pip install pillow numpy
"""
import os, shutil, subprocess, sys

RES = os.path.join("src", "main", "resources")
ARM = os.path.join(RES, "assets", "soulspoon", "textures", "models", "armor")

# guarda copia de las texturas puestas anteriores por si las quieres de vuelta
backup = os.path.join("tools", "backup_texturas_armadura")
os.makedirs(backup, exist_ok=True)
for name in os.listdir(ARM) if os.path.isdir(ARM) else []:
    shutil.copy2(os.path.join(ARM, name), os.path.join(backup, name))
print("copia de las texturas puestas anteriores en", backup)

code = subprocess.call([sys.executable, os.path.join("tools", "make_armor.py"), RES])
if code != 0:
    print("!! make_armor.py fallo (falta Pillow/numpy?)  ->  python -m pip install pillow numpy")
    sys.exit(1)
# ModArmor debe inicializarse en SoulSpoon.onInitialize (si no, las piezas no existen en el juego)
main = os.path.join("src", "main", "java", "neth6", "soulspoon", "SoulSpoon.java")
if os.path.exists(main):
    src = open(main, encoding="utf-8").read()
    if "ModArmor.initialize()" not in src:
        import re
        if "import neth6.soulspoon.item.ModArmor;" not in src:
            src = re.sub(r"(package [^;]+;\n)", r"\1\nimport neth6.soulspoon.item.ModArmor;\n", src, count=1)
        src, n = re.subn(r"([ \t]*)(ModItems\.initialize\(\);\n)", r"\1\2\1ModArmor.initialize();\n", src, count=1)
        if n:
            open(main, "w", encoding="utf-8").write(src)
            print("SoulSpoon.java: ModArmor.initialize() agregado")
        else:
            print("!! agrega a mano ModArmor.initialize(); en SoulSpoon.onInitialize")
    else:
        print("SoulSpoon.java: ModArmor ya se inicializa")
print("Listo. Recarga Gradle y Run (./gradlew build).")
