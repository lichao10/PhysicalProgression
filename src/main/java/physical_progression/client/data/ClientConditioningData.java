package physical_progression.client.data;

import net.minecraft.nbt.CompoundTag;

/** Read-only-to-gameplay mirror. Network handlers replace snapshots; no local training logic. */
public final class ClientConditioningData {
  public CompoundTag resources = new CompoundTag(), attributes = new CompoundTag();

  public double minimumFatigue() {
    return attributes.contains("minimumFatigue") ? attribute("minimumFatigue") : 0.2;
  }

  public double optimalFatigue() {
    return attributes.contains("optimalFatigue") ? attribute("optimalFatigue") : 0.7;
  }

  public boolean ready() {
    return resources.contains("stamina");
  }

  public double resource(String key) {
    return resources.getDoubleOr(key, 0.0);
  }

  public double attribute(String key) {
    return attributes.getDoubleOr(key, 0.0);
  }
}
