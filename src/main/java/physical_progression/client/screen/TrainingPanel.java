package physical_progression.client.screen;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import physical_progression.client.widget.TrainingIntensityBar;

public final class TrainingPanel {
  public static void current(PanelContext p) {
    p.heading("ui.physical_progression.current_session");
    var t = p.data.resources;
    p.row(
        "ui.physical_progression.activity",
        Component.translatable(
                "activity.physical_progression." + t.getStringOr("sessionActivity", ""))
            .getString(),
        null);
    p.row("ui.physical_progression.duration", duration(t.getIntOr("sessionTicks", 0)), null);
    if (t.contains("sessionState"))
      p.row(
          "ui.physical_progression.state",
          Component.translatable(
                  "session.physical_progression." + t.getStringOr("sessionState", ""))
              .getString(),
          null);
    p.row(
        "ui.physical_progression.distance",
        PanelContext.number(t.getDoubleOr("sessionDistance", 0.0)),
        null);
    p.row(
        "ui.physical_progression.used",
        PanelContext.number(t.getDoubleOr("sessionCost", 0.0)),
        null);
    if (t.getDoubleOr("climbDistance", 0.0) > 0)
      p.row(
          "ui.physical_progression.climb_distance",
          PanelContext.number(t.getDoubleOr("climbDistance", 0.0)),
          null);
    TrainingIntensityBar.draw(p.g, p.x, p.y, p.width, t.getDoubleOr("intensity", 0.0), p.data);
    p.y += 29;
  }

