/*
 * This file is part of Meteor Satellite Addon (https://github.com/crazycat256/meteor-satellite-addon).
 * Copyright (c) crazycat256.
 */

package fr.crazycat256.satellite.utils;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * The most part of the code of this class comes from {@link net.minecraft.server.network.ServerPlayNetworkHandler}
 */
public class ServerUtils {

    public static boolean noBlocksAround(Entity entity) {
        AABB box = entity.getBoundingBox().inflate(0.0625D).expandTowards(0.0D, -0.55D, 0.0D);
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
                    BlockState type = mc.level.getBlockState(pos);
                    if (type != null && !type.isAir()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static double getSquaredMovementDelta(Vec3 startPos, Vec3 endPos) {
        double d0 = clampHorizontal(endPos.x);
        double d1 = clampVertical(endPos.y);
        double d2 = clampHorizontal(endPos.z);

        double d6 = d0 - startPos.x;
        double d7 = d1 - startPos.y;
        double d8 = d2 - startPos.z;

        Vec3 movedPos = ServerUtils.move(startPos, new Vec3(d6, d7, d8));

        d6 = d0 - movedPos.x;
        d7 = d1 - movedPos.y;
        d8 = d2 - movedPos.z;

        if (d7 > -0.5D || d7 < 0.5D) {
            d7 = 0.0D;
        }
        return d6 * d6 + d7 * d7 + d8 * d8;
    }

    public static Vec3 move(Vec3 startPos, Vec3 movement) {
        Vec3 vec3d = adjustMovementForCollisions(startPos, movement);
        if (vec3d.lengthSqr() > 1.0E-7) {
            return startPos.add(vec3d);
        }
        return startPos;
    }

    private static Vec3 adjustMovementForCollisions(Vec3 startPos, Vec3 movement) {
        AABB box = mc.player.getBoundingBox().move(mc.player.position().reverse()).move(startPos);
        final float stepHeight = 1;
        List<VoxelShape> list = mc.level.getEntityCollisions(mc.player, box.expandTowards(movement));
        Vec3 vec3d = movement.lengthSqr() == 0.0 ? movement : adjustMovementForCollisions(mc.player, movement, box, mc.level, list);

        boolean bl = movement.x != vec3d.x;
        boolean bl2 = movement.y != vec3d.y;
        boolean bl3 = movement.z != vec3d.z;
        boolean bl4 = mc.player.onGround() || bl2 && movement.y < 0.0;
        if (bl4 && (bl || bl3)) {
            Vec3 vec3d2 = adjustMovementForCollisions(mc.player, new Vec3(movement.x, stepHeight, movement.z), box, mc.level, list);
            Vec3 vec3d3 = adjustMovementForCollisions(
                mc.player, new Vec3(0.0, stepHeight, 0.0), box.expandTowards(movement.x, 0.0, movement.z), mc.level, list
            );
            if (vec3d3.y < (double)stepHeight) {
                Vec3 vec3d4 = adjustMovementForCollisions(mc.player, new Vec3(movement.x, 0.0, movement.z), box.move(vec3d3), mc.level, list).add(vec3d3);
                if (vec3d4.horizontalDistanceSqr() > vec3d2.horizontalDistanceSqr()) {
                    vec3d2 = vec3d4;
                }
            }

            if (vec3d2.horizontalDistanceSqr() > vec3d.horizontalDistanceSqr()) {
                return vec3d2.add(adjustMovementForCollisions(mc.player, new Vec3(0.0, -vec3d2.y + movement.y, 0.0), box.move(vec3d2), mc.level, list));
            }
        }

        return vec3d;
    }

    public static Vec3 adjustMovementForCollisions(@Nullable Entity entity, Vec3 movement, AABB entityBoundingAABB, Level world, List<VoxelShape> collisions) {
        ImmutableList.Builder<VoxelShape> builder = ImmutableList.builderWithExpectedSize(collisions.size() + 1);
        if (!collisions.isEmpty()) {
            builder.addAll(collisions);
        }

        WorldBorder worldBorder = world.getWorldBorder();
        boolean bl = entity != null && worldBorder.isInsideCloseToBorder(entity, entityBoundingAABB.expandTowards(movement));
        if (bl) {
            builder.add(worldBorder.getCollisionShape());
        }

        builder.addAll(world.getBlockCollisions(entity, entityBoundingAABB.expandTowards(movement)));
        return adjustMovementForCollisions(movement, entityBoundingAABB, builder.build());
    }

    private static Vec3 adjustMovementForCollisions(Vec3 movement, AABB entityBoundingAABB, List<VoxelShape> collisions) {
        if (collisions.isEmpty()) {
            return movement;
        } else {
            double d = movement.x;
            double e = movement.y;
            double f = movement.z;
            if (e != 0.0) {
                e = Shapes.collide(Direction.Axis.Y, entityBoundingAABB, collisions, e);
                if (e != 0.0) {
                    entityBoundingAABB = entityBoundingAABB.move(0.0, e, 0.0);
                }
            }

            boolean bl = Math.abs(d) < Math.abs(f);
            if (bl && f != 0.0) {
                f = Shapes.collide(Direction.Axis.Z, entityBoundingAABB, collisions, f);
                if (f != 0.0) {
                    entityBoundingAABB = entityBoundingAABB.move(0.0, 0.0, f);
                }
            }

            if (d != 0.0) {
                d = Shapes.collide(Direction.Axis.X, entityBoundingAABB, collisions, d);
                if (!bl && d != 0.0) {
                    entityBoundingAABB = entityBoundingAABB.move(d, 0.0, 0.0);
                }
            }

            if (!bl && f != 0.0) {
                f = Shapes.collide(Direction.Axis.Z, entityBoundingAABB, collisions, f);
            }

            return new Vec3(d, e, f);
        }
    }


    private static double clampHorizontal(double d) {
        return Mth.clamp(d, -3.0E7D, 3.0E7D);
    }

    private static double clampVertical(double d) {
        return Mth.clamp(d, -2.0E7D, 2.0E7D);
    }

}
