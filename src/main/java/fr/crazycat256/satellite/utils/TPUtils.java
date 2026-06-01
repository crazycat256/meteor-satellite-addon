/*
 * This file is part of Meteor Satellite Addon (https://github.com/crazycat256/meteor-satellite-addon).
 * Copyright (c) crazycat256.
 */

package fr.crazycat256.satellite.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

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
    public static void PaperTP(Vec3 startPos, Vec3 pos)  {

        if (mc.player.isShiftKeyDown()) {
            Input lastInput = mc.player.getLastSentInput();
            Input input = new Input(
                lastInput.forward(),
                lastInput.backward(),
                lastInput.left(),
                lastInput.right(),
                lastInput.jump(),
                false,
                lastInput.sprint()
            );
            mc.player.connection.send(new ServerboundPlayerInputPacket(input));
        }

        double distance = startPos.distanceTo(pos);

        int packetsRequired = (int) Math.ceil(Math.abs(distance / 10));
        for (int packetNumber = 0; packetNumber < (packetsRequired - 1); packetNumber++) {
            mc.player.connection.send(new ServerboundMovePlayerPacket.StatusOnly(true, mc.player.horizontalCollision));
        }

        mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(pos.x, pos.y, pos.z, true, mc.player.horizontalCollision));
    }
    public static void PaperTP(Vec3 pos) {
        PaperTP(mc.player.position(), pos);
    }

    public static BlockPos vec3ToBlockPos(Vec3 pos) {
        return new BlockPos((int) floor(pos.x), (int) floor(pos.y), (int) floor(pos.z));
    }
    public static ArrayList<Vec3> findTPPath(Vec3 pos, double maxDistance) {
        return findTPPath(mc.player.position(), pos, 10, maxDistance, 8);
    }

    public static ArrayList<Vec3> findTPPath(Vec3 startPos, Vec3 newPos, int maxSteps, double maxDistance, double accuracy) {
        final double maxSquaredDistance = maxDistance * maxDistance;
        final List<Direction> directions = new ArrayList<>(Arrays.stream(Direction.values()).toList());
        directions.sort((d1, d2) -> {
            double d1Dist = newPos.distanceTo(startPos.relative(d1, 10));
            double d2Dist = newPos.distanceTo(startPos.relative(d2, 10));
            return Double.compare(d1Dist, d2Dist);
        });

        ArrayList<Vec3> positions = new ArrayList<>();


        for (int h = 0; h < maxSteps; h++) {
            double closestPosSquaredDistance = positions.isEmpty() ? Integer.MAX_VALUE : (positions.getLast().distanceToSqr(newPos) + 100);
            Vec3 currentPos = positions.isEmpty() ? startPos : positions.getLast();
            double currentPosDistance = currentPos.distanceTo(newPos);
            Vec3 roundCurrentPos = new Vec3(Math.floor(currentPos.x) + 0.5, Math.floor(currentPos.y), Math.floor(currentPos.z) + 0.5);
            Vec3 closestPos = null;

            Vec3 potentialPos;
            if (currentPos.distanceToSqr(newPos) <= 100) {
                potentialPos = newPos;
            } else {
                potentialPos = currentPos.add(newPos.subtract(currentPos).normalize().scale(10));
                if (positions.contains(potentialPos)) {
                    return null;
                }
            }
            if (isTPValid(currentPos, potentialPos)) {
                positions.add(potentialPos);
                if (potentialPos.distanceToSqr(newPos) <= maxSquaredDistance) {
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
                            potentialPos = roundCurrentPos.relative(directions.get(0), i).relative(directions.get(1), j).relative(directions.get(2), k);
                            double potentialPosSquaredDistance = potentialPos.distanceToSqr(newPos);
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
    public static boolean isObstructed(Vec3 pos) {
        AABB box = mc.player.getBoundingBox().move(mc.player.position().reverse()).move(pos);
        box = box.inflate(-0.0001, -0.0001, -0.0001);

        // Using a loop like this is faster than using a stream
        for (VoxelShape v: mc.level.getBlockCollisions(mc.player, box)) {
            return true;
        }
        return false;
    }

    /**
     * Checks if a teleport is valid
     * @param startPos The position of the player before the teleport
     * @param endPos The position of the player after the teleport
     */
    public static boolean isTPValid(Vec3 startPos, Vec3 endPos) {
        return startPos.distanceToSqr(endPos) < 100.0000000000001 && !isObstructed(endPos) && !isWrongMove(startPos, endPos);
    }

    /**
     * Checks if a move will trigger the "Player mover wrongly!" message
     * @param startPos The position of the player before the move
     * @param endPos The position of the player after the move
     * @return true if the move is wrong
     */
    public static boolean isWrongMove(Vec3 startPos, Vec3 endPos) {
        return ServerUtils.getSquaredMovementDelta(startPos, endPos) > movedWronglyThreshold;
    }


}

