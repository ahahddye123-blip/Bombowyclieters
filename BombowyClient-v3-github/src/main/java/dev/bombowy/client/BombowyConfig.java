package dev.bombowy.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BombowyConfig {
    public Map<String, Boolean> modules = new HashMap<>();
    public Map<String, String> orig = new HashMap<>();
    public Set<String> favorites = new HashSet<>();

    // ---- Hitboxy ----
    public boolean hitboxAimOnly = true;
    public double hitboxReach = 3.0;
    public int hitboxColorAim = 0xFFFF3B3B;   // czerwony przy celowaniu
    public int hitboxColorSide = 0xFFFFFFFF;  // biały gdy widoczny z boku

    // ---- Celownik ----
    public int crosshairSize = 5;
    public int crosshairGap = 2;
    public int crosshairColor = 0xFFFFFFFF;
    public boolean crosshairDot = true;

    // ---- Status tarczy / Armor HUD ----
    public boolean showShieldIcon = true;
    public boolean showArmorDurability = true;

    // ---- Czysty obraz ----
    public boolean cleanLowFire = true;
    public boolean cleanNoVignette = true;
    public boolean cleanNoCameraShake = true;

    // ---- Boost kolorów ----
    public float colorBoostStrength = 0.35f;

    // ---- Optymalizacja / Low-End ----
    public boolean potatoMode = false;
    public boolean disableBlockParticles = true;

    // ---- Scoreboard ----
    public boolean scoreboardHideNumbers = true;
    public boolean scoreboardTransparent = true;

    // ---- Odznaka bomby przy nicku (zawsze aktywna, biały nick) ----
    public boolean bombBadgeShowSelf = true;
    public Set<String> bombBadgeUsers = new HashSet<>();

    // ---- HUD elements (x, y, scale, enabled) ----
    public Map<String, HudElementConfig> hud = new HashMap<>();

    public static class HudElementConfig {
        public int x;
        public int y;
        public float scale = 1.0f;
        public boolean enabled = true;

        public HudElementConfig() {}

        public HudElementConfig(int x, int y, float scale, boolean enabled) {
            this.x = x;
            this.y = y;
            this.scale = scale;
            this.enabled = enabled;
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static BombowyConfig instance = new BombowyConfig();

    public static BombowyConfig get() {
        return instance;
    }

    public static HudElementConfig hud(String key, int defaultX, int defaultY, float defaultScale) {
        if (instance.hud == null) instance.hud = new HashMap<>();
        return instance.hud.computeIfAbsent(key, k -> new HudElementConfig(defaultX, defaultY, defaultScale, true));
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("bombowyclient.json");
    }

    public static void load() {
        try {
            Path p = path();
            if (Files.exists(p)) {
                BombowyConfig c = GSON.fromJson(Files.readString(p), BombowyConfig.class);
                if (c != null) {
                    if (c.orig == null) c.orig = new HashMap<>();
                    if (c.modules == null) c.modules = new HashMap<>();
                    if (c.favorites == null) c.favorites = new HashSet<>();
                    if (c.hud == null) c.hud = new HashMap<>();
                    if (c.bombBadgeUsers == null) c.bombBadgeUsers = new HashSet<>();
                    c.colorBoostStrength = Math.max(0.0f, Math.min(1.5f, c.colorBoostStrength));
                    instance = c;
                }
            }
        } catch (Exception e) {
            System.err.println("[BombowyClient] could not read config: " + e);
        }
    }

    public static void save() {
        try {
            Files.createDirectories(path().getParent());
            Files.writeString(path(), GSON.toJson(instance));
        } catch (Exception e) {
            System.err.println("[BombowyClient] could not save config: " + e);
        }
    }
}
