package tb;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectHelper;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.InfusionRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchManager;
import thaumcraft.research.ResearchPage;
import thaumcraft.research.Warp;
import thaumcraft.research.WarpType;

final class EntityDeconstructor extends Block implements EntityBlock {
    private static final String NAME = "entity_deconstructor";
    private static final String IMPROVEMENTS = "TB.EntityDecAdv";
    private static final List<String> CRYSTALS = List.of("air", "fire", "water", "earth", "order", "entropy");
    private static final int FULL = (1 << CRYSTALS.size()) - 1;
    private static final IntegerProperty CRYSTAL_MASK = IntegerProperty.create("crystals", 0, FULL);
    private static java.util.function.Supplier<EntityDeconstructor> block;
    private static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Bases.ID);
    private static final java.util.function.Supplier<BlockEntityType<Chamber>> TYPE = TYPES.register(NAME,
        () -> new BlockEntityType<>(Chamber::new, block.get()));

    private EntityDeconstructor(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(CRYSTAL_MASK, 0));
    }

    static void register(DeferredRegister.Blocks blocks, DeferredRegister.Items items) {
        var deconstructor = blocks.registerBlock(NAME, EntityDeconstructor::new, p -> p.strength(3, 15).noOcclusion());
        block = deconstructor;
        items.registerSimpleBlockItem(deconstructor);
    }

    static void registerEntities(IEventBus bus) {
        TYPES.register(bus);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CRYSTAL_MASK);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Chamber(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != TYPE.get()) return null;
        return (world, pos, blockState, entity) -> ((Chamber) entity).tick((ServerLevel) world);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof Chamber chamber)) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            if (chamber.owner == null) {
                chamber.owner = player.getUUID();
                chamber.setChanged();
                message(player, "claimed");
            } else if (chamber.owner.equals(player.getUUID())) {
                message(player, "crystals", Integer.bitCount(state.getValue(CRYSTAL_MASK)));
            } else {
                message(player, "owned");
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof Chamber chamber)) return InteractionResult.PASS;
        var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!id.getNamespace().equals(Bases.ID) || !id.getPath().endsWith("_crystal_block")) return InteractionResult.TRY_WITH_EMPTY_HAND;
        int index = CRYSTALS.indexOf(id.getPath().replace("_crystal_block", ""));
        if (index < 0) return InteractionResult.TRY_WITH_EMPTY_HAND;
        int current = state.getValue(CRYSTAL_MASK);
        int bit = 1 << index;
        if (!level.isClientSide()) {
            if (!player.getUUID().equals(chamber.owner)) {
                message(player, "owned");
            } else if (!player.hasInfiniteMaterials() && !thaumcraft.research.Knowledge.get(player).isResearchComplete(IMPROVEMENTS)) {
                message(player, "research");
            } else if ((current & bit) == 0 && level.setBlockAndUpdate(pos, state.setValue(CRYSTAL_MASK, current | bit))) {
                stack.consume(1, player);
                message(player, "crystals", Integer.bitCount(current | bit));
            }
        }
        return InteractionResult.SUCCESS;
    }

    private static void message(Player player, String key, Object... values) {
        player.sendSystemMessage(Component.translatable("message.thaumicbases.entity_deconstructor." + key, values));
    }

    static final class Chamber extends BlockEntity {
        private UUID owner;
        private int ticks;

        Chamber(BlockPos pos, BlockState state) {
            super(TYPE.get(), pos, state);
        }

        void tick(ServerLevel level) {
            if (owner == null) return;
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
            if (player == null) return;
            int crystals = Integer.bitCount(getBlockState().getValue(CRYSTAL_MASK));
            int interval = Math.ceilDiv(40, 1 + crystals / 2);
            if (++ticks < interval) return;
            ticks = 0;
            runCycle(level, player, crystals);
        }

        void runCycle(ServerLevel level, ServerPlayer player, int crystals) {
            Vec3 center = Vec3.atBottomCenterOf(worldPosition.above());
            double distance = player.level() == level ? Math.sqrt(player.distanceToSqr(center)) : Double.POSITIVE_INFINITY;
            int range = 128 + 10 * crystals;
            if (crystals < 6 && distance > range) return;

            var area = new AABB(worldPosition.above());
            for (var entity : level.getEntitiesOfClass(LivingEntity.class, area,
                mob -> mob.isAlive() && !mob.isSpectator() && !mob.getUUID().equals(owner)
                    && (!(mob instanceof Player target) || !target.hasInfiniteMaterials()))) {
                AspectList aspects = AspectHelper.getEntityTags(entity);
                if (aspects == null || aspects.isEmpty() || !entity.hurtServer(level, level.damageSources().magic(), 1)) continue;
                reward(level, player, crystals, distance, range, aspects);
                particles(level, center);
                return;
            }
            if (crystals < 6) return;
            for (var item : level.getEntitiesOfClass(ItemEntity.class, area, entity -> entity.isAlive())) {
                AspectList tags = AspectHelper.getBonusTags(item.getItem(), AspectHelper.getObjectTags(item.getItem()));
                if (tags == null || tags.isEmpty()) continue;
                AspectList primals = AspectHelper.reduceToPrimals(tags);
                if (primals.isEmpty()) continue;
                var stack = item.getItem().copy();
                stack.shrink(1);
                if (stack.isEmpty()) item.discard();
                else item.setItem(stack);
                if (level.getRandom().nextInt(40) < primals.visSize()) reward(level, player, crystals, distance, range, primals);
                particles(level, center);
                return;
            }
        }

        private static void reward(ServerLevel level, ServerPlayer player, int crystals, double distance, int range, AspectList aspects) {
            var random = level.getRandom();
            if (crystals < 6 && random.nextInt(Math.max(1, (int) (range - distance))) == 0) {
                Warp.add(player, 1, WarpType.TEMPORARY);
                return;
            }
            var candidates = aspects.getAspects();
            ResearchManager.addAspectPool(player, candidates.get(random.nextInt(candidates.size())), 1);
        }

        private static void particles(ServerLevel level, Vec3 center) {
            level.sendParticles(ParticleTypes.ENCHANT, center.x, center.y + 0.15, center.z, 8, 0.25, 0.3, 0.25, 0.02);
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            if (owner != null) output.store("owner", net.minecraft.core.UUIDUtil.CODEC, owner);
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            owner = input.read("owner", net.minecraft.core.UUIDUtil.CODEC).orElse(null);
        }
    }

    static void research() {
        String id = Bases.id(NAME).toString();
        String key = "TB.EntityDec";
        var result = TcStack.of(id);
        var glass = TcIngredient.item("thaumcraft:warded_glass");
        var gold = TcIngredient.item("minecraft:gold_block");
        var mechanism = TcIngredient.item("thaumcraft:alchemical_construct");
        var pearl = TcIngredient.item("thaumcraft:primordial_pearl");
        TcRecipes.register(new InfusionRecipe(id, key, result, 4,
            new AspectList().add(Aspect.DEATH, 16).add(Aspect.SOUL, 4).add(Aspect.MIND, 16),
            TcIngredient.item("thaumcraft:thaumometer"), List.of(glass, glass, gold, gold, mechanism, mechanism, pearl, pearl)));
        ResearchCategories.addResearch(new BasesResearch(key, Bases.ID,
            new AspectList().add(Aspect.DEATH, 8).add(Aspect.SOUL, 8).add(Aspect.MIND, 8), 7, -19, 1, result)
            .setParents("INFUSION").setPages(ResearchPage.text("tc.research_page." + key + ".1"), ResearchPage.infusion(id)));
        ResearchCategories.addResearch(new BasesResearch(IMPROVEMENTS, Bases.ID,
            new AspectList().add(Aspect.ORDER, 8).add(Aspect.CRYSTAL, 8), 9, -19, 1, result)
            .setParents(key).setPages(ResearchPage.text("tc.research_page." + IMPROVEMENTS + ".1")));
    }
}
