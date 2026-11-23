package dev.bombowy.client;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.Optional;

public final class BombowyRender {
    private BombowyRender() {}

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(BombowyRender::onAfterEntities);
    }

    public static boolean hasBombBadge(String playerName) {
        BombowyConfig cfg = BombowyConfig.get();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && playerName.equalsIgnoreCase(client.player.getName().getString())) {
            return cfg.bombBadgeShowSelf;
        }
        return cfg.bombBadgeUsers != null && cfg.bombBadgeUsers.contains(playerName.toLowerCase());
    }

    /** Nick i ikona bomby renderowane na biało (Formatting.WHITE). */
    public static Text decorateName(Text original, String plainName) {
        MutableText whiteName = Text.literal(original.getString()).formatted(Formatting.WHITE);
        if (!hasBombBadge(plainName)) return whiteName;
        MutableText badge = Text.literal("💣 ").formatted(Formatting.WHITE);
        return badge.append(whiteName);
    }

    private static boolean isAimingAtBox(Vec3d eyePos, Vec3d lookDir, Box box, double maxDist) {
        Vec3d end = eyePos.add(lookDir.multiply(maxDist));
        Optional<Vec3d> hit = box.raycast(eyePos, end);
        return hit.isPresent();
    }

    private static void onAfterEntities(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;

        boolean hitboxes = Modules.isOn("hitboxes");
        boolean ownName = Modules.isOn("nametags") && !client.options.getPerspective().isFirstPerson();
        if (!hitboxes && !ownName) return;

        MatrixStack matrices = context.matrixStack();
        if (matrices == null) return;
        Camera camera = context.camera();
        Vec3d cam = camera.getPos();
        float td = client.getRenderTickCounter().getTickDelta(false);
        VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();
        BombowyConfig cfg = BombowyConfig.get();

        if (hitboxes) {
            VertexConsumer lines = immediate.getBuffer(RenderLayer.getLines());
            Vec3d eye = client.player.getCameraPosVec(td);
            Vec3d look = client.player.getRotationVec(td).normalize();
            double reach = Math.max(1.0, cfg.hitboxReach);

            matrices.push();
            matrices.translate(-cam.x, -cam.y, -cam.z);
            for (AbstractClientPlayerEntity p : client.world.getPlayers()) {
                if (p == client.player || p.isSpectator()) continue;
                double ix = MathHelper.lerp(td, p.prevX, p.getX());
                double iy = MathHelper.lerp(td, p.prevY, p.getY());
                double iz = MathHelper.lerp(td, p.prevZ, p.getZ());
                Box box = p.getBoundingBox().offset(ix - p.getX(), iy - p.getY(), iz - p.getZ());

                boolean aiming = isAimingAtBox(eye, look, box, reach);
                int col = aiming ? cfg.hitboxColorAim : cfg.hitboxColorSide;
                float r = ((col >> 16) & 0xFF) / 255f;
                float g = ((col >> 8) & 0xFF) / 255f;
                float b = (col & 0xFF) / 255f;
                float alpha = (!cfg.hitboxAimOnly || aiming) ? 1.0f : 0.85f;
                WorldRenderer.drawBox(matrices, lines, box, r, g, b, alpha);
            }
            matrices.pop();
            immediate.draw(RenderLayer.getLines());
        }

        if (ownName) {
            Text name = decorateName(client.player.getDisplayName(), client.player.getName().getString());
            Vec3d pos = client.player.getLerpedPos(td);
            TextRenderer tr = client.textRenderer;

            matrices.push();
            matrices.translate(pos.x - cam.x, pos.y + client.player.getHeight() + 0.5 - cam.y, pos.z - cam.z);
            matrices.multiply(camera.getRotation());
            matrices.scale(0.025f, -0.025f, 0.025f);
            Matrix4f matrix = matrices.peek().getPositionMatrix();
            float x = -tr.getWidth(name) / 2.0f;
            tr.draw(name, x, 0.0f, 0xFFFFFFFF, false, matrix, immediate,
                    TextRenderer.TextLayerType.NORMAL, 0, 0xF000F0);
            immediate.draw();
            matrices.pop();
        }
    }
}
