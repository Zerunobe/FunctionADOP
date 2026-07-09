package com.example.itemfunction.item;

import com.example.itemfunction.data.ItemFunctionData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class FunctionItemExecuteHandler {

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;

        ItemStack stack = event.getItemStack();
        if (stack.getItem() instanceof FunctionItem) return; // that one opens the editor GUI instead
        if (!ItemFunctionData.hasData(stack)) return;

        boolean ran = ItemFunctionData.execute(serverPlayer, stack);
        if (ran) {
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        }
    }
}
