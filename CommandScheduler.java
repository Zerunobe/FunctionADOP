package com.example.itemfunction.data;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

/**
 * Executes a player's configured command list one at a time, honoring each
 * command's individual delay (in ticks) before it fires.
 */
public class CommandScheduler {

    private static class Job {
        final UUID playerId;
        final Deque<CommandEntry> remaining;
        int ticksUntilNext;

        Job(UUID playerId, List<CommandEntry> commands) {
            this.playerId = playerId;
            this.remaining = new ArrayDeque<>(commands);
            // first command still honors its own delay before firing
            this.ticksUntilNext = this.remaining.isEmpty() ? 0 : this.remaining.peek().delayTicks;
        }
    }

    private static final List<Job> ACTIVE_JOBS = new ArrayList<>();

    public static void schedule(ServerPlayer player, List<CommandEntry> commands) {
        ACTIVE_JOBS.add(new Job(player.getUUID(), commands));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (ACTIVE_JOBS.isEmpty()) return;

        MinecraftServer server = ACTIVE_JOBS.isEmpty() ? null : findServer();
        if (server == null) return;

        List<Job> finished = new ArrayList<>();
        for (Job job : ACTIVE_JOBS) {
            if (job.remaining.isEmpty()) {
                finished.add(job);
                continue;
            }
            if (job.ticksUntilNext > 0) {
                job.ticksUntilNext--;
                continue;
            }

            CommandEntry next = job.remaining.poll();
            ServerPlayer player = server.getPlayerList().getPlayer(job.playerId);
            if (player != null && next != null && !next.command.isBlank()) {
                CommandSourceStack source = player.createCommandSourceStack().withSuppressedOutput();
                server.getCommands().performPrefixedCommand(source, next.command);
            }

            if (!job.remaining.isEmpty()) {
                job.ticksUntilNext = job.remaining.peek().delayTicks;
            } else {
                finished.add(job);
            }
        }
        ACTIVE_JOBS.removeAll(finished);
    }

    private static MinecraftServer findServer() {
        // Grabbed lazily from any online player's server reference via the event; kept simple for phase 1.
        return net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
    }
}
