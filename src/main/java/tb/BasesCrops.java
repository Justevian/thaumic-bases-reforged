package tb;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.CraftingRecipeRef;
import thaumcraft.crafting.ShapelessArcaneRecipe;
import thaumcraft.crafting.CrucibleRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchPage;

final class BasesCrops {
    private static final String SALT = "salis_mundus_block";

    static void register(DeferredRegister.Blocks blocks, DeferredRegister.Items items) {
        var salt = blocks.registerSimpleBlock(SALT, properties -> properties.strength(0.5F).sound(SoundType.SAND));
        items.registerSimpleBlockItem(salt);
        for (String name : new String[] {"plax", "metalleat", "lucrite", "lazullia", "glieonia", "tobacco", "void_plant"}) {
            var seedId = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ITEM,
                Bases.id(name + "_seeds"));
            var plant = blocks.registerBlock(name, properties -> new Plant(properties,
                () -> net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(seedId),
                name.equals("lucrite") ? salt : null, name.equals("glieonia"),
                name.equals("lucrite") || name.equals("lazullia") || name.equals("tobacco") || name.equals("void_plant"), outline(name)),
                properties -> properties.noCollision().randomTicks().instabreak().sound(SoundType.CROP));
            items.registerItem(name + "_seeds", properties -> new net.minecraft.world.item.BlockItem(plant.get(), properties));

        }
    }

    private static VoxelShape[] outline(String name) {
        return switch (name) {
            case "plax" -> PlantOutline.PLAX;
            case "metalleat" -> PlantOutline.METALLEAT;
            case "lucrite" -> PlantOutline.LUCRITE;
            case "lazullia" -> PlantOutline.LAZULLIA;
            case "glieonia" -> PlantOutline.GLIEONIA;
            case "tobacco" -> PlantOutline.PLAX;
            case "void_plant" -> PlantOutline.GLIEONIA;
            default -> throw new IllegalArgumentException(name);
        };
    }

    private static final class Plant extends CropBlock {
        private final Supplier<ItemLike> seeds;
        private final Supplier<? extends Block> salt;
        private final boolean flower;
        private final boolean slow;
        private final VoxelShape[] outline;

        private Plant(BlockBehaviour.Properties properties, Supplier<ItemLike> seeds,
                      Supplier<? extends Block> salt, boolean flower, boolean slow, VoxelShape[] outline) {
            super(properties);
            this.seeds = seeds;
            this.salt = salt;
            this.flower = flower;
            this.slow = slow;
            this.outline = outline;
        }

        @Override

        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return PlantOutline.at(outline, getAge(state));
        }

        @Override
        protected ItemLike getBaseSeedId() {
            return seeds.get();
        }

        @Override
        protected boolean mayPlaceOn(BlockState soil, BlockGetter level, BlockPos pos) {
            return super.mayPlaceOn(soil, level, pos)
                || flower && (soil.is(Blocks.GRASS_BLOCK) || soil.is(net.minecraft.tags.BlockTags.DIRT));
        }

        private boolean hasGrowthSubstrate(BlockGetter level, BlockPos pos) {
            return salt == null || level.getBlockState(pos.below(2)).is(salt.get());
        }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (hasGrowthSubstrate(level, pos) && (!slow || random.nextBoolean())) {
                super.randomTick(state, level, pos, random);
            }
        }

        @Override
        public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
            return hasGrowthSubstrate(level, pos) && super.isValidBonemealTarget(level, pos, state, source);
        }

        @Override
        public void growCrops(Level level, BlockPos pos, BlockState state) {
            if (hasGrowthSubstrate(level, pos)) super.growCrops(level, pos, state);
        }

        @Override
        protected int getBonemealAgeIncrease(Level level) {
            return salt == null ? super.getBonemealAgeIncrease(level) : 1;
        }
    }

    static void research() {
        String multiplication = "TB.SM";
        String salis = "thaumcraft:salis_mundus";
        String duplicate = Bases.id("duplicate_salis_mundus").toString();
        var duplicationCost = new AspectList().add(Aspect.MAGIC, 3);
        for (Aspect aspect : Aspect.getPrimalAspects()) duplicationCost.add(aspect, 2);
        TcRecipes.register(new CrucibleRecipe(duplicate, multiplication, TcStack.of(salis, 2),
            TcIngredient.item(salis), duplicationCost));
        ResearchCategories.addResearch(new BasesResearch(multiplication, Bases.ID, duplicationCost,
            0, -2, 1, TcStack.of(salis)).setParents("CRUCIBLE", "TB.Quicksilver", "TB.Amber")
            .setPages(ResearchPage.text("tc.research_page.TB.SM.1"), ResearchPage.crucible(duplicate)));
        String saltId = Bases.id(SALT).toString();
        String unpackId = Bases.id("salis_mundus_from_block").toString();
        String saltResearch = "TB.SMB";
        var vis = new AspectList();
        for (Aspect aspect : Aspect.getPrimalAspects()) vis.add(aspect, 1);
        TcRecipes.register(new ShapelessArcaneRecipe(saltId, saltResearch, TcStack.of(saltId), vis,
            java.util.Collections.nCopies(9, TcIngredient.item("thaumcraft:salis_mundus"))));
        TcRecipes.register(new CraftingRecipeRef(unpackId, saltResearch, Bases.id("salis_mundus_from_block"),
            TcStack.of("thaumcraft:salis_mundus", 9)));
        ResearchCategories.addResearch(new BasesResearch(saltResearch, Bases.ID, vis, 0, 0, 1, TcStack.of(saltId))
            .setParents(multiplication).setPages(ResearchPage.text("tc.research_page.TB.SMB.1"),
                ResearchPage.arcane(saltId), ResearchPage.crafting(unpackId)));
        add("Plax", "minecraft:wheat_seeds", "TB.BasicPlants", -4, 2, new AspectList().add(Aspect.CLOTH, 4));
        add("Metalleat", "minecraft:wheat_seeds", "TB.BasicPlants", -2, 2,
            new AspectList().add(Aspect.ORDER, 4).add(Aspect.METAL, 12).add(Aspect.LIFE, 4));
        add("Lucrite", "minecraft:carrot", "TB.Metalleat", 0, 2,
            new AspectList().add(Aspect.HUNGER, 4).add(Aspect.GREED, 12).add(Aspect.LIFE, 4));
        add("Lazullia", "minecraft:wheat_seeds", "TB.Metalleat", -3, 4,
            new AspectList().add(Aspect.SENSES, 12).add(Aspect.GREED, 3));
        add("Glieonia", "minecraft:azure_bluet", "TB.Aurelia", -5, 4,
            new AspectList().add(Aspect.LIGHT, 8).add(Aspect.MAGIC, 6).add(Aspect.LIFE, 4));
    }

    private static AspectList researchTags(String crop) {
        return switch (crop) {
            case "Plax" -> new AspectList().add(Aspect.CROP, 1).add(Aspect.CLOTH, 1);
            case "Metalleat" -> new AspectList().add(Aspect.LIFE, 6).add(Aspect.MAGIC, 6)
                .add(Aspect.METAL, 6).add(Aspect.EXCHANGE, 6);
            case "Lucrite" -> new AspectList().add(Aspect.GREED, 6).add(Aspect.MAGIC, 6)
                .add(Aspect.PLANT, 6).add(Aspect.HUNGER, 6);
            case "Lazullia" -> new AspectList().add(Aspect.LIFE, 6).add(Aspect.MAGIC, 6)
                .add(Aspect.GREED, 6).add(Aspect.CRYSTAL, 6).add(Aspect.PLANT, 6).add(Aspect.SENSES, 8);
            case "Glieonia" -> new AspectList().add(Aspect.LIFE, 6).add(Aspect.LIGHT, 6)
                .add(Aspect.SENSES, 6).add(Aspect.FIRE, 6).add(Aspect.PLANT, 4);
            default -> throw new IllegalArgumentException(crop);
        };
    }

    private static void add(String crop, String catalyst, String parent, int column, int row, AspectList cost) {
        String key = "TB." + crop;
        String seed = crop.toLowerCase(java.util.Locale.ROOT) + "_seeds";
        String id = Bases.id(seed).toString();
        TcStack result = TcStack.of(id);
        TcRecipes.register(new CrucibleRecipe(id, key, result, TcIngredient.item(catalyst), cost));
        var description = ResearchPage.text("tc.research_page." + key + ".1");
        var recipe = ResearchPage.crucible(id);
        var entry = new BasesResearch(key, Bases.ID, researchTags(crop), column, row, crop.equals("Plax") ? 1 : 2, result).setParents(parent);
        if (crop.equals("Lucrite")) entry.setParents(parent, "TB.SMB");
        entry.setPages(description, recipe);
        ResearchCategories.addResearch(entry);
    }
}
