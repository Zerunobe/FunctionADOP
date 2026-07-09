package com.example.itemfunction.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads/writes the "ItemFunctionData" compound tag on an ItemStack.
 *
 * Root tag layout (phase 1 - condition/cost tabs are stubs for now):
 * ItemFunctionData: {
 *   Commands: [ { Command: string, Delay: int }, ... ]
 *   Warning: string                (message shown when execution is blocked / as a notice)
 *   ReduceDurability: int          (cost stub - 0 = disabled)
 * }
 */
public class ItemFunctionData {

    private static final String ROOT = "ItemFunctionData";
    private static final String COMMANDS = "Commands";
    private static final String COMMAND = "Command";
    private static final String DELAY = "Delay";
    private static final String WARNING = "Warning";
    private static final String REDUCE_DURABILITY = "ReduceDurability";

    public static boolean hasData(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(ROOT);
    }

    private static CompoundTag root(ItemStack stack, boolean create) {
        CompoundTag itemTag = stack.getOrCreateTag();
        if (!itemTag.contains(ROOT) && create) {
            itemTag.put(ROOT, new CompoundTag());
        }
        return itemTag.getCompound(ROOT);
    }

    public static List<CommandEntry> getCommands(ItemStack stack) {
        List<CommandEntry> list = new ArrayList<>();
        if (!hasData(stack)) return list;
        CompoundTag data = root(stack, false);
        ListTag commandsTag = data.getList(COMMANDS, Tag.TAG_COMPOUND);
        for (int i = 0; i < commandsTag.size(); i++) {
            CompoundTag entry = commandsTag.getCompound(i);
            list.add(new CommandEntry(entry.getString(COMMAND), entry.getInt(DELAY)));
        }
        return list;
    }

    public static void setCommands(ItemStack stack, List<CommandEntry> commands) {
        CompoundTag data = root(stack, true);
        ListTag listTag = new ListTag();
        for (CommandEntry entry : commands) {
            CompoundTag tag = new CompoundTag();
            tag.putString(COMMAND, entry.command);
            tag.putInt(DELAY, entry.delayTicks);
            listTag.add(tag);
        }
        data.put(COMMANDS, listTag);
        stack.getOrCreateTag().put(ROOT, data);
    }

    public static String getWarning(ItemStack stack) {
        if (!hasData(stack)) return "";
        return root(stack, false).getString(WARNING);
    }

    public static void setWarning(ItemStack stack, String warning) {
        CompoundTag data = root(stack, true);
        data.putString(WARNING, warning == null ? "" : warning);
        stack.getOrCreateTag().put(ROOT, data);
    }

    public static int getReduceDurability(ItemStack stack) {
        if (!hasData(stack)) return 0;
        return root(stack, false).getInt(REDUCE_DURABILITY);
    }

    public static void setReduceDurability(ItemStack stack, int amount) {
        CompoundTag data = root(stack, true);
        data.putInt(REDUCE_DURABILITY, Math.max(0, amount));
        stack.getOrCreateTag().put(ROOT, data);
    }

    /**
     * Runs the configured cost + command list for this stack against the given player.
     * Condition checking is a stub for phase 1 (always passes).
     * Returns true if execution proceeded (cost paid, commands queued).
     */
    public static boolean execute(ServerPlayer player, ItemStack stack) {
        List<CommandEntry> commands = getCommands(stack);
        if (commands.isEmpty()) {
            return false;
        }

        // --- Cost step (stub: only reduce durability supported for now) ---
        int reduce = getReduceDurability(stack);
        if (reduce > 0 && stack.isDamageableItem()) {
            stack.hurtAndBreak(reduce, player, p -> {});
        }

        // --- Command step ---
        CommandScheduler.schedule(player, commands);
        return true;
    }
}
