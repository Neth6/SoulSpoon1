package neth6.soulspoon.mob;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

// Huevo de aparicion de una variante: clic derecho en un bloque para invocarla.
public class VariantEggItem extends Item {
    private final String variantId;

    public VariantEggItem(String variantId, Properties properties) {
        super(properties);
        this.variantId = variantId;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        Variant variant = Variants.byId(variantId);
        if (variant == null) {
            return InteractionResult.FAIL;
        }

        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        // Los dromoraptores salen en manada.
        int amount = variantId.startsWith("dromoraptor") ? 3 : 1;
        for (int i = 0; i < amount; i++) {
            VariantManager.spawn(serverLevel, pos, variant, true);
        }
        serverLevel.playSound(null, pos, SoundEvents.EGG_THROW, SoundSource.NEUTRAL, 1.0f, 0.8f);

        Player player = context.getPlayer();
        if (player == null || !player.isCreative()) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }
}
