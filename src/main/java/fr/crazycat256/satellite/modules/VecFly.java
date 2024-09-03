/*
 * This file is part of Meteor Satellite Addon (https://github.com/crazycat256/meteor-satellite-addon).
 * Copyright (c) crazycat256.
 */

package fr.crazycat256.satellite.modules;

import fr.crazycat256.satellite.Addon;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.PlayerMoveC2SPacketAccessor;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;


/**
 * This module was originally made to bypass <a href="https://github.com/KRYMZ0N/AnarchyAnticheat">this anticheat</a>
 */
public class VecFly extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> flySpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("fly-speed")
        .description("The speed to fly at.")
        .defaultValue(0.35)
        .min(0)
        .sliderMax(1)
        .build()
    );

    private final Setting<Double> yWeight = sgGeneral.add(new DoubleSetting.Builder()
        .name("y-weight")
        .description("The weight of the y axis.")
        .defaultValue(0.5)
        .min(0)
        .sliderMax(1)
        .build()
    );

    private final Setting<SpeedBypassMode> speedBypassMode = sgGeneral.add(new EnumSetting.Builder<SpeedBypassMode>()
        .name("speed-bypass-mode")
        .description("The mode to use when speed bypass is enabled.")
        .defaultValue(SpeedBypassMode.OnSprint)
        .build()
    );

    private final Setting<Double> speedBypassMultiplier = sgGeneral.add(new DoubleSetting.Builder()
        .name("speed-bypass-multiplier")
        .description("The speed multiplier when speed bypass is enabled.")
        .defaultValue(10)
        .min(0)
        .sliderMax(100)
        .visible(() -> speedBypassMode.get() != SpeedBypassMode.None)
        .build()
    );

    private final Setting<Integer> stepDownDelay = sgGeneral.add(new IntSetting.Builder()
        .name("step-down-delay")
        .description("The delay in ticks between each step down.")
        .defaultValue(20)
        .min(0)
        .sliderMax(20)
        .visible(() -> speedBypassMode.get() != SpeedBypassMode.None)
        .build()
    );

    public VecFly() {
        super(Addon.CATEGORY, "vec-fly", "Allows you to fly at a precise speed.");
    }

    private int ticks = 0;
    private boolean bypassLastTick = false;
    private Vec3d lastForward = Vec3d.ZERO;

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        Vec3d dir = Vec3d.fromPolar(0, mc.player.getYaw()).normalize();

        double x = 0;
        double y = 0;
        double z = 0;

        if (mc.options.forwardKey.isPressed()) {
            x += dir.x;
            z += dir.z;
        }
        if (mc.options.backKey.isPressed()) {
            x += -dir.x;
            z += -dir.z;
        }
        if (mc.options.rightKey.isPressed()) {
            x += -dir.z;
            z += dir.x;
        }
        if (mc.options.leftKey.isPressed()) {
            x += dir.z;
            z += -dir.x;
        }
        if (mc.options.jumpKey.isPressed()) {
            y += yWeight.get();
        }
        if (mc.options.sneakKey.isPressed()) {
            y -= yWeight.get();
        }

        Vec3d forward;
        if (x != 0 || y != 0 || z != 0) {
            forward = new Vec3d(x, y, z).normalize().multiply(flySpeed.get());
        } else {
            forward = new Vec3d(0, 0, 0);
        }

        BlockPos stepDownPos = mc.player.getBlockPos().down();
        boolean sprinting = mc.options.sprintKey.isPressed() && !mc.options.jumpKey.isPressed() && speedBypassMode.get() != SpeedBypassMode.None;
        if (sprinting != bypassLastTick && !mc.player.isOnGround() && !mc.world.getBlockState(stepDownPos).isSolidBlock(mc.world, stepDownPos)) {
            mc.player.updatePosition(mc.player.getX(), stepDownPos.getY(), mc.player.getZ());
        }
        bypassLastTick = sprinting;
        mc.player.setVelocity(forward);

        Vec3d tempLastForward = lastForward;
        lastForward = forward;

        if (speedBypassMode.get() == SpeedBypassMode.None) return;
        if (speedBypassMode.get() == SpeedBypassMode.OnSprint && !mc.options.sprintKey.isPressed()) return;
        if (forward.equals(Vec3d.ZERO) && tempLastForward.equals(Vec3d.ZERO)) return;
        if (mc.options.jumpKey.isPressed()) return;


        if (!mc.world.getBlockState(stepDownPos).isSolidBlock(mc.world, stepDownPos)) {
            forward = forward.multiply(speedBypassMultiplier.get());
            if (++ticks >= stepDownDelay.get()) {
                mc.player.updatePosition(mc.player.getX(), stepDownPos.getY(), mc.player.getZ());
                ticks = 0;
            }
        }
        mc.player.setVelocity(forward);

    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (event.packet instanceof PlayerMoveC2SPacket packet) {
            if (!packet.isOnGround()) {
                ((PlayerMoveC2SPacketAccessor) packet).setOnGround(true);
            }
        }
    }

    @SuppressWarnings("unused")
    public enum SpeedBypassMode {
        OnSprint,
        OnMove,
        None
    }
}
