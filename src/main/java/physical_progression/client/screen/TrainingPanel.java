package physical_progression.client.screen;

import physical_progression.client.widget.TrainingIntensityBar;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class TrainingPanel {
  public static void current(PanelContext p) {
    p.heading("ui.physical_progression.current_session");
    var t = p.data.resources;
    p.row(
        "ui.physical_progression.activity",
        Component.translatable("activity.physical_progression." + t.getString("sessionActivity"))
            .getString(),
        null);
    p.row("ui.physical_progression.duration", duration(t.getInt("sessionTicks")), null);
    if (t.contains("sessionState"))
      p.row(
          "ui.physical_progression.state",
          Component.translatable("session.physical_progression." + t.getString("sessionState")).getString(),
          null);
    p.row("ui.physical_progression.distance", PanelContext.number(t.getDouble("sessionDistance")), null);
    p.row("ui.physical_progression.used", PanelContext.number(t.getDouble("sessionCost")), null);
    if (t.getDouble("climbDistance") > 0)
      p.row(
          "ui.physical_progression.climb_distance",
          PanelContext.number(t.getDouble("climbDistance")),
          null);
    TrainingIntensityBar.draw(p.g, p.x, p.y, p.width, t.getDouble("intensity"), p.data);
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
            java.time.Instant.ofEpochMilli(tag.getLong("finishedAt"))
                .atZone(java.time.ZoneId.systemDefault()));
  }

  public static void details(PanelContext p) {
    details(p, p.data.attributes.getCompound("lastSession"));
  }

  public static void details(PanelContext p, net.minecraft.nbt.CompoundTag t) {
    p.heading("ui.physical_progression.training_details");
    p.row("ui.physical_progression.record_number", "#" + t.getLong("id"), null);
    p.row("ui.physical_progression.finished_at", completedAt(t), null);
    p.row(
        "ui.physical_progression.settlement_reason",
        Component.translatable(
                "reason.physical_progression." + (t.contains("reason") ? t.getString("reason") : "legacy"))
            .getString(),
        null);
    p.row(
        "ui.physical_progression.result",
        Component.translatable(
                "outcome.physical_progression."
                    + (t.contains("outcome") ? t.getString("outcome") : "legacy"))
            .getString(),
        null);
    if (!t.contains("id")) {
      p.text("ui.physical_progression.no_session");
      return;
    }
    p.row(
        "ui.physical_progression.activity",
        Component.translatable("activity.physical_progression." + t.getString("activity")).getString(),
        null);
    p.row("ui.physical_progression.duration", duration(t.getInt("ticks")), null);
    if (t.getDouble("distance") > 0)
      p.row("ui.physical_progression.distance", PanelContext.number(t.getDouble("distance")), null);
    p.row("ui.physical_progression.intensity", PanelContext.percent(t.getDouble("intensity")), null);
    if (t.contains("cost"))
      p.row("ui.physical_progression.used", PanelContext.number(t.getDouble("cost")), null);
    if (t.contains("peakFatigue"))
      p.row("ui.physical_progression.peak_fatigue", PanelContext.percent(t.getDouble("peakFatigue")), null);
    if (t.getDouble("recovered") > 0)
      p.row(
          "ui.physical_progression.recovered_during", PanelContext.number(t.getDouble("recovered")), null);
    for (String side : new String[] {"left", "right"})
      if (t.contains(side + "Cost")) {
        p.row(
            "hud.physical_progression." + side,
            PanelContext.number(t.getDouble(side + "Cost"))
                + " · "
                + PanelContext.percent(t.getDouble(side + "Intensity")),
            null);
        p.row(
            "ui.physical_progression.result",
            Component.translatable("outcome.physical_progression." + t.getString(side + "Outcome"))
                .getString(),
            null);
      }
    if (t.contains("airUsed"))
      p.row(
          "ui.physical_progression.air_used",
          PanelContext.number(t.getDouble("airUsed") / 20) + " s",
          null);
    if (t.getDouble("verticalDistance") > 0)
      p.row(
          "ui.physical_progression.vertical_distance",
          PanelContext.number(t.getDouble("verticalDistance")),
          null);
    if (t.getDouble("climbDistance") > 0)
      p.row(
          "ui.physical_progression.climb_distance",
          PanelContext.number(t.getDouble("climbDistance")),
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
      if (t.getDouble(key + "Xp") > 0.0001)
        p.row(
            "attribute.physical_progression." + key,
            "+" + PanelContext.number(t.getDouble(key + "Xp")) + " XP",
            "ui.physical_progression.stimulus_note");
    }
    boolean grew = false;
    for (String key : t.getAllKeys())
      if (key.endsWith("Growth") && t.getDouble(key) > 0) grew = true;
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
        if (t.getDouble(key + "Growth") > 0)
          p.row(
              "attribute.physical_progression." + key,
              "+" + PanelContext.number(t.getDouble(key + "Growth")),
              null);
    }
    if (t.getDouble("maxAfter") > t.getDouble("maxBefore"))
      p.row(
          "ui.physical_progression.adaptation",
          PanelContext.number(t.getDouble("maxBefore"))
              + " → "
              + PanelContext.number(t.getDouble("maxAfter")),
          null);
  }

  public static void footer(PanelContext p) {
    var t = p.data.attributes.getCompound("lastSession");
    var font = Minecraft.getInstance().font;
    p.heading("ui.physical_progression.training");
    var live = p.data.resources;
    String current =
        Component.translatable("activity.physical_progression." + live.getString("sessionActivity"))
                .getString()
            + " · "
            + duration(live.getInt("sessionTicks"))
            + " · "
            + PanelContext.percent(live.getDouble("intensity"))
            + " · "
            + Component.translatable(
                    live.getString("sessionState").equals("rest")
                            || live.getString("sessionState").equals("settled")
                            || live.getString("sessionState").equals("invalid")
                        ? "session.physical_progression." + live.getString("sessionState")
                        : "quality.physical_progression."
                            + TrainingIntensityBar.status(live.getDouble("intensity"), p.data))
                .getString();
    p.g.drawString(font, current, p.x, p.y, 0xFFE2DDCF);
    p.y += 13;
    if (!t.contains("id")) {
      p.g.drawString(
          font, Component.translatable("ui.physical_progression.stimulus_note"), p.x, p.y, 0xFFBBB6A7);
      return;
    }
    double xp = 0;
    for (String key : t.getAllKeys()) if (key.endsWith("Xp")) xp += t.getDouble(key);
    String last =
        Component.translatable("ui.physical_progression.last").getString()
            + ": "
            + Component.translatable("activity.physical_progression." + t.getString("activity")).getString()
            + " · +"
            + PanelContext.number(xp)
            + " XP";
    if (t.getDouble("maxAfter") > t.getDouble("maxBefore"))
      last +=
          " · "
              + PanelContext.number(t.getDouble("maxBefore"))
              + " → "
              + PanelContext.number(t.getDouble("maxAfter"));
    p.g.drawString(font, last, p.x, p.y, 0xFFBBB6A7);
  }
}
