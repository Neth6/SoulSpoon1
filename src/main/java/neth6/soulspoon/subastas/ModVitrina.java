package neth6.soulspoon.subastas;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.PushReaction;

public final class ModVitrina {
   public static final Block VITRINA = (Block)Registry.register(
      BuiltInRegistries.BLOCK,
      id("vitrina"),
      new VitrinaBlock(
         Properties.of().strength(-1.0F, 3600000.0F).noLootTable().noOcclusion().noCollission().sound(SoundType.GLASS).pushReaction(PushReaction.BLOCK)
      )
   );
   public static final Item VITRINA_ITEM = (Item)Registry.register(
      BuiltInRegistries.ITEM, id("vitrina"), new BlockItem(VITRINA, new net.minecraft.world.item.Item.Properties())
   );
   public static final BlockEntityType<VitrinaBlockEntity> ENTIDAD = (BlockEntityType<VitrinaBlockEntity>)Registry.register(
      BuiltInRegistries.BLOCK_ENTITY_TYPE, id("vitrina"), BlockEntityType.Builder.of(VitrinaBlockEntity::new, VITRINA).build(null)
   );

   private ModVitrina() {
   }

   private static ResourceLocation id(String nombre) {
      return ResourceLocation.fromNamespaceAndPath("soulspoon", nombre);
   }

   public static void initialize() {
   }
}
