package com.lastchance.downed.screen.gui;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;

import java.util.UUID;

public final class DownedLootScreenHandler extends GenericContainerScreenHandler {
    private static final int TOP_INVENTORY_SIZE = 54;

    private final DownedLootInventory inventory;

    public DownedLootScreenHandler(int syncId, PlayerInventory playerInventory, DownedLootInventory inventory) {
        super(ScreenHandlerType.GENERIC_9X6, syncId, playerInventory, inventory, 6);
        this.inventory = inventory;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        if (slot >= 0 && slot < TOP_INVENTORY_SIZE && !inventory.isBackedSlot(slot)) {
            return ItemStack.EMPTY;
        }

        return super.quickMove(player, slot);
    }

    public boolean isViewingTarget(UUID targetUuid) {
        return inventory.getTargetUuid().equals(targetUuid);
    }
}
