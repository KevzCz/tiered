package draylar.tiered.fabric.mixin;

import draylar.tiered.fabric.PlayerAttributesFabric;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerAttributesMixin {

    @Inject(method = "createAttributes", at = @At("RETURN"))
    private static void tiered$addAttributes(CallbackInfoReturnable<AttributeSupplier.Builder> info) {
        PlayerAttributesFabric.addTo(info.getReturnValue());
    }
}
