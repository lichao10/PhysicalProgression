package physical_progression.gametest.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@Mod("physical_progressionqa")
@EventBusSubscriber(modid = "physical_progressionqa", value = Dist.CLIENT)
public final class PackagedQaBootstrap {
  public PackagedQaBootstrap() {
    if (Boolean.getBoolean("physical_progression.connectionQa"))
      net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(PackagedConnectionTest.class);
  }

  private static boolean connected;
  private static int ticks;

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post e) {
    if (!Boolean.getBoolean("physical_progression.rejectQa")) return;
    var mc = Minecraft.getInstance();
    if (mc.getOverlay() != null || mc.screen == null) return;
    if (!connected) {
      connected = true;
      ConnectScreen.startConnecting(
          new TitleScreen(),
          mc,
          ServerAddress.parseString("127.0.0.1:25573"),
          new ServerData("protocol rejection QA", "127.0.0.1:25573", ServerData.Type.OTHER),
          false,
          null);
      return;
    }
    if (mc.player != null) throw new IllegalStateException("LEGACY_QA old protocol was accepted");
    if (mc.screen.getClass().getSimpleName().equals("ModMismatchDisconnectedScreen")) {
      com.mojang.logging.LogUtils.getLogger().info("LEGACY_QA disconnected as expected");
      mc.stop();
      return;
    }
    if (++ticks > 1200) throw new IllegalStateException("LEGACY_QA timed out");
  }
}
