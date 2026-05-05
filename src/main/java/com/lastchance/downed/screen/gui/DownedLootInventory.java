package com.lastchance.downed.screen.gui;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

public final class DownedLootInventory implements Inventory {
    private static final int GENERIC_9X6_SIZE = 54;

    private final ServerPlayerEntity target;
    private final Runnable closeCallback;

    public DownedLootInventory(ServerPlayerEntity target, Runnable closeCallback) {
        this.target = target;
        this.closeCallback = closeCallback;
    }

    @Override
    public int size() {
        return GENERIC_9X6_SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (int slot = 0; slot < target.getInventory().size(); slot++) {
            if (!target.getInventory().getStack(slot).isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        return isBackedSlot(slot) ? target.getInventory().getStack(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        return isBackedSlot(slot) ? target.getInventory().removeStack(slot, amount) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeStack(int slot) {
        return isBackedSlot(slot) ? target.getInventory().removeStack(slot) : ItemStack.EMPTY;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if (isBackedSlot(slot)) {
            target.getInventory().setStack(slot, stack);
        }
    }

    @Override
    public void markDirty() {
        target.getInventory().markDirty();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return target.isAlive();
    }

    @Override
    public void onClose(PlayerEntity player) {
        closeCallback.run();
    }

    @Override
    public void clear() {
        target.getInventory().clear();
    }

    private boolean isBackedSlot(int slot) {
        return slot >= 0 && slot < target.getInventory().size();
    }
}
