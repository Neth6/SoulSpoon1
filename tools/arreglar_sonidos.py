"""Revisa y arregla la instalacion de los sonidos. Ejecutar desde la raiz del repo: python tools/arreglar_sonidos.py"""
import pathlib, re

J = pathlib.Path("src/main/java/neth6/soulspoon")
RES = pathlib.Path("src/main/resources/assets/soulspoon")

def rd(p): return p.read_text(encoding="utf-8")
def wr(p, s): p.write_text(s, encoding="utf-8")
def add_import(s, line):
    if line in s: return s
    return re.sub(r"(package [^;]+;\n)", r"\1\n" + line + "\n", s, count=1)

print("== Archivos ==")
for f in ["sounds.json", "sounds/ruleta.ogg", "sounds/notify_message.ogg"]:
    print(("OK   " if (RES / f).exists() else "FALTA"), RES / f)
print(("OK   " if (J / "sound/ModSounds.java").exists() else "FALTA"), J / "sound/ModSounds.java")

# 1) SoulSpoon.onInitialize debe llamar a ModSounds.initialize() PRIMERO
p = J / "SoulSpoon.java"; s = rd(p)
if "ModSounds.initialize()" not in s:
    s = add_import(s, "import neth6.soulspoon.sound.ModSounds;")
    s, n = re.subn(r"(public void onInitialize\(\)\s*\{\n)", r"\1        ModSounds.initialize();\n", s, count=1)
    print("SoulSpoon.java: ModSounds.initialize() agregado" if n else "!! no encontre onInitialize en SoulSpoon.java")
    wr(p, s)
else:
    print("SoulSpoon.java: ya llama a ModSounds.initialize()")

# 2) Sonido de cada anuncio (WheelRegistry.broadcast)
p = J / "wheel/WheelRegistry.java"; s = rd(p)
if "ModSounds.NOTIFY_MESSAGE" not in s:
    s = add_import(s, "import neth6.soulspoon.sound.ModSounds;")
    s, n = re.subn(r"player\.playNotifySound\([^;]*?\);", "player.playNotifySound(ModSounds.NOTIFY_MESSAGE, SoundSource.MASTER, 1.0f, 1.0f);", s, count=1)
    print("WheelRegistry.java: sonido de anuncio puesto" if n else "!! no encontre playNotifySound en WheelRegistry.java")
    wr(p, s)
else:
    print("WheelRegistry.java: ok")

# 3) Sonido de la ruleta al girar (WheelMenu.startSpin)
p = J / "wheel/WheelMenu.java"; s = rd(p)
if "ModSounds.RULETA" not in s:
    s = add_import(s, "import neth6.soulspoon.sound.ModSounds;")
    s, n = re.subn(r"[ \t]*player\.playNotifySound\([^;]*?\);\n", "        ModSounds.playAll(server, ModSounds.RULETA);\n", s, count=1)
    if not n:
        s, n = re.subn(r"([ \t]*)(WheelAnim\.play\()", r"\1ModSounds.playAll(server, ModSounds.RULETA);\n\1\2", s, count=1)
    print("WheelMenu.java: sonido de ruleta puesto" if n else "!! no encontre donde poner el sonido en WheelMenu.java")
    wr(p, s)
else:
    print("WheelMenu.java: ok")

print("\nListo. Ahora: ./gradlew build y runClient.")
print("Prueba en el juego: /sonido ruleta  y  /sonido notify  (si suenan, el audio esta bien; mira el log: 'Sonidos registrados').")
