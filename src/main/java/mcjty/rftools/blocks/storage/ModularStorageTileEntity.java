package mcjty.rftools.blocks.storage;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import mcjty.container.InventoryHelper;
import mcjty.entity.GenericTileEntity;
import mcjty.rftools.ClientInfo;
import mcjty.rftools.RFTools;
import mcjty.rftools.items.storage.StorageModuleItem;
import mcjty.rftools.network.Argument;
import mcjty.varia.Coordinate;
import mcjty.varia.GlobalCoordinate;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.Constants;

import java.util.Map;

public class ModularStorageTileEntity extends GenericTileEntity implements ISidedInventory {

    public static final String CMD_SETTINGS = "settings";

    private int[] accessible = null;
    private int maxSize = 0;

    private InventoryHelper inventoryHelper = new InventoryHelper(this, ModularStorageContainer.factory, 2 + ModularStorageContainer.MAXSIZE_STORAGE);

    private String sortMode = "";
    private String viewMode = "";
    private boolean groupMode = false;
    private String filter = "";

    private int numStacks = -1;       // -1 means no storage cell.
    private int remoteId = 0;

    @Override
    public boolean canUpdate() {
        return false;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        if (accessible == null) {
            accessible = new int[maxSize];
            for (int i = 0 ; i < maxSize ; i++) {
                accessible[i] = 2 + i;
            }
        }
        return accessible;
    }

    public boolean isGroupMode() {
        return groupMode;
    }

    public void setGroupMode(boolean groupMode) {
        this.groupMode = groupMode;
        markDirty();
    }

    public String getSortMode() {
        return sortMode;
    }

    public void setSortMode(String sortMode) {
        this.sortMode = sortMode;
        markDirty();
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
        markDirty();
    }

    public String getViewMode() {
        return viewMode;
    }

    public void setViewMode(String viewMode) {
        this.viewMode = viewMode;
        markDirty();
    }

    public int getMaxSize() {
        return maxSize;
    }

    @Override
    public boolean canInsertItem(int index, ItemStack item, int side) {
        return index >= ModularStorageContainer.SLOT_STORAGE;
    }

    @Override
    public boolean canExtractItem(int index, ItemStack item, int side) {
        return index >= ModularStorageContainer.SLOT_STORAGE;
    }

    @Override
    public int getSizeInventory() {
        return 2 + maxSize;
    }

    private boolean containsItem(int index) {
        if (isStorageAvailableRemotely(index)) {
            RemoteStorageTileEntity storageTileEntity = getRemoteStorage(remoteId);
            if (storageTileEntity == null) {
                return false;
            }
            index -= ModularStorageContainer.SLOT_STORAGE;
            ItemStack[] slots = storageTileEntity.findStacksForId(remoteId);
            if (slots == null || index >= slots.length) {
                return false;
            }
            return slots[index] != null && slots[index].stackSize > 0;
        } else {
            return inventoryHelper.containsItem(index);
        }
    }

    // On server, and if we have a remote storage module and if we're accessing a remote slot we check the remote storage.
    private boolean isStorageAvailableRemotely(int index) {
        return (!worldObj.isRemote) && remoteId != 0 && index >= ModularStorageContainer.SLOT_STORAGE;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        if (index >= getSizeInventory()) {
            return null;
        }
        if (isStorageAvailableRemotely(index)) {
            RemoteStorageTileEntity storageTileEntity = getRemoteStorage(remoteId);
            if (storageTileEntity == null) {
                return null;
            }
            index -= ModularStorageContainer.SLOT_STORAGE;
            ItemStack[] slots = storageTileEntity.findStacksForId(remoteId);
            if (slots == null || index >= slots.length) {
                return null;
            }
            return slots[index];
        }
        return inventoryHelper.getStacks()[index];
    }

