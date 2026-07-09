package com.example.itemfunction.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/**
 * The "editor" tool: right-clicking this item always opens the command-list GUI
 * so the player can build up the Commands/Condition/Cost config.
 * Executing that config on OTHER items (once "set" onto them) is handled by
 * FunctionItemExecuteHandler instead.
 */
public class FunctionItem extends Item {

    public FunctionItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.example.itemfunction.client.ClientScreenOpener.openCommandList(stack));
        }
        return InteractionResultHolder.success(stack);
    }
}
