package dev.bombowy.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/** Konfigurator w stylu SS 3 (OneConfig): lewy pasek nawigacji, wiersze ustawień z przełącznikami suwakowymi po prawej. */
public class ModuleSettingsScreen extends Screen {
    private static final int SIDE = 132;

    private final Modules.Module module;
    private final Screen parent;
    private TextFieldWidget colorAimField;
    private TextFieldWidget colorSideField;
    private TextFieldWidget badgeUserField;
    private float anim = 0.0f;
    private int px, py, pw, ph;

    private static class SettingRow {
        final String title;
        final String subtitle;
        final BooleanSupplier getter;
        final Runnable toggler;
        final String valueLabel;

        SettingRow(String title, String subtitle, BooleanSupplier getter, Runnable toggler, String valueLabel) {
            this.title = title;
            this.subtitle = subtitle;
            this.getter = getter;
            this.toggler = toggler;
            this.valueLabel = valueLabel;
        }
    }

    private final List<SettingRow> rows = new ArrayList<>();

    public ModuleSettingsScreen(Modules.Module module, Screen parent) {
        super(Text.literal("Konfiguracja: " + module.name));
        this.module = module;
        this.parent = parent;
    }

    private void playClick() {
        if (this.client != null) {
            this.client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        }
    }

    private void rebuildRows() {
        rows.clear();
        BombowyConfig cfg = BombowyConfig.get();

        if (!module.alwaysOn) {
            rows.add(new SettingRow(
                    "Aktywność modułu",
                    "Włącza lub wyłącza działanie modułu " + module.name + ".",
                    module::isOn,
                    module::toggle,
                    null
            ));
        }

        switch (module.id) {
            case "hitboxes" -> {
                rows.add(new SettingRow(
                        "Podświetlanie przy celowaniu",
                        "Zmienia kolor ramki na kolor celowania, gdy patrzysz w gracza.",
                        () -> cfg.hitboxAimOnly,
                        () -> { cfg.hitboxAimOnly = !cfg.hitboxAimOnly; BombowyConfig.save(); },
                        null
                ));
            }
            case "crosshair" -> {
                rows.add(new SettingRow(
                        "Kropka na środku",
                        "Wyświetla centralny punkt w celowniku.",
                        () -> cfg.crosshairDot,
                        () -> { cfg.crosshairDot = !cfg.crosshairDot; BombowyConfig.save(); },
                        null
                ));
                rows.add(new SettingRow(
                        "Rozmiar celownika",
                        "Kliknij, aby zmienić długość ramion celownika.",
                        () -> true,
                        () -> { cfg.crosshairSize = cfg.crosshairSize >= 10 ? 3 : cfg.crosshairSize + 2; BombowyConfig.save(); },
                        cfg.crosshairSize + " px"
                ));
            }
            case "cleanview" -> {
                rows.add(new SettingRow("Niski ogień", "Obniża płomienie na ekranie podczas podpalenia.",
                        () -> cfg.cleanLowFire, () -> { cfg.cleanLowFire = !cfg.cleanLowFire; BombowyConfig.save(); }, null));
                rows.add(new SettingRow("Bez winiety", "Usuwa przyciemnione rogi ekranu.",
                        () -> cfg.cleanNoVignette, () -> { cfg.cleanNoVignette = !cfg.cleanNoVignette; BombowyConfig.save(); }, null));
                rows.add(new SettingRow("Bez trzęsienia kamery", "Wyłącza kołysanie kamery przy chodzeniu i obrażeniach.",
                        () -> cfg.cleanNoCameraShake, () -> { cfg.cleanNoCameraShake = !cfg.cleanNoCameraShake; BombowyConfig.save(); }, null));
            }
            case "noblockparticles" -> {
                rows.add(new SettingRow("Blokuj okruchy bloków", "Usuwa cząsteczki przy kopaniu bloków.",
                        () -> cfg.disableBlockParticles, () -> { cfg.disableBlockParticles = !cfg.disableBlockParticles; BombowyConfig.save(); }, null));
            }
            case "colorboost" -> {
                rows.add(new SettingRow("Intensywność kolorów", "Kliknij, aby przełączyć nasycenie obrazu.",
                        () -> true,
                        () -> { cfg.colorBoostStrength = cfg.colorBoostStrength >= 1.0f ? 0.20f : MathHelper.clamp(cfg.colorBoostStrength + 0.25f, 0.0f, 1.5f); BombowyConfig.save(); },
                        Math.round(cfg.colorBoostStrength * 100f) + "%"));
            }
            case "scoreboard" -> {
                rows.add(new SettingRow("Przezroczysty scoreboard", "Czytelny tekst bez ciężkiego ciemnego tła.",
                        () -> cfg.scoreboardTransparent, () -> { cfg.scoreboardTransparent = !cfg.scoreboardTransparent; BombowyConfig.save(); }, null));
                rows.add(new SettingRow("Ukryj czerwone liczby", "Chowa zbędną numerację punktów po prawej.",
                        () -> cfg.scoreboardHideNumbers, () -> { cfg.scoreboardHideNumbers = !cfg.scoreboardHideNumbers; BombowyConfig.save(); }, null));
            }
            case "optimization" -> {
                rows.add(new SettingRow("Tryb słabego PC (Potato Mode)", "Obniża obciążające detale dla maksymalnej płynności.",
                        () -> cfg.potatoMode, () -> { cfg.potatoMode = !cfg.potatoMode; Features.optimization(module.isOn()); BombowyConfig.save(); }, null));
            }
            case "bombBadge" -> {
                rows.add(new SettingRow("Ikona bomby przy własnym nicku", "Wyświetla białą ikonę bomby obok Twojego nicku.",
                        () -> cfg.bombBadgeShowSelf, () -> { cfg.bombBadgeShowSelf = !cfg.bombBadgeShowSelf; BombowyConfig.save(); }, null));
            }
            case "inventoryhud", "cooldowns", "fpshud", "shieldstatus" -> {
                String key = switch (module.id) {
                    case "inventoryhud" -> BombowyHud.KEY_INVENTORY;
                    case "cooldowns" -> BombowyHud.KEY_COOLDOWNS;
                    case "fpshud" -> BombowyHud.KEY_FPS;
                    default -> BombowyHud.KEY_SHIELD;
                };
                BombowyConfig.HudElementConfig hCfg = BombowyConfig.hud(key, 8, 40, 1.0f);
                rows.add(new SettingRow("Skala elementu HUD", "Kliknij, aby dostosować wielkość elementu na ekranie.",
                        () -> true,
                        () -> { hCfg.scale = hCfg.scale >= 2.0f ? 0.7f : Math.round((hCfg.scale + 0.3f) * 10f) / 10f; BombowyConfig.save(); },
                        String.format("%.1fx", hCfg.scale)));
            }
        }
    }

