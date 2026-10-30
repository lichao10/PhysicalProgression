package physical_progression.gametest.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.nbt.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import physical_progression.client.data.ClientConditioningData;
import physical_progression.client.screen.ConditioningScreen;

/** Opt-in rendering regression harness, excluded from the published JAR. */
@EventBusSubscriber(modid = "physical_progression", value = Dist.CLIENT)
public final class UiRenderTest {
  private static final String[] PAGES = {
    "overview",
    "body",
    "legs",
    "left",
    "right",
    "cardio",
    "lungs",
    "training",
    "details",
    "history-back",
    "overview",
    "legs",
    "settings",
    "settings-return",
    "hud",
    "climbing",
    "preview",
    "hud-bottom-left"
  };
  private static ConditioningScreen screen;
  private static int frames, stage, oldScale;
  private static String oldLanguage;
  private static physical_progression.config.ConditioningClientConfig.HudAnchor oldAnchor;
  private static boolean finished;

  private static boolean click(
      net.minecraft.client.gui.screens.Screen screen, double x, double y, int button) {
    return screen.mouseClicked(
        new net.minecraft.client.input.MouseButtonEvent(
            x, y, new net.minecraft.client.input.MouseButtonInfo(button, 0)),
        false);
  }

  private static boolean release(
      net.minecraft.client.gui.screens.Screen screen, double x, double y, int button) {
    return screen.mouseReleased(
        new net.minecraft.client.input.MouseButtonEvent(
            x, y, new net.minecraft.client.input.MouseButtonInfo(button, 0)));
  }

  @SubscribeEvent
  public static void start(ClientTickEvent.Post event) {
    if (!Boolean.getBoolean("physical_progression.uiQa") || screen != null || finished) return;
    var mc = Minecraft.getInstance();
    if (mc.screen == null || mc.getOverlay() != null) return;
    try {
      var file = java.nio.file.Path.of("../build/ui-qa/trained-snapshot.nbt");
      var tag = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
      var mirror = new ClientConditioningData();
      mirror.resources = tag.getCompoundOrEmpty("resources");
      mirror.attributes = tag.getCompoundOrEmpty("attributes");
      oldScale = mc.options.guiScale().get();
      oldLanguage = mc.getLanguageManager().getSelected();
      oldAnchor = physical_progression.config.ConditioningClientConfig.HUD_ANCHOR.get();
      screen =
          new ConditioningScreen() {
            @Override
            public void render(net.minecraft.client.gui.GuiGraphics g, int mx, int my, float pt) {
              if (stage == 14 || stage == 17) {
                g.fill(0, 0, width, height, 0xFF303C43);
                physical_progression.client.hud.ConditioningHud.renderSnapshot(g, mirror, true);
              } else super.render(g, mx, my, pt);
            }

            @Override
            protected ClientConditioningData snapshot() {
              return mirror;
            }
          };
      mc.setScreen(screen);
    } catch (Exception exception) {
      throw new IllegalStateException("UI QA requires runGameTestServer snapshot", exception);
    }
  }

