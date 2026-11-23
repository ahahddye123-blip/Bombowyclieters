package dev.bombowy.client.mixin;

import dev.bombowy.client.BombowyConfig;
import dev.bombowy.client.Modules;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true, require = 0)
    private void bombowy$noCameraShake(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        if (Modules.isOn("cleanview") && BombowyConfig.get().cleanNoCameraShake) {
            ci.cancel();
        }
    }
}
