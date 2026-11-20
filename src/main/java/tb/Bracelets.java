package tb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.InfusionRecipe;
import thaumcraft.crafting.ShapedArcaneRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchPage;
import thaumcraft.wand.WandRegistry;

final class Bracelets {
    private static final String MODEL_DATA = "thaumicbases:bracelet";

    enum Variant {
        IRON(10, "iron", "wood"), GOLD(15, "gold", "wood"), GREATWOOD(17, "gold", "greatwood"),
        THAUMIUM(20, "thauminite", "tbthaumium"), SILVERWOOD(23, "thaumium", "silverwood"),
        REED(23, "thaumium", "reed"), BONE(25, "thaumium", "bone"), OBSIDIAN(25, "thaumium", "obsidian"),
        BLAZE(25, "thaumium", "blaze"), ICE(25, "thaumium", "ice"), QUARTZ(25, "thaumium", "quartz"),
        VOID(27, "thaumium", "tbvoid"), PRIMAL(30, "void", "primal_staff"), ICHOR(150, "void", "primal_staff");

        final String id = name().toLowerCase(java.util.Locale.ROOT);
        final String key = "TB.Bracelet." + name().charAt(0) + id.substring(1);
        final int capacity;
        final String cap;
        final String rod;

        Variant(int capacity, String cap, String rod) {
            this.capacity = capacity;
            this.cap = cap;
            this.rod = rod;
        }

        boolean available() {
            return this != ICHOR || net.neoforged.fml.ModList.get().isLoaded("thaumictinkerer");
        }

        String rodKey() { return "tb_bracelet_" + id; }
        String recipe() { return Bases.id("bracelet_" + id).toString(); }

        TcStack stack() {
            return TcStack.withData("thaumcraft:wand", 1, Map.of(
                "cap", "tb_bracelet_" + cap, "rod", rodKey(), MODEL_DATA, id));
        }
    }

    static void register() {
        thaumcraft.crafting.ArcaneCrafting.registerDynamic(new BraceletRecap());
        TcStack.registerDataHandler(MODEL_DATA, (stack, variant) -> {
            stack.set(DataComponents.ITEM_MODEL, Bases.id("bracelet_" + variant));
            stack.set(DataComponents.CUSTOM_NAME, Component.translatable("item.thaumicbases.bracelet_" + variant));
        });
        for (String cap : List.of("iron", "gold", "thauminite", "thaumium", "void", "copper", "silver")) {
            var original = WandRegistry.cap(cap);
            WandRegistry.registerCap("tb_bracelet_" + cap, original.baseCostModifier() / 1.5F,
                original.specialAspects(), original.specialCostModifier() / 1.5F, Bases.id("internal_bracelet_cap_" + cap),
                original.craftCost(), null, original.texture());
        }
        for (var variant : Variant.values()) {
            if (!variant.available()) continue;
            var rod = WandRegistry.rod(variant.rod);
            WandRegistry.registerRod(variant.rodKey(), variant.capacity, Bases.id("internal_" + variant.rodKey()),
                rod.craftCost(), rod.onUpdate(), rod.glowing(), false, false, rod.texture());
        }

        arcane(Variant.IRON, null, 1, "thaumcraft:iron_wand_cap", "minecraft:iron_ingot", "minecraft:iron_nugget", "CIC", "N N");
        arcane(Variant.GOLD, Variant.IRON.key, 5, "thaumcraft:gold_wand_cap", "minecraft:gold_ingot", "minecraft:gold_nugget", "CIC", "N N");
        arcane(Variant.GREATWOOD, Variant.GOLD.key, 25, "thaumcraft:gold_wand_cap", "thaumcraft:greatwood_log", "", "CIC", "I I");
        arcane(Variant.THAUMIUM, Variant.GREATWOOD.key, 25, "thaumicbases:thauminite_wand_cap", "thaumcraft:thaumium_ingot",
            "thaumcraft:salis_mundus", "CIC", "INI", "N N");

        infusion(Variant.SILVERWOOD, Variant.GREATWOOD.key, 4, primals(17).add(Aspect.MAGIC, 17),
            TcIngredient.item("thaumcraft:silverwood_rod"), ingredients("thaumium_wand_cap", "balanced_shard",
                "silverwood_log", "mixed_crystal_cluster", "thaumium_wand_cap", "balanced_shard", "silverwood_log", "mixed_crystal_cluster"));
        elemental(Variant.REED, Aspect.AIR, Aspect.MOTION, "reed_rod", "air_shard");
        elemental(Variant.BONE, Aspect.ENTROPY, Aspect.UNDEAD, "bone_rod", "entropy_shard");
        elemental(Variant.OBSIDIAN, Aspect.EARTH, Aspect.DARKNESS, "obsidian_rod", "earth_shard");
        elemental(Variant.BLAZE, Aspect.FIRE, Aspect.BEAST, "blazing_rod", "fire_shard");
        elemental(Variant.ICE, Aspect.WATER, Aspect.COLD, "icy_rod", "water_shard");
        elemental(Variant.QUARTZ, Aspect.ORDER, Aspect.ENERGY, "quartz_rod", "order_shard");
        var voidCap = TcIngredient.item("thaumcraft:thaumium_wand_cap");
        var salt = TcIngredient.item("thaumicbases:salis_mundus_block");
        var metal = TcIngredient.item("thaumcraft:void_metal_ingot");
        var crystal = TcIngredient.item("thaumicbases:mixed_crystal_block");
        infusion(Variant.VOID, Variant.SILVERWOOD.key, 8,
            new AspectList().add(Aspect.ELDRITCH, 64).add(Aspect.VOID, 64).add(Aspect.MAGIC, 64)
                .add(Aspect.AURA, 24).add(Aspect.ENERGY, 24),
            TcIngredient.item("thaumicbases:void_wand_core"), List.of(voidCap, salt, metal, crystal, voidCap, salt, metal, crystal));
        var primalCap = TcIngredient.item("thaumcraft:void_wand_cap");
        infusion(Variant.PRIMAL, Variant.SILVERWOOD.key, 7, primals(48).add(Aspect.MAGIC, 96),
            TcIngredient.item("thaumcraft:primal_charm"),
            List.of(primalCap, TcIngredient.of(Variant.REED.stack()), TcIngredient.of(Variant.BONE.stack()),
                TcIngredient.of(Variant.OBSIDIAN.stack()), primalCap, TcIngredient.of(Variant.BLAZE.stack()),
                TcIngredient.of(Variant.ICE.stack()), TcIngredient.of(Variant.QUARTZ.stack())));
        ResearchCategories.getResearch(Variant.PRIMAL.key).setParents(Variant.SILVERWOOD.key, Variant.REED.key,
            Variant.BONE.key, Variant.OBSIDIAN.key, Variant.BLAZE.key, Variant.ICE.key, Variant.QUARTZ.key, "ROD_primal_staff");

        if (Variant.ICHOR.available()) {
            var primalCharm = TcIngredient.item("thaumcraft:primal_charm");
            var enderShard = TcIngredient.item("thaumictinkerer:ender_shard");
            var netherShard = TcIngredient.item("thaumictinkerer:nether_shard");
            var ichor = TcIngredient.item("thaumictinkerer:ichor");
            var cost = new AspectList().add(Aspect.MAGIC, 96)
                .add(Aspect.FIRE, 64).add(Aspect.WATER, 64).add(Aspect.EARTH, 64).add(Aspect.AIR, 64)
                .add(Aspect.ORDER, 64).add(Aspect.ENTROPY, 64).add(Aspect.VOID, 64).add(Aspect.ELDRITCH, 32);
            infusion(Variant.ICHOR, Variant.PRIMAL.key, 8, cost,
                TcIngredient.item("thaumictinkerer:ichorcloth_rod"),
                List.of(primalCharm, enderShard, ichor, netherShard, primalCharm, enderShard, ichor, netherShard));
            ResearchCategories.getResearch(Variant.ICHOR.key).setParents(Variant.PRIMAL.key, "ROD_ICHORCLOTH").setWarp(1);
        }
    }

