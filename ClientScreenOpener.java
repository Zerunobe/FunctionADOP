package com.example.itemfunction.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

public class ClientScreenOpener {
    public static void openCommandList(ItemStack stack) {
        Minecraft.getInstance().setScreen(new CommandListScreen(stack));
    }
}
