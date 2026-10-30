package physical_progression.body;

import java.util.EnumMap;
import net.minecraft.nbt.CompoundTag;
import physical_progression.attachment.Capacity;
import physical_progression.config.ConditioningConfig;

public final class BodyState {
  public final EnumMap<BodyAbility, AbilityProgress> abilities = new EnumMap<>(BodyAbility.class);
  public final Capacity legs = new Capacity(ConditioningConfig.LEG_INITIAL.get());
  public final EnumMap<BodyAbility, Double> pending = new EnumMap<>(BodyAbility.class);
  public double baseStamina = ConditioningConfig.INITIAL.get(),
      baseLegs = ConditioningConfig.LEG_INITIAL.get();
  public double recoveryCredit,
      sessionCapacity,
      sessionLoad,
      sessionDistance,
      sessionVertical,
      sessionClimbDistance;
  public double sessionPeakFatigue, sessionRecovered;
  public boolean climbEngaged, climbExhausted;
  public boolean breathActive, breathPaid, breathInvalid;
  public double breathSpent;
  public int breathStartAir, breathLastAir, breathMaximum, breathTicks;
  public final java.util.ArrayDeque<CompoundTag> history = new java.util.ArrayDeque<>();
  public int sessionTicks, sessionIdle, trainingKinds, sessionKinds;
  public long completedId;
  public CompoundTag lastSession = new CompoundTag();
  public String activity = "rest", sessionActivity = "rest";
  // Runtime tracking is reset on login/respawn: no phantom jump or sprint restart rewards.
  public boolean groundKnown, wasGrounded, jumping, diveDamaged;
  public double takeoffY, peakY, jumpDistance, jumpCost, equipmentWeight;
  public int jumpTicks,
      burstTicks,
      burstCooldown,
      continuousSprint,
      legActive,
      leftActive,
      rightActive;

  public BodyState() {
    for (var a : BodyAbility.values()) abilities.put(a, new AbilityProgress());
  }

  public double value(BodyAbility a) {
    return abilities.get(a).value;
  }

  public void add(BodyAbility a, double amount) {
    if (amount > 0) pending.merge(a, amount, Double::sum);
  }

  public void clearSession() {
    pending.clear();
    sessionLoad = sessionDistance = sessionCapacity = sessionVertical = sessionClimbDistance = 0;
    sessionTicks = sessionIdle = sessionKinds = 0;
    sessionPeakFatigue = sessionRecovered = 0;
    sessionActivity = "rest";
  }

  public CompoundTag save() {
    var t = new CompoundTag();
    var attrs = new CompoundTag();
    for (var a : BodyAbility.values()) attrs.put(a.key(), abilities.get(a).save());
    t.putBoolean("diveDamaged", diveDamaged);
    t.put("abilities", attrs);
    t.put("legs", legs.save());
    t.putDouble("baseStamina", baseStamina);
    t.putDouble("baseLegs", baseLegs);
    t.putDouble("recoveryCredit", recoveryCredit);
    t.putInt("trainingKinds", trainingKinds);
    t.putDouble("sessionCapacity", sessionCapacity);
    t.putDouble("sessionLoad", sessionLoad);
    t.putDouble("sessionPeakFatigue", sessionPeakFatigue);
    t.putDouble("sessionRecovered", sessionRecovered);
    t.putDouble("sessionDistance", sessionDistance);
    t.putInt("sessionTicks", sessionTicks);
    t.putInt("sessionIdle", sessionIdle);
    t.putInt("sessionKinds", sessionKinds);
    t.putString("sessionActivity", sessionActivity);
    var loads = new CompoundTag();
    for (var a : BodyAbility.values()) loads.putDouble(a.key(), pending.getOrDefault(a, 0.0));
    t.put("pending", loads);
    t.putLong("completedId", completedId);
    t.put("lastSession", lastSession.copy());
    t.put("history", historyTag());
    t.putDouble("sessionVertical", sessionVertical);
    t.putDouble("sessionClimbDistance", sessionClimbDistance);
    t.putBoolean("breathActive", breathActive);
    t.putBoolean("breathPaid", breathPaid);
    t.putBoolean("breathInvalid", breathInvalid);
    t.putDouble("breathSpent", breathSpent);
    t.putInt("breathStartAir", breathStartAir);
    t.putInt("breathLastAir", breathLastAir);
    t.putInt("breathMaximum", breathMaximum);
    t.putInt("breathTicks", breathTicks);
    return t;
  }

