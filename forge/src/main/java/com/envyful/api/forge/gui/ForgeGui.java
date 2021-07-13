package com.envyful.api.forge.gui;

import com.envyful.api.forge.gui.item.EmptySlot;
import com.envyful.api.forge.gui.pane.ForgeSimplePane;
import com.envyful.api.forge.player.ForgeEnvyPlayer;
import com.envyful.api.gui.Gui;
import com.envyful.api.gui.item.Displayable;
import com.envyful.api.gui.pane.Pane;
import com.envyful.api.player.EnvyPlayer;
import com.envyful.api.player.PlayerManager;
import com.envyful.api.type.Pair;
import com.google.common.collect.Lists;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketOpenWindow;
import net.minecraft.network.play.server.SPacketSetSlot;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import scala.xml.dtd.REQUIRED;

import java.util.List;

/**
 *
 * Forge implementation of the {@link Gui} interface.
 *
 */
public class ForgeGui implements Gui {

    private final ITextComponent title;
    private final int height;
    private final PlayerManager<ForgeEnvyPlayer, EntityPlayerMP> playerManager;
    private final ForgeSimplePane parentPane;
    private final ForgeSimplePane[] panes;

    private final List<ForgeGuiContainer> containers = Lists.newArrayList();

    ForgeGui(String title, int height, PlayerManager<ForgeEnvyPlayer, EntityPlayerMP> playerManager, Pane... panes) {
        this.title = new TextComponentString(title);
        this.height = height;
        this.playerManager = playerManager;
        this.parentPane = (ForgeSimplePane) new ForgeSimplePane.Builder().height(height).topLeftX(0).topLeftY(0).width(9).build();
        this.panes = new ForgeSimplePane[panes.length];
        int i = 0;

        for (Pane pane : panes) {
            if (!(pane instanceof ForgeSimplePane)) {
                continue;
            }

            this.panes[i] = (ForgeSimplePane) pane;
            ++i;
        }
    }

    @Override
    public void open(EnvyPlayer<?> player) {
        if (!(player instanceof ForgeEnvyPlayer)) {
            return;
        }

        EntityPlayerMP parent = ((ForgeEnvyPlayer) player).getParent();
        ForgeGuiContainer container = new ForgeGuiContainer(this, parent);

        parent.closeContainer();
        parent.openContainer = container;
        parent.currentWindowId = 1;
        parent.connection.sendPacket(new SPacketOpenWindow(parent.currentWindowId, "minecraft:container", this.title, 9 * this.height));
        container.detectAndSendChanges();
        parent.sendAllContents(container, container.inventoryItemStacks);

        this.containers.add(container);
        ForgeGuiTracker.addGui(player, this);
    }

    public void update() {
        for (ForgeGuiContainer value : this.containers) {
            value.update(this.panes, false);
        }
    }

    /**
     *
     * Forge container class for the GUI
     *
     */
    private final class ForgeGuiContainer extends Container {

        private final ForgeGui gui;
        private final EntityPlayerMP player;
        private final List<EmptySlot> emptySlots = Lists.newArrayList();

        public ForgeGuiContainer(ForgeGui gui, EntityPlayerMP player) {
            this.windowId = 1;
            this.gui = gui;
            this.player = player;

            this.update(this.gui.panes, true);
        }

