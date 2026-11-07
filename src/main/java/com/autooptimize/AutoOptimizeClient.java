package com.autooptimize;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.ParticlesMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Modo SUPER permanente. Niveles: 0 = SUPER (base), 1 = EXTREMO, 2 = ULTRA.
 * Sube de nivel solo si aun asi los FPS son bajos, y baja cuando sobran FPS.
 */
public class AutoOptimizeClient implements ClientModInitializer {

    // Culling de entidades normales (bloques)
    private static final double[] ENTITY_DIST   = {80, 56, 40};
    // Culling de items / orbes de XP (bloques)
    private static final double[] SMALL_DIST    = {24, 16, 12};
    // Culling de block entities: cofres, carteles, camas... (bloques)
    private static final double[] BLOCKENT_DIST = {40, 28, 20};
    // A partir de esta distancia los mobs se tickean 1 de cada 4 veces (cliente)
    private static final double[] TICK_DIST     = {32, 24, 16};
    // Texturas animadas: se actualizan 1 de cada N ticks
    private static final int[]    ANIM_DIVISOR  = {4, 8, 20};
    // Particulas permitidas (solo nivel 0: feedback de combate cerca)
    private static final double[] PARTICLE_DIST = {16, 0, 0};

    // Maximo de entidades lejanas (>12 bloques, dentro de pantalla) renderizadas por frame
    private static final int[]    FAR_CAP       = {64, 40, 24};
    private static final String[] NAMES         = {"SUPER", "EXTREMO", "ULTRA"};
    private static final Logger   LOG           = LoggerFactory.getLogger("AutoOptimize");

    private static final int[]    VIEW_CAP = {12, 10, 8};
    private static final int[]    SIM_CAP  = {8, 6, 5};
    private static final double[] ENT_SCALE = {0.75, 0.6, 0.5};

    public static volatile int level = 0;

    private static final int LOW_FPS = 30, HIGH_FPS = 90, UNFOCUSED_FPS = 10;

    private int tickCounter, lowSamples, highSamples, cooldown;
    private boolean baselineApplied, unfocusedApplied, menuTweaked;

    // Valores originales del jugador
    private boolean saved;
    private ParticlesMode oParticles;
    private double oEntScale;
    private int oView, oSim, oBlend, oMaxFps;
    private boolean oShadows, oAo;
    private CloudRenderMode oClouds;
    private GraphicsMode oGraphics;

    public static double entityDistSq()   { return sq(ENTITY_DIST[level]); }
    public static double smallDistSq()    { return sq(SMALL_DIST[level]); }
    public static double blockEntDistSq() { return sq(BLOCKENT_DIST[level]); }
    public static double tickDistSq()     { return sq(TICK_DIST[level]); }
    public static double particleDistSq() { return sq(PARTICLE_DIST[level]); }
    public static int animDivisor()       { return ANIM_DIVISOR[level]; }
    public static int farCap()            { return FAR_CAP[level]; }
    public static boolean cancelRain()    { return level >= 1; }
    private static double sq(double d)    { return d * d; }

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(c -> restoreAll(c.options));
    }

    private void onTick(MinecraftClient client) {
        GameOptions o = client.options;
        if (!menuTweaked && o != null) {
            // Menos pantallas de aviso al iniciar (se aplica en los siguientes arranques)
            o.onboardAccessibility = false;
            o.skipMultiplayerWarning = true;
            menuTweaked = true;
        }
        if (client.world == null || client.player == null) {
            if (baselineApplied) { level = 0; restoreAll(o); baselineApplied = false; }
            return;
        }
        if (!baselineApplied) { applyLevel(o); baselineApplied = true; }

        handleFocus(client, o);

        if (++tickCounter < 20) return;
        tickCounter = 0;
        if (cooldown > 0) cooldown--;
        if (!client.isWindowFocused()) return;

        int fps = client.getCurrentFps();
        if (fps < LOW_FPS) { lowSamples++; highSamples = 0; }
        else if (fps > HIGH_FPS) { highSamples++; lowSamples = 0; }
        else { lowSamples = 0; highSamples = 0; }

        if (lowSamples >= 3 && level < 2 && cooldown == 0) {
            level++; lowSamples = 0; cooldown = 5; applyLevel(o);
        } else if (highSamples >= 20 && level > 0 && cooldown == 0) {
            level--; highSamples = 0; cooldown = 15; applyLevel(o);
        }
    }

    private void handleFocus(MinecraftClient client, GameOptions o) {
        if (!client.isWindowFocused()) {
            if (!unfocusedApplied) {
                oMaxFps = o.getMaxFps().getValue();
                o.getMaxFps().setValue(UNFOCUSED_FPS);
                unfocusedApplied = true;
            }
        } else if (unfocusedApplied) {
            o.getMaxFps().setValue(oMaxFps);
            unfocusedApplied = false;
        }
    }

    private void saveOriginals(GameOptions o) {
        if (saved) return;
        oParticles = o.getParticles().getValue();
        oEntScale  = o.getEntityDistanceScaling().getValue();
        oView      = o.getViewDistance().getValue();
        oSim       = o.getSimulationDistance().getValue();
        oBlend     = o.getBiomeBlendRadius().getValue();
        oShadows   = o.getEntityShadows().getValue();
        oAo        = o.getAo().getValue();
        oClouds    = o.getCloudRenderMode().getValue();
        oGraphics  = o.getGraphicsMode().getValue();
        saved = true;
    }

    private void applyLevel(GameOptions o) {
        LOG.info("[AutoOptimize] Nivel {}", NAMES[level]);
        saveOriginals(o);
        o.getParticles().setValue(ParticlesMode.MINIMAL);
        o.getEntityShadows().setValue(false);
        o.getCloudRenderMode().setValue(CloudRenderMode.OFF);
        o.getGraphicsMode().setValue(GraphicsMode.FAST);
        o.getAo().setValue(false);
        o.getBiomeBlendRadius().setValue(Math.min(oBlend, level == 0 ? 1 : 0));
        o.getEntityDistanceScaling().setValue(Math.min(oEntScale, ENT_SCALE[level]));
        o.getViewDistance().setValue(Math.min(oView, VIEW_CAP[level]));
        o.getSimulationDistance().setValue(Math.min(oSim, SIM_CAP[level]));
    }

    private void restoreAll(GameOptions o) {
        if (o == null) return;
        if (saved) {
            o.getParticles().setValue(oParticles);
            o.getEntityShadows().setValue(oShadows);
            o.getCloudRenderMode().setValue(oClouds);
            o.getGraphicsMode().setValue(oGraphics);
            o.getAo().setValue(oAo);
            o.getBiomeBlendRadius().setValue(oBlend);
            o.getEntityDistanceScaling().setValue(oEntScale);
            o.getViewDistance().setValue(oView);
            o.getSimulationDistance().setValue(oSim);
            saved = false;
        }
        if (unfocusedApplied) { o.getMaxFps().setValue(oMaxFps); unfocusedApplied = false; }
    }
}
