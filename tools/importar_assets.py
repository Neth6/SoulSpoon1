"""Importa los assets de DEDSAFIO 4 al mod (sin pisar nada tuyo). Ejecutar desde la raiz del repo."""
import json, os, pathlib, re, shutil, sys

SRC = pathlib.Path("tools/assets_importados")
RES = pathlib.Path("src/main/resources")
if not SRC.exists():
    sys.exit("!! no encuentro tools/assets_importados (descomprime el zip en la raiz del repo)")

# 1) copiar archivos nuevos (si ya existe uno tuyo, NO se toca)
copied = skipped = 0
for sub in ("assets", "data"):
    for root, _, files in os.walk(SRC / sub):
        for f in files:
            sp = pathlib.Path(root) / f
            dp = RES / sp.relative_to(SRC)
            if dp.exists():
                skipped += 1
                continue
            dp.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(sp, dp)
            copied += 1
print(f"archivos copiados: {copied} | ya existian (no se tocaron): {skipped}")

# 2) idiomas: se agregan solo las claves que faltan
for lang in ("es_es", "en_us"):
    extra = json.loads((SRC / f"lang_{lang}.json").read_text(encoding="utf-8"))
    p = RES / "assets/soulspoon/lang" / f"{lang}.json"
    data = json.loads(p.read_text(encoding="utf-8")) if p.exists() else {}
    before = len(data)
    for k, v in extra.items():
        data.setdefault(k, v)
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"lang {lang}: +{len(data) - before} textos")

# 3) sounds.json: se agregan solo los sonidos que faltan
extra = json.loads((SRC / "sounds_extra.json").read_text(encoding="utf-8"))
p = RES / "assets/soulspoon/sounds.json"
data = json.loads(p.read_text(encoding="utf-8")) if p.exists() else {}
before = len(data)
for k, v in extra.items():
    data.setdefault(k, v)
p.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
print(f"sounds.json: +{len(data) - before} sonidos (los que ya tenias quedan igual)")

# 4) registrar ImportedContent AL FINAL de onInitialize
main = pathlib.Path("src/main/java/neth6/soulspoon/SoulSpoon.java")
s = main.read_text(encoding="utf-8")
if "ImportedContent.initialize()" in s:
    print("SoulSpoon.java: ya registrado")
else:
    if "import neth6.soulspoon.item.ImportedContent;" not in s:
        s = re.sub(r"(package [^;]+;\n)", r"\1\nimport neth6.soulspoon.item.ImportedContent;\n", s, count=1)
    s, n = re.subn(r"([ \t]*)(LOGGER\.info\(\"SoulSpoon cargado)", r"\1ImportedContent.initialize();\n\n\1\2", s, count=1)
    if n:
        main.write_text(s, encoding="utf-8")
        print("SoulSpoon.java: ImportedContent.initialize() agregado (al final)")
    else:
        print("!! agrega a mano al FINAL de onInitialize:  ImportedContent.initialize();")

print("\nListo. ./gradlew build   (las texturas de estos objetos llegan despues: sin ellas se ven morado/negro)")
