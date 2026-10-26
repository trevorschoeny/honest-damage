package com.trevorschoeny.honestdamage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * The gate between a hit reaching the local player and its hurt cues.
 *
 * <p>A server tells the client about a hit with up to three packets, in this order:
 * the damage event (hurt sound, red flash, and the flash's screen shake), the hurt
 * direction (which way the shake tilts), and, if health changed, the new health. A hit
 * that took no health sends no health packet at all. So the cues can't be judged
 * when they arrive: the hit is held here as pending, and each client tick compares the
 * player's hearts with how they were when it arrived. If a heart moved, the held cues
 * play through vanilla's own code; if none did by the end of the window, they are
 * dropped, and a ding plays when health still went down (the hit landed, but too little
 * to show). A hit that took nothing at all stays silent.
 *
 * <p>Only the cues are held. Health, armor, knockback and death are the server's, and
 * nothing here touches them.
 */
public final class HonestDamage {
    /**
     * How long a hit waits for its health packet. The server sends that packet in the
     * same tick as the damage event, so it normally lands in the same or next client
     * tick; the rest is slack for a laggy connection. Every real hit is decided as soon
     * as the hearts move, so this only delays the ding.
     */
    // Fixed window; a packet later than this is judged in allowHealthFlash (flash or ding, no hurt sound).
    static final int WINDOW_TICKS = 4;

    /** The hit waiting for a verdict, or null. One at a time: a second hit settles the first. */
    private static Pending pending;
    /** True while a held cue is replayed through vanilla, so the gate lets it pass. */
    private static boolean replaying;

    private static final class Pending {
        final LocalPlayer player;
        final DamageSource source;
        final float health, absorption;
        float lowestTotal;   // the least health+absorption seen while held, in case regen outraces the window
        Float hurtDir;   // set if the server sent which way the shake tilts
        int ticksLeft = WINDOW_TICKS;

        Pending(LocalPlayer player, DamageSource source) {
            this.player = player;
            this.source = source;
            this.health = player.getHealth();
            this.absorption = player.getAbsorptionAmount();
            this.lowestTotal = this.health + this.absorption;
        }
    }

    /**
     * The decision: did the heart bar visibly drop? It draws ceil(health) and
     * ceil(absorption) half hearts (Hud.renderHearts). Only a drop counts:
     * regeneration in the same moment is not a hit.
     */
    static boolean heartsMoved(float healthBefore, float absorptionBefore, float healthAfter, float absorptionAfter) {
        return Mth.ceil(healthAfter) < Mth.ceil(healthBefore)
                || Mth.ceil(absorptionAfter) < Mth.ceil(absorptionBefore);
    }

    /** Any health or absorption lost, however little. */
    static boolean healthLost(float healthBefore, float absorptionBefore, float healthAfter, float absorptionAfter) {
        return healthAfter + absorptionAfter < healthBefore + absorptionBefore;
    }

    /** Damage event for any living entity. True: it's the local player's, held until decided. */
    public static boolean holdDamageEvent(LivingEntity entity, DamageSource source) {
        if (replaying || !(entity instanceof LocalPlayer player)) return false;
        if (pending != null) settle(pending, true);
        pending = new Pending(player, source);
        return true;
    }

    /** Hurt direction for any player. True: it belongs to the held hit and waits with it. */
    public static boolean holdHurtDirection(LivingEntity entity, float yaw) {
        if (replaying || pending == null || pending.player != entity) return false;
        pending.hurtDir = yaw;
        return true;
    }

    /**
     * The new-health packet flashes the screen by itself when health drops (LocalPlayer.hurtTo).
     * With a hit held, the hit's own verdict decides instead; without one (a hit inside the
     * damage cooldown, which sends no damage event, or a health packet later than the
     * window), the drop is judged here: same rule, cues if a heart moved, else a ding.
     */
    public static boolean allowHealthFlash(LocalPlayer player, float healthBefore, float healthAfter) {
        if (pending != null && pending.player == player) return false;
        float absorption = player.getAbsorptionAmount();
        if (heartsMoved(healthBefore, absorption, healthAfter, absorption)) return true;
        if (healthLost(healthBefore, absorption, healthAfter, absorption)) ding();
        return false;
    }

    /** Every client tick of the local player. */
    public static void tick(LocalPlayer player) {
        Pending p = pending;
        if (p == null) return;
        if (p.player != player) { pending = null; return; }   // respawned or rejoined: the hit is moot
        float health = player.getHealth(), absorption = player.getAbsorptionAmount();
        p.lowestTotal = Math.min(p.lowestTotal, health + absorption);
        if (heartsMoved(p.health, p.absorption, health, absorption)) {
            settle(p, false);
        } else if (--p.ticksLeft <= 0) {
            settle(p, true);
        }
    }

    /**
     * Ends the held hit: replays its cues, or drops them and maybe dings. A ding is judged
     * against the lowest health+absorption seen while held, not the value at this instant:
     * regeneration can heal a tiny hit back within the window, and it still happened.
     */
    private static void settle(Pending p, boolean silenced) {
        pending = null;
        LocalPlayer player = p.player;
        float health = player.getHealth(), absorption = player.getAbsorptionAmount();
        if (!silenced || heartsMoved(p.health, p.absorption, health, absorption)) {
            replaying = true;
            try {
                player.handleDamageEvent(p.source);
                if (p.hurtDir != null) player.animateHurt(p.hurtDir);
            } finally {
                replaying = false;
            }
        } else if (p.lowestTotal < p.health + p.absorption) {
            // The hit landed but no heart moved. A shield block sends no damage
            // event, so it never reaches here.
            ding();
        }
    }

    private static void ding() {
        if (!Settings.ding) return;
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvent.createVariableRangeEvent(dingSound()), 1F, 0.6F));
    }

    /** The player's chosen sound, or the default when it's blank, malformed, or not a sound the game knows (a typo shouldn't mute the ding). */
    static Identifier dingSound() {
        Identifier id = Identifier.tryParse(Settings.sound.trim());
        return id != null && Minecraft.getInstance().getSoundManager().getSoundEvent(id) != null
                ? id : Identifier.parse(Settings.DEFAULT_SOUND);
    }

    private HonestDamage() {}
}
