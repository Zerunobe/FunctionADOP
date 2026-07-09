package com.example.itemfunction.network;

import com.example.itemfunction.data.CommandEntry;
import com.example.itemfunction.data.ItemFunctionData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SaveCommandsPacket {

    private final List<CommandEntry> commands;
    private final boolean mainHand;

    public SaveCommandsPacket(List<CommandEntry> commands, boolean mainHand) {
        this.commands = commands;
        this.mainHand = mainHand;
    }

    public static void encode(SaveCommandsPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.mainHand);
        buf.writeVarInt(packet.commands.size());
        for (CommandEntry entry : packet.commands) {
            buf.writeUtf(entry.command, 32767);
            buf.writeVarInt(entry.delayTicks);
        }
    }

    public static SaveCommandsPacket decode(FriendlyByteBuf buf) {
        boolean mainHand = buf.readBoolean();
        int size = buf.readVarInt();
        List<CommandEntry> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            String command = buf.readUtf(32767);
            int delay = buf.readVarInt();
            list.add(new CommandEntry(command, delay));
        }
        return new SaveCommandsPacket(list, mainHand);
    }

    public static void handle(SaveCommandsPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            ItemStack stack = player.getItemInHand(
                    packet.mainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
            if (!stack.isEmpty()) {
                ItemFunctionData.setCommands(stack, packet.commands);
            }
        });
        ctx.setPacketHandled(true);
    }
}
