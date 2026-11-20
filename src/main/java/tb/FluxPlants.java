package tb;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.CrucibleRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.node.NodeHooks;
import thaumcraft.registry.ModBlocks;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchPage;

final class FluxPlants {
    static void register(DeferredRegister.Blocks blocks, DeferredRegister.Items items) {
        var ashroom = blocks.registerBlock("ashroom", Ashroom::new,
            p -> p.noCollision().instabreak().randomTicks().sound(SoundType.CROP));
        var flaxium = blocks.registerBlock("flaxium", Flaxium::new,
            p -> p.noCollision().instabreak().randomTicks().sound(SoundType.GRASS));
        items.registerSimpleBlockItem(ashroom);
        items.registerSimpleBlockItem(flaxium);
    }

    static final class Ashroom extends CropBlock {
        Ashroom(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return PlantOutline.at(PlantOutline.ASHROOM, getAge(state));
        }

        @Override
        public int getMaxAge() {
            return 3;
        }

        @Override
        protected ItemLike getBaseSeedId() {
            return this;
        }

        @Override
        protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
            return state.is(BlockTags.DIRT) || state.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
        }

        @Override
        protected int getBonemealAgeIncrease(Level level) {
            return 1;
        }

        @Override
        protected void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean dropExperience) {
            super.spawnAfterBreak(state, level, pos, tool, dropExperience);
            if (!isMaxAge(state)) return;
            var primals = Aspect.getPrimalAspects();
            var random = level.getRandom();
            int count = 8 + random.nextInt(32);
            for (int i = 0; i < count; i++) {
                NodeHooks.spawnAspectOrb(level, pos, primals.get(random.nextInt(primals.size())), 1);
            }
        }
    }

    static final class Flaxium extends VegetationBlock {
        Flaxium(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return PlantOutline.FLAXIUM;
        }

        @Override
        protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
            return state.is(BlockTags.DIRT) || state.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
        }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (level.isEmptyBlock(pos.above())) {
                level.setBlockAndUpdate(pos.above(), ModBlocks.FLUX_GAS.get().defaultBlockState());
            }
        }
    }

    static void research() {
        add("Ashroom", "ashroom", "thaumcraft:vishroom", -6, 4,
            new AspectList().add(Aspect.ENTROPY, 4).add(Aspect.CROP, 2),
            new AspectList().add(Aspect.LIFE, 6).add(Aspect.ENTROPY, 6)
                .add(Aspect.AURA, 6).add(Aspect.AIR, 6).add(Aspect.CROP, 4));
        add("Flaxium", "flaxium", "minecraft:allium", -7, 2,
            new AspectList().add(Aspect.ENTROPY, 1).add(Aspect.MAGIC, 2),
            new AspectList().add(Aspect.LIFE, 6).add(Aspect.ENTROPY, 6)
                .add(Aspect.AURA, 6).add(Aspect.MAGIC, 6));
    }

    private static void add(String title, String block, String catalyst, int x, int y,
                            AspectList cost, AspectList tags) {
        String key = "TB." + title;
        String id = Bases.id(block).toString();
        var result = TcStack.of(id);
        TcRecipes.register(new CrucibleRecipe(id, key, result, TcIngredient.item(catalyst), cost));
        ResearchCategories.addResearch(new BasesResearch(key, Bases.ID, tags, x, y, 2, result)
            .setParents("TB.Aurelia")
            .setPages(ResearchPage.text("tc.research_page." + key + ".1"), ResearchPage.crucible(id)));
    }
}
