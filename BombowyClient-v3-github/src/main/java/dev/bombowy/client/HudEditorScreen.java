package dev.bombowy.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

/** Edytor HUD w stylu SS 4: pełna widoczność gry bez blura, przeciąganie, skalowanie za róg, [⚙] i [-]. */
public class HudEditorScreen extends Screen {
    private static final String[] KEYS = {
            BombowyHud.KEY_FPS,
            BombowyHud.KEY_SHIELD,
            BombowyHud.KEY_INVENTORY,
            BombowyHud.KEY_COOLDOWNS
    };

    private String draggingKey = null;
    private String resizingKey = null;
    private double dragOffsetX, dragOffsetY;
    private float startScale = 1.0f;
    private double resizeStartX, resizeStartY;
    private float anim = 0.0f;

    public HudEditorScreen() {
        super(Text.literal("Edytor HUD - BombowyClient"));
    }

    private void playClick() {
        if (this.client != null) {
            this.client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        }
    }

    @Override
    protected void init() {
        anim = 0.0f;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Brak zaciemnienia/blura — widać cały świat jak na SS 4
    }

    private BombowyConfig.HudElementConfig getHudConfig(String key) {
        return switch (key) {
            case BombowyHud.KEY_FPS -> BombowyConfig.hud(key, 8, 8, 1.0f);
            case BombowyHud.KEY_SHIELD -> BombowyConfig.hud(key, 8, 24, 1.0f);
            case BombowyHud.KEY_INVENTORY -> BombowyConfig.hud(key, 8, 42, 1.0f);
            default -> BombowyConfig.hud(key, 8, 108, 1.0f);
        };
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        anim = Math.min(1.0f, anim + delta * 0.18f);
        float ease = 1.0f - (1.0f - anim) * (1.0f - anim);

        MinecraftClient client = MinecraftClient.getInstance();

        if (draggingKey != null || resizingKey != null) {
            for (int gx = 0; gx < this.width; gx += 16) ctx.fill(gx, 0, gx + 1, this.height, 0x1EFFFFFF);
            for (int gy = 0; gy < this.height; gy += 16) ctx.fill(0, gy, this.width, gy + 1, 0x1EFFFFFF);
        }

        int barW = Math.min(this.width - 20, 460);
        int barX = (this.width - barW) / 2;
        int barY = Math.round(-20 + ease * 26);
        ctx.fill(barX, barY, barX + barW, barY + 18, 0xCC141619);
        ctx.drawBorder(barX, barY, barW, 18, 0xAA3A3E46);
        ctx.drawCenteredTextWithShadow(this.textRenderer,
                "Edytor HUD • Przeciągnij • Róg = rozmiar • [⚙] opcje • [-] wyłącz • ESC powrót",
                this.width / 2, barY + 5, 0xFFFFFFFF);

        for (String key : KEYS) {
            BombowyConfig.HudElementConfig cfg = getHudConfig(key);
            int w = BombowyHud.getElementWidth(key, cfg.scale);
            int h = BombowyHud.getElementHeight(key, cfg.scale);
            boolean active = key.equals(draggingKey) || key.equals(resizingKey);
            boolean hovered = mx >= cfg.x && mx <= cfg.x + w && my >= cfg.y && my <= cfg.y + h;

            if (cfg.enabled) {
                switch (key) {
                    case BombowyHud.KEY_FPS -> BombowyHud.drawFpsHud(ctx, client, cfg, 0);
                    case BombowyHud.KEY_SHIELD -> BombowyHud.drawShieldHud(ctx, client, cfg);
                    case BombowyHud.KEY_INVENTORY -> BombowyHud.drawInventoryHud(ctx, client, cfg);
                    default -> BombowyHud.drawCooldownsHud(ctx, client, cfg, delta, true);
                }
            } else {
                ctx.drawTextWithShadow(this.textRenderer, key + " (OFF)", cfg.x + 2, cfg.y + 2, 0xFFFF7777);
            }

            int borderCol = active ? 0xFF2EBD6B : (hovered ? 0xFFFFFFFF : 0x88AEB4BE);
            ctx.drawBorder(cfg.x - 1, cfg.y - 1, w + 2, h + 2, borderCol);

            // Przycisk "-"
            int minusX = cfg.x + w - 11;
            int minusY = cfg.y - 12;
            ctx.fill(minusX, minusY, minusX + 11, minusY + 11, 0xCCAA2828);
            ctx.drawCenteredTextWithShadow(this.textRenderer, "-", minusX + 6, minusY + 2, 0xFFFFFFFF);

            // Przycisk "⚙"
            int gearX = cfg.x + w - 24;
            int gearY = cfg.y - 12;
            ctx.fill(gearX, gearY, gearX + 11, gearY + 11, 0xCC22262E);
            ctx.drawCenteredTextWithShadow(this.textRenderer, "⚙", gearX + 6, gearY + 2, 0xFFFFFFFF);

            // Róg do zmiany rozmiaru
            int rzX = cfg.x + w - 6;
            int rzY = cfg.y + h - 6;
            ctx.fill(rzX, rzY, rzX + 6, rzY + 6, 0xFF2EBD6B);
        }

        super.render(ctx, mx, my, delta);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            for (String key : KEYS) {
                BombowyConfig.HudElementConfig cfg = getHudConfig(key);
                int w = BombowyHud.getElementWidth(key, cfg.scale);
                int h = BombowyHud.getElementHeight(key, cfg.scale);

                int minusX = cfg.x + w - 11;
                int minusY = cfg.y - 12;
                if (mx >= minusX && mx <= minusX + 11 && my >= minusY && my <= minusY + 11) {
                    playClick();
                    cfg.enabled = !cfg.enabled;
                    BombowyConfig.save();
                    return true;
                }

                int gearX = cfg.x + w - 24;
                int gearY = cfg.y - 12;
                if (mx >= gearX && mx <= gearX + 11 && my >= gearY && my <= gearY + 11) {
                    playClick();
                    String modId = switch (key) {
                        case BombowyHud.KEY_INVENTORY -> "inventoryhud";
                        case BombowyHud.KEY_COOLDOWNS -> "cooldowns";
                        case BombowyHud.KEY_FPS -> "fpshud";
                        default -> "shieldstatus";
                    };
                    Modules.Module m = Modules.get(modId);
                    if (m != null && this.client != null) {
                        this.client.setScreen(new ModuleSettingsScreen(m, this));
                        return true;
                    }
                }

                int rzX = cfg.x + w - 6;
                int rzY = cfg.y + h - 6;
                if (mx >= rzX && mx <= rzX + 6 && my >= rzY && my <= rzY + 6) {
                    playClick();
                    resizingKey = key;
                    resizeStartX = mx;
                    resizeStartY = my;
                    startScale = cfg.scale;
                    return true;
                }

                if (mx >= cfg.x && mx <= cfg.x + w && my >= cfg.y && my <= cfg.y + h) {
                    playClick();
                    draggingKey = key;
                    dragOffsetX = mx - cfg.x;
                    dragOffsetY = my - cfg.y;
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double deltaX, double deltaY) {
        if (draggingKey != null) {
            BombowyConfig.HudElementConfig cfg = getHudConfig(draggingKey);
            int rawX = MathHelper.clamp((int) (mx - dragOffsetX), 0, Math.max(0, this.width - 20));
            int rawY = MathHelper.clamp((int) (my - dragOffsetY), 0, Math.max(0, this.height - 20));
            cfg.x = Math.round(rawX / 4.0f) * 4;
            cfg.y = Math.round(rawY / 4.0f) * 4;
            return true;
        }
        if (resizingKey != null) {
            BombowyConfig.HudElementConfig cfg = getHudConfig(resizingKey);
            double diff = (mx - resizeStartX) + (my - resizeStartY);
            float next = MathHelper.clamp(startScale + (float) (diff / 120.0), 0.5f, 2.5f);
            cfg.scale = Math.round(next * 20f) / 20f;
            return true;
        }
        return super.mouseDragged(mx, my, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (draggingKey != null || resizingKey != null) {
            draggingKey = null;
            resizingKey = null;
            BombowyConfig.save();
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            BombowyConfig.save();
            if (this.client != null) this.client.setScreen(new MenuScreen());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
