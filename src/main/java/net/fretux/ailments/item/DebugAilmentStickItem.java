package net.fretux.ailments.item;

import net.fretux.ailments.api.AilmentApi;
import net.fretux.ailments.api.AilmentApplication;
import net.fretux.ailments.api.AilmentType;
import net.fretux.ailments.config.AilmentsConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Creative-only test item that applies one ailment through the authoritative public API. */
public final class DebugAilmentStickItem extends Item {
    private final AilmentType ailmentType;

    public DebugAilmentStickItem(AilmentType ailmentType, Properties properties) {
        super(properties);
        this.ailmentType = ailmentType;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (target.level().isClientSide || !(attacker instanceof Player player) || !player.isCreative()) return false;

        return switch (ailmentType) {
            case SOUL_ROT, BLEED, FRACTURE ->
                    AilmentApi.applyEffect(target, attacker, AilmentApplication.stack(ailmentType));
            case FEAR, CHARM, TAUNT, OVERCHARM, WINDED ->
                    AilmentApi.applyEffect(target, attacker, ailmentType,
                            AilmentsConfig.value(AilmentsConfig.TAGGED_WEAPON_EFFECT_DURATION), 0);
        };
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
                                TooltipFlag tooltipFlag) {
        tooltip.add(Component.translatable("item.ascend_ailments.debug_stick.tooltip")
                .withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, level, tooltip, tooltipFlag);
    }
}
