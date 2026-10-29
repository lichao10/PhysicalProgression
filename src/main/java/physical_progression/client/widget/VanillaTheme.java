package physical_progression.client.widget;

import net.minecraft.client.gui.GuiGraphics;

/** Square inventory-style surfaces shared by the menu, history and feedback. */
public final class VanillaTheme {
  private VanillaTheme() {}

  public static void frame(GuiGraphics g, int x, int y, int w, int h) {
    g.fill(x, y, x + w, y + h, 0xFF101010);
    g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF555555);
    g.fill(x + 2, y + 2, x + w - 2, y + h - 2, 0xFFC6C6C6);
    g.fill(x + 2, y + 2, x + w - 2, y + 4, 0xFFFFFFFF);
    g.fill(x + 2, y + 4, x + 4, y + h - 2, 0xFFE4E4E4);
    g.fill(x + w - 4, y + 4, x + w - 2, y + h - 2, 0xFF777777);
    g.fill(x + 4, y + h - 4, x + w - 4, y + h - 2, 0xFF777777);
    inset(g, x + 6, y + 6, w - 12, h - 12);
  }

  public static void inset(GuiGraphics g, int x, int y, int w, int h) {
    g.fill(x, y, x + w, y + h, 0xFF111111);
    g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF292929);
    g.fill(x + 1, y + h - 1, x + w, y + h, 0xFF737373);
    g.fill(x + w - 1, y + 1, x + w, y + h, 0xFF737373);
  }
}
