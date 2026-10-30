package physical_progression.client.screen;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import physical_progression.client.data.ClientConditioningData;

public final class PanelContext {
  public final GuiGraphicsExtractor g;
  public final ClientConditioningData data;
  public final int x, width, mouseX, mouseY;
  public int y;
  public final List<Component> tooltip = new ArrayList<>();

  public PanelContext(
      GuiGraphicsExtractor g,
      ClientConditioningData data,
      int x,
      int y,
      int width,
      int mx,
      int my) {
    this.g = g;
    this.data = data;
    this.x = x;
    this.y = y;
    this.width = width;
    mouseX = mx;
    mouseY = my;
  }

  public static String number(double value) {
    return String.format(Locale.ROOT, "%.2f", value);
  }

  public static String percent(double value) {
    return String.format(Locale.ROOT, "%.1f%%", value * 100);
  }

  public void heading(String key) {
    g.text(Minecraft.getInstance().font, Component.translatable(key), x, y, 0xFFFFD56A);
    g.fill(x, y + 12, x + width, y + 13, 0xFF555555);
    y += 20;
  }

  public void row(String key, String value, String tip) {
    var font = Minecraft.getInstance().font;
    String label = Component.translatable(key).getString();
    String display = font.plainSubstrByWidth(value, (int) (width * 0.55));
    int valueWidth = font.width(display);
    g.text(
        font,
        font.plainSubstrByWidth(label, Math.max(25, width - valueWidth - 9)),
        x,
        y,
        0xFFDDDDDD);
    g.text(font, display, x + width - valueWidth, y, 0xFFFFFFFF);
    if (mouseX >= x && mouseX < x + width && mouseY >= y - 1 && mouseY < y + 12) {
      g.fill(x, y + 10, x + width, y + 11, 0xFF8E835F);
      tooltip.add(Component.translatable(key));
      if (!display.equals(value)) tooltip.add(Component.literal(value));
      if (tip != null) tooltip.add(Component.translatable(tip));
    }
    y += 14;
  }

  public void attr(String key) {
    row(
        "attribute.physical_progression." + key,
        number(data.attribute(key)),
        "tooltip.physical_progression." + key);
    if (mouseX >= x && mouseX < x + width && mouseY >= y - 15 && mouseY < y - 2) {
      tooltip.add(Component.literal(String.format(Locale.ROOT, "%.4f", data.attribute(key))));
      if (data.attribute(key + "Required") > 0)
        tooltip.add(
            Component.literal(
                number(data.attribute(key + "Xp"))
                    + " / "
                    + number(data.attribute(key + "Required"))
                    + " XP"));
    }
  }

  public void text(String key) {
    g.textWithWordWrap(
        Minecraft.getInstance().font, Component.translatable(key), x, y, width, 0xFFBBBBBB);
    y += 24;
  }
}
