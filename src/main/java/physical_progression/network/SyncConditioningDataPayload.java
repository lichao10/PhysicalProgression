package physical_progression.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import physical_progression.attachment.PlayerConditioningData;

public record SyncConditioningDataPayload(CompoundTag values) implements CustomPacketPayload {
  public static final Type<SyncConditioningDataPayload> TYPE =
      new Type<>(Identifier.fromNamespaceAndPath("physical_progression", "body"));
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
    return values.getDoubleOr("stamina", 0.0);
  }

  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }
}
