/*
 * This file is part of Meteor Satellite Addon (https://github.com/crazycat256/meteor-satellite-addon).
 * Copyright (c) crazycat256.
 */

package fr.crazycat256.satellite.mixin;

import fr.crazycat256.satellite.modules.NBTTooltip;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Inject(method = "getTooltip", at = @At(value = "RETURN", ordinal = 1))
    private void getTooltip(Item.TooltipContext tooltipContext, @Nullable PlayerEntity entity, TooltipType tooltipType, CallbackInfoReturnable<List<Text>> info) {
        String nbtStr = Modules.get().get(NBTTooltip.class).getTooltip((ItemStack) (Object) this, tooltipContext);
        if (nbtStr != null) {
            List<Text> tooltip = info.getReturnValue();
            for (String line : nbtStr.split("\n")) {
                tooltip.add(Text.literal(line));
            }
        }
    }
}
