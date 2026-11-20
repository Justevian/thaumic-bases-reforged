package tb;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.sounds.AmbientLeavesBlockSoundPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.CraftingRecipeRef;
import thaumcraft.crafting.InfusionRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchPage;

final class MagicalTrees {
    private enum Tree {
        GOLDEN_OAK("golden_oak", "GoldenOak"),
        PEACEFUL("peaceful", "PeacefulSapling"),
        NETHER("nether", "NetherSapling"),
        ENDER("ender", "EnderSapling");

        final String id;
        final String research;

        Tree(String id, String research) {
            this.id = id;
            this.research = "TB." + research;
        }

        String sapling() { return id + "_sapling"; }
        String leaves() { return id + "_leaves"; }
        String log() { return id + "_log"; }
    }

    static void register(DeferredRegister.Blocks blocks, DeferredRegister.Items items) {
        for (Tree tree : Tree.values()) {
            var feature = ResourceKey.create(Registries.FEATURE, Bases.id(tree.id + "_tree"));
            var grower = new TreeGrower(Bases.ID + ":" + tree.id, WeightedList.of(feature),
                WeightedList.of(), WeightedList.of(), feature);
            items.registerSimpleBlockItem(blocks.registerBlock(tree.sapling(), p -> new SaplingBlock(grower, p),
                p -> p.noCollision().instabreak().randomTicks().sound(SoundType.GRASS)));
            items.registerSimpleBlockItem(blocks.registerBlock(tree.leaves(),
                p -> new LeavesBlock(AmbientLeavesBlockSoundPlayer.noAmbientSound(), p),
                p -> p.strength(0.2F).randomTicks().noOcclusion().sound(SoundType.GRASS)));
            if (tree != Tree.GOLDEN_OAK) {
                items.registerSimpleBlockItem(blocks.registerBlock(tree.log(), p -> new MagicalLog(tree, p),
                    p -> p.strength(2).randomTicks().sound(SoundType.WOOD)));
            }
        }
        items.registerSimpleBlockItem(blocks.registerSimpleBlock("ender_planks", p -> p.strength(2, 30).sound(SoundType.WOOD)));
    }

    private static final class MagicalLog extends RotatedPillarBlock {
        private final Tree tree;

        MagicalLog(Tree tree, Properties properties) {
            super(properties);
            this.tree = tree;
        }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (state.getValue(BlockStateProperties.AXIS) != net.minecraft.core.Direction.Axis.Y
                || level.getBlockState(pos.below()).is(this) || !level.getBlockState(pos.above()).is(this)) return;
            BlockPos target = pos.offset(random.nextInt(5) - 2, -1, random.nextInt(5) - 2);
            if (!level.hasChunkAt(target)) return;
            if (tree != Tree.PEACEFUL) {
                var ground = level.getBlockState(target);
                if (ground.is(BlockTags.DIRT) || ground.is(Blocks.STONE) || ground.is(Blocks.COBBLESTONE)) {
                    Block replacement = tree == Tree.NETHER ? Blocks.NETHERRACK
                        : random.nextInt(20) == 0 ? Blocks.OBSIDIAN : Blocks.END_STONE;
                    level.setBlockAndUpdate(target, replacement.defaultBlockState());
                }
            }
            if (random.nextInt(8) != 0 || !level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS)) return;
            BlockPos spawn = target.above();
            if (!level.isEmptyBlock(spawn) || !level.isEmptyBlock(spawn.above())) return;
            if (!level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
                new net.minecraft.world.phys.AABB(pos).inflate(8)).isEmpty()) return;
            EntityType<?> entity = switch (tree) {
                case NETHER -> random.nextBoolean() ? EntityTypes.ZOMBIFIED_PIGLIN : EntityTypes.MAGMA_CUBE;
                case ENDER -> EntityTypes.ENDERMAN;
                case PEACEFUL -> level.getChunkSource().getGenerator().getMobsAt(level, level.structureManager(), MobCategory.CREATURE, spawn)
                    .getRandom(random).map(entry -> entry.type()).orElse(null);
                default -> null;
            };
            if (entity != null) entity.spawn(level, spawn, EntitySpawnReason.TRIGGERED);
        }
    }

    static void research() {
        add(Tree.GOLDEN_OAK, "TB.Briar", -7, 5, 2, "minecraft:oak_sapling",
            new AspectList().add(Aspect.PLANT, 16).add(Aspect.GREED, 64),
            "minecraft:apple", "minecraft:apple", "minecraft:golden_apple", "minecraft:golden_apple");
        add(Tree.PEACEFUL, Tree.GOLDEN_OAK.research, -9, 4, 3, "minecraft:oak_sapling",
            new AspectList().add(Aspect.TREE, 16).add(Aspect.LIFE, 12).add(Aspect.HEAL, 6),
            "minecraft:golden_carrot", "thaumcraft:salis_mundus", "minecraft:golden_carrot",
            "thaumcraft:greatwood_sapling", "thaumcraft:salis_mundus", "thaumcraft:nitor",
            "thaumcraft:salis_mundus", "thaumcraft:silverwood_sapling");
        add(Tree.NETHER, Tree.GOLDEN_OAK.research, -9, 6, 5, "thaumcraft:greatwood_sapling",
            new AspectList().add(Aspect.TREE, 32).add(Aspect.FIRE, 32).add(Aspect.HUNGER, 16),
            "minecraft:nether_wart", "thaumcraft:fire_shard", "minecraft:blaze_powder", "thaumcraft:fire_shard",
            "minecraft:nether_wart", "minecraft:netherrack", "thaumcraft:fire_shard", "minecraft:magma_cream",
            "thaumcraft:fire_shard", "minecraft:nether_wart");
        add(Tree.ENDER, Tree.GOLDEN_OAK.research, -9, 8, 7, "thaumcraft:silverwood_sapling",
            new AspectList().add(Aspect.TREE, 64).add(Aspect.ELDRITCH, 24).add(Aspect.VOID, 16).add(Aspect.DARKNESS, 8),
            "thaumcraft:balanced_shard", "minecraft:ender_pearl", "thaumcraft:balanced_shard", "thaumcraft:quicksilver",
            "minecraft:ender_pearl", "minecraft:ender_pearl", "minecraft:end_stone", "thaumcraft:balanced_shard",
            "minecraft:ender_pearl", "thaumcraft:balanced_shard", "thaumcraft:quicksilver");
    }

    private static void add(Tree tree, String parent, int x, int y, int instability, String catalyst,
                            AspectList cost, String... ingredients) {
        var recipe = Bases.id(tree.sapling()).toString();
        var stack = TcStack.of(recipe);
        TcRecipes.register(new InfusionRecipe(recipe, tree.research, stack, instability, cost,
            TcIngredient.item(catalyst), java.util.Arrays.stream(ingredients).map(TcIngredient::item).toList()));
        var entry = new BasesResearch(tree.research, Bases.ID, cost, x, y, 2, stack).setParents(parent, "INFUSION");
        var text = ResearchPage.text("tc.research_page." + tree.research + ".1");
        var infusion = ResearchPage.infusion(recipe);
        if (tree == Tree.ENDER) {
            var planks = Bases.id("ender_planks");
            TcRecipes.register(new CraftingRecipeRef(planks.toString(), tree.research, planks, TcStack.of(planks.toString(), 4)));
            entry.setPages(text, infusion, ResearchPage.crafting(planks.toString()));
        } else {
            entry.setPages(text, infusion);
        }
        ResearchCategories.addResearch(entry);
    }
}
