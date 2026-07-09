package com.example.itemfunction.data;

/**
 * A single command row in the command list GUI.
 * command - the raw command string, WITHOUT the leading slash (e.g. "give @s diamond 1")
 * delayTicks - how long to wait (in ticks, 20 = 1s) after the PREVIOUS command before running this one
 */
public class CommandEntry {
    public String command;
    public int delayTicks;

    public CommandEntry(String command, int delayTicks) {
        this.command = command == null ? "" : command;
        this.delayTicks = Math.max(0, delayTicks);
    }

    public static CommandEntry empty() {
        return new CommandEntry("", 0);
    }
}
