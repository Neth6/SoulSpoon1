#!/bin/bash
# Conecta el Limbo (Parte 5) al mod: agrega Limbo.initialize() en SoulSpoon.java.
set -e
F=src/main/java/neth6/soulspoon/SoulSpoon.java
if grep -q "Limbo.initialize" "$F"; then
  echo "Limbo.initialize() ya estaba agregado"
else
  sed -i 's/^\(\s*\)Dimensions.initialize();/&\n\1neth6.soulspoon.world.limbo.Limbo.initialize();/' "$F"
  grep -q "Limbo.initialize" "$F" && echo "Agregado Limbo.initialize() en SoulSpoon.java" \
    || echo "NO se pudo agregar: pon a mano neth6.soulspoon.world.limbo.Limbo.initialize(); despues de Dimensions.initialize();"
fi
