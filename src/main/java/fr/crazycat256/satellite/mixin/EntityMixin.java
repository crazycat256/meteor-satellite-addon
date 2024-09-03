/*
 * This file is part of Meteor Satellite Addon (https://github.com/crazycat256/meteor-satellite-addon).
 * Copyright (c) crazycat256.
 */

package fr.crazycat256.satellite.mixin;

import fr.crazycat256.satellite.modules.EventlessFly;
import fr.crazycat256.satellite.modules.VecFly;
import fr.crazycat256.satellite.utils.MathUtils;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "isSprinting", at = @At("HEAD"), cancellable = true)
    private void onIsSprinting(CallbackInfoReturnable<Boolean> info) {
        if (Modules.get().get(VecFly.class).isActive()) info.setReturnValue(false);
    }

    @Inject(method = "changeLookDirection", at = @At("HEAD"))
    private void onChangeLookDirection(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        EventlessFly eventlessFly = Modules.get().get(EventlessFly.class);

        if (eventlessFly.noRotate()){
            eventlessFly.cameraYaw += (float) (cursorDeltaX / 8);
            eventlessFly.cameraYaw = (float) MathUtils.mod(eventlessFly.cameraYaw + 180, 360) - 180;
            eventlessFly.cameraPitch += (float) (cursorDeltaY / 8);
            eventlessFly.cameraPitch = MathHelper.clamp(eventlessFly.cameraPitch, -90, 90);
        }
    }
}

