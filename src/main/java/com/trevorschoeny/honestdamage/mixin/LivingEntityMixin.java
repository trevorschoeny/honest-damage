package com.trevorschoeny.honestdamage.mixin;

import com.trevorschoeny.honestdamage.HonestDamage;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Holds the local player's damage event (hurt sound, flash, shake) until the hit is judged. */
@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
    @Inject(method = "handleDamageEvent", at = @At("HEAD"), cancellable = true)
    private void honestdamage$hold(DamageSource source, CallbackInfo ci) {
        if (HonestDamage.holdDamageEvent((LivingEntity) (Object) this, source)) ci.cancel();
    }
}
