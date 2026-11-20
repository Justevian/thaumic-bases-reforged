package tb;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.CrucibleRecipe;
import thaumcraft.crafting.ShapelessArcaneRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.registry.ModItems;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchPage;

final class Rosehip {
    static void register(DeferredRegister.Blocks blocks, DeferredRegister.Items items) {
        var briar = blocks.registerBlock("briar", Briar::new,
            p -> p.noCollision().randomTicks().instabreak().sound(SoundType.GRASS));
        items.registerSimpleBlockItem(briar);
        items.registerItem("briar_seedbag", p -> new BlockItem(briar.get(), p));
        items.registerItem("rosehip_syrup", Syrup::new, p -> p.stacksTo(16));
    }

    private static final class Briar extends CropBlock {
        Briar(Properties properties) { super(properties); }

        @Override
        protected boolean mayPlaceOn(BlockState soil, BlockGetter level, BlockPos pos) {
            return soil.is(BlockTags.DIRT) || soil.is(Blocks.GRASS_BLOCK) || soil.is(Blocks.FARMLAND);
        }

        @Override
        protected ItemLike getBaseSeedId() { return net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Bases.id("briar_seedbag")); }
    }

    private static final class Syrup extends Item {
        Syrup(Properties properties) { super(properties); }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            player.startUsingItem(hand);
            return InteractionResult.CONSUME;
        }

        @Override
        public ItemUseAnimation getUseAnimation(ItemStack stack) { return ItemUseAnimation.DRINK; }

        @Override
        public int getUseDuration(ItemStack stack, LivingEntity user) { return 32; }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
            if (level.isClientSide()) return stack;
            for (var effect : List.copyOf(user.getActiveEffects())) {
                if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                    user.removeEffect(effect.getEffect());
                }
            }
            stack.consume(1, user);
            if (user instanceof Player player && player.hasInfiniteMaterials()) return stack;
            var phial = new ItemStack(ModItems.GLASS_PHIAL.get());
            if (stack.isEmpty()) return phial;
            if (user instanceof Player player && !player.getInventory().add(phial)) {
                player.drop(phial, false, net.minecraft.util.Prediction.PREDICTED);
            }
            return stack;
        }
    }

    static void research() {
        String key = "TB.Briar";
        String briar = Bases.id("briar").toString();
        String syrup = Bases.id("rosehip_syrup").toString();
        var cost = new AspectList().add(Aspect.HEAL, 4).add(Aspect.LIFE, 4).add(Aspect.PLANT, 4);
        TcRecipes.register(new CrucibleRecipe(briar, key, TcStack.of(briar), TcIngredient.item("minecraft:rose_bush"), cost));
        TcRecipes.register(new ShapelessArcaneRecipe(syrup, key, TcStack.of(syrup),
            new AspectList().add(Aspect.FIRE, 3).add(Aspect.WATER, 5),
            List.of(TcIngredient.item("thaumcraft:glass_phial"), TcIngredient.item(Bases.id("briar_seedbag").toString()),
                TcIngredient.item("minecraft:sugar"))));
        ResearchCategories.addResearch(new BasesResearch(key, Bases.ID, cost.copy(), -5, 5, 2, TcStack.of(briar))
            .setParents("TB.Plax").setPages(ResearchPage.text("tc.research_page.TB.Briar.1"),
                ResearchPage.crucible(briar), ResearchPage.arcane(syrup)));
    }
}
