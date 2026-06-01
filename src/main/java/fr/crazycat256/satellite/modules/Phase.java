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
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;

public class Phase extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("The mode of the phase.")
        .defaultValue(Mode.TP)
        .onChanged(v -> {
            if (isActive()) startPos = mc.player.position();
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

    private Vec3 startPos;

    public Phase() {
        super(Addon.CATEGORY, "Phase", "Allows you to fly trough blocks.");
    }

    @Override
    public void onActivate() {
        startPos = mc.player.position();
    }


    @Override
    public void onDeactivate() {
        if (mc.player != null && mode.get() == Mode.TP) {
            TPUtils.PaperTP(startPos, mc.player.position());
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.getCameraEntity() == null) return;

        double forwardX = mc.player.getLookAngle().x;
        double forwardZ = mc.player.getLookAngle().z;
        Vec3 forward = new Vec3(forwardX, 0, forwardZ).normalize();

        double x = 0;
        double y = 0;
        double z = 0;

        double hs = horizontalSpeed.get() / 10;
        double vs = verticalSpeed.get() / 10;

        if (mc.options.keySprint.isDown()) {
            hs *= 3;
            vs *= 3;
        }

        if (mc.options.keyUp.isDown()) {
            x += forward.x * hs;
            z += forward.z * hs;
        }
        if (mc.options.keyDown.isDown()) {
            x -= forward.x * hs;
            z -= forward.z * hs;

        }
        if (mc.options.keyLeft.isDown()) {
            x += forward.z * hs;
            z -= forward.x * hs;
        }
        if (mc.options.keyRight.isDown()) {
            x -= forward.z * hs;
            z += forward.x * hs;

        }
        if (mc.options.keyJump.isDown()) {
            y += vs;
        }
        if (mc.options.keyShift.isDown()) {
            y -= vs;
            mc.player.setShiftKeyDown(false);
        }

        mc.player.setDeltaMovement(x, y, z);

    }


    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (mc.player == null) return;
        if (event.packet instanceof ServerboundMovePlayerPacket && mode.get() == Mode.TP) {
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
