package tb;

import java.util.Arrays;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.TcStack;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchItem;
import thaumcraft.research.ResearchManager;
import thaumcraft.research.ResearchPage;

@EventBusSubscriber(modid = Bases.ID)
final class BasesResearch extends ResearchItem {
    private record Position(int column, int row) {}

    private record Reference(String original, int column, int row) {
        String key() {
            return original.startsWith("ROD_") || original.equals("CAP_gold")
                ? "TB." + original : "TB.Reference." + original;
        }
    }

    private static final List<Reference> REFERENCES = List.of(
        new Reference("CRUCIBLE", 0, 0),
        new Reference("THAUMIUM", 7, 0),
        new Reference("VOIDMETAL", 11, 0),
        new Reference("INFUSION", 3, -18),
        new Reference("INFUSIONENCHANTMENT", -3, 1),
        new Reference("ENCHFABRIC", 8, -4),
        new Reference("NODESTABILIZERADV", -1, -23),
        new Reference("PRIMPEARL", 3, -29),
        new Reference("VISPOWER", 4, -21),
        new Reference("LEVITATOR", 0, 6),
        new Reference("DISTILESSENTIA", 6, 2),
        new Reference("BELLOWS", 6, 4),
        new Reference("CAP_thaumium", 10, 0),
        new Reference("JARVOID", 0, -19),
        new Reference("FOCUSEXCAVATION", -3, -20),
        new Reference("CAP_gold", -8, -3),
        new Reference("ROD_greatwood", -11, -3),
        new Reference("ROD_silverwood", -12, -5),
        new Reference("ROD_reed", -17, -7),
        new Reference("ROD_blaze", -15, -6),
        new Reference("ROD_obsidian", -16, -4),
        new Reference("ROD_ice", -15, -2),
        new Reference("ROD_quartz", -18, 0),
        new Reference("ROD_bone", -16, 1),
        new Reference("ROD_primal_staff", -19, -6)
    );

    private final Position position;

    BasesResearch(String key, String category, AspectList tags, int column, int row, int complexity, TcStack icon) {
        super(key, category, tags, column, row, complexity, icon);
        position = position(key, column, row);
    }

    BasesResearch(String key, String category, AspectList tags, int column, int row, int complexity, Identifier icon) {
        super(key, category, tags, column, row, complexity, icon);
        position = position(key, column, row);
    }

    @Override
    public int column() { return position.column(); }

    @Override
    public int row() { return compactRow(position.row()); }

    @Override
    public ResearchItem setParents(String... parents) {
        return super.setParents(Arrays.stream(parents).map(parent -> {
            for (Reference reference : REFERENCES) {
                if (reference.original().equals(parent) && ResearchCategories.getResearch(reference.key()) != null) {
                    return reference.key();
                }
            }
            return parent;
        }).toArray(String[]::new));
    }

    static void registerReferences() {
        for (Reference ref : REFERENCES) {
            ResearchItem original = ResearchCategories.getResearch(ref.original());
            if (original == null || (original.iconItem() == null && original.iconTexture() == null)) continue;
            ResearchItem reference = original.iconItem() == null
                ? new ResearchItem(ref.key(), Bases.ID, new AspectList(), ref.column(), compactRow(ref.row()), 1, original.iconTexture()) {
                    @Override public String nameKey() { return original.nameKey(); }
                    @Override public String textKey() { return original.textKey(); }
                }
                : new ResearchItem(ref.key(), Bases.ID, new AspectList(), ref.column(), compactRow(ref.row()), 1, original.iconItem()) {
                    @Override public String nameKey() { return original.nameKey(); }
                    @Override public String textKey() { return original.textKey(); }
                };
            reference.setStub().setHidden().setParentsHidden(ref.original())
                .setPages(original.pages().toArray(ResearchPage[]::new)).registerResearchItem();
        }
    }

