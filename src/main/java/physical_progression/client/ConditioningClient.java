package physical_progression.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;
import physical_progression.client.hud.ConditioningHud;
import physical_progression.client.screen.ConditioningScreen;

@EventBusSubscriber(modid = "physical_progression", value = Dist.CLIENT)
public final class ConditioningClient {
  public static final KeyMapping OPEN_SCREEN =
      new KeyMapping(
          "key.physical_progression.open",
          KeyConflictContext.IN_GAME,
          InputConstants.Type.KEYSYM,
          GLFW.GLFW_KEY_C,
          KeyMapping.Category.register(
              Identifier.fromNamespaceAndPath("physical_progression", "general")));

  private static boolean climbHeld, movementHeld;

  @SubscribeEvent
  public static void climb(ClientTickEvent.Pre e) {
    var mc = Minecraft.getInstance();
    if (mc.player == null) {
      climbHeld = movementHeld = false;
      return;
    }
    boolean held = mc.screen == null && mc.options.keyJump.isDown();
    boolean moving =
        mc.screen == null
            && (mc.options.keyUp.isDown()
                || mc.options.keyDown.isDown()
                || mc.options.keyLeft.isDown()
                || mc.options.keyRight.isDown()
                || held);
    physical_progression.body.ClimbingSystem.request(mc.player, held);
    if (held != climbHeld
        || moving != movementHeld
        || (held || moving) && mc.player.tickCount % 5 == 0)
      net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(
          new physical_progression.network.ClimbInputPayload(held, moving));
    climbHeld = held;
    movementHeld = moving;
    physical_progression.body.ClimbingSystem.tick(mc.player);
  }

  @SubscribeEvent
  public static void layers(RegisterGuiLayersEvent e) {
    e.registerAboveAll(
        Identifier.fromNamespaceAndPath("physical_progression", "hud"), ConditioningHud::render);
  }

  @SubscribeEvent
  public static void keys(RegisterKeyMappingsEvent e) {
    e.register(OPEN_SCREEN);
  }

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post e) {
    var mc = Minecraft.getInstance();
    while (OPEN_SCREEN.consumeClick())
      if (mc.player != null && mc.screen == null) mc.setScreen(new ConditioningScreen());
    ConditioningHud.tick();
  }
}