        public void update(ForgeSimplePane[] panes, boolean force) {
            this.inventorySlots = Lists.newArrayList();
            this.inventoryItemStacks = NonNullList.create();
            boolean createEmptySlots = this.emptySlots.isEmpty();

            if (!createEmptySlots) {
                this.inventorySlots.addAll(this.emptySlots);
            }

            for (int i = 0; i < (9 * this.gui.height); i++) {
                if (createEmptySlots) {
                    EmptySlot emptySlot = new EmptySlot(this.gui.parentPane, i);

                    this.emptySlots.add(emptySlot);
                    this.inventorySlots.add(emptySlot);
                }

                this.inventoryItemStacks.add(ItemStack.EMPTY);
            }

            for (ForgeSimplePane pane : panes) {
                if (pane == null) {
                    continue;
                }

                for (int y = 0; y < pane.getItems().length; y++) {
                    ForgeSimplePane.SimpleDisplayableSlot[] row = pane.getItems()[y];

                    for (int x = 0; x < row.length; x++) {
                        ForgeSimplePane.SimpleDisplayableSlot item = row[x];

                        int index = pane.updateIndex((9 * y) + x);

                        this.inventorySlots.set(index, item);
                        this.inventoryItemStacks.set(index, item.getStack());
                    }
                }
            }

            for (int i = 9; i < 36; i++) {
                ItemStack itemStack = player.inventory.mainInventory.get(i);
                inventorySlots.add(new Slot(player.inventory, i, 0, 0));
                inventoryItemStacks.add(itemStack);
            }
            // Sets the slots for the hotbar.
            for (int i = 0; i < 9; i++) {
                ItemStack itemStack = player.inventory.mainInventory.get(i);
                inventorySlots.add(new Slot(player.inventory, i, 0, 0));
                inventoryItemStacks.add(itemStack);
            }

            if (force || ForgeGuiTracker.requiresUpdate(this.player)) {
                this.refreshPlayerContents();
            }
        }

        @Override
        public boolean canInteractWith(EntityPlayer playerIn) {
            return true;
        }

        @Override
        public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean canMergeSlot(ItemStack stack, Slot slotIn) {
            return false;
        }

        @Override
        protected boolean mergeItemStack(ItemStack stack, int startIndex, int endIndex, boolean reverseDirection) {
            return false;
        }

        @Override
        public void putStackInSlot(int slotID, ItemStack stack) {}

        @Override
        public boolean canDragIntoSlot(Slot slotIn) {
            return false;
        }

        @Override
        public ItemStack slotClick(int slot, int dragType, ClickType clickTypeIn, EntityPlayer player) {
            if (slot <= -1) {
                return ItemStack.EMPTY;
            }

            this.refreshPlayerContents();

            if (clickTypeIn == ClickType.CLONE || clickTypeIn == ClickType.QUICK_CRAFT) {
                this.clearPlayerCursor();
                return ItemStack.EMPTY;
            }

            Displayable.ClickType clickType = this.convertClickType(dragType);

            if (clickType == null) {
                return ItemStack.EMPTY;
            }

            EnvyPlayer<?> envyPlayer = this.gui.playerManager.getPlayer((EntityPlayerMP) player);

            if (envyPlayer == null) {
                return ItemStack.EMPTY;
            }

            int xPos = slot % 9;
            int yPos = slot / 9;

            for (ForgeSimplePane pane : this.gui.panes) {
                if (!pane.inPane(xPos, yPos)) {
                    continue;
                }

                Pair<Integer, Integer> panePosition = pane.convertXandY(xPos, yPos);

                ForgeSimplePane.SimpleDisplayableSlot simpleDisplayableSlot = pane.getItems()[panePosition.getY()][panePosition.getX()];
                simpleDisplayableSlot.getDisplayable().onClick(envyPlayer, clickType);
                ForgeGuiTracker.enqueueUpdate(envyPlayer);
            }

            return ItemStack.EMPTY;
        }

        private Displayable.ClickType convertClickType(int id) {
            switch(id) {
                case 0 : return Displayable.ClickType.LEFT;
                case 1 : return Displayable.ClickType.RIGHT;
                case 2 : return Displayable.ClickType.MIDDLE;
                default : return null;
            }
        }

        private void refreshPlayerContents() {
            this.player.sendAllContents(this, this.inventoryItemStacks);
            ForgeGuiTracker.dequeueUpdate(this.player);
        }

        private void clearPlayerCursor() {
            SPacketSetSlot setCursorSlot = new SPacketSetSlot(-1, 0, ItemStack.EMPTY);
            player.connection.sendPacket(setCursorSlot);
        }

        @Override
        public void onContainerClosed(EntityPlayer playerIn) {
            super.onContainerClosed(playerIn);

            EnvyPlayer<?> player = this.gui.playerManager.getPlayer(playerIn.getUniqueID());
            ForgeGuiTracker.removePlayer(player);
        }
    }
}