/*
 * This file is part of Meteor Satellite Addon (https://github.com/crazycat256/meteor-satellite-addon).
 * Copyright (c) crazycat256.
 */

package fr.crazycat256.satellite.utils;

import com.google.common.collect.ImmutableList;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * The most part of the code of this class comes from {@link net.minecraft.server.network.ServerPlayNetworkHandler}
 */
public class ServerUtils {

    public static boolean noBlocksAround(Entity entity) {
        Box box = entity.getBoundingBox().expand(0.0625D).stretch(0.0D, -0.55D, 0.0D);
        int minX = (int) Math.floor(box.minX);
        int minY = (int) Math.floor(box.minY);
        int minZ = (int) Math.floor(box.minZ);
        int maxX = (int) Math.floor(box.maxX);
        int maxY = (int) Math.floor(box.maxY);
        int maxZ = (int) Math.floor(box.maxZ);

        BlockPos pos;

        for (int y = minY; y <= maxY; ++y) {
            for (int z = minZ; z <= maxZ; ++z) {
                for (int x = minX; x <= maxX; ++x) {
                    pos = new BlockPos(x, y, z);
                    BlockState type = mc.world.getBlockState(pos);
                    if (type != null && !type.isAir()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static double getSquaredMovementDelta(Vec3d startPos, Vec3d endPos) {
        double d0 = clampHorizontal(endPos.getX());
        double d1 = clampVertical(endPos.getY());
        double d2 = clampHorizontal(endPos.getZ());

        double d6 = d0 - startPos.getX();
        double d7 = d1 - startPos.getY();
        double d8 = d2 - startPos.getZ();

        Vec3d movedPos = ServerUtils.move(startPos, new Vec3d(d6, d7, d8));

        d6 = d0 - movedPos.x;
        d7 = d1 - movedPos.y;
        d8 = d2 - movedPos.z;

        if (d7 > -0.5D || d7 < 0.5D) {
            d7 = 0.0D;
        }
        return d6 * d6 + d7 * d7 + d8 * d8;
    }

    public static Vec3d move(Vec3d startPos, Vec3d movement) {
        Vec3d vec3d = adjustMovementForCollisions(startPos, movement);
        if (vec3d.lengthSquared() > 1.0E-7) {
            return startPos.add(vec3d);
        }
        return startPos;
    }

    private static Vec3d adjustMovementForCollisions(Vec3d startPos, Vec3d movement) {
        Box box = mc.player.getBoundingBox().offset(mc.player.getPos().negate()).offset(startPos);
        final float stepHeight = 1;
        List<VoxelShape> list = mc.world.getEntityCollisions(mc.player, box.stretch(movement));
        Vec3d vec3d = movement.lengthSquared() == 0.0 ? movement : adjustMovementForCollisions(mc.player, movement, box, mc.world, list);

        boolean bl = movement.x != vec3d.x;
        boolean bl2 = movement.y != vec3d.y;
        boolean bl3 = movement.z != vec3d.z;
        boolean bl4 = mc.player.isOnGround() || bl2 && movement.y < 0.0;
        if (bl4 && (bl || bl3)) {
            Vec3d vec3d2 = adjustMovementForCollisions(mc.player, new Vec3d(movement.x, stepHeight, movement.z), box, mc.world, list);
            Vec3d vec3d3 = adjustMovementForCollisions(
                mc.player, new Vec3d(0.0, stepHeight, 0.0), box.stretch(movement.x, 0.0, movement.z), mc.world, list
            );
            if (vec3d3.y < (double)stepHeight) {
                Vec3d vec3d4 = adjustMovementForCollisions(mc.player, new Vec3d(movement.x, 0.0, movement.z), box.offset(vec3d3), mc.world, list).add(vec3d3);
                if (vec3d4.horizontalLengthSquared() > vec3d2.horizontalLengthSquared()) {
                    vec3d2 = vec3d4;
                }
            }

            if (vec3d2.horizontalLengthSquared() > vec3d.horizontalLengthSquared()) {
                return vec3d2.add(adjustMovementForCollisions(mc.player, new Vec3d(0.0, -vec3d2.y + movement.y, 0.0), box.offset(vec3d2), mc.world, list));
            }
        }

        return vec3d;
    }

    public static Vec3d adjustMovementForCollisions(@Nullable Entity entity, Vec3d movement, Box entityBoundingBox, World world, List<VoxelShape> collisions) {
        ImmutableList.Builder<VoxelShape> builder = ImmutableList.builderWithExpectedSize(collisions.size() + 1);
        if (!collisions.isEmpty()) {
            builder.addAll(collisions);
        }

        WorldBorder worldBorder = world.getWorldBorder();
        boolean bl = entity != null && worldBorder.canCollide(entity, entityBoundingBox.stretch(movement));
        if (bl) {
            builder.add(worldBorder.asVoxelShape());
        }

        builder.addAll(world.getBlockCollisions(entity, entityBoundingBox.stretch(movement)));
        return adjustMovementForCollisions(movement, entityBoundingBox, builder.build());
    }

    private static Vec3d adjustMovementForCollisions(Vec3d movement, Box entityBoundingBox, List<VoxelShape> collisions) {
        if (collisions.isEmpty()) {
            return movement;
        } else {
            double d = movement.x;
            double e = movement.y;
            double f = movement.z;
            if (e != 0.0) {
                e = VoxelShapes.calculateMaxOffset(Direction.Axis.Y, entityBoundingBox, collisions, e);
                if (e != 0.0) {
                    entityBoundingBox = entityBoundingBox.offset(0.0, e, 0.0);
                }
            }

            boolean bl = Math.abs(d) < Math.abs(f);
            if (bl && f != 0.0) {
                f = VoxelShapes.calculateMaxOffset(Direction.Axis.Z, entityBoundingBox, collisions, f);
                if (f != 0.0) {
                    entityBoundingBox = entityBoundingBox.offset(0.0, 0.0, f);
                }
            }

            if (d != 0.0) {
                d = VoxelShapes.calculateMaxOffset(Direction.Axis.X, entityBoundingBox, collisions, d);
                if (!bl && d != 0.0) {
                    entityBoundingBox = entityBoundingBox.offset(d, 0.0, 0.0);
                }
            }

            if (!bl && f != 0.0) {
                f = VoxelShapes.calculateMaxOffset(Direction.Axis.Z, entityBoundingBox, collisions, f);
            }

            return new Vec3d(d, e, f);
        }
    }


    private static double clampHorizontal(double d) {
        return MathHelper.clamp(d, -3.0E7D, 3.0E7D);
    }

    private static double clampVertical(double d) {
        return MathHelper.clamp(d, -2.0E7D, 2.0E7D);
    }

}
