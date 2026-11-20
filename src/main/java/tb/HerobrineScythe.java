package tb;

import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jspecify.annotations.Nullable;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.InfusionRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.item.gear.Repairable;
import thaumcraft.item.gear.SelfRepair;
import thaumcraft.item.gear.WarpingGear;
import thaumcraft.network.ShockZapPayload;
import thaumcraft.platform.ModNetwork;
import thaumcraft.registry.ModSounds;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchPage;

final class HerobrineScythe extends Item implements Repairable, WarpingGear {
    static final String ID = "herobrines_scythe";
    private static final String RESEARCH = "TB.HerobrinesScythe";
    private static final float ATTACK = 14.5F;
    private static final ItemAttributeModifiers MODIFIERS = ItemAttributeModifiers.builder()
        .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, ATTACK, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
        .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -2.4, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
        .add(Attributes.MOVEMENT_SPEED, new AttributeModifier(Bases.id("scythe_speed"), 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), EquipmentSlotGroup.MAINHAND)
        .build();

    HerobrineScythe(Properties properties) { super(properties); }

    static void register(DeferredRegister.Items items) {
        items.registerItem(ID, HerobrineScythe::new,
            p -> p.sword(ToolMaterial.DIAMOND, 3, -2.4F).rarity(Rarity.UNCOMMON).attributes(MODIFIERS));
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        SelfRepair.tick(stack, owner);
    }

    @Override
    public int getWarp(ItemStack stack, Player player) { return 3; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("item.thaumicbases.herobrines_scythe.lore").withStyle(ChatFormatting.ITALIC));
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity initial, LivingEntity attacker) {
        if (!(attacker instanceof Player player) || !(initial instanceof Enemy)
            || !(player.level() instanceof ServerLevel level)) return;
        var visited = new HashSet<LivingEntity>();
        visited.add(initial);
        LivingEntity origin = initial;
        while (!stack.isEmpty()) {
            var candidates = level.getEntitiesOfClass(LivingEntity.class, origin.getBoundingBox().inflate(6),
                target -> target instanceof Enemy && target.isAlive() && !visited.contains(target)
                    && !player.isAlliedTo(target) && level.mayInteract(player, target.blockPosition()));
            if (candidates.isEmpty()) break;
            var target = candidates.get(player.getRandom().nextInt(candidates.size()));
            visited.add(target);
            if (!target.hurtServer(level, level.damageSources().playerAttack(player), ATTACK)) continue;
            ModNetwork.sendToNear(level, null, origin.getX(), origin.getY(), origin.getZ(), 64,
                new ShockZapPayload(origin.getId(), target.getId()));
            level.playSound(null, target.blockPosition(), ModSounds.ZAP.get(), SoundSource.PLAYERS, 1.0F, 0.8F);
            stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            origin = target;
        }
    }

    static void research() {
        String id = Bases.id(ID).toString();
        var result = TcStack.of(id);
        var cost = new AspectList().add(Aspect.WEAPON, 128).add(Aspect.ENERGY, 64).add(Aspect.AURA, 12)
            .add(Aspect.ELDRITCH, 32).add(Aspect.DEATH, 64);
        var death = TcIngredient.item("thaumcraft:liquid_death_bucket");
        TcRecipes.register(new InfusionRecipe(id, RESEARCH, result, 7, cost, TcIngredient.item("thaumcraft:void_hoe"),
            List.of(TcIngredient.item("minecraft:nether_star"), death,
                TcIngredient.item("thaumcraft:greatwood_staff_core"), death)));
        var tags = new AspectList().add(Aspect.WEAPON, 16).add(Aspect.DEATH, 16).add(Aspect.ENERGY, 16)
            .add(Aspect.ELDRITCH, 8).add(Aspect.AURA, 6);
        ResearchCategories.addResearch(new BasesResearch(RESEARCH, Bases.ID, tags, 12, 2, 4, result)
            .setParents("VOIDMETAL").setPages(
                ResearchPage.text("tc.research_page." + RESEARCH + ".1"), ResearchPage.infusion(id)));
    }
}
