package physical_progression.attachment;

import physical_progression.config.ConditioningConfig;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

public final class PlayerConditioningData implements INBTSerializable<CompoundTag> {
  public final Capacity stamina = new Capacity(ConditioningConfig.INITIAL.get());
  public final physical_progression.body.BodyState body = new physical_progression.body.BodyState();
  public net.minecraft.nbt.CompoundTag lastDetails;
  public int lastScreenRequest = -100;
  public final Arm left = new Arm(), right = new Arm();
  public double recoveryRate = ConditioningConfig.RECOVERY.get();
  // Position and network baselines intentionally do not survive loading or cloning.
  public double lastX, lastY, lastZ;
  public boolean positioned;
  public boolean movementInput;
  public int movementInputUntil = -1;
  public int movementSuppression, lastAttackTick = -100;
  public net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension;
  public physical_progression.network.SyncConditioningDataPayload lastSent;

  public static final class Arm {
    public final Capacity endurance = new Capacity(ConditioningConfig.ARM_INITIAL.get());
    public double strength = 1, strengthXp;

    CompoundTag save() {
      var t = endurance.save();
      t.putDouble("strength", strength);
      t.putDouble("strengthXp", strengthXp);
      return t;
    }

    void read(CompoundTag t) {
      endurance.read(t);
      strength = Capacity.valid(t, "strength", 1, 1, 1000);
      strengthXp = Capacity.valid(t, "strengthXp", 0, 0, 1e9);
    }
  }

  public CompoundTag serializeNBT(HolderLookup.Provider provider) {
    var t = new CompoundTag();
    t.putInt("version", 4);
    t.put("body", body.save());
    t.put("stamina", stamina.save());
    t.put("left", left.save());
    t.put("right", right.save());
    t.putDouble("recoveryRate", recoveryRate);
    return t;
  }

  public void deserializeNBT(HolderLookup.Provider provider, CompoundTag t) {
    stamina.read(t.getCompound("stamina"));
    left.read(t.getCompound("left"));
    right.read(t.getCompound("right"));
    recoveryRate = Capacity.valid(t, "recoveryRate", recoveryRate, 0, 1000);
    if (t.contains("body")) body.read(t.getCompound("body"));
    else {
      body.baseStamina = stamina.maximum;
      body.abilities.get(physical_progression.body.BodyAbility.CARDIO).xp = stamina.xp;
    }
    // Original capacities are retained for v1 saves; the new attributes add contributions.

  }
}
