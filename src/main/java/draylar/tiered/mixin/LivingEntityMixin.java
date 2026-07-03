package draylar.tiered.mixin;

import draylar.tiered.api.imprint.ImprintAttributes;
import draylar.tiered.api.imprint.ImprintResolver;
import draylar.tiered.api.imprint.behavior.AfflictionBehavior;
import draylar.tiered.mixin.access.ServerPlayerEntityAccessor;
import draylar.tiered.network.TieredServerPacket;
import draylar.tiered.util.AttributeHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    @Mutable
    @Shadow
    @Final
    private static TrackedData<Float> HEALTH;

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    private static final TagKey<DamageType> TIERED_COUNTS_AS_MAGIC =
            TagKey.of(RegistryKeys.DAMAGE_TYPE,
                    Identifier.of("tiered", "counts_as_magic"));
    private static final TagKey<DamageType> TIERED_COUNTS_AS_RANGED =
            TagKey.of(RegistryKeys.DAMAGE_TYPE,
                    Identifier.of("tiered", "counts_as_ranged"));

    private static final TagKey<DamageType> TIERED_AFFLICTION_AMPLIFIES =
            TagKey.of(RegistryKeys.DAMAGE_TYPE,
                    Identifier.of("tiered", "affliction_amplifies"));

    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
    private float tieredImprintMeleeDamage(float amount, DamageSource source) {
        if (TIERED_BONUS_GUARD.get()) return amount;
        if (source == null) return amount;
        LivingEntity target = (LivingEntity) (Object) this;

        if (source.getAttacker() == null && source.isIn(TIERED_AFFLICTION_AMPLIFIES)) {
            float afflict = AfflictionBehavior.amplifierFor(target);
            if (afflict > 0f) return amount * (1.0f + afflict);
        }

        if (!(source.getAttacker() instanceof PlayerEntity player)) return amount;
        boolean direct = source.getSource() == source.getAttacker();
        boolean ranged = (!direct && source.getSource() != null) || source.isIn(TIERED_COUNTS_AS_RANGED);
        if (direct) {
            amount = AttributeHelper.applyMeleeDamageImprints(player, target, amount);
        } else if (ranged) {
            amount = AttributeHelper.applyRangedDamageImprints(player, target, amount);
        }
        if (source.isIn(TIERED_COUNTS_AS_MAGIC)) {
            amount = AttributeHelper.applyMagicDamageImprints(player, target, source, amount);
        }
        return amount;
    }

    @Inject(method = "damage", at = @At("RETURN"))
    private void tieredImprintOnDamageDealt(DamageSource source, float amount,
            CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        if (TIERED_BONUS_GUARD.get()) return;
        if (this.getWorld().isClient()) return;
        if (!(source.getAttacker() instanceof PlayerEntity player)) return;
        if (source.getSource() != source.getAttacker()) return;
        LivingEntity self = (LivingEntity) (Object) this;
        ImprintResolver.dispatchDamageDealt(player, self, amount);

        if (self.isAlive()) {
            float bonus = ImprintResolver.resolveDataBonusMeleeDamage(player, self, amount);
            if (bonus > 0f) {
                TIERED_BONUS_GUARD.set(true);
                try {
                    self.damage(self.getDamageSources().playerAttack(player), bonus);
                } finally {
                    TIERED_BONUS_GUARD.set(false);
                }
            }
        }
        if (!self.isAlive()) {
            ImprintResolver.dispatchKill(player, self);
        }
    }

    private static final ThreadLocal<Boolean> TIERED_BONUS_GUARD = ThreadLocal.withInitial(() -> false);

    @Inject(method = "damage", at = @At("RETURN"))
    private void tieredImprintOnProjectileDamage(DamageSource source, float amount,
            CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        if (TIERED_BONUS_GUARD.get()) return;
        if (this.getWorld().isClient()) return;
        if (!(source.getAttacker() instanceof PlayerEntity player)) return;
        boolean projectile = (source.getSource() != null && source.getSource() != source.getAttacker())
                || source.isIn(TIERED_COUNTS_AS_RANGED);
        if (!projectile) return;
        LivingEntity self = (LivingEntity) (Object) this;
        ImprintResolver.dispatchProjectileDamageDealt(player, self, amount);
        if (!self.isAlive()) {
            ImprintResolver.dispatchProjectileKill(player, self);
        }
    }

    @Inject(method = "damage", at = @At("RETURN"))
    private void tieredImprintOnDamageTaken(DamageSource source, float amount,
            CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        if (TIERED_BONUS_GUARD.get()) return;
        if (this.getWorld().isClient()) return;
        if (!((Object) this instanceof PlayerEntity player)) return;

        if (source.getAttacker() == player) return;
        ImprintResolver.dispatchDamageTaken(player, amount, source);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("RETURN"))
    private void readCustomDataFromNbtMixin(CallbackInfo ci) {
        float current = this.dataTracker.get(HEALTH);
        this.setHealth(current);
    }

    @Inject(method = "getEquipmentChanges", at = @At(value = "TAIL"))
    private void getEquipmentChangesMixin(CallbackInfoReturnable<Map<EquipmentSlot, ItemStack>> cir) {
        if ((Object) this instanceof ServerPlayerEntity serverPlayerEntity) {
            ImprintAttributes.refreshPlayerModifiers(serverPlayerEntity);
            this.setHealth(this.getHealth() > this.getMaxHealth() ? this.getMaxHealth() : this.getHealth());
            TieredServerPacket.writeS2CHealthPacket(serverPlayerEntity);
            ((ServerPlayerEntityAccessor) serverPlayerEntity).setSyncedHealth(serverPlayerEntity.getHealth());
        }
    }

    @Shadow
    public float getHealth() {
        return 0f;
    }

    @Shadow
    public final float getMaxHealth() {
        return 0;
    }

    @Shadow
    public void setHealth(float health) {
    }

}
