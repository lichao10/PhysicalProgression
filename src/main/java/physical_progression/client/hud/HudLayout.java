package physical_progression.client.hud;

import java.util.*;
import physical_progression.client.data.ClientConditioningData;
import physical_progression.config.ConditioningClientConfig.HudMode;

public final class HudLayout {
  public static List<String> visible(ClientConditioningData d, HudMode mode) {
    var keys = new ArrayList<String>();
    if (mode == HudMode.OFF) return keys;
    boolean swimming = d.resources.getStringOr("activity", "").equals("swimming"),
        moving =
            !d.resources.getStringOr("activity", "").equals("rest")
                && !d.resources.getStringOr("activity", "").equals("diving");
    if (mode == HudMode.ALWAYS_SHOW || moving || low(d, "stamina", 0.999)) keys.add("stamina");
    if (mode == HudMode.MINIMAL) return keys;
    if (mode == HudMode.ALWAYS_SHOW
        || moving
        || d.resources.getIntOr("legActive", 0) > 0
        || low(d, "legs", 0.9)) keys.add("legs");
    for (String arm : new String[] {"left", "right"})
      if (mode == HudMode.ALWAYS_SHOW
          || swimming
          || d.resources.getIntOr(arm + "Active", 0) > 0
          || low(d, arm, 0.9)) keys.add(arm);
    if (d.resources.getBooleanOr("underwater", false)) keys.add("breath");
    return keys;
  }

  private static boolean low(ClientConditioningData d, String key, double threshold) {
    return d.resource(key) / Math.max(1, d.resource(key + "Max")) < threshold;
  }
}
