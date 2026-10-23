package neth6.soulspoon.intercambio;

import neth6.soulspoon.economy.Coins;
import neth6.soulspoon.economy.Ids;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;

public final class ModIntercambio {
   public static final Block PUESTO = (Block)Registry.register(
      BuiltInRegistries.BLOCK,
      id("puesto_intercambio"),
      new ModIntercambio.PuestoBlock(
         Properties.of()
            .mapColor(MapColor.NONE)
            .strength(-1.0F, 3600000.0F)
            .noLootTable()
            .pushReaction(PushReaction.BLOCK)
            .noOcclusion()
            .isViewBlocking((estado, mundo, pos) -> false)
            .isSuffocating((estado, mundo, pos) -> false)
            .sound(SoundType.STONE)
      )
   );
   public static final Item PUESTO_ITEM = (Item)Registry.register(
      BuiltInRegistries.ITEM, id("puesto_intercambio"), new BlockItem(PUESTO, new net.minecraft.world.item.Item.Properties().rarity(Rarity.EPIC))
   );

   private ModIntercambio() {
   }

   private static ResourceLocation id(String nombre) {
      return ResourceLocation.fromNamespaceAndPath("soulspoon", nombre);
   }

   public static void initialize() {
   }

   private static MerchantOffers tratos() {
      MerchantOffers o = new MerchantOffers();
      o.add(trato(new ItemCost(Coins.azul(), 1), new ItemStack(Ids.item("dedita_casino"))));
      o.add(trato(new ItemCost(Ids.item("dilitio"), 2), new ItemStack(Coins.azul())));
      o.add(trato(new ItemCost(Coins.azul(), 1), new ItemStack(Ids.item("manuscrito_hermandad"))));
      return o;
   }

   private static MerchantOffer trato(ItemCost cuesta, ItemStack da) {
      return new MerchantOffer(cuesta, da, Integer.MAX_VALUE, 0, 0.0F);
   }

   static final class Puesto implements Merchant {
      private final MerchantOffers ofertas = ModIntercambio.tratos();
      private Player jugador;

      Puesto(Player jugador) {
         this.jugador = jugador;
      }

      public void setTradingPlayer(Player p) {
         this.jugador = p;
      }

      public Player getTradingPlayer() {
         return this.jugador;
      }

      public MerchantOffers getOffers() {
         return this.ofertas;
      }

      public void overrideOffers(MerchantOffers o) {
      }

      public void notifyTrade(MerchantOffer o) {
         o.resetUses();
      }

      public void notifyTradeUpdated(ItemStack pila) {
      }

      public int getVillagerXp() {
         return 0;
      }

      public void overrideXp(int xp) {
      }

      public boolean showProgressBar() {
         return false;
      }

      public SoundEvent getNotifyTradeSound() {
         return SoundEvents.VILLAGER_YES;
      }

      public boolean isClientSide() {
         return false;
      }
   }

   static final class PuestoBlock extends Block {
      static final MapCodec<ModIntercambio.PuestoBlock> CODEC = simpleCodec(ModIntercambio.PuestoBlock::new);

      PuestoBlock(Properties propiedades) {
         super(propiedades);
      }

      protected MapCodec<? extends Block> codec() {
         return CODEC;
      }

      protected RenderShape getRenderShape(BlockState estado) {
         return RenderShape.INVISIBLE;
      }

      protected boolean propagatesSkylightDown(BlockState estado, BlockGetter mundo, BlockPos pos) {
         return true;
      }

      protected float getShadeBrightness(BlockState estado, BlockGetter mundo, BlockPos pos) {
         return 1.0F;
      }

      protected InteractionResult useWithoutItem(BlockState estado, Level mundo, BlockPos pos, Player jugador, BlockHitResult golpe) {
         if (!mundo.isClientSide) {
            new ModIntercambio.Puesto(jugador).openTradingScreen(jugador, Component.literal("Puesto de Intercambio"), 0);
         }

         return InteractionResult.sidedSuccess(mundo.isClientSide);
      }
   }
}
