/*
 * This file is part of Meteor Satellite Addon (https://github.com/crazycat256/meteor-satellite-addon).
 * Copyright (c) crazycat256.
 */

package fr.crazycat256.satellite.modules;

import fr.crazycat256.satellite.Addon;
import meteordevelopment.meteorclient.events.entity.player.*;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;

import static fr.crazycat256.satellite.utils.TPUtils.findTPPath;

public class InfiniteInteract extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Boolean> notIfSneaking = sgGeneral.add(new BoolSetting.Builder()
        .name("not-if-sneaking")
        .description("Will not interact if you are sneaking.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> hitEntities = sgGeneral.add(new BoolSetting.Builder()
        .name("hit-entities")
        .description("Allows you to hit entities at a distance.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> interactEntities = sgGeneral.add(new BoolSetting.Builder()
        .name("interact-entities")
        .description("Allows you to interact with entities at a distance.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> breakBlocks = sgGeneral.add(new BoolSetting.Builder()
        .name("break-blocks")
        .description("Allows you to break blocks at a distance.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> interactBlocks = sgGeneral.add(new BoolSetting.Builder()
        .name("interact-blocks")
        .description("Allows you to interact with blocks at a distance.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showTrail = sgRender.add(new BoolSetting.Builder()
        .name("show-trail")
        .description("Shows the trail of your teleportation.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> trailColor = sgRender.add(new ColorSetting.Builder()
        .name("trail-color")
        .description("The color of the block highlight.")
        .defaultValue(new SettingColor(255, 255, 255, 255))
        .visible(showTrail::get)
        .build()
    );

    private final Setting<Boolean> showSteps = sgRender.add(new BoolSetting.Builder()
        .name("show-steps")
        .description("Shows the boxes of your teleportation.")
        .defaultValue(false)
        .build()
    );

    private final Setting<SettingColor> boxColor = sgRender.add(new ColorSetting.Builder()
        .name("box-color")
        .description("The color of the block highlight.")
        .defaultValue(new SettingColor(255, 156, 0, 96))
        .visible(showSteps::get)
        .build()
    );

    private final Setting<Integer> trailRenderTime = sgRender.add(new IntSetting.Builder()
        .name("trail-render-time")
        .description("The time in seconds the trail is rendered.")
        .defaultValue(3)
        .sliderRange(0, 10)
        .build()
    );

    private final ArrayList<Vec3d> positions = new ArrayList<>();
    private long lastTP = 0;

    private Vec3d playerPos = null;
    private Vec3d tpBackPos = null;

    public InfiniteInteract() {
        super(Addon.CATEGORY, "infinite-interact", "Allows you to interact with blocks and entities at a distance. (require reach module)");
    }


    @EventHandler
    private void onTick(TickEvent.Pre event) {

        if (tpBackPos != null) {
            tpBack(tpBackPos, playerPos);
            tpBackPos = null;
        }
    }


    @EventHandler
    private void onAttack(AttackEntityEvent event) {
        if (mc.player == null || !hitEntities.get()) return;
        if (notIfSneaking.get() && mc.player.isSneaking()) return;

        Entity entity = event.entity;
        Vec3d entityPos = entity.getPos();

        if (mc.player.distanceTo(entity) <= 3) return;

        Vec3d playerPos = mc.player.getPos();

        Vec3d tempPos = tpTo(entityPos);

        mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(entity, false));
        event.setCancelled(true);
        mc.player.resetLastAttackedTicks();

        tpBack(tempPos, playerPos);

        lastTP = System.currentTimeMillis();

    }

    @EventHandler
    private void onInteractBlock(InteractBlockEvent event) {
        if (mc.player == null || !interactBlocks.get()) return;
        if (notIfSneaking.get() && mc.player.isSneaking()) return;

        Vec3d blockPos = event.result.getPos();

        if (blockPos.distanceTo(mc.player.getPos()) <= 5) return;

        lastTP = System.currentTimeMillis();
    }

    @EventHandler
    private void onInteractEntity(InteractEntityEvent event) {
        if (mc.player == null || !interactEntities.get()) return;
        if (notIfSneaking.get() && mc.player.isSneaking()) return;

        Entity entity = event.entity;
        Vec3d entityPos = entity.getPos();

        if (mc.player.distanceTo(entity) <= 3) return;

        Vec3d playerPos = mc.player.getPos();

        Vec3d tempPos = tpTo(entityPos);

        entity.interactAt(mc.player, entity.getPos(), Hand.MAIN_HAND);
        event.setCancelled(true);

        tpBack(tempPos, playerPos);

        lastTP = System.currentTimeMillis();
    }

    @EventHandler
    private void onStartBreakingBlock(StartBreakingBlockEvent event) {
        if (mc.player == null || !breakBlocks.get()) return;
        if (notIfSneaking.get() && mc.player.isSneaking()) return;

        Vec3d blockPos = new Vec3d(event.blockPos.getX(), event.blockPos.getY(), event.blockPos.getZ());

        if (blockPos.distanceTo(mc.player.getPos()) <= 5) return;

        Vec3d playerPos = mc.player.getPos();

        Vec3d tempPos = tpTo(blockPos);

        mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, event.blockPos, event.direction));

        tpBack(tempPos, playerPos);

        lastTP = System.currentTimeMillis();
    }

    @EventHandler
    private void onBreakBlock(BreakBlockEvent event) {
        if (mc.player == null || !breakBlocks.get()) return;
        if (notIfSneaking.get() && mc.player.isSneaking()) return;

        Vec3d blockPos = new Vec3d(event.blockPos.getX(), event.blockPos.getY(), event.blockPos.getZ());

        if (blockPos.distanceTo(mc.player.getPos()) <= 5) return;

        Vec3d playerPos = mc.player.getPos();

        Vec3d tempPos = tpTo(blockPos);

        mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, event.blockPos, Direction.UP));

        tpBack(tempPos, playerPos);

        lastTP = System.currentTimeMillis();
    }

    @EventHandler
    private void onRender(Render3DEvent event) {

        if (lastTP + trailRenderTime.get() * 1000 < System.currentTimeMillis()) return;

        if (showTrail.get()) {

            for (int i = 0; i < positions.size() - 1; i++) {
                Vec3d  p1 = positions.get(i);
                Vec3d  p2 = positions.get(i + 1);
                event.renderer.line(p1.x, p1.y, p1.z, p2.x, p2.y, p2.z, trailColor.get());
            }

        }
        if (showSteps.get()) {
            for (Vec3d vec : positions) {
                event.renderer.box(vec.x - 0.375, vec.y, vec.z - 0.375, vec.x + 0.375, vec.y + 2, vec.z + 0.375, boxColor.get(), boxColor.get(), ShapeMode.Sides, 0);
            }
        }
    }

    private void sendPositionPacket(double x, double y, double z) {
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, true));
    }

    private Vec3d tpTo(Vec3d tpPos) {
        Vec3d playerPos = mc.player.getPos();

        ArrayList<Vec3d> path = findTPPath(playerPos, tpPos, 20, 3, 8);
        if (path == null) return null;
        positions.clear();
        positions.add(playerPos);
        for (int i = 0; i < 20 - path.size(); i++) {
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true));
        }
        for (Vec3d vec : path) {
            sendPositionPacket(vec.x, vec.y, vec.z);
            positions.add(vec);
        }
        Vec3d endPos = path.getLast();
        sendPositionPacket(endPos.x, endPos.y, endPos.z);
        this.playerPos = playerPos;
        tpBackPos = endPos;
        return endPos;
    }

    private void tpBack(Vec3d tempPos, Vec3d initialPos) {
        ArrayList<Vec3d> path = findTPPath(tempPos, initialPos, 10, 0, 9);
        if (path == null) return;
        for (int i = 0; i < 20 - path.size(); i++) {
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true));
        }
        for (Vec3d vec : path) {
            sendPositionPacket(vec.x, vec.y, vec.z);
        }
        sendPositionPacket(initialPos.x, initialPos.y, initialPos.z);
        playerPos = null;
        tpBackPos = null;
    }
}
