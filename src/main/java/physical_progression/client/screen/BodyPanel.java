package physical_progression.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import physical_progression.client.widget.*;

public final class BodyPanel {
  public static String condition(physical_progression.client.data.ClientConditioningData d) {
    if (d.resources.getBooleanOr("recovering", false)) return "recovering";
    double fraction =
        Math.min(
            d.resource("stamina") / Math.max(1, d.resource("staminaMax")),
            Math.min(
                d.resource("legs") / Math.max(1, d.resource("legsMax")),
                Math.min(
                    d.resource("left") / Math.max(1, d.resource("leftMax")),
                    d.resource("right") / Math.max(1, d.resource("rightMax")))));
    return fraction <= 0
        ? "exhausted"
        : fraction < 0.2
            ? "fatigued"
            : !d.resources.getStringOr("activity", "").equals("rest")
                ? "working"
                : fraction < 0.95 ? "warm" : "fresh";
  }

  public static void model(PanelContext p) {
    var d = p.data;
    int cx = p.x + p.width / 2, y = p.y;
    p.g.centeredText(
        Minecraft.getInstance().font,
        Component.translatable("ui.physical_progression.body"),
        cx,
        y,
        0xFFFFD56A);
    var player = Minecraft.getInstance().player;
    if (player != null) {
      renderPlayer(p, cx, y + 137, player);
      var font = Minecraft.getInstance().font;
      p.g.text(
          font,
          Component.translatable("ui.physical_progression.part_right"),
          p.x + 2,
          y + 126,
          0xFFDDDDDD);
      p.g.text(
          font,
          Component.translatable("ui.physical_progression.part_left"),
          p.x + p.width - 12,
          y + 126,
          0xFFDDDDDD);
    } else {
      p.g.textWithWordWrap(
          Minecraft.getInstance().font,
          Component.translatable("ui.physical_progression.waiting"),
          p.x + 6,
          y + 66,
          p.width - 12,
          0xFFAAAAAA);
    }
    p.g.centeredText(
        Minecraft.getInstance().font,
        Component.translatable("state.physical_progression." + condition(d)),
        cx,
        y + 145,
        0xFFEAEAEA);
    ConditioningBar.draw(
        p.g,
        "hud.physical_progression.stamina",
        p.x,
        y + 162,
        p.width,
        d.resource("stamina"),
        d.resource("staminaMax"));
    ConditioningBar.draw(
        p.g,
        "hud.physical_progression.legs",
        p.x,
        y + 190,
        p.width,
        d.resource("legs"),
        d.resource("legsMax"));
  }

  private static void renderPlayer(
      PanelContext p, int x, int y, net.minecraft.world.entity.player.Player player) {
    // Entity previews use screen-space bounds, unlike regular GUI elements in the retained
    // renderer.
    var pose = p.g.pose();
    var start = pose.transformPosition(p.x + 2, y - 122, new org.joml.Vector2f());
    var end = pose.transformPosition(p.x + p.width - 2, y, new org.joml.Vector2f());
    var mouse = pose.transformPosition(p.mouseX, p.mouseY, new org.joml.Vector2f());
    int size = Math.max(1, Math.round(55 * (float) Math.hypot(pose.m00(), pose.m01())));
    pose.pushMatrix();
    pose.identity();
    net.minecraft.client.gui.screens.inventory.InventoryScreen.extractEntityInInventoryFollowsMouse(
        p.g,
        (int) start.x,
        (int) start.y,
        (int) end.x,
        (int) end.y,
        size,
        0f,
        mouse.x,
        mouse.y,
        player);
    pose.popMatrix();
  }

  public static void details(PanelContext p) {
    p.heading("ui.physical_progression.current_status");
    for (String key : new String[] {"stamina", "legs", "left", "right"}) {
      ConditioningBar.draw(
          p.g,
          "hud.physical_progression." + key,
          p.x,
          p.y,
          p.width,
          p.data.resource(key),
          p.data.resource(key + "Max"));
      p.y += 30;
    }
    p.row(
        "ui.physical_progression.recovery_rate",
        PanelContext.number(p.data.resource("recoveryRate")) + " /s",
        null);
    p.row(
        "ui.physical_progression.walking_recovery",
        PanelContext.number(p.data.resource("walkingRecoveryRate")) + " /s",
        null);
    p.row(
        "ui.physical_progression.recovery_delay",
        PanelContext.number(p.data.resources.getIntOr("recoveryDelay", 0) / 20.0) + " s",
        null);
    p.text("ui.physical_progression.capacity_note");
  }
}
