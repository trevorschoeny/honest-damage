package com.trevorschoeny.honestdamage.mixin;

import com.trevorschoeny.honestdamage.HonestDamage;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Holds the hurt direction (which way the screen shake tilts) with the hit it belongs to. */
@Mixin(Player.class)
abstract class PlayerMixin {
    @Inject(method = "animateHurt", at = @At("HEAD"), cancellable = true)
    private void honestdamage$hold(float yaw, CallbackInfo ci) {
        if (HonestDamage.holdHurtDirection((Player) (Object) this, yaw)) ci.cancel();
    }
}
