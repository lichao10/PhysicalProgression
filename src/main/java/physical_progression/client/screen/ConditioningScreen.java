package physical_progression.client.screen;

import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import physical_progression.client.ConditioningClient;
import physical_progression.client.data.ClientConditioningData;
import physical_progression.network.ModNetworking.ClientSnapshot;
import physical_progression.network.RequestBodySnapshotPayload;

public class ConditioningScreen extends Screen {
  private static final int PANEL_WIDTH = 520, PANEL_HEIGHT = 340;
  private final Map<String, Button> navigation = new LinkedHashMap<>();
  private String page = "overview";
  private long selectedTraining = -1;
  private final TrainingHistoryPanel trainingHistory = new TrainingHistoryPanel();
  private float scale;
  private int left, top;
  private double scroll, maxScroll;

  public ConditioningScreen() {
    super(Component.translatable("ui.physical_progression.title"));
  }

  @Override
  protected void init() {
    scale =
        Math.min(
            physical_progression.config.ConditioningClientConfig.MENU_SCALE.get().floatValue(),
            Math.min(width / (float) PANEL_WIDTH, height / (float) PANEL_HEIGHT));
    left = ((int) (width / scale) - PANEL_WIDTH) / 2;
    top = ((int) (height / scale) - PANEL_HEIGHT) / 2;
    navigation.clear();
    int y = top + 51;
    for (String id :
        new String[] {
          "overview", "body", "legs", "left", "right", "cardio", "lungs", "climbing", "training"
        }) {
      Button nav =
          addRenderableWidget(
              Button.builder(
                      Component.translatable("ui.physical_progression." + id),
                      button -> {
                        showPage(id);
                      })
                  .bounds(left + 31, y, 69, 20)
                  .build());
      navigation.put(id, nav);
      y += 23;
    }
    addRenderableWidget(
        Button.builder(Component.literal("×"), button -> onClose())
            .bounds(left + 492, top + 7, 20, 20)
            .build());
    addRenderableWidget(
        Button.builder(
                Component.translatable("ui.physical_progression.settings"),
                b -> minecraft.setScreen(new UiSettingsScreen(this)))
            .bounds(left + 412, top + 7, 75, 20)
            .build());
    if (minecraft != null && minecraft.player != null)
      PacketDistributor.sendToServer(RequestBodySnapshotPayload.INSTANCE);
  }

  // Background is painted before our panels; the default Screen background would blur them again.
  @Override
  public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {}

  @Override
  public boolean isPauseScreen() {
    return false;
  }

