package dev.bombowy.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Renderowanie HUD: przezroczyste Cooldowny bez obramówki, Ekwipunek, FPS/CPS, Status tarczy i Celownik. */
public final class BombowyHud {
    private BombowyHud() {}

    public static final String KEY_INVENTORY = "inventory";
    public static final String KEY_COOLDOWNS = "cooldowns";
    public static final String KEY_FPS = "fpshud";
    public static final String KEY_SHIELD = "shieldhud";

    private static final Deque<Long> leftClicks = new ArrayDeque<>();
    private static boolean wasAttackPressed = false;

    public static void register() {
        HudRenderCallback.EVENT.register(BombowyHud::onHudRender);
    }

    private static int updateAndGetCps(MinecraftClient client) {
        long now = System.currentTimeMillis();
        boolean pressed = client.options.attackKey.isPressed();
        if (pressed && !wasAttackPressed) {
            leftClicks.addLast(now);
        }
        wasAttackPressed = pressed;
        while (!leftClicks.isEmpty() && now - leftClicks.peekFirst() > 1000L) {
            leftClicks.removeFirst();
        }
        return leftClicks.size();
    }

    private static void onHudRender(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null || client.options.hudHidden) return;

        int cps = updateAndGetCps(client);

        // Boost kolorów
        if (Modules.isOn("colorboost")) {
            float s = MathHelper.clamp(BombowyConfig.get().colorBoostStrength, 0.0f, 1.5f);
            int alpha = MathHelper.clamp((int) (s * 22), 0, 44);
            if (alpha > 0) {
                int tint = (alpha << 24) | 0x00FFAA44;
                ctx.fill(0, 0, client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight(), tint);
            }
        }

        // Celownik PvP
        if (Modules.isOn("crosshair")) {
            drawCustomCrosshair(ctx, client);
        }

        // Ekwipunek na HUD
        if (Modules.isOn("inventoryhud")) {
            BombowyConfig.HudElementConfig invCfg = BombowyConfig.hud(KEY_INVENTORY, 8, 42, 1.0f);
            if (invCfg.enabled) {
                drawInventoryHud(ctx, client, invCfg);
            }
        }

        // Cooldowny — PRZEZROCZYSTE BEZ OBRAMÓWKI I TŁA
        if (Modules.isOn("cooldowns")) {
            BombowyConfig.HudElementConfig cdCfg = BombowyConfig.hud(KEY_COOLDOWNS, 8, 108, 1.0f);
            if (cdCfg.enabled) {
                drawCooldownsHud(ctx, client, cdCfg, tickCounter.getTickDelta(true), false);
            }
        }

        // FPS & CPS na HUD
        if (Modules.isOn("fpshud")) {
            BombowyConfig.HudElementConfig fpsCfg = BombowyConfig.hud(KEY_FPS, 8, 8, 1.0f);
            if (fpsCfg.enabled) {
                drawFpsHud(ctx, client, fpsCfg, cps);
            }
        }

