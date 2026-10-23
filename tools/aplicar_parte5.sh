#!/usr/bin/env bash
# Registra /dedsafio ir en SoulSpoon.java (se puede ejecutar varias veces).
J=src/main/java/neth6/soulspoon
if ! grep -q "DimCommands" $J/SoulSpoon.java; then
  sed -i 's|        RuletaCommands.initialize();|        neth6.soulspoon.world.DimCommands.initialize();\n        RuletaCommands.initialize();|' $J/SoulSpoon.java
  echo "OK  SoulSpoon.java actualizado"
else echo "--  ya estaba"; fi
