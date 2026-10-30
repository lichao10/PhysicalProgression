package physical_progression.gametest;

import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

/** In-memory transport for standalone players; no sockets or client state are required. */
final class TestPlayers {
  static void connect(ServerPlayer player) {
    var transport =
        new Connection(PacketFlow.SERVERBOUND) {
          @Override
          public void setListenerForServerboundHandshake(PacketListener listener) {}
        };
    new ServerGamePacketListenerImpl(
        player.level().getServer(),
        transport,
        player,
        CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
      @Override
      public void send(Packet<?> packet, ChannelFutureListener listener) {}
    };
  }
}
