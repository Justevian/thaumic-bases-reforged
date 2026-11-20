package tb;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.InfusionRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.entity.AspectOrb;
import thaumcraft.node.NodeBlockEntity;
import thaumcraft.node.NodeModifier;
import thaumcraft.node.NodeType;
import thaumcraft.registry.ModBlocks;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchPage;

/** Node Manipulator from Thaumic Bases 1.7.10: a focus-driven device that reshapes the aura node below it. */
final class NodeManipulator extends Block implements EntityBlock {
    private static final String NAME = "node_manipulator";
    static final String RESEARCH = "TB.NodeMan";

    enum Focus {
        BRIGHTNESS("Bright", 0xFFFFFF),
        DESTRUCTION("Destr", 0x4E4756),
        EFFICIENCY("Efficiency", 0xD2D300),
        HUNGER("Hunger", 0xAF7C23),
        INSTABILITY("Unstable", 0x0B4D42),
        PURITY("Purity", 0xCCC8F7),
        SINISTER("Sinister", 0x643C5B),
        SPEED("Speed", 0xEAEAEA),
        STABILITY("Stability", 0xD0E0F8),
        TAINT("Taint", 0x713496);

        final String research;
        final int color;
        DeferredItem<Item> item;

        Focus(String suffix, int color) {
            this.research = "TB.NodeFoci." + suffix;
            this.color = color;
        }

        String id() { return "node_focus_" + name().toLowerCase(java.util.Locale.ROOT); }

        static Focus of(ItemStack stack) {
            for (Focus focus : values()) if (focus.item != null && stack.is(focus.item.get())) return focus;
            return null;
        }
    }

