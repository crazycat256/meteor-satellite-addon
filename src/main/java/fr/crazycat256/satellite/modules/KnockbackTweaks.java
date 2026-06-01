/*
 * This file is part of Meteor Satellite Addon (https://github.com/crazycat256/meteor-satellite-addon).
 * Copyright (c) crazycat256.
 */

package fr.crazycat256.satellite.modules;

import fr.crazycat256.satellite.Addon;
import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;

public class KnockbackTweaks extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");


    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("The mode of the knockback direction.")
        .defaultValue(Mode.RelativeToPlayer)
        .build()
    );
    private final Setting<Integer> direction = sgGeneral.add(new IntSetting.Builder()
        .name("direction")
        .description("The direction of the knockback.")
        .defaultValue(0)
        .range(-180, 180)
        .sliderRange(-180, 180)
        .build()
    );

    private final Setting<Integer> randomRange = sgGeneral.add(new IntSetting.Builder()
        .name("random")
        .description("The random range of the knockback.")
        .defaultValue(0)
        .min(0)
        .sliderMax(90)
        .max(180)
        .build()
    );

    private final Setting<Boolean> fakeSprint = sgGeneral.add(new BoolSetting.Builder()
        .name("fake-sprint")
        .description("Make you sprint when you hit an entity.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> renderDirection = sgRender.add(new BoolSetting.Builder()
        .name("render-direction")
        .description("Display the direction range.")
        .defaultValue(false)
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The color of the line.")
        .defaultValue(new SettingColor(255, 128, 0, 255))
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The color of the side.")
        .defaultValue(new SettingColor(255, 128, 0, 64))
        .build()
    );

    private final Setting<Double> distance = sgRender.add(new DoubleSetting.Builder()
        .name("distance")
        .description("The distance of the direction range.")
        .defaultValue(3)
        .min(0)
        .sliderMax(5)
        .build()
    );

    private final Setting<Double> radius = sgRender.add(new DoubleSetting.Builder()
        .name("radius")
        .description("The radius of the direction range.")
        .defaultValue(3)
        .min(0)
        .sliderMax(5)
        .build()
    );

    private boolean shouldResendLookPacket = false;
    private boolean shouldResendSprintPacket = false;

    public KnockbackTweaks() {
        super(Addon.CATEGORY, "knockback-tweaks", "Allows you to change the direction of the knockback received by the entities you hit. (doesn't work with criticals)");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.level == null) return;
        if (shouldResendLookPacket) {
            shouldResendLookPacket = false;
            mc.player.connection.send(new ServerboundMovePlayerPacket.Rot(mc.player.getYRot(), mc.player.getXRot(), mc.player.onGround(), mc.player.horizontalCollision));
        }
        if (shouldResendSprintPacket) {
            shouldResendSprintPacket = false;
            mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
        }
    }

    @EventHandler
    private void onEntityAttack(AttackEntityEvent event) {
        if (mc.player == null || mc.level == null) return;
        if (fakeSprint.get()) {
            if (mc.player.isSprinting())
                mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
            mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
            if (mc.player.isSprinting()) shouldResendSprintPacket = true;
        }

        float yaw;
        if (mode.get() == Mode.Absolute) {
            yaw = direction.get();
        } else if (mode.get() == Mode.RelativeToView) {
            yaw = direction.get() + mc.player.getYRot();
        } else if (mode.get() == Mode.RelativeToPlayer) {
            Vec3 playerPos = mc.player.position();
            Vec3 entityPos = event.entity.position();
            Vec3 diff = entityPos.subtract(playerPos);

            yaw = (float) Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90 + direction.get();

        } else {
            return;
        }

        if (randomRange.get() != 0) {
            int range = randomRange.get() * 2 + 1;
            yaw += (float) (Math.random() * range - randomRange.get());
        }
        mc.player.connection.send(new ServerboundMovePlayerPacket.Rot(yaw, mc.player.getXRot(), mc.player.onGround(), mc.player.horizontalCollision));
        shouldResendLookPacket = true;
    }


    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.player == null || mc.level == null || mc.getCameraEntity() == null || !renderDirection.get()) return;

        float yawMin = (mode.get() == Mode.Absolute ? mc.player.getYRot() - direction.get() : -direction.get()) - randomRange.get();
        float yawMax = yawMin + 2 * randomRange.get();

        Vec3 rotation = mc.player.getLookAngle().multiply(1, 0, 1);

        Vec3 lookPos = mc.player.position().add(new Vec3(
            (mc.player.getX() - mc.player.xo) * event.tickDelta + mc.player.xo - mc.player.getX(),
            (mc.player.getY() - mc.player.yo) * event.tickDelta + mc.player.yo - mc.player.getY(),
            (mc.player.getZ() - mc.player.zo) * event.tickDelta + mc.player.zo - mc.player.getZ()
        )).add(rotation.scale(distance.get()));

        Vec3 kbRangeMin = lookPos.add(rotation.normalize().multiply(radius.get(), 0, radius.get()).yRot((float) Math.toRadians(yawMin)));
        Vec3 kbRangeMax = lookPos.add(rotation.normalize().multiply(radius.get(), 0, radius.get()).yRot((float) Math.toRadians(yawMax)));

        event.renderer.sideHorizontal(
            lookPos.x-0.25, lookPos.y, lookPos.z-0.25, lookPos.x+0.25, lookPos.z+0.25,
            sideColor.get(), lineColor.get(), ShapeMode.Both
        );

        event.renderer.line(lookPos.x, lookPos.y, lookPos.z, kbRangeMin.x, kbRangeMin.y, kbRangeMin.z, lineColor.get());
        event.renderer.line(lookPos.x, lookPos.y, lookPos.z, kbRangeMax.x, kbRangeMax.y, kbRangeMax.z, lineColor.get());

        if (randomRange.get() != 0) {
            Vec3 lastPoint = null;
            for (float yaw = yawMin; yaw < yawMax; yaw++) {
                Vec3 currentPoint = lookPos.add(rotation.normalize().multiply(radius.get(), 0, radius.get()).yRot((float) Math.toRadians(yaw)));
                if (lastPoint != null)
                    event.renderer.line(lastPoint.x, lastPoint.y, lastPoint.z, currentPoint.x, currentPoint.y, currentPoint.z, lineColor.get());
                lastPoint = currentPoint;
            }
            event.renderer.line(lastPoint.x, lastPoint.y, lastPoint.z, kbRangeMax.x, kbRangeMax.y, kbRangeMax.z, lineColor.get());
        }

    }

    public enum Mode {
        RelativeToPlayer,
        RelativeToView,
        Absolute
    }
}
