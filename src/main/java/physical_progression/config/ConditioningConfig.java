package physical_progression.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ConditioningConfig {
  private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

  private static ModConfigSpec.DoubleValue number(
      String name, double value, double min, double max) {
    return B.comment(description(name)).defineInRange(name, value, min, max);
  }

  public static final ModConfigSpec.DoubleValue INITIAL = number("initialMaxStamina", 50, 1, 10000),
      ARM_INITIAL = number("initialArmStamina", 30, 1, 10000),
      SPRINT_COST = number("sprintCostPerBlock", 1, 0, 100),
      WALK_COST = number("walkCostPerBlock", 0.25, 0, 100),
      RECOVERY = number("staminaRecoveryRate", 5, 0, 1000),
      WALK_RECOVERY = number("walkingRecoveryMultiplier", 0.5, 0, 1),
      CROUCH_RECOVERY = number("crouchRecoveryMultiplier", 1.1, 0, 10),
      MIN_FATIGUE = number("minimumTrainingFatigue", 0.2, 0.01, 0.39),
      OPTIMAL = number("optimalTrainingFatigue", 0.7, 0.7, 0.89),
      GROWTH_XP = number("baseStaminaGrowthXp", 100, 1, 1000000),
      CAP = number("capacitySafetyLimit", 500, 50, 100000),
      STRENGTH_GROWTH = number("strengthGrowthMultiplier", 1, 0, 100),
      ENDURANCE_GROWTH = number("enduranceGrowthMultiplier", 1, 0, 100),
      MIN_COST = number("minimumCostMultiplier", 0.15, 0.01, 1),
      ARM_RECOVERY = number("armRecoveryPerSecond", 3, 0, 1000),
      MAX_DISTANCE = number("maximumDistancePerTick", 0.85, 0.1, 10),
      ATTACK_COST = number("attackBaseCost", 2, 0, 100),
      MINING_COST = number("miningBaseCost", 1, 0, 100),
      HOLD_COST = number("heldItemCostPerSecond", 2, 0, 100),
      HEAVY_WEIGHT = number("heavyItemWeight", 2, 0.1, 100);
  public static final ModConfigSpec.IntValue
      RECOVERY_DELAY =
          B.comment("停止高强度活动后的恢复等待 tick；20 tick = 1 秒。")
              .defineInRange("staminaRecoveryDelay", 50, 0, 12000),
      SESSION_DELAY =
          B.comment("停止活动多久后结算训练，单位 tick。").defineInRange("trainingSessionEndDelay", 60, 1, 12000),
      SYNC_INTERVAL = B.comment("服务端资源同步检查间隔 tick。").defineInRange("syncIntervalTicks", 5, 1, 100);
  public static final ModConfigSpec.DoubleValue
      LEG_INITIAL = number("initialLegCapacity", 80, 1, 10000),
      LEG_COST = number("legCostMultiplier", 1, 0, 10),
      LEG_RECOVERY = number("legRecoveryPerSecond", 5, 0, 100),
      SWIM_COST = number("swimCostPerBlock", 1.2, 0, 100),
      JUMP_COST = number("jumpStaminaCost", 0.6, 0, 100),
      MAX_JUMP_BONUS = number("maximumJumpPowerBonus", 0.15, 0, 0.3),
      MAX_BURST = number("maximumSprintBurstBonus", 0.08, 0, 0.2),
      ABILITY_XP = number("bodyAbilityGrowthXp", 100, 1, 1000000),
      ABILITY_STEP = number("bodyAbilityGrowthStep", 0.02, 0.001, 1),
      EQUIPMENT_ALLOWANCE = number("unpenalizedEquipmentWeight", 8, 0, 100),
      MAX_BREATH_BONUS = number("maximumBreathBonus", 1, 0, 5);
  public static final ModConfigSpec.DoubleValue CLIMB_COST =
      number("climbCostPerBlock", 1.2, 0, 100);
  public static final ModConfigSpec.IntValue HISTORY_LIMIT =
      B.comment("保留最近训练记录条数。").defineInRange("trainingHistoryLimit", 20, 1, 50);
  public static final ModConfigSpec.DoubleValue
      TRAINING_XP = number("trainingXpMultiplier", 2.0, 0, 20),
      CLIMB_SPEED = number("baseClimbingSpeed", 0.16, 0.05, 0.3),
      CLIMB_SPEED_GAIN = number("climbingSpeedPerAbility", 0.04, 0, 0.1),
      CLIMB_MAX_SPEED = number("maximumClimbingSpeed", 0.25, 0.05, 0.4),
      BREATH_XP = number("breathTrainingBaseXp", 12, 0, 1000);
  public static final ModConfigSpec.DoubleValue RECOVERY_SPEED =
      number("recoverySpeedMultiplier", 1, 0, 20);
  public static final ModConfigSpec.DoubleValue
      CLIMB_HOLD_COST = number("climbingHoldStaminaPerSecond", 1.0, 0, 100),
      CLIMB_ARM_HOLD_COST = number("climbingHoldArmCostPerSecond", 1.5, 0, 100),
      SESSION_LIMIT = number("trainingSessionLoadLimit", 0.85, 0.4, 0.89),
      LUNGS_STEP = number("lungsGrowthStep", 0.1, 0.001, 1),
      CLIMBING_STEP = number("climbingGrowthStep", 0.1, 0.001, 1);
  public static final ModConfigSpec.IntValue SESSION_MAX_TICKS =
      B.comment("持续训练分段的最长 tick，默认 900 = 45 秒。已结算收益不会被后续活动覆盖。")
          .defineInRange("trainingSessionMaximumTicks", 900, 100, 12000);
  public static final ModConfigSpec.BooleanValue SWIM_INPUT =
      B.comment("游泳训练要求玩家主动移动或跳跃输入，防止水流推动产生挂机收益；静止憋气不受影响。").define("requireSwimmingInput", true);
  public static final ModConfigSpec SPEC = B.build();

  private static String description(String name) {
    return switch (name) {
      case "initialMaxStamina" -> "新玩家基础全身体力上限；不会重置已有玩家的容量。";
      case "initialArmStamina" -> "新玩家单臂体力上限；已有玩家保留训练结果。";
      case "sprintCostPerBlock" -> "疾跑每移动一格的基础体力消耗。";
      case "walkCostPerBlock" -> "正常行走每格基础体力消耗。";
      case "staminaRecoveryRate" -> "新玩家全身体力基础恢复量，每秒；旧玩家保留基础恢复值。";
      case "walkingRecoveryMultiplier" -> "正常行走恢复倍率，相对静止恢复速度。0.5 表示一半速度。";
      case "crouchRecoveryMultiplier" -> "静止潜行时恢复倍率。";
      case "minimumTrainingFatigue" -> "有效训练最低消耗比例，0.2 表示本次消耗达到容量的 20%。";
      case "optimalTrainingFatigue" -> "最佳训练强度区间的起点。";
      case "baseStaminaGrowthXp" -> "手臂耐力/力量基础成长所需 XP；越小成长越快。";
      case "capacitySafetyLimit" -> "全身、腿部与手臂体力容量安全上限，已有基础容量不会因此被压低。";
      case "strengthGrowthMultiplier" -> "手臂力量 XP 额外倍率，与 trainingXpMultiplier 相乘。";
      case "enduranceGrowthMultiplier" -> "手臂耐力 XP 额外倍率，与 trainingXpMultiplier 相乘。";
      case "minimumCostMultiplier" -> "训练带来的耗能降低下限，防止动作消耗降至零。";
      case "armRecoveryPerSecond" -> "双臂各自每秒基础恢复体力。";
      case "maximumDistancePerTick" -> "允许计入训练的单 tick 最大水平距离；异常位移过滤。";
      case "attackBaseCost" -> "有效近战攻击的基础手臂体力消耗。";
      case "miningBaseCost" -> "成功挖掘的基础手臂体力消耗，另计重量和硬度。";
      case "heldItemCostPerSecond" -> "有效持续使用物品的每秒基础消耗。";
      case "heavyItemWeight" -> "重负荷力量训练的重量判定参数。";
      case "initialLegCapacity" -> "新玩家腿部基础容量。";
      case "legCostMultiplier" -> "腿部相对全身运动耗能的倍率。";
      case "legRecoveryPerSecond" -> "腿部每秒基础恢复体力。";
      case "swimCostPerBlock" -> "游泳每格基础体力消耗。";
      case "jumpStaminaCost" -> "每次起跳基础体力消耗。";
      case "maximumJumpPowerBonus" -> "训练带来的额外起跳力上限，0.15 表示最多增加 15%。";
      case "maximumSprintBurstBonus" -> "训练带来的短时疾跑爆发速度加成上限。";
      case "bodyAbilityGrowthXp" -> "身体能力基础成长所需 XP，越小成长越快。";
      case "bodyAbilityGrowthStep" -> "每次身体能力成长增加的能力指数。";
      case "unpenalizedEquipmentWeight" -> "手持物品和盔甲负担的免惩罚重量。";
      case "maximumBreathBonus" -> "肺活量训练带来的最大额外空气容量比例。";
      case "climbCostPerBlock" -> "攀爬每格基础体力消耗，计入垂直距离。";
      case "trainingXpMultiplier" -> "所有有效训练 XP 的总倍率。默认 2.0 为原来的两倍，1.0 为原速，0.0 关闭新训练收益；不会翻倍已有 XP。";
      case "baseClimbingSpeed" -> "贴墙按住跳跃键（默认空格）的基础上升速度，格/tick；20 tick = 1 秒。";
      case "climbingSpeedPerAbility" -> "攀爬能力每超过初始指数 1.0 一点带来的速度增量。";
      case "maximumClimbingSpeed" -> "攀爬上升速度上限，优先于基础速度；实际移动受碰撞和物理影响。";
      case "breathTrainingBaseXp" -> "单次憋气训练基础 XP，另乘实际空气消耗比例、训练质量与总训练倍率。";
      case "recoverySpeedMultiplier" -> "全身、腿部与双臂恢复速度的总倍率，对已有玩家也生效；1.5 表示恢复快 50%。";
      case "climbingHoldStaminaPerSecond" -> "抓墙或悬挂的每秒全身体力消耗，另计实际上爬距离消耗。";
      case "climbingHoldArmCostPerSecond" -> "抓墙或悬挂的每秒单臂基础消耗，乘以手臂能力与疲劳修正。";
      case "trainingSessionLoadLimit" -> "一个训练分段累计负荷达到容量的该比例时自动结算，避免长距离行走被判为过度训练。";
      case "lungsGrowthStep" -> "肺活量每次适应的能力指数步长；默认 0.1，使首次空气容量提升约 0.45 秒。";
      case "climbingGrowthStep" -> "攀爬每次适应的能力指数步长；默认 0.1，使首次基础速度提高约 2.5%。";
      default -> name;
    };
  }

  private ConditioningConfig() {}
}
