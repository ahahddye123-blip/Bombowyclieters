package dev.bombowy.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.ParticlesMode;
import net.minecraft.client.option.SimpleOption;

import java.util.Map;

public final class Features {
    private Features() {}

    private static Map<String, String> orig() {
        return BombowyConfig.get().orig;
    }

    private static <E extends Enum<E>> void en(String key, SimpleOption<E> opt, E val, boolean on, Class<E> cls) {
        if (on) {
            orig().putIfAbsent(key, opt.getValue().name());
            opt.setValue(val);
        } else {
            String s = orig().remove(key);
            if (s != null) {
                try { opt.setValue(Enum.valueOf(cls, s)); } catch (Exception ignored) { }
            }
        }
    }

    private static void bool(String key, SimpleOption<Boolean> opt, boolean val, boolean on) {
        if (on) {
            orig().putIfAbsent(key, String.valueOf(opt.getValue()));
            opt.setValue(val);
        } else {
            String s = orig().remove(key);
            if (s != null) opt.setValue(Boolean.parseBoolean(s));
        }
    }

    private static void num(String key, SimpleOption<Integer> opt, int val, boolean on, boolean asCap) {
        if (on) {
            orig().putIfAbsent(key, String.valueOf(opt.getValue()));
            int cur = opt.getValue();
            opt.setValue(asCap ? Math.min(cur, val) : val);
        } else {
            String s = orig().remove(key);
            if (s != null) {
                try { opt.setValue(Integer.parseInt(s)); } catch (Exception ignored) { }
            }
        }
    }

    private static void dbl(String key, SimpleOption<Double> opt, double val, boolean on) {
        if (on) {
            orig().putIfAbsent(key, String.valueOf(opt.getValue()));
            opt.setValue(val);
        } else {
            String s = orig().remove(key);
            if (s != null) {
                try { opt.setValue(Double.parseDouble(s)); } catch (Exception ignored) { }
            }
        }
    }

    public static void fpsBoost(boolean on) {
        GameOptions o = MinecraftClient.getInstance().options;
        en("graphics", o.getGraphicsMode(), GraphicsMode.FAST, on, GraphicsMode.class);
        en("particles", o.getParticles(), ParticlesMode.MINIMAL, on, ParticlesMode.class);
        en("clouds", o.getCloudRenderMode(), CloudRenderMode.OFF, on, CloudRenderMode.class);
        bool("shadows", o.getEntityShadows(), false, on);
    }

    public static void unlimitedFps(boolean on) {
        GameOptions o = MinecraftClient.getInstance().options;
        num("maxfps", o.getMaxFps(), 260, on, false);
        bool("vsync", o.getEnableVsync(), false, on);
    }

    public static void fastLighting(boolean on) {
        GameOptions o = MinecraftClient.getInstance().options;
        bool("ao", o.getAo(), false, on);
    }

    public static void entityDistance(boolean on) {
        GameOptions o = MinecraftClient.getInstance().options;
        dbl("entdist", o.getEntityDistanceScaling(), 0.6, on);
    }

    public static void chunkCap(boolean on) {
        GameOptions o = MinecraftClient.getInstance().options;
        num("view", o.getViewDistance(), 12, on, true);
        num("sim", o.getSimulationDistance(), 8, on, true);
    }

    public static void quality(boolean on) {
        GameOptions o = MinecraftClient.getInstance().options;
        num("mipmap", o.getMipmapLevels(), 4, on, false);
        num("blend", o.getBiomeBlendRadius(), 3, on, false);
    }

    public static void optimization(boolean on) {
        GameOptions o = MinecraftClient.getInstance().options;
        boolean apply = on && BombowyConfig.get().potatoMode;
        en("opt_gfx", o.getGraphicsMode(), GraphicsMode.FAST, apply, GraphicsMode.class);
        en("opt_prt", o.getParticles(), ParticlesMode.MINIMAL, apply, ParticlesMode.class);
        en("opt_cld", o.getCloudRenderMode(), CloudRenderMode.OFF, apply, CloudRenderMode.class);
        bool("opt_shd", o.getEntityShadows(), false, apply);
        bool("opt_ao", o.getAo(), false, apply);
        dbl("opt_ed", o.getEntityDistanceScaling(), 0.5, apply);
        num("opt_view", o.getViewDistance(), 8, apply, true);
        num("opt_sim", o.getSimulationDistance(), 6, apply, true);
    }

    public static void writeOptions() {
        MinecraftClient.getInstance().options.write();
    }
}
