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
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

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
        if (mc.player == null || mc.world == null) return;
        if (shouldResendLookPacket) {
            shouldResendLookPacket = false;
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(mc.player.getYaw(), mc.player.getPitch(), mc.player.isOnGround()));
        }
        if (shouldResendSprintPacket) {
            shouldResendSprintPacket = false;
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
        }
    }

    @EventHandler
    private void onEntityAttack(AttackEntityEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (fakeSprint.get()) {
            if (mc.player.isSprinting())
                mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
            if (mc.player.isSprinting()) shouldResendSprintPacket = true;
        }

        float yaw;
        if (mode.get() == Mode.Absolute) {
            yaw = direction.get();
        } else if (mode.get() == Mode.RelativeToView) {
            yaw = direction.get() + mc.player.getYaw();
        } else if (mode.get() == Mode.RelativeToPlayer) {
            Vec3d playerPos = mc.player.getPos();
            Vec3d entityPos = event.entity.getPos();
            Vec3d diff = entityPos.subtract(playerPos);

            yaw = (float) Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90 + direction.get();

        } else {
            return;
        }

        if (randomRange.get() != 0) {
            int range = randomRange.get() * 2 + 1;
            yaw += (float) (Math.random() * range - randomRange.get());
        }
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(yaw, mc.player.getPitch(), mc.player.isOnGround()));
        shouldResendLookPacket = true;
    }


    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.player == null || mc.world == null || mc.cameraEntity == null || !renderDirection.get()) return;

        float yawMin = (mode.get() == Mode.Absolute ? mc.player.getYaw() - direction.get() : -direction.get()) - randomRange.get();
        float yawMax = yawMin + 2 * randomRange.get();

        Vec3d rotation = mc.player.getRotationVector().multiply(1, 0, 1);

        Vec3d lookPos = mc.player.getPos().add(new Vec3d(
            (mc.player.getX() - mc.player.prevX) * event.tickDelta + mc.player.prevX - mc.player.getX(),
            (mc.player.getY() - mc.player.prevY) * event.tickDelta + mc.player.prevY - mc.player.getY(),
            (mc.player.getZ() - mc.player.prevZ) * event.tickDelta + mc.player.prevZ - mc.player.getZ()
        )).add(rotation.multiply(distance.get()));

        Vec3d kbRangeMin = lookPos.add(rotation.normalize().multiply(radius.get(), 0, radius.get()).rotateY((float) Math.toRadians(yawMin)));
        Vec3d kbRangeMax = lookPos.add(rotation.normalize().multiply(radius.get(), 0, radius.get()).rotateY((float) Math.toRadians(yawMax)));

        event.renderer.sideHorizontal(
            lookPos.x-0.25, lookPos.y, lookPos.z-0.25, lookPos.x+0.25, lookPos.z+0.25,
            sideColor.get(), lineColor.get(), ShapeMode.Both
        );

        event.renderer.line(lookPos.x, lookPos.y, lookPos.z, kbRangeMin.x, kbRangeMin.y, kbRangeMin.z, lineColor.get());
        event.renderer.line(lookPos.x, lookPos.y, lookPos.z, kbRangeMax.x, kbRangeMax.y, kbRangeMax.z, lineColor.get());

        if (randomRange.get() != 0) {
            Vec3d lastPoint = null;
            for (float yaw = yawMin; yaw < yawMax; yaw++) {
                Vec3d currentPoint = lookPos.add(rotation.normalize().multiply(radius.get(), 0, radius.get()).rotateY((float) Math.toRadians(yaw)));
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
