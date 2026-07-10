package draylar.tiered.mixin;

import draylar.tiered.api.ModifierUtils;
import draylar.tiered.api.imprint.ImprintAttributes;
import draylar.tiered.api.imprint.ImprintResolver;
import draylar.tiered.api.imprint.behavior.RavenousBehavior;
import draylar.tiered.util.AttributeHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerEntityMixin extends LivingEntity {

    private PlayerEntityMixin(EntityType<? extends LivingEntity> type, Level world) {
        super(type, world);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void tieredImprintTick(CallbackInfo info) {
        Player self = (Player) (Object) this;
        if (!self.level().isClientSide()) {
            ImprintResolver.serverTick(self);

            if (self instanceof ServerPlayer sp) {
                if (ImprintAttributes.hasConditionalAnyWornAttribute(sp)) {
                    ImprintAttributes.refreshPlayerModifiers(sp);
                }
                ModifierUtils.refreshAccessoryTierAttributes(sp);
            }
        }
    }

    @ModifyVariable(method = "attack", at = @At(value = "JUMP", ordinal = 2), slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isSprinting()Z", ordinal = 1)), index = 8)
    private boolean attackMixin(boolean bl3) {
        return bl3 || AttributeHelper.shouldMeeleCrit((Player) (Object) this);
    }

    @ModifyVariable(method = "causeFoodExhaustion", at = @At("HEAD"), argsOnly = true)
    private float tieredRavenousExhaustion(float exhaustion) {
        return exhaustion * RavenousBehavior.exhaustionMultiplier((Player) (Object) this);
    }
}
