package dev.bombowy.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** GUI 1:1 wzorowane na przesłanym zrzucie ekranu, dopasowane pod BombowyClient z płynnymi animacjami. */
public class MenuScreen extends Screen {
    private static final int SIDE = 132;
    private static final int HEAD = 50;
    private static final int PAD = 12;
    private static final int GAP = 8;
    private static final int CARD_H = 88;
    private static final int MIN_CARD = 122;

    private static int lastTab = 0;
    private static boolean filterFavorites = false;

    private TextFieldWidget search;
    private double scroll = 0;
    private int px, py, pw, ph;
    private float openAnim = 0.0f;
    private float tabAnim = 1.0f;

    public MenuScreen() {
        super(Text.literal("BombowyClient"));
    }

    private void playClick() {
        if (this.client != null) {
            this.client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        }
    }

    @Override
    protected void init() {
        openAnim = 0.0f;
        tabAnim = 1.0f;
        pw = Math.min(this.width - 16, 680);
        ph = Math.min(this.height - 16, 384);
        px = (this.width - pw) / 2;
        py = (this.height - ph) / 2;

        int sw = Math.max(85, Math.min(148, pw - SIDE - PAD * 2 - 190));
        search = new TextFieldWidget(this.textRenderer, px + pw - PAD - 46 - sw, py + 15, sw, 17, Text.literal("Szukaj"));
        search.setPlaceholder(Text.literal("Szukaj modułu..."));
        search.setChangedListener(s -> {
            scroll = 0;
            tabAnim = 0.25f;
        });
        this.addSelectableChild(search);
    }

    private int gridX() { return px + SIDE + PAD; }
    private int gridY() { return py + HEAD; }
    private int gridW() { return pw - SIDE - PAD * 2; }
    private int gridH() { return ph - HEAD - PAD; }
    private int cols() { return Math.max(2, Math.min(4, (gridW() + GAP) / (MIN_CARD + GAP))); }
    private int cardW() { int c = cols(); return (gridW() - GAP * (c - 1)) / c; }
    private int cardX(int i) { return gridX() + (i % cols()) * (cardW() + GAP); }
    private int cardY(int i) { return gridY() + (i / cols()) * (CARD_H + GAP) - (int) scroll; }

