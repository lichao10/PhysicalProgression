package physical_progression.body;

import static physical_progression.body.BodyAbility.*;

import net.minecraft.server.level.ServerPlayer;
import physical_progression.arm.ItemWeightManager;
import physical_progression.attachment.*;
import physical_progression.config.ConditioningConfig;

/** The same server-computed values drive gameplay and screen explanations. */
public final class DerivedPerformance {
  private static double gain(BodyState b, BodyAbility a) {
    return Math.max(0, b.value(a) - 1);
  }

  public static double fraction(Capacity c) {
    return Math.clamp(c.current / Math.max(1, c.maximum), 0, 1);
  }

  public static double cardioCapacity(BodyState b) {
    return 20 * gain(b, CARDIO);
  }

  public static double enduranceCapacity(BodyState b) {
    return 15 * gain(b, LEG_ENDURANCE);
  }

  public static void refresh(PlayerConditioningData d) {
    var b = d.body;
    d.stamina.maximum =
        Math.min(
            Math.max(b.baseStamina, ConditioningConfig.CAP.get()),
            b.baseStamina + cardioCapacity(b) + enduranceCapacity(b));
    b.legs.maximum =
        Math.min(
            Math.max(b.baseLegs, ConditioningConfig.CAP.get()),
            b.baseLegs + 30 * gain(b, LEG_ENDURANCE) + 10 * gain(b, LEG_STRENGTH));
    d.stamina.current = Math.min(d.stamina.current, d.stamina.maximum);
    b.legs.current = Math.min(b.legs.current, b.legs.maximum);
  }

  public static double recoveryMultiplier(BodyState b) {
    return ConditioningConfig.RECOVERY_SPEED.get()
        * Math.min(3, 1 + 0.3 * gain(b, RECOVERY) + 0.15 * gain(b, CARDIO) + 0.05 * gain(b, LUNGS));
  }

  public static int recoveryDelay(BodyState b) {
    return (int)
        Math.ceil(
            ConditioningConfig.RECOVERY_DELAY.get()
                / Math.min(2, 1 + 0.2 * gain(b, CARDIO) + 0.1 * gain(b, LUNGS)));
  }

  public static double cardioEfficiency(BodyState b) {
    return Math.max(0.75, 1 / (1 + 0.08 * gain(b, CARDIO) + 0.03 * gain(b, LUNGS)));
  }

  public static double movementCost(PlayerConditioningData d, boolean sprint) {
    return Math.max(
            0.4,
            cardioEfficiency(d.body) / (1 + (sprint ? 0.12 : 0.18) * gain(d.body, LEG_ENDURANCE)))
        * loadPenalty(d.body);
  }

  public static double loadPenalty(BodyState b) {
    return 1
        + Math.min(
            0.3,
            Math.max(0, b.equipmentWeight - ConditioningConfig.EQUIPMENT_ALLOWANCE.get())
                / (40 * b.value(CORE) + 20 * b.value(LEG_STRENGTH)));
  }

  public static double fatigueMultiplier(PlayerConditioningData d) {
    return 0.9 + 0.1 * Math.min(fraction(d.stamina), fraction(d.body.legs));
  }

  public static double jumpMultiplier(PlayerConditioningData d) {
    var b = d.body;
    double ability =
        1
            + Math.min(
                ConditioningConfig.MAX_JUMP_BONUS.get(),
                0.06 * gain(b, LEG_STRENGTH)
                    + 0.05 * gain(b, EXPLOSIVENESS)
                    + 0.02 * gain(b, MOBILITY)
                    + 0.02 * gain(b, CORE));
    return ability * fatigueMultiplier(d) / Math.sqrt(loadPenalty(b));
  }

  public static double burstBonus(BodyState b) {
    return Math.min(
        ConditioningConfig.MAX_BURST.get(),
        0.035 * gain(b, EXPLOSIVENESS) + 0.015 * gain(b, LEG_STRENGTH));
  }

  public static int burstDuration(BodyState b) {
    return 30 + (int) Math.min(30, 10 * gain(b, EXPLOSIVENESS));
  }

  public static int breathBonus(BodyState b) {
    return (int) (300 * Math.min(ConditioningConfig.MAX_BREATH_BONUS.get(), 0.3 * gain(b, LUNGS)));
  }

  public static double armFatigue(PlayerConditioningData.Arm arm) {
    return 1 + 0.25 * (1 - fraction(arm.endurance));
  }

  public static double armCostMultiplier(PlayerConditioningData d, PlayerConditioningData.Arm arm) {
    return Math.max(
        ConditioningConfig.MIN_COST.get(),
        (1 / arm.strength)
            / (1 + Math.min(0.3, 0.1 * gain(d.body, GRIP) + 0.05 * gain(d.body, CORE)))
            * armFatigue(arm));
  }

  public static void equipment(ServerPlayer p, BodyState b) {
    double weight =
        (p.getMainHandItem().isEmpty() ? 0 : ItemWeightManager.weight(p.getMainHandItem()))
            + (p.getOffhandItem().isEmpty() ? 0 : ItemWeightManager.weight(p.getOffhandItem()));
    for (var slot :
        new net.minecraft.world.entity.EquipmentSlot[] {
          net.minecraft.world.entity.EquipmentSlot.HEAD,
              net.minecraft.world.entity.EquipmentSlot.CHEST,
          net.minecraft.world.entity.EquipmentSlot.LEGS,
              net.minecraft.world.entity.EquipmentSlot.FEET
        }) {
      var item = p.getItemBySlot(slot);
      if (!item.isEmpty()) weight += ItemWeightManager.weight(item);
    }
    b.equipmentWeight = weight;
  }
}
