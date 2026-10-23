package neth6.soulspoon.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Set;
import neth6.soulspoon.candados.Candados;
import neth6.soulspoon.client.cofres.MallaCofre;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChestRenderer.class)
public class ChestRendererMixin {
    @Unique private static final MallaCofre CHICO = new MallaCofre("cofre_chico.json", Set.of(1), 2);
    @Unique private static final MallaCofre OSCURO = new MallaCofre("cofre_oscuro.json", Set.of(1), 2);

    @Inject(
        method = "render(Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
        at = @At("HEAD"), cancellable = true)
    private void soulspoon$conCandado(BlockEntity entidad, float parcial, PoseStack pose, MultiBufferSource buffers, int luz, int overlay, CallbackInfo ci) {
        Level level = entidad.getLevel();
        if (level == null) return;
        BlockPos pos = entidad.getBlockPos();
        BlockState estado = entidad.getBlockState();
        if (estado.getBlock() instanceof ChestBlock && Candados.tieneCandado(level, pos)) {
            ci.cancel();
            boolean simple = estado.getValue(ChestBlock.TYPE) == ChestType.SINGLE;
            if (simple || Candados.principal(level, pos).equals(pos)) {
                Direction frente = estado.getValue(ChestBlock.FACING);
                float abierto = entidad instanceof LidBlockEntity tapa ? tapa.getOpenNess(parcial) : 0.0F;
                pose.pushPose();
                if (simple) {
                    pose.translate(0.5, 0.0, 0.5);
                } else {
                    Direction otra = ChestBlock.getConnectedDirection(estado);
                    pose.translate(0.5 + otra.getStepX() * 0.5, 0.0, 0.5 + otra.getStepZ() * 0.5);
                }
                pose.mulPose(Axis.YP.rotationDegrees(-frente.toYRot()));
                (simple ? CHICO : OSCURO).dibujar(pose, buffers, luz, overlay, abierto);
                pose.popPose();
            }
        }
    }
}
