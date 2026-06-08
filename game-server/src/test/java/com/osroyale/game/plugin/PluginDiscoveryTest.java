package com.osroyale.game.plugin;

import com.osroyale.game.event.bus.PlayerDataBus;
import com.osroyale.game.event.listener.PlayerEventListener;
import com.osroyale.game.plugin.extension.CommandExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PluginDiscoveryTest {

    private Set<String> savedPlugins;
    private Set<PlayerEventListener> savedChain;
    private Set<CommandExtension> savedExtensions;

    @BeforeEach
    void setUp() throws Exception {
        savedPlugins = drainStaticSet(PluginManager.class, "plugins");
        savedChain = drainStaticSet(PlayerDataBus.class, "chain");
        savedExtensions = drainStaticSet(CommandExtension.class, "extensions");
    }

    @AfterEach
    void tearDown() throws Exception {
        refillStaticSet(PluginManager.class, "plugins", savedPlugins);
        refillStaticSet(PlayerDataBus.class, "chain", savedChain);
        refillStaticSet(CommandExtension.class, "extensions", savedExtensions);
    }

    @Test
    void shouldLoadAtLeast100Plugins() {
        PluginManager.load("plugin");
        int count = PluginManager.getPluginCount();
        assertTrue(count >= 100, "Expected at least 100 plugins, got " + count);
    }

    @Test
    void shouldPopulatePluginCountAfterLoad() {
        PluginManager.load("plugin");
        assertTrue(PluginManager.getPluginCount() > 0);
    }

    @SuppressWarnings("unchecked")
    private static <T> Set<T> drainStaticSet(Class<?> clazz, String fieldName) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        Set<T> set = (Set<T>) field.get(null);
        Set<T> saved = new HashSet<>(set);
        set.clear();
        return saved;
    }

    @SuppressWarnings("unchecked")
    private static <T> void refillStaticSet(Class<?> clazz, String fieldName, Set<T> saved) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        Set<T> set = (Set<T>) field.get(null);
        set.clear();
        set.addAll(saved);
    }
}