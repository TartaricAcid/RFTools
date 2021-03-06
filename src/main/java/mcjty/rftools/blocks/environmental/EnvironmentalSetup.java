package mcjty.rftools.blocks.environmental;

import mcjty.rftools.blocks.ModBlocks;
import mcjty.rftools.crafting.NBTMatchingRecipe;
import mcjty.rftools.items.ModItems;
import mcjty.rftools.items.SyringeItem;
import mcjty.rftools.items.envmodules.*;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.monster.*;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.Map;

public class EnvironmentalSetup {
    public static EnvironmentalControllerBlock environmentalControllerBlock;

    public static RegenerationEModuleItem regenerationEModuleItem;
    public static RegenerationPlusEModuleItem regenerationPlusEModuleItem;
    public static SpeedEModuleItem speedEModuleItem;
    public static SpeedPlusEModuleItem speedPlusEModuleItem;
    public static HasteEModuleItem hasteEModuleItem;
    public static HastePlusEModuleItem hastePlusEModuleItem;
    public static SaturationEModuleItem saturationEModuleItem;
    public static SaturationPlusEModuleItem saturationPlusEModuleItem;
    public static FeatherFallingEModuleItem featherFallingEModuleItem;
    public static FeatherFallingPlusEModuleItem featherFallingPlusEModuleItem;
    public static FlightEModuleItem flightEModuleItem;
    public static PeacefulEModuleItem peacefulEModuleItem;
    public static WaterBreathingEModuleItem waterBreathingEModuleItem;
    public static NightVisionEModuleItem nightVisionEModuleItem;
    public static GlowingEModuleItem glowingEModuleItem;
    public static LuckEModuleItem luckEModuleItem;
    public static NoTeleportEModuleItem noTeleportEModuleItem;

    public static BlindnessEModuleItem blindnessEModuleItem;
    public static WeaknessEModuleItem weaknessEModuleItem;
    public static PoisonEModuleItem poisonEModuleItem;
    public static SlownessEModuleItem slownessEModuleItem;

    public static void init() {
        environmentalControllerBlock = new EnvironmentalControllerBlock();
        regenerationEModuleItem = new RegenerationEModuleItem();
        regenerationPlusEModuleItem = new RegenerationPlusEModuleItem();
        speedEModuleItem = new SpeedEModuleItem();
        speedPlusEModuleItem = new SpeedPlusEModuleItem();
        hasteEModuleItem = new HasteEModuleItem();
        hastePlusEModuleItem = new HastePlusEModuleItem();
        saturationEModuleItem = new SaturationEModuleItem();
        saturationPlusEModuleItem = new SaturationPlusEModuleItem();
        featherFallingEModuleItem = new FeatherFallingEModuleItem();
        featherFallingPlusEModuleItem = new FeatherFallingPlusEModuleItem();
        flightEModuleItem = new FlightEModuleItem();
        peacefulEModuleItem = new PeacefulEModuleItem();
        waterBreathingEModuleItem = new WaterBreathingEModuleItem();
        nightVisionEModuleItem = new NightVisionEModuleItem();
        blindnessEModuleItem = new BlindnessEModuleItem();
        weaknessEModuleItem = new WeaknessEModuleItem();
        poisonEModuleItem = new PoisonEModuleItem();
        slownessEModuleItem = new SlownessEModuleItem();
        glowingEModuleItem = new GlowingEModuleItem();
        luckEModuleItem = new LuckEModuleItem();
        noTeleportEModuleItem = new NoTeleportEModuleItem();
    }

    @SideOnly(Side.CLIENT)
    public static void initClient() {
        environmentalControllerBlock.initModel();
        regenerationEModuleItem.initModel();
        regenerationPlusEModuleItem.initModel();
        speedEModuleItem.initModel();
        speedPlusEModuleItem.initModel();
        hasteEModuleItem.initModel();
        hastePlusEModuleItem.initModel();
        saturationEModuleItem.initModel();
        saturationPlusEModuleItem.initModel();
        featherFallingEModuleItem.initModel();
        featherFallingPlusEModuleItem.initModel();
        flightEModuleItem.initModel();
        peacefulEModuleItem.initModel();
        waterBreathingEModuleItem.initModel();
        nightVisionEModuleItem.initModel();
        blindnessEModuleItem.initModel();
        weaknessEModuleItem.initModel();
        poisonEModuleItem.initModel();
        slownessEModuleItem.initModel();
        glowingEModuleItem.initModel();
        luckEModuleItem.initModel();
        noTeleportEModuleItem.initModel();
    }

