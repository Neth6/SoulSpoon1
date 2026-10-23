"""Registra DeathSequence en SoulSpoon.java. Ejecutar desde la raiz del repo."""
import pathlib, re, sys
main = pathlib.Path("src/main/java/neth6/soulspoon/SoulSpoon.java")
s = main.read_text(encoding="utf-8")
if "DeathSequence.initialize()" in s:
    print("SoulSpoon.java: ya registrado")
else:
    if "import neth6.soulspoon.soul.DeathSequence;" not in s:
        s = re.sub(r"(package [^;]+;\n)", r"\1\nimport neth6.soulspoon.soul.DeathSequence;\n", s, count=1)
    s, n = re.subn(r"([ \t]*)(WheelAnim\.initialize\(\);\n)", r"\1\2\1DeathSequence.initialize();\n", s, count=1)
    if not n:
        print("!! agrega a mano en onInitialize:  DeathSequence.initialize();"); sys.exit(1)
    main.write_text(s, encoding="utf-8")
    print("SoulSpoon.java: DeathSequence.initialize() agregado")
print("Listo. Ahora: ./gradlew build")
