"""Parchea WheelRegistry, WheelMenu y SoulSpoon para usar sonidos e iconos. Ejecutar desde la raiz del repo."""
import pathlib, sys

R = pathlib.Path("src/main/java/neth6/soulspoon")

def patch(path, pairs):
    p = R / path
    s = p.read_text(encoding="utf-8")
    for old, new in pairs:
        if new in s:
            continue
        if old not in s:
            print("!! no encontre el texto en", path, "->", old[:50]); sys.exit(1)
        s = s.replace(old, new, 1)
    p.write_text(s, encoding="utf-8")
    print("ok", path)

patch("wheel/WheelRegistry.java", [
    ("import neth6.soulspoon.rule.RuleData;\n",
     "import neth6.soulspoon.rule.RuleData;\nimport neth6.soulspoon.sound.ModSounds;\n"),
    ("Component titleText = Component.literal(title).withStyle(color.format());",
     "Component titleText = WheelIcons.of(color).append(Component.literal(\" \"))\n"
     "                .append(Component.literal(title).withStyle(color.format()));"),
    ("            player.playNotifySound(SoundEvents.BEACON_ACTIVATE, SoundSource.MASTER, 1.0f, 1.0f);\n",
     "            player.playNotifySound(ModSounds.NOTIFY_MESSAGE, SoundSource.MASTER, 1.0f, 1.0f);\n"),
    ("""                Component.literal("[" + color.colorName() + "] ")
                        .withStyle(color.format())
                        .append(Component.literal(title + ": " + subtitle)),""",
     """                WheelIcons.of(color).append(Component.literal(" "))
                        .append(Component.literal(title).withStyle(color.format()))
                        .append(Component.literal(subtitle.isEmpty() ? "" : ": " + subtitle)),"""),
])
patch("wheel/WheelMenu.java", [
    ("import net.minecraft.sounds.SoundEvents;\n", "import neth6.soulspoon.sound.ModSounds;\nimport net.minecraft.sounds.SoundEvents;\n"),
    ("        player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.MASTER, 1.0f, 1.0f);\n",
     "        ModSounds.playAll(server, ModSounds.RULETA);\n"),
])
patch("SoulSpoon.java", [
    ("import neth6.soulspoon.wheel.ModWheel;\n",
     "import neth6.soulspoon.wheel.ModWheel;\nimport neth6.soulspoon.sound.ModSounds;\nimport neth6.soulspoon.command.AnnounceCommand;\n"),
    ("        ModWheel.initialize();\n",
     "        ModSounds.initialize();\n        ModWheel.initialize();\n"),
    ("        RuletaCommands.initialize();\n",
     "        RuletaCommands.initialize();\n        AnnounceCommand.initialize();\n"),
])
print("Listo. Ahora: ./gradlew build")