  public static String duration(int ticks) {
    int seconds = ticks / 20;
    return String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60);
  }

  public static String completedAt(net.minecraft.nbt.CompoundTag tag) {
    if (!tag.contains("finishedAt"))
      return Component.translatable("ui.physical_progression.unknown_time").getString();
    return java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm:ss")
        .format(
            java.time.Instant.ofEpochMilli(tag.getLongOr("finishedAt", 0L))
                .atZone(java.time.ZoneId.systemDefault()));
  }

  public static void details(PanelContext p) {
    details(p, p.data.attributes.getCompoundOrEmpty("lastSession"));
  }

  public static void details(PanelContext p, net.minecraft.nbt.CompoundTag t) {
    p.heading("ui.physical_progression.training_details");
    p.row("ui.physical_progression.record_number", "#" + t.getLongOr("id", 0L), null);
    p.row("ui.physical_progression.finished_at", completedAt(t), null);
    p.row(
        "ui.physical_progression.settlement_reason",
        Component.translatable(
                "reason.physical_progression."
                    + (t.contains("reason") ? t.getStringOr("reason", "") : "legacy"))
            .getString(),
        null);
    p.row(
        "ui.physical_progression.result",
        Component.translatable(
                "outcome.physical_progression."
                    + (t.contains("outcome") ? t.getStringOr("outcome", "") : "legacy"))
            .getString(),
        null);
    if (!t.contains("id")) {
      p.text("ui.physical_progression.no_session");
      return;
    }
    p.row(
        "ui.physical_progression.activity",
        Component.translatable("activity.physical_progression." + t.getStringOr("activity", ""))
            .getString(),
        null);
    p.row("ui.physical_progression.duration", duration(t.getIntOr("ticks", 0)), null);
    if (t.getDoubleOr("distance", 0.0) > 0)
      p.row(
          "ui.physical_progression.distance",
          PanelContext.number(t.getDoubleOr("distance", 0.0)),
          null);
    p.row(
        "ui.physical_progression.intensity",
        PanelContext.percent(t.getDoubleOr("intensity", 0.0)),
        null);
    if (t.contains("cost"))
      p.row("ui.physical_progression.used", PanelContext.number(t.getDoubleOr("cost", 0.0)), null);
    if (t.contains("peakFatigue"))
      p.row(
          "ui.physical_progression.peak_fatigue",
          PanelContext.percent(t.getDoubleOr("peakFatigue", 0.0)),
          null);
    if (t.getDoubleOr("recovered", 0.0) > 0)
      p.row(
          "ui.physical_progression.recovered_during",
          PanelContext.number(t.getDoubleOr("recovered", 0.0)),
          null);
    for (String side : new String[] {"left", "right"})
      if (t.contains(side + "Cost")) {
        p.row(
            "hud.physical_progression." + side,
            PanelContext.number(t.getDoubleOr(side + "Cost", 0.0))
                + " · "
                + PanelContext.percent(t.getDoubleOr(side + "Intensity", 0.0)),
            null);
        p.row(
            "ui.physical_progression.result",
            Component.translatable(
                    "outcome.physical_progression." + t.getStringOr(side + "Outcome", ""))
                .getString(),
            null);
      }
    if (t.contains("airUsed"))
      p.row(
          "ui.physical_progression.air_used",
          PanelContext.number(t.getDoubleOr("airUsed", 0.0) / 20) + " s",
          null);
    if (t.getDoubleOr("verticalDistance", 0.0) > 0)
      p.row(
          "ui.physical_progression.vertical_distance",
          PanelContext.number(t.getDoubleOr("verticalDistance", 0.0)),
          null);
    if (t.getDoubleOr("climbDistance", 0.0) > 0)
      p.row(
          "ui.physical_progression.climb_distance",
          PanelContext.number(t.getDoubleOr("climbDistance", 0.0)),
          null);
    for (String key :
        new String[] {
          "cardio",
          "leg_endurance",
          "leg_strength",
          "explosiveness",
          "lungs",
          "grip",
          "core",
          "mobility",
          "climbing",
          "recovery",
          "leftStrength",
          "leftEndurance",
          "rightStrength",
          "rightEndurance"
        }) {
      if (t.getDoubleOr(key + "Xp", 0.0) > 0.0001)
        p.row(
            "attribute.physical_progression." + key,
            "+" + PanelContext.number(t.getDoubleOr(key + "Xp", 0.0)) + " XP",
            "ui.physical_progression.stimulus_note");
    }
    boolean grew = false;
    for (String key : t.keySet())
      if (key.endsWith("Growth") && t.getDoubleOr(key, 0.0) > 0) grew = true;
    if (grew) {
      p.heading("ui.physical_progression.adaptation");
      for (String key :
          new String[] {
            "cardio",
            "leg_endurance",
            "leg_strength",
            "explosiveness",
            "lungs",
            "grip",
            "core",
            "mobility",
            "climbing",
            "recovery",
            "leftStrength",
            "leftEndurance",
            "rightStrength",
            "rightEndurance"
          })
        if (t.getDoubleOr(key + "Growth", 0.0) > 0)
          p.row(
              "attribute.physical_progression." + key,
              "+" + PanelContext.number(t.getDoubleOr(key + "Growth", 0.0)),
              null);
    }
    if (t.getDoubleOr("maxAfter", 0.0) > t.getDoubleOr("maxBefore", 0.0))
      p.row(
          "ui.physical_progression.adaptation",
          PanelContext.number(t.getDoubleOr("maxBefore", 0.0))
              + " → "
              + PanelContext.number(t.getDoubleOr("maxAfter", 0.0)),
          null);
  }

  public static void footer(PanelContext p) {
    var t = p.data.attributes.getCompoundOrEmpty("lastSession");
    var font = Minecraft.getInstance().font;
    p.heading("ui.physical_progression.training");
    var live = p.data.resources;
    String current =
        Component.translatable(
                    "activity.physical_progression." + live.getStringOr("sessionActivity", ""))
                .getString()
            + " · "
            + duration(live.getIntOr("sessionTicks", 0))
            + " · "
            + PanelContext.percent(live.getDoubleOr("intensity", 0.0))
            + " · "
            + Component.translatable(
                    live.getStringOr("sessionState", "").equals("rest")
                            || live.getStringOr("sessionState", "").equals("settled")
                            || live.getStringOr("sessionState", "").equals("invalid")
                        ? "session.physical_progression." + live.getStringOr("sessionState", "")
                        : "quality.physical_progression."
                            + TrainingIntensityBar.status(
                                live.getDoubleOr("intensity", 0.0), p.data))
                .getString();
    p.g.drawString(font, current, p.x, p.y, 0xFFE2DDCF);
    p.y += 13;
    if (!t.contains("id")) {
      p.g.drawString(
          font,
          Component.translatable("ui.physical_progression.stimulus_note"),
          p.x,
          p.y,
          0xFFBBB6A7);
      return;
    }
    double xp = 0;
    for (String key : t.keySet()) if (key.endsWith("Xp")) xp += t.getDoubleOr(key, 0.0);
    String last =
        Component.translatable("ui.physical_progression.last").getString()
            + ": "
            + Component.translatable(
                    "activity.physical_progression." + t.getStringOr("activity", ""))
                .getString()
            + " · +"
            + PanelContext.number(xp)
            + " XP";
    if (t.getDoubleOr("maxAfter", 0.0) > t.getDoubleOr("maxBefore", 0.0))
      last +=
          " · "
              + PanelContext.number(t.getDoubleOr("maxBefore", 0.0))
              + " → "
              + PanelContext.number(t.getDoubleOr("maxAfter", 0.0));
    p.g.drawString(font, last, p.x, p.y, 0xFFBBB6A7);
  }
}
