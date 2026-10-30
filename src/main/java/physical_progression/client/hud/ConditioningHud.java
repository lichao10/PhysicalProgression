package physical_progression.client.hud;

import java.util.Locale;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import physical_progression.client.widget.ConditioningBar;
import physical_progression.config.ConditioningClientConfig;
import physical_progression.network.ModNetworking.ClientSnapshot;

public final class ConditioningHud {
  private static Object playerIdentity;
  private static long lastId;
  private static int toastTicks;
  private static long lastFrame;
  private static final java.util.Map<String, Float> visibility = new java.util.HashMap<>();

  public static void tick() {
    var p = Minecraft.getInstance().player;
    if (p == null) {
      playerIdentity = null;
      toastTicks = 0;
      return;
    }
    var d = p.getData(ClientSnapshot.TYPE);
    long id = d.attributes.getCompoundOrEmpty("lastSession").getLongOr("id", 0L);
    if (playerIdentity != p) {
      visibility.clear();
      playerIdentity = p;
      lastId = id;
      toastTicks = 0;
    } else if (id > lastId) {
      if ((lastId != 0 || p.tickCount > 40)
          && (!d.attributes.getCompoundOrEmpty("lastSession").contains("outcome")
              || d.attributes
                  .getCompoundOrEmpty("lastSession")
                  .getStringOr("outcome", "")
                  .equals("rewarded"))) toastTicks = 120;
      lastId = id;
    }
    if (toastTicks > 0) toastTicks--;
  }

  public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
    var mc = Minecraft.getInstance();
    var p = mc.player;
    if (p == null
        || mc.gui.hud.isHidden()
        || p.isCreative()
        || p.isSpectator()
        || mc.gui.screen() instanceof physical_progression.client.screen.ConditioningScreen) return;
    var d = p.getData(ClientSnapshot.TYPE);
    renderSnapshot(g, d, toastTicks > 0);
  }

  public static void renderSnapshot(
      GuiGraphicsExtractor g,
      physical_progression.client.data.ClientConditioningData d,
      boolean feedback) {
    var mc = Minecraft.getInstance();
    if (!d.ready()) return;
    g.pose().pushMatrix();
    float hudScale = ConditioningClientConfig.HUD_SCALE.get().floatValue();
    g.pose().scale(hudScale, hudScale);
    var visible = HudLayout.visible(d, ConditioningClientConfig.HUD_MODE.get());
    visible.removeIf(
        key -> {
          double value = key.equals("breath") ? d.resources.getIntOr("air", 0) : d.resource(key);
          double maximum =
              key.equals("breath") ? d.resources.getIntOr("airMax", 0) : d.resource(key + "Max");
          return !ConditioningClientConfig.visible(key)
              || (ConditioningClientConfig.HUD_MODE.get()
                      != ConditioningClientConfig.HudMode.ALWAYS_SHOW
                  && !key.equals("breath")
                  && value / Math.max(1, maximum) >= 0.98);
        });
    int screenHeight = (int) (g.guiHeight() / hudScale);
    int y =
        ConditioningClientConfig.HUD_ANCHOR.get() == ConditioningClientConfig.HudAnchor.LEFT_CENTER
            ? Math.max(8, screenHeight / 2 - Math.max(11, visible.size() * 11) / 2)
            : Math.max(8, screenHeight - 92);
    long now = System.nanoTime();
    float elapsed = lastFrame == 0 ? 0.016f : Math.min(0.1f, (now - lastFrame) / 1_000_000_000f);
    lastFrame = now;
    for (String key : new String[] {"stamina", "legs", "left", "right", "breath"}) {
      double current =
          key.equals("breath") ? d.resources.getIntOr("air", 0) / 20.0 : d.resource(key);
      double max =
          key.equals("breath") ? d.resources.getIntOr("airMax", 0) / 20.0 : d.resource(key + "Max");
      double ratio = Math.clamp(current / Math.max(1, max), 0, 1);
      boolean show =
          visible.contains(key)
              && ConditioningClientConfig.visible(key)
              && (ConditioningClientConfig.HUD_MODE.get()
                      == ConditioningClientConfig.HudMode.ALWAYS_SHOW
                  || key.equals("breath")
                  || ratio < 0.98);
      float opacity = visibility.getOrDefault(key, 0f);
      opacity = Math.clamp(opacity + (show ? 1 : -1) * elapsed * 3, 0, 1);
      visibility.put(key, opacity);
      if (opacity < 0.05f) continue;
      int alpha = (int) (opacity * 255) << 24;
      float textScale = ConditioningClientConfig.HUD_TEXT_SCALE.get().floatValue();
      g.pose().pushMatrix();
      g.pose().translate(8, y);
      g.pose().scale(textScale, textScale);
      g.text(
          mc.font,
          Component.translatable("hud.physical_progression.short_" + key),
          0,
          0,
          alpha | 0xDDDDDD);
      g.pose().popMatrix();
      int x = 33, w = 44;
      g.fill(x - 1, y + 1, x + w + 1, y + 7, ((int) (opacity * 100) << 24) | 0x101010);
      g.fill(x, y + 2, x + w, y + 6, ((int) (opacity * 100) << 24) | 0x777777);
      int color = key.equals("breath") ? 0x62B6DB : ConditioningBar.color(ratio) & 0xFFFFFF;
      g.fill(x, y + 2, x + (int) (w * ratio), y + 6, alpha | color);
      if (ratio <= 0.2) {
        g.pose().pushMatrix();
        g.pose().translate(x + w + 4, y);
        g.pose().scale(textScale, textScale);
        g.text(mc.font, Math.round(ratio * 100) + "%", 0, 0, alpha | 0xFFBD75);
        g.pose().popMatrix();
      }
      y += 11;
    }
    g.pose().popMatrix();
    if (feedback
        && ConditioningClientConfig.FEEDBACK.get()
        && ConditioningClientConfig.HUD_MODE.get() != ConditioningClientConfig.HudMode.OFF) {
      var t = d.attributes.getCompoundOrEmpty("lastSession");
      double xp = 0;
      for (String key : t.keySet()) if (key.endsWith("Xp")) xp += t.getDoubleOr(key, 0.0);
      float toastScale = ConditioningClientConfig.TOAST_SCALE.get().floatValue();
      g.pose().pushMatrix();
      g.pose().translate(g.guiWidth() - 6 - 200 * toastScale, 8);
      g.pose().scale(toastScale, toastScale);
      int x = 0;
      physical_progression.client.widget.VanillaTheme.frame(g, x, 8, 200, 53);
      g.text(
          mc.font,
          Component.translatable("ui.physical_progression.training_complete"),
          x + 9,
          16,
          0xFFE6D39E);
      g.text(
          mc.font,
          Component.translatable("activity.physical_progression." + t.getStringOr("activity", "")),
          x + 9,
          29,
          0xFFE3DDCD);
      g.text(mc.font, String.format(Locale.ROOT, "+%.2f XP", xp), x + 9, 42, 0xFFB6CB9D);
      boolean adapted =
          t.getDoubleOr("maxAfter", 0.0) > t.getDoubleOr("maxBefore", 0.0)
              || t.keySet().stream()
                  .anyMatch(k -> k.endsWith("Growth") && t.getDoubleOr(k, 0.0) > 0);
      if (adapted)
        g.text(
            mc.font,
            Component.translatable("ui.physical_progression.adaptation"),
            x + 90,
            42,
            0xFFE6D39E);
      g.pose().popMatrix();
    }
  }
}
