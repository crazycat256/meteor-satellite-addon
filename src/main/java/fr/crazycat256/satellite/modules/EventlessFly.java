/*
 * This file is part of Meteor Satellite Addon (https://github.com/crazycat256/meteor-satellite-addon).
 * Copyright (c) crazycat256.
 */

package fr.crazycat256.satellite.modules;

import fr.crazycat256.satellite.Addon;
import fr.crazycat256.satellite.mixin.PlayerPositionLookS2CPacketAccessor;
import fr.crazycat256.satellite.utils.ServerUtils;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Vec3d;

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
        startYaw = mc.player.getYaw();
        startPitch = mc.player.getPitch();
        cameraYaw = mc.player.getYaw();
        cameraPitch = mc.player.getPitch();
    }
    @Override
    public void onDeactivate() {
        if (cancelRotations.get() && resetRotationOnDisable.get()) {
            mc.player.setYaw(startYaw);
            mc.player.setPitch(startPitch);
        } else if (!cancelRotations.get()){
            mc.player.setYaw(cameraYaw);
            mc.player.setPitch(cameraPitch);
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(cameraYaw, cameraPitch, mc.player.isOnGround()));
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        ticks++;

        if (cancelRotations.get()) {
            Rotations.rotate(startYaw, startPitch);
            mc.player.setYaw(cameraYaw);
            mc.player.setPitch(cameraPitch);
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
            Vec3d dir = Vec3d.fromPolar(0, mc.player.getYaw()).normalize();

            if (mc.options.forwardKey.isPressed()) {
                x += dir.x;
                z += dir.z;
            }
            if (mc.options.backKey.isPressed()) {
                x -= dir.x;
                z -= dir.z;
            }
            if (mc.options.rightKey.isPressed()) {
                x -= dir.z;
                z += dir.x;
            }
            if (mc.options.leftKey.isPressed()) {
                x += dir.z;
                z -= dir.x;
            }
            if (mc.options.jumpKey.isPressed()) {
                y += 1;
            }
            if (mc.options.sneakKey.isPressed()) {
                y -= 1;
                mc.player.setSneaking(false);
                ticks = 0;
            }
            if (mc.options.sprintKey.isPressed()) {
                mc.player.setSprinting(false);
            }
        }



        Vec3d velocity = new Vec3d(x, y, z);

        if (velocity.equals(Vec3d.ZERO)) {
            mc.player.setVelocity(0, 0, 0);
            return;
        }

        velocity = velocity.normalize().multiply(speed.get());
        mc.player.setVelocity(velocity);
        Vec3d endPos = mc.player.getPos().add(velocity);

        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(endPos.x, endPos.y, endPos.z, mc.player.isOnGround()));
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(endPos.x, -1E6, endPos.z, false));
    }



    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (mc.player == null || !cancelRotations.get()) return;

        if (event.packet instanceof PlayerPositionLookS2CPacket packet) {

            ((PlayerPositionLookS2CPacketAccessor) packet).setPitch(mc.player.getPitch());
            ((PlayerPositionLookS2CPacketAccessor) packet).setYaw(mc.player.getYaw());
        }
    }
}