    private static final IntegerProperty FOCUS = IntegerProperty.create("focus", 0, Focus.values().length);
    private static Supplier<NodeManipulator> block;
    private static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Bases.ID);
    private static final Supplier<BlockEntityType<Manipulator>> TYPE = TYPES.register(NAME,
        () -> new BlockEntityType<>(Manipulator::new, block.get()));

    private NodeManipulator(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FOCUS, 0));
    }

    static void register(DeferredRegister.Blocks blocks, DeferredRegister.Items items) {
        var manipulator = blocks.registerBlock(NAME, NodeManipulator::new, p -> p.strength(3, 15).noOcclusion());
        block = manipulator;
        items.registerSimpleBlockItem(manipulator);
        for (Focus focus : Focus.values()) focus.item = items.registerSimpleItem(focus.id(), p -> p.stacksTo(1));
    }

    static void registerTypes(IEventBus bus) {
        TYPES.register(bus);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FOCUS);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Manipulator(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != TYPE.get()) return null;
        return (world, pos, blockState, entity) -> ((Manipulator) entity).tick((ServerLevel) world, blockState);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        Focus focus = Focus.of(stack);
        if (focus == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide()) {
            dropFocus(level, pos, state);
            level.setBlockAndUpdate(pos, state.setValue(FOCUS, focus.ordinal() + 1));
            stack.consume(1, player);
            resetWork(level, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(FOCUS) == 0) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            dropFocus(level, pos, state);
            level.setBlockAndUpdate(pos, state.setValue(FOCUS, 0));
            resetWork(level, pos);
        }
        return InteractionResult.SUCCESS;
    }

    private static void resetWork(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof Manipulator manipulator) {
            manipulator.workTime = 0;
            manipulator.setChanged();
        }
    }

    private static void dropFocus(Level level, BlockPos pos, BlockState state) {
        int index = state.getValue(FOCUS);
        if (index > 0) Block.popResource(level, pos, new ItemStack(Focus.values()[index - 1].item.get()));
    }

    private static NodeBlockEntity node(Level level, BlockPos manipulator) {
        BlockPos below = manipulator.below();
        return level.getBlockState(below).is(ModBlocks.AURA_NODE.get())
            && level.getBlockEntity(below) instanceof NodeBlockEntity aura ? aura : null;
    }

    static final class Manipulator extends BlockEntity {
        private int workTime;
        private final Map<Aspect, Integer> lastAmounts = new HashMap<>();

        Manipulator(BlockPos pos, BlockState state) { super(TYPE.get(), pos, state); }

        void tick(ServerLevel level, BlockState state) {
            NodeBlockEntity node = node(level, worldPosition);
            int index = state.getValue(FOCUS);
            if (node == null || index == 0) {
                if (workTime != 0) {
                    workTime = 0;
                    setChanged();
                }
                remember(node);
                return;
            }
            Focus focus = Focus.values()[index - 1];
            if (level.getGameTime() % 5 == 0) beam(level, focus.color);
            boolean changed = apply(level, node, focus);
            remember(node);
            if (changed) node.nodeChange();
            setChanged();
        }

        private boolean apply(ServerLevel level, NodeBlockEntity node, Focus focus) {
            var random = level.getRandom();
            AspectList aspects = node.getAspects();
            switch (focus) {
                case BRIGHTNESS -> {
                    if (node.getNodeModifier() != null) return false;
                    if (++workTime <= 24000) return false;
                    workTime = 0;
                    node.setNodeModifier(NodeModifier.BRIGHT);
                    return true;
                }
                case DESTRUCTION -> {
                    NodeModifier modifier = node.getNodeModifier();
                    int required = modifier == NodeModifier.BRIGHT ? 1200
                        : modifier == NodeModifier.PALE ? 3600 : modifier == NodeModifier.FADING ? 7200 : 2400;
                    if (workTime >= required) {
                        workTime = 0;
                        if (modifier == NodeModifier.FADING) {
                            level.removeBlock(worldPosition.below(), false);
                            return false;
                        }
                        node.setNodeModifier(modifier == NodeModifier.BRIGHT ? null : modifier == NodeModifier.PALE
                            ? NodeModifier.FADING : NodeModifier.PALE);
                        return true;
                    }
                    workTime++;
                    List<Aspect> present = aspects.getAspects();
                    if (level.getGameTime() % 10 == 0 && !present.isEmpty()) {
                        AspectOrb orb = AspectOrb.create(level, worldPosition.getX() + 0.5, worldPosition.getY() - 0.5,
                            worldPosition.getZ() + 0.5, present.get(random.nextInt(present.size())), 1);
                        if (orb != null) level.addFreshEntity(orb);
                    }
                    return false;
                }
                case EFFICIENCY -> {
                    boolean changed = false;
                    for (Aspect aspect : aspects.getAspects()) {
                        Integer previous = lastAmounts.get(aspect);
                        if (previous != null && aspects.getAmount(aspect) < previous && random.nextFloat() < 0.5F) {
                            aspects.add(aspect, 1);
                            changed = true;
                        }
                    }
                    return changed;
                }
                case HUNGER -> { return convert(node, NodeType.HUNGRY, 6000); }
                case INSTABILITY -> { return convert(node, NodeType.UNSTABLE, 8400); }
                case PURITY -> { return convert(node, NodeType.PURE, 3600); }
                case SINISTER -> { return convert(node, NodeType.DARK, 7200); }
                case TAINT -> { return convert(node, NodeType.TAINTED, 7200); }
                case SPEED -> {
                    boolean changed = false;
                    for (Aspect aspect : aspects.getAspects()) {
                        if (aspects.getAmount(aspect) < node.getNodeVisBase(aspect) && random.nextFloat() < 0.01F) {
                            aspects.add(aspect, 1);
                            changed = true;
                        }
                    }
                    return changed;
                }
                case STABILITY -> {
                    NodeModifier modifier = node.getNodeModifier();
                    NodeType type = node.getNodeType();
                    int required = 0;
                    if (modifier == NodeModifier.FADING) required = 6000;
                    if (modifier == NodeModifier.PALE) required = 12000;
                    if (type == NodeType.DARK) required = 2400;
                    if (type == NodeType.HUNGRY) required = 600;
                    if (type == NodeType.UNSTABLE) required = 8400;
                    if (workTime < required) {
                        workTime++;
                        return false;
                    }
                    workTime = 0;
                    if (modifier == NodeModifier.FADING) node.setNodeModifier(NodeModifier.PALE);
                    else if (modifier == NodeModifier.PALE) node.setNodeModifier(null);
                    else if (type == NodeType.DARK || type == NodeType.HUNGRY || type == NodeType.UNSTABLE) node.setNodeType(NodeType.NORMAL);
                    else return false;
                    return true;
                }
            }
            return false;
        }

        private boolean convert(NodeBlockEntity node, NodeType target, int time) {
            if (node.getNodeType() != NodeType.NORMAL) return false;
            if (++workTime <= time) return false;
            workTime = 0;
            node.setNodeType(target);
            return true;
        }

        private void remember(NodeBlockEntity node) {
            lastAmounts.clear();
            if (node == null) return;
            for (Aspect aspect : node.getAspects().getAspects()) lastAmounts.put(aspect, node.getAspects().getAmount(aspect));
        }

        private void beam(ServerLevel level, int color) {
            var dust = new DustParticleOptions(color, 0.8F);
            Vec3 from = Vec3.atCenterOf(worldPosition);
            Vec3 to = Vec3.atCenterOf(worldPosition.below());
            for (int step = 0; step <= 4; step++) {
                Vec3 point = from.lerp(to, step / 4.0);
                level.sendParticles(dust, point.x, point.y, point.z, 1, 0, 0, 0, 0);
            }
        }

        @Override
        public void preRemoveSideEffects(BlockPos pos, BlockState state) {
            if (getLevel() != null && !getLevel().isClientSide()) dropFocus(getLevel(), pos, state);
            super.preRemoveSideEffects(pos, state);
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            output.putInt("workTime", workTime);
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            workTime = Math.max(0, input.getIntOr("workTime", 0));
        }
    }

    static void research() {
        String id = Bases.id(NAME).toString();
        var stabilizer = TcIngredient.item("thaumcraft:advanced_node_stabilizer");
        var magicMirror = TcIngredient.item("thaumcraft:magic_mirror");
        var essentiaMirror = TcIngredient.item("thaumcraft:essentia_mirror");
        var relay = TcIngredient.item("thaumcraft:vis_relay");
        var salt = TcIngredient.item(Bases.id("salis_mundus_block").toString());
        TcRecipes.register(new InfusionRecipe(id, RESEARCH, TcStack.of(id), 6,
            new AspectList().add(Aspect.MECHANISM, 32).add(Aspect.AURA, 32).add(Aspect.MAGIC, 16).add(Aspect.ENERGY, 64).add(Aspect.VOID, 16),
            TcIngredient.item("thaumcraft:node_transducer"),
            List.of(stabilizer, magicMirror, essentiaMirror, relay, salt, stabilizer, magicMirror, essentiaMirror, relay, salt)));
        ResearchCategories.addResearch(new BasesResearch(RESEARCH, Bases.ID,
            new AspectList().add(Aspect.MECHANISM, 8).add(Aspect.AURA, 8).add(Aspect.ENERGY, 16).add(Aspect.MAGIC, 8),
            0, -25, 3, TcStack.of(id)).setParents("INFUSION", "NODESTABILIZERADV", "VISPOWER").setConcealed()
            .setPages(ResearchPage.text("tc.research_page." + RESEARCH + ".1"), ResearchPage.text("tc.research_page." + RESEARCH + ".2"),
                ResearchPage.text("tc.research_page." + RESEARCH + ".3"), ResearchPage.text("tc.research_page." + RESEARCH + ".4"),
                ResearchPage.infusion(id)));

        var charm = TcIngredient.item("thaumcraft:primal_charm");
        var salis = TcIngredient.item("thaumcraft:salis_mundus");
        var saltBlock = salt;
        var tainted = crystal("tainted");
        var entropy = crystal("entropy");
        var mixed = crystal("mixed");
        var excavation = TcIngredient.item("thaumcraft:focus_excavation");
        var pech = TcIngredient.item("thaumcraft:focus_pech");
        var wisp = TcIngredient.item("thaumcraft:wisp_essence");

        focus(Focus.BRIGHTNESS, 3, aura(256).add(Aspect.LIGHT, 64).add(Aspect.ENERGY, 256), TcIngredient.item("thaumcraft:primordial_pearl"),
            alternate(TcIngredient.item("thaumcraft:arcane_lamp"), salis, TcIngredient.item("thaumcraft:cinderpearl"), salis),
            2, -28, new AspectList().add(Aspect.AURA, 8).add(Aspect.LIGHT, 8), "PRIMPEARL");
        focus(Focus.DESTRUCTION, 5, aura(64).add(Aspect.VOID, 16).add(Aspect.DEATH, 32), charm,
            alternate(TcIngredient.item("thaumcraft:amber"), tainted, TcIngredient.item("thaumcraft:essentia_crystallizer"), excavation),
            1, -28, new AspectList().add(Aspect.AURA, 8).add(Aspect.DEATH, 8).add(Aspect.VOID, 8));
        focus(Focus.EFFICIENCY, 1, aura(64).add(Aspect.VOID, 16).add(Aspect.DEATH, 32), charm,
            alternate(TcIngredient.item("thaumcraft:balanced_shard"), saltBlock),
            -1, -28, new AspectList().add(Aspect.AURA, 8).add(Aspect.ENERGY, 16));
        focus(Focus.HUNGER, 7, aura(64).add(Aspect.VOID, 64).add(Aspect.HUNGER, 256).add(Aspect.GREED, 64).add(Aspect.ENERGY, 128), charm,
            alternate(salis, TcIngredient.item("thaumcraft:thaumometer"), salis, TcIngredient.item("thaumcraft:node_transducer")),
            0, -29, new AspectList().add(Aspect.AURA, 8).add(Aspect.DEATH, 16).add(Aspect.ENERGY, 16));
        focus(Focus.INSTABILITY, 3, aura(64).add(Aspect.DEATH, 32).add(Aspect.ENTROPY, 64), charm,
            alternate(TcIngredient.item("minecraft:ender_pearl"), entropy),
            -2, -28, new AspectList().add(Aspect.AURA, 8).add(Aspect.ENTROPY, 8).add(Aspect.VOID, 8));
        focus(Focus.PURITY, 0, aura(64).add(Aspect.LIFE, 64).add(Aspect.ORDER, 64), charm,
            List.of(TcIngredient.item("thaumcraft:silverwood_sapling"), TcIngredient.item("thaumcraft:ethereal_bloom"),
                TcIngredient.item("thaumcraft:silverwood_log"), TcIngredient.item("thaumcraft:shimmerleaf"),
                TcIngredient.item("thaumcraft:silverwood_leaves"), TcIngredient.item("thaumcraft:ethereal_bloom"),
                TcIngredient.item("thaumcraft:silverwood_log"), TcIngredient.item("thaumcraft:shimmerleaf")),
            3, -27, new AspectList().add(Aspect.AURA, 8).add(Aspect.LIFE, 8).add(Aspect.ORDER, 8));
        focus(Focus.SINISTER, 4, aura(64).add(Aspect.DARKNESS, 32).add(Aspect.UNDEAD, 64), charm,
            alternate(wisp, tainted, pech, mixed),
            -3, -27, new AspectList().add(Aspect.AURA, 8).add(Aspect.UNDEAD, 8).add(Aspect.ENTROPY, 8));
        focus(Focus.SPEED, 1, aura(64).add(Aspect.MOTION, 32).add(Aspect.MAGIC, 16), charm,
            alternate(mixed, saltBlock),
            -4, -26, new AspectList().add(Aspect.AURA, 8).add(Aspect.MOTION, 8).add(Aspect.ENERGY, 8));
        focus(Focus.STABILITY, 1, aura(64).add(Aspect.ORDER, 32).add(Aspect.EXCHANGE, 16), charm,
            alternate(saltBlock, TcIngredient.item("thaumcraft:mirrored_glass"), saltBlock, mixed),
            4, -26, new AspectList().add(Aspect.AURA, 8).add(Aspect.ORDER, 8).add(Aspect.EXCHANGE, 8));
        focus(Focus.TAINT, 5, aura(64).add(Aspect.TAINT, 64).add(Aspect.ENTROPY, 16), charm,
            alternate(TcIngredient.item("thaumcraft:tainted_goo"), tainted, TcIngredient.item("thaumcraft:taint_tendril"),
                TcIngredient.item(Bases.id("tainted_mysteria_fragment").toString())),
            0, -31, new AspectList().add(Aspect.AURA, 8).add(Aspect.TAINT, 16).add(Aspect.ENERGY, 8));
    }

    private static AspectList aura(int amount) { return new AspectList().add(Aspect.AURA, amount); }

    private static TcIngredient crystal(String aspect) { return TcIngredient.item(Bases.id(aspect + "_crystal_block").toString()); }

    /** Repeats the given pattern until eight infusion components are filled, as the original recipes did. */
    private static List<TcIngredient> alternate(TcIngredient... pattern) {
        var components = new java.util.ArrayList<TcIngredient>(8);
        for (int i = 0; i < 8; i++) components.add(pattern[i % pattern.length]);
        return components;
    }

    private static void focus(Focus focus, int instability, AspectList cost, TcIngredient center, List<TcIngredient> components,
                              int column, int row, AspectList tags, String... extraParents) {
        String id = Bases.id(focus.id()).toString();
        TcRecipes.register(new InfusionRecipe(id, focus.research, TcStack.of(id), instability, cost, center, components));
        String[] parents = new String[extraParents.length + 1];
        parents[0] = RESEARCH;
        System.arraycopy(extraParents, 0, parents, 1, extraParents.length);
        var entry = new BasesResearch(focus.research, Bases.ID, tags, column, row, 0, TcStack.of(id))
            .setParents(parents).setSecondary().setConcealed();
        if (focus == Focus.HUNGER) entry.setSpecial();
        ResearchCategories.addResearch(entry.setPages(ResearchPage.text("tc.research_page." + focus.research + ".1"),
            ResearchPage.infusion(id)));
    }
}
