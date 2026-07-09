package com.example.itemfunction.client;

import com.example.itemfunction.data.CommandEntry;
import com.example.itemfunction.data.ItemFunctionData;
import com.example.itemfunction.network.ModNetwork;
import com.example.itemfunction.network.SaveCommandsPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 1 command editor.
 * Each row = [ up ][ down ][ command EditBox ][ delay EditBox ][ X ]
 * Bottom bar = [ + Add ][ Done ]
 * Mouse wheel scrolls when the list overflows the visible area.
 */
public class CommandListScreen extends Screen {

    private static final int ROW_HEIGHT = 22;
    private static final int PANEL_WIDTH = 300;
    private static final int VISIBLE_ROWS = 6;

    private final ItemStack editingStack;
    private final List<CommandEntry> commands = new ArrayList<>();
    private final List<RowWidgets> rowWidgets = new ArrayList<>();

    private int scrollOffset = 0;
    private int listTop;
    private int listLeft;

    private static class RowWidgets {
        EditBox commandBox;
        EditBox delayBox;
        Button upButton;
        Button downButton;
        Button deleteButton;
    }

    public CommandListScreen(ItemStack editingStack) {
        super(Component.literal("Command Editor"));
        this.editingStack = editingStack;
        this.commands.addAll(ItemFunctionData.getCommands(editingStack));
    }

    @Override
    protected void init() {
        super.init();
        listLeft = (this.width - PANEL_WIDTH) / 2;
        listTop = 40;
        rebuildRows();

        // + button
        this.addRenderableWidget(Button.builder(Component.literal("+"), b -> {
            commands.add(CommandEntry.empty());
            scrollOffset = Math.max(0, commands.size() - VISIBLE_ROWS);
            rebuildRows();
        }).bounds(listLeft, listTop + VISIBLE_ROWS * ROW_HEIGHT + 10, 40, 20).build());

        // Done button
        this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> {
            save();
            onClose();
        }).bounds(listLeft + PANEL_WIDTH - 60, listTop + VISIBLE_ROWS * ROW_HEIGHT + 10, 60, 20).build());
    }

    private void rebuildRows() {
        // remove old widgets
        for (RowWidgets rw : rowWidgets) {
            this.removeWidget(rw.commandBox);
            this.removeWidget(rw.delayBox);
            this.removeWidget(rw.upButton);
            this.removeWidget(rw.downButton);
            this.removeWidget(rw.deleteButton);
        }
        rowWidgets.clear();

        int visibleCount = Math.min(VISIBLE_ROWS, commands.size());
        for (int visibleIndex = 0; visibleIndex < visibleCount; visibleIndex++) {
            int dataIndex = scrollOffset + visibleIndex;
            if (dataIndex >= commands.size()) break;
            addRowWidgets(dataIndex, visibleIndex);
        }
    }

    private void addRowWidgets(int dataIndex, int visibleIndex) {
        int y = listTop + visibleIndex * ROW_HEIGHT;
        CommandEntry entry = commands.get(dataIndex);
        RowWidgets rw = new RowWidgets();

        rw.upButton = Button.builder(Component.literal("^"), b -> moveRow(dataIndex, -1))
                .bounds(listLeft, y, 16, 18).build();
        rw.downButton = Button.builder(Component.literal("v"), b -> moveRow(dataIndex, 1))
                .bounds(listLeft + 18, y, 16, 18).build();

        rw.commandBox = new EditBox(this.font, listLeft + 38, y, 170, 18, Component.literal("command"));
        rw.commandBox.setMaxLength(256);
        rw.commandBox.setValue(entry.command);
        rw.commandBox.setResponder(text -> entry.command = text);

        rw.delayBox = new EditBox(this.font, listLeft + 212, y, 40, 18, Component.literal("delay"));
        rw.delayBox.setMaxLength(6);
        rw.delayBox.setValue(String.valueOf(entry.delayTicks));
        rw.delayBox.setResponder(text -> {
            try {
                entry.delayTicks = Math.max(0, Integer.parseInt(text.isBlank() ? "0" : text));
            } catch (NumberFormatException ignored) {
                // keep previous value on bad input
            }
        });

        rw.deleteButton = Button.builder(Component.literal("X"), b -> {
            commands.remove(dataIndex);
            if (scrollOffset > 0 && scrollOffset >= commands.size()) scrollOffset--;
            rebuildRows();
        }).bounds(listLeft + 256, y, 18, 18).build();

        this.addRenderableWidget(rw.upButton);
        this.addRenderableWidget(rw.downButton);
        this.addRenderableWidget(rw.commandBox);
        this.addRenderableWidget(rw.delayBox);
        this.addRenderableWidget(rw.deleteButton);
        rowWidgets.add(rw);
    }

    private void moveRow(int dataIndex, int direction) {
        int target = dataIndex + direction;
        if (target < 0 || target >= commands.size()) return;
        CommandEntry tmp = commands.get(dataIndex);
        commands.set(dataIndex, commands.get(target));
        commands.set(target, tmp);
        rebuildRows();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (commands.size() > VISIBLE_ROWS) {
            int maxOffset = commands.size() - VISIBLE_ROWS;
            scrollOffset = Math.max(0, Math.min(maxOffset, scrollOffset - (int) Math.signum(delta)));
            rebuildRows();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void save() {
        ModNetwork.CHANNEL.sendToServer(new SaveCommandsPacket(commands, true));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(this.font, "Command Editor", this.width / 2, 20, 0xFFFFFF);

        // simple panel background behind the rows
        int visibleCount = Math.min(VISIBLE_ROWS, commands.size());
        int panelHeight = Math.max(1, visibleCount) * ROW_HEIGHT;
        graphics.fill(listLeft - 4, listTop - 4, listLeft + PANEL_WIDTH + 4, listTop + panelHeight + 4, 0x88000000);

        if (commands.isEmpty()) {
            graphics.drawCenteredString(this.font, "No commands yet - press + to add one",
                    this.width / 2, listTop + 8, 0xAAAAAA);
        }

        // simple scrollbar indicator
        if (commands.size() > VISIBLE_ROWS) {
            int trackHeight = VISIBLE_ROWS * ROW_HEIGHT;
            int maxOffset = commands.size() - VISIBLE_ROWS;
            int thumbHeight = Math.max(10, trackHeight * VISIBLE_ROWS / commands.size());
            int thumbY = listTop + (trackHeight - thumbHeight) * scrollOffset / Math.max(1, maxOffset);
            RenderSystem.disableDepthTest();
            graphics.fill(listLeft + PANEL_WIDTH + 6, listTop, listLeft + PANEL_WIDTH + 9, listTop + trackHeight, 0x66FFFFFF);
            graphics.fill(listLeft + PANEL_WIDTH + 6, thumbY, listLeft + PANEL_WIDTH + 9, thumbY + thumbHeight, 0xFFFFFFFF);
            RenderSystem.enableDepthTest();
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
