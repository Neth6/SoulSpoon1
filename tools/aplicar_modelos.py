#!/usr/bin/env python3
"""
Aplica los cambios de codigo de soulspoon-modelos.zip sobre tu repo (los archivos nuevos ya los copiaste
al descomprimir). Es seguro ejecutarlo varias veces: cada cambio dice "hecho" u "omitido".

    python tools/aplicar_modelos.py
"""
import os
import re
import sys

ROOT = os.getcwd()
MAIN = os.path.join(ROOT, "src", "main", "java", "neth6", "soulspoon")
CLIENT = os.path.join(ROOT, "src", "client", "java", "neth6", "soulspoon", "client")


def edit(path, fn, label):
    if not os.path.exists(path):
        print("  NO ENCONTRADO  %s (%s)" % (label, path))
        return
    s = open(path, encoding="utf-8").read()
    n = fn(s)
    if n is None or n == s:
        print("  omitido        %s" % label)
        return
    open(path, "w", encoding="utf-8", newline="\n").write(n)
    print("  hecho          %s" % label)


def add_import(s, imp):
    line = "import %s;" % imp
    if line in s:
        return s
    m = re.search(r"^package [^;]+;\s*$", s, re.M)
    return s[:m.end()] + "\n\n" + line + s[m.end():]


def retype(var_id, new_type, drop_lines=()):
    def fn(s):
        pat = re.compile(r'(new Variant\("%s",\s*"[^"]*",\s*)EntityType\.[A-Z_]+(\))' % var_id)
        if not pat.search(s):
            return None
        s = pat.sub(r"\1%s\2" % new_type, s)
        for d in drop_lines:
            s = s.replace(d, "")
        if new_type.startswith("GeoMobs."):
            s = add_import(s, "neth6.soulspoon.entity.GeoMobs")
        else:
            s = add_import(s, "neth6.soulspoon.entity.ModEntities")
        return s
    return fn


def variants(s):
    orig = s
    table = [
        ("creeper_nuclear", "ModEntities.NUCLEAR_CREEPER"),
        ("creeper_pasto", "GeoMobs.CREEPER_PASTO"),
        ("reptisaurio_salvaje", "GeoMobs.REPTISAURIO_SALVAJE"),
        ("plumosaurio", "ModEntities.PLUMOSAURIO"),
        ("narval", "GeoMobs.NARVAL"),
        ("herobrine", "GeoMobs.HEROBRINE"),
        ("fantasma_negro", "GeoMobs.FANTASMA_NEGRO"),
        ("fantasma_blanco", "GeoMobs.FANTASMA_BLANCO"),
        ("creeper_pastel", "GeoMobs.CREEPER_PASTEL"),
    ]
    for vid, t in table:
        extra = ()
        if vid == "creeper_pasto":
            # el modelo nuevo ya es pequeno: la escala 0.5 lo dejaria diminuto
            s = re.sub(r"\n\s*attr\(mob, Attributes\.SCALE, 0\.5\);", "", s)
        r = retype(vid, t)(s)
        if r:
            s = r
    if "Variants6.defineAll();" not in s and "Variants5.defineAll();" in s:
        s = s.replace("Variants5.defineAll();", "Variants5.defineAll();\n        Variants6.defineAll();")
    return s if s != orig else None


def variants4(s):
    r = retype("creeper_pastel", "GeoMobs.CREEPER_PASTEL")(s)
    return r


def entities_client(s):
    pat = re.compile(r'\n\s*renderer\(ModEntities\.FANTASMA_AMARILLO,[^\n]*')
    if not pat.search(s):
        return None
    return pat.sub("", s)


def renderers(s):
    if "GeoMobsClient.register();" in s:
        return None
    i = s.index("public static void register() {")
    j = s.index("{", i) + 1
    return s[:j] + "\n        GeoMobsClient.register();" + s[j:]


def main_class(s):
    if "ModArmor.initialize();" in s:
        return None
    s = s.replace("ModEntities.initialize();",
                  "ModEntities.initialize();\n        GeoMobs.initialize();\n        ModArmor.initialize();", 1)
    s = add_import(s, "neth6.soulspoon.entity.GeoMobs")
    s = add_import(s, "neth6.soulspoon.item.ModArmor")
    return s


print("Aplicando cambios de codigo...")
edit(os.path.join(MAIN, "mob", "Variants.java"), variants, "Variants.java (tipos de entidad + Variants6)")
edit(os.path.join(MAIN, "mob", "Variants4.java"), variants4, "Variants4.java (Creeper Pastel, si esta ahi)")
edit(os.path.join(CLIENT, "ModEntitiesClient.java"), entities_client, "ModEntitiesClient.java (quita el fantasma amarillo viejo)")
edit(os.path.join(CLIENT, "entity", "ModEntityRenderers.java"), renderers, "ModEntityRenderers.java (registra GeoMobsClient)")
edit(os.path.join(MAIN, "SoulSpoon.java"), main_class, "SoulSpoon.java (GeoMobs + ModArmor)")

print("\nGenerando recetas, modelos de item y texturas de armadura puesta...")
os.system('"%s" tools/make_armor.py src/main/resources' % sys.executable)
print("\nListo. Ahora: gradlew build (o Run en IntelliJ).")
