"""Ruleta: la animacion dura lo mismo que el audio. Ejecutar desde la raiz del repo."""
import pathlib, re, sys

p = pathlib.Path("src/main/java/neth6/soulspoon/wheel/WheelMenu.java")
s = p.read_text(encoding="utf-8")
if "playFitted" in s:
    print("WheelMenu.java: ya estaba ajustado")
else:
    s, n = re.subn(r"WheelAnim\.play\(\s*server\s*,\s*landed\s*,", "WheelAnim.playFitted(server, landed, ModSounds.RULETA_TICKS,", s, count=1)
    if not n:
        print("!! no encontre WheelAnim.play(server, landed, ...) en WheelMenu.java"); sys.exit(1)
    if "import neth6.soulspoon.sound.ModSounds;" not in s:
        s = re.sub(r"(package [^;]+;\n)", r"\1\nimport neth6.soulspoon.sound.ModSounds;\n", s, count=1)
    p.write_text(s, encoding="utf-8")
    print("ok WheelMenu.java")
print("Listo. Ahora: ./gradlew build")
