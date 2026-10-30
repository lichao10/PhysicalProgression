package physical_progression.client.widget;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class TrainingIntensityBar {
  public static String status(
      double ratio, physical_progression.client.data.ClientConditioningData data) {
    return ratio <= data.minimumFatigue()
        ? "too_light"
        : ratio < 0.4
            ? "light"
            : ratio < data.optimalFatigue() ? "effective" : ratio < 0.9 ? "optimal" : "very_high";
  }

  public static void draw(
      GuiGraphics g,
      int x,
      int y,
      int width,
      double ratio,
      physical_progression.client.data.ClientConditioningData data) {
    double[] limits = {0, data.minimumFatigue(), 0.4, data.optimalFatigue(), 0.9, 1};
    int[] colors = {0xFF55534B, 0xFF8C875D, 0xFF7B9C60, 0xFF547A43, 0xFFC28D54};
    for (int i = 0; i < 5; i++)
      g.fill(
          x + (int) (width * limits[i]),
          y,
          x + (int) (width * limits[i + 1]) - 1,
          y + 6,
          colors[i]);
    int marker = x + (int) (Math.clamp(ratio, 0, 1) * (width - 1));
    g.fill(marker, y - 2, marker + 1, y + 8, 0xFFFFFFFF);
    var font = Minecraft.getInstance().font;
    g.drawString(
        font,
        Component.translatable("quality.physical_progression." + status(ratio, data)),
        x,
        y + 10,
        0xFFE6DECA);
    String percent = String.format(Locale.ROOT, "%.0f%%", ratio * 100);
    g.drawString(font, percent, x + width - font.width(percent), y + 10, 0xFFFFFFFF);
  }
}
