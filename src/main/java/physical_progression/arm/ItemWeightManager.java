package physical_progression.arm;

import physical_progression.datamap.ItemWeightData;
import net.minecraft.world.item.*;

public final class ItemWeightManager {
  public static double weight(ItemStack stack) {
    if (stack.isEmpty()) return 1;
    var data = stack.getItemHolder().getData(ItemWeightData.TYPE);
    if (data != null) return data.weight();
    if (stack.getItem() instanceof ShieldItem) return 3;
    if (stack.getItem() instanceof AxeItem) return 2.5;
    if (stack.getItem() instanceof DiggerItem) return 2;
    if (stack.getItem() instanceof SwordItem) return 1.5;
    if (stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem) return 1.2;
    return 1;
  }
}
