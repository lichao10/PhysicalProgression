package physical_progression.network;

import physical_progression.attachment.PlayerConditioningData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SyncConditioningDataPayload(CompoundTag values) implements CustomPacketPayload {
  public static final Type<SyncConditioningDataPayload> TYPE =
      new Type<>(ResourceLocation.fromNamespaceAndPath("physical_progression", "body"));
  public static final StreamCodec<RegistryFriendlyByteBuf, SyncConditioningDataPayload>
      STREAM_CODEC =
          new StreamCodec<>() {
            public SyncConditioningDataPayload decode(RegistryFriendlyByteBuf b) {
              return new SyncConditioningDataPayload(java.util.Objects.requireNonNull(b.readNbt()));
            }

            public void encode(RegistryFriendlyByteBuf b, SyncConditioningDataPayload p) {
              b.writeNbt(p.values);
            }
          };

  public static SyncConditioningDataPayload of(PlayerConditioningData d) {
    return new SyncConditioningDataPayload(BodySnapshots.resources(d));
  }

  public double stamina() {
    return values.getDouble("stamina");
  }

  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }
}
