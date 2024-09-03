/*
 * This file is part of Meteor Satellite Addon (https://github.com/crazycat256/meteor-satellite-addon).
 * Copyright (c) crazycat256.
 */

package fr.crazycat256.satellite.modules;

import com.google.common.collect.Streams;
import fr.crazycat256.satellite.Addon;
import fr.crazycat256.satellite.utils.TPUtils;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;

public class InfiniteClickTP extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("render");


    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("The mode of the teleportation.")
        .defaultValue(Mode.Paper)
        .build()
    );

    private final Setting<Integer> maxDistance = sgGeneral.add(new IntSetting.Builder()
        .name("max-distance")
        .description("The maximum distance you can teleport.")
        .defaultValue(100)
        .min(0)
        .sliderRange(3, 200)
        .build()
    );

    private final Setting<Boolean> onlyIfSneaking = sgGeneral.add(new BoolSetting.Builder()
        .name("only-if-sneaking")
        .description("Only teleport if you are sneaking.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> preventWrongMoves = sgGeneral.add(new BoolSetting.Builder()
        .name("prevent-wrong-moves")
        .description("Tries to prevent teleports that would rollback.")
        .defaultValue(true)
        .visible(() -> mode.get() != Mode.Pathfinder)
        .build()
    );

    private final Setting<Boolean> showBlock = sgRender.add(new BoolSetting.Builder()
        .name("show-block")
        .description("Shows the trail of your teleportation.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> blockColor = sgRender.add(new ColorSetting.Builder()
        .name("block-color")
        .description("The color of the block highlight.")
        .defaultValue(new SettingColor(0, 255, 255, 255))
        .visible(showBlock::get)
        .build()
    );

    private final Setting<SettingColor> wrongBlockColor = sgRender.add(new ColorSetting.Builder()
        .name("wrong-block-color")
        .description("The color of the block highlight if the move is wrong.")
        .defaultValue(new SettingColor(255, 0, 0, 255))
        .visible(() -> showBlock.get() && preventWrongMoves.isVisible() && preventWrongMoves.get())
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
        .description("Shows the trail of your teleportation.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> stepColor = sgRender.add(new ColorSetting.Builder()
        .name("box-color")
        .description("The color of the block highlight.")
        .defaultValue(new SettingColor(255, 156, 0, 96))
        .visible(showSteps::get)
        .build()
    );

    private final Setting<Integer> renderTime = sgRender.add(new IntSetting.Builder()
        .name("trail-render-time")
        .description("The time in seconds the trail is rendered.")
        .defaultValue(3)
        .sliderRange(0, 10)
        .build()
    );

    private boolean cancel;
    private long lastTP;
    private final ArrayList<Vec3d> positions = new ArrayList<>();
    private final BlockPos.Mutable blockPos = new BlockPos.Mutable();
    private boolean wrongMove = true;

    public InfiniteClickTP() {
        super(Addon.CATEGORY, "infinite-click-tp", "Teleports you to the block you click on.");
    }

    @Override
    public void onActivate() {
        cancel = false;
        lastTP = 0;
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        if (mc.options.sneakKey.isPressed() || !onlyIfSneaking.get()) {

            RaycastContext context = new RaycastContext(mc.player.getEyePos(), mc.player.getEyePos().add(mc.player.getRotationVector().multiply(maxDistance.get())), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.ANY, mc.player);
            blockPos.set(mc.world.raycast(context).getBlockPos());

            Box box = mc.player.getBoundingBox().offset(mc.player.getPos().negate()).offset(0, 1, 0);

            while (!Streams.stream(mc.world.getBlockCollisions(mc.player, box.offset(Vec3d.ofBottomCenter(blockPos)))).toList().isEmpty()) {
                blockPos.set(blockPos.up());
            }

            Vec3d tpPos = Vec3d.ofBottomCenter(blockPos).add(0, 1, 0);
            Vec3d tpVec = tpPos.subtract(mc.player.getPos());

            wrongMove = false;
            if (mode.get() == Mode.Paper) {
                wrongMove = mc.player.getPos().squaredDistanceTo(tpPos) > 40_000 || TPUtils.isWrongMove(mc.player.getPos(), tpPos);
            } else if (mode.get() == Mode.Straight){
                Vec3d tempPos = mc.player.getPos();
                for (int i = 10; i < tpVec.length(); i += 10) {
                    Vec3d tempPos2 = tempPos.add(tpVec.normalize().multiply(10));
                    if (!TPUtils.isTPValid(tempPos, tempPos2)) {
                        wrongMove = true;
                        break;
                    }
                }
            }

            if (mode.get() != Mode.Pathfinder && preventWrongMoves.get() && wrongMove) return;


            if (mc.options.useKey.isPressed() && !cancel) {


                positions.clear();
                positions.add(mc.player.getPos());

                switch (mode.get()) {

                    case Paper -> {
                        TPUtils.PaperTP(tpPos);
                        positions.add(tpPos);
                    }

                    case Straight -> {
                        for (int i = 10; i < tpVec.length(); i += 10) {
                            Vec3d vec = mc.player.getPos().add(tpVec.normalize().multiply(i));
                            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(vec.x, vec.y, vec.z, true));
                            positions.add(vec);
                        }
                        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(tpPos.x, tpPos.y, tpPos.z, true));
                        positions.add(tpPos);
                    }

                    case Pathfinder -> {
                        ArrayList<Vec3d> steps = TPUtils.findTPPath(mc.player.getPos(), tpPos, 20, 0, 9);
                        if (steps != null) {
                            positions.addAll(steps);
                            for (Vec3d vec : steps) {
                                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(vec.x, vec.y, vec.z, true));
                            }
                            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(tpPos.x, tpPos.y, tpPos.z, true));
                        }
                    }
                }
                mc.player.updatePosition(tpPos.x, tpPos.y, tpPos.z);
                mc.player.setVelocity(0, 0, 0);

                cancel = true;
                lastTP = System.currentTimeMillis();
            }
        }
        if (!mc.options.useKey.isPressed()) {
            cancel = false;
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if ((mc.options.sneakKey.isPressed() || !onlyIfSneaking.get()) && showBlock.get()) {
            Color color;
            if (mode.get() != Mode.Pathfinder && wrongMove && preventWrongMoves.get()) {
                color = wrongBlockColor.get();
            } else {
                color = blockColor.get();
            }
            double x1 = blockPos.getX();
            double x2 = blockPos.getX() + 1;
            double y = blockPos.getY() + 1;
            double z1 = blockPos.getZ();
            double z2 = blockPos.getZ() + 1;

            event.renderer.line(x1, y, z1, x2, y, z1, color);
            event.renderer.line(x2, y, z1, x2, y, z2, color);
            event.renderer.line(x2, y, z2, x1, y, z2, color);
            event.renderer.line(x1, y, z2, x1, y, z1, color);
        }

        if (lastTP + renderTime.get() * 1000 < System.currentTimeMillis()) return;

        if (showTrail.get()) {
            for (int i = 0; i < positions.size() - 1; i++) {
                Vec3d p1 = positions.get(i);
                Vec3d p2 = positions.get(i+1);
                event.renderer.line(p1.x, p1.y, p1.z, p2.x, p2.y, p2.z, trailColor.get());
            }
        }
        if (showSteps.get()) {
            for (Vec3d vec : positions) {
                event.renderer.box(vec.x - 0.375, vec.y, vec.z - 0.375, vec.x + 0.375, vec.y + 2, vec.z + 0.375, stepColor.get(), stepColor.get(), ShapeMode.Sides, 0);
            }
        }
    }


    public enum Mode {
        Paper,
        Straight,
        Pathfinder

    }
}
