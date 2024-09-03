/*
 * This file is part of Meteor Satellite Addon (https://github.com/crazycat256/meteor-satellite-addon).
 * Copyright (c) crazycat256.
 */

package fr.crazycat256.satellite.mixin;

import fr.crazycat256.satellite.modules.EventlessFly;
import meteordevelopment.meteorclient.mixininterface.ICamera;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.FreeLook;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.systems.modules.world.HighwayBuilder;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(Camera.class)
public abstract class CameraMixin implements ICamera {

    @ModifyArgs(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V"))
    private void onUpdateSetRotationArgs(Args args) {
        Freecam freecam = Modules.get().get(Freecam.class);
        FreeLook freeLook = Modules.get().get(FreeLook.class);
        HighwayBuilder highwayBuilder = Modules.get().get(HighwayBuilder.class);
        EventlessFly eventlessFly = Modules.get().get(EventlessFly.class);

        if (eventlessFly.noRotate() && !freecam.isActive() && !highwayBuilder.isActive() && !freeLook.isActive()) {
            if (mc.options.getPerspective() != Perspective.THIRD_PERSON_FRONT) {
                args.set(0, eventlessFly.cameraYaw);

            } else {
                args.set(0, eventlessFly.cameraYaw + 180);
            }
            args.set(1, eventlessFly.cameraPitch);
        }
    }
}
