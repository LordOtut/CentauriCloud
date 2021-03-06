package org.centauri.cloud.bungee;

import lombok.Getter;
import net.md_5.bungee.api.plugin.Plugin;
import org.centauri.cloud.bungee.config.CloudConfiguration;
import org.centauri.cloud.bungee.listener.PlayerListener;
import org.centauri.cloud.bungee.netty.NetworkHandler;
import org.centauri.cloud.bungee.server.ServerManager;
import org.centauri.cloud.centauricloud.connector.netty.Client;
import org.centauri.cloud.common.network.packets.PacketCloseConnection;

import java.util.logging.Level;
import java.util.logging.Logger;

public class BungeeConnectorPlugin extends Plugin{
	
	@Getter private static BungeeConnectorPlugin instance;
	@Getter private static Logger pluginLogger;
	@Getter private Client client;
	@Getter private CloudConfiguration cloudConfiguration;
	@Getter private ServerManager serverManager;
	
	@Override
	public void onLoad() {
		BungeeConnectorPlugin.instance = this;
		BungeeConnectorPlugin.pluginLogger = this.getLogger();
				
		getPluginLogger().info("Loaded CentauriCloud bungee connector.");
	}
	
	@Override
	public void onEnable() {		
		this.cloudConfiguration = new CloudConfiguration("centauricloud.yml");
		
		getPluginLogger().info(String.format("%s -> %s:%s", cloudConfiguration.getPrefix(), cloudConfiguration.getHostname(), cloudConfiguration.getPort()));
				
		this.serverManager = new ServerManager();
		
		this.getProxy().getPluginManager().registerListener(this, new PlayerListener());
		getProxy().getServers().clear();
		
		new Thread(() -> {
			getPluginLogger().info("Try to start netty client...");

			if(cloudConfiguration.getHostname() == null) {
				getPluginLogger().info("Cannot start netty client!");
				return;
			}
			
			this.client = new Client(new NetworkHandler(), cloudConfiguration.getHostname(), cloudConfiguration.getPort());
			try {
				this.client.start();
			} catch (Exception ex) {
				Logger.getLogger(BungeeConnectorPlugin.class.getName()).log(Level.SEVERE, null, ex);
			}
		}, "Netty-Thread").start();
		
		getPluginLogger().info("Enabled CentauriCloud bungee connector.");
	}
	
	@Override
	public void onDisable() {
		this.client.getChannel().writeAndFlush(new PacketCloseConnection());
		getPluginLogger().info("Disabled CentauriCloud bungee connector.");
	}

}
