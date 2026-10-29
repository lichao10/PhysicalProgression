package physical_progression.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ConditioningClientConfig {
  public enum HudMode {
    ALWAYS_SHOW,
    CONTEXTUAL,
    MINIMAL,
    OFF
  }

  public enum HudAnchor {
    LEFT_CENTER,
    BOTTOM_LEFT
  }

  private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();
  public static final ModConfigSpec.EnumValue<HudMode> HUD_MODE =
      B.defineEnum("hudMode", HudMode.CONTEXTUAL);
  public static final ModConfigSpec.EnumValue<HudAnchor> HUD_ANCHOR =
      B.defineEnum("hudAnchor", HudAnchor.LEFT_CENTER);
  public static final ModConfigSpec.BooleanValue FEEDBACK = B.define("trainingFeedback", true);
  public static final ModConfigSpec.DoubleValue HUD_TEXT_SCALE =
      B.defineInRange("hudTextScale", 0.75, 0.5, 1.0);
  public static final ModConfigSpec.DoubleValue HUD_SCALE =
      B.defineInRange("hudScale", 1.0, 0.5, 1.5);
  public static final ModConfigSpec.DoubleValue TOAST_SCALE =
      B.defineInRange("trainingToastScale", 0.5, 0.25, 1.0);
  public static final ModConfigSpec.DoubleValue MENU_SCALE =
      B.defineInRange("bodyMenuScale", 1.0, 0.6, 1.0);
  public static final ModConfigSpec.BooleanValue SHOW_STAMINA = B.define("showStamina", true),
      SHOW_LEGS = B.define("showLegs", true),
      SHOW_ARMS = B.define("showArms", true),
      SHOW_LUNGS = B.define("showLungs", true);

  public static boolean visible(String key) {
    return switch (key) {
      case "stamina" -> SHOW_STAMINA.get();
      case "legs" -> SHOW_LEGS.get();
      case "left", "right" -> SHOW_ARMS.get();
      default -> SHOW_LUNGS.get();
    };
  }

  public static void reset() {
    HUD_MODE.set(HudMode.CONTEXTUAL);
    HUD_ANCHOR.set(HudAnchor.LEFT_CENTER);
    FEEDBACK.set(true);
    HUD_TEXT_SCALE.set(0.75);
    HUD_SCALE.set(1.0);
    TOAST_SCALE.set(0.5);
    MENU_SCALE.set(1.0);
    SHOW_STAMINA.set(true);
    SHOW_LEGS.set(true);
    SHOW_ARMS.set(true);
    SHOW_LUNGS.set(true);
    SPEC.save();
  }

  public static final ModConfigSpec SPEC = B.build();
}
