package com.lastchance.downed.item;

import com.lastchance.downed.DownedPlayersMod;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

public final class ModItems {
    public static final RegistryKey<Item> DETECTOR_KEY = RegistryKey.of(RegistryKeys.ITEM, DownedPlayersMod.id("detector"));
    public static final Item DETECTOR = Registry.register(
            Registries.ITEM,
            DETECTOR_KEY,
            new Item(new Item.Settings().registryKey(DETECTOR_KEY).maxCount(1))
    );

    private ModItems() {
    }

    public static void register() {
    }
}
