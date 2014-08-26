package com.mcjty.rftools.gui;

import com.mcjty.gui.*;
import com.mcjty.gui.Label;
import com.mcjty.gui.Panel;
import com.mcjty.gui.Window;
import com.mcjty.rftools.BlockInfo;
import com.mcjty.rftools.Coordinate;
import com.mcjty.rftools.RFTools;
import com.mcjty.rftools.blocks.RFMonitorBlock;
import com.mcjty.rftools.blocks.RFMonitorBlockTileEntity;
import com.mcjty.rftools.network.PacketHandler;
import com.mcjty.rftools.network.PacketRFMonitor;
import net.minecraft.block.Block;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;

import java.awt.*;
import java.util.*;
import java.util.List;

public class GuiRFMonitor extends GuiScreen {
    public static final String MODE_MORE = "More";
    public static final String MODE_LESS = "Less";
    private RFMonitorBlock monitorBlock;
    private RFMonitorBlockTileEntity monitorBlockTileEntity;

    private Window window;
    private WidgetList list;
    private ChoiceLabel alarmModeChoiceLabel;
    private ScrollableLabel alarmLabel;
    private int listDirty;

    public static final int TEXT_COLOR = 0x19979f;

    // A copy of the adjacent blocks we're currently showing
    private List<BlockInfo> adjacentBlocks;

    private static final ResourceLocation iconLocation = new ResourceLocation(RFTools.MODID, "textures/gui/networkMonitorBack.png");

    /** The X size of the window in pixels. */
    private int xSize = 256;
    /** The Y size of the window in pixels. */
    private int ySize = 180;


