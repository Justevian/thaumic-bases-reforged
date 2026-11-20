package tb;

import java.util.List;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.InfusionEnchantmentRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchPage;

final class BasesEnchantments {
    private static final String STUB = "TB.Enchantments";

    static void stub() {
        var pages = new ResearchPage[6];
        for (int i = 0; i < pages.length; i++) pages[i] = ResearchPage.text("tc.research_page." + STUB + "." + (i + 1));
        ResearchCategories.addResearch(new BasesResearch(STUB, Bases.ID, new AspectList(), -3, 2, 0,
            Bases.id("textures/thaumonomicon/enchantments_icon.png")).setAutoUnlock().setRound().setStub().setPages(pages));
    }

    static void research() {
        var id = Bases.id("eldritch_bane");
        String key = "TB.EldritchBane";
        var aspects = new AspectList().add(Aspect.ELDRITCH, 8).add(Aspect.DEATH, 8).add(Aspect.WEAPON, 8);
        TcRecipes.register(new InfusionEnchantmentRecipe(id.toString(), key, id, 3, aspects.copy(),
            List.of(TcIngredient.item("minecraft:ender_pearl"), TcIngredient.item("minecraft:iron_sword"),
                TcIngredient.item("thaumcraft:salis_mundus"))));
        ResearchCategories.addResearch(new BasesResearch(key, Bases.ID, aspects, -4, 4, 1,
            Bases.id("textures/thaumonomicon/eldritch_bane.png")).setParents("INFUSIONENCHANTMENT", STUB)
            .setPages(ResearchPage.text("tc.research_page." + key + ".1"), ResearchPage.infusionEnchantment(id.toString())));
    }

    static void vaporising() {
        var id = Bases.id("vaporising");
        String key = "TB.Vaporising";
        var aspects = new AspectList().add(Aspect.MAGIC, 8).add(Aspect.CRYSTAL, 8).add(Aspect.TRAP, 8);
        TcRecipes.register(new InfusionEnchantmentRecipe(id.toString(), key, id, 3, aspects.copy(),
            List.of(TcIngredient.item("thaumcraft:crystallized_essence"), TcIngredient.item("minecraft:iron_sword"),
                TcIngredient.item("thaumcraft:salis_mundus"))));
        ResearchCategories.addResearch(new BasesResearch(key, Bases.ID, aspects, -3, 3, 1,
            Bases.id("textures/thaumonomicon/vaporising.png")).setParents("INFUSIONENCHANTMENT", STUB)
            .setPages(ResearchPage.text("tc.research_page." + key + ".1"), ResearchPage.infusionEnchantment(id.toString())));
    }

    static void elderWisdom() {
        var id = Bases.id("elder_wisdom");
        String key = "TB.ElderKnowledge";
        var aspects = new AspectList().add(Aspect.MIND, 8).add(Aspect.DEATH, 8).add(Aspect.ELDRITCH, 8);
        TcRecipes.register(new InfusionEnchantmentRecipe(id.toString(), key, id, 3, aspects.copy(),
            List.of(TcIngredient.item("minecraft:book"), TcIngredient.item("minecraft:iron_sword"),
                TcIngredient.item("thaumcraft:salis_mundus"))));
        ResearchCategories.addResearch(new BasesResearch(key, Bases.ID, aspects, -5, 3, 1,
            Bases.id("textures/thaumonomicon/elder_knowledge.png")).setParents("INFUSIONENCHANTMENT", STUB)
            .setPages(ResearchPage.text("tc.research_page." + key + ".1"), ResearchPage.infusionEnchantment(id.toString())));
    }

    static void remainingWeaponEnchantments() {
        var magic = Bases.id("magic_touch");
        String key = "TB.MagicTouch";
        var aspects = new AspectList().add(Aspect.MAGIC, 8).add(Aspect.AURA, 8).add(Aspect.WEAPON, 8);
        var salt = TcIngredient.item("thaumcraft:salis_mundus");
        TcRecipes.register(new InfusionEnchantmentRecipe(magic.toString(), key, magic, 3, aspects.copy(),
            List.of(salt, salt, salt, TcIngredient.item("minecraft:ender_pearl"))));
        ResearchCategories.addResearch(new BasesResearch(key, Bases.ID, aspects, -11, 3, 1,
            Bases.id("textures/thaumonomicon/magic_touched.png")).setParents("INFUSIONENCHANTMENT", STUB)
            .setPages(ResearchPage.text("tc.research_page." + key + ".1"), ResearchPage.infusionEnchantment(magic.toString())));

        String tainted = "TB.Tainted";
        ResearchCategories.addResearch(new BasesResearch(tainted, Bases.ID,
            new AspectList().add(Aspect.TAINT, 8).add(Aspect.WEAPON, 8), -12, 3, 1,
            Bases.id("textures/thaumonomicon/tainted.png")).setParents("INFUSIONENCHANTMENT", STUB)
            .setPages(ResearchPage.text("tc.research_page." + tainted + ".1")));
    }

    private BasesEnchantments() {}
}
