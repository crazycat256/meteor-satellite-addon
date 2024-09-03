/*
 * This file is part of Meteor Satellite Addon (https://github.com/crazycat256/meteor-satellite-addon).
 * Copyright (c) crazycat256.
 */

package fr.crazycat256.satellite.commands;


import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import fr.crazycat256.satellite.utils.TPUtils;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.util.math.Vec3d;

public class TPCamCommand extends Command {
    public TPCamCommand() {
        super("tpcam", "Teleports you to the camera entity.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {


        builder.executes(context -> {
            if (mc.cameraEntity == null) {
                error("Camera entity doesn't exist.");
                return 0;
            }
            Vec3d pos = mc.gameRenderer.getCamera().getPos();
            TPUtils.PaperTP(pos);
            mc.player.updatePosition(pos.x, pos.y, pos.z);
            return SINGLE_SUCCESS;
        });

    }
}
