package com.trevorschoeny.honestdamage;

/**
 * The three player settings from the design. Defaults only for now: there is no settings
 * file or screen yet, so these never change at runtime.
 */
public final class Settings {
    /** Which hits count as real. */
    public enum Threshold {
        /** Any health or absorption lost, however little. */
        DAMAGE_TAKEN,
        /** The heart bar lost at least a half heart (red or yellow). */
        HEARTS_MOVED
    }

    // ponytail: plain statics until settings delivery is decided (Mod Hub, YACL or a JSON file).
    /** Off: every hit plays vanilla's hurt cues, and there is no ding. */
    public static boolean silencing = true;
    public static Threshold threshold = Threshold.HEARTS_MOVED;
    /** Off: a hit the armor took is silent, with no ding. */
    public static boolean armorDing = true;

    private Settings() {}
}
