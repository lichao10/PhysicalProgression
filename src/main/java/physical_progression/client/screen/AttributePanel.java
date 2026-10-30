package physical_progression.client.screen;

import net.minecraft.network.chat.Component;
import physical_progression.client.widget.ConditioningBar;

public final class AttributePanel {
  public static void legs(PanelContext p) {
    p.heading("ui.physical_progression.legs");
    capacity(p, "legs");
    for (String key : new String[] {"leg_strength", "leg_endurance", "explosiveness", "mobility"})
      p.attr(key);
    p.heading("ui.physical_progression.performance");
    p.row(
        "ui.physical_progression.sprint_cost",
        PanelContext.percent(p.data.resource("sprintCost")),
        "tooltip.physical_progression.cost_ratio");
    p.row(
        "ui.physical_progression.walk_cost",
        PanelContext.percent(p.data.resource("walkCost")),
        "tooltip.physical_progression.cost_ratio");
    p.row("ui.physical_progression.jump", PanelContext.number(p.data.resource("jump")) + "x", null);
    p.row(
        "ui.physical_progression.fatigue_penalty",
        PanelContext.percent(p.data.resource("fatiguePenalty")),
        null);
    p.row(
        "ui.physical_progression.load_penalty",
        PanelContext.percent(p.data.resource("loadPenalty")),
        "tooltip.physical_progression.equipment");
    p.row(
        "ui.physical_progression.burst", PanelContext.percent(p.data.resource("burstBonus")), null);
  }

  public static void arm(PanelContext p, String side) {
    p.heading("hud.physical_progression." + side);
    capacity(p, side);
    p.attr(side + "Strength");
    p.attr(side + "Endurance");
    p.attr("grip");
    p.attr("core");
    String name =
        p.data.resources.contains(side + "CustomName")
            ? p.data.resources.getStringOr(side + "CustomName", "")
            : Component.translatable(p.data.resources.getStringOr(side + "Item", "")).getString();
    p.row("ui.physical_progression.item", name, "tooltip.physical_progression.action_cost");
    p.row(
        "ui.physical_progression.weight",
        PanelContext.number(p.data.resource(side + "Weight")),
        null);
    p.row(
        "ui.physical_progression.action_cost",
        PanelContext.number(p.data.resource(side + "Cost")),
        "tooltip.physical_progression.action_cost");
    p.row(
        "ui.physical_progression.fatigue_penalty",
        PanelContext.percent(p.data.resource(side + "Penalty")),
        null);
    physical_progression.client.widget.TrainingIntensityBar.draw(
        p.g, p.x, p.y, p.width, p.data.resource(side + "Intensity"), p.data);
    p.y += 25;
  }

  public static void cardio(PanelContext p) {
    p.heading("ui.physical_progression.cardio");
    p.attr("cardio");
    p.row(
        "ui.physical_progression.capacity_contribution",
        "+" + PanelContext.number(p.data.attribute("cardioContribution")),
        null);
    p.row(
        "ui.physical_progression.recovery_contribution",
        "+" + PanelContext.percent(p.data.attribute("cardioRecovery")),
        null);
    p.row(
        "ui.physical_progression.efficiency",
        "+" + PanelContext.percent(p.data.attribute("sustainedEfficiency")),
        null);
    p.y += 4;
    TrainingPanel.current(p);
  }

  public static void lungs(PanelContext p) {
    p.heading("ui.physical_progression.lungs");
    p.attr("lungs");
    p.row("ui.physical_progression.base_breath", "15.0 s", null);
    p.row(
        "ui.physical_progression.breath_duration",
        PanelContext.number(p.data.resources.getIntOr("airMax", 0) / 20.0) + " s",
        "tooltip.physical_progression.breath");
    p.row(
        "ui.physical_progression.recovery_contribution",
        "+" + PanelContext.percent(p.data.attribute("lungsRecovery")),
        null);
    p.row(
        "ui.physical_progression.efficiency",
        "+" + PanelContext.percent(p.data.attribute("lungsEfficiency")),
        null);
    ConditioningBar.draw(
        p.g,
        "hud.physical_progression.breath",
        p.x,
        p.y,
        p.width,
        p.data.resources.getIntOr("air", 0) / 20.0,
        p.data.resources.getIntOr("airMax", 0) / 20.0);
    p.y += 30;
    p.row(
        "ui.physical_progression.lung_stimulus",
        Component.translatable(
                p.data.resources.getBooleanOr("lungTraining", false)
                    ? "ui.physical_progression.active"
                    : "ui.physical_progression.inactive")
            .getString(),
        "tooltip.physical_progression.lungs");
    p.text("ui.physical_progression.dive_note");
  }

  public static void climbing(PanelContext p) {
    p.heading("ui.physical_progression.climbing");
    p.attr("climbing");
    p.attr("grip");
    p.attr("core");
    p.row(
        "ui.physical_progression.climbing_speed",
        PanelContext.number(p.data.attribute("climbingSpeed")) + " m/s",
        null);
    p.row(
        "ui.physical_progression.holding_cost",
        PanelContext.number(p.data.attribute("climbingHoldCost")) + " /s",
        null);
    p.row(
        "ui.physical_progression.holding_arm_cost",
        PanelContext.number(p.data.attribute("climbingArmHoldCost")) + " /s",
        null);
    p.row(
        "ui.physical_progression.climbing_cost",
        PanelContext.number(p.data.attribute("climbingMoveCost")),
        null);
    p.row(
        "ui.physical_progression.progress",
        PanelContext.number(p.data.attribute("climbingXp"))
            + " / "
            + PanelContext.number(p.data.attribute("climbingRequired"))
            + " XP",
        null);
    p.text("tooltip.physical_progression.climbing");
  }

  private static void capacity(PanelContext p, String key) {
    ConditioningBar.draw(
        p.g,
        "ui.physical_progression.current_capacity",
        p.x,
        p.y,
        p.width,
        p.data.resource(key),
        p.data.resource(key + "Max"));
    p.y += 29;
  }
}