    public static void initCrafting() {
        GameRegistry.addRecipe(new ItemStack(environmentalControllerBlock), "oDo", "GMI", "oEo", 'o', Items.ENDER_PEARL, 'M', ModBlocks.machineFrame,
                'D', Blocks.DIAMOND_BLOCK, 'E', Blocks.EMERALD_BLOCK, 'G', Blocks.GOLD_BLOCK, 'I', Blocks.IRON_BLOCK);

        Object inkSac = Item.REGISTRY.getObjectById(351);

        String[] syringeMatcher = new String[] { "level", "mobId" };
        String[] pickMatcher = new String[] { "ench" };

        ItemStack ironGolemSyringe = SyringeItem.createMobSyringe(EntityIronGolem.class);
        ItemStack endermanSyringe = SyringeItem.createMobSyringe(EntityEnderman.class);
        ItemStack ghastSyringe = SyringeItem.createMobSyringe(EntityGhast.class);
        ItemStack chickenSyringe = SyringeItem.createMobSyringe(EntityChicken.class);
        ItemStack batSyringe = SyringeItem.createMobSyringe(EntityBat.class);
        ItemStack horseSyringe = SyringeItem.createMobSyringe(EntityHorse.class);
        ItemStack zombieSyringe = SyringeItem.createMobSyringe(EntityZombie.class);
        ItemStack squidSyringe = SyringeItem.createMobSyringe(EntitySquid.class);
        ItemStack guardianSyringe = SyringeItem.createMobSyringe(EntityGuardian.class);
        ItemStack caveSpiderSyringe = SyringeItem.createMobSyringe(EntityCaveSpider.class);
        ItemStack blazeSyringe = SyringeItem.createMobSyringe(EntityBlaze.class);
        ItemStack shulkerEntity = SyringeItem.createMobSyringe(EntityShulker.class);
        ItemStack diamondPick = createEnchantedItem(Items.DIAMOND_PICKAXE, Enchantment.REGISTRY.getObject(new ResourceLocation("efficiency")), 3);
        ItemStack reds = new ItemStack(Items.REDSTONE);
        ItemStack gold = new ItemStack(Items.GOLD_INGOT);
        ItemStack ink = new ItemStack((Item) inkSac);
        ItemStack obsidian = new ItemStack(Blocks.OBSIDIAN);
        ItemStack lapis = new ItemStack(Items.DYE, 1, 4);

        GameRegistry.addRecipe(new NBTMatchingRecipe(3, 3,
                new ItemStack[] {null, chickenSyringe, null, reds, gold, reds, null, ink, null},
                new String[][] {null, syringeMatcher, null, null, null, null, null, null, null},
                new ItemStack(featherFallingEModuleItem)));

        GameRegistry.addRecipe(new NBTMatchingRecipe(3, 3,
                new ItemStack[] {null, ironGolemSyringe, null, reds, gold, reds, null, ink, null},
                new String[][] {null, syringeMatcher, null, null, null, null, null, null, null},
                new ItemStack(regenerationEModuleItem)));

        GameRegistry.addRecipe(new NBTMatchingRecipe(3, 3,
                new ItemStack[] {null, horseSyringe, null, reds, gold, reds, null, ink, null},
                new String[][] {null, syringeMatcher, null, null, null, null, null, null, null},
                new ItemStack(speedEModuleItem)));

        GameRegistry.addRecipe(new NBTMatchingRecipe(3, 3, new ItemStack[] {null, diamondPick, null, reds, gold, reds, null, ink, null},
                new String[][] {null, pickMatcher, null, null, null, null, null, null, null},
                new ItemStack(hasteEModuleItem)));

        GameRegistry.addRecipe(new NBTMatchingRecipe(3, 3,
                new ItemStack[] {null, zombieSyringe, null, reds, gold, reds, null, ink, null},
                new String[][] {null, syringeMatcher, null, null, null, null, null, null, null},
                new ItemStack(saturationEModuleItem)));

        GameRegistry.addRecipe(new NBTMatchingRecipe(3, 3,
                new ItemStack[] {null, ghastSyringe, null, reds, gold, reds, null, ink, null},
                new String[][] {null, syringeMatcher, null, null, null, null, null, null, null},
                new ItemStack(flightEModuleItem)));

        GameRegistry.addRecipe(new NBTMatchingRecipe(3, 3,
                new ItemStack[] {null, guardianSyringe, null, reds, gold, reds, null, ink, null},
                new String[][] {null, syringeMatcher, null, null, null, null, null, null, null},
                new ItemStack(waterBreathingEModuleItem)));

        GameRegistry.addRecipe(new NBTMatchingRecipe(3, 3,
                new ItemStack[] {null, caveSpiderSyringe, null, reds, gold, reds, null, ink, null},
                new String[][] {null, syringeMatcher, null, null, null, null, null, null, null},
                new ItemStack(nightVisionEModuleItem)));

        GameRegistry.addRecipe(new NBTMatchingRecipe(2, 2,
                new ItemStack[]{new ItemStack(regenerationEModuleItem), ironGolemSyringe, ironGolemSyringe, null},
                new String[][] {null, syringeMatcher, syringeMatcher, null},
                new ItemStack(regenerationPlusEModuleItem)));

        GameRegistry.addRecipe(new NBTMatchingRecipe(2, 2,
                new ItemStack[]{new ItemStack(speedEModuleItem), horseSyringe, horseSyringe, null},
                new String[][] {null, syringeMatcher, syringeMatcher, null},
                new ItemStack(speedPlusEModuleItem)));

        GameRegistry.addRecipe(new NBTMatchingRecipe(2, 2,
                new ItemStack[]{new ItemStack(hasteEModuleItem), diamondPick, null, null},
                new String[][] {null, pickMatcher, null, null},
                new ItemStack(hastePlusEModuleItem)));

        GameRegistry.addRecipe(new NBTMatchingRecipe(2, 2,
                new ItemStack[]{new ItemStack(saturationEModuleItem), zombieSyringe, zombieSyringe, null},
                new String[][] {null, syringeMatcher, syringeMatcher, null},
                new ItemStack(saturationPlusEModuleItem)));

        GameRegistry.addRecipe(new NBTMatchingRecipe(2, 2,
                new ItemStack[]{new ItemStack(featherFallingEModuleItem), chickenSyringe, batSyringe, null},
                new String[][] {null, syringeMatcher, syringeMatcher, null},
                new ItemStack(featherFallingPlusEModuleItem)));

        GameRegistry.addRecipe(new NBTMatchingRecipe(3, 3,
                new ItemStack[] {null, blazeSyringe, null, reds, gold, reds, null, ink, null},
                new String[][] {null, syringeMatcher, null, null, null, null, null, null, null},
                new ItemStack(glowingEModuleItem)));

        GameRegistry.addRecipe(new NBTMatchingRecipe(3, 3,
                new ItemStack[] {null, shulkerEntity, null, reds, gold, reds, null, ink, null},
                new String[][] {null, syringeMatcher, null, null, null, null, null, null, null},
                new ItemStack(luckEModuleItem)));

        GameRegistry.addRecipe(new ItemStack(peacefulEModuleItem, 1), " p ", "rgr", " i ", 'p', ModItems.peaceEssenceItem,
                'r', reds, 'g', gold, 'i', ink);

        GameRegistry.addRecipe(new NBTMatchingRecipe(3, 3,
                new ItemStack[]{null, squidSyringe, null, lapis, obsidian, lapis, null, ink, null},
                new String[][]{null, syringeMatcher, null, null, null, null, null, null, null},
                new ItemStack(blindnessEModuleItem)));

        GameRegistry.addRecipe(new NBTMatchingRecipe(3, 3,
                new ItemStack[]{null, batSyringe, null, lapis, obsidian, lapis, null, ink, null},
                new String[][]{null, syringeMatcher, null, null, null, null, null, null, null},
                new ItemStack(weaknessEModuleItem)));

        GameRegistry.addRecipe(new NBTMatchingRecipe(3, 3,
                new ItemStack[]{null, caveSpiderSyringe, null, lapis, obsidian, lapis, null, ink, null},
                new String[][]{null, syringeMatcher, null, null, null, null, null, null, null},
                new ItemStack(poisonEModuleItem)));

        GameRegistry.addRecipe(new NBTMatchingRecipe(3, 3,
                new ItemStack[]{null, new ItemStack(Items.CLOCK), null, lapis, obsidian, lapis, null, ink, null},
                new String[][]{null, null, null, null, null, null, null, null, null},
                new ItemStack(slownessEModuleItem)));

        GameRegistry.addRecipe(new NBTMatchingRecipe(3, 3,
                new ItemStack[]{null, endermanSyringe, null, lapis, obsidian, lapis, null, ink, null},
                new String[][]{null, syringeMatcher, null, null, null, null, null, null, null},
                new ItemStack(noTeleportEModuleItem)));
    }

    public static ItemStack createEnchantedItem(Item item, Enchantment effectId, int amount) {
        ItemStack stack = new ItemStack(item);
        Map enchant = new HashMap();
        enchant.put(effectId, amount);
        EnchantmentHelper.setEnchantments(enchant, stack);
        return stack;
    }

}