        // Status tarczy & pancerza
        if (Modules.isOn("shieldstatus")) {
            BombowyConfig.HudElementConfig shCfg = BombowyConfig.hud(KEY_SHIELD, 8, 24, 1.0f);
            if (shCfg.enabled) {
                drawShieldHud(ctx, client, shCfg);
            }
        }
    }

    private static void drawCustomCrosshair(DrawContext ctx, MinecraftClient client) {
        BombowyConfig cfg = BombowyConfig.get();
        int cx = client.getWindow().getScaledWidth() / 2;
        int cy = client.getWindow().getScaledHeight() / 2;
        int sz = MathHelper.clamp(cfg.crosshairSize, 2, 14);
        int gap = MathHelper.clamp(cfg.crosshairGap, 0, 8);
        int col = cfg.crosshairColor;

        ctx.fill(cx - gap - sz, cy, cx - gap, cy + 1, col);
        ctx.fill(cx + gap + 1, cy, cx + gap + 1 + sz, cy + 1, col);
        ctx.fill(cx, cy - gap - sz, cx + 1, cy - gap, col);
        ctx.fill(cx, cy + gap + 1, cx + 1, cy + gap + 1 + sz, col);
        if (cfg.crosshairDot) {
            ctx.fill(cx, cy, cx + 1, cy + 1, col);
        }
    }

    public static int getElementWidth(String key, float scale) {
        return switch (key) {
            case KEY_INVENTORY -> Math.round(9 * 18 * scale);
            case KEY_COOLDOWNS -> Math.round(96 * scale);
            case KEY_FPS -> Math.round(108 * scale);
            default -> Math.round(96 * scale);
        };
    }

    public static int getElementHeight(String key, float scale) {
        return switch (key) {
            case KEY_INVENTORY -> Math.round(3 * 18 * scale);
            case KEY_COOLDOWNS -> Math.round(42 * scale);
            case KEY_FPS -> Math.round(14 * scale);
            default -> Math.round(18 * scale);
        };
    }

    public static void drawFpsHud(DrawContext ctx, MinecraftClient client, BombowyConfig.HudElementConfig cfg, int cps) {
        float sc = MathHelper.clamp(cfg.scale, 0.5f, 2.5f);
        ctx.getMatrices().push();
        ctx.getMatrices().translate(cfg.x, cfg.y, 0);
        ctx.getMatrices().scale(sc, sc, 1f);
        String text = "[" + client.getCurrentFps() + " FPS  |  " + cps + " CPS]";
        ctx.drawTextWithShadow(client.textRenderer, text, 0, 2, 0xFFFFFFFF);
        ctx.getMatrices().pop();
    }

    public static void drawShieldHud(DrawContext ctx, MinecraftClient client, BombowyConfig.HudElementConfig cfg) {
        if (client.player == null) return;
        float sc = MathHelper.clamp(cfg.scale, 0.5f, 2.5f);
        ctx.getMatrices().push();
        ctx.getMatrices().translate(cfg.x, cfg.y, 0);
        ctx.getMatrices().scale(sc, sc, 1f);

        ItemStack offhand = client.player.getOffHandStack();
        ItemStack main = client.player.getMainHandStack();
        boolean hasShield = offhand.isOf(Items.SHIELD) || main.isOf(Items.SHIELD);
        ctx.drawItem(new ItemStack(Items.SHIELD), 0, 0);
        String st = hasShield ? "Tarcza: GOTOWA" : "Tarcza: BRAK";
        int col = hasShield ? 0xFF55FF88 : 0xFFBBBBCC;
        ctx.drawTextWithShadow(client.textRenderer, st, 20, 4, col);

        ctx.getMatrices().pop();
    }

    public static void drawInventoryHud(DrawContext ctx, MinecraftClient client, BombowyConfig.HudElementConfig cfg) {
        if (client.player == null) return;
        float sc = MathHelper.clamp(cfg.scale, 0.5f, 2.5f);
        int w = 9 * 18;
        int h = 3 * 18;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(cfg.x, cfg.y, 0);
        ctx.getMatrices().scale(sc, sc, 1f);

        ctx.fill(-2, -2, w + 2, h + 2, 0x55101015);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = 9 + row * 9 + col;
                ItemStack stack = client.player.getInventory().getStack(slotIndex);
                int sx = col * 18;
                int sy = row * 18;
                ctx.fill(sx, sy, sx + 16, sy + 16, 0x3322222A);
                if (!stack.isEmpty()) {
                    ctx.drawItem(stack, sx, sy);
                    ctx.drawStackOverlay(client.textRenderer, stack, sx, sy);
                }
            }
        }

        ctx.getMatrices().pop();
    }

    private record ActiveCooldown(Item item, float remainingSec) {}

    /** Cooldowny całkowicie przezroczyste, bez obramówki i bez ciemnego boxa. */
    public static void drawCooldownsHud(DrawContext ctx, MinecraftClient client, BombowyConfig.HudElementConfig cfg, float tickDelta, boolean editorPreview) {
        if (client.player == null) return;
        float sc = MathHelper.clamp(cfg.scale, 0.5f, 2.5f);
        TextRenderer tr = client.textRenderer;
        ItemCooldownManager manager = client.player.getItemCooldownManager();

        List<ActiveCooldown> list = new ArrayList<>();
        Item[] watched = new Item[]{Items.ENDER_PEARL, Items.SHIELD, Items.CHORUS_FRUIT, Items.WIND_CHARGE};
        for (Item item : watched) {
            float progress = manager.getCooldownProgress(new ItemStack(item), tickDelta);
            if (progress > 0f) {
                float approxSec = progress * (item == Items.ENDER_PEARL ? 15.0f : 5.0f);
                list.add(new ActiveCooldown(item, Math.max(0.1f, approxSec)));
            }
        }
        for (Identifier id : Registries.ITEM.getIds()) {
            Item item = Registries.ITEM.get(id);
            boolean already = false;
            for (ActiveCooldown ac : list) if (ac.item == item) already = true;
            if (already) continue;
            float p = manager.getCooldownProgress(new ItemStack(item), tickDelta);
            if (p > 0f) {
                list.add(new ActiveCooldown(item, Math.max(0.1f, p * 5.0f)));
                if (list.size() >= 5) break;
            }
        }

        if (list.isEmpty() && !editorPreview) {
            return;
        }

        ctx.getMatrices().push();
        ctx.getMatrices().translate(cfg.x, cfg.y, 0);
        ctx.getMatrices().scale(sc, sc, 1f);

        if (list.isEmpty()) {
            ctx.drawItem(new ItemStack(Items.ENDER_PEARL), 0, 0);
            ctx.drawTextWithShadow(tr, "Perła: 0.0s", 20, 4, 0xFFFFFFFF);
            ctx.drawItem(new ItemStack(Items.SHIELD), 0, 18);
            ctx.drawTextWithShadow(tr, "Tarcza: 0.0s", 20, 22, 0xFFFFFFFF);
        } else {
            for (int i = 0; i < list.size(); i++) {
                ActiveCooldown ac = list.get(i);
                int iy = i * 18;
                ctx.drawItem(new ItemStack(ac.item), 0, iy);
                ctx.drawTextWithShadow(tr, String.format("%.1fs", ac.remainingSec), 20, iy + 4, 0xFFFFFFFF);
            }
        }

        ctx.getMatrices().pop();
    }
}
