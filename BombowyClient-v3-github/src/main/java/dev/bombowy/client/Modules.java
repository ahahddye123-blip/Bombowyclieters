package dev.bombowy.client;

import java.util.List;
import java.util.function.Consumer;

public final class Modules {
    private Modules() {}

    public enum Tab {
        ALL("Wszystkie"),
        ENABLED("Włączone"),
        WALKA("Walka"),
        WYGLAD("Wygląd"),
        HUD("HUD"),
        DZWIEKI("Dźwięki"),
        NARZEDZIA("Narzędzia");

        public final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    public enum IconType {
        CUBE, CROSSHAIR, SHIELD, BOLT, SPARKLES, DROP, SLASH, ARROWS,
        PEARL, CAPE, PALETTE, WIND, SUN, SKY, INVENTORY, TIMER,
        SCOREBOARD, FPS, GEAR, BOMB, TAG, PING
    }

    public static final class Module {
        public final String id;
        public final String name;
        public final String desc;
        public final Tab tab;
        public final IconType icon;
        public final boolean configurable;
        public final boolean alwaysOn;
        private final Consumer<Boolean> action;

        Module(String id, String name, String desc, Tab tab, IconType icon, boolean configurable, boolean alwaysOn, Consumer<Boolean> action) {
            this.id = id;
            this.name = name;
            this.desc = desc;
            this.tab = tab;
            this.icon = icon;
            this.configurable = configurable;
            this.alwaysOn = alwaysOn;
            this.action = action;
        }

        public boolean isOn() {
            if (alwaysOn) return true;
            return Boolean.TRUE.equals(BombowyConfig.get().modules.get(id));
        }

        public void setOn(boolean on) {
            if (alwaysOn) return;
            action.accept(on);
            BombowyConfig.get().modules.put(id, on);
            Features.writeOptions();
            BombowyConfig.save();
        }

        public void toggle() {
            if (alwaysOn) return;
            setOn(!isOn());
        }

        public boolean isFavorite() {
            return BombowyConfig.get().favorites.contains(id);
        }

        public void toggleFavorite() {
            if (!BombowyConfig.get().favorites.add(id)) {
                BombowyConfig.get().favorites.remove(id);
            }
            BombowyConfig.save();
        }
    }

    public static final List<Module> ALL = List.of(
            // ---- Walka ----
            new Module("hitboxes", "Hitboxy", "Ramki graczy zmieniające kolor przy celowaniu w przeciwnika.", Tab.WALKA, IconType.CUBE, true, false, on -> {}),
            new Module("crosshair", "Celownik", "Własny precyzyjny celownik PvP z kropką i regulacją rozmiaru.", Tab.WALKA, IconType.CROSSHAIR, true, false, on -> {}),
            new Module("shieldstatus", "Status tarczy", "Wyświetla stan tarczy oraz wytrzymałość pancerza na HUD.", Tab.WALKA, IconType.SHIELD, true, false, on -> {}),
            new Module("hiteffects", "Efekty uderzenia", "Wyraźne potwierdzenie trafienia i podświetlenie celu.", Tab.WALKA, IconType.BOLT, false, false, on -> {}),

            // ---- Wygląd ----
            new Module("cleanview", "Czysty obraz", "Niski ogień, brak winiety oraz brak trzęsienia kamery.", Tab.WYGLAD, IconType.SPARKLES, true, false, on -> {}),
            new Module("noblockparticles", "Bez cząsteczek bloków", "Usuwa okruchy bloków przy kopaniu dla czystszego obrazu i FPS.", Tab.WYGLAD, IconType.DROP, true, false, on -> {}),
            new Module("colorboost", "Boost kolorów", "Żywe kolory świata jak w montażach PvP.", Tab.WYGLAD, IconType.PALETTE, true, false, on -> {}),
            new Module("brightness", "Jasność", "Wyraźny obraz w nocy i w jaskiniach.", Tab.WYGLAD, IconType.SUN, false, false, on -> {}),
            new Module("quality", "Jakość 2K/4K", "Ostrzejszy obraz (mipmapy 4, biome blend 3).", Tab.WYGLAD, IconType.SKY, false, false, Features::quality),
            new Module("fpsboost", "FPS Boost", "Grafika Fast, minimalne cząsteczki, bez chmur i cieni.", Tab.WYGLAD, IconType.WIND, false, false, Features::fpsBoost),
            new Module("fastlight", "Szybkie światło", "Wyłącza gładkie oświetlenie (AO) dla wyższego FPS.", Tab.WYGLAD, IconType.SLASH, false, false, Features::fastLighting),

            // ---- HUD ----
            new Module("inventoryhud", "Ekwipunek na HUD", "Twój ekwipunek stale widoczny na ekranie.", Tab.HUD, IconType.INVENTORY, true, false, on -> {}),
            new Module("cooldowns", "Cooldowny", "Przezroczysty licznik odnowienia perły, tarczy i przedmiotów.", Tab.HUD, IconType.TIMER, true, false, on -> {}),
            new Module("fpshud", "Licznik FPS & CPS", "Przezroczysty wskaźnik FPS, CPS i koordynatów na HUD.", Tab.HUD, IconType.FPS, true, false, on -> {}),
            new Module("scoreboard", "Scoreboard", "Przezroczysty scoreboard bez czerwonych liczb.", Tab.HUD, IconType.SCOREBOARD, true, false, on -> {}),
            new Module("bombBadge", "Odznaka Bomby", "Zawsze aktywna ikona bomby przy białym nicku BombowyClient.", Tab.HUD, IconType.BOMB, true, true, on -> {}),
            new Module("nametags", "Nicki", "Przezroczyste tło nicków i własny biały nick w F5.", Tab.HUD, IconType.TAG, false, false, on -> {}),

            // ---- Dźwięki ----
            new Module("hitsounds", "Dźwięk kliknięć UI", "Czyste dźwięki interfejsu podczas konfiguracji.", Tab.DZWIEKI, IconType.ARROWS, false, true, on -> {}),

            // ---- Narzędzia ----
            new Module("optimization", "Optymalizacja", "Więcej FPS i tryb słabego komputera (Potato Mode).", Tab.NARZEDZIA, IconType.GEAR, true, false, Features::optimization),
            new Module("unlimitedfps", "Unlimited FPS", "Bez limitu FPS i z wyłączonym VSync.", Tab.NARZEDZIA, IconType.ARROWS, false, false, Features::unlimitedFps),
            new Module("entitydist", "Dystans encji 60%", "Encje renderują się z mniejszej odległości.", Tab.NARZEDZIA, IconType.PEARL, false, false, Features::entityDistance),
            new Module("chunkcap", "Limit chunków 12/8", "Render distance maks. 12, simulation distance maks. 8.", Tab.NARZEDZIA, IconType.CAPE, false, false, Features::chunkCap),
            new Module("quickjoin", "Szybkie dołączanie", "Podgląd graczy i pingu serwera po wpisaniu adresu.", Tab.NARZEDZIA, IconType.PING, false, false, on -> {})
    );

    public static Module get(String id) {
        for (Module m : ALL) {
            if (m.id.equals(id)) return m;
        }
        return null;
    }

    public static boolean isOn(String id) {
        Module m = get(id);
        if (m != null && m.alwaysOn) return true;
        return Boolean.TRUE.equals(BombowyConfig.get().modules.get(id));
    }
}