  public void read(CompoundTag t) {
    diveDamaged = t.getBooleanOr("diveDamaged", false);
    for (var a : BodyAbility.values())
      abilities.get(a).read(t.getCompoundOrEmpty("abilities").getCompoundOrEmpty(a.key()));
    legs.read(t.getCompoundOrEmpty("legs"));
    baseStamina = Capacity.valid(t, "baseStamina", baseStamina, 1, 100000);
    baseLegs = Capacity.valid(t, "baseLegs", baseLegs, 1, 100000);
    recoveryCredit = Capacity.valid(t, "recoveryCredit", 0, 0, 100);
    trainingKinds = t.getIntOr("trainingKinds", 0) & 31;
    sessionCapacity = Capacity.valid(t, "sessionCapacity", 0, 0, 100000);
    sessionPeakFatigue = Capacity.valid(t, "sessionPeakFatigue", 0, 0, 1);
    sessionRecovered = Capacity.valid(t, "sessionRecovered", 0, 0, 1e6);
    sessionLoad = Capacity.valid(t, "sessionLoad", 0, 0, 1e6);
    sessionDistance = Capacity.valid(t, "sessionDistance", 0, 0, 1e6);
    sessionTicks = Math.clamp(t.getIntOr("sessionTicks", 0), 0, 1000000);
    sessionIdle = Math.clamp(t.getIntOr("sessionIdle", 0), 0, 12000);
    sessionKinds = t.getIntOr("sessionKinds", 0) & 31;
    sessionActivity = t.getStringOr("sessionActivity", "");
    pending.clear();
    for (var a : BodyAbility.values())
      add(a, Capacity.valid(t.getCompoundOrEmpty("pending"), a.key(), 0, 0, 1e6));
    completedId = Math.max(0, t.getLongOr("completedId", 0L));
    lastSession = t.getCompoundOrEmpty("lastSession").copy();
    history.clear();
    var records = t.getListOrEmpty("history");
    for (int i = 0; i < Math.min(records.size(), ConditioningConfig.HISTORY_LIMIT.get()); i++) {
      var entry = records.getCompoundOrEmpty(i);
      if (entry.getLongOr("id", 0L) > 0) history.addLast(entry.copy());
    }
    if (history.isEmpty() && lastSession.getLongOr("id", 0L) > 0) history.addFirst(lastSession);
    if (!history.isEmpty()) lastSession = history.getFirst();
    completedId = Math.max(completedId, lastSession.getLongOr("id", 0L));
    sessionVertical = Capacity.valid(t, "sessionVertical", 0, 0, 1e6);
    sessionClimbDistance = Capacity.valid(t, "sessionClimbDistance", 0, 0, 1e6);
    breathActive = t.getBooleanOr("breathActive", false);
    breathPaid = t.getBooleanOr("breathPaid", false);
    breathInvalid = t.getBooleanOr("breathInvalid", false) || diveDamaged;
    breathSpent = Capacity.valid(t, "breathSpent", 0, 0, 100000);
    breathStartAir = Math.clamp(t.getIntOr("breathStartAir", 0), 0, 100000);
    breathLastAir = Math.clamp(t.getIntOr("breathLastAir", 0), 0, 100000);
    breathMaximum = Math.clamp(t.getIntOr("breathMaximum", 0), 0, 100000);
    breathTicks = Math.clamp(t.getIntOr("breathTicks", 0), 0, 1000000);
  }

  public void complete(CompoundTag entry) {
    lastSession = entry.copy();
    lastSession.putLong("finishedAt", System.currentTimeMillis());
    history.addFirst(lastSession);
    while (history.size() > ConditioningConfig.HISTORY_LIMIT.get()) history.removeLast();
  }

  public net.minecraft.nbt.ListTag historyTag() {
    var list = new net.minecraft.nbt.ListTag();
    for (var entry : history) list.add(entry.copy());
    return list;
  }
}