    @Override
    protected void init() {
        anim = 0.0f;
        pw = Math.min(this.width - 16, 680);
        ph = Math.min(this.height - 16, 384);
        px = (this.width - pw) / 2;
        py = (this.height - ph) / 2;
        rebuildRows();

        BombowyConfig cfg = BombowyConfig.get();
        int contentX = px + SIDE + 14;
        int contentW = pw - SIDE - 28;
        int extraY = py + 54 + rows.size() * 44;

        if ("hitboxes".equals(module.id)) {
            colorAimField = new TextFieldWidget(this.textRenderer, contentX + 10, extraY + 18, Math.min(180, contentW - 60), 17, Text.literal("Kolor celowania"));
            colorAimField.setText(String.format("#%06X", cfg.hitboxColorAim & 0xFFFFFF));
            this.addSelectableChild(colorAimField);

            colorSideField = new TextFieldWidget(this.textRenderer, contentX + 10, extraY + 56, Math.min(180, contentW - 60), 17, Text.literal("Kolor z boku"));
            colorSideField.setText(String.format("#%06X", cfg.hitboxColorSide & 0xFFFFFF));
            this.addSelectableChild(colorSideField);
        } else if ("bombBadge".equals(module.id)) {
            badgeUserField = new TextFieldWidget(this.textRenderer, contentX + 10, extraY + 18, Math.min(190, contentW - 90), 17, Text.literal("Nick gracza"));
            badgeUserField.setPlaceholder(Text.literal("Dodaj nick z ikoną bomby..."));
            this.addSelectableChild(badgeUserField);
        }
    }

    private int parseHexColor(String text, int fallback) {
        if (text == null) return fallback;
        String clean = text.trim().replace("#", "").replace("0x", "");
        try {
            int rgb = Integer.parseInt(clean, 16) & 0xFFFFFF;
            return 0xFF000000 | rgb;
        } catch (Exception e) {
            return fallback;
        }
    }

