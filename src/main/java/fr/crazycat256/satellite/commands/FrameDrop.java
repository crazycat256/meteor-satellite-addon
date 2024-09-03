/*
 * This file is part of Meteor Satellite Addon (https://github.com/crazycat256/meteor-satellite-addon).
 * Copyright (c) crazycat256.
 */

package fr.crazycat256.satellite.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.Items;

import java.util.List;

public class FrameDrop extends Command {
    public FrameDrop() {
        super("framedrop", "Drops all items from item frames around you.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            if (mc.player == null || mc.world == null || mc.interactionManager == null) return 0;
            List<ItemFrameEntity> itemFrames = mc.world.getEntitiesByClass(ItemFrameEntity.class, mc.player.getBoundingBox().expand(4), entity -> entity.getHeldItemStack().getItem() != Items.AIR);
            for (ItemFrameEntity itemFrame : itemFrames) {
                mc.interactionManager.attackEntity(mc.player, itemFrame);
            }
            info("Dropped " + itemFrames.size() + " items.");
            return SINGLE_SUCCESS;
        });
    }
}