    private static void elemental(Variant variant, Aspect primary, Aspect secondary, String rod, String shard) {
        infusion(variant, "ROD_" + variant.rod, 2, new AspectList().add(primary, 24).add(secondary, 12).add(Aspect.MAGIC, 8),
            TcIngredient.item("thaumcraft:" + rod), ingredients("thaumium_wand_cap", shard, "thaumium_wand_cap", shard));
    }

    private static List<TcIngredient> ingredients(String... names) {
        return Arrays.stream(names).map(name -> TcIngredient.item("thaumcraft:" + name)).toList();
    }

    private static void arcane(Variant variant, String parent, int vis, String cap, String body, String binding, String... rows) {
        var grid = new ArrayList<TcIngredient>();
        for (String row : rows) {
            for (char slot : row.toCharArray()) {
                grid.add(switch (slot) {
                    case 'C' -> TcIngredient.item(cap);
                    case 'I' -> TcIngredient.item(body);
                    case 'N' -> TcIngredient.item(binding);
                    default -> TcIngredient.EMPTY;
                });
            }
        }
        var cost = primals(vis);
        TcRecipes.register(new ShapedArcaneRecipe(variant.recipe(), variant.key, variant.stack(), cost, 3, rows.length, grid));
        entry(variant, new AspectList().add(Aspect.CRAFT, 4).add(Aspect.MAGIC, 4),
            ResearchPage.arcane(variant.recipe()), parent == null ? new String[0] : new String[]{parent});
    }

    private static void infusion(Variant variant, String parent, int instability, AspectList cost,
                                 TcIngredient center, List<TcIngredient> components) {
        TcRecipes.register(new InfusionRecipe(variant.recipe(), variant.key, variant.stack(), instability, cost, center, components));
        entry(variant, new AspectList().add(Aspect.CRAFT, 4).add(Aspect.MAGIC, 4).add(Aspect.AURA, 4),
            ResearchPage.infusion(variant.recipe()), parent);
    }

    private static void entry(Variant variant, AspectList tags, ResearchPage recipe, String... parents) {
        String componentResearch = switch (variant) {
            case GOLD -> "CAP_gold";
            case GREATWOOD -> "ROD_greatwood";
            case SILVERWOOD -> "ROD_silverwood";
            default -> null;
        };
        var dependencies = new ArrayList<>(List.of(parents));
        if (componentResearch != null) dependencies.add(componentResearch);
        String[] hidden = recipe instanceof ResearchPage.Infusion ? new String[]{"INFUSION"} : new String[0];
        int i = variant.ordinal();
        ResearchCategories.addResearch(new BasesResearch(variant.key, Bases.ID, tags,
            7 + (i % 4) * 2, 6 + (i / 4) * 2, 2, variant.stack()).setParents(dependencies.toArray(String[]::new)).setParentsHidden(hidden)
            .setPages(ResearchPage.text("tc.research_page." + variant.key + ".1"), recipe,
                ResearchPage.text("tc.research_page.TB.Bracelet.Recap")));
    }

    private static AspectList primals(int amount) {
        var cost = new AspectList();
        for (Aspect primal : Aspect.getPrimalAspects()) cost.add(primal, amount);
        return cost;
    }
}
