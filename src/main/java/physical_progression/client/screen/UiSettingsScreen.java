package physical_progression.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec;
import physical_progression.config.ConditioningClientConfig;

/** Local presentation preferences; saved immediately and independent of server gameplay. */
public final class UiSettingsScreen extends Screen {
  private final Screen parent;
  private float uiScale;

  public UiSettingsScreen(Screen parent) {
    super(Component.translatable("ui.physical_progression.settings"));
    this.parent = parent;
  }

  private String label(String key) {
    return Component.translatable("settings.physical_progression." + key).getString();
  }

  private void toggle(String key, ModConfigSpec.BooleanValue value, int x, int y, int w) {
    addRenderableWidget(
        Button.builder(
                Component.literal(label(key) + ": " + label(value.get() ? "on" : "off")),
                b -> {
                  value.set(!value.get());
                  ConditioningClientConfig.SPEC.save();
                  rebuildWidgets();
                })
            .bounds(x, y, w, 20)
            .build());
  }

  private void scale(
      String key,
      ModConfigSpec.DoubleValue preference,
      double min,
      double max,
      int x,
      int y,
      int width) {
    addRenderableWidget(
        new net.minecraft.client.gui.components.AbstractSliderButton(
            x, y, width, 20, Component.empty(), (preference.get() - min) / (max - min)) {
          {
            updateMessage();
          }

          protected void updateMessage() {
            setMessage(
                Component.literal(label(key) + ": " + Math.round(preference.get() * 100) + "%"));
          }

          protected void applyValue() {
            preference.set(
                Math.clamp(Math.round((min + value * (max - min)) * 100) / 100.0, min, max));
            ConditioningClientConfig.SPEC.save();
          }
        });
  }

  @Override
  protected void init() {
    uiScale = Math.min(1, Math.min(width / 340f, height / 280f));
    int w = 310, x = ((int) (width / uiScale) - w) / 2, y = 35;
    addRenderableWidget(
        Button.builder(
                Component.literal(
                    label("mode") + ": " + label(ConditioningClientConfig.HUD_MODE.get().name())),
                b -> {
                  var all = ConditioningClientConfig.HudMode.values();
                  ConditioningClientConfig.HUD_MODE.set(
                      all[(ConditioningClientConfig.HUD_MODE.get().ordinal() + 1) % all.length]);
                  ConditioningClientConfig.SPEC.save();
                  rebuildWidgets();
                })
            .bounds(x, y, w, 20)
            .build());
    y += 23;
    scale("hud_scale", ConditioningClientConfig.HUD_SCALE, 0.5, 1.5, x, y, w);
    y += 23;
    scale("text_scale", ConditioningClientConfig.HUD_TEXT_SCALE, 0.5, 1.0, x, y, w);
    y += 23;
    scale("toast_scale", ConditioningClientConfig.TOAST_SCALE, 0.25, 1.0, x, y, w);
    y += 23;
    scale("menu_scale", ConditioningClientConfig.MENU_SCALE, 0.6, 1.0, x, y, w);
    y += 23;
    int half = (w - 4) / 2;
    toggle("feedback", ConditioningClientConfig.FEEDBACK, x, y, half);
    addRenderableWidget(
        Button.builder(
                Component.literal(
                    label("anchor")
                        + ": "
                        + label(ConditioningClientConfig.HUD_ANCHOR.get().name())),
                b -> {
                  var anchors = ConditioningClientConfig.HudAnchor.values();
                  ConditioningClientConfig.HUD_ANCHOR.set(
                      anchors[
                          (ConditioningClientConfig.HUD_ANCHOR.get().ordinal() + 1)
                              % anchors.length]);
                  ConditioningClientConfig.SPEC.save();
                  rebuildWidgets();
                })
            .bounds(x + half + 4, y, half, 20)
            .build());
    y += 23;
    toggle("stamina", ConditioningClientConfig.SHOW_STAMINA, x, y, half);
    toggle("legs", ConditioningClientConfig.SHOW_LEGS, x + half + 4, y, half);
    y += 23;
    toggle("arms", ConditioningClientConfig.SHOW_ARMS, x, y, half);
    toggle("lungs", ConditioningClientConfig.SHOW_LUNGS, x + half + 4, y, half);
    y += 26;
    addRenderableWidget(
        Button.builder(
                Component.translatable("settings.physical_progression.reset"),
                b -> {
                  ConditioningClientConfig.reset();
                  rebuildWidgets();
                })
            .bounds(x, y, half, 20)
            .build());
    addRenderableWidget(
        Button.builder(
                Component.translatable("settings.physical_progression.preview"),
                b -> minecraft.gui.setScreen(new UiPreviewScreen(this)))
            .bounds(x + half + 4, y, half, 20)
            .build());
    y += 23;
    addRenderableWidget(
        Button.builder(Component.translatable("gui.done"), b -> onClose())
            .bounds(x, y, w, 20)
            .build());
  }

  @Override
  public void onClose() {
    minecraft.gui.setScreen(parent);
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }

  @Override
  public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float pt) {}

  @Override
  public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) {
    g.fill(0, 0, width, height, 0xBA101010);
    g.pose().pushMatrix();
    g.pose().scale(uiScale, uiScale);
    physical_progression.client.widget.VanillaTheme.frame(
        g, (int) (width / uiScale) / 2 - 164, 5, 328, 270);
    g.nextStratum();
    super.extractRenderState(g, (int) (mx / uiScale), (int) (my / uiScale), pt);
    g.centeredText(font, title, (int) (width / uiScale) / 2, 15, 0xFFFFD56A);
    g.pose().popMatrix();
  }

  private net.minecraft.client.input.MouseButtonEvent scaled(
      net.minecraft.client.input.MouseButtonEvent event) {
    return new net.minecraft.client.input.MouseButtonEvent(
        event.x() / uiScale, event.y() / uiScale, event.buttonInfo());
  }

  @Override
  public boolean mouseClicked(
      net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
    return super.mouseClicked(scaled(event), doubleClick);
  }

  @Override
  public boolean mouseDragged(
      net.minecraft.client.input.MouseButtonEvent event, double dx, double dy) {
    return super.mouseDragged(scaled(event), dx / uiScale, dy / uiScale);
  }

  @Override
  public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
    return super.mouseReleased(scaled(event));
  }
}
