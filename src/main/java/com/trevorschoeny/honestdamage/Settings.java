package com.trevorschoeny.honestdamage;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The two player settings, edited on the YACL screen (SettingsScreen) and kept in
 * {@code config/honestdamage/config.json}. Loaded the first time anything reads them,
 * so the mod needs no entrypoint. A missing or unreadable file means defaults.
 */
public final class Settings {
    private static final Logger LOG = LoggerFactory.getLogger("honestdamage");
    private static final int CURRENT_VERSION = 1;
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("honestdamage").resolve("config.json");

    /** Off: every hit plays vanilla's hurt cues, and there is no ding. */
    public static boolean silencing = true;
    /** Off: a hit that took health without moving a heart is silent, with no ding. */
    public static boolean ding = true;

    static {
        if (Files.exists(FILE)) {
            try {
                JsonObject root = JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();
                silencing = readBool(root, "silencing", silencing);
                ding = readBool(root, "ding", ding);
            } catch (Exception e) {
                LOG.error("[config] failed to read {}, using defaults", FILE, e);
            }
        }
    }

    private static boolean readBool(JsonObject root, String key, boolean fallback) {
        return root.has(key) && root.get(key).isJsonPrimitive() ? root.get(key).getAsBoolean() : fallback;
    }

    static void save() {
        JsonObject root = new JsonObject();
        root.addProperty("version", CURRENT_VERSION);
        root.addProperty("silencing", silencing);
        root.addProperty("ding", ding);
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, new GsonBuilder().setPrettyPrinting().create().toJson(root));
        } catch (Exception e) {
            LOG.error("[config] failed to write {}, changes won't persist", FILE, e);
        }
    }

    private Settings() {}
}
