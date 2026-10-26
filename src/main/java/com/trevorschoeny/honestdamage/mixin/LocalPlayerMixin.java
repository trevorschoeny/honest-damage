package com.trevorschoeny.honestdamage.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.trevorschoeny.honestdamage.HonestDamage;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
abstract class LocalPlayerMixin {
    /** Health when the new-health packet arrived; hurtTo has already set the new one when it flashes. */
    @Unique private float honestdamage$healthBefore;

    @Inject(method = "hurtTo", at = @At("HEAD"))
    private void honestdamage$rememberHealth(float newHealth, CallbackInfo ci) {
        honestdamage$healthBefore = ((LocalPlayer) (Object) this).getHealth();
    }

    /** hurtTo's own flash on a health drop: only if the drop counts as a real hit. */
    @WrapWithCondition(method = "hurtTo", at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/player/LocalPlayer;hurtTime:I", opcode = org.objectweb.asm.Opcodes.PUTFIELD))
    private boolean honestdamage$gateFlash(LocalPlayer self, int hurtTime, float newHealth) {
        return HonestDamage.allowHealthFlash(self, honestdamage$healthBefore, newHealth);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void honestdamage$tick(CallbackInfo ci) {
        HonestDamage.tick((LocalPlayer) (Object) this);
    }
}
