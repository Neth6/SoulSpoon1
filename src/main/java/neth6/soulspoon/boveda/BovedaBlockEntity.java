package neth6.soulspoon.boveda;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BovedaBlockEntity extends BlockEntity {
   public static final float DURACION = 80.0F;
   private static final int DEMORA_PANTALLA = 34;
   private float desde;
   private long inicio = -1L;
   private final Map<UUID, Integer> pendientes = new HashMap<>();
   private int ticks = 1000;

   public BovedaBlockEntity(BlockPos pos, BlockState estado) {
      super(ModBoveda.ENTIDAD, pos, estado);
   }

   private boolean abierta() {
      return (Boolean)this.getBlockState().getValue(BovedaBlock.ABIERTA);
   }

   public void setBlockState(BlockState nuevo) {
      boolean antes = this.abierta();
      float actual = this.progreso(0.0F);
      super.setBlockState(nuevo);
      if (this.level != null && this.level.isClientSide && antes != (Boolean)nuevo.getValue(BovedaBlock.ABIERTA)) {
         this.desde = actual;
         this.inicio = this.level.getGameTime();
      }
   }

   public float progreso(float parcial) {
      boolean abierta = this.abierta();
      if (this.inicio >= 0L && this.level != null) {
         float avance = ((float)(this.level.getGameTime() - this.inicio) + parcial) / 80.0F;
         return abierta ? Math.min(1.0F, this.desde + avance) : Math.max(0.0F, this.desde - avance);
      } else {
         return abierta ? 1.0F : 0.0F;
      }
   }

   void pedirApertura(ServerPlayer jugador) {
      if (this.level != null) {
         if (!this.abierta()) {
            BovedaBlock.setAbierta(this.level, this.worldPosition, this.getBlockState(), true);
            this.ticks = 0;
         }

         this.pendientes.put(jugador.getUUID(), Math.max(1, 34 - this.ticks));
      }
   }

   public static void tickServidor(Level level, BlockPos pos, BlockState estado, BovedaBlockEntity boveda) {
      boveda.ticks++;
      boveda.sonidos(level, pos.above());
      Iterator<Entry<UUID, Integer>> it = boveda.pendientes.entrySet().iterator();

      while (it.hasNext()) {
         Entry<UUID, Integer> pendiente = it.next();
         int quedan = pendiente.getValue() - 1;
         if (quedan > 0) {
            pendiente.setValue(quedan);
         } else {
            it.remove();
            if (level.getPlayerByUUID(pendiente.getKey()) instanceof ServerPlayer jugador && jugador.isAlive()) {
               ModBoveda.abrirMenu(jugador, pos);
            }
         }
      }

      if ((Boolean)estado.getValue(BovedaBlock.ABIERTA)
         && boveda.pendientes.isEmpty()
         && boveda.ticks > 44
         && level.getGameTime() % 10L == 0L
         && !enUso((ServerLevel)level, pos)) {
         BovedaBlock.setAbierta(level, pos, estado, false);
         boveda.ticks = 0;
      }
   }

   private static boolean enUso(ServerLevel level, BlockPos pos) {
      for (ServerPlayer jugador : level.players()) {
         if (jugador.containerMenu instanceof BovedaMenu menu && menu.pos().equals(pos)) {
            return true;
         }
      }

      return false;
   }

   private void sonidos(Level level, BlockPos centro) {
      boolean abriendo = this.abierta();
      int rueda = abriendo ? 1 : 62;
      int cerrojos = abriendo ? 14 : 48;
      int bisagras = abriendo ? 34 : 1;
      if (this.ticks == rueda) {
         sonar(level, centro, SoundEvents.GRINDSTONE_USE, 0.8F, 0.6F);
      }

      if (this.ticks == cerrojos) {
         sonar(level, centro, abriendo ? SoundEvents.PISTON_CONTRACT : SoundEvents.PISTON_EXTEND, 0.7F, 0.8F);
      }

      if (this.ticks == bisagras) {
         sonar(level, centro, abriendo ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE, 1.0F, 0.5F);
      }

      if (!abriendo && this.ticks == 76) {
         sonar(level, centro, SoundEvents.ANVIL_LAND, 0.3F, 1.6F);
      }
   }

   private static void sonar(Level level, BlockPos pos, SoundEvent sonido, float volumen, float tono) {
      level.playSound(null, pos, sonido, SoundSource.BLOCKS, volumen, tono);
   }
}