    private void saveAndBack() {
        BombowyConfig cfg = BombowyConfig.get();
        if (colorAimField != null) cfg.hitboxColorAim = parseHexColor(colorAimField.getText(), cfg.hitboxColorAim);
        if (colorSideField != null) cfg.hitboxColorSide = parseHexColor(colorSideField.getText(), cfg.hitboxColorSide);
        BombowyConfig.save();
        if (this.client != null) this.client.setScreen(parent);
    }

    private static void rrect(DrawContext c, int x, int y, int w, int h, int color) {
        c.fill(x + 1, y, x + w - 1, y + h, color);
        c.fill(x, y + 1, x + w, y + h - 1, color);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        anim = Math.min(1.0f, anim + delta * 0.18f);
        float ease = 1.0f - (1.0f - anim) * (1.0f - anim);
        int offsetY = Math.round((1.0f - ease) * 10f);

        ctx.fill(0, 0, this.width, this.height, 0xB8000000);
        ctx.getMatrices().push();
        ctx.getMatrices().translate(0, offsetY, 0);

        // Ramka okna 1:1 jak OneConfig / 22MOD
        rrect(ctx, px - 1, py - 1, pw + 2, ph + 2, 0xFF282B30);
        rrect(ctx, px, py, pw, ph, 0xFF141619);
        ctx.fill(px + 1, py + 1, px + SIDE, py + ph - 1, 0xFF181A1E);
        ctx.fill(px + SIDE, py + 1, px + SIDE + 1, py + ph - 1, 0xFF24272C);

        // Logo BOMBOWY
        ctx.fill(px + 13, py + 17, px + 24, py + 28, 0xFFFFFFFF);
        ctx.fill(px + 15, yTop(py + 15), px + 22, py + 30, 0xFFFFFFFF);
        ctx.drawText(this.textRenderer, Text.literal("BOMBOWY").styled(s -> s.withBold(true)), px + 29, py + 18, 0xFFFFFFFF, false);

        // Lewy pasek kategorii konfiguracyjnych (1:1 styl SS 3)
        rrect(ctx, px + 8, py + 44, SIDE - 16, 20, 0xFF2E6BFF);
        ctx.drawText(this.textRenderer, "⚙  Opcje modułu", px + 14, py + 50, 0xFFFFFFFF, false);

        rrect(ctx, px + 8, py + 68, SIDE - 16, 20, 0xFF1E2126);
        ctx.drawText(this.textRenderer, "✥  Edytor HUD", px + 14, py + 74, 0xFFB0B5BF, false);

        // Przycisk Powrót na dole
        rrect(ctx, px + 8, py + ph - 28, SIDE - 16, 20, 0xFF23272F);
        ctx.drawCenteredTextWithShadow(this.textRenderer, "← Zapisz i wróć", px + SIDE / 2, py + ph - 22, 0xFFFFFFFF);

        // Nagłówek konfigurowanego modułu (1:1 SS 3)
        int contentX = px + SIDE + 14;
        int contentW = pw - SIDE - 28;
        ctx.drawText(this.textRenderer, Text.literal("←  " + module.name).styled(s -> s.withBold(true)), contentX, py + 15, 0xFFFFFFFF, false);
        ctx.drawText(this.textRenderer, module.desc, contentX, py + 29, 0xFF8B909B, false);

        // Wiersze ustawień (karty z przełącznikiem suwakowym po prawej)
        for (int i = 0; i < rows.size(); i++) {
            SettingRow row = rows.get(i);
            int ry = py + 50 + i * 44;
            boolean hov = mx >= contentX && mx <= contentX + contentW && my - offsetY >= ry && my - offsetY <= ry + 38;
            rrect(ctx, contentX, ry, contentW, 38, hov ? 0xFF23272E : 0xFF1C1F24);

            ctx.drawText(this.textRenderer, Text.literal(row.title).styled(s -> s.withBold(true)), contentX + 10, ry + 8, 0xFFFFFFFF, false);
            ctx.drawText(this.textRenderer, row.subtitle, contentX + 10, ry + 21, 0xFF8B909B, false);

            if (row.valueLabel != null) {
                int badgeW = Math.max(46, this.textRenderer.getWidth(row.valueLabel) + 14);
                int bx = contentX + contentW - badgeW - 10;
                rrect(ctx, bx, ry + 10, badgeW, 18, 0xFF2A2E37);
                ctx.drawCenteredTextWithShadow(this.textRenderer, row.valueLabel, bx + badgeW / 2, ry + 15, 0xFFFFFFFF);
            } else {
                boolean state = row.getter.getAsBoolean();
                int swX = contentX + contentW - 44;
                int swY = ry + 11;
                rrect(ctx, swX, swY, 32, 16, state ? 0xFF2E6BFF : 0xFF32363F);
                int knobX = state ? swX + 18 : swX + 2;
                rrect(ctx, knobX, swY + 2, 12, 12, 0xFFFFFFFF);
            }
        }

        // Dodatkowe pola HEX dla Hitboxów
        int extraY = py + 54 + rows.size() * 44;
        if (colorAimField != null && colorSideField != null) {
            rrect(ctx, contentX, extraY, contentW, 78, 0xFF1C1F24);
            ctx.drawText(this.textRenderer, "Kolor hitboxa przy celowaniu (HEX):", contentX + 10, extraY + 6, 0xFFCDD2DC, false);
            colorAimField.render(ctx, mx, my - offsetY, delta);
            int aimCol = parseHexColor(colorAimField.getText(), BombowyConfig.get().hitboxColorAim);
            rrect(ctx, colorAimField.getX() + colorAimField.getWidth() + 8, colorAimField.getY(), 24, 17, aimCol);

            ctx.drawText(this.textRenderer, "Kolor hitboxa z boku (HEX):", contentX + 10, extraY + 44, 0xFFCDD2DC, false);
            colorSideField.render(ctx, mx, my - offsetY, delta);
            int sideCol = parseHexColor(colorSideField.getText(), BombowyConfig.get().hitboxColorSide);
            rrect(ctx, colorSideField.getX() + colorSideField.getWidth() + 8, colorSideField.getY(), 24, 17, sideCol);
        }

        if (badgeUserField != null) {
            rrect(ctx, contentX, extraY, contentW, 44, 0xFF1C1F24);
            ctx.drawText(this.textRenderer, "Dodaj gracza z ikoną bomby przy białym nicku:", contentX + 10, extraY + 6, 0xFFCDD2DC, false);
            badgeUserField.render(ctx, mx, my - offsetY, delta);
            int addX = badgeUserField.getX() + badgeUserField.getWidth() + 8;
            rrect(ctx, addX, badgeUserField.getY(), 58, 17, 0xFF2EBD6B);
            ctx.drawCenteredTextWithShadow(this.textRenderer, "Dodaj", addX + 29, badgeUserField.getY() + 5, 0xFFFFFFFF);
        }

        ctx.getMatrices().pop();
    }

