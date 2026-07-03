package draylar.tiered.util;

import draylar.tiered.api.CustomEntityAttributes;
import draylar.tiered.api.imprint.ImprintResolver;
import draylar.tiered.api.imprint.behavior.RetaliationBehavior;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;

public class AttributeHelper {

    public static float applyMeleeDamageImprints(PlayerEntity player, LivingEntity target, float baseDamage) {

        float bonusFraction = ImprintResolver.resolveDataMeleeDamageFraction(player, target);

        bonusFraction += RetaliationBehavior.consumeCharge(player);
        if (bonusFraction <= 0f) return baseDamage;
        return baseDamage * (1.0f + bonusFraction);
    }

    public static float applyRangedDamageImprints(PlayerEntity player, LivingEntity target, float baseDamage) {
        float bonusFraction = ImprintResolver.resolveDataRangedDamageFraction(player, target);
        return bonusFraction <= 0f ? baseDamage : baseDamage * (1.0f + bonusFraction);
    }

    public static float applyMagicDamageImprints(PlayerEntity player, LivingEntity target, DamageSource source,
            float baseDamage) {
        float bonusFraction = ImprintResolver.resolveDataMagicDamageFraction(player, target, source);
        return bonusFraction <= 0f ? baseDamage : baseDamage * (1.0f + bonusFraction);
    }

    public static boolean shouldMeeleCrit(PlayerEntity playerEntity) {
        EntityAttributeInstance instance = playerEntity.getAttributeInstance(CustomEntityAttributes.CRIT_CHANCE);
        if (instance != null) {
            float critChance = 0.0f;
            for (EntityAttributeModifier modifier : instance.getModifiers()) {
                float amount = (float) modifier.value();
                critChance += amount;
            }
            return playerEntity.getRandom().nextDouble() < critChance;
        }
        return false;
    }

    public static float getExtraDigSpeed(PlayerEntity playerEntity, float oldDigSpeed) {
        EntityAttributeInstance instance = playerEntity.getAttributeInstance(CustomEntityAttributes.DIG_SPEED);
        if (instance != null) {
            float extraDigSpeed = oldDigSpeed;
            for (EntityAttributeModifier modifier : instance.getModifiers()) {
                float amount = (float) modifier.value();

                if (modifier.operation() == EntityAttributeModifier.Operation.ADD_VALUE) {
                    extraDigSpeed += amount;
                } else {
                    extraDigSpeed *= (amount + 1);
                }
            }
            return extraDigSpeed;
        }

        return oldDigSpeed;
    }

    public static float getExtraRangeDamage(PlayerEntity playerEntity, float oldDamage) {
        EntityAttributeInstance instance = playerEntity.getAttributeInstance(CustomEntityAttributes.RANGE_ATTACK_DAMAGE);
        if (instance != null) {
            float rangeDamage = oldDamage;
            for (EntityAttributeModifier modifier : instance.getModifiers()) {
                float amount = (float) modifier.value();

                if (modifier.operation() == EntityAttributeModifier.Operation.ADD_VALUE) {
                    rangeDamage += amount;
                } else {
                    rangeDamage *= (amount + 1.0f);
                }
            }
            return Math.min(rangeDamage, Integer.MAX_VALUE);
        }
        return oldDamage;
    }

    public static float getExtraCritDamage(PlayerEntity playerEntity, float oldDamage) {
        EntityAttributeInstance instance = playerEntity.getAttributeInstance(CustomEntityAttributes.CRIT_CHANCE);
        if (instance != null) {
            float customChance = 0.0f;
            for (EntityAttributeModifier modifier : instance.getModifiers()) {
                customChance += (float) modifier.value();
            }
            if (playerEntity.getWorld().getRandom().nextFloat() > (1.0f - Math.abs(customChance))) {
                float extraCrit = oldDamage;
                if (customChance < 0.0f) {
                    extraCrit = extraCrit / 2.0f;
                }
                return oldDamage + Math.min(customChance > 0.0f ? extraCrit : -extraCrit, Integer.MAX_VALUE);
            }
        }
        return oldDamage;
    }

}
