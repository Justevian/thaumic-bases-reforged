package tb;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ShearsItem;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.*;
import thaumcraft.item.gear.Repairable;
import thaumcraft.platform.ArmorSlot;
import thaumcraft.platform.Registration;
import thaumcraft.platform.TcArmorMaterial;
import thaumcraft.platform.TcToolMaterial;
import thaumcraft.research.*;
import thaumcraft.wand.WandRegistry;

final class ThauminiteGear {
    private static final TagKey<Item> REPAIR = TagKey.create(Registries.ITEM, Bases.id("repairs_thauminite"));
    private static final TcToolMaterial TOOL = new TcToolMaterial(974, 7, 2.8F, 15, REPAIR, BlockTags.INCORRECT_FOR_DIAMOND_TOOL);
    private static final TcArmorMaterial ARMOR = new TcArmorMaterial(27, 3, 8, 6, 3, 17,
        SoundEvents.ARMOR_EQUIP_GENERIC, 0, 0, REPAIR, Bases.id("thauminite"));

    private static final class Gear extends Item implements Repairable {
        Gear(Properties properties) { super(properties); }
    }

    private static final class Shears extends ShearsItem implements Repairable {
        Shears(Properties properties) { super(properties); }
    }

    static void items(net.neoforged.neoforge.registries.DeferredRegister.Items items) {
        items.registerItem("thauminite_axe", Gear::new, p -> Registration.axe(p, TOOL, 5, -3));
        items.registerItem("thauminite_hoe", Gear::new, p -> Registration.hoe(p, TOOL, -2.8F, 0));
        items.registerItem("thauminite_shovel", Gear::new, p -> Registration.shovel(p, TOOL, 1.5F, -3));
        items.registerItem("thauminite_sword", Gear::new, p -> Registration.sword(p, TOOL, 3, -2.4F));
        items.registerItem("thauminite_pickaxe", Gear::new, p -> Registration.pickaxe(p, TOOL, 1, -2.8F));
        items.registerItem("thauminite_shears", Shears::new, p -> p.durability(974).component(net.minecraft.core.component.DataComponents.TOOL, ShearsItem.createToolProperties()));
        items.registerItem("thauminite_helmet", Gear::new, p -> Registration.armor(p, ARMOR, ArmorSlot.HELMET));
        items.registerItem("thauminite_chestplate", Gear::new, p -> Registration.armor(p, ARMOR, ArmorSlot.CHESTPLATE));
        items.registerItem("thauminite_leggings", Gear::new, p -> Registration.armor(p, ARMOR, ArmorSlot.LEGGINGS));
        items.registerItem("thauminite_boots", Gear::new, p -> Registration.armor(p, ARMOR, ArmorSlot.BOOTS));
        items.registerSimpleItem("thauminite_wand_cap");
    }

    static List<ResearchPage> recipes() {
        var pages = new ArrayList<ResearchPage>();
        String key = "TB.Thauminite";
        pages.add(recipe(key, "thauminite_axe", 15, "##", "#|", " |"));
        pages.add(recipe(key, "thauminite_hoe", 15, "##", " |", " |"));
        pages.add(recipe(key, "thauminite_shovel", 15, "#", "|", "|"));
        pages.add(recipe(key, "thauminite_sword", 15, "#", "#", "|"));
        pages.add(recipe(key, "thauminite_shears", 15, " #", "# "));
        pages.add(recipe(key, "thauminite_pickaxe", 15, "###", " | ", " | "));
        pages.add(recipe(key, "thauminite_helmet", 25, "###", "# #"));
        pages.add(recipe(key, "thauminite_chestplate", 25, "# #", "###", "###"));
        pages.add(recipe(key, "thauminite_leggings", 25, "###", "# #", "# #"));
        pages.add(recipe(key, "thauminite_boots", 25, "# #", "# #"));
        return pages;
    }

    static void registerCap() {
        String key = "CAP_thauminite";
        String item = "thauminite_wand_cap";
        WandRegistry.registerCap("thauminite", 0.85F, List.of(), 0, Bases.id(item), 6, null,
            Bases.id("textures/model/thauminite_wand_cap.png"));
        ResearchCategories.addResearch(new BasesResearch(key, Bases.ID,
            new AspectList().add(Aspect.METAL, 6).add(Aspect.MAGIC, 4), 4, 2, 2, TcStack.of(Bases.id(item).toString()))
            .setParents("TB.Thauminite").setPages(ResearchPage.text("tc.research_page." + key + ".1"),
                recipe(key, item, 25, "###", "#@#", "@@@")));
    }

    private static ResearchPage recipe(String research, String name, int vis, String... rows) {
        String id = Bases.id(name).toString();
        var grid = new ArrayList<TcIngredient>();
        for (String row : rows) {
            for (char slot : row.toCharArray()) {
                grid.add(switch (slot) {
                    case '#' -> TcIngredient.item(Bases.id("thauminite_ingot").toString());
                    case '|' -> TcIngredient.item("minecraft:stick");
                    case '@' -> TcIngredient.item("thaumcraft:vis_filter");
                    default -> TcIngredient.EMPTY;
                });
            }
        }
        var aspects = new AspectList();
        for (Aspect primal : Aspect.getPrimalAspects()) aspects.add(primal, vis);
        TcRecipes.register(new ShapedArcaneRecipe(id, research, TcStack.of(id), aspects, rows[0].length(), rows.length, grid));
        return ResearchPage.arcane(id);
    }
}
