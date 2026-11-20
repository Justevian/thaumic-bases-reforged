package tb;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShearsItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.CraftingRecipeRef;
import thaumcraft.crafting.InfusionRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.item.gear.Repairable;
import thaumcraft.item.gear.SelfRepair;
import thaumcraft.item.gear.WarpingGear;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchPage;

final class VoidTools {
    private static final String RESEARCH = "TB.VoidTools";
    private static final String SEED_RESEARCH = "TB.VoidSeed";

    static void register(DeferredRegister.Items items) {
        items.registerItem("void_shears", Shears::new,
            p -> p.durability(238).rarity(Rarity.UNCOMMON).component(DataComponents.TOOL, ShearsItem.createToolProperties()));
        items.registerItem("void_flint_and_steel", Flint::new, p -> p.durability(64).rarity(Rarity.UNCOMMON));
    }

    private static final class Shears extends ShearsItem implements Repairable, WarpingGear {
        Shears(Properties p) { super(p); }

        @Override
        public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, EquipmentSlot slot) {
            SelfRepair.tick(stack, owner);
        }

        @Override
        public int getWarp(ItemStack stack, Player player) { return 1; }
    }

    private static final class Flint extends FlintAndSteelItem implements Repairable, WarpingGear {
        Flint(Properties p) { super(p); }

        @Override
        public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, EquipmentSlot slot) {
            SelfRepair.tick(stack, owner);
        }

        @Override
        public int getWarp(ItemStack stack, Player player) { return 1; }
    }

    static void research() {
        String seed = Bases.id("void_plant_seeds").toString();
        var cost = new AspectList().add(Aspect.ELDRITCH, 16).add(Aspect.DARKNESS, 16).add(Aspect.CROP, 16);
        TcRecipes.register(new InfusionRecipe(seed, SEED_RESEARCH, TcStack.of(seed), 3, cost,
            TcIngredient.item("thaumcraft:void_seed"), List.of("wheat_seeds", "pumpkin_seeds", "melon_seeds",
                "carrot", "potato", "nether_wart").stream().map(name -> TcIngredient.item("minecraft:" + name)).toList()));
        ResearchCategories.addResearch(new BasesResearch(SEED_RESEARCH, Bases.ID, cost, 2, 4, 2, TcStack.of(seed))
            .setParents("VOIDMETAL")
            .setPages(ResearchPage.text("tc.research_page." + SEED_RESEARCH + ".1"), ResearchPage.infusion(seed)));

        for (String name : List.of("void_shears", "void_flint_and_steel", "void_metal_block", "void_metal_from_block")) {
            String id = Bases.id(name).toString();
            TcStack output = name.equals("void_metal_from_block") ? TcStack.of("thaumcraft:void_metal_ingot", 9) : TcStack.of(id);
            TcRecipes.register(new CraftingRecipeRef(id, RESEARCH, Bases.id(name), output));
        }
        ResearchCategories.addResearch(new BasesResearch(RESEARCH, Bases.ID,
            new AspectList().add(Aspect.METAL, 4).add(Aspect.TOOL, 4).add(Aspect.VOID, 4), 5, 4, 2,
            TcStack.of(Bases.id("void_shears").toString())).setParents("VOIDMETAL")
            .setPages(ResearchPage.text("tc.research_page." + RESEARCH + ".1"),
                ResearchPage.crafting(Bases.id("void_shears").toString(), Bases.id("void_flint_and_steel").toString()),
                ResearchPage.crafting(Bases.id("void_metal_block").toString(), Bases.id("void_metal_from_block").toString())));
    }
}
