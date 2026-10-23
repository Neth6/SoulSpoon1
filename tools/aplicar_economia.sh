#!/usr/bin/env bash
# Registra la economia (servidor y cliente). Se puede ejecutar varias veces.
set -e
J=src/main/java/neth6/soulspoon/SoulSpoon.java
C=src/client/java/neth6/soulspoon/client/SoulSpoonClient.java
if ! grep -q "economy.Economy.initialize" $J; then
  sed -i 's|        QuestService.initialize();|        QuestService.initialize();\n        neth6.soulspoon.economy.Economy.initialize();|' $J
  echo "OK  SoulSpoon.java actualizado"
else echo "--  SoulSpoon.java ya estaba"; fi
if ! grep -q "EconomyHud.register" $C; then
  sed -i 's|        ModEntitiesClient.initialize();|        ModEntitiesClient.initialize();\n        EconomyHud.register();|' $C
  echo "OK  SoulSpoonClient.java actualizado"
else echo "--  SoulSpoonClient.java ya estaba"; fi
