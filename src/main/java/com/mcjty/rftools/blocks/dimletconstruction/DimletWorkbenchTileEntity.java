package com.mcjty.rftools.blocks.dimletconstruction;

import com.mcjty.container.InventoryHelper;
import com.mcjty.entity.GenericEnergyHandlerTileEntity;
import com.mcjty.rftools.blocks.BlockTools;
import com.mcjty.rftools.blocks.ModBlocks;
import com.mcjty.rftools.blocks.dimlets.DimletConfiguration;
import com.mcjty.rftools.items.ModItems;
import com.mcjty.rftools.items.dimlets.DimletEntry;
import com.mcjty.rftools.items.dimlets.DimletMapping;
import com.mcjty.rftools.items.dimlets.DimletType;
import com.mcjty.rftools.items.dimlets.KnownDimletConfiguration;
import com.mcjty.rftools.network.Argument;
import com.mcjty.rftools.network.PacketHandler;
import com.mcjty.rftools.network.PacketRequestIntegerFromServer;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.Map;

public class DimletWorkbenchTileEntity extends GenericEnergyHandlerTileEntity implements ISidedInventory {
    public static final String CMD_STARTEXTRACT = "startExtract";
    public static final String CMD_GETEXTRACTING = "getExtracting";
    public static final String CLIENTCMD_GETEXTRACTING = "getExtracting";

    private InventoryHelper inventoryHelper = new InventoryHelper(this, DimletWorkbenchContainer.factory, DimletWorkbenchContainer.SIZE_BUFFER + 9);

    private int extracting = 0;
    private int idToExtract = -1;
    private int inhibitCrafting = 0;

    public int getExtracting() {
        return extracting;
    }


    public DimletWorkbenchTileEntity() {
        super(DimletConfiguration.WORKBENCH_MAXENERGY, DimletConfiguration.WORKBENCH_RECEIVEPERTICK);
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return DimletWorkbenchContainer.factory.getAccessibleSlots();
    }

    @Override
    public boolean canInsertItem(int index, ItemStack item, int side) {
        return index == DimletWorkbenchContainer.SLOT_INPUT;
    }

    @Override
    public boolean canExtractItem(int index, ItemStack item, int side) {
        return index == DimletWorkbenchContainer.SLOT_OUTPUT;
    }

    @Override
    public int getSizeInventory() {
        return inventoryHelper.getStacks().length;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return inventoryHelper.getStacks()[index];
    }

    @Override
    public ItemStack decrStackSize(int index, int amount) {
        ItemStack s = inventoryHelper.decrStackSize(index, amount);
        checkCrafting();
        return s;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int index) {
        return null;
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        inventoryHelper.setInventorySlotContents(getInventoryStackLimit(), index, stack);
        if (index < DimletWorkbenchContainer.SLOT_BASE || index > DimletWorkbenchContainer.SLOT_ESSENCE) {
            return;
        }

        checkCrafting();
    }

    private void checkCrafting() {
        if (inhibitCrafting == 0) {
            if (!checkDimletCrafting()) {
                if (inventoryHelper.getStacks()[DimletWorkbenchContainer.SLOT_OUTPUT] != null) {
                    inventoryHelper.setInventorySlotContents(0, DimletWorkbenchContainer.SLOT_OUTPUT, null);
                }
            }
        }
    }

    @Override
    public String getInventoryName() {
        return "Workbench Inventory";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return true;
    }

    @Override
    public void openInventory() {

    }

    @Override
    public void closeInventory() {

    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return true;
    }

    @Override
    protected void checkStateServer() {
        if (extracting > 0) {
            extracting--;
            if (extracting == 0) {
                if (!doExtract()) {
                    // We failed due to not enough power. Try again later.
                    extracting = 10;
                }
            }
            markDirty();
        }

    }