    private void handleNewAmount(boolean s1, int index) {
        if (index < ModularStorageContainer.SLOT_STORAGE) {
            return;
        }
        boolean s2 = containsItem(index);
        if (s1 == s2) {
            return;
        }

        int rlold = getRenderLevel();

        if (s1) {
            numStacks--;
        } else {
            numStacks++;
        }
        int rlnew = getRenderLevel();
        if (rlold != rlnew) {
            markDirty();
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }

    public int getRenderLevel() {
        if (numStacks == -1 || maxSize == 0) {
            return -1;
        }
        return (numStacks+6) * 7 / maxSize;
    }

    public int getNumStacks() {
        return numStacks;
    }

    private ItemStack decrStackSizeHelper(int index, int amount) {
        if (isStorageAvailableRemotely(index)) {
            RemoteStorageTileEntity storageTileEntity = getRemoteStorage(remoteId);
            if (storageTileEntity == null) {
                return null;
            }
            index -= ModularStorageContainer.SLOT_STORAGE;

            ItemStack[] stacks = storageTileEntity.findStacksForId(remoteId);
            if (stacks == null || index >= stacks.length) {
                return null;
            }

            if (stacks[index] != null) {
                if (stacks[index].stackSize <= amount) {
                    ItemStack old = stacks[index];
                    stacks[index] = null;
                    storageTileEntity.markDirty();
                    return old;
                }
                ItemStack its = stacks[index].splitStack(amount);
                if (stacks[index].stackSize == 0) {
                    stacks[index] = null;
                }
                storageTileEntity.markDirty();
                return its;
            }
            return null;
        } else {
            return inventoryHelper.decrStackSize(index, amount);
        }
    }

    @Override
    public ItemStack decrStackSize(int index, int amount) {
        boolean s1 = containsItem(index);
        ItemStack itemStack = decrStackSizeHelper(index, amount);
        handleNewAmount(s1, index);

        if (index == ModularStorageContainer.SLOT_STORAGE_MODULE) {
            copyFromModule(inventoryHelper.getStacks()[ModularStorageContainer.SLOT_STORAGE_MODULE]);
        }
        return itemStack;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int index) {
        return null;
    }

    private void setInventorySlotContentsHelper(int limit, int index, ItemStack stack) {
        if (isStorageAvailableRemotely(index)) {
            RemoteStorageTileEntity storageTileEntity = getRemoteStorage(remoteId);
            if (storageTileEntity == null) {
                return;
            }
            index -= ModularStorageContainer.SLOT_STORAGE;

            ItemStack[] stacks = storageTileEntity.findStacksForId(remoteId);
            if (stacks == null || index >= stacks.length) {
                return;
            }
            stacks[index] = stack;
            if (stack != null && stack.stackSize > limit) {
                stack.stackSize = limit;
            }
            storageTileEntity.markDirty();
        } else {
            inventoryHelper.setInventorySlotContents(getInventoryStackLimit(), index, stack);
        }
    }

    public void syncToClient() {
        markDirty();
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);

    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        if (index == ModularStorageContainer.SLOT_STORAGE_MODULE) {
            copyFromModule(stack);
        } else if (index == ModularStorageContainer.SLOT_TYPE_MODULE) {
            // Make sure front side is updated.
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
        boolean s1 = containsItem(index);
        setInventorySlotContentsHelper(getInventoryStackLimit(), index, stack);
        handleNewAmount(s1, index);
    }

    @Override
    public String getInventoryName() {
        return "Modular Storage Inventory";
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
        if (index >= getSizeInventory()) {
            return false;
        }
        return true;
    }

    public void copyToModule() {
        ItemStack stack = inventoryHelper.getStacks()[ModularStorageContainer.SLOT_STORAGE_MODULE];
        if (stack == null || stack.stackSize == 0) {
            // Should be impossible.
            return;
        }
        if (stack.getItemDamage() == StorageModuleItem.STORAGE_REMOTE) {
            return;
        }
        NBTTagCompound tagCompound = stack.getTagCompound();
        if (tagCompound == null) {
            tagCompound = new NBTTagCompound();
            stack.setTagCompound(tagCompound);
        }
        writeBufferToNBT(tagCompound, ModularStorageContainer.SLOT_STORAGE);

        for (int i = ModularStorageContainer.SLOT_STORAGE ; i < inventoryHelper.getStacks().length ; i++) {
            inventoryHelper.setInventorySlotContents(0, i, null);
        }
        numStacks = -1;
        remoteId = 0;

        markDirty();
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    public void copyFromModule(ItemStack stack) {
        for (int i = ModularStorageContainer.SLOT_STORAGE ; i < inventoryHelper.getStacks().length ; i++) {
            inventoryHelper.setInventorySlotContents(0, i, null);
        }

        if (stack == null || stack.stackSize == 0) {
            setMaxSize(0);
            numStacks = -1;
            return;
        }
        remoteId = 0;
        if (stack.getItemDamage() == StorageModuleItem.STORAGE_REMOTE) {
            NBTTagCompound tagCompound = stack.getTagCompound();
            if (tagCompound == null || !tagCompound.hasKey("id")) {
                setMaxSize(0);
                numStacks = -1;
                return;
            }
            remoteId = tagCompound.getInteger("id");
            RemoteStorageTileEntity remoteStorageTileEntity = getRemoteStorage(remoteId);
            if (remoteStorageTileEntity == null) {
                setMaxSize(0);
                numStacks = -1;
                return;
            }
            ItemStack storageStack = remoteStorageTileEntity.findStorageWithId(remoteId);
            if (storageStack == null) {
                setMaxSize(0);
                numStacks = -1;
                return;
            }
            setMaxSize(StorageModuleItem.MAXSIZE[storageStack.getItemDamage()]);
        } else {
            NBTTagCompound tagCompound = stack.getTagCompound();
            if (tagCompound != null) {
                readBufferFromNBT(tagCompound, ModularStorageContainer.SLOT_STORAGE);
            }
            setMaxSize(StorageModuleItem.MAXSIZE[stack.getItemDamage()]);
        }

        updateStackCount();
    }

    private RemoteStorageTileEntity getRemoteStorage(int id) {
        World world = getWorld();
        RemoteStorageIdRegistry registry = RemoteStorageIdRegistry.getRegistry(world);
        if (registry == null) {
            if (!world.isRemote) {
                System.out.println("ModularStorageTileEntity.getRemoteStorage 1 : " + id);
            }
            return null;
        }
        GlobalCoordinate coordinate = registry.getStorage(id);
        if (coordinate == null) {
            System.out.println("ModularStorageTileEntity.getRemoteStorage 2 : " + id);

            return null;
        }
        World w = DimensionManager.getWorld(coordinate.getDimension());
        if (w == null) {
            System.out.println("ModularStorageTileEntity.getRemoteStorage 3 : " + id);

            return null;
        }
        Coordinate c = coordinate.getCoordinate();
        boolean exists = w.getChunkProvider().chunkExists(c.getX() >> 4, c.getZ() >> 4);
        if (!exists) {
            System.out.println("ModularStorageTileEntity.getRemoteStorage 4 : " + id);

            return null;
        }
        TileEntity te = w.getTileEntity(c.getX(), c.getY(), c.getZ());
        if (te instanceof RemoteStorageTileEntity) {
            return (RemoteStorageTileEntity) te;
        } else {
            System.out.println("ModularStorageTileEntity.getRemoteStorage 5 : " + id);

            return null;
        }
    }

    private void updateStackCount() {
        numStacks = 0;
        World world = getWorld();
        if ((!world.isRemote) && remoteId != 0) {
            RemoteStorageTileEntity storageTileEntity = getRemoteStorage(remoteId);
            if (storageTileEntity == null) {
                System.out.println("ModularStorageTileEntity.updateStackCount 1: " + remoteId);
                return;
            }
            ItemStack[] stacks = storageTileEntity.findStacksForId(remoteId);
            if (stacks == null) {
                System.out.println("ModularStorageTileEntity.updateStackCount 2: " + remoteId);
                return;
            }

            for (int i = 0; i < maxSize; i++) {
                if (stacks[i] != null && stacks[i].stackSize > 0) {
                    numStacks++;
                }
            }
        } else {
            for (int i = 2; i < 2 + maxSize; i++) {
                if (inventoryHelper.containsItem(i)) {
                    numStacks++;
                }
            }
        }
    }

    private World getWorld() {
        World world = worldObj;
        if (world == null) {
            if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {
                world = ClientInfo.getWorld();
            } else {
                world = DimensionManager.getWorld(0);
            }
        }
        return world;
    }

    private void setMaxSize(int ms) {
        maxSize = ms;
        inventoryHelper.setNewCount(ModularStorageContainer.SLOT_STORAGE + maxSize);
        accessible = null;

        markDirty();
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    @Override
    public void readFromNBT(NBTTagCompound tagCompound) {
        super.readFromNBT(tagCompound);
    }

    @Override
    public void readRestorableFromNBT(NBTTagCompound tagCompound) {
        super.readRestorableFromNBT(tagCompound);
        numStacks = tagCompound.getInteger("numStacks");
        maxSize = tagCompound.getInteger("maxSize");
        remoteId = tagCompound.getInteger("remoteId");
        sortMode = tagCompound.getString("sortMode");
        viewMode = tagCompound.getString("viewMode");
        groupMode = tagCompound.getBoolean("groupMode");
        filter = tagCompound.getString("filter");
        readBufferFromNBT(tagCompound, 0);
        inventoryHelper.setNewCount(ModularStorageContainer.SLOT_STORAGE + maxSize);
        accessible = null;

        updateStackCount();
    }

    private void readBufferFromNBT(NBTTagCompound tagCompound, int offset) {
        NBTTagList bufferTagList = tagCompound.getTagList("Items", Constants.NBT.TAG_COMPOUND);
        for (int i = 0 ; i < bufferTagList.tagCount() ; i++) {
            NBTTagCompound nbtTagCompound = bufferTagList.getCompoundTagAt(i);
            inventoryHelper.getStacks()[i+offset] = ItemStack.loadItemStackFromNBT(nbtTagCompound);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tagCompound) {
        super.writeToNBT(tagCompound);
    }

    @Override
    public void writeRestorableToNBT(NBTTagCompound tagCompound) {
        super.writeRestorableToNBT(tagCompound);
        writeBufferToNBT(tagCompound, 0);
        tagCompound.setInteger("numStacks", numStacks);
        tagCompound.setInteger("maxSize", maxSize);
        tagCompound.setInteger("remoteId", remoteId);
        tagCompound.setString("sortMode", sortMode);
        tagCompound.setString("viewMode", viewMode);
        tagCompound.setBoolean("groupMode", groupMode);
        tagCompound.setString("filter", filter);
    }

    private void writeBufferToNBT(NBTTagCompound tagCompound, int offset) {
        // If sendToClient is true we have to send dummy information to the client
        // so that it can remotely open gui's.
        boolean sendToClient = (!worldObj.isRemote) && offset == 0 && remoteId != 0;
System.out.println("sendToClient = " + sendToClient);

        NBTTagList bufferTagList = new NBTTagList();
        if (sendToClient) {
            for (int i = 0 ; i < ModularStorageContainer.SLOT_STORAGE ; i++) {
                ItemStack stack = inventoryHelper.getStacks()[i];
                NBTTagCompound nbtTagCompound = new NBTTagCompound();
                if (stack != null) {
                    stack.writeToNBT(nbtTagCompound);
                }
                bufferTagList.appendTag(nbtTagCompound);
            }
            RemoteStorageTileEntity storageTileEntity = getRemoteStorage(remoteId);
            if (storageTileEntity != null) {
                ItemStack[] slots = storageTileEntity.findStacksForId(remoteId);
System.out.println("slots.length = " + slots.length);
                for (ItemStack stack : slots) {
                    NBTTagCompound nbtTagCompound = new NBTTagCompound();
                    if (stack != null) {
                        stack.writeToNBT(nbtTagCompound);
                    }
                    bufferTagList.appendTag(nbtTagCompound);
                }
            }
        } else {
            for (int i = offset; i < inventoryHelper.getCount(); i++) {
                ItemStack stack = inventoryHelper.getStacks()[i];
                NBTTagCompound nbtTagCompound = new NBTTagCompound();
                if (stack != null) {
                    stack.writeToNBT(nbtTagCompound);
                }
                bufferTagList.appendTag(nbtTagCompound);
            }
        }
        tagCompound.setTag("Items", bufferTagList);
    }

    @Override
    public boolean execute(EntityPlayerMP playerMP, String command, Map<String, Argument> args) {
        boolean rc = super.execute(playerMP, command, args);
        if (rc) {
            return true;
        }
        if (CMD_SETTINGS.equals(command)) {
            setFilter(args.get("filter").getString());
            setViewMode(args.get("viewMode").getString());
            setSortMode(args.get("sortMode").getString());
            setGroupMode(args.get("groupMode").getBoolean());
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            return true;
        }
        return false;
    }


}
