package com.example.itemfunction;

import com.example.itemfunction.data.CommandScheduler;
import com.example.itemfunction.item.ModItems;
import com.example.itemfunction.network.ModNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ItemFunctionMod.MOD_ID)
public class ItemFunctionMod {

    public static final String MOD_ID = "itemfunction";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public ItemFunctionMod() {
        ModItems.ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModNetwork.register();

        MinecraftForge.EVENT_BUS.register(CommandScheduler.class);
        MinecraftForge.EVENT_BUS.register(com.example.itemfunction.item.FunctionItemExecuteHandler.class);
    }
}
