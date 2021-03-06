package mcjty.rftools.blocks.powercell;

import cofh.api.energy.IEnergyConnection;
import cofh.api.energy.IEnergyContainerItem;
import cofh.api.energy.IEnergyProvider;
import cofh.api.energy.IEnergyReceiver;
import mcjty.lib.api.MachineInformation;
import mcjty.lib.api.smartwrench.SmartWrenchSelector;
import mcjty.lib.container.DefaultSidedInventory;
import mcjty.lib.container.InventoryHelper;
import mcjty.lib.entity.GenericTileEntity;
import mcjty.lib.network.Argument;
import mcjty.lib.varia.BlockPosTools;
import mcjty.lib.varia.EnergyTools;
import mcjty.lib.varia.GlobalCoordinate;
import mcjty.lib.varia.Logging;
import mcjty.rftools.blocks.teleporter.TeleportationTools;
import mcjty.rftools.items.powercell.PowerCellCardItem;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import java.util.Map;
import java.util.Set;

import static mcjty.rftools.blocks.powercell.PowerCellConfiguration.advancedFactor;
import static mcjty.rftools.blocks.powercell.PowerCellConfiguration.simpleFactor;

public class PowerCellTileEntity extends GenericTileEntity implements IEnergyProvider, IEnergyReceiver,
        DefaultSidedInventory, ITickable, SmartWrenchSelector, MachineInformation {

    public static String CMD_SETNONE = "setNone";
    public static String CMD_SETINPUT = "setInput";
    public static String CMD_SETOUTPUT = "setOutput";
    public static String CMD_CLEARSTATS = "clearStats";

    private static final String[] TAGS = new String[]{"rfpertick_out", "rfpertick_in", "rftotal_in", "rftotal_out"};
    private static final String[] TAG_DESCRIPTIONS = new String[] {
            "The current RF/t output given by this block (last 2 seconds)",
            "The current RF/t input received by this block (last 2 seconds)",
            "The total RF/t output given by this block",
            "The current RF/t input received by this block"};

    private InventoryHelper inventoryHelper = new InventoryHelper(this, PowerCellContainer.factory, 3);

    private int networkId = -1;

    // Only used when this block is not part of a network
    private int energy = 0;

    // Total amount of energy extracted from this block (local or not)
    private int totalExtracted = 0;
    // Total amount of energy inserted in this block (local or not)
    private int totalInserted = 0;

    private int lastRfPerTickIn = 0;
    private int lastRfPerTickOut = 0;
    private int powerIn = 0;
    private int powerOut = 0;
    private long lastTime = 0;

    public enum Mode implements IStringSerializable {
        MODE_NONE("none"),
        MODE_INPUT("input"),   // Blue
        MODE_OUTPUT("output"); // Yellow

        private final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }
    private Mode modes[] = new Mode[] { Mode.MODE_NONE, Mode.MODE_NONE, Mode.MODE_NONE, Mode.MODE_NONE, Mode.MODE_NONE, Mode.MODE_NONE };

    public PowerCellTileEntity() {
        super();
    }

    @Override
    public int getTagCount() {
        return TAGS.length;
    }

    @Override
    public String getTagName(int index) {
        return TAGS[index];
    }

    @Override
    public String getTagDescription(int index) {
        return TAG_DESCRIPTIONS[index];
    }

    @Override
    public String getData(int index, long millis) {
        switch (index) {
            case 0: return lastRfPerTickOut + "RF/t";
            case 1: return lastRfPerTickIn + "RF/t";
            case 2: return totalExtracted + "RF";
            case 3: return totalInserted + "RF";
        }
        return null;
    }

    public int getLastRfPerTickIn() {
        return lastRfPerTickIn;
    }

    public int getLastRfPerTickOut() {
        return lastRfPerTickOut;
    }

    @Override
    protected boolean needsCustomInvWrapper() {
        return true;
    }

    public int getNetworkId() {
        return networkId;
    }

    public PowerCellNetwork.Network getNetwork() {
        int networkId = getNetworkId();
        if (networkId == -1) {
            return null;
        }
        PowerCellNetwork generatorNetwork = PowerCellNetwork.getChannels(worldObj);
        return generatorNetwork.getOrCreateNetwork(networkId);
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
        Mode[] old = new Mode[] { modes[0], modes[1], modes[2], modes[3], modes[4], modes[5] };
        super.onDataPacket(net, packet);
        for (int i = 0 ; i < 6 ; i++) {
            if (old[i] != modes[i]) {
                worldObj.markBlockRangeForRenderUpdate(getPos(), getPos());
                return;
            }
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tagCompound) {
        super.readFromNBT(tagCompound);
    }

    @Override
    public void readRestorableFromNBT(NBTTagCompound tagCompound) {
        super.readRestorableFromNBT(tagCompound);
        readBufferFromNBT(tagCompound, inventoryHelper);
        energy = tagCompound.getInteger("energy");
        totalInserted = tagCompound.getInteger("totIns");
        totalExtracted = tagCompound.getInteger("totExt");
        networkId = tagCompound.getInteger("networkId");
        modes[0] = Mode.values()[tagCompound.getByte("m0")];
        modes[1] = Mode.values()[tagCompound.getByte("m1")];
        modes[2] = Mode.values()[tagCompound.getByte("m2")];
        modes[3] = Mode.values()[tagCompound.getByte("m3")];
        modes[4] = Mode.values()[tagCompound.getByte("m4")];
        modes[5] = Mode.values()[tagCompound.getByte("m5")];
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tagCompound) {
        super.writeToNBT(tagCompound);
        return tagCompound;
    }

    @Override
    public void writeRestorableToNBT(NBTTagCompound tagCompound) {
        super.writeRestorableToNBT(tagCompound);
        writeBufferToNBT(tagCompound, inventoryHelper);
        tagCompound.setInteger("energy", energy);
        tagCompound.setInteger("totIns", totalInserted);
        tagCompound.setInteger("totExt", totalExtracted);
        tagCompound.setInteger("networkId", networkId);
        tagCompound.setByte("m0", (byte) modes[0].ordinal());
        tagCompound.setByte("m1", (byte) modes[1].ordinal());
        tagCompound.setByte("m2", (byte) modes[2].ordinal());
        tagCompound.setByte("m3", (byte) modes[3].ordinal());
        tagCompound.setByte("m4", (byte) modes[4].ordinal());
        tagCompound.setByte("m5", (byte) modes[5].ordinal());
    }

    public Mode getMode(EnumFacing side) {
        return modes[side.ordinal()];
    }

    public void toggleMode(EnumFacing side) {
        switch (modes[side.ordinal()]) {
            case MODE_NONE:
                modes[side.ordinal()] = Mode.MODE_INPUT;
                break;
            case MODE_INPUT:
                modes[side.ordinal()] = Mode.MODE_OUTPUT;
                break;
            case MODE_OUTPUT:
                modes[side.ordinal()] = Mode.MODE_NONE;
                break;
        }
        markDirtyClient();
    }

    @Override
    public void update() {
        if (!worldObj.isRemote) {
            long time = System.currentTimeMillis();
            if (lastTime == 0) {
                lastTime = time;
            } else if (time > lastTime + 2000) {
                lastRfPerTickIn = (int) (50 * powerIn / (time - lastTime));
                lastRfPerTickOut = (int) (50 * powerOut / (time - lastTime));
                lastTime = time;
                powerIn = 0;
                powerOut = 0;
            }

            if (isCreative()) {
                // A creative powercell automatically generates 1000000 RF/tick
                int gain = 1000000;
                int networkId = getNetworkId();
                if (networkId == -1) {
                    receiveEnergyLocal(gain, false);
                } else {
                    receiveEnergyMulti(gain, false);
                }
            }

            int energyStored = getEnergyStored(EnumFacing.DOWN);
            if (energyStored <= 0) {
                return;
            }

            handleChargingItem();
            sendOutEnergy();
        }
    }

    private void handleChargingItem() {
        ItemStack stack = inventoryHelper.getStackInSlot(PowerCellContainer.SLOT_CHARGEITEM);
        if (stack == null) {
            return;
        }

        if (stack.hasCapability(CapabilityEnergy.ENERGY, null)) {
            IEnergyStorage capability = stack.getCapability(CapabilityEnergy.ENERGY, null);
            int energyStored = getEnergyStored(EnumFacing.DOWN);
            int rfToGive = PowerCellConfiguration.CHARGEITEMPERTICK <= energyStored ? PowerCellConfiguration.CHARGEITEMPERTICK : energyStored;
            int received = capability.receiveEnergy(rfToGive, false);
            extractEnergyInternal(received, false, PowerCellConfiguration.CHARGEITEMPERTICK);
        } else if (stack.getItem() instanceof IEnergyContainerItem) {
            IEnergyContainerItem energyContainerItem = (IEnergyContainerItem) stack.getItem();
            int energyStored = getEnergyStored(EnumFacing.DOWN);
            int rfToGive = PowerCellConfiguration.CHARGEITEMPERTICK <= energyStored ? PowerCellConfiguration.CHARGEITEMPERTICK : energyStored;
            int received = energyContainerItem.receiveEnergy(stack, rfToGive, false);
            extractEnergyInternal(received, false, PowerCellConfiguration.CHARGEITEMPERTICK);
        }
    }

    private void sendOutEnergy() {
        int energyStored = getEnergyStored(EnumFacing.DOWN);

        for (EnumFacing face : EnumFacing.values()) {
            if (modes[face.ordinal()] == Mode.MODE_OUTPUT) {
                BlockPos pos = getPos().offset(face);
                TileEntity te = worldObj.getTileEntity(pos);
                if (EnergyTools.isEnergyTE(te)) {
                    // If the adjacent block is also a powercell then we only send energy if this cell is local or the other cell has a different id
                    if ((!(te instanceof PowerCellTileEntity)) || getNetworkId() == -1 || ((PowerCellTileEntity) te).getNetworkId() != getNetworkId()) {
                        EnumFacing opposite = face.getOpposite();
                        float factor = getCostFactor();
                        int rfPerTick = getRfPerTickPerSide();
                        int received;

                        int rfToGive;
                        if (rfPerTick <= ((int) (energyStored / factor))) {
                            rfToGive = rfPerTick;
                        } else {
                            rfToGive = (int) (energyStored / factor);
                        }

                        if (te instanceof IEnergyConnection) {
                            IEnergyConnection connection = (IEnergyConnection) te;
                            if (connection.canConnectEnergy(opposite)) {
                                received = EnergyTools.receiveEnergy(te, opposite, rfToGive);
                            } else {
                                received = 0;
                            }
                        } else {
                            // Forge unit
                            received = EnergyTools.receiveEnergy(te, opposite, rfToGive);
                        }

                        energyStored -= extractEnergyInternal((int) (received * factor), false,
                                PowerCellConfiguration.rfPerTick * getPowerFactor() / simpleFactor);
                        if (energyStored <= 0) {
                            break;
                        }
                    }
                }
            }
        }
    }

    public float getCostFactor() {
        float infusedFactor = getInfusedFactor();

        float factor;
        if (getNetworkId() == -1) {
            factor = 1.0f; // Local energy
        } else {
            factor = getNetwork().calculateCostFactor(worldObj, getGlobalPos());
            factor = (factor - 1) * (1-infusedFactor/2) + 1;
        }
        return factor;
    }

    public int getRfPerTickPerSide() {
        return (int) (PowerCellConfiguration.rfPerTick * getPowerFactor() / simpleFactor * (getInfusedFactor()*.5+1));
    }

    private void handleCardRemoval() {
        if (!worldObj.isRemote) {
            PowerCellNetwork.Network network = getNetwork();
            if (network != null) {
                energy = network.extractEnergySingleBlock(isAdvanced(), isSimple());
                network.remove(worldObj, getGlobalPos(), isAdvanced(), isSimple());
                PowerCellNetwork.getChannels(worldObj).save(worldObj);
            }
        }
        networkId = -1;
        markDirty();
    }

    private void handleCardInsertion() {
        ItemStack stack = inventoryHelper.getStackInSlot(PowerCellContainer.SLOT_CARD);
        int id = PowerCellCardItem.getId(stack);
        if (!worldObj.isRemote) {
            PowerCellNetwork channels = PowerCellNetwork.getChannels(worldObj);
            if (id == -1) {
                id = channels.newChannel();
                PowerCellCardItem.setId(stack, id);
            }
            networkId = id;
            PowerCellNetwork.Network network = getNetwork();
            network.add(worldObj, getGlobalPos(), isAdvanced(), isSimple());
            network.setEnergy(network.getEnergy() + energy);
            channels.save(worldObj);
        } else {
            networkId = id;
        }
        markDirty();
    }

    private boolean isAdvanced() {
        return PowerCellBlock.isAdvanced(worldObj.getBlockState(getPos()).getBlock());
    }

    private boolean isSimple() {
        return PowerCellBlock.isSimple(worldObj.getBlockState(getPos()).getBlock());
    }

    private boolean isCreative() {
        return PowerCellBlock.isCreative(worldObj.getBlockState(getPos()).getBlock());
    }

    // Get the power factor relative to the simple powercell
    private int getPowerFactor() {
        if (isSimple()) {
            return 1;
        }
        return isAdvanced() ? (advancedFactor * simpleFactor) : simpleFactor;
    }

    public int getEnergy() {
        return energy;
    }

    public GlobalCoordinate getGlobalPos() {
        return new GlobalCoordinate(getPos(), worldObj.provider.getDimension());
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        if (index == PowerCellContainer.SLOT_CARD) {
            handleCardRemoval();
        }
        return inventoryHelper.removeStackFromSlot(index);
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        if (index == PowerCellContainer.SLOT_CARD && inventoryHelper.containsItem(index) && count >= inventoryHelper.getStackInSlot(index).stackSize) {
            handleCardRemoval();
        }
        return inventoryHelper.decrStackSize(index, count);
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        if (index == PowerCellContainer.SLOT_CARD) {
            handleCardRemoval();
        }
        inventoryHelper.setInventorySlotContents(getInventoryStackLimit(), index, stack);
        if (index == PowerCellContainer.SLOT_CARD && inventoryHelper.containsItem(index)) {
            handleCardInsertion();
        }
        else if (index == PowerCellContainer.SLOT_CARDCOPY && inventoryHelper.containsItem(index)) {
            PowerCellCardItem.setId(inventoryHelper.getStackInSlot(index), networkId);
        }
    }

    public int getTotalExtracted() {
        return totalExtracted;
    }

    public int getTotalInserted() {
        return totalInserted;
    }

    @Override
    public int receiveEnergy(EnumFacing from, int maxReceive, boolean simulate) {
        if (modes[from.ordinal()] != Mode.MODE_INPUT) {
            return 0;
        }
        int networkId = getNetworkId();
        int received;
        if (networkId == -1) {
            received = receiveEnergyLocal(maxReceive, simulate);
        } else {
            received = receiveEnergyMulti(maxReceive, simulate);
        }
        if (!simulate) {
            totalInserted += received;
            powerIn += received;
            markDirty();
        }
        return received;
    }

    private int receiveEnergyMulti(int maxReceive, boolean simulate) {
        PowerCellNetwork.Network network = getNetwork();
        int totEnergy = PowerCellConfiguration.rfPerNormalCell * (network.getBlockCount() - network.getAdvancedBlockCount() - network.getSimpleBlockCount())
                + PowerCellConfiguration.rfPerNormalCell * advancedFactor * network.getAdvancedBlockCount() +
                + PowerCellConfiguration.rfPerNormalCell * network.getSimpleBlockCount() / simpleFactor;
        int maxInsert = Math.min(totEnergy - network.getEnergy(), maxReceive);
        if (maxInsert > 0) {
            if (!simulate) {
                network.receiveEnergy(maxInsert);
                PowerCellNetwork.getChannels(worldObj).save(worldObj);
            }
        }
        return maxInsert;
    }

    private int receiveEnergyLocal(int maxReceive, boolean simulate) {
        int maxInsert = Math.min(PowerCellConfiguration.rfPerNormalCell * getPowerFactor() / simpleFactor - energy, maxReceive);
        if (maxInsert > 0) {
            if (!simulate) {
                energy += maxInsert;
                markDirty();
            }
        }
        return maxInsert;
    }

    @Override
    public int extractEnergy(EnumFacing from, int maxExtract, boolean simulate) {
        return 0;
//        return extractEnergyInternal(maxExtract, simulate, PowerCellConfiguration.rfPerTick * getAdvancedFactor());
    }

    private int extractEnergyInternal(int maxExtract, boolean simulate, int maximum) {
        int networkId = getNetworkId();
        int extracted;
        if (networkId == -1) {
            extracted = extractEnergyLocal(maxExtract, simulate, maximum);
        } else {
            extracted =  extractEnergyMulti(maxExtract, simulate, maximum);
        }
        if (!simulate) {
            totalExtracted += extracted;
            powerOut += extracted;
            markDirty();
        }
        return extracted;
    }

    private int extractEnergyMulti(int maxExtract, boolean simulate, int maximum) {
        PowerCellNetwork.Network network = getNetwork();
        int energy = network.getEnergy();
        if (maxExtract > energy) {
            maxExtract = energy;
        }
        if (maxExtract > maximum) {
            maxExtract = maximum;
        }
        if (!simulate) {
            network.extractEnergy(maxExtract);
            PowerCellNetwork.getChannels(worldObj).save(worldObj);
        }
        return maxExtract;
    }

    private int extractEnergyLocal(int maxExtract, boolean simulate, int maximum) {
        // We act as a single block
        if (maxExtract > energy) {
            maxExtract = energy;
        }
        if (maxExtract > maximum) {
            maxExtract = maximum;
        }
        if (!simulate) {
            energy -= maxExtract;
            markDirty();
        }
        return maxExtract;
    }

    @Override
    public int getEnergyStored(EnumFacing from) {
        int networkId = getNetworkId();
        if (networkId == -1) {
            return energy;
        }
        PowerCellNetwork.Network network = getNetwork();
        return network.getEnergy();
    }

    @Override
    public int getMaxEnergyStored(EnumFacing from) {
        int networkId = getNetworkId();
        if (networkId == -1) {
            return PowerCellConfiguration.rfPerNormalCell * getPowerFactor() / simpleFactor;
        }
        PowerCellNetwork.Network network = getNetwork();
        return (network.getBlockCount() - network.getAdvancedBlockCount() - network.getSimpleBlockCount()) * PowerCellConfiguration.rfPerNormalCell +
                network.getAdvancedBlockCount() * PowerCellConfiguration.rfPerNormalCell * advancedFactor +
                network.getSimpleBlockCount() * PowerCellConfiguration.rfPerNormalCell / simpleFactor;
    }


    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        if (index == PowerCellContainer.SLOT_CARD && stack.getItem() != PowerCellSetup.powerCellCardItem) {
            return false;
        }
        if (index == PowerCellContainer.SLOT_CARDCOPY && stack.getItem() != PowerCellSetup.powerCellCardItem) {
            return false;
        }
        return true;
    }

    @Override
    public boolean canConnectEnergy(EnumFacing from) {
        return true;
    }

    @Override
    public InventoryHelper getInventoryHelper() {
        return inventoryHelper;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return canPlayerAccess(player);
    }

    @Override
    public boolean execute(EntityPlayerMP playerMP, String command, Map<String, Argument> args) {
        boolean rc = super.execute(playerMP, command, args);
        if (rc) {
            return true;
        }
        if (CMD_SETNONE.equals(command)) {
            for (EnumFacing facing : EnumFacing.values()) {
                modes[facing.ordinal()] = Mode.MODE_NONE;
            }
            markDirtyClient();
            return true;
        } else if (CMD_SETINPUT.equals(command)) {
            for (EnumFacing facing : EnumFacing.values()) {
                modes[facing.ordinal()] = Mode.MODE_INPUT;
            }
            markDirtyClient();
            return true;
        } else if (CMD_SETOUTPUT.equals(command)) {
            for (EnumFacing facing : EnumFacing.values()) {
                modes[facing.ordinal()] = Mode.MODE_OUTPUT;
            }
            markDirtyClient();
            return true;
        } else if (CMD_CLEARSTATS.equals(command)) {
            totalExtracted = 0;
            totalInserted = 0;
            markDirty();
            return true;
        }
        return false;
    }

    @Override
    public void selectBlock(EntityPlayer player, BlockPos pos) {
        dumpNetwork(player, this);
    }

    public static void dumpNetwork(EntityPlayer player, PowerCellTileEntity powerCellTileEntity) {
        PowerCellNetwork.Network network = powerCellTileEntity.getNetwork();
        Set<GlobalCoordinate> blocks = network.getBlocks();
        System.out.println("blocks.size() = " + blocks.size());
        blocks.forEach(b -> {
            String msg;
            World w = TeleportationTools.getWorldForDimension(player.worldObj, b.getDimension());
            if (w == null) {
                msg = "dimension missing!";
            } else {
                Block block = w.getBlockState(b.getCoordinate()).getBlock();
                if (block == PowerCellSetup.powerCellBlock) {
                    msg = "normal";
                } else if (block == PowerCellSetup.advancedPowerCellBlock) {
                    msg = "advanced";
                } else if (block == PowerCellSetup.creativePowerCellBlock) {
                    msg = "creative";
                } else {
                    msg = "not a powercell!";
                }
                TileEntity te = w.getTileEntity(b.getCoordinate());
                if (te instanceof PowerCellTileEntity) {
                    PowerCellTileEntity power = (PowerCellTileEntity) te;
                    msg += " (+:" + power.getTotalInserted() + ", -:" + power.getTotalExtracted() + ")";
                }
            }

            Logging.message(player, "Block: " + BlockPosTools.toString(b.getCoordinate()) + " (" + b.getDimension() + "): " + msg);
        });
    }

    // Forge energy
    private IEnergyStorage[] sidedHandlers = new IEnergyStorage[6];
    private IEnergyStorage nullHandler;

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return true;
        }
        return super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            if (facing == null) {
                if (nullHandler == null) {
                    createNullHandler();
                }
                return (T) nullHandler;
            } else {
                if (sidedHandlers[facing.ordinal()] == null) {
                    createSidedHandler(facing);
                }
                return (T) sidedHandlers[facing.ordinal()];
            }
        }
        return super.getCapability(capability, facing);
    }

    private void createSidedHandler(EnumFacing facing) {
        sidedHandlers[facing.ordinal()] = new IEnergyStorage() {
            @Override
            public int receiveEnergy(int maxReceive, boolean simulate) {
                return PowerCellTileEntity.this.receiveEnergy(facing, maxReceive, simulate);
            }

            @Override
            public int extractEnergy(int maxExtract, boolean simulate) {
                return 0;
            }

            @Override
            public int getEnergyStored() {
                return PowerCellTileEntity.this.getEnergyStored(facing);
            }

            @Override
            public int getMaxEnergyStored() {
                return PowerCellTileEntity.this.getMaxEnergyStored(facing);
            }

            @Override
            public boolean canExtract() {
                return false;
            }

            @Override
            public boolean canReceive() {
                return true;
            }
        };
    }

    private void createNullHandler() {
        nullHandler = new IEnergyStorage() {
            @Override
            public int receiveEnergy(int maxReceive, boolean simulate) {
                return 0;
            }

            @Override
            public int extractEnergy(int maxExtract, boolean simulate) {
                return 0;
            }

            @Override
            public int getEnergyStored() {
                return PowerCellTileEntity.this.getEnergyStored(EnumFacing.DOWN);
            }

            @Override
            public int getMaxEnergyStored() {
                return PowerCellTileEntity.this.getMaxEnergyStored(EnumFacing.DOWN);
            }

            @Override
            public boolean canExtract() {
                return false;
            }

            @Override
            public boolean canReceive() {
                return false;
            }
        };
    }
}
