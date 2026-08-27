package net.fretux.ailments;

import com.mojang.logging.LogUtils;
import net.fretux.ailments.command.AilmentCommands;
import net.fretux.ailments.compat.AscendCompat;
import net.fretux.ailments.config.AilmentsConfig;
import net.fretux.ailments.registry.ModEffects;
import net.fretux.ailments.registry.ModItems;
import net.fretux.ailments.registry.ModPotions;
import net.fretux.ailments.network.AilmentNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(AscendAilments.MOD_ID)
public final class AscendAilments {
    public static final String MOD_ID = "ascend_ailments";
    private static final Logger LOGGER = LogUtils.getLogger();

    public AscendAilments(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        ModEffects.register(modBus);
        ModItems.register(modBus);
        ModPotions.register(modBus);
        AilmentNetwork.register();
        modBus.addListener(this::commonSetup);
        // Gameplay settings belong to the server and are synchronized to multiplayer clients by Forge.
        context.registerConfig(ModConfig.Type.SERVER, AilmentsConfig.SPEC,
                "ascend-ailments-server.toml");
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModPotions.registerBrewingRecipes();
            if (AscendCompat.isAscendLoaded())
                LOGGER.info("Ascend Arcane integration API available: {}", AscendCompat.validateIntegration());
        });
    }

    private void registerCommands(RegisterCommandsEvent event) {
        AilmentCommands.register(event.getDispatcher());
    }
}
