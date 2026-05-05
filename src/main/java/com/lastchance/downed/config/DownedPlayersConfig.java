package com.lastchance.downed.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DownedPlayersConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("downed_players.json");

    private static DownedPlayersConfig current = defaults();

    public int downed_duration_seconds;
    public int revive_duration_seconds;
    public double revive_distance_blocks;
    public double loot_open_distance_blocks;
    public double hp_after_revive;
    public boolean allow_full_inventory_looting;
    public boolean allow_multiple_looters_at_once;
    public boolean allow_downed_player_to_take_damage;
    public boolean allow_finishing_downed_player;
    public boolean enable_surrender_button;
    public boolean enable_chat_status_messages;
    public boolean preserve_downed_state_on_logout;
    public boolean preserve_downed_state_on_server_restart;

    public static DownedPlayersConfig get() {
        return current;
    }

    public static void load() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());

            if (!Files.exists(CONFIG_PATH)) {
                current = defaults();
                save();
                return;
            }

            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                DownedPlayersConfig loaded = GSON.fromJson(reader, DownedPlayersConfig.class);
                current = loaded == null ? defaults() : loaded.withSanityLimits();
            }

            save();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load Downed Players config", exception);
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());

            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(current.withSanityLimits(), writer);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to save Downed Players config", exception);
        }
    }

    public static DownedPlayersConfig defaults() {
        DownedPlayersConfig config = new DownedPlayersConfig();
        config.downed_duration_seconds = 120;
        config.revive_duration_seconds = 10;
        config.revive_distance_blocks = 4.0D;
        config.loot_open_distance_blocks = 4.0D;
        config.hp_after_revive = 6.0D;
        config.allow_full_inventory_looting = true;
        config.allow_multiple_looters_at_once = false;
        config.allow_downed_player_to_take_damage = false;
        config.allow_finishing_downed_player = false;
        config.enable_surrender_button = true;
        config.enable_chat_status_messages = true;
        config.preserve_downed_state_on_logout = true;
        config.preserve_downed_state_on_server_restart = true;
        return config;
    }

    private DownedPlayersConfig withSanityLimits() {
        downed_duration_seconds = Math.max(1, downed_duration_seconds);
        revive_duration_seconds = Math.max(1, revive_duration_seconds);
        revive_distance_blocks = Math.max(1.0D, revive_distance_blocks);
        loot_open_distance_blocks = Math.max(1.0D, loot_open_distance_blocks);
        hp_after_revive = Math.max(1.0D, hp_after_revive);
        return this;
    }
}
