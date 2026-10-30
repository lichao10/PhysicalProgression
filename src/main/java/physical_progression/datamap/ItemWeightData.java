package physical_progression.datamap;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.datamaps.*;

@EventBusSubscriber(modid = "physical_progression")
public record ItemWeightData(double weight) {
  public static final Codec<ItemWeightData> CODEC =
      Codec.doubleRange(0.01, 100)
          .fieldOf("weight")
          .xmap(ItemWeightData::new, ItemWeightData::weight)
          .codec();
  public static final DataMapType<Item, ItemWeightData> TYPE =
      DataMapType.builder(
              Identifier.fromNamespaceAndPath("physical_progression", "item_weight"),
              Registries.ITEM,
              CODEC)
          .build();

  @SubscribeEvent
  public static void register(RegisterDataMapTypesEvent e) {
    e.register(TYPE);
  }
}
