package physical_progression.client.screen;

public final class OverviewPanel {
  public static void render(PanelContext p) {
    p.heading("ui.physical_progression.attributes");
    group(p, "ui.physical_progression.cardio", "cardio", "lungs", "recovery");
    group(p, "ui.physical_progression.legs", "leg_strength", "leg_endurance", "explosiveness");
    group(p, "ui.physical_progression.climbing", "climbing", "grip", "core", "mobility");
    group(p, "ui.physical_progression.left", "leftStrength", "leftEndurance");
    group(p, "ui.physical_progression.right", "rightStrength", "rightEndurance");
  }

  private static void group(PanelContext p, String heading, String... attributes) {
    p.heading(heading);
    for (String attribute : attributes) p.attr(attribute);
    p.y += 7;
  }
}