    private int maxScroll(int count) {
        int rows = (count + cols() - 1) / cols();
        return Math.max(0, rows * (CARD_H + GAP) - GAP - gridH());
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private List<Modules.Module> visible() {
        String q = search == null ? "" : search.getText().trim().toLowerCase();
        Modules.Tab currentTab = Modules.Tab.values()[MathHelper.clamp(lastTab, 0, Modules.Tab.values().length - 1)];
        List<Modules.Module> out = new ArrayList<>();
        for (Modules.Module m : Modules.ALL) {
            if (filterFavorites && !m.isFavorite()) continue;
            boolean tabOk;
            if (!q.isEmpty()) {
                tabOk = m.name.toLowerCase().contains(q) || m.desc.toLowerCase().contains(q);
            } else if (currentTab == Modules.Tab.ALL) {
                tabOk = true;
            } else if (currentTab == Modules.Tab.ENABLED) {
                tabOk = m.isOn();
            } else {
                tabOk = m.tab == currentTab;
            }
            if (tabOk) out.add(m);
        }
        return out;
    }

    private static int countIn(Modules.Tab tab) {
        if (tab == Modules.Tab.ALL) return Modules.ALL.size();
        if (tab == Modules.Tab.ENABLED) {
            int e = 0;
            for (Modules.Module m : Modules.ALL) if (m.isOn()) e++;
            return e;
        }
        int n = 0;
        for (Modules.Module m : Modules.ALL) if (m.tab == tab) n++;
        return n;
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
        openAnim = Math.min(1.0f, openAnim + delta * 0.17f);
        tabAnim = Math.min(1.0f, tabAnim + delta * 0.24f);

        float easedOpen = 1.0f - (1.0f - openAnim) * (1.0f - openAnim);
        float easedTab = 1.0f - (1.0f - tabAnim) * (1.0f - tabAnim);
        int animOffsetY = Math.round((1.0f - easedOpen) * 12.0f);
        int tabShift = Math.round((1.0f - easedTab) * 6.0f);

        TextRenderer tr = this.textRenderer;
        ctx.fill(0, 0, this.width, this.height, 0xB8000000);

        ctx.getMatrices().push();
        ctx.getMatrices().translate(0, animOffsetY, 0);

        // Główne okno 1:1
        rrect(ctx, px - 1, py - 1, pw + 2, ph + 2, 0xFF282B30);
        rrect(ctx, px, py, pw, ph, 0xFF141619);
        ctx.fill(px + 1, py + 1, px + SIDE, py + ph - 1, 0xFF181A1E);
        ctx.fill(px + SIDE, py + 1, px + SIDE + 1, py + ph - 1, 0xFF24272C);

        // Logo BOMBOWY z ikoną bomby na górze po lewej (1:1 jak 22MOD)
        drawBombLogo(ctx, px + 11, py + 12);

        // Kategorie po lewej z ikonami + licznikiem po prawej (1:1 jak na SS 1)
        Modules.Tab[] tabs = Modules.Tab.values();
        boolean searching = !search.getText().trim().isEmpty();
        for (int i = 0; i < tabs.length; i++) {
            int tx = px + 8, ty = py + 42 + i * 22, tw = SIDE - 16, th = 19;
            boolean sel = !searching && !filterFavorites && i == lastTab;
            boolean hov = inside(mx, my, tx, ty - animOffsetY, tw, th);
            if (sel) {
                rrect(ctx, tx, ty, tw, th, 0xFF25282E);
                ctx.fill(tx - 3, ty + 3, tx - 1, ty + th - 3, 0xFFFFFFFF);
            } else if (hov) {
                rrect(ctx, tx, ty, tw, th, 0xFF1E2025);
            }
            drawTabIcon(ctx, tabs[i], tx + 6, ty + 5, sel ? 0xFFFFFFFF : 0xFF9AA0AA);
            ctx.drawText(tr, tabs[i].label, tx + 19, ty + 6, sel ? 0xFFFFFFFF : 0xFFB0B5BF, false);
            String cnt = String.valueOf(countIn(tabs[i]));
            ctx.drawText(tr, cnt, tx + tw - 5 - tr.getWidth(cnt), ty + 6, 0xFF7D838E, false);
        }

        // Dolna sekcja lewego paska 1:1 (Konfiguracja, Ustawienia, Edytor HUD, stopka wersji)
        int bottomBase = py + ph - 82;
        drawSidebarFooterItem(ctx, tr, mx, my - animOffsetY, px + 8, bottomBase, SIDE - 16, "Konfiguracja", false);
        drawSidebarFooterItem(ctx, tr, mx, my - animOffsetY, px + 8, bottomBase + 20, SIDE - 16, "Ustawienia", false);
        drawSidebarFooterItem(ctx, tr, mx, my - animOffsetY, px + 8, bottomBase + 41, SIDE - 16, "Edytor HUD", true);

        ctx.drawText(tr, "v3.0  •  ` / RShift", px + 11, py + ph - 15, 0xFF6B707B, false);

        // Nagłówek główny po prawej (1:1 jak na SS 1)
        int hx = px + SIDE + PAD;
        ctx.drawText(tr, Text.literal("Moduły").styled(s -> s.withBold(true)), hx, py + 12, 0xFFFFFFFF, false);
        String sub = filterFavorites ? "Ulubione moduły BombowyClient"
                : (searching ? "Wyniki wyszukiwania" : "Wszystkie moduły BombowyClient");
        ctx.drawText(tr, sub, hx, py + 26, 0xFF8B909B, false);

        // Wyszukiwarka + przycisk ulubionych (serce) + przycisk układu po prawej u góry (1:1 SS 1)
        search.render(ctx, mx, my - animOffsetY, delta);
        int favBtnX = px + pw - PAD - 40;
        int favBtnY = py + 15;
        rrect(ctx, favBtnX, favBtnY, 17, 17, filterFavorites ? 0xFF3A222A : 0xFF202328);
        ctx.drawCenteredTextWithShadow(tr, filterFavorites ? "♥" : "♡", favBtnX + 9, favBtnY + 5,
                filterFavorites ? 0xFFFF5577 : 0xFFA0A5B0);

        int listBtnX = px + pw - PAD - 20;
        rrect(ctx, listBtnX, favBtnY, 17, 17, 0xFF202328);
        ctx.drawCenteredTextWithShadow(tr, "≡", listBtnX + 9, favBtnY + 4, 0xFFA0A5B0);

        // Siatka kart 1:1 ze zrzutu ekranu 1
        List<Modules.Module> list = visible();
        int gx = gridX(), gy = gridY(), gw = gridW(), gh = gridH();
        scroll = MathHelper.clamp(scroll, 0, maxScroll(list.size()));
        int cw = cardW();

        ctx.enableScissor(gx, gy + animOffsetY, gx + gw, gy + gh + animOffsetY);
        if (list.isEmpty()) {
            ctx.drawText(tr, "Brak modułów w tej kategorii", gx + 4, gy + 8, 0xFF8B909B, false);
        }
        for (int i = 0; i < list.size(); i++) {
            Modules.Module m = list.get(i);
            int cx = cardX(i), cy = cardY(i) + tabShift / 2;
            if (cy + CARD_H < gy || cy > gy + gh) continue;
            boolean inGrid = inside(mx, my - animOffsetY, gx, gy, gw, gh);
            boolean hov = inGrid && inside(mx, my - animOffsetY, cx, cy, cw, CARD_H);
            boolean on = m.isOn();

            // Karta modułu
            rrect(ctx, cx - 1, cy - 1, cw + 2, CARD_H + 2, hov ? 0xFF3A3E46 : 0xFF26292E);
            rrect(ctx, cx, cy, cw, CARD_H, hov ? 0xFF23262B : 0xFF1D2024);

            // Tytuł u góry po lewej
            String title = tr.trimToWidth(m.name, cw - 24);
            ctx.drawText(tr, title, cx + 8, cy + 7, 0xFFE8ECF2, false);

            // Serduszko u góry po prawej
            String heart = m.isFavorite() ? "♥" : "♡";
            ctx.drawText(tr, heart, cx + cw - 14, cy + 7, m.isFavorite() ? 0xFFFF5577 : 0xFF7D838E, false);

            // Centralna ikona wektorowa 1:1 jak na SS 1
            drawModuleCenterIcon(ctx, m.icon, cx + cw / 2, cy + 40, on ? 0xFFFFFFFF : 0xFF7E848F);

            // Dolny pasek karty: po lewej zębatka ⚙, po prawej szeroki przycisk Włączony / Wyłączony
            int bottomY = cy + CARD_H - 22;
            int gearX = cx + 7;
            if (m.configurable) {
                boolean ghov = inGrid && inside(mx, my - animOffsetY, gearX, bottomY, 16, 16);
                ctx.drawCenteredTextWithShadow(tr, "⚙", gearX + 8, bottomY + 4, ghov ? 0xFFFFFFFF : 0xFF8C929D);
            }

            int btnX = cx + (m.configurable ? 26 : 8);
            int btnW = cw - (m.configurable ? 34 : 16);
            int btnH = 16;

            if (m.alwaysOn) {
                rrect(ctx, btnX, bottomY, btnW, btnH, 0xFF2EBD6B);
                String lbl = "Włączony";
                ctx.drawText(tr, lbl, btnX + (btnW - tr.getWidth(lbl)) / 2, bottomY + 4, 0xFFFFFFFF, false);
            } else {
                boolean bhov = inGrid && inside(mx, my - animOffsetY, btnX, bottomY, btnW, btnH);
                int bg = on ? (bhov ? 0xFF39D47A : 0xFF2EBD6B) : (bhov ? 0xFF383C44 : 0xFF2E3239);
                rrect(ctx, btnX, bottomY, btnW, btnH, bg);
                String lbl = on ? "Włączony" : "Wyłączony";
                ctx.drawText(tr, lbl, btnX + (btnW - tr.getWidth(lbl)) / 2, bottomY + 4, on ? 0xFFFFFFFF : 0xFFAEB4BE, false);
            }
        }
        ctx.disableScissor();

        // Pasek przewijania
        int max = maxScroll(list.size());
        if (max > 0) {
            int trackH = gh;
            int barH = Math.max(20, trackH * trackH / (trackH + max));
            int barY = gy + (int) ((trackH - barH) * (scroll / max));
            ctx.fill(gx + gw + 3, barY, gx + gw + 5, barY + barH, 0xFF3A3E46);
        }

        ctx.getMatrices().pop();
    }

    private void drawSidebarFooterItem(DrawContext ctx, TextRenderer tr, int mx, int my, int x, int y, int w, String label, boolean highlighted) {
        boolean hov = inside(mx, my, x, y, w, 18);
        if (highlighted) {
            rrect(ctx, x, y, w, 18, hov ? 0xFF32363E : 0xFF262A31);
        } else if (hov) {
            rrect(ctx, x, y, w, 18, 0xFF1F2227);
        }
        String prefix = highlighted ? "✥ " : (label.startsWith("Konf") ? "❐ " : "⚙ ");
        ctx.drawText(tr, prefix + label, x + 6, y + 5, highlighted ? 0xFFFFFFFF : 0xFFA4AAB5, false);
    }

    private void drawTabIcon(DrawContext ctx, Modules.Tab tab, int x, int y, int col) {
        switch (tab) {
            case ALL -> {
                ctx.fill(x, y, x + 3, y + 3, col);
                ctx.fill(x + 5, y, x + 8, y + 3, col);
                ctx.fill(x, y + 5, x + 3, y + 8, col);
                ctx.fill(x + 5, y + 5, x + 8, y + 8, col);
            }
            case ENABLED -> {
                ctx.drawBorder(x, y, 8, 8, col);
                ctx.fill(x + 2, y + 4, x + 4, y + 6, col);
                ctx.fill(x + 4, y + 2, x + 6, y + 5, col);
            }
            case WALKA -> {
                ctx.fill(x + 1, y + 1, x + 3, y + 3, col);
                ctx.fill(x + 3, y + 3, x + 5, y + 5, col);
                ctx.fill(x + 5, y + 5, x + 8, y + 8, col);
            }
            case WYGLAD -> {
                ctx.fill(x + 3, y, x + 5, y + 8, col);
                ctx.fill(x, y + 3, x + 8, y + 5, col);
            }
            case HUD -> {
                ctx.drawBorder(x, y + 1, 8, 6, col);
                ctx.fill(x + 2, y + 3, x + 6, y + 4, col);
            }
            case DZWIEKI -> {
                ctx.fill(x + 1, y + 2, x + 4, y + 6, col);
                ctx.fill(x + 5, y + 1, x + 7, y + 7, col);
            }
            case NARZEDZIA -> {
                ctx.fill(x + 2, y + 1, x + 6, y + 3, col);
                ctx.fill(x + 3, y + 3, x + 5, y + 8, col);
            }
        }
    }

    private void drawModuleCenterIcon(DrawContext ctx, Modules.IconType icon, int cx, int cy, int col) {
        switch (icon) {
            case CUBE -> {
                ctx.drawBorder(cx - 8, cy - 8, 16, 16, col);
                ctx.drawBorder(cx - 5, cy - 5, 10, 10, col);
            }
            case CROSSHAIR -> {
                ctx.drawBorder(cx - 7, cy - 7, 14, 14, col);
                ctx.fill(cx - 10, cy, cx - 4, cy + 1, col);
                ctx.fill(cx + 5, cy, cx + 11, cy + 1, col);
                ctx.fill(cx, cy - 10, cx + 1, cy - 4, col);
                ctx.fill(cx, cy + 5, cx + 1, cy + 11, col);
            }
            case SHIELD -> {
                ctx.fill(cx - 7, cy - 8, cx + 7, cy + 1, col);
                ctx.fill(cx - 5, cy + 1, cx + 5, cy + 5, col);
                ctx.fill(cx - 2, cy + 5, cx + 2, cy + 8, col);
                ctx.fill(cx - 2, cy - 2, cx + 3, cy + 1, 0xFF1D2024);
            }
            case BOLT -> {
                ctx.fill(cx, cy - 9, cx + 4, cy - 1, col);
                ctx.fill(cx - 4, cy - 2, cx + 5, cy + 1, col);
                ctx.fill(cx - 3, cy + 1, cx + 1, cy + 9, col);
            }
            case PALETTE -> {
                ctx.drawBorder(cx - 8, cy - 8, 16, 16, col);
                ctx.fill(cx - 4, cy - 4, cx - 1, cy - 1, col);
                ctx.fill(cx + 2, cy - 4, cx + 5, cy - 1, col);
                ctx.fill(cx - 4, cy + 2, cx - 1, cy + 5, col);
            }
            case INVENTORY -> {
                ctx.drawBorder(cx - 9, cy - 6, 18, 12, col);
                ctx.fill(cx - 3, cy - 6, cx - 2, cy + 6, col);
                ctx.fill(cx + 3, cy - 6, cx + 4, cy + 6, col);
                ctx.fill(cx - 9, cy, cx + 9, cy + 1, col);
            }
            case TIMER -> {
                ctx.drawBorder(cx - 7, cy - 7, 14, 14, col);
                ctx.fill(cx, cy - 4, cx + 1, cy + 1, col);
                ctx.fill(cx, cy, cx + 4, cy + 1, col);
            }
            case BOMB -> {
                ctx.fill(cx - 6, cy - 4, cx + 6, cy + 8, col);
                ctx.fill(cx - 4, cy - 6, cx + 4, cy + 9, col);
                ctx.fill(cx - 1, cy - 9, cx + 1, cy - 6, col);
                ctx.fill(cx + 1, cy - 10, cx + 4, cy - 8, 0xFFFF6644);
            }
            default -> {
                ctx.drawBorder(cx - 7, cy - 7, 14, 14, col);
                ctx.fill(cx - 3, cy - 3, cx + 3, cy + 3, col);
            }
        }
    }

    private void drawBombLogo(DrawContext ctx, int x, int y) {
        // Biała ikona bomby z lontem obok pogrubionego napisu BOMBOWY (1:1 styl logo z SS 1)
        ctx.fill(x + 2, y + 5, x + 13, y + 16, 0xFFFFFFFF);
        ctx.fill(x + 4, y + 3, x + 11, y + 18, 0xFFFFFFFF);
        ctx.fill(x + 6, y + 1, x + 9, y + 3, 0xFFAEB4BE);
        ctx.fill(x + 9, y, x + 12, y + 2, 0xFFFF6B3D);

        ctx.drawText(this.textRenderer, Text.literal("BOMBOWY").styled(s -> s.withBold(true)), x + 18, y + 6, 0xFFFFFFFF, false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            Modules.Tab[] tabs = Modules.Tab.values();
            for (int i = 0; i < tabs.length; i++) {
                if (inside(mx, my, px + 8, py + 42 + i * 22, SIDE - 16, 19)) {
                    playClick();
                    lastTab = i;
                    filterFavorites = false;
                    tabAnim = 0.0f;
                    scroll = 0;
                    search.setText("");
                    return true;
                }
            }

            // Przycisk serduszka u góry obok wyszukiwarki
            int favBtnX = px + pw - PAD - 40;
            if (inside(mx, my, favBtnX, py + 15, 17, 17)) {
                playClick();
                filterFavorites = !filterFavorites;
                scroll = 0;
                return true;
            }

            // Dolne przyciski w lewym panelu (Konfiguracja, Ustawienia, Edytor HUD)
            int bottomBase = py + ph - 82;
            if (inside(mx, my, px + 8, bottomBase, SIDE - 16, 18)) {
                playClick();
                if (this.client != null) {
                    this.client.setScreen(new ModuleSettingsScreen(Modules.get("hitboxes"), this));
                }
                return true;
            }
            if (inside(mx, my, px + 8, bottomBase + 20, SIDE - 16, 18)) {
                playClick();
                if (this.client != null) {
                    this.client.setScreen(new ModuleSettingsScreen(Modules.get("optimization"), this));
                }
                return true;
            }
            if (inside(mx, my, px + 8, bottomBase + 41, SIDE - 16, 18)) {
                playClick();
                if (this.client != null) {
                    this.client.setScreen(new HudEditorScreen());
                }
                return true;
            }

            // Karty modułów
            if (inside(mx, my, gridX(), gridY(), gridW(), gridH())) {
                List<Modules.Module> list = visible();
                int cw = cardW();
                for (int i = 0; i < list.size(); i++) {
                    Modules.Module m = list.get(i);
                    int cx = cardX(i), cy = cardY(i);

                    // Serduszko
                    if (inside(mx, my, cx + cw - 18, cy + 4, 16, 14)) {
                        playClick();
                        m.toggleFavorite();
                        return true;
                    }

                    // Zębatka ustawień po lewej na dole (1:1 SS 1 & SS 5)
                    int bottomY = cy + CARD_H - 22;
                    if (m.configurable && inside(mx, my, cx + 6, bottomY, 18, 16)) {
                        playClick();
                        if (this.client != null) {
                            this.client.setScreen(new ModuleSettingsScreen(m, this));
                        }
                        return true;
                    }

                    // Przycisk Włączony / Wyłączony po prawej na dole
                    int btnX = cx + (m.configurable ? 26 : 8);
                    int btnW = cw - (m.configurable ? 34 : 16);
                    if (!m.alwaysOn && inside(mx, my, btnX, bottomY, btnW, 16)) {
                        playClick();
                        m.toggle();
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll = MathHelper.clamp(scroll - verticalAmount * 20, 0, maxScroll(visible().size()));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == GLFW.GLFW_KEY_GRAVE_ACCENT || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) && !search.isFocused()) {
            this.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
