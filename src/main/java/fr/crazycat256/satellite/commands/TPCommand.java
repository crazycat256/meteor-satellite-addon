/*
 * This file is part of Meteor Satellite Addon (https://github.com/crazycat256/meteor-satellite-addon).
 * Copyright (c) crazycat256.
 */

package fr.crazycat256.satellite.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import fr.crazycat256.satellite.utils.TPUtils;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.PlayerArgumentType;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerEntity;

public class TPCommand extends Command {
    public TPCommand() {
        super("tp", "Teleports you to a player.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {

        builder.then(argument("player", PlayerArgumentType.create())
            .executes(ctx -> {

                PlayerEntity player = PlayerArgumentType.get(ctx);

                if (player == mc.player) {
                    error("You can't teleport to yourself.");
                    return 0;
                } else if (player == mc.getCameraEntity()) {
                    error("Camera entity doesn't exist.");
                    return 0;
                }

                TPUtils.PaperTP(player.getPos());
                mc.player.updatePosition(player.getX(), player.getY(), player.getZ());

                return SINGLE_SUCCESS;
        }));
    }
}