  @SubscribeEvent
  public static void capture(RenderFrameEvent.Post event) {
    if (screen == null || finished) return;
    var mc = Minecraft.getInstance();
    if (mc.getOverlay() != null
        || (mc.screen != screen
            && !(mc.screen instanceof physical_progression.client.screen.UiSettingsScreen)
            && !(mc.screen instanceof physical_progression.client.screen.UiPreviewScreen))
        || ++frames < (stage == 10 ? 120 : 45)) return;
    frames = 0;
    boolean finalCapture = stage == PAGES.length - 1;
    Screenshot.grab(
        mc.gameDirectory,
        "physical_progression-qa-" + stage + "-" + PAGES[stage] + ".png",
        mc.getMainRenderTarget(),
        1,
        message -> {
          com.mojang.logging.LogUtils.getLogger().info("UI_QA {}", message.getString());
          if (finalCapture) mc.execute(mc::stop);
        });
    stage++;
    if (stage == PAGES.length) {
      finished = true;
      physical_progression.config.ConditioningClientConfig.HUD_ANCHOR.set(oldAnchor);
      physical_progression.config.ConditioningClientConfig.SPEC.save();
      mc.options.guiScale().set(oldScale);
      mc.getLanguageManager().setSelected(oldLanguage);
      mc.options.languageCode = oldLanguage;
      mc.options.save();
      return;
    }
    if (stage == 10) {
      mc.options.guiScale().set(4);
      mc.resizeDisplay();
      mc.getLanguageManager().setSelected("zh_cn");
      mc.options.languageCode = "zh_cn";
      mc.reloadResourcePacks();
    }
    if (stage == 12) {
      float scale =
          Math.min(
              physical_progression.config.ConditioningClientConfig.MENU_SCALE.get().floatValue(),
              Math.min(screen.width / 520f, screen.height / 340f));
      int left = ((int) (screen.width / scale) - 520) / 2,
          top = ((int) (screen.height / scale) - 340) / 2;
      click(screen, (left + 445) * scale, (top + 17) * scale, 0);
      if (!(mc.screen instanceof physical_progression.client.screen.UiSettingsScreen))
        throw new IllegalStateException("Settings entry failed");
      return;
    }
    if (stage == 13) {
      var settings = mc.screen;
      var oldMode = physical_progression.config.ConditioningClientConfig.HUD_MODE.get();
      float settingScale = Math.min(1, Math.min(settings.width / 340f, settings.height / 280f));
      click(settings, settings.width / 2.0, 44 * settingScale, 0);
      if (physical_progression.config.ConditioningClientConfig.HUD_MODE.get() == oldMode)
        throw new IllegalStateException("Settings did not apply");
      for (int i = 0; i < 3; i++) click(settings, settings.width / 2.0, 44 * settingScale, 0);
      if (physical_progression.config.ConditioningClientConfig.HUD_MODE.get() != oldMode)
        throw new IllegalStateException("Settings cycle did not restore preference");
      double oldText = physical_progression.config.ConditioningClientConfig.HUD_TEXT_SCALE.get();
      click(settings, settings.width / 2.0, 91 * settingScale, 0);
      release(settings, settings.width / 2.0, 91 * settingScale, 0);
      click(settings, settings.width / 2.0 + 100 * settingScale, 91 * settingScale, 0);
      release(settings, settings.width / 2.0 + 100 * settingScale, 91 * settingScale, 0);
      if (physical_progression.config.ConditioningClientConfig.HUD_TEXT_SCALE.get() <= 0.8)
        throw new IllegalStateException("Slider did not map screen coordinates");
      physical_progression.config.ConditioningClientConfig.HUD_TEXT_SCALE.set(oldText);
      physical_progression.config.ConditioningClientConfig.SPEC.save();
      mc.screen.onClose();
      if (mc.screen != screen) throw new IllegalStateException("Settings return failed");
      return;
    }
    if (stage == 17) {
      physical_progression.config.ConditioningClientConfig.HUD_ANCHOR.set(
          physical_progression.config.ConditioningClientConfig.HudAnchor.BOTTOM_LEFT);
      mc.setScreen(screen);
      return;
    }
    if (stage == 16) {
      mc.setScreen(new physical_progression.client.screen.UiSettingsScreen(screen));
      float scale = Math.min(1, Math.min(mc.screen.width / 340f, mc.screen.height / 280f));
      click(mc.screen, mc.screen.width / 2.0 + 70 * scale, 232 * scale, 0);
      if (!(mc.screen instanceof physical_progression.client.screen.UiPreviewScreen))
        throw new IllegalStateException("Preview entry failed");
      return;
    }
    if (stage == 8 || stage == 9) {
      float scale = Math.min(1, Math.min(screen.width / 520f, screen.height / 340f));
      int left = ((int) (screen.width / scale) - 520) / 2,
          top = ((int) (screen.height / scale) - 340) / 2;
      if (!click(screen, (left + 250) * scale, (top + (stage == 8 ? 72 : 51)) * scale, 0))
        throw new IllegalStateException("Training history click failed");
    } else screen.showPage(PAGES[stage]);
  }
}
