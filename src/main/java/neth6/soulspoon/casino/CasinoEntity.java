package neth6.soulspoon.casino;

import neth6.soulspoon.SoulSpoon;
import neth6.soulspoon.economy.Dedita;
import neth6.soulspoon.economy.Economy;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.animation.AnimationProcessor.QueuedAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class CasinoEntity extends Mob implements GeoEntity {
   private static final RawAnimation QUIETO = RawAnimation.begin().thenLoop("idle");
   private static final Map<String, CasinoEntity.Resultado> RESULTADOS = Map.ofEntries(
      Map.entry("4", new CasinoEntity.Resultado("", 1, 202, 240)),
      Map.entry("5", new CasinoEntity.Resultado("", 1, 205, 240)),
      Map.entry("7", new CasinoEntity.Resultado("", 1, 179, 240)),
      Map.entry("1", new CasinoEntity.Resultado("botella de experiencia", 2, 230, 320)),
      Map.entry("3", new CasinoEntity.Resultado("corazon", 2, 201, 271)),
      Map.entry("6", new CasinoEntity.Resultado("libro de encantamientos", 2, 160, 199)),
      Map.entry("2 hierro", new CasinoEntity.Resultado("hierro", 2, 230, 320)),
      Map.entry("2 pechera de hierro", new CasinoEntity.Resultado("pechera de hierro", 2, 201, 271)),
      Map.entry("2 pico de hierro", new CasinoEntity.Resultado("pico de hierro", 2, 160, 199)),
      Map.entry("2 filete", new CasinoEntity.Resultado("filete", 2, 230, 320)),
      Map.entry("2 poción", new CasinoEntity.Resultado("poción", 2, 201, 271)),
      Map.entry("libro de encantamientos", new CasinoEntity.Resultado("libro de encantamientos", 3, 148, 188)),
      Map.entry("pico de hierro", new CasinoEntity.Resultado("pico de hierro", 3, 170, 220)),
      Map.entry("pechera de hierro", new CasinoEntity.Resultado("pechera de hierro", 3, 181, 220)),
      Map.entry("filete", new CasinoEntity.Resultado("filete", 3, 202, 235)),
      Map.entry("corazon", new CasinoEntity.Resultado("corazon", 3, 204, 240)),
      Map.entry("hierro", new CasinoEntity.Resultado("hierro", 3, 201, 240)),
      Map.entry("poción", new CasinoEntity.Resultado("poción", 3, 162, 200)),
      Map.entry("botella de experiencia", new CasinoEntity.Resultado("botella de experiencia", 3, 176, 220))
   );
   static final int GIRO = 168;
   private static final int PROBABILIDAD_TRES = 20;
   private static final int PROBABILIDAD_DOS = 30;
   private static final int PROBABILIDAD_TRES_CORAZONES = 1;
   public static final List<String> ANIMACIONES = RESULTADOS.keySet().stream().sorted().toList();
   private int girandoHasta = 0;
   private CasinoEntity.Resultado pendiente;
   private ItemStack premio = ItemStack.EMPTY;
   private UUID jugadorPendiente;
   private int premioEn;
   private final AnimatableInstanceCache animaciones = GeckoLibUtil.createInstanceCache(this);

   public CasinoEntity(EntityType<? extends CasinoEntity> tipo, Level mundo) {
      super(tipo, mundo);
      this.setPersistenceRequired();
   }

   public static Builder crearAtributos() {
      return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
   }

   public SpawnGroupData finalizeSpawn(ServerLevelAccessor mundo, DifficultyInstance dificultad, MobSpawnType tipo, SpawnGroupData datos) {
      Player jugador = mundo.getNearestPlayer(this, 16.0);
      if (jugador != null) {
         float giro = jugador.getDirection().getOpposite().toYRot();
         this.setYRot(giro);
         this.yRotO = giro;
      }

      return super.finalizeSpawn(mundo, dificultad, tipo, datos);
   }

   public void tick() {
      super.tick();
      if (!this.level().isClientSide && this.pendiente != null && this.tickCount >= this.premioEn) {
         if (this.level().getPlayerByUUID(this.jugadorPendiente) instanceof ServerPlayer jugador) {
            CasinoPremios.entregar(jugador, this.premio, this);
         }

         if (!this.premio.isEmpty()) {
            this.level().playSound(null, this.blockPosition(), ModCasino.sonidoGana(), SoundSource.BLOCKS, 1.0F, 1.0F);
         }

         this.pendiente = null;
         this.premio = ItemStack.EMPTY;
      }

      this.yBodyRot = this.getYRot();
      this.yBodyRotO = this.getYRot();
      this.yHeadRot = this.getYRot();
      this.yHeadRotO = this.getYRot();
   }

   public boolean hurt(DamageSource fuente, float cantidad) {
      if (fuente.getEntity() instanceof ServerPlayer jugador && jugador.isShiftKeyDown()) {
         CasinoNet.abrir(jugador);
         return false;
      }

      if (fuente.getEntity() instanceof Player p && p.isCreative()) {
         return super.hurt(fuente, Float.MAX_VALUE);
      } else {
         return fuente.is(DamageTypeTags.BYPASSES_INVULNERABILITY) ? super.hurt(fuente, cantidad) : false;
      }
   }

   public boolean isPushable() {
      return false;
   }

   protected void doPush(Entity otro) {
   }

   public boolean removeWhenFarAway(double distancia) {
      return false;
   }

   public void registerControllers(ControllerRegistrar controladores) {
      AnimationController<CasinoEntity> principal = new AnimationController(this, "principal", 0, estado -> estado.setAndContinue(QUIETO));

      for (String nombre : ANIMACIONES) {
         principal.triggerableAnim(nombre, RawAnimation.begin().thenPlay(nombre));
      }

      principal.setAnimationSpeedHandler(casino -> {
         QueuedAnimation actual = principal.getCurrentAnimation();
         CasinoEntity.Resultado r = actual == null ? null : RESULTADOS.get(actual.animation().name());
         return r == null ? 1.0 : r.duracion() / 168.0;
      });
      controladores.add(principal);
   }

   protected InteractionResult mobInteract(Player jugador, InteractionHand mano) {
      ItemStack item = jugador.getItemInHand(mano);
      Item ficha = BuiltInRegistries.ITEM.getOptional(SoulSpoon.id("dedita_casino")).orElse(null);

      if (this.level().isClientSide) {
         return InteractionResult.SUCCESS;
      }

      if (this.tickCount < this.girandoHasta) {
         jugador.displayClientMessage(Component.literal("El casino está girando...").withStyle(ChatFormatting.GOLD), true);
         return InteractionResult.CONSUME;
      }

      long precio = CasinoPremios.modo(this.level().getServer()).precio;
      if (!cobrar(jugador, ficha, precio)) {
         jugador.displayClientMessage(Component.literal("Cada giro: 1 dedita naranja o " + precio + " (" + Dedita.describe(precio) + ").").withStyle(ChatFormatting.RED), true);
         return InteractionResult.CONSUME;
      }

      RandomSource azar = this.getRandom();
      CasinoPremios.Modo modoCasino = CasinoPremios.modo(this.level().getServer());
      List<CasinoPool.Entry> tabla = modoCasino.tabla();
      CasinoPool.Entry sorteo = CasinoPool.elegir(tabla, azar);
      double prob = CasinoPool.total(tabla) <= 0 ? 0.0 : (double) sorteo.weight() / CasinoPool.total(tabla);
      String tirada;
      if (sorteo.nada()) {
         tirada = elegirAnimacion(1, azar);
      } else if (sorteo.stack().is(CasinoPool.corazon())) {
         tirada = "corazon";
      } else if (prob <= 0.08) {
         tirada = elegirAnimacion(3, azar);
      } else {
         tirada = elegirAnimacion(2, azar);
      }
      this.premio = sorteo.stack().copy();

      String forzada = System.getenv("SOULSPOON_CASINO_FORZAR");
      if (forzada != null && RESULTADOS.containsKey(forzada)) {
         tirada = forzada;
      }

      this.animar(tirada);
      this.pendiente = RESULTADOS.get(tirada);
      this.girandoHasta = this.tickCount + 168;
      this.jugadorPendiente = jugador.getUUID();
      this.premioEn = this.tickCount + Math.round(this.pendiente.parada() * 168.0F / this.pendiente.duracion());
      this.level().playSound(null, this.blockPosition(), SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 1.0F, 0.8F);
      this.level().playSound(null, this.blockPosition(), ModCasino.sonidoGira(), SoundSource.BLOCKS, 1.0F, 1.0F);
      return InteractionResult.CONSUME;
   }

   /** Primero gasta una dedita naranja (mano o inventario); si no hay, descuenta del saldo. */
   private static boolean cobrar(Player jugador, Item ficha, long precio) {
      if (ficha != null) {
         for (int i = 0; i < jugador.getInventory().getContainerSize(); i++) {
            ItemStack s = jugador.getInventory().getItem(i);
            if (s.is(ficha)) {
               s.shrink(1);
               return true;
            }
         }
      }
      return jugador instanceof ServerPlayer sp && Economy.spend(sp, precio);
   }

   private static String elegirAnimacion(int iguales, RandomSource azar) {
      List<String> nombres = ANIMACIONES.stream().filter(n -> RESULTADOS.get(n).cantidad() == iguales && !n.equals("corazon")).toList();
      return nombres.get(azar.nextInt(nombres.size()));
   }

   public void animar(String nombre) {
      this.triggerAnim("principal", nombre);
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.animaciones;
   }

   protected void registerGoals() {
   }

   public boolean isInvulnerableTo(DamageSource fuente) {
      return super.isInvulnerableTo(fuente) || fuente.is(DamageTypeTags.IS_FALL) || fuente.is(DamageTypeTags.IS_DROWNING);
   }

   private record Resultado(String figura, int cantidad, int parada, int duracion) {
   }
}
