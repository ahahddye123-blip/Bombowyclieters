package dev.bombowy.client.mixin;

import dev.bombowy.client.BombowyConfig;
import dev.bombowy.client.Modules;
import net.minecraft.block.BlockState;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {
    @Inject(method = "addBlockBreakParticles", at = @At("HEAD"), cancellable = true, require = 0)
    private void bombowy$noBreakParticles(BlockPos pos, BlockState state, CallbackInfo ci) {
        if (Modules.isOn("noblockparticles") && BombowyConfig.get().disableBlockParticles) {
            ci.cancel();
        }
    }

    @Inject(method = "addBlockBreakingParticles", at = @At("HEAD"), cancellable = true, require = 0)
    private void bombowy$noBreakingParticles(BlockPos pos, Direction direction, CallbackInfo ci) {
        if (Modules.isOn("noblockparticles") && BombowyConfig.get().disableBlockParticles) {
            ci.cancel();
        }
    }
}
