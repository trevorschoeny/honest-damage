package com.trevorschoeny.honestdamage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * The gate between a hit reaching the local player and its hurt cues.
 *
 * <p>A server tells the client about a hit with up to three packets, in this order:
 * the damage event (hurt sound, red flash, and the flash's screen shake), the hurt
 * direction (which way the shake tilts), and, if health changed, the new health. A hit
 * the armor fully absorbed sends no health packet at all. So the cues can't be judged
 * when they arrive: the hit is held here as pending, and each client tick compares the
 * player's hearts with how they were when it arrived. If the hit hurt, the held cues
 * play through vanilla's own code; if nothing moved by the end of the window, they are
 * dropped, and a ding plays when an armor piece lost durability.
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
    // ponytail: fixed window; a packet later than this plays its flash from hurtTo, but no sound.
    static final int WINDOW_TICKS = 4;

    private static final EquipmentSlot[] ARMOR = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    /** The hit waiting for a verdict, or null. One at a time: a second hit settles the first. */
    private static Pending pending;
    /** True while a held cue is replayed through vanilla, so the gate lets it pass. */
    private static boolean replaying;

    private static final class Pending {
        final LocalPlayer player;
        final DamageSource source;
        final float health, absorption;
        final int[] armorDamage;
        Float hurtDir;   // set if the server sent which way the shake tilts
        int ticksLeft = WINDOW_TICKS;

        Pending(LocalPlayer player, DamageSource source) {
            this.player = player;
            this.source = source;
            this.health = player.getHealth();
            this.absorption = player.getAbsorptionAmount();
            this.armorDamage = armorDamage(player);
        }
    }

    /**
     * The decision. Single function on purpose: per-source thresholds, if they come,
     * would add the source as an argument here.
     */
    static boolean hurt(Settings.Threshold threshold, float healthBefore, float absorptionBefore,
                        float healthAfter, float absorptionAfter) {
        return switch (threshold) {
            case DAMAGE_TAKEN -> healthAfter + absorptionAfter < healthBefore + absorptionBefore;
            // The heart bar draws ceil(health) and ceil(absorption) half hearts (Hud.renderHearts).
            // Only a drop counts: regeneration in the same moment is not a hit.
            case HEARTS_MOVED -> Mth.ceil(healthAfter) < Mth.ceil(healthBefore)
                    || Mth.ceil(absorptionAfter) < Mth.ceil(absorptionBefore);
        };
    }

    /** Damage event for any living entity. True: it's the local player's, held until decided. */
    public static boolean holdDamageEvent(LivingEntity entity, DamageSource source) {
        if (replaying || !Settings.silencing || !(entity instanceof LocalPlayer player)) return false;
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
     * damage cooldown, which sends no damage event), the drop is judged here.
     */
    public static boolean allowHealthFlash(LocalPlayer player, float healthBefore, float healthAfter) {
        if (!Settings.silencing) return true;
        if (pending != null && pending.player == player) return false;
        float absorption = player.getAbsorptionAmount();
        return hurt(Settings.threshold, healthBefore, absorption, healthAfter, absorption);
    }

    /** Every client tick of the local player. */
    public static void tick(LocalPlayer player) {
        Pending p = pending;
        if (p == null) return;
        if (p.player != player) { pending = null; return; }   // respawned or rejoined: the hit is moot
        if (hurt(Settings.threshold, p.health, p.absorption, player.getHealth(), player.getAbsorptionAmount())) {
            settle(p, false);
        } else if (--p.ticksLeft <= 0) {
            settle(p, true);
        }
    }

    /** Ends the held hit: replays its cues, or drops them and maybe dings. */
    private static void settle(Pending p, boolean silenced) {
        pending = null;
        LocalPlayer player = p.player;
        if (!silenced || hurt(Settings.threshold, p.health, p.absorption, player.getHealth(), player.getAbsorptionAmount())) {
            replaying = true;
            try {
                player.handleDamageEvent(p.source);
                if (p.hurtDir != null) player.animateHurt(p.hurtDir);
            } finally {
                replaying = false;
            }
        } else if (Settings.armorDing && armorTookIt(p.armorDamage, armorDamage(player))) {
            // One ding per hit, however many pieces wore down. A shield block sends no
            // damage event, so it never reaches here.
            Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BELL.value(), 1.6F, 0.5F));
        }
    }

    /** Damage value per armor slot, or -1 for a slot with nothing that wears down. */
    private static int[] armorDamage(LivingEntity entity) {
        int[] d = new int[ARMOR.length];
        for (int i = 0; i < ARMOR.length; i++) {
            ItemStack s = entity.getItemBySlot(ARMOR[i]);
            d[i] = s.isDamageableItem() ? s.getDamageValue() : -1;
        }
        return d;
    }

    /** A piece lost durability, or broke (it wore down to nothing and left the slot). */
    static boolean armorTookIt(int[] before, int[] after) {
        for (int i = 0; i < before.length; i++)
            if (before[i] >= 0 && (after[i] > before[i] || after[i] < 0)) return true;
        return false;
    }

    private HonestDamage() {}
}
