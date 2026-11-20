package tb;

import java.util.List;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.InfusionRecipe;
import thaumcraft.crafting.ShapedArcaneRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchPage;
import thaumcraft.wand.RodUpdates;
import thaumcraft.wand.WandRegistry;

final class MetalRods {
    static void register(DeferredRegister.Items items) {
        items.registerSimpleItem("thaumium_wand_core");
        items.registerSimpleItem("void_wand_core");
    }

    static void research() {
        var thaumium = Bases.id("thaumium_wand_core");
        var voidCore = Bases.id("void_wand_core");
        WandRegistry.registerRod("tbthaumium", 80, thaumium, 6, RodUpdates.primal(), false, false, false,
            Bases.id("textures/model/thaumium_wand_core.png"));
        WandRegistry.registerRod("tbvoid", 160, voidCore, 16, RodUpdates.primal(), false, false, false,
            Bases.id("textures/model/void_wand_core.png"));

        var salt = TcIngredient.item("thaumcraft:salis_mundus");
        var ingot = TcIngredient.item("thaumcraft:thaumium_ingot");
        var blank = TcIngredient.EMPTY;
        var vis = new AspectList();
        for (Aspect aspect : Aspect.getPrimalAspects()) vis.add(aspect, 50);
        String key = "ROD_tbthaumium";
        TcRecipes.register(new ShapedArcaneRecipe(thaumium.toString(), key, TcStack.of(thaumium.toString()), vis, 3, 3,
            List.of(blank, salt, ingot, salt, ingot, salt, ingot, salt, blank)));
        ResearchCategories.addResearch(new BasesResearch(key, Bases.ID,
            new AspectList().add(Aspect.METAL, 6).add(Aspect.MAGIC, 6).add(Aspect.CRAFT, 2).add(Aspect.MAN, 6),
            6, 2, 2, TcStack.of(thaumium.toString())).setParents("THAUMIUM")
            .setPages(ResearchPage.text("tc.research_page." + key + ".1"), ResearchPage.arcane(thaumium.toString())));

        key = "ROD_tbvoid";
        var voidMetal = TcIngredient.item("thaumcraft:void_metal_ingot");
        var blockSalt = TcIngredient.item(Bases.id("salis_mundus_block").toString());
        var tainted = TcIngredient.item(Bases.id("tainted_crystal_block").toString());
        TcRecipes.register(new InfusionRecipe(voidCore.toString(), key, TcStack.of(voidCore.toString()), 6,
            new AspectList().add(Aspect.ELDRITCH, 64).add(Aspect.VOID, 64).add(Aspect.MAGIC, 64)
                .add(Aspect.AURA, 24).add(Aspect.ENERGY, 24), TcIngredient.item(thaumium.toString()),
            List.of(voidMetal, voidMetal, voidMetal, voidMetal, blockSalt, blockSalt, tainted, tainted)));
        ResearchCategories.addResearch(new BasesResearch(key, Bases.ID,
            new AspectList().add(Aspect.ELDRITCH, 4).add(Aspect.MAGIC, 3).add(Aspect.AURA, 4)
                .add(Aspect.VOID, 2).add(Aspect.MIND, 6), 7, 4, 3, TcStack.of(voidCore.toString()))
            .setParents("ROD_tbthaumium", "VOIDMETAL")
            .setPages(ResearchPage.text("tc.research_page." + key + ".1"), ResearchPage.infusion(voidCore.toString())));
    }
}
