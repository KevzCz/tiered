package draylar.tiered.mixin;

import draylar.tiered.Tiered;
import draylar.tiered.api.ModifierUtils;
import draylar.tiered.api.PotentialAttribute;
import draylar.tiered.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LevelEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class ScreenHandlerMixin {

    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void onBlessedScrollDrop(
            int slotIndex,
            int button,
            ClickType actionType,
            Player player,
            CallbackInfo ci
    ) {
        if (actionType != ClickType.PICKUP) return;
        if (slotIndex < 0 || slotIndex >= ((AbstractContainerMenu) (Object) this).slots.size()) return;

        AbstractContainerMenu handler = (AbstractContainerMenu) (Object) this;
        Slot targetSlot = handler.getSlot(slotIndex);
        ItemStack cursorStack = handler.getCarried();

        if (!cursorStack.isEmpty()
                && cursorStack.getItem() == ModItems.BLESSED_SCROLL
                && targetSlot.hasItem()) {

            ItemStack targetStack = targetSlot.getItem();

            ResourceLocation attributeId = ModifierUtils.getAttributeId(targetStack);
            if (attributeId != null) {
                PotentialAttribute attribute = Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(attributeId);
                if (attribute != null && attribute.isCursed()) {

                    ModifierUtils.removeItemStackAttribute(targetStack);

                    ModifierUtils.setItemStackAttribute(player, targetStack, false, null, true, true);

                    cursorStack.shrink(1);

                    targetSlot.setByPlayer(targetStack);
                    targetSlot.setChanged();

                    player.level().levelEvent(LevelEvent.PARTICLES_TRIAL_SPAWNER_SPAWN_ITEM, player.blockPosition(), 0);

                    for (int i = 0; i < 20; i++) {
                        double height = player.getY() + (i / 20.0f) * player.getBbHeight();
                        double angle = Math.toRadians(i * 36);
                        double radius = 0.5;
                        double x = player.getX() + Math.cos(angle) * radius;
                        double z = player.getZ() + Math.sin(angle) * radius;

                        if (!player.level().isClientSide()) {
                            if (player.level() instanceof ServerLevel serverWorld) {
                                serverWorld.sendParticles(
                                        ParticleTypes.WITCH,
                                        x, height, z,
                                        1,
                                        0.0, 0.0, 0.0, 0.0
                                );
                            }
                        }

                    }

                    ci.cancel();
                }
            }
        }
    }
}
