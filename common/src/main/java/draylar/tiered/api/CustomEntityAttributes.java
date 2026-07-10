package draylar.tiered.api;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

public class CustomEntityAttributes {

    // Registering entity attributes directly via Registry.registerReference in a static
    // initializer is unsafe: PlayerEntityMixin's injected code references these fields, so
    // this class gets loaded (triggering its <clinit>) the moment Mixin finishes transforming
    // PlayerEntity - i.e. mid mixin-transform, on whichever thread is doing that transform.
    // Registering into a live registry from inside that nested classloading context can hang
    // instead of throwing. Deferring via Architectury's DeferredRegister (applied later, on
    // RegisterEvent/mod init) avoids that entirely, matching the ModDataComponents fix.
    @SuppressWarnings("unchecked")
    private static final ResourceKey<Registry<Attribute>> ATTRIBUTE_KEY =
            (ResourceKey<Registry<Attribute>>) (ResourceKey<?>) Registries.ATTRIBUTE;

    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create("tiered_more", ATTRIBUTE_KEY);

    private static final RegistrySupplier<Attribute> DIG_SPEED_SUPPLIER =
            ATTRIBUTES.register(ResourceLocation.fromNamespaceAndPath("tiered", "generic.dig_speed"), () ->
                    new RangedAttribute("attribute.name.generic.dig_speed", 0.0D, 0.0D, 2048.0D).setSyncable(true));
    private static final RegistrySupplier<Attribute> CRIT_CHANCE_SUPPLIER =
            ATTRIBUTES.register(ResourceLocation.fromNamespaceAndPath("tiered", "generic.crit_chance"), () ->
                    new RangedAttribute("attribute.name.generic.crit_chance", 0.0D, 0.0D, 1D).setSyncable(true));
    private static final RegistrySupplier<Attribute> DURABLE_SUPPLIER =
            ATTRIBUTES.register(ResourceLocation.fromNamespaceAndPath("tiered", "generic.durable"), () ->
                    new RangedAttribute("attribute.name.generic.durable", 0.0D, 0.0D, 1D).setSyncable(true));
    private static final RegistrySupplier<Attribute> RANGE_ATTACK_DAMAGE_SUPPLIER =
            ATTRIBUTES.register(ResourceLocation.fromNamespaceAndPath("tiered", "generic.range_attack_damage"), () ->
                    new RangedAttribute("attribute.name.generic.range_attack_damage", 0.0D, 0.0D, 2048.0D).setSyncable(true));

    public static Holder<Attribute> DIG_SPEED;
    public static Holder<Attribute> CRIT_CHANCE;
    public static Holder<Attribute> DURABLE;
    public static Holder<Attribute> RANGE_ATTACK_DAMAGE;

    // Holder fields are resolved fresh from BuiltInRegistries rather than via
    // RegistrySupplier#getRegistrar().delegate(...): on NeoForge, feeding Architectury's
    // RegistrySupplier wrapper (which implements both Holder and Architectury's own
    // DeferredSupplier) into AttributeSupplier.Builder throws IncompatibleClassChangeError
    // ("Conflicting default methods") against NeoForge's own DeferredHolder/IHolderExtension.
    // A plain vanilla Holder.Reference looked up by id has no such conflict.
    public static void init() {
        DIG_SPEED = getHolder(DIG_SPEED_SUPPLIER.getId());
        CRIT_CHANCE = getHolder(CRIT_CHANCE_SUPPLIER.getId());
        DURABLE = getHolder(DURABLE_SUPPLIER.getId());
        RANGE_ATTACK_DAMAGE = getHolder(RANGE_ATTACK_DAMAGE_SUPPLIER.getId());
    }

    private static Holder<Attribute> getHolder(ResourceLocation id) {
        return BuiltInRegistries.ATTRIBUTE.getHolder(ResourceKey.create(Registries.ATTRIBUTE, id)).orElseThrow();
    }

    public static void register() {
        ATTRIBUTES.register();
    }
}
