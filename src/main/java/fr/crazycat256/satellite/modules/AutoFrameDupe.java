/*
 * This file is part of Meteor Satellite Addon (https://github.com/crazycat256/meteor-satellite-addon).
 * Copyright (c) crazycat256.
 */

package fr.crazycat256.satellite.modules;

import fr.crazycat256.satellite.Addon;
import fr.crazycat256.satellite.utils.TPUtils;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.Renderer3D;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemFrameItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.time.DurationFormatUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AutoFrameDupe extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final SettingGroup sgSpeedrun = settings.createGroup("Speedrun");
    private final SettingGroup sgRender = settings.createGroup("Render");

    public final Setting<List<Item>> dupeItems = sgGeneral.add(new ItemListSetting.Builder()
        .name("items")
        .description("Items to dupe.")
        .defaultValue(Arrays.asList(Items.SHULKER_BOX, Items.WHITE_SHULKER_BOX, Items.ORANGE_SHULKER_BOX, Items.MAGENTA_SHULKER_BOX,
            Items.LIGHT_BLUE_SHULKER_BOX, Items.YELLOW_SHULKER_BOX, Items.LIME_SHULKER_BOX, Items.PINK_SHULKER_BOX,
            Items.GRAY_SHULKER_BOX, Items.LIGHT_GRAY_SHULKER_BOX, Items.CYAN_SHULKER_BOX, Items.PURPLE_SHULKER_BOX,
            Items.BLUE_SHULKER_BOX, Items.BROWN_SHULKER_BOX, Items.GREEN_SHULKER_BOX, Items.RED_SHULKER_BOX, Items.BLACK_SHULKER_BOX))
        .build()
    );

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("The mode to use when duping.")
        .defaultValue(Mode.Normal)
        .build()
    );

    private final Setting<Integer> minStackSize = sgGeneral.add(new IntSetting.Builder()
        .name("min-stack-size")
        .description("The minimum stack size to use when duping.")
        .defaultValue(5)
        .min(1)
        .sliderMax(64)
        .visible(() -> mode.get() == Mode.Fast)
        .build()
    );

    private final Setting<Boolean> replaceItemFrames = sgGeneral.add(new BoolSetting.Builder()
        .name("replace-item-frames")
        .description("Replaces item frames when they are dropped.")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.Fast)
        .build()
    );

    private final Setting<Integer> maxPlacements = sgGeneral.add(new IntSetting.Builder()
        .name("max-placements")
        .description("The maximum number of item to place per tick.")
        .defaultValue(12)
        .min(0)
        .sliderRange(1, 32)
        .build()
    );

    private final Setting<Integer> maxSwaps = sgGeneral.add(new IntSetting.Builder()
        .name("max-swaps")
        .description("The maximum number of item swaps to perform per tick.")
        .defaultValue(8)
        .min(0)
        .sliderRange(1, 8)
        .build()
    );

    private final Setting<Integer> maxInventoryMoves = sgGeneral.add(new IntSetting.Builder()
        .name("max-inventory-moves")
        .description("The maximum number of item moves from inventory to hotbar to perform per tick (recommended to leave at 0).")
        .defaultValue(0)
        .min(0)
        .sliderMax(20)
        .build()
    );

    private final Setting<Boolean> chronometer = sgSpeedrun.add(new BoolSetting.Builder()
        .name("chronometer")
        .description("Prints the time elapsed since the start of the dupe.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> autoDisable = sgSpeedrun.add(new BoolSetting.Builder()
        .name("auto-disable")
        .description("Automatically stops the dupe when you have enough items.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> itemCount = sgSpeedrun.add(new IntSetting.Builder()
        .name("item-count")
        .description("The number of items to dupe.")
        .defaultValue(2304)
        .min(0)
        .sliderMax(3000)
        .visible(autoDisable::get)
        .build()
    );

    private final Setting<Boolean> renderPlace = sgRender.add(new BoolSetting.Builder()
        .name("render-empty")
        .description("Renders the item frames where the items will be placed.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> emptyColor = sgRender.add(new ColorSetting.Builder()
        .name("empty-color")
        .description("The color of the item frame where the item will be placed.")
        .defaultValue(new Color(0, 255, 0, 32))
        .visible(renderPlace::get)
        .build()
    );

    private final Setting<Boolean> renderDrop = sgRender.add(new BoolSetting.Builder()
        .name("render-filled")
        .description("Renders the item frames where the items will be dropped.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> filledColor = sgRender.add(new ColorSetting.Builder()
        .name("filled-color")
        .description("The color of the item frame where the item will be dropped.")
        .defaultValue(new Color(255, 0, 0, 32))
        .visible(renderDrop::get)
        .build()
    );

    private final List<ItemFrame> reachableItemFrames = new ArrayList<>();
    private final List<ItemFrame> toReplace = new ArrayList<>();

    private final List<ItemFrame> dontHit = new ArrayList<>();


    private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    private long startTime = 0;

    public AutoFrameDupe() {
        super(Addon.CATEGORY, "auto-frame-dupe", "Automate Cafouillage item frame dupe.");
    }

    @Override
    public void onActivate() {
        startTime = System.currentTimeMillis();
        if (chronometer.get())
            info("Started at §f" + sdf.format(startTime));
        reachableItemFrames.clear();
        toReplace.clear();
        dontHit.clear();
    }

    @Override
    public void onDeactivate() {
        if (chronometer.get()) {
            long stopTime = System.currentTimeMillis();
            info("Stopped at §f" + sdf.format(stopTime));
            info("Duration: §f" + DurationFormatUtils.formatDurationWords(stopTime - startTime, true, true));
        }
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        MeteorExecutor.execute(() -> {
            try {Thread.sleep(100);} catch (InterruptedException ignored) {}

            List<ItemFrame> itemFrames = new ArrayList<>();
            Inventory inv = mc.player.getInventory();


            for (Entity entity: mc.level.entitiesForRendering()) {
                if (entity instanceof ItemFrame itemFrame) {
                    if (entity.distanceToSqr(mc.player) < 25) {
                        if (dupeItems.get().contains(itemFrame.getItem().getItem()))
                            mc.gameMode.attack(mc.player, itemFrame);
                        itemFrames.add(itemFrame);
                    }
                }
            }

            if (mode.get() == Mode.Fast && replaceItemFrames.get()) {
                for (ItemFrame itemFrame : reachableItemFrames) {
                    if (itemFrame.distanceToSqr(mc.player) < 25 && !itemFrames.contains(itemFrame))
                        toReplace.add(itemFrame);
                }

                replaceLoop:
                for (ItemFrame itemFrame : toReplace) {

                    for (ItemFrame existingItemFrame : itemFrames) {
                        if (existingItemFrame.position().equals(itemFrame.position()) && existingItemFrame.getDirection() == itemFrame.getDirection()) {
                            continue replaceLoop;
                        }
                    }

                    BlockPos pos = TPUtils.vec3ToBlockPos(itemFrame.position().subtract(itemFrame.getLookAngle().normalize()));
                    if (inv.getItem(Inventory.SLOT_OFFHAND).getItem() instanceof ItemFrameItem) {
                        mc.gameMode.useItemOn(mc.player, InteractionHand.OFF_HAND, new BlockHitResult(Vec3.atCenterOf(pos), itemFrame.getDirection(), pos, false));
                        continue;
                    }

                    boolean swapped = false;
                    if (!(inv.getSelectedItem().getItem() instanceof ItemFrameItem)) {
                        for (int i = 0; i < 9; i++) {
                            if (inv.getItem(i).getItem() instanceof ItemFrameItem) {
                                InvUtils.swap(i, false);
                                swapped = true;
                                break;
                            }
                        }
                    }
                    if (!(inv.getSelectedItem().getItem() instanceof ItemFrameItem) && !swapped) {
                        for (int i = 0; i < inv.getContainerSize(); i++) {
                            if (inv.getItem(i).getItem() instanceof ItemFrameItem) {
                                InvUtils.move().from(i).toHotbar(inv.getSelectedSlot());
                                break;
                            }
                        }
                    }

                    if (inv.getSelectedItem().getItem() instanceof ItemFrameItem) {
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), itemFrame.getDirection(), pos, false));
                        ItemStack stack = mc.player.getItemInHand(InteractionHand.MAIN_HAND).copy();
                        stack.setCount(stack.getCount() - 1);
                        if (stack.getCount() == 0) stack = ItemStack.EMPTY;
                        mc.player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                    }
                }
            }
        });
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;

        if (chronometer.get()) {
            mc.gui.setOverlayMessage(Component.literal("Duration: §f" + DurationFormatUtils.formatDurationWords(System.currentTimeMillis() - startTime, true, true)), false);
        }

        Inventory inv = mc.player.getInventory();

        if (autoDisable.get()) {
            int count = 0;
            for (Item item : dupeItems.get()) {
                count += countInventoryItem(inv, item);
            }
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (entity instanceof ItemEntity item && entity.distanceToSqr(mc.player) < 49) {
                    if (dupeItems.get().contains(item.getItem().getItem())) {
                        count += item.getItem().getCount();
                    }
                }
                if (entity instanceof ItemFrame itemFrame && entity.distanceToSqr(mc.player) < 25) {
                    if (dupeItems.get().contains(itemFrame.getItem().getItem())) {
                        count++;
                    }
                }
            }
            if (count >= itemCount.get()) {
                info("You have reached the item count, disabled.");
                toggle();
                return;
            }
        }

        if (mode.get() == Mode.Normal) {

            List<ItemFrame> emptyItemFrames = new ArrayList<>();
            List<ItemFrame> filledItemFrames = new ArrayList<>();

            for (Entity entity : mc.level.entitiesForRendering()) {
                if (entity instanceof ItemFrame itemFrame) {
                    if (itemFrame.getItem().getItem() == Items.AIR) {
                        emptyItemFrames.add(itemFrame);
                    } else if (dupeItems.get().contains(itemFrame.getItem().getItem())) {
                        filledItemFrames.add(itemFrame);
                    }
                }
            }

            int placements = 0;
            int swaps = 0;
            int moves = 0;
            boolean swapped = false;
            for (ItemFrame emptyItemFrame: emptyItemFrames) {
                if (!dupeItems.get().contains(inv.getSelectedItem().getItem()) && swaps < maxSwaps.get()) {
                    for (int i = 0; i < 9; i++) {
                        if (dupeItems.get().contains(inv.getItem(i).getItem())) {
                            InvUtils.swap(i, false);
                            swaps++;
                            swapped = true;
                            break;
                        }
                    }
                }
                if (!dupeItems.get().contains(inv.getSelectedItem().getItem()) && moves < maxInventoryMoves.get() && !swapped) {
                    for (int i = 0; i < inv.getContainerSize(); i++) {
                        if (dupeItems.get().contains(inv.getItem(i).getItem())) {
                            InvUtils.move().from(i).toHotbar(inv.getSelectedSlot());
                            moves++;
                            break;
                        }
                    }
                }
                if (dupeItems.get().contains(inv.getSelectedItem().getItem()) && placements < maxPlacements.get()) {
                    interactItemFrameEntity(emptyItemFrame);
                    ItemStack stack = mc.player.getItemInHand(InteractionHand.MAIN_HAND).copy();
                    stack.setCount(stack.getCount() - 1);
                    if (stack.getCount() == 0) stack = ItemStack.EMPTY;
                    mc.player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                    dontHit.remove(emptyItemFrame);
                    placements++;
                }
            }

            for (ItemFrame itemFrame: filledItemFrames) {
                if (!dontHit.contains(itemFrame)) {
                    mc.gameMode.attack(mc.player, itemFrame);
                    dontHit.add(itemFrame);
                }
            }
        }

        else if (mode.get() == Mode.Fast) {

            List<ItemFrame> itemFrames = new ArrayList<>();
            for (Entity entity: mc.level.entitiesForRendering()) {
                if (entity instanceof ItemFrame itemFrame) {
                    if (itemFrame.distanceToSqr(mc.player) < 25) {
                        if (itemFrame.getItem().getItem() == Items.AIR)
                            itemFrames.add(itemFrame);
                        else if (dupeItems.get().contains(itemFrame.getItem().getItem()))
                            itemFrames.add(itemFrame);
                    }
                }
            }

            int placements = 0;
            int swaps = 0;
            int moves = 0;


            if (replaceItemFrames.get()) {
                for (ItemFrame itemFrame : List.copyOf(reachableItemFrames)) {
                    if (itemFrame.distanceToSqr(mc.player) > 25)
                        reachableItemFrames.remove(itemFrame);
                    else if (!itemFrames.contains(itemFrame)) {
                        reachableItemFrames.remove(itemFrame);
                        toReplace.add(itemFrame);
                    }

                }

                replaceLoop:
                for (ItemFrame itemFrame: List.copyOf(toReplace)) {

                    for (ItemFrame existingItemFrame: itemFrames) {
                        if (existingItemFrame.position().equals(itemFrame.position()) && existingItemFrame.getDirection() == itemFrame.getDirection()) {
                            toReplace.remove(itemFrame);
                            continue replaceLoop;
                        }
                    }

                    BlockPos pos = TPUtils.vec3ToBlockPos(itemFrame.position().subtract(itemFrame.getLookAngle().normalize()));
                    if (inv.getItem(Inventory.SLOT_OFFHAND).getItem() instanceof ItemFrameItem) {
                        mc.gameMode.useItemOn(mc.player, InteractionHand.OFF_HAND, new BlockHitResult(Vec3.atCenterOf(pos), itemFrame.getDirection(), pos, false));
                        placements++;
                        continue;
                    }

                    boolean swapped = false;
                    if (!(inv.getSelectedItem().getItem() instanceof ItemFrameItem)) {
                        for (int i = 0; i < 9; i++) {
                            if (inv.getItem(i).getItem() instanceof ItemFrameItem) {
                                InvUtils.swap(i, false);
                                swaps++;
                                swapped = true;
                                break;
                            }
                        }
                    }
                    if (!(inv.getSelectedItem().getItem() instanceof ItemFrameItem) && !swapped) {
                        for (int i = 0; i < inv.getContainerSize(); i++) {
                            if (inv.getItem(i).getItem() instanceof ItemFrameItem) {
                                InvUtils.move().from(i).toHotbar(inv.getSelectedSlot());
                                moves++;
                                break;
                            }
                        }
                    }

                    if (inv.getSelectedItem().getItem() instanceof ItemFrameItem) {
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), itemFrame.getDirection(), pos, false));
                        ItemStack stack = mc.player.getItemInHand(InteractionHand.MAIN_HAND).copy();
                        stack.setCount(stack.getCount() - 1);
                        if (stack.getCount() == 0) stack = ItemStack.EMPTY;
                        mc.player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                    }
                }
            }
            reachableItemFrames.clear();
            reachableItemFrames.addAll(itemFrames);


            boolean swapped = false;
            for (ItemFrame itemFrame: itemFrames) {
                if ((!dupeItems.get().contains(inv.getSelectedItem().getItem()) || inv.getSelectedItem().getCount() < minStackSize.get()) && swaps < maxSwaps.get()) {
                    for (int i = 0; i < 9; i++) {
                        if (dupeItems.get().contains(inv.getItem(i).getItem()) && inv.getItem(i).getCount() >= minStackSize.get()) {
                            InvUtils.swap(i, false);
                            swapped = true;
                            swaps++;
                            break;
                        }
                    }
                }
                if ((!dupeItems.get().contains(inv.getSelectedItem().getItem()) || inv.getSelectedItem().getCount() < minStackSize.get()) && moves < maxInventoryMoves.get() && !swapped) {
                    for (int i = 0; i < inv.getContainerSize(); i++) {
                        if (dupeItems.get().contains(inv.getItem(i).getItem()) && inv.getItem(i).getCount() >= minStackSize.get()) {
                            InvUtils.move().from(i).toHotbar(inv.getSelectedSlot());
                            moves++;
                            swapped = true;
                            break;
                        }
                    }
                }
                if (!swapped) {
                    for (int i = 0; i < 9; i++) {
                        if (dupeItems.get().contains(inv.getItem(i).getItem())) {
                            InvUtils.swap(i, false);
                            swapped = true;
                            swaps++;
                            break;
                        }
                    }
                }
                if (!swapped) {
                    for (int i = 0; i < inv.getContainerSize(); i++) {
                        if (dupeItems.get().contains(inv.getItem(i).getItem())) {
                            InvUtils.move().from(i).toHotbar(inv.getSelectedSlot());
                            moves++;
                            swapped = true;
                            break;
                        }
                    }
                }

                if (dupeItems.get().contains(inv.getSelectedItem().getItem()) && itemFrame.getItem().getItem() == Items.AIR && placements < maxPlacements.get()) {
                    interactItemFrame(itemFrame);
                    dontHit.remove(itemFrame);
                    placements++;
                    continue;
                }

                if (!dontHit.contains(itemFrame) && dupeItems.get().contains(itemFrame.getItem().getItem()) && placements < maxPlacements.get()) {
                    dontHit.add(itemFrame);
                    mc.gameMode.attack(mc.player, itemFrame);
                    if (dupeItems.get().contains(inv.getSelectedItem().getItem())) {
                        interactItemFrame(itemFrame);
                        dontHit.remove(itemFrame);
                        placements++;
                    }
                }
            }
        }
    }

    private void interactItemFrame(ItemFrame itemFrame) {
        interactItemFrameEntity(itemFrame);
        ItemStack stack = mc.player.getItemInHand(InteractionHand.MAIN_HAND).copy();
        stack.setCount(stack.getCount() - 1);
        if (stack.getCount() == 0) stack = ItemStack.EMPTY;
        mc.player.setItemInHand(InteractionHand.MAIN_HAND, stack);
    }

    private void interactItemFrameEntity(ItemFrame itemFrame) {
        mc.gameMode.interact(
            mc.player,
            itemFrame,
            new EntityHitResult(itemFrame, itemFrame.getBoundingBox().getCenter()),
            InteractionHand.MAIN_HAND
        );
    }

    private int countInventoryItem(Inventory inv, Item item) {
        int count = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.getItem() == item) count += stack.getCount();
        }
        return count;
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.player == null || mc.level == null) return;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof ItemFrame itemFrame) {
                if (renderPlace.get() && itemFrame.getItem().getItem() == Items.AIR) {
                    renderItemFrame(event.renderer, itemFrame, emptyColor.get());
                } else if (renderDrop.get() && dupeItems.get().contains(itemFrame.getItem().getItem())) {
                    renderItemFrame(event.renderer, itemFrame, filledColor.get());
                }
            }
        }
    }

    private void renderItemFrame(Renderer3D renderer, ItemFrame itemFrame, Color color) {
        Vec3 pos = itemFrame.position();
        renderer.boxSides(pos.x-0.25, pos.y-0.25, pos.z-0.25, pos.x+0.25, pos.y+0.25, pos.z+0.25, color, 0);
    }

    public enum Mode {
        Normal,
        Fast
    }
}
