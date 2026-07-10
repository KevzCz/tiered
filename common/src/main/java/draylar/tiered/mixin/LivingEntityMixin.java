package draylar.tiered.mixin;

import draylar.tiered.api.imprint.ImprintAttributes;
import draylar.tiered.api.imprint.ImprintResolver;
import draylar.tiered.api.imprint.behavior.AfflictionBehavior;
import draylar.tiered.mixin.access.ServerPlayerEntityAccessor;
import draylar.tiered.network.TieredServerPacket;
import draylar.tiered.util.AttributeHelper;
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
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    @Mutable
    @Shadow
    @Final
    private static EntityDataAccessor<Float> DATA_HEALTH_ID;

    public LivingEntityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    private static final TagKey<DamageType> TIERED_COUNTS_AS_MAGIC =
            TagKey.create(Registries.DAMAGE_TYPE,
                    ResourceLocation.fromNamespaceAndPath("tiered", "counts_as_magic"));
    private static final TagKey<DamageType> TIERED_COUNTS_AS_RANGED =
            TagKey.create(Registries.DAMAGE_TYPE,
                    ResourceLocation.fromNamespaceAndPath("tiered", "counts_as_ranged"));

    private static final TagKey<DamageType> TIERED_AFFLICTION_AMPLIFIES =
            TagKey.create(Registries.DAMAGE_TYPE,
                    ResourceLocation.fromNamespaceAndPath("tiered", "affliction_amplifies"));

    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true)
    private float tieredImprintMeleeDamage(float amount, DamageSource source) {
        if (TIERED_BONUS_GUARD.get()) return amount;
        if (source == null) return amount;
        LivingEntity target = (LivingEntity) (Object) this;

        if (source.getEntity() == null && source.is(TIERED_AFFLICTION_AMPLIFIES)) {
            float afflict = AfflictionBehavior.amplifierFor(target);
            if (afflict > 0f) return amount * (1.0f + afflict);
        }

        if (!(source.getEntity() instanceof Player player)) return amount;
        boolean direct = source.getDirectEntity() == source.getEntity();
        boolean ranged = (!direct && source.getDirectEntity() != null) || source.is(TIERED_COUNTS_AS_RANGED);
        if (direct) {
            amount = AttributeHelper.applyMeleeDamageImprints(player, target, amount);
        } else if (ranged) {
            amount = AttributeHelper.applyRangedDamageImprints(player, target, amount);
        }
        if (source.is(TIERED_COUNTS_AS_MAGIC)) {
            amount = AttributeHelper.applyMagicDamageImprints(player, target, source, amount);
        }
        return amount;
    }

    @Inject(method = "hurt", at = @At("RETURN"))
    private void tieredImprintOnDamageDealt(DamageSource source, float amount,
            CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        if (TIERED_BONUS_GUARD.get()) return;
        if (this.level().isClientSide()) return;
        if (!(source.getEntity() instanceof Player player)) return;
        if (source.getDirectEntity() != source.getEntity()) return;
        LivingEntity self = (LivingEntity) (Object) this;
        ImprintResolver.dispatchDamageDealt(player, self, amount);

        if (self.isAlive()) {
            float bonus = ImprintResolver.resolveDataBonusMeleeDamage(player, self, amount);
            if (bonus > 0f) {
                TIERED_BONUS_GUARD.set(true);
                try {
                    self.hurt(self.damageSources().playerAttack(player), bonus);
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

    @Inject(method = "hurt", at = @At("RETURN"))
    private void tieredImprintOnProjectileDamage(DamageSource source, float amount,
            CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        if (TIERED_BONUS_GUARD.get()) return;
        if (this.level().isClientSide()) return;
        if (!(source.getEntity() instanceof Player player)) return;
        boolean projectile = (source.getDirectEntity() != null && source.getDirectEntity() != source.getEntity())
                || source.is(TIERED_COUNTS_AS_RANGED);
        if (!projectile) return;
        LivingEntity self = (LivingEntity) (Object) this;
        ImprintResolver.dispatchProjectileDamageDealt(player, self, amount);
        if (!self.isAlive()) {
            ImprintResolver.dispatchProjectileKill(player, self);
        }
    }

    @Inject(method = "hurt", at = @At("RETURN"))
    private void tieredImprintOnDamageTaken(DamageSource source, float amount,
            CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        if (TIERED_BONUS_GUARD.get()) return;
        if (this.level().isClientSide()) return;
        if (!((Object) this instanceof Player player)) return;

        if (source.getEntity() == player) return;
        ImprintResolver.dispatchDamageTaken(player, amount, source);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void readCustomDataFromNbtMixin(CallbackInfo ci) {
        float current = this.entityData.get(DATA_HEALTH_ID);
        this.setHealth(current);
    }

    @Inject(method = "collectEquipmentChanges", at = @At(value = "TAIL"))
    private void getEquipmentChangesMixin(CallbackInfoReturnable<Map<EquipmentSlot, ItemStack>> cir) {
        if ((Object) this instanceof ServerPlayer serverPlayerEntity) {
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
