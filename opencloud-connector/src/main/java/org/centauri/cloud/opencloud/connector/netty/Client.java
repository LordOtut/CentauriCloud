package org.centauri.cloud.opencloud.connector.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.centauri.cloud.cloud.network.packets.Packet;

@RequiredArgsConstructor
public class Client {
	
	@Getter @Setter private Channel channel;
	@Getter private final SimpleChannelInboundHandler<Packet> handler;
	private final String host;
	private final int port;
	
	public void start() throws InterruptedException {
		final EventLoopGroup workerGroup = Epoll.isAvailable() ? new EpollEventLoopGroup() : new NioEventLoopGroup();
	
		try{
			Bootstrap bootstrap = new Bootstrap();
			bootstrap.group(workerGroup)
					.channel(Epoll.isAvailable() ? EpollSocketChannel.class : NioSocketChannel.class)
					.handler(new OpenCloudChannelIntiializer(this))
					.connect(this.host, this.port).sync().channel().closeFuture().syncUninterruptibly();
		} finally {
			workerGroup.shutdownGracefully();
			System.out.println("Netty client stopped");
		}
	}
	
}
