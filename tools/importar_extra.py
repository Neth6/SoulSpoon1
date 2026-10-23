"""Copia el GUI del catalogo y las fuentes de iconos SIN pisar nada tuyo. Ejecutar desde la raiz del repo."""
import os, pathlib, shutil, sys
SRC = pathlib.Path("tools/importados_extra"); RES = pathlib.Path("src/main/resources")
if not SRC.exists(): sys.exit("!! falta tools/importados_extra")
new, kept = 0, []
for root, _, files in os.walk(SRC):
    for f in files:
        sp = pathlib.Path(root) / f; dp = RES / sp.relative_to(SRC)
        if dp.exists(): kept.append(str(dp.relative_to(RES))); continue
        dp.parent.mkdir(parents=True, exist_ok=True); shutil.copy2(sp, dp); new += 1
print(f"copiados: {new}")
for k in kept: print("  ya existia, no se toco:", k)
