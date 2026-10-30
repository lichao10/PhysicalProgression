package physical_progression.gametest.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import physical_progression.client.screen.ConditioningScreen;
import physical_progression.network.ModNetworking.ClientSnapshot;

/** Opt-in actual client-to-dedicated-server smoke test; excluded from release JAR. */
@EventBusSubscriber(modid = "physical_progression", value = Dist.CLIENT)
public final class PackagedConnectionTest {
  private static int phase, ticks, allTicks;
  private static long savedId;
  private static double startY, startStamina;
  private static boolean finished;

  private static void connect() {
    var mc = Minecraft.getInstance();
    ConnectScreen.startConnecting(
        new TitleScreen(),
        mc,
        ServerAddress.parseString("127.0.0.1:25573"),
        new ServerData("Conditioning isolated QA", "127.0.0.1:25573", ServerData.Type.OTHER),
        false,
        null);
  }

  private static void require(boolean value, String description) {
    if (!value) throw new IllegalStateException("CONNECTION_QA: " + description);
  }

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post e) {
    if (!Boolean.getBoolean("physical_progression.connectionQa") || finished) return;
    var mc = Minecraft.getInstance();
    if (mc.getOverlay() != null) return;
    if (++allTicks > 2400)
      throw new IllegalStateException("CONNECTION_QA timed out, phase=" + phase);
    if (phase == 0) {
      if (mc.screen == null) return;
      connect();
      phase = 1;
      return;
    }
    if (mc.player == null) {
      if (phase == 5 && ++ticks >= 30) {
        connect();
        phase = 6;
        ticks = 0;
      }
      return;
    }
    var d = mc.player.getData(ClientSnapshot.TYPE);
    if (!d.ready() || !d.attributes.contains("climbing")) return;
    if (phase == 1) {
      if (!mc.player.hasPermissions(2)) {
        if (ticks++ == 0)
          com.mojang.logging.LogUtils.getLogger()
              .info("CONNECTION_QA awaiting_op player={}", mc.player.getGameProfile().getName());
        require(ticks < 400, "fixture operator permission was not supplied");
        return;
      }
      com.mojang.logging.LogUtils.getLogger()
          .info(
              "CONNECTION_QA connected player={} protocol=5", mc.player.getGameProfile().getName());
      mc.player.connection.sendCommand("tp @s 0.65 64 0.5 -90 0");
      phase = 2;
      ticks = 0;
      return;
    }
    if (phase == 2) {
      if (++ticks < 40) return;
      startY = mc.player.getY();
      startStamina = d.resource("stamina");
      mc.options.keyJump.setDown(true);
      phase = 3;
      ticks = 0;
      return;
    }
    if (phase == 3) {
      // Keep synthetic input held when the unattended window loses focus.
      mc.options.keyJump.setDown(true);
      net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(
          new physical_progression.network.ClimbInputPayload(true, false));
      ticks++;
      if (ticks == 100) {
        require(mc.player.getY() > startY + 1, "jump input produces actual upward movement");
        require(
            d.resource("stamina") < startStamina - 3
                && d.resource("left") < 28
                && d.resource("right") < 28,
            "server snapshots show paid climb and hanging");
        com.mojang.logging.LogUtils.getLogger()
            .info(
                "CONNECTION_QA paid_hanging y={} stamina={} left={} right={} activity={}",
                mc.player.getY(),
                d.resource("stamina"),
                d.resource("left"),
                d.resource("right"),
                d.resources.getStringOr("activity", ""));
      }
      if (d.resources.getBooleanOr("climbExhausted", false)) {
        mc.options.keyJump.setDown(false);
        phase = 4;
        ticks = 0;
        return;
      }
      require(ticks < 800, "hanging must exhaust and release");
      return;
    }
    if (phase == 4) {
      if (++ticks < 100) return;
      require(mc.player.getY() < startY + 0.3, "exhaustion causes real descent onto floor");
      savedId = d.attributes.getCompoundOrEmpty("lastSession").getLongOr("id", 0L);
      require(savedId > 0, "training record arrived over network");
      mc.player.connection.sendCommand(
          "item replace entity @s armor.chest with minecraft:diamond_chestplate");
      mc.setScreen(new ConditioningScreen());
      phase = 7;
      ticks = 0;
      return;
    }
    if (phase == 7) {
      if (++ticks == 1) org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), 15, 15);
      if (ticks < 180) return;
      require(mc.screen instanceof ConditioningScreen, "body preview stays open with real player");
      Screenshot.grab(
          mc.gameDirectory,
          "physical_progression-connected-body.png",
          mc.getMainRenderTarget(),
          1,
          m -> {});
      com.mojang.logging.LogUtils.getLogger()
          .info("CONNECTION_QA preview rendered with player and equipment");
      mc.player
          .connection
          .getConnection()
          .disconnect(Component.literal("QA persistence reconnect"));
      phase = 5;
      ticks = 0;
      return;
    }
    if (phase == 6) {
      if (++ticks < 40) return;
      require(
          d.attributes.getCompoundOrEmpty("lastSession").getLongOr("id", 0L) >= savedId,
          "training history persists across actual disconnect and login");
      com.mojang.logging.LogUtils.getLogger()
          .info(
              "CONNECTION_QA PASS historyId={} reconnectedId={}",
              savedId,
              d.attributes.getCompoundOrEmpty("lastSession").getLongOr("id", 0L));
      finished = true;
      mc.options.keyJump.setDown(false);
      mc.player.connection.getConnection().disconnect(Component.literal("QA complete"));
      mc.stop();
    }
  }
}
