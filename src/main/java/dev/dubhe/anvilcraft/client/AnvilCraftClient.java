package dev.dubhe.anvilcraft.client;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.event.GuiLayerRegistrationEventListener;
import dev.dubhe.anvilcraft.config.AnvilCraftConfig;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import me.shedaniel.autoconfig.AutoConfig;

@Mod(value = AnvilCraft.MOD_ID, dist = Dist.CLIENT)
public class AnvilCraftClient {
    public AnvilCraftClient(IEventBus modBus, ModContainer container) {
        modBus.addListener(GuiLayerRegistrationEventListener::onRegister);

        container.registerExtensionPoint(
            IConfigScreenFactory.class,
            (c, s) -> AutoConfig.getConfigScreen(AnvilCraftConfig.class, s).get()
        );
    }
}
