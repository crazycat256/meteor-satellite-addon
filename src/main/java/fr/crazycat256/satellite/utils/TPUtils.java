/*
 * This file is part of Meteor Satellite Addon (https://github.com/crazycat256/meteor-satellite-addon).
 * Copyright (c) crazycat256.
 */

package fr.crazycat256.satellite.utils;

import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import java.lang.Math;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Math.floor;
import static meteordevelopment.meteorclient.MeteorClient.mc;

public class TPUtils {

    public static final double movedWronglyThreshold = 0.0625D;

    /**
     * Teleports the player to a position using the paper method
     * @see meteordevelopment.meteorclient.commands.commands.VClipCommand
     */
    public static void PaperTP(Vec3d startPos, Vec3d pos)  {

        if (mc.player.isSneaking()) {
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
        }

        double distance = startPos.distanceTo(pos);

        int packetsRequired = (int) Math.ceil(Math.abs(distance / 10));
        for (int packetNumber = 0; packetNumber < (packetsRequired - 1); packetNumber++) {
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true));
        }

        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y, pos.z, true));
    }
    public static void PaperTP(Vec3d pos) {
        PaperTP(mc.player.getPos(), pos);
    }

    public static BlockPos Vec3d2BlockPos(Vec3d pos) {
        return new BlockPos((int) floor(pos.x), (int) floor(pos.y), (int) floor(pos.z));
    }
    public static ArrayList<Vec3d> findTPPath(Vec3d pos, double maxDistance) {
        return findTPPath(mc.player.getPos(), pos, 10, maxDistance, 8);
    }

    public static ArrayList<Vec3d> findTPPath(Vec3d startPos, Vec3d newPos, int maxSteps, double maxDistance, double accuracy) {
        final double maxSquaredDistance = maxDistance * maxDistance;
        final List<Direction> directions = new ArrayList<>(Arrays.stream(Direction.values()).toList());
        directions.sort((d1, d2) -> {
            double d1Dist = newPos.distanceTo(startPos.offset(d1, 10));
            double d2Dist = newPos.distanceTo(startPos.offset(d2, 10));
            return Double.compare(d1Dist, d2Dist);
        });

        ArrayList<Vec3d> positions = new ArrayList<>();


        for (int h = 0; h < maxSteps; h++) {
            double closestPosSquaredDistance = positions.isEmpty() ? Integer.MAX_VALUE : (positions.getLast().squaredDistanceTo(newPos) + 100);
            Vec3d currentPos = positions.isEmpty() ? startPos : positions.getLast();
            double currentPosDistance = currentPos.distanceTo(newPos);
            Vec3d roundCurrentPos = new Vec3d(Math.floor(currentPos.x) + 0.5, Math.floor(currentPos.y), Math.floor(currentPos.z) + 0.5);
            Vec3d closestPos = null;

            Vec3d potentialPos;
            if (currentPos.squaredDistanceTo(newPos) <= 100) {
                potentialPos = newPos;
            } else {
                potentialPos = currentPos.add(newPos.subtract(currentPos).normalize().multiply(10));
                if (positions.contains(potentialPos)) {
                    return null;
                }
            }
            if (isTPValid(currentPos, potentialPos)) {
                positions.add(potentialPos);
                if (potentialPos.squaredDistanceTo(newPos) <= maxSquaredDistance) {
                    return positions;
                }
            }

            else {
                positionsLoop:
                for (int i = 10; i >= -10; i--) {
                    int maxJ = (int) Math.sqrt(100 - i * i);
                    for (int j = maxJ; j >= -maxJ; j--) {
                        int maxK = (int) Math.sqrt(100 - i * i - j * j);
                        for (int k = maxK; k >= -maxK; k--) {
                            potentialPos = roundCurrentPos.offset(directions.get(0), i).offset(directions.get(1), j).offset(directions.get(2), k);
                            double potentialPosSquaredDistance = potentialPos.squaredDistanceTo(newPos);
                            if (potentialPosSquaredDistance < closestPosSquaredDistance && isTPValid(currentPos, potentialPos)) {
                                if (potentialPosSquaredDistance <= maxSquaredDistance) {
                                    positions.add(potentialPos);
                                    return positions;
                                } else {
                                    closestPos = potentialPos;
                                    closestPosSquaredDistance = potentialPosSquaredDistance;
                                    if (currentPosDistance - Math.sqrt(closestPosSquaredDistance) > accuracy) {
                                        break positionsLoop;
                                    }
                                }
                            }
                        }
                    }
                }
                if (closestPos != null && !positions.contains(closestPos) && !(closestPosSquaredDistance > 10 * Math.pow(maxSteps - h, 2 + maxDistance))) {
                    positions.add(closestPos);
                } else {
                    return null;
                }
            }

        }
        return positions;
    }

    /**
     * Checks if a position is obstructed for the player
     * @param pos The position to check
     * @return true if the position is obstructed
     */
    public static boolean isObstructed(Vec3d pos) {
        Box box = mc.player.getBoundingBox().offset(mc.player.getPos().negate()).offset(pos);
        box = box.expand(-0.0001, -0.0001, -0.0001);

        // Using a loop like this is faster than using a stream
        for (VoxelShape v: mc.world.getBlockCollisions(mc.player, box)) {
            return true;
        }
        return false;
    }

    /**
     * Checks if a teleport is valid
     * @param startPos The position of the player before the teleport
     * @param endPos The position of the player after the teleport
     */
    public static boolean isTPValid(Vec3d startPos, Vec3d endPos) {
        return startPos.squaredDistanceTo(endPos) < 100.0000000000001 && !isObstructed(endPos) && !isWrongMove(startPos, endPos);
    }

    /**
     * Checks if a move will trigger the "Player mover wrongly!" message
     * @param startPos The position of the player before the move
     * @param endPos The position of the player after the move
     * @return true if the move is wrong
     */
    public static boolean isWrongMove(Vec3d startPos, Vec3d endPos) {
        return ServerUtils.getSquaredMovementDelta(startPos, endPos) > movedWronglyThreshold;
    }


}


