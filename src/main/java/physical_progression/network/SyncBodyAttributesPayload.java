package physical_progression.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SyncBodyAttributesPayload(CompoundTag values) implements CustomPacketPayload {
  public static final Type<SyncBodyAttributesPayload> TYPE =
      new Type<>(Identifier.fromNamespaceAndPath("physical_progression", "attributes"));
  public static final StreamCodec<RegistryFriendlyByteBuf, SyncBodyAttributesPayload> STREAM_CODEC =
      new StreamCodec<>() {
        public SyncBodyAttributesPayload decode(RegistryFriendlyByteBuf b) {
          return new SyncBodyAttributesPayload(java.util.Objects.requireNonNull(b.readNbt()));
        }

        public void encode(RegistryFriendlyByteBuf b, SyncBodyAttributesPayload p) {
          b.writeNbt(p.values);
        }
      };

  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }
}
