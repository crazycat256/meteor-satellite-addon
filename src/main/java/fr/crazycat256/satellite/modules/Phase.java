/*
 * This file is part of Meteor Satellite Addon (https://github.com/crazycat256/meteor-satellite-addon).
 * Copyright (c) crazycat256.
 */

package fr.crazycat256.satellite.modules;

import fr.crazycat256.satellite.Addon;
import fr.crazycat256.satellite.utils.TPUtils;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.CollisionShapeEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

public class Phase extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("The mode of the phase.")
        .defaultValue(Mode.TP)
        .onChanged(v -> {
            if (isActive()) startPos = mc.player.getPos();
        })
        .build()
    );

    private final Setting<Double> horizontalSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("horizontal-speed")
        .description("The horizontal speed.")
        .defaultValue(1)
        .min(0)
        .sliderMax(10)
        .build()
    );

    private final Setting<Double> verticalSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("vertical-speed")
        .description("The vertical speed.")
        .defaultValue(1)
        .min(0)
        .sliderMax(10)
        .build()
    );

    private Vec3d startPos;

    public Phase() {
        super(Addon.CATEGORY, "Phase", "Allows you to fly trough blocks.");
    }

    @Override
    public void onActivate() {
        startPos = mc.player.getPos();
    }


    @Override
    public void onDeactivate() {
        if (mc.player != null && mode.get() == Mode.TP) {
            TPUtils.PaperTP(startPos, mc.player.getPos());
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.cameraEntity == null) return;

        double forwardX = mc.player.getRotationVector().x;
        double forwardZ = mc.player.getRotationVector().z;
        Vec3d forward = new Vec3d(forwardX, 0, forwardZ).normalize();

        double x = 0;
        double y = 0;
        double z = 0;

        double hs = horizontalSpeed.get() / 10;
        double vs = verticalSpeed.get() / 10;

        if (mc.options.sprintKey.isPressed()) {
            hs *= 3;
            vs *= 3;
        }

        if (mc.options.forwardKey.isPressed()) {
            x += forward.x * hs;
            z += forward.z * hs;
        }
        if (mc.options.backKey.isPressed()) {
            x -= forward.x * hs;
            z -= forward.z * hs;

        }
        if (mc.options.leftKey.isPressed()) {
            x += forward.z * hs;
            z -= forward.x * hs;
        }
        if (mc.options.rightKey.isPressed()) {
            x -= forward.z * hs;
            z += forward.x * hs;

        }
        if (mc.options.jumpKey.isPressed()) {
            y += vs;
        }
        if (mc.options.sneakKey.isPressed()) {
            y -= vs;
            mc.player.setSneaking(false);
        }

        mc.player.setVelocity(x, y, z);

    }


    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (mc.player == null) return;
        if (event.packet instanceof PlayerMoveC2SPacket && mode.get() == Mode.TP) {
            event.cancel();
        }
    }

    @EventHandler
    private void onCollisionShape(CollisionShapeEvent event) {
        event.cancel();
    }

    @SuppressWarnings("unused")
    public enum Mode {
        NoClip,
        TP
    }
}
