package com.trevorschoeny.honestdamage.checks;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import testgallery.SceneWorld;

/**
 * Checks for the core scene (test-gallery/scenes/honest-damage/core.json). The player
 * arrives in netherite with Protection IV and Feather Falling IV boots.
 *
 * <p>Each check hits the player with the station's own damage command and watches the
 * client: hurtTime (the red flash and the shake both run on it) and the sounds it plays.
 */
public class HitChecks implements FabricClientGameTest {
    private static final String HURT = "minecraft:entity.player.hurt";
    private static final String DING = "minecraft:block.note_block.bell";

    @Override
    public void runTest(ClientGameTestContext ctx) {
        List<String> heard = new CopyOnWriteArrayList<>();
        try (SceneWorld.Opened scene = SceneWorld.open(ctx, "honest-damage", "core")) {
            ctx.runOnClient(c -> c.getSoundManager().addListener(
                    (sound, events, range) -> heard.add(sound.getIdentifier().toString())));

            // Station "honestdamage:armor_save": 2 damage, armor leaves ~0.16 of it. The hearts
            // don't move, so no cues; the armor lost durability, so a ding.
            Hit save = hit(ctx, scene, heard, "honestdamage:armor_save", "damage @p 2 minecraft:mob_attack");
            save.expect(save.health < 20, "the light hit should land (health below 20)");
            save.expect(save.maxHurtTime == 0, "the light hit should not flash");
            save.expect(!save.sounds.contains(HURT), "the light hit should not play the hurt sound");
            save.expect(save.sounds.contains(DING), "the light hit should ding");

            // Station "honestdamage:real_hit": 20 damage gets ~2.6 through, the hearts drop.
            Hit real = hit(ctx, scene, heard, "honestdamage:real_hit", "damage @p 20 minecraft:mob_attack");
            real.expect(real.maxHurtTime > 0, "the heavy hit should flash");
            real.expect(real.sounds.contains(HURT), "the heavy hit should play the hurt sound");
            real.expect(!real.sounds.contains(DING), "the heavy hit should not ding");

            // Station "honestdamage:feather_fall": a 4.5-block drop, Feather Falling leaves under a half heart.
            JsonArray o = scene.station("honestdamage:feather_fall").getAsJsonArray("origin");
            String drop = "tp @p " + (o.get(0).getAsInt() + 0.5) + " " + (o.get(1).getAsInt() + 4.5) + " " + (o.get(2).getAsInt() + 1.5);
            Hit fall = hit(ctx, scene, heard, "honestdamage:feather_fall", drop);
            fall.expect(fall.health < 20, "the fall should land (health below 20)");
            fall.expect(fall.maxHurtTime == 0, "the cushioned fall should not flash");
            fall.expect(!fall.sounds.contains(HURT), "the cushioned fall should not play the hurt sound");
            fall.expect(!fall.sounds.contains(DING), "fall damage doesn't wear armor, so no ding");
        }
    }

    /** What the client showed for one hit. */
    private record Hit(String station, float health, int maxHurtTime, List<String> sounds) {
        void expect(boolean ok, String what) {
            if (!ok) throw new AssertionError(station + ": " + what + "; health " + health
                    + ", max hurtTime " + maxHurtTime + ", sounds " + sounds);
        }
    }

    /** Full health, out of the damage cooldown, at the station; then the hit, watched for 40 ticks. */
    private static Hit hit(ClientGameTestContext ctx, SceneWorld.Opened scene, List<String> heard, String check, String command) {
        JsonObject station = scene.station(check);
        scene.standAt(station, 0);
        scene.world().getServer().runCommand("effect give @p minecraft:instant_health 1 4 true");
        ctx.waitTicks(30);
        heard.clear();
        scene.world().getServer().runCommand(command);
        int maxHurt = 0;
        float health = 20;   // the lowest seen: natural regeneration soon heals a light hit back
        for (int t = 0; t < 40; t++) {
            ctx.waitTicks(1);
            maxHurt = Math.max(maxHurt, ctx.computeOnClient(c -> c.player.hurtTime));
            health = Math.min(health, ctx.computeOnClient(c -> c.player.getHealth()));
        }
        return new Hit(check, health, maxHurt, List.copyOf(heard));
    }
}
