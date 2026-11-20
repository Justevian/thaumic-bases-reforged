package tb;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StemBlock;
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
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchPage;

final class SpreadingCrops {
    private static final ResourceKey<Block> REDLON = ResourceKey.create(Registries.BLOCK, Bases.id("redlon_stem"));
    private static final ResourceKey<Block> ATTACHED_REDLON = ResourceKey.create(Registries.BLOCK, Bases.id("attached_redlon_stem"));
    private static final ResourceKey<Block> FRUIT = ResourceKey.create(Registries.BLOCK, net.minecraft.resources.Identifier.withDefaultNamespace("redstone_block"));
    private static final ResourceKey<net.minecraft.world.item.Item> REDLON_SEEDS =
        ResourceKey.create(Registries.ITEM, Bases.id("redlon_seeds"));
    private static final TagKey<Block> FARMLAND = TagKey.create(Registries.BLOCK, Bases.id("redlon_stem_support"));
    private static final TagKey<Block> FRUIT_SOIL = TagKey.create(Registries.BLOCK, Bases.id("redlon_fruit_support"));

    static void register(DeferredRegister.Blocks blocks, DeferredRegister.Items items) {
        var sweed = blocks.registerBlock("sweed", Sweed::new, p -> plantProperties(p).replaceable());
        items.registerItem("sweed_seeds", p -> new BlockItem(sweed.get(), p));
        var redlon = blocks.registerBlock("redlon_stem", Redlon::new, SpreadingCrops::plantProperties);
        blocks.registerBlock("attached_redlon_stem",
            AttachedRedlon::new,
            SpreadingCrops::plantProperties);
        items.registerItem("redlon_seeds", p -> new BlockItem(redlon.get(), p));
    }

    private static BlockBehaviour.Properties plantProperties(BlockBehaviour.Properties properties) {
        return properties.noCollision().randomTicks().instabreak().sound(SoundType.CROP);
    }

    static final class Sweed extends CropBlock {
        Sweed(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return PlantOutline.at(PlantOutline.SWEED, getAge(state));
        }

        @Override
        public int getMaxAge() {
            return 3;
        }

        @Override
        protected ItemLike getBaseSeedId() {
            return BuiltInRegistries.ITEM.getValue(Bases.id("sweed_seeds"));
        }

        @Override
        protected boolean mayPlaceOn(BlockState soil, BlockGetter level, BlockPos pos) {
            return soil.is(BlockTags.DIRT) || soil.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
        }

        @Override
        protected boolean isRandomlyTicking(BlockState state) {
            return true;
        }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (!isMaxAge(state) && level.getRawBrightness(pos.above(), 0) >= 9 && random.nextInt(5) == 0) {
                level.setBlock(pos, getStateForAge(getAge(state) + 1), Block.UPDATE_CLIENTS);
            }
            if (!isMaxAge(level.getBlockState(pos))) return;
            BlockPos side = pos.relative(Direction.Plane.HORIZONTAL.getRandomDirection(random));
            if (!level.hasChunkAt(side)) return;
            for (int dy = 1; dy >= -2; dy--) {
                BlockPos target = side.above(dy + 1);
                if (!mayPlaceOn(level.getBlockState(target.below()), level, target.below())) continue;
                if (level.isEmptyBlock(target) && defaultBlockState().canSurvive(level, target)) {
                    level.setBlockAndUpdate(target, defaultBlockState());
                }
                break;
            }
        }
    }

    private static final class AttachedRedlon extends AttachedStemBlock {
        AttachedRedlon(BlockBehaviour.Properties properties) {
            super(FRUIT, REDLON, REDLON_SEEDS, FARMLAND, properties);
        }

        @Override
        protected BlockState updateShape(BlockState state, net.minecraft.world.level.LevelReader level,
                                         net.minecraft.world.level.ScheduledTickAccess ticks, BlockPos pos,
                                         Direction direction, BlockPos neighborPos, BlockState neighbor,
                                         RandomSource random) {
            if (direction == state.getValue(FACING) && !neighbor.is(FRUIT)) {
                return BuiltInRegistries.BLOCK.getValue(REDLON).defaultBlockState().setValue(StemBlock.AGE, 7);
            }
            return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random);
        }
    }

    private static final class Redlon extends StemBlock {
        Redlon(BlockBehaviour.Properties properties) {
            super(FRUIT, ATTACHED_REDLON, REDLON_SEEDS, FARMLAND, FRUIT_SOIL, properties);
        }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (random.nextInt(4) == 0) super.randomTick(state, level, pos, random);
        }
    }

    static void research() {
        registerResearch("TB.BasicPlants", "sweed_seeds", "CRUCIBLE", -4, 0,
            TcIngredient.item("minecraft:short_grass"), new AspectList().add(Aspect.MOTION, 2).add(Aspect.LIFE, 1));
        registerResearch("TB.Redlon", "redlon_seeds", "TB.Metalleat", -6, 2,
            TcIngredient.item("minecraft:melon_seeds"), new AspectList().add(Aspect.MECHANISM, 16).add(Aspect.ENERGY, 16));
    }

    private static void registerResearch(String key, String seed, String parent, int x, int y,
                                         TcIngredient catalyst, AspectList cost) {
        String id = Bases.id(seed).toString();
        var result = TcStack.of(id);
        TcRecipes.register(new CrucibleRecipe(id, key, result, catalyst, cost));
        ResearchCategories.addResearch(new BasesResearch(key, Bases.ID, key.equals("TB.Redlon")
                ? new AspectList().add(Aspect.LIFE, 6).add(Aspect.MECHANISM, 6).add(Aspect.ENERGY, 6)
                    .add(Aspect.CROP, 6).add(Aspect.ORDER, 6).add(Aspect.GREED, 8)
                : cost, x, y, key.equals("TB.Redlon") ? 3 : 1, result)
            .setParents(parent).setPages(ResearchPage.text("tc.research_page." + key + ".1"), ResearchPage.crucible(id)));
    }
}
