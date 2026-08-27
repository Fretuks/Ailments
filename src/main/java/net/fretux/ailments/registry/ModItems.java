package net.fretux.ailments.registry;

import net.fretux.ailments.AscendAilments;
import net.fretux.ailments.api.AilmentType;
import net.fretux.ailments.item.DebugAilmentStickItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

/** Item registration for development tools that are intentionally unavailable in survival progression. */
public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, AscendAilments.MOD_ID);

    public static final RegistryObject<Item> SOUL_ROT_DEBUG_STICK = debugStick("soul_rot", AilmentType.SOUL_ROT);
    public static final RegistryObject<Item> BLEED_DEBUG_STICK = debugStick("bleed", AilmentType.BLEED);
    public static final RegistryObject<Item> FRACTURE_DEBUG_STICK = debugStick("fracture", AilmentType.FRACTURE);
    public static final RegistryObject<Item> FEAR_DEBUG_STICK = debugStick("fear", AilmentType.FEAR);
    public static final RegistryObject<Item> CHARM_DEBUG_STICK = debugStick("charm", AilmentType.CHARM);
    public static final RegistryObject<Item> TAUNT_DEBUG_STICK = debugStick("taunt", AilmentType.TAUNT);
    public static final RegistryObject<Item> OVERCHARM_DEBUG_STICK = debugStick("overcharm", AilmentType.OVERCHARM);
    public static final RegistryObject<Item> WINDED_DEBUG_STICK = debugStick("winded", AilmentType.WINDED);

    private static final List<RegistryObject<Item>> DEBUG_STICKS = List.of(
            SOUL_ROT_DEBUG_STICK,
            BLEED_DEBUG_STICK,
            FRACTURE_DEBUG_STICK,
            FEAR_DEBUG_STICK,
            CHARM_DEBUG_STICK,
            TAUNT_DEBUG_STICK,
            OVERCHARM_DEBUG_STICK,
            WINDED_DEBUG_STICK
    );

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        modBus.addListener(ModItems::addCreativeItems);
    }

    private static RegistryObject<Item> debugStick(String effectName, AilmentType ailmentType) {
        return ITEMS.register(effectName + "_debug_stick",
                () -> new DebugAilmentStickItem(ailmentType,
                        new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    }

    private static void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES)
            DEBUG_STICKS.forEach(event::accept);
    }

    private ModItems() {}
}
