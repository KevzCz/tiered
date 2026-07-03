package draylar.tiered.mixin;

import draylar.tiered.api.CustomEntityAttributes;
import draylar.tiered.api.imprint.ImprintAttributes;
import draylar.tiered.api.imprint.ImprintResolver;
import draylar.tiered.api.imprint.behavior.RavenousBehavior;
import draylar.tiered.util.AttributeHelper;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {

    private PlayerEntityMixin(EntityType<? extends LivingEntity> type, World world) {
        super(type, world);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void tieredImprintTick(CallbackInfo info) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (!self.getWorld().isClient()) {
            ImprintResolver.serverTick(self);

            if (self instanceof ServerPlayerEntity sp
                    && ImprintAttributes.hasConditionalAnyWornAttribute(sp)) {
                ImprintAttributes.refreshPlayerModifiers(sp);
            }
        }
    }

    @Inject(method = "createPlayerAttributes", at = @At("RETURN"))
    private static void createPlayerAttributesMixin(CallbackInfoReturnable<DefaultAttributeContainer.Builder> info) {
        info.getReturnValue().add(CustomEntityAttributes.CRIT_CHANCE);
        info.getReturnValue().add(CustomEntityAttributes.DIG_SPEED);
        info.getReturnValue().add(CustomEntityAttributes.DURABLE);
        info.getReturnValue().add(CustomEntityAttributes.RANGE_ATTACK_DAMAGE);
    }

    @ModifyVariable(method = "getBlockBreakingSpeed", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/effect/StatusEffectUtil;hasHaste(Lnet/minecraft/entity/LivingEntity;)Z"), index = 2)
    private float getBlockBreakingSpeedMixin(float f) {
        return AttributeHelper.getExtraDigSpeed((PlayerEntity) (Object) this, f);
    }

    @ModifyVariable(method = "attack", at = @At(value = "JUMP", ordinal = 2), slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;isSprinting()Z", ordinal = 1)), index = 8)
    private boolean attackMixin(boolean bl3) {
        return bl3 || AttributeHelper.shouldMeeleCrit((PlayerEntity) (Object) this);
    }

    @ModifyVariable(method = "addExhaustion", at = @At("HEAD"), argsOnly = true)
    private float tieredRavenousExhaustion(float exhaustion) {
        return exhaustion * RavenousBehavior.exhaustionMultiplier((PlayerEntity) (Object) this);
    }
}