    public GuiRFMonitor(RFMonitorBlock monitorBlock, RFMonitorBlockTileEntity monitorBlockTileEntity) {
        this.monitorBlock = monitorBlock;
        this.monitorBlockTileEntity = monitorBlockTileEntity;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void initGui() {
        super.initGui();
        int k = (this.width - this.xSize) / 2;
        int l = (this.height - this.ySize) / 2;

        list = new WidgetList(mc, this).setRowheight(16).addSelectionEvent(new SelectionEvent() {
            @Override
            public void select(Widget parent, int index) {
                setSelectedBlock(index);
            }
        });
        listDirty = 0;
        Slider listSlider = new Slider(mc, this).setDesiredWidth(15).setVertical().setScrollable(list);
        Panel listPanel = new Panel(mc, this).setLayout(new HorizontalLayout()).addChild(list).addChild(listSlider);

        alarmModeChoiceLabel = new ChoiceLabel(mc, this).addChoices(MODE_LESS, MODE_MORE).setDesiredWidth(60).setDesiredHeight(15).
                setHorizontalAlignment(HorizontalAlignment.ALIGN_CENTER).
                setVerticalAlignment(VerticalAlignment.ALIGN_CENTER).
                addChoiceEvent(new ChoiceEvent() {
                    @Override
                    public void choiceChanged(Widget parent, String newChoice) {
                        changeAlarmMode(MODE_MORE.equals(newChoice));
                    }
                });
        if (monitorBlockTileEntity.isAlarmIfMore()) {
            alarmModeChoiceLabel.setChoice(MODE_MORE);
        }

        alarmLabel = new ScrollableLabel(mc, this).setSuffix("%").setDesiredWidth(30).setRealMinimum(0).setRealMaximum(100).
                setRealValue(monitorBlockTileEntity.getAlarmLevel()).
                addValueEvent(new ValueEvent() {
                    @Override
                    public void valueChanged(Widget parent, int newValue) {
                        changeAlarmValue(newValue);
                    }
                });
        Slider alarmSlider = new Slider(mc, this).setDesiredHeight(15).setHorizontal().setScrollable(alarmLabel);
        Panel alarmPanel = new Panel(mc, this).setLayout(new HorizontalLayout()).addChild(alarmModeChoiceLabel).addChild(alarmSlider).addChild(alarmLabel).setDesiredHeight(20);

        Widget toplevel = new Panel(mc, this).setBackground(iconLocation).setLayout(new VerticalLayout()).addChild(listPanel).addChild(alarmPanel);
        toplevel.setBounds(new Rectangle(k, l, xSize, ySize));
        window = new Window(this, toplevel);
    }

    private void changeAlarmMode(boolean mode) {
        int alarmLevel = alarmLabel.getRealValue();
        monitorBlockTileEntity.setAlarm(mode, alarmLevel);
        sendChangeToServer(mode, alarmLevel);
    }

    private void changeAlarmValue(int newValue) {
        boolean mode = MODE_MORE.equals(alarmModeChoiceLabel.getCurrentChoice());
        monitorBlockTileEntity.setAlarm(mode, newValue);
        sendChangeToServer(mode, newValue);
    }

    private void refreshList() {
    }

    private void setSelectedBlock(int index) {
        if (index != -1) {
            Coordinate c = adjacentBlocks.get(index).getCoordinate();
            monitorBlockTileEntity.setMonitor(c);
            sendChangeToServer(c);
        } else {
            monitorBlockTileEntity.setInvalid();
            sendChangeToServer(Coordinate.INVALID);
        }
    }

    private void sendChangeToServer(Coordinate c) {
        PacketHandler.INSTANCE.sendToServer(new PacketRFMonitor(monitorBlockTileEntity.xCoord, monitorBlockTileEntity.yCoord, monitorBlockTileEntity.zCoord, c));
    }

    private void sendChangeToServer(boolean mode, int level) {
        System.out.println("mode = " + mode);
        System.out.println("level = " + level);
        PacketHandler.INSTANCE.sendToServer(new PacketRFMonitor(monitorBlockTileEntity.xCoord, monitorBlockTileEntity.yCoord, monitorBlockTileEntity.zCoord, mode, level));
    }

    private void populateList() {
        List<BlockInfo> newAdjacentBlocks = monitorBlock.getAdjacentBlocks();
        if (newAdjacentBlocks.equals(adjacentBlocks)) {
            refreshList();
            return;
        }

        adjacentBlocks = new ArrayList<BlockInfo>(newAdjacentBlocks);
        list.removeChildren();

        int index = 0, sel = -1;
        for (BlockInfo blockInfo : adjacentBlocks) {
            Block block = blockInfo.getBlock();
            Coordinate coordinate = blockInfo.getCoordinate();

            int color = TEXT_COLOR;

            String displayName = blockInfo.getReadableName(mc.theWorld);

            Panel panel = new Panel(mc, this).setLayout(new HorizontalLayout());
            panel.addChild(new BlockRender(mc, this).setRenderItem(block));
            panel.addChild(new Label(mc, this).setText(displayName).setColor(color).setDesiredWidth(120));
            panel.addChild(new Label(mc, this).setText(coordinate.toString()).setColor(color));
            list.addChild(panel);

            if (coordinate.getX() == monitorBlockTileEntity.getMonitorX() &&
                    coordinate.getY() == monitorBlockTileEntity.getMonitorY() &&
                    coordinate.getZ() == monitorBlockTileEntity.getMonitorZ()) {
                sel = index;
            }
            index++;
        }

        list.setSelected(sel);
    }

    @Override
    protected void mouseClicked(int x, int y, int button) {
        super.mouseClicked(x, y, button);
        window.mouseClicked(x, y, button);
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        window.handleMouseInput();
    }

    @Override
    protected void mouseMovedOrUp(int x, int y, int button) {
        super.mouseMovedOrUp(x, y, button);
        window.mouseMovedOrUp(x, y, button);
    }


    @Override
    public void drawScreen(int xSize_lo, int ySize_lo, float par3) {
        super.drawScreen(xSize_lo, ySize_lo, par3);

        listDirty--;
        if (listDirty <= 0) {
            populateList();
            listDirty = 5;
        }

        window.draw();
    }
}
