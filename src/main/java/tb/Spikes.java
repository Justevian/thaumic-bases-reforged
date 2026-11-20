package tb;

import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.CrucibleRecipe;
import thaumcraft.crafting.ShapedArcaneRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.registry.ModItems;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchPage;

final class Spikes {
    private static final GameProfile PROFILE = new GameProfile(UUID.fromString("bdf19fd7-3f97-45ed-adf8-5e79afb488cb"), "ThaumicBasesSpike");
    private static final String EXTRACTOR = "enchanted_fabric";
    enum Kind {
        IRON("iron", "Iron", 8),
        THAUMIC("thaumic", "Thaumic", 14),
        VOID("void", "Void", 20);

        final String id;
        final String key;
        final int damage;
        Kind(String id, String name, int damage) {
            this.id = id + "_spike";
            this.key = "TB.Spike." + name;
            this.damage = damage;
        }
    }

    private static net.neoforged.neoforge.registries.DeferredItem<net.minecraft.world.item.Item> blood;

    static void register(DeferredRegister.Blocks blocks, DeferredRegister.Items items) {
        blood = items.registerSimpleItem("bloody_cloth");
        for (var kind : Kind.values()) {
            items.registerSimpleBlockItem(blocks.registerBlock(kind.id, p -> new Spike(p, kind),
                p -> p.strength(1.5F).noCollision().sound(net.minecraft.world.level.block.SoundType.METAL)));
        }
    }

    static final class Spike extends Block {
        static final BooleanProperty BLOODY = BooleanProperty.create("bloody");
        private static final VoxelShape SHAPE = Block.column(14, 0, 15);
        private final Kind kind;

        Spike(Properties props, Kind kind) {
            super(props);
            this.kind = kind;
            registerDefaultState(defaultBlockState().setValue(BLOODY, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(BLOODY);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                                    InsideBlockEffectApplier effects, boolean moving) {
            if (!(level instanceof ServerLevel server) || !(entity instanceof LivingEntity living) || !living.isAlive()) return;
            if (kind == Kind.THAUMIC && level.hasNeighborSignal(pos)) return;
            if (kind == Kind.VOID && living instanceof Player) return;
            var damage = kind == Kind.VOID
                ? level.damageSources().playerAttack(FakePlayerFactory.get(server, PROFILE))
                : level.damageSources().cactus();
            if (living.hurtServer(server, damage, kind.damage) && !living.isAlive() && !state.getValue(BLOODY)) {
                level.setBlockAndUpdate(pos, state.setValue(BLOODY, true));
            }
        }

        @Override
        protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
            if (!state.getValue(BLOODY) || !stack.is(ModItems.ENCHANTED_FABRIC.get())) {
                return InteractionResult.TRY_WITH_EMPTY_HAND;
            }
            if (!level.isClientSide()) {
                stack.consume(1, player);
                var collected = new ItemStack(blood.get());
                if (!player.addItem(collected)) player.drop(collected, false, net.minecraft.util.Prediction.PREDICTED);
                level.setBlockAndUpdate(pos, state.setValue(BLOODY, false));
            }
            return InteractionResult.SUCCESS;
        }
    }

    static void research() {
        String fabric = "thaumcraft:" + EXTRACTOR;
        String bloodId = Bases.id("bloody_cloth").toString();
        TcRecipes.register(new CrucibleRecipe(bloodId, Kind.IRON.key, TcStack.of(fabric),
            TcIngredient.item(bloodId), new AspectList().add(Aspect.ORDER, 1)));
        add(Kind.IRON, "ENCHFABRIC", "minecraft:iron_nugget", "minecraft:iron_ingot", "minecraft:light_weighted_pressure_plate",
            new AspectList().add(Aspect.FIRE, 1).add(Aspect.ENTROPY, 1));
        add(Kind.THAUMIC, Kind.IRON.key, "thaumcraft:thaumium_nugget", "thaumcraft:thaumium_ingot", id(Kind.IRON), primals(4));
        add(Kind.VOID, Kind.THAUMIC.key, "thaumcraft:void_metal_nugget", "thaumcraft:void_metal_ingot", id(Kind.THAUMIC), primals(4));
    }

    private static void add(Kind kind, String parent, String nugget, String ingot, String base, AspectList cost) {
        var n = TcIngredient.item(nugget);
        var m = TcIngredient.item(ingot);
        var empty = TcIngredient.EMPTY;
        String id = id(kind);
        var stack = TcStack.of(id);
        TcRecipes.register(new ShapedArcaneRecipe(id, kind.key, stack, cost, 3, 3,
            List.of(empty, n, empty, n, m, n, m, TcIngredient.item(base), m)));
        var entry = new BasesResearch(kind.key, Bases.ID, new AspectList().add(Aspect.WEAPON, 4).add(Aspect.METAL, 4),
            3 + kind.ordinal() * 2, -4, 2, stack).setParents(parent);
        if (kind == Kind.THAUMIC) entry.setParents(parent, "THAUMIUM");
        if (kind == Kind.VOID) entry.setParents(parent, "VOIDMETAL");
        var page = ResearchPage.text("tc.research_page." + kind.key + ".1");
        if (kind == Kind.IRON) entry.setPages(page, ResearchPage.arcane(id), ResearchPage.crucible(Bases.id("bloody_cloth").toString()));
        else entry.setPages(page, ResearchPage.arcane(id));
        ResearchCategories.addResearch(entry);
    }

    private static String id(Kind kind) { return Bases.id(kind.id).toString(); }

    private static AspectList primals(int amount) {
        var result = new AspectList();
        for (var aspect : Aspect.getPrimalAspects()) result.add(aspect, amount);
        return result;
    }
}
