package physical_progression.client.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class BodyPartWidget {
  public static boolean draw(
      GuiGraphicsExtractor g,
      int x,
      int y,
      int width,
      int height,
      double remaining,
      int mouseX,
      int mouseY) {
    g.fill(x, y, x + width, y + height, 0xFF161713);
    g.fill(x + 1, y + 1, x + width - 1, y + height - 1, ConditioningBar.color(remaining));
    // A filled stripe supplements color, so exhaustion remains readable without color perception.
    g.fill(x + 2, y + height - 4, x + width - 2, y + height - 2, 0xFF292A24);
    g.fill(
        x + 2,
        y + height - 4,
        x + 2 + (int) ((width - 4) * Math.clamp(remaining, 0, 1)),
        y + height - 2,
        0xFFF0E9D3);
    return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
  }
}