  @Override
  public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    g.fill(0, 0, width, height, 0xAD10100F);
    g.pose().pushPose();
    g.pose().scale(scale, scale, 1);
    int mx = (int) (mouseX / scale), my = (int) (mouseY / scale);
    physical_progression.client.widget.VanillaTheme.frame(g, left, top, 520, 340);
    g.drawString(font, "PHYSICAL PROGRESSION", left + 14, top + 13, 0xFFFFD56A);
    g.drawString(font, title, left + 165, top + 13, 0xFFF4F4F4);
    g.fill(left + 13, top + 29, left + 400, top + 30, 0xFF555555);
    panel(g, left + 7, top + 37, 98, 229);
    panel(g, left + 110, top + 37, 106, 229);
    panel(g, left + 221, top + 37, 292, 229);
    panel(g, left + 7, top + 272, 506, 61);
    ClientConditioningData data = snapshot();
    var model = new PanelContext(g, data, left + 116, top + 46, 94, mx, my);
    var content = new PanelContext(g, data, left + 230, top + 47 - (int) scroll, 274, mx, my);
    var footer = new PanelContext(g, data, left + 16, top + 280, 488, mx, my);
    if (data.ready()) {
      BodyPanel.model(model);
      g.enableScissor(
          (int) ((left + 223) * scale),
          (int) ((top + 40) * scale),
          (int) ((left + 511) * scale),
          (int) ((top + 264) * scale));
      switch (page) {
        case "body" -> BodyPanel.details(content);
        case "legs" -> AttributePanel.legs(content);
        case "left", "right" -> AttributePanel.arm(content, page);
        case "cardio" -> AttributePanel.cardio(content);
        case "lungs" -> AttributePanel.lungs(content);
        case "climbing" -> AttributePanel.climbing(content);
        case "training" -> trainingHistory.render(content, selectedTraining);
        default -> OverviewPanel.render(content);
      }
      maxScroll = Math.max(0, content.y + scroll - (top + 258));
      scroll = Math.clamp(scroll, 0, maxScroll);
      if (maxScroll > 0) {
        int track = 220, thumb = Math.max(12, (int) (track * track / (track + maxScroll)));
        int thumbY = top + 42 + (int) ((track - thumb) * scroll / maxScroll);
        g.fill(left + 508, top + 42, left + 510, top + 262, 0xFF151515);
        g.fill(left + 508, thumbY, left + 510, thumbY + thumb, 0xFFC6C6C6);
      }
      g.disableScissor();
      TrainingPanel.footer(footer);
    } else
      g.drawCenteredString(
          font,
          Component.translatable("ui.physical_progression.waiting"),
          left + 365,
          top + 135,
          0xFFE3DFD2);
    for (var entry : navigation.entrySet()) {
      var button = entry.getValue();
      button.setMessage(
          Component.translatable("ui.physical_progression." + entry.getKey())
              .withStyle(
                  entry.getKey().equals(page)
                      ? net.minecraft.ChatFormatting.YELLOW
                      : net.minecraft.ChatFormatting.WHITE));
      if (entry.getKey().equals(page)) {
        g.fill(
            button.getX() - 2,
            button.getY() - 1,
            button.getX() + button.getWidth() + 2,
            button.getY() + 21,
            0xFFFFD56A);
      }
    }
    super.render(g, mx, my, partialTick);
    for (var entry : navigation.entrySet()) {
      int iconY = entry.getValue().getY();
      physical_progression.client.widget.VanillaTheme.inset(g, left + 10, iconY, 20, 20);
      net.minecraft.world.item.Item icon =
          switch (entry.getKey()) {
            case "body" -> net.minecraft.world.item.Items.LEATHER_CHESTPLATE;
            case "legs" -> net.minecraft.world.item.Items.LEATHER_BOOTS;
            case "left" -> net.minecraft.world.item.Items.SHIELD;
            case "right" -> net.minecraft.world.item.Items.IRON_AXE;
            case "cardio" -> net.minecraft.world.item.Items.APPLE;
            case "lungs" -> net.minecraft.world.item.Items.GLASS_BOTTLE;
            case "climbing" -> net.minecraft.world.item.Items.LADDER;
            case "training" -> net.minecraft.world.item.Items.WRITABLE_BOOK;
            default -> net.minecraft.world.item.Items.COMPASS;
          };
      g.renderItem(icon.getDefaultInstance(), left + 12, iconY + 2);
    }
    var tips = new ArrayList<Component>();
    tips.addAll(model.tooltip);
    if (my >= top + 40 && my < top + 264) tips.addAll(content.tooltip);
    g.pose().popPose();
    if (!tips.isEmpty()) {
      var lines = new ArrayList<net.minecraft.util.FormattedCharSequence>();
      for (var tip : tips) lines.addAll(font.split(tip, Math.min(230, width - 20)));
      g.renderTooltip(font, lines, mouseX, mouseY);
    }
  }

  protected ClientConditioningData snapshot() {
    return minecraft.player == null
        ? new ClientConditioningData()
        : minecraft.player.getData(ClientSnapshot.TYPE);
  }

  public void showPage(String id) {
    page = id;
    selectedTraining = -1;
    scroll = 0;
  }

  private static void panel(GuiGraphics g, int x, int y, int w, int h) {
    physical_progression.client.widget.VanillaTheme.inset(g, x, y, w, h);
  }

  @Override
  public boolean mouseClicked(double x, double y, int button) {
    if (button == 0
        && page.equals("training")
        && x / scale >= left + 223
        && x / scale < left + 511
        && y / scale >= top + 40
        && y / scale < top + 264) {
      Long entry = trainingHistory.click(x / scale, y / scale);
      if (entry != null) {
        selectedTraining = entry;
        scroll = 0;
        return true;
      }
    }
    return super.mouseClicked(x / scale, y / scale, button);
  }

  @Override
  public boolean mouseReleased(double x, double y, int button) {
    return super.mouseReleased(x / scale, y / scale, button);
  }

  @Override
  public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
    if (x / scale >= left + 221 && y / scale >= top + 37 && y / scale < top + 266) {
      scroll = Math.clamp(scroll - vertical * 14, 0, maxScroll);
      return true;
    }
    return super.mouseScrolled(x / scale, y / scale, horizontal, vertical);
  }

  @Override
  public boolean keyPressed(int key, int scan, int modifiers) {
    if (ConditioningClient.OPEN_SCREEN.matches(key, scan)) {
      onClose();
      return true;
    }
    return super.keyPressed(key, scan, modifiers);
  }
}