    @SubscribeEvent
    static void syncReferences(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) return;
        for (Reference ref : REFERENCES) {
            if (ResearchCategories.getResearch(ref.key()) != null && ResearchManager.isComplete(player, ref.original())
                && !ResearchManager.isComplete(player, ref.key())) {
                ResearchManager.complete(player, ref.key(), false);
            }
        }
    }

    // Remove the empty rows left by content outside the current port without reordering the occupied rows.
    private static int compactRow(int row) {
        int result = row;

        if (row > 5) result--;
        if (row > 7) result--;
        return result;
    }

    private static Position position(String key, int column, int row) {
        return switch (key) {
            case "TB.Quicksilver" -> new Position(-1, 1);
            case "TB.Thauminite" -> new Position(9, 0);
            case "CAP_thauminite" -> new Position(11, 1);
            case "TB.BasicPlants" -> new Position(0, -8);
            case "TB.Redlon" -> new Position(-3, -12);
            case "TB.SM" -> new Position(0, 1);
            case "TB.SMB" -> new Position(0, 3);
            case "TB.Plax" -> new Position(0, -10);
            case "TB.Metalleat" -> new Position(-3, -9);
            case "TB.Lucrite" -> new Position(-2, -11);
            case "TB.Lazullia" -> new Position(-5, -10);
            case "TB.Glieonia" -> new Position(5, -10);
            case "TB.Ashroom" -> new Position(4, -11);
            case "TB.Flaxium" -> new Position(3, -12);
            case "TB.Tobacco" -> new Position(1, -13);
            case "TB.Tobacco.Angry" -> new Position(3, -16);
            case "TB.Tobacco.Mining" -> new Position(4, -15);
            case "TB.Tobacco.Wisdom" -> new Position(4, -14);
            case "TB.Tobacco.Tainted" -> new Position(4, -13);
            case "TB.Tobacco.Sanity" -> new Position(3, -14);
            case "TB.Tobacco.Saturating" -> new Position(2, -15);
            case "TB.Tobacco.Eldritch" -> new Position(1, -16);
            case "TB.Tobacco.Wispy" -> new Position(2, -16);
            case "TB.SilverwoodPipe" -> new Position(5, -17);
            case "TB.AlchemyRestoration" -> new Position(1, -2);
            case "TB.CrystalBlocks" -> new Position(2, -4);
            case "TB.LegacySlabs" -> new Position(4, -3);
            case "TB.VoidSeed" -> new Position(13, -3);
            case "TB.VoidTools" -> new Position(13, 0);
            case "ROD_tbthaumium" -> new Position(8, 1);
            case "ROD_tbvoid" -> new Position(11, 2);
            case "TB.GoldenOak" -> new Position(-4, -16);
            case "TB.PeacefulSapling" -> new Position(-7, -16);
            case "TB.NetherSapling" -> new Position(-6, -15);
            case "TB.EnderSapling" -> new Position(-6, -17);
            case "TB.Bracelet.Iron" -> new Position(-5, 0);
            case "TB.Bracelet.Gold" -> new Position(-8, -1);
            case "TB.Bracelet.Greatwood" -> new Position(-11, -1);
            case "TB.Bracelet.Thaumium" -> new Position(-11, 2);
            case "TB.Bracelet.Silverwood" -> new Position(-13, -3);
            case "TB.Bracelet.Reed" -> new Position(-17, -6);
            case "TB.Bracelet.Bone" -> new Position(-16, 0);
            case "TB.Bracelet.Obsidian" -> new Position(-17, -4);
            case "TB.Bracelet.Blaze" -> new Position(-15, -5);
            case "TB.Bracelet.Ice" -> new Position(-16, -2);
            case "TB.Bracelet.Quartz" -> new Position(-18, -1);
            case "TB.Bracelet.Void" -> new Position(-13, 0);
            case "TB.Bracelet.Primal" -> new Position(-19, -4);
            case "TB.Briar" -> new Position(-1, -13);
            case "TB.Aurelia" -> new Position(3, -9);
            case "TB.Spike.Iron" -> new Position(7, -6);
            case "TB.Spike.Thaumic" -> new Position(9, -6);
            case "TB.Spike.Void" -> new Position(11, -6);
            case "TB.BloodyRobes" -> new Position(8, -8);
            case "TB.Backprocessing" -> new Position(0, -3);
            case "TB.CryingObs" -> new Position(6, -18);
            case "TB.Knose" -> new Position(2, -11);
            case "TB.SimpleDublication" -> new Position(2, 0);
            case "TB.Amber" -> new Position(1, 1);
            case "TB.Pyrofluid" -> new Position(3, -2);
            case "TB.RainbowCacti" -> new Position(-4, -11);
            case "TB.Foci.Activation" -> new Position(-2, -19);
            case "TB.Foci.Drain" -> new Position(-1, -20);
            case "TB.Foci.Experience" -> new Position(-2, -20);
            case "TB.Foci.Flux" -> new Position(-1, -19);
            case "TB.Revolver" -> new Position(-6, -21);
            case "TB.HerobrinesScythe" -> new Position(15, 1);
            case "TB.AdvancedEntropy" -> new Position(-1, -2);
            case "TB.MasterEntropy" -> new Position(-2, -3);
            case "TB.StrongEntropy" -> new Position(-1, -4);
            case "TB.OreDestruction" -> new Position(-4, -4);
            case "TB.Campfire" -> new Position(3, 6);
            case "TB.Brazier" -> new Position(3, 8);
            case "TB.ThaumicAnvil" -> new Position(9, 4);
            case "TB.VoidAnvil" -> new Position(12, 4);
            case "TB.AdvAlc" -> new Position(7, 3);
            case "TB.Overchanter" -> new Position(6, -20);
            case "TB.NodeLinker" -> new Position(4, -23);
            case "TB.EntityDec" -> new Position(7, -19);
            case "TB.EntityDecAdv" -> new Position(9, -19);
            case "TB.Relocator" -> new Position(0, 8);
            case "TB.EldritchBane" -> new Position(-4, 4);
            case "TB.Vaporising" -> new Position(-3, 3);
            case "TB.ElderKnowledge" -> new Position(-5, 3);
            case "TB.MagicTouch" -> new Position(-4, 2);
            case "TB.Tainted" -> new Position(-5, 4);
            case "TB.Bracelet.Ichor" -> new Position(-21, -4);
            default -> new Position(column, row);
        };
    }
}
