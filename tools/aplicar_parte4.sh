#!/usr/bin/env bash
# Aplica los 3 cambios pequeños de la Parte 4 en tus archivos (se puede ejecutar varias veces).
# Uso, desde la carpeta del proyecto:  bash tools/aplicar_parte4.sh
set -e
J=src/main/java/neth6/soulspoon
C=src/client/java/neth6/soulspoon/client/SoulSpoonClient.java

# 1) Variants.register(...)
if ! grep -q "public static Variant register" $J/mob/Variants.java; then
  sed -i 's|    private static Variant add(Variant variant) {|    public static Variant register(Variant variant) {\n        return add(variant);\n    }\n\n    private static Variant add(Variant variant) {|' $J/mob/Variants.java
  echo "OK  Variants.register agregado"
else echo "--  Variants ya tenia register"; fi

# 2) SoulSpoon.onInitialize
if ! grep -q "ModEntities.initialize" $J/SoulSpoon.java; then
  sed -i 's|        VariantManager.initialize();|        neth6.soulspoon.entity.ModEntities.initialize();\n        neth6.soulspoon.mob.MindControl.initialize();\n        neth6.soulspoon.mob.Variants4.defineAll();\n        VariantManager.initialize();|' $J/SoulSpoon.java
  echo "OK  SoulSpoon.java actualizado"
else echo "--  SoulSpoon.java ya estaba"; fi

# 3) SoulSpoonClient.onInitializeClient
if [ ! -f "$C" ]; then C=$(grep -rl "onInitializeClient" src | head -1); fi
if ! grep -q "ModEntitiesClient" "$C"; then
  sed -i '/onInitializeClient() *{/a\        neth6.soulspoon.client.ModEntitiesClient.initialize();' "$C"
  echo "OK  $C actualizado"
else echo "--  el cliente ya estaba"; fi
