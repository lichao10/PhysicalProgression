package physical_progression.client.screen;

import physical_progression.client.data.ClientConditioningData;
import physical_progression.client.hud.ConditioningHud;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Explicit sample values let players inspect presentation without affecting gameplay. */
public final class UiPreviewScreen extends Screen {
  private final Screen parent;
  private final ClientConditioningData data = new ClientConditioningData();

  public UiPreviewScreen(Screen parent) {
    super(Component.translatable("settings.physical_progression.preview_note"));
    this.parent = parent;
    for (String key : new String[] {"stamina", "legs", "left", "right"}) {
      double max = key.equals("stamina") ? 50 : key.equals("legs") ? 80 : 30;
      data.resources.putDouble(key, max * 0.65);
      data.resources.putDouble(key + "Max", max);
    }
    data.resources.putString("activity", "climbing");
    data.resources.putBoolean("underwater", true);
    data.resources.putInt("air", 200);
    data.resources.putInt("airMax", 300);
    var result = new net.minecraft.nbt.CompoundTag();
    result.putLong("id", 1);
    result.putString("activity", "climbing");
    result.putDouble("climbingXp", 18.5);
    data.attributes.put("lastSession", result);
  }

  @Override
  protected void init() {
    addRenderableWidget(
        Button.builder(Component.translatable("gui.back"), b -> onClose())
            .bounds(width / 2 - 50, height - 27, 100, 20)
            .build());
  }

  @Override
  public void onClose() {
    minecraft.setScreen(parent);
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }

  @Override
  public void renderBackground(GuiGraphics g, int mx, int my, float pt) {}

  @Override
  public void render(GuiGraphics g, int mx, int my, float pt) {
    g.fill(0, 0, width, height, 0xFF303C43);
    ConditioningHud.renderSnapshot(g, data, true);
    super.render(g, mx, my, pt);
    g.drawCenteredString(font, title, width / 2, height - 44, 0xFFEAE3CC);
  }
}
