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
 * The player settings, edited on the YACL screen (SettingsScreen) and kept in
 * {@code config/honestdamage/config.json}. Loaded the first time anything reads it,
 * so the mod needs no entrypoint. A missing or unreadable file means the default.
 */
public final class Settings {
    private static final Logger LOG = LoggerFactory.getLogger("honestdamage");
    private static final int CURRENT_VERSION = 1;
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("honestdamage").resolve("config.json");

    /** Off: a hit that took health without moving a heart stays silent, with no ding. */
    public static boolean ding = true;

    /** The ding's default sound: an iron armor clank, since the armor took the hit. */
    public static final String DEFAULT_SOUND = "minecraft:item.armor.equip_iron";
    /** Sound ID the ding plays, typed by the player. HonestDamage falls back to DEFAULT_SOUND if it isn't a known sound. */
    public static String sound = DEFAULT_SOUND;

    static {
        if (Files.exists(FILE)) {
            try {
                JsonObject root = JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();
                ding = root.has("ding") && root.get("ding").isJsonPrimitive() ? root.get("ding").getAsBoolean() : ding;
                sound = root.has("sound") && root.get("sound").isJsonPrimitive() ? root.get("sound").getAsString() : sound;
            } catch (Exception e) {
                LOG.error("[config] failed to read {}, using defaults", FILE, e);
            }
        }
    }

    static void save() {
        JsonObject root = new JsonObject();
        root.addProperty("version", CURRENT_VERSION);
        root.addProperty("ding", ding);
        root.addProperty("sound", sound);
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, new GsonBuilder().setPrettyPrinting().create().toJson(root));
        } catch (Exception e) {
            LOG.error("[config] failed to write {}, changes won't persist", FILE, e);
        }
    }

    private Settings() {}
}
