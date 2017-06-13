package org.centauri.cloud.cloud.server;

import io.netty.channel.Channel;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import lombok.Getter;
import org.centauri.cloud.cloud.Cloud;
import org.centauri.cloud.cloud.event.events.ServerConnectEvent;
import org.centauri.cloud.cloud.event.events.ServerDisconnectEvent;

public class ServerManager {
	
	@Getter private final ConcurrentMap<Channel, Server> channelToServer = new ConcurrentHashMap<>();
	@Getter private final ConcurrentMap<String, Server> nameToServer = new ConcurrentHashMap<>();
	@Getter private final ReentrantLock lock = new ReentrantLock();
	
	public void registerServer(Server server) {
		this.lock.lock();
		try{
			this.channelToServer.put(server.getChannel(), server);
			this.nameToServer.put(server.getName(), server);
			server.setId(this.getId(server.getPrefix()));
			server.setName(server.getPrefix() + "-" + server.getId());
			Cloud.getInstance().getEventManager().callEvent(new ServerConnectEvent(server));
		} finally {
			this.lock.unlock();
		}
	}
	
	public void removeServer(Channel channel) {
		this.lock.lock();
		try{
			Server server = this.channelToServer.get(channel);
			this.channelToServer.remove(channel);
			this.nameToServer.remove(server.getName());
			Cloud.getInstance().getEventManager().callEvent(new ServerDisconnectEvent(server));
		} finally {
			this.lock.unlock();
		}
	}
	
	private int getId(final String prefix) {
		Set<Server> serversWithPrefix = this.channelToServer.values().stream().filter(server -> server.getPrefix().equals(prefix)).collect(Collectors.toSet());
		
		for(int i = 1; i < Integer.MAX_VALUE; i++) {
			if(!this.isIdUsed(i, serversWithPrefix))
				return i;
		}
		
		return -1337;
	}
	
	private boolean isIdUsed(int id, Set<Server> serversWithPrefix) {
		for(Server server : serversWithPrefix) {
			if(server.getId() == id)
				return true;
		}
		
		return false;
	}
}