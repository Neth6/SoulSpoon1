# SoulSpoon1 integrado con partes 2 y 3

- Se conserva el código fuente y los recursos del SoulSpoon1 original.
- Se agrega `RuleCommands` en el paquete compilable `src/main/java/neth6/soulspoon/command/`.
- `SoulSpoon.onInitialize()` conserva las inicializaciones originales y registra `RuleCommands.initialize()`.
- Las clases compartidas de partes 2 y 3 que ya eran idénticas no se reemplazaron.
- El ZIP original no incluía `build.gradle`, `settings.gradle`, `gradle.properties` ni los lanzadores `gradlew`; se añadieron configuraciones Fabric Loom para permitir probar la compilación.

## Compilar en Windows
1. Instala JDK 21.
2. Abre una terminal en esta carpeta.
3. Ejecuta `\.\gradlew.bat build`.
4. Si falla, comparte el bloque completo desde `* What went wrong:` y las primeras líneas `error:`.

La compilación no se marca como verificada: el entorno de preparación no dispone de acceso garantizado a Maven/Gradle para resolver dependencias.
