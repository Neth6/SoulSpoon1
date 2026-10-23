"""Copia las texturas importadas al mod SIN pisar las tuyas. Ejecutar desde la raiz del repo.
   python tools/importar_texturas.py            -> solo agrega las que faltan
   python tools/importar_texturas.py --forzar   -> pisa las que ya existan (usalo con cuidado)"""
import os, pathlib, shutil, sys

SRC = pathlib.Path("tools/texturas_importadas")
RES = pathlib.Path("src/main/resources")
FORCE = "--forzar" in sys.argv
if not SRC.exists():
    sys.exit("!! no encuentro tools/texturas_importadas (descomprime el zip en la raiz del repo)")
new = 0
kept = []
for root, _, files in os.walk(SRC):
    for f in files:
        sp = pathlib.Path(root) / f
        dp = RES / sp.relative_to(SRC)
        if dp.exists() and not FORCE:
            kept.append(str(dp.relative_to(RES)))
            continue
        dp.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(sp, dp)
        new += 1
print(f"texturas copiadas: {new}")
if kept:
    print(f"ya existian y NO se tocaron ({len(kept)}):")
    for k in kept[:30]:
        print("  ", k)
print("Listo. ./gradlew build")
