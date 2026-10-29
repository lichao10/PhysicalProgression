package physical_progression.client.screen;

import physical_progression.client.widget.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class BodyPanel {
  public static String condition(physical_progression.client.data.ClientConditioningData d) {
    if (d.resources.getBoolean("recovering")) return "recovering";
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
            : !d.resources.getString("activity").equals("rest")
                ? "working"
                : fraction < 0.95 ? "warm" : "fresh";
  }

  public static void model(PanelContext p) {
    var d = p.data;
    int cx = p.x + p.width / 2, y = p.y;
    p.g.drawCenteredString(
        Minecraft.getInstance().font,
        Component.translatable("ui.physical_progression.body"),
        cx,
        y,
        0xFFFFD56A);
    var player = Minecraft.getInstance().player;
    if (player != null) {
      renderPlayer(p, cx, y + 137, player);
      var font = Minecraft.getInstance().font;
      p.g.drawString(
          font, Component.translatable("ui.physical_progression.part_right"), p.x + 2, y + 126, 0xFFDDDDDD);
      p.g.drawString(
          font,
          Component.translatable("ui.physical_progression.part_left"),
          p.x + p.width - 12,
          y + 126,
          0xFFDDDDDD);
    } else {
      p.g.drawWordWrap(
          Minecraft.getInstance().font,
          Component.translatable("ui.physical_progression.waiting"),
          p.x + 6,
          y + 66,
          p.width - 12,
          0xFFAAAAAA);
    }
    p.g.drawCenteredString(
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

  // Render in the menu's pose space; the vanilla follows-mouse helper clips in unscaled screen
  // coordinates.
  private static void renderPlayer(
      PanelContext p, int x, int y, net.minecraft.world.entity.player.Player player) {
    float body = player.yBodyRot, yaw = player.getYRot(), pitch = player.getXRot();
    float head = player.yHeadRot, previousHead = player.yHeadRotO;
    float angleX = (float) Math.atan((x - p.mouseX) / 80.0);
    float angleY = (float) Math.atan((y - 65 - p.mouseY) / 80.0);
    var tilt = new org.joml.Quaternionf().rotateX(angleY * 20 * (float) Math.PI / 180);
    try {
      player.yBodyRot = 180 + angleX * 20;
      player.setYRot(180 + angleX * 40);
      player.setXRot(-angleY * 20);
      player.yHeadRot = player.getYRot();
      player.yHeadRotO = player.getYRot();
      net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventory(
          p.g,
          x,
          y,
          55,
          new org.joml.Vector3f(),
          new org.joml.Quaternionf().rotateZ((float) Math.PI).mul(tilt),
          tilt,
          player);
    } finally {
      player.yBodyRot = body;
      player.setYRot(yaw);
      player.setXRot(pitch);
      player.yHeadRot = head;
      player.yHeadRotO = previousHead;
    }
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
        PanelContext.number(p.data.resources.getInt("recoveryDelay") / 20.0) + " s",
        null);
    p.text("ui.physical_progression.capacity_note");
  }
}
