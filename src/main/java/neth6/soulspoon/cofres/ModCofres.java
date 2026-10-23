package neth6.soulspoon.cofres;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;

public final class ModCofres {
   public static final Block COFRE = (Block)Registry.register(
      BuiltInRegistries.BLOCK,
      id("cofre_protegido"),
      new CofreBlock(
         Properties.of()
            .mapColor(MapColor.COLOR_BLACK)
            .strength(4.0F, 1200.0F)
            .requiresCorrectToolForDrops()
            .sound(SoundType.NETHERITE_BLOCK)
            .lightLevel(estado -> 4)
            .noOcclusion()
      )
   );
   public static final Item COFRE_ITEM = (Item)Registry.register(
      BuiltInRegistries.ITEM, id("cofre_protegido"), new BlockItem(COFRE, new net.minecraft.world.item.Item.Properties().rarity(Rarity.EPIC))
   );
   public static final BlockEntityType<CofreBlockEntity> COFRE_ENTIDAD = (BlockEntityType<CofreBlockEntity>)Registry.register(
      BuiltInRegistries.BLOCK_ENTITY_TYPE, id("cofre_protegido"), BlockEntityType.Builder.of(CofreBlockEntity::new, COFRE).build(null)
   );
   public static final Block COFRE_HUESOS = (Block)Registry.register(
      BuiltInRegistries.BLOCK,
      id("cofre_huesos"),
      new CofreHuesosBlock(Properties.of().mapColor(MapColor.SAND).strength(2.5F).sound(SoundType.BONE_BLOCK).noOcclusion())
   );
   public static final Item COFRE_HUESOS_ITEM = (Item)Registry.register(
      BuiltInRegistries.ITEM, id("cofre_huesos"), new BlockItem(COFRE_HUESOS, new net.minecraft.world.item.Item.Properties().rarity(Rarity.RARE))
   );
   public static final BlockEntityType<CofreBlockEntity> COFRE_HUESOS_ENTIDAD = (BlockEntityType<CofreBlockEntity>)Registry.register(
      BuiltInRegistries.BLOCK_ENTITY_TYPE,
      id("cofre_huesos"),
      BlockEntityType.Builder.of((pos, estado) -> new CofreBlockEntity(ModCofres.COFRE_HUESOS_ENTIDAD, pos, estado), COFRE_HUESOS).build(null)
   );
   public static final SoundEvent SONIDO_ABRIR = sonido("cofre_abrir");
   public static final SoundEvent SONIDO_CERRAR = sonido("cofre_cerrar");

   private ModCofres() {
   }

   private static ResourceLocation id(String nombre) {
      return ResourceLocation.fromNamespaceAndPath("soulspoon", nombre);
   }

   // Los sonidos ya estan en sounds.json: solo se arma el evento (no se registra de nuevo).
   private static SoundEvent sonido(String nombre) {
      return SoundEvent.createVariableRangeEvent(id(nombre));
   }

   public static void initialize() {
      net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents.modifyEntriesEvent(net.minecraft.world.item.CreativeModeTabs.FUNCTIONAL_BLOCKS).register(e -> {
         e.accept(COFRE_ITEM);
         e.accept(COFRE_HUESOS_ITEM);
      });
   }
}