    private boolean checkDimletCrafting() {
        ItemStack stackBase = inventoryHelper.getStacks()[DimletWorkbenchContainer.SLOT_BASE];
        if (stackBase == null) {
            return false;
        }
        ItemStack stackController = inventoryHelper.getStacks()[DimletWorkbenchContainer.SLOT_CONTROLLER];
        if (stackController == null) {
            return false;
        }
        ItemStack stackTypeController = inventoryHelper.getStacks()[DimletWorkbenchContainer.SLOT_TYPE_CONTROLLER];
        if (stackTypeController == null) {
            return false;
        }
        ItemStack stackMemory = inventoryHelper.getStacks()[DimletWorkbenchContainer.SLOT_MEMORY];
        if (stackMemory == null) {
            return false;
        }
        ItemStack stackEnergy = inventoryHelper.getStacks()[DimletWorkbenchContainer.SLOT_ENERGY];
        if (stackEnergy == null) {
            return false;
        }
        ItemStack stackEssence = inventoryHelper.getStacks()[DimletWorkbenchContainer.SLOT_ESSENCE];
        if (stackEssence == null) {
            return false;
        }

        Block essenceBlock = ((ItemBlock) stackEssence.getItem()).field_150939_a;
        NBTTagCompound essenceCompound = stackEssence.getTagCompound();

        DimletType type = DimletType.values()[stackTypeController.getItemDamage()];
        switch (type) {
            case DIMLET_BIOME:
                if (!isValidBiomeEssence(essenceBlock, essenceCompound)) return false;
                int biomeDimlet = findBiomeDimlet(essenceCompound);
                if (biomeDimlet == -1) {
                    return false;
                }
                if (!matchDimletRecipe(biomeDimlet, stackController, stackMemory, stackEnergy)) {
                    return false;
                }
                inventoryHelper.setInventorySlotContents(1, DimletWorkbenchContainer.SLOT_OUTPUT, new ItemStack(ModItems.knownDimlet, 1, biomeDimlet));
                break;
            case DIMLET_FOLIAGE:
            case DIMLET_LIQUID:
            case DIMLET_MATERIAL:
            case DIMLET_MOBS:
            case DIMLET_SKY:
            case DIMLET_STRUCTURE:
            case DIMLET_TERRAIN:
            case DIMLET_FEATURE:
            case DIMLET_TIME:
            case DIMLET_DIGIT:
            case DIMLET_EFFECT:
            case DIMLET_SPECIAL:
            case DIMLET_CONTROLLER:
                return false;
        }

        return true;
    }

    public void craftDimlet() {
        inhibitCrafting++;
        inventoryHelper.decrStackSize(DimletWorkbenchContainer.SLOT_BASE, 1);
        inventoryHelper.decrStackSize(DimletWorkbenchContainer.SLOT_CONTROLLER, 1);
        inventoryHelper.decrStackSize(DimletWorkbenchContainer.SLOT_TYPE_CONTROLLER, 1);
        inventoryHelper.decrStackSize(DimletWorkbenchContainer.SLOT_ENERGY, 1);
        inventoryHelper.decrStackSize(DimletWorkbenchContainer.SLOT_MEMORY, 1);
        inventoryHelper.decrStackSize(DimletWorkbenchContainer.SLOT_ESSENCE, 1);
        inhibitCrafting--;
        checkCrafting();
    }

    private boolean matchDimletRecipe(int id, ItemStack stackController, ItemStack stackMemory, ItemStack stackEnergy) {
        DimletEntry dimletEntry = KnownDimletConfiguration.idToDimlet.get(id);
        int rarity = dimletEntry.getRarity();
        if (stackController.getItemDamage() != rarity) {
            return false;
        }
        int level;
        if (rarity <= 1) {
            level = 0;
        } else if (rarity <= 3) {
            level = 1;
        } else {
            level = 2;
        }
        if (stackMemory.getItemDamage() != level) {
            return false;
        }
        if (stackEnergy.getItemDamage() != level) {
            return false;
        }

        return true;
    }

    private int findBiomeDimlet(NBTTagCompound essenceCompound) {
        int biomeID = essenceCompound.getInteger("biome");
        for (Map.Entry<Integer, BiomeGenBase> entry : DimletMapping.idToBiome.entrySet()) {
            if (entry.getValue().biomeID == biomeID) {
                return entry.getKey();
            }
        }
        return -1;
    }

    private boolean isValidBiomeEssence(Block essenceBlock, NBTTagCompound essenceCompound) {
        if (essenceBlock != ModBlocks.biomeAbsorberBlock) {
            return false;
        }
        if (essenceCompound == null) {
            return false;
        }
        int absorbing = essenceCompound.getInteger("absorbing");
        int biome = essenceCompound.getInteger("biome");
        if (absorbing > 0 || biome == -1) {
            return false;
        }
        return true;
    }

    private void startExtracting() {
        if (extracting > 0) {
            // Already extracting
            return;
        }
        ItemStack stack = inventoryHelper.getStacks()[DimletWorkbenchContainer.SLOT_INPUT];
        if (stack != null) {
            if (ModItems.knownDimlet.equals(stack.getItem())) {
                int id = stack.getItemDamage();
                if (!KnownDimletConfiguration.craftableDimlets.contains(id)) {
                    extracting = 64;
                    idToExtract = id;
                    inventoryHelper.decrStackSize(DimletWorkbenchContainer.SLOT_INPUT, 1);
                    markDirty();
                }
            }
        }
    }

