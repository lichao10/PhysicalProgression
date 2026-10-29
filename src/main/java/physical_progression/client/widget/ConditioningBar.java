package physical_progression.client.widget;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class ConditioningBar {
  public static int color(double fraction) {
    return fraction > 0.5
        ? 0xFF80B83F
        : fraction > 0.2 ? 0xFFCEBA68 : fraction > 0.05 ? 0xFFD08B50 : 0xFFBB5555;
  }

  public static void draw(
      GuiGraphics g, String label, int x, int y, int width, double current, double maximum) {
    var font = Minecraft.getInstance().font;
    double ratio = Math.clamp(current / Math.max(1, maximum), 0, 1);
    g.drawString(font, Component.translatable(label), x, y, 0xFFEAEAEA);
    g.fill(x, y + 11, x + width, y + 22, 0xFF101010);
    g.fill(x + 1, y + 12, x + width - 1, y + 21, 0xFF484848);
    g.fill(x + 1, y + 12, x + 1 + (int) ((width - 2) * ratio), y + 21, color(ratio));
    int filled = (int) ((width - 2) * ratio);
    if (filled > 0) g.fill(x + 1, y + 12, x + 1 + filled, y + 13, 0xFFB4D68A);
    String value =
        String.format(
            Locale.ROOT,
            label.endsWith(".breath") ? "%.1f / %.1f" : "%.0f / %.0f",
            current,
            maximum);
    g.drawString(font, value, x + (width - font.width(value)) / 2, y + 13, 0xFFFFFFFF);
  }

  /** Compact HUD typography; full-size body-screen bars keep their existing layout. */
  public static void compact(
      GuiGraphics g, String label, int x, int y, int width, double current, double maximum) {
    float scale = physical_progression.config.ConditioningClientConfig.HUD_TEXT_SCALE.get().floatValue();
    double ratio = Math.clamp(current / Math.max(1, maximum), 0, 1);
    smallText(g, Component.translatable(label).getString(), x, y, scale, 0xFFEAEAEA);
    g.fill(x, y + 9, x + width, y + 18, 0xFF101010);
    g.fill(x + 1, y + 10, x + width - 1, y + 17, 0xFF484848);
    g.fill(x + 1, y + 10, x + 1 + (int) ((width - 2) * ratio), y + 17, color(ratio));
    int filled = (int) ((width - 2) * ratio);
    if (filled > 0) g.fill(x + 1, y + 10, x + 1 + filled, y + 11, 0xFFB4D68A);
    String value =
        String.format(
            Locale.ROOT,
            label.endsWith(".breath") ? "%.1f / %.1f" : "%.0f / %.0f",
            current,
            maximum);
    smallText(
        g,
        value,
        x + (width - Minecraft.getInstance().font.width(value) * scale) / 2,
        y + 10,
        scale,
        0xFFFFFFFF);
  }

  private static void smallText(
      GuiGraphics g, String text, float x, float y, float scale, int color) {
    g.pose().pushPose();
    g.pose().translate(x, y, 0);
    g.pose().scale(scale, scale, 1);
    g.drawString(Minecraft.getInstance().font, text, 0, 0, color);
    g.pose().popPose();
  }
}
