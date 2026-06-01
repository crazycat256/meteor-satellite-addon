/*
 * This file is part of Meteor Satellite Addon (https://github.com/crazycat256/meteor-satellite-addon).
 * Copyright (c) crazycat256.
 */

package fr.crazycat256.satellite.modules;

import fr.crazycat256.satellite.Addon;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.ServerboundMovePlayerPacketAccessor;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;


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
    private Vec3 lastForward = Vec3.ZERO;

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.level == null) return;

        Vec3 dir = Vec3.directionFromRotation(0, mc.player.getYRot()).normalize();

        double x = 0;
        double y = 0;
        double z = 0;

        if (mc.options.keyUp.isDown()) {
            x += dir.x;
            z += dir.z;
        }
        if (mc.options.keyDown.isDown()) {
            x += -dir.x;
            z += -dir.z;
        }
        if (mc.options.keyRight.isDown()) {
            x += -dir.z;
            z += dir.x;
        }
        if (mc.options.keyLeft.isDown()) {
            x += dir.z;
            z += -dir.x;
        }
        if (mc.options.keyJump.isDown()) {
            y += yWeight.get();
        }
        if (mc.options.keyShift.isDown()) {
            y -= yWeight.get();
        }

        Vec3 forward;
        if (x != 0 || y != 0 || z != 0) {
            forward = new Vec3(x, y, z).normalize().scale(flySpeed.get());
        } else {
            forward = new Vec3(0, 0, 0);
        }

        BlockPos stepDownPos = mc.player.blockPosition().below();
        boolean sprinting = mc.options.keySprint.isDown() && !mc.options.keyJump.isDown() && speedBypassMode.get() != SpeedBypassMode.None;
        if (sprinting != bypassLastTick && !mc.player.onGround() && !mc.level.getBlockState(stepDownPos).isSolidRender()) {
            mc.player.setPos(mc.player.getX(), stepDownPos.getY(), mc.player.getZ());
        }
        bypassLastTick = sprinting;
        mc.player.setDeltaMovement(forward);

        Vec3 tempLastForward = lastForward;
        lastForward = forward;

        if (speedBypassMode.get() == SpeedBypassMode.None) return;
        if (speedBypassMode.get() == SpeedBypassMode.OnSprint && !mc.options.keySprint.isDown()) return;
        if (forward.equals(Vec3.ZERO) && tempLastForward.equals(Vec3.ZERO)) return;
        if (mc.options.keyJump.isDown()) return;


        if (!mc.level.getBlockState(stepDownPos).isSolidRender()) {
            forward = forward.scale(speedBypassMultiplier.get());
            if (++ticks >= stepDownDelay.get()) {
                mc.player.setPos(mc.player.getX(), stepDownPos.getY(), mc.player.getZ());
                ticks = 0;
            }
        }
        mc.player.setDeltaMovement(forward);

    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (event.packet instanceof ServerboundMovePlayerPacket packet) {
            if (!packet.isOnGround()) {
                ((ServerboundMovePlayerPacketAccessor) packet).meteor$setOnGround(true);
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
