package draylar.tiered.mixin;

import draylar.tiered.api.ModifierUtils;
import draylar.tiered.api.PotentialAttribute;
import draylar.tiered.registry.ModItems;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;
import net.minecraft.world.WorldEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenHandler.class)
public abstract class ScreenHandlerMixin {

    @Inject(method = "onSlotClick", at = @At("HEAD"), cancellable = true)
    private void onBlessedScrollDrop(
            int slotIndex,
            int button,
            SlotActionType actionType,
            PlayerEntity player,
            CallbackInfo ci
    ) {
        if (actionType != SlotActionType.PICKUP) return; // Only trigger on normal click
        if (slotIndex < 0 || slotIndex >= ((ScreenHandler) (Object) this).slots.size()) return;

        ScreenHandler handler = (ScreenHandler) (Object) this;
        Slot targetSlot = handler.getSlot(slotIndex);
        ItemStack cursorStack = handler.getCursorStack();

        // Only activate if holding Blessed Scroll and clicking a filled slot
        if (!cursorStack.isEmpty()
                && cursorStack.getItem() == ModItems.BLESSED_SCROLL
                && targetSlot.hasStack()) {

            ItemStack targetStack = targetSlot.getStack();

            // Check if item has a cursed modifier
            Identifier attributeId = ModifierUtils.getAttributeId(targetStack);
            if (attributeId != null) {
                PotentialAttribute attribute = draylar.tiered.Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(attributeId);
                if (attribute != null && attribute.isCursed()) {

                    ModifierUtils.removeItemStackAttribute(targetStack);

                    ModifierUtils.setItemStackAttribute(player, targetStack, false, null, true); // skipCursed = true

                    cursorStack.decrement(1);

                    targetSlot.setStack(targetStack);
                    targetSlot.markDirty();

                    player.getWorld().syncWorldEvent(WorldEvents.OMINOUS_ITEM_SPAWNER_SPAWNS_ITEM, player.getBlockPos(), 0);

                    for (int i = 0; i < 20; i++) {
                        double height = player.getY() + (i / 20.0f) * player.getHeight();
                        double angle = Math.toRadians(i * 36);
                        double radius = 0.5;
                        double x = player.getX() + Math.cos(angle) * radius;
                        double z = player.getZ() + Math.sin(angle) * radius;

                        if (!player.getWorld().isClient()) {
                            if (player.getWorld() instanceof net.minecraft.server.world.ServerWorld serverWorld) {
                                serverWorld.spawnParticles(
                                        net.minecraft.particle.ParticleTypes.WITCH,
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
