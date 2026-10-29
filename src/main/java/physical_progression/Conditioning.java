package physical_progression;

import physical_progression.attachment.ModAttachments;
import physical_progression.config.ConditioningConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(Conditioning.MOD_ID)
public final class Conditioning {
  public static final String MOD_ID = "physical_progression";

  public Conditioning(IEventBus bus, ModContainer container) {
    physical_progression.network.ModNetworking.ClientSnapshot.init();
    ModAttachments.REGISTER.register(bus);
    container.registerConfig(ModConfig.Type.SERVER, ConditioningConfig.SPEC);
    container.registerConfig(
        ModConfig.Type.CLIENT, physical_progression.config.ConditioningClientConfig.SPEC);
  }
}
