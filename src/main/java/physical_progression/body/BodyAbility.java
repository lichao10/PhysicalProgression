package physical_progression.body;

import java.util.Locale;

/** Shared physical adaptations, never action-specific skill levels. Baseline is 1.0. */
public enum BodyAbility {
  CARDIO,
  LUNGS,
  LEG_STRENGTH,
  LEG_ENDURANCE,
  EXPLOSIVENESS,
  GRIP,
  CORE,
  RECOVERY,
  MOBILITY,
  CLIMBING;

  public String key() {
    return name().toLowerCase(Locale.ROOT);
  }
}
