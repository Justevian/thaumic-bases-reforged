package tb;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.InfusionRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.essentia.EssentiaHandler;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchPage;
import thaumcraft.wand.WandInteractable;
import thaumcraft.wand.Wands;

final class Overchanter extends Block implements EntityBlock, WandInteractable {
    private static final String NAME = "overchanter";
    private static final int ESSENTIA_COST = 32;
    private static final int LEVEL_COST = 30;
    private static final String MARK_PREFIX = "thaumicbases:overchanted/";
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 12, 16);
    private static java.util.function.Supplier<Overchanter> block;
    private static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Bases.ID);
    private static final java.util.function.Supplier<BlockEntityType<Table>> TYPE = TYPES.register(NAME,
        () -> new BlockEntityType<>(Table::new, block.get()));

    private Overchanter(Properties properties) {
        super(properties);
    }

    static void register(DeferredRegister.Blocks blocks, DeferredRegister.Items items) {
        var table = blocks.registerBlock(NAME, Overchanter::new,
            p -> p.strength(5, 20).noOcclusion().requiresCorrectToolForDrops());
        block = table;
        items.registerSimpleBlockItem(table);
    }

    static void registerEntities(IEventBus bus) {
        TYPES.register(bus);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Table(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != TYPE.get()) return null;
        return (world, pos, blockState, entity) -> ((Table) entity).process((ServerLevel) world);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (Wands.isWand(stack) || !(level.getBlockEntity(pos) instanceof Table table) || !table.stack.isEmpty()
            || nextEnchantment(stack) == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide()) {
            table.stack = stack.copyWithCount(1);
            stack.consume(1, player);
            table.setChanged();
            status(player, "loaded");
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof Table table)) return InteractionResult.PASS;
        if (level instanceof ServerLevel server) {
            if (table.stack.isEmpty()) {
                status(player, "empty");
            } else {
                ItemStack result = table.remove();
                if (!player.addItem(result)) Block.popResource(server, pos.above(), result);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult onWandUseOn(Level level, BlockPos pos, Player player, ItemStack wand, Direction side) {
        if (!(level.getBlockEntity(pos) instanceof Table table)) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            if (table.stack.isEmpty()) status(player, "empty");
            else if (nextEnchantment(table.stack) == null) status(player, "exhausted");
            else if (table.running) status(player, "running");
            else if (!table.armed) {
                table.armed = true;
                table.setChanged();
                status(player, "armed");
            } else {
                table.running = true;
                table.setChanged();
                status(player, "running");
            }
        }
        return InteractionResult.SUCCESS;
    }

    private static void status(Player player, String key) {
        player.sendSystemMessage(Component.translatable("message.thaumicbases.overchanter." + key));
    }

    private static net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> nextEnchantment(ItemStack stack) {
        var marks = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        for (var enchantment : EnchantmentHelper.getEnchantmentsForCrafting(stack).keySet()) {
            var id = enchantment.unwrapKey();
            if (id.isPresent() && !marks.getBooleanOr(MARK_PREFIX + id.get().identifier(), false)) return enchantment;
        }
        return null;
    }

    static boolean upgrade(ItemStack stack) {
        var enchantment = nextEnchantment(stack);
        if (enchantment == null) return false;
        var enchantments = new ItemEnchantments.Mutable(EnchantmentHelper.getEnchantmentsForCrafting(stack));
        enchantments.set(enchantment, enchantments.getLevel(enchantment) + 1);
        EnchantmentHelper.setEnchantments(stack, enchantments.toImmutable());
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
            tag -> tag.putBoolean(MARK_PREFIX + enchantment.unwrapKey().orElseThrow().identifier(), true));
        return true;
    }

    static final class Table extends BlockEntity {
        private ItemStack stack = ItemStack.EMPTY;
        private boolean armed;
        private boolean running;
        private boolean paid;
        private int drawn;

        Table(BlockPos pos, BlockState state) {
            super(TYPE.get(), pos, state);
        }

        private void process(ServerLevel level) {
            if (!running || stack.isEmpty() || level.getGameTime() % 10 != 0) return;
            if (drawn >= ESSENTIA_COST / 2 && !paid) {
                var center = net.minecraft.world.phys.Vec3.atCenterOf(worldPosition);
                var payer = level.getNearestPlayer(center.x, center.y, center.z, 6,
                    entity -> entity instanceof Player player &&
                        !player.isSpectator() && (player.hasInfiniteMaterials() || player.experienceLevel >= LEVEL_COST));
                if (payer == null) return;
                if (!payer.hasInfiniteMaterials()) payer.giveExperienceLevels(-LEVEL_COST);
                paid = true;
                setChanged();
            }
            if (!EssentiaHandler.drainEssentia(this, Aspect.MAGIC, null, 8)) return;
            drawn++;
            level.sendParticles(ParticleTypes.ENCHANT, worldPosition.getX() + 0.5, worldPosition.getY() + 0.8,
                worldPosition.getZ() + 0.5, 6, 0.3, 0.15, 0.3, 0.02);
            if (drawn == ESSENTIA_COST) {
                upgrade(stack);
                reset();
            }
            setChanged();
        }

        private ItemStack remove() {
            ItemStack result = stack;
            stack = ItemStack.EMPTY;
            reset();
            setChanged();
            return result;
        }

        private void reset() {
            armed = false;
            running = false;
            paid = false;
            drawn = 0;
        }

        @Override
        public void preRemoveSideEffects(BlockPos pos, BlockState state) {
            if (getLevel() instanceof ServerLevel server && !stack.isEmpty()) Block.popResource(server, pos, remove());
            super.preRemoveSideEffects(pos, state);
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            output.store("item", ItemStack.OPTIONAL_CODEC, stack);
            output.putBoolean("armed", armed);
            output.putBoolean("running", running);
            output.putBoolean("paid", paid);
            output.putInt("drawn", drawn);
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            stack = input.read("item", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
            armed = input.getBooleanOr("armed", false);
            running = input.getBooleanOr("running", false);
            paid = input.getBooleanOr("paid", false);
            drawn = input.getIntOr("drawn", 0);
        }
    }

    static void research() {
        String key = "TB.Overchanter";
        String id = Bases.id(NAME).toString();
        var result = TcStack.of(id);
        var cost = new AspectList().add(Aspect.MAGIC, 32).add(Aspect.MECHANISM, 32).add(Aspect.MIND, 16);
        for (var aspect : Aspect.getPrimalAspects()) cost.add(aspect, 32);
        var components = new ArrayList<TcIngredient>();
        for (String aspect : List.of("fire", "water", "earth", "air", "order", "entropy", "mixed", "mixed"))
            components.add(TcIngredient.item(Bases.id(aspect + "_crystal_block").toString()));
        for (int i = 0; i < 2; i++) {
            components.add(TcIngredient.item(Bases.id("salis_mundus_block").toString()));
            components.add(TcIngredient.item("thaumcraft:thaumium_block"));
        }
        TcRecipes.register(new InfusionRecipe(id, key, result, 4, cost, TcIngredient.item("minecraft:enchanting_table"), components));
        ResearchCategories.addResearch(new BasesResearch(key, Bases.ID,
            new AspectList().add(Aspect.MAGIC, 8).add(Aspect.MIND, 8).add(Aspect.MECHANISM, 8), -12, 4, 3, result)
            .setParents("INFUSION")
            .setPages(ResearchPage.text("tc.research_page." + key + ".1"), ResearchPage.infusion(id)));
    }
}
