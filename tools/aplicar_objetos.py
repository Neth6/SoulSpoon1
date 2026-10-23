"""Registra ModGear en SoulSpoon.java y agrega los nombres a los archivos de idioma. Ejecutar desde la raiz del repo."""
import json, pathlib, sys

main = pathlib.Path("src/main/java/neth6/soulspoon/SoulSpoon.java")
s = main.read_text(encoding="utf-8")
if "ModGear.initialize()" not in s:
    s = s.replace("import neth6.soulspoon.item.ModItems;\n",
                  "import neth6.soulspoon.item.ModItems;\nimport neth6.soulspoon.item.ModGear;\n", 1)
    if "        ModItems.initialize();\n" not in s:
        print("!! no encontre ModItems.initialize() en SoulSpoon.java"); sys.exit(1)
    s = s.replace("        ModItems.initialize();\n", "        ModItems.initialize();\n        ModGear.initialize();\n", 1)
    main.write_text(s, encoding="utf-8")
    print("ok SoulSpoon.java")

NAMES = {
    "es_es": {"tridente_limbo": "Tridente del Limbo", "totem_corazones": "Totem de Corazones",
              "totem_dorado": "Totem Dorado", "arco_hueso": "Arco de Hueso", "espada_violeta": "Espada Violeta"},
    "en_us": {"tridente_limbo": "Limbo Trident", "totem_corazones": "Totem of Hearts",
              "totem_dorado": "Golden Totem", "arco_hueso": "Bone Bow", "espada_violeta": "Violet Sword"},
}
for lang, names in NAMES.items():
    p = pathlib.Path(f"src/main/resources/assets/soulspoon/lang/{lang}.json")
    data = json.loads(p.read_text(encoding="utf-8")) if p.exists() else {}
    for k, v in names.items():
        data.setdefault(f"item.soulspoon.{k}", v)
    p.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print("ok", p)
print("Listo. Ahora: ./gradlew build")