    private boolean doExtract() {
        int rf = DimletConfiguration.rfExtractOperation;
        if (getEnergyStored(ForgeDirection.DOWN) < rf) {
            // Not enough energy.
            return false;
        }
        extractEnergy(ForgeDirection.DOWN, rf, false);

        float factor = getInfusedFactor();

        DimletEntry entry = KnownDimletConfiguration.idToDimlet.get(idToExtract);

        if (extractSuccess(factor)) {
            mergeItemOrThrowInWorld(new ItemStack(ModItems.dimletBaseItem));
        }

        int rarity = entry.getRarity();

        if (extractSuccess(factor)) {
            mergeItemOrThrowInWorld(new ItemStack(ModItems.dimletTypeControllerItem, 1, entry.getKey().getType().ordinal()));
        }

        int level;
        if (rarity <= 1) {
            level = 0;
        } else if (rarity <= 3) {
            level = 1;
        } else {
            level = 2;
        }
        if (extractSuccess(factor)) {
            mergeItemOrThrowInWorld(new ItemStack(ModItems.dimletMemoryUnitItem, 1, level));
        } else {
            factor += 0.1f;     // If this failed we increase our chances a bit
        }

        if (extractSuccess(factor)) {
            mergeItemOrThrowInWorld(new ItemStack(ModItems.dimletEnergyModuleItem, 1, level));
        } else {
            factor += 0.1f;     // If this failed we increase our chances a bit
        }

        if (extractSuccess(factor)) {
            mergeItemOrThrowInWorld(new ItemStack(ModItems.dimletControlCircuitItem, 1, rarity));
        }

        idToExtract = -1;
        markDirty();

        return true;
    }

    private boolean extractSuccess(float factor) {
        return (worldObj.rand.nextFloat() * (1.0f - factor) + (0.71f * factor)) > 0.7f;
    }

    private void mergeItemOrThrowInWorld(ItemStack stack) {
        int notInserted = inventoryHelper.mergeItemStack(this, stack, DimletWorkbenchContainer.SLOT_BUFFER, DimletWorkbenchContainer.SLOT_BUFFER + DimletWorkbenchContainer.SIZE_BUFFER, null);
        if (notInserted > 0) {
            BlockTools.spawnItemStack(worldObj, xCoord, yCoord, zCoord, stack);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tagCompound) {
        super.readFromNBT(tagCompound);
    }

    @Override
    public void readRestorableFromNBT(NBTTagCompound tagCompound) {
        super.readRestorableFromNBT(tagCompound);
        readBufferFromNBT(tagCompound);
        extracting = tagCompound.getInteger("extracting");
        idToExtract = tagCompound.getInteger("idToExtract");
    }

    private void readBufferFromNBT(NBTTagCompound tagCompound) {
        NBTTagList bufferTagList = tagCompound.getTagList("Items", Constants.NBT.TAG_COMPOUND);
        for (int i = 0 ; i < bufferTagList.tagCount() ; i++) {
            NBTTagCompound nbtTagCompound = bufferTagList.getCompoundTagAt(i);
            inventoryHelper.getStacks()[i] = ItemStack.loadItemStackFromNBT(nbtTagCompound);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tagCompound) {
        super.writeToNBT(tagCompound);
    }

    @Override
    public void writeRestorableToNBT(NBTTagCompound tagCompound) {
        super.writeRestorableToNBT(tagCompound);
        writeBufferToNBT(tagCompound);
        tagCompound.setInteger("extracting", extracting);
        tagCompound.setInteger("idToExtract", idToExtract);
    }

    private void writeBufferToNBT(NBTTagCompound tagCompound) {
        NBTTagList bufferTagList = new NBTTagList();
        for (int i = 0 ; i < inventoryHelper.getStacks().length ; i++) {
            ItemStack stack = inventoryHelper.getStacks()[i];
            NBTTagCompound nbtTagCompound = new NBTTagCompound();
            if (stack != null) {
                stack.writeToNBT(nbtTagCompound);
            }
            bufferTagList.appendTag(nbtTagCompound);
        }
        tagCompound.setTag("Items", bufferTagList);
    }

    @Override
    public boolean execute(String command, Map<String, Argument> args) {
        boolean rc = super.execute(command, args);
        if (rc) {
            return true;
        }
        if (CMD_STARTEXTRACT.equals(command)) {
            startExtracting();
            return true;
        }
        return false;
    }

    // Request the extracting amount from the server. This has to be called on the client side.
    public void requestExtractingFromServer() {
        PacketHandler.INSTANCE.sendToServer(new PacketRequestIntegerFromServer(xCoord, yCoord, zCoord,
                CMD_GETEXTRACTING,
                CLIENTCMD_GETEXTRACTING));
    }

    @Override
    public Integer executeWithResultInteger(String command, Map<String, Argument> args) {
        Integer rc = super.executeWithResultInteger(command, args);
        if (rc != null) {
            return rc;
        }
        if (CMD_GETEXTRACTING.equals(command)) {
            return extracting;
        }
        return null;
    }

    @Override
    public boolean execute(String command, Integer result) {
        boolean rc = super.execute(command, result);
        if (rc) {
            return true;
        }
        if (CLIENTCMD_GETEXTRACTING.equals(command)) {
            extracting = result;
            return true;
        }
        return false;
    }

}