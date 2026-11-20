package tb;

import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.CrucibleRecipe;
import thaumcraft.crafting.CraftingRecipeRef;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchPage;

@Mod(Bases.ID)
public final class Bases {
    static final String ID = "thaumicbases";
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);
    private static final net.neoforged.neoforge.registries.DeferredItem<net.minecraft.world.item.Item> INGOT =
        ITEMS.registerSimpleItem("thauminite_ingot");
    static { ITEMS.registerSimpleItem("thauminite_nugget"); }

    static {
        for (String name : List.of("thauminite_block", "quicksilver_block")) {
            ITEMS.registerSimpleBlockItem(BLOCKS.registerSimpleBlock(name, p -> p.strength(4, 8)
                .sound(net.minecraft.world.level.block.SoundType.METAL).requiresCorrectToolForDrops()));
        }
        ITEMS.registerSimpleBlockItem(BLOCKS.registerBlock("quicksilver_bricks", net.minecraft.world.level.block.TransparentBlock::new,
            p -> p.strength(0.5F).sound(net.minecraft.world.level.block.SoundType.METAL)
                .requiresCorrectToolForDrops().noOcclusion()));
        for (String name : List.of("old_cobblestone", "old_mossy_cobblestone", "old_bricks", "old_lapis_block",
            "old_iron_block", "old_gold_block", "old_diamond_block", "void_metal_block")) {
            ITEMS.registerSimpleBlockItem(BLOCKS.registerSimpleBlock(name, p -> p.strength(3, 6)
                .requiresCorrectToolForDrops()));
        }
        ITEMS.registerSimpleBlockItem(BLOCKS.registerBlock("old_gravel", p -> new net.minecraft.world.level.block.ColoredFallingBlock(new net.minecraft.util.ColorRGBA(0xFF807C78), p),
            p -> p.strength(0.6F).sound(net.minecraft.world.level.block.SoundType.GRAVEL)));
        ThauminiteGear.items(ITEMS);
        BasesCrops.register(BLOCKS, ITEMS);
        SpreadingCrops.register(BLOCKS, ITEMS);
        FluxPlants.register(BLOCKS, ITEMS);
        Tobacco.register(ITEMS);
        CrystalBuilding.register(BLOCKS, ITEMS);
        VoidTools.register(ITEMS);
        MetalRods.register(ITEMS);
        MagicalTrees.register(BLOCKS, ITEMS);
        Rosehip.register(BLOCKS, ITEMS);
        Aurelia.register(BLOCKS, ITEMS);
        Spikes.register(BLOCKS, ITEMS);
        BloodyRobes.register(ITEMS);
        ObeliskAndRecycling.register(BLOCKS, ITEMS);
        RosaMysteria.register(BLOCKS, ITEMS);
        MoreAlchemy.register(BLOCKS, ITEMS);
        RainbowCactus.register(BLOCKS, ITEMS);
        ActivationFocus.register(ITEMS);
        DrainFocus.register(BLOCKS, ITEMS);
        ExperienceFocus.register(ITEMS);
        FluxFocus.register(ITEMS);
        Revolver.register(ITEMS);
        HerobrineScythe.register(ITEMS);
        ThaumicCampfire.register(BLOCKS, ITEMS);
        MagicalAnvil.register(BLOCKS, ITEMS);
        Overchanter.register(BLOCKS, ITEMS);
        NodeLinker.register(BLOCKS, ITEMS);
        NodeManipulator.register(BLOCKS, ITEMS);
        EntityDeconstructor.register(BLOCKS, ITEMS);
        ThaumiumFurnace.register(BLOCKS, ITEMS);
        ThaumicRelocator.register(BLOCKS, ITEMS);
        TABS.register("bases", () -> CreativeModeTab.builder()
            .title(Component.literal("Thaumic Bases"))
            .icon(() -> new ItemStack(INGOT.get()))
            .displayItems((parameters, output) -> {
                ITEMS.getEntries().forEach(item -> output.accept(item.get()));
                for (var bracelet : Bracelets.Variant.values()) {
                    if (bracelet.available()) output.accept(bracelet.stack().create());
                }
            })
            .build());
    }

    public Bases(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        TABS.register(bus);
        MoreAlchemy.registerFluids(bus);
        ActivationFocus.registerComponents(bus);
        Revolver.registerTypes(bus);
        ThaumicCampfire.registerEntities(bus);
        ThaumicRelocator.registerEntities(bus);
        ThaumiumFurnace.registerEntities(bus);
        Overchanter.registerEntities(bus);
        NodeLinker.registerTypes(bus);
        NodeManipulator.registerTypes(bus);
        EntityDeconstructor.registerEntities(bus);
        bus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(Bases::research));
    }

    static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(ID, name);
    }

    private static void research() {
        ResearchCategories.registerCategory(ID, id("textures/thaumonomicon/bases.png"),
            id("textures/thaumonomicon/background.png"));
        BasesResearch.registerReferences();
        String silver = "TB.Quicksilver";
        String metal = "TB.Thauminite";
        var cost = new AspectList().add(Aspect.CRYSTAL, 8).add(Aspect.CRAFT, 2);
        for (Aspect aspect : Aspect.getPrimalAspects()) cost.add(aspect, 1);
        TcRecipes.register(new CrucibleRecipe(id("quicksilver").toString(), silver, TcStack.of("thaumcraft:quicksilver", 3),
            TcIngredient.item("thaumcraft:silverwood_log"), new AspectList().add(Aspect.ORDER, 1).add(Aspect.POISON, 1)));
        TcRecipes.register(new CrucibleRecipe(id("thauminite").toString(), metal, TcStack.of(id("thauminite_nugget").toString(), 8),
            TcIngredient.item("thaumcraft:thaumium_ingot"), cost));
        add(silver, "thaumcraft:quicksilver", "CRUCIBLE", -2,
            "quicksilver", "quicksilver_block", "quicksilver_from_block", "quicksilver_bricks");
        add(metal, id("thauminite_ingot").toString(), "THAUMIUM", 2,
            "thauminite", "thauminite_ingot", "thauminite_nuggets", "thauminite_block", "thauminite_from_block");
        ThauminiteGear.registerCap();
        SpreadingCrops.research();
        BasesCrops.research();
        FluxPlants.research();
        Tobacco.research();
        CrystalBuilding.research();
        VoidTools.research();
        MetalRods.research();
        MagicalTrees.research();
        Bracelets.register();
        Rosehip.research();
        Aurelia.research();
        Spikes.research();
        BloodyRobes.research();
        ObeliskAndRecycling.research();
        RosaMysteria.research();
        MoreAlchemy.research();
        RainbowCactus.research();
        ActivationFocus.research();
        DrainFocus.research();
        ExperienceFocus.research();
        FluxFocus.research();
        Revolver.research();
        HerobrineScythe.research();
        EntropyAlchemy.research();
        ThaumicCampfire.research();
        MagicalAnvil.research();
        ThaumiumFurnace.research();
        Overchanter.research();
        NodeLinker.research();
        NodeManipulator.research();
        EntityDeconstructor.research();
        ThaumicRelocator.research();
        BasesEnchantments.stub();
        BasesEnchantments.research();
        BasesEnchantments.vaporising();
        BasesEnchantments.elderWisdom();
        BasesEnchantments.remainingWeaponEnchantments();
    }

    private static void add(String key, String icon, String parent, int column, String crucible, String... crafting) {
        var pages = new java.util.ArrayList<ResearchPage>();
        pages.add(ResearchPage.text("tc.research_page." + key + ".1"));
        pages.add(ResearchPage.crucible(id(crucible).toString()));
        for (String recipe : crafting) {
            String output = switch (recipe) {
                case "quicksilver_from_block" -> "thaumcraft:quicksilver";
                case "thauminite_from_block" -> id("thauminite_ingot").toString();
                case "thauminite_nuggets" -> id("thauminite_nugget").toString();
                default -> id(recipe).toString();
            };
            int count = recipe.endsWith("from_block") || recipe.equals("thauminite_nuggets") ? 9
                : recipe.equals("quicksilver_bricks") ? 4 : 1;
            TcRecipes.register(new CraftingRecipeRef(id(recipe).toString(), key, id(recipe), TcStack.of(output, count)));
            pages.add(ResearchPage.crafting(id(recipe).toString()));
        }
        if (key.equals("TB.Thauminite")) pages.addAll(ThauminiteGear.recipes());
        ResearchCategories.addResearch(new BasesResearch(key, ID,
            new AspectList().add(Aspect.METAL, 4).add(Aspect.ORDER, 3), column, 0, 2, TcStack.of(icon))
            .setParents(parent).setPages(pages.toArray(ResearchPage[]::new)));
    }
}