    private int yTop(int y) { return y; }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            // Lewy pasek: Edytor HUD
            if (mx >= px + 8 && mx <= px + SIDE - 8 && my >= py + 68 && my <= py + 88) {
                playClick();
                if (this.client != null) this.client.setScreen(new HudEditorScreen());
                return true;
            }
            // Powrót
            if (mx >= px + 8 && mx <= px + SIDE - 8 && my >= py + ph - 28 && my <= py + ph - 8) {
                playClick();
                saveAndBack();
                return true;
            }

            int contentX = px + SIDE + 14;
            int contentW = pw - SIDE - 28;
            for (int i = 0; i < rows.size(); i++) {
                int ry = py + 50 + i * 44;
                if (mx >= contentX && mx <= contentX + contentW && my >= ry && my <= ry + 38) {
                    playClick();
                    rows.get(i).toggler.run();
                    rebuildRows();
                    return true;
                }
            }

            if (badgeUserField != null) {
                int addX = badgeUserField.getX() + badgeUserField.getWidth() + 8;
                if (mx >= addX && mx <= addX + 58 && my >= badgeUserField.getY() && my <= badgeUserField.getY() + 17) {
                    playClick();
                    String nick = badgeUserField.getText().trim().toLowerCase();
                    if (!nick.isEmpty()) {
                        BombowyConfig.get().bombBadgeUsers.add(nick);
                        badgeUserField.setText("");
                        BombowyConfig.save();
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            saveAndBack();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
