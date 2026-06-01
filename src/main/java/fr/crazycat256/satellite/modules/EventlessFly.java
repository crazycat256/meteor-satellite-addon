/*
 * This file is part of Meteor Satellite Addon (https://github.com/crazycat256/meteor-satellite-addon).
 * Copyright (c) crazycat256.
 */

package fr.crazycat256.satellite.modules;

import fr.crazycat256.satellite.Addon;
import fr.crazycat256.satellite.utils.ServerUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;

public class EventlessFly extends Module{

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAntiKick = settings.createGroup("Anti Kick");

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
        .name("Speed")
        .description("Your speed when flying.")
        .defaultValue(0.062)
        .min(0.0)
        .sliderMax(1.0)
        .build()
    );

    private final Setting<Boolean> cancelRotations = sgGeneral.add(new BoolSetting.Builder()
        .name("cancel-rotations")
        .description("Cancels rotations when flying.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> resetRotationOnDisable = sgGeneral.add(new BoolSetting.Builder()
        .name("reset-rotation-on-disable")
        .description("Resets your rotation when you disable eventless-fly.")
        .defaultValue(false)
        .visible(cancelRotations::get)
        .build()
    );

    private final Setting<Boolean> antiKick = sgAntiKick.add(new BoolSetting.Builder()
        .name("anti-kick")
        .description("Tries to prevent you from getting kicked for flying.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> delay = sgAntiKick.add(new IntSetting.Builder()
        .name("delay")
        .description("The amount of delay, in ticks, between flying down a bit and return to original position.")
        .defaultValue(20)
        .min(1)
        .sliderMax(200)
        .visible(antiKick::get)
        .build()
    );

    private final Setting<Integer> offTime = sgAntiKick.add(new IntSetting.Builder()
        .name("off-time")
        .description("The amount of delay, in milliseconds, to fly down a bit to reset floating ticks.")
        .defaultValue(3)
        .min(1)
        .sliderRange(1, 20)
        .visible(antiKick::get)
        .build()
    );

    private final Setting<Boolean> flyUp = sgAntiKick.add(new BoolSetting.Builder()
        .name("fly-up")
        .description("Fly up a bit after flying down.")
        .defaultValue(true)
        .visible(antiKick::get)
        .build()
    );

    public float cameraYaw;
    public float cameraPitch;


    private float startPitch;
    private float startYaw ;
    private int ticks = 0;
    private boolean shouldFlyDown = false;
    private boolean shouldFlyUp = false;

    public EventlessFly() {
        super(Addon.CATEGORY, "eventless-fly", "Allows you to fly without triggering events.");
    }


    public boolean noRotate() {
        return isActive() && cancelRotations.get();
    }

    @Override
    public void onActivate() {
        startYaw = mc.player.getYRot();
        startPitch = mc.player.getXRot();
        cameraYaw = mc.player.getYRot();
        cameraPitch = mc.player.getXRot();
    }
    @Override
    public void onDeactivate() {
        if (cancelRotations.get() && resetRotationOnDisable.get()) {
            mc.player.setYRot(startYaw);
            mc.player.setXRot(startPitch);
        } else if (!cancelRotations.get()){
            mc.player.setYRot(cameraYaw);
            mc.player.setXRot(cameraPitch);
            mc.player.connection.send(new ServerboundMovePlayerPacket.Rot(cameraYaw, cameraPitch, mc.player.onGround(), mc.player.horizontalCollision));
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.level == null) return;

        ticks++;

        if (cancelRotations.get()) {
            Rotations.rotate(startYaw, startPitch);
            mc.player.setYRot(cameraYaw);
            mc.player.setXRot(cameraPitch);
        }

        if (!ServerUtils.noBlocksAround(mc.player)) {
            ticks = 0;
            shouldFlyDown = false;
            shouldFlyUp = false;
        }
        if (ticks >= delay.get()) shouldFlyDown = true;
        if (ticks > delay.get() + offTime.get()) {
            ticks = 0;
            shouldFlyDown = false;
            shouldFlyUp = flyUp.get();
        }
        if (ticks >= offTime.get() + 1) shouldFlyUp = false;

        double x = 0;
        double y = 0;
        double z = 0;

        if (shouldFlyDown && antiKick.get()) {
            y = -1;
        } else if (shouldFlyUp && antiKick.get() && flyUp.get()) {
            y = 1;
        } else {
            Vec3 dir = Vec3.directionFromRotation(0, mc.player.getYRot()).normalize();

            if (mc.options.keyUp.isDown()) {
                x += dir.x;
                z += dir.z;
            }
            if (mc.options.keyDown.isDown()) {
                x -= dir.x;
                z -= dir.z;
            }
            if (mc.options.keyRight.isDown()) {
                x -= dir.z;
                z += dir.x;
            }
            if (mc.options.keyLeft.isDown()) {
                x += dir.z;
                z -= dir.x;
            }
            if (mc.options.keyJump.isDown()) {
                y += 1;
            }
            if (mc.options.keyShift.isDown()) {
                y -= 1;
                mc.player.setShiftKeyDown(false);
                ticks = 0;
            }
            if (mc.options.keySprint.isDown()) {
                mc.player.setSprinting(false);
            }
        }



        Vec3 velocity = new Vec3(x, y, z);

        if (velocity.equals(Vec3.ZERO)) {
            mc.player.setDeltaMovement(0, 0, 0);
            return;
        }

        velocity = velocity.normalize().scale(speed.get());
        mc.player.setDeltaMovement(velocity);
        Vec3 endPos = mc.player.position().add(velocity);

        mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(endPos.x, endPos.y, endPos.z, mc.player.onGround(), mc.player.horizontalCollision));
        mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(endPos.x, -1E6, endPos.z, false, mc.player.horizontalCollision));
    }
}
