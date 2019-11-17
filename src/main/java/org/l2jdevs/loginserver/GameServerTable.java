/*
 * Copyright © 2004-2019 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jdevs.loginserver;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.RSAKeyGenParameterSpec;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import org.l2jdevs.commons.database.pool.impl.ConnectionFactory;
import org.l2jdevs.loginserver.network.gameserverpackets.ServerStatus;
import org.l2jdevs.util.IPSubnet;
import org.l2jdevs.util.Rnd;
import org.l2jdevs.util.data.xml.IXmlReader;

/**
 * The Class GameServerTable loads the game server names and initialize the game server tables.
 * @author KenM, Zoey76
 */
public final class GameServerTable implements IXmlReader
{
	// Server Names
	private static final Map<Integer, String> SERVER_NAMES = new HashMap<>();
	// Game Server Table
	private static final Map<Integer, GameServerInfo> GAME_SERVER_TABLE = new HashMap<>();
	// RSA Config
	private static final int KEYS_SIZE = 10;
	private KeyPair[] _keyPairs;
	
	/**
	 * Instantiates a new game server table.
	 */
	public GameServerTable()
	{
		load();
		
		loadRegisteredGameServers();
		LOG.info("{}: Loaded {} registered Game Servers.", getClass().getSimpleName(), GAME_SERVER_TABLE.size());
		
		initRSAKeys();
		LOG.info("{}: Cached {} RSA keys for Game Server communication.", getClass().getSimpleName(), _keyPairs.length);
	}
	
	/**
	 * Gets the single instance of GameServerTable.
	 * @return single instance of GameServerTable
	 */
	public static GameServerTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	/**
	 * Gets the key pair.
	 * @return a random key pair.
	 */
	public KeyPair getKeyPair()
	{
		return _keyPairs[Rnd.nextInt(10)];
	}
	
	/**
	 * Gets the registered game server by id.
	 * @param id the game server Id
	 * @return the registered game server by id
	 */
	public GameServerInfo getRegisteredGameServerById(int id)
	{
		return GAME_SERVER_TABLE.get(id);
	}
	
	/**
	 * Gets the registered game servers.
	 * @return the registered game servers
	 */
	public Map<Integer, GameServerInfo> getRegisteredGameServers()
	{
		return GAME_SERVER_TABLE;
	}
	
	/**
	 * Gets the server name by id.
	 * @param id the id
	 * @return the server name by id
	 */
	public String getServerNameById(int id)
	{
		return SERVER_NAMES.get(id);
	}
	
	/**
	 * Gets the server names.
	 * @return the game server names map.
	 */
	public Map<Integer, String> getServerNames()
	{
		return SERVER_NAMES;
	}
	
	/**
	 * Checks for registered game server on id.
	 * @param id the id
	 * @return true, if successful
	 */
	public boolean hasRegisteredGameServerOnId(int id)
	{
		return GAME_SERVER_TABLE.containsKey(id);
	}
	
	@Override
	public void load()
	{
		SERVER_NAMES.clear();
		parseDatapackFile("data/servername.xml");
		LOG.info("{}: Loaded {} server names.", getClass().getSimpleName(), SERVER_NAMES.size());
	}
	
	@Override
	public void parseDocument(Document doc)
	{
		final NodeList servers = doc.getElementsByTagName("server");
		for (int s = 0; s < servers.getLength(); s++)
		{
			SERVER_NAMES.put(parseInteger(servers.item(s).getAttributes(), "id"), parseString(servers.item(s).getAttributes(), "name"));
		}
	}
	
	/**
	 * Register a game server.
	 * @param id the id
	 * @param gsi the gsi
	 * @return true, if successful
	 */
	public boolean register(int id, GameServerInfo gsi)
	{
		// avoid two servers registering with the same id
		synchronized (GAME_SERVER_TABLE)
		{
			if (!GAME_SERVER_TABLE.containsKey(id))
			{
				GAME_SERVER_TABLE.put(id, gsi);
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Register server on db.
	 * @param hexId the hex id
	 * @param id the id
	 * @param externalHost the external host
	 */
	public void registerServerOnDB(byte[] hexId, int id, String externalHost)
	{
		register(id, new GameServerInfo(id, hexId));
		try (Connection con = ConnectionFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("INSERT INTO gameservers (hexid,server_id,host) values (?,?,?)"))
		{
			ps.setString(1, hexToString(hexId));
			ps.setInt(2, id);
			ps.setString(3, externalHost);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOG.error("{}: Error while saving gameserver!", getClass().getSimpleName(), e);
		}
	}
	
	/**
	 * Wrapper method.
	 * @param gsi the game server info DTO.
	 */
	public void registerServerOnDB(GameServerInfo gsi)
	{
		registerServerOnDB(gsi.getHexId(), gsi.getId(), gsi.getExternalHost());
	}
	
	/**
	 * Register with first available id.
	 * @param gsi the game server information DTO
	 * @return true, if successful
	 */
	public boolean registerWithFirstAvailableId(GameServerInfo gsi)
	{
		// avoid two servers registering with the same "free" id
		synchronized (GAME_SERVER_TABLE)
		{
			for (Integer serverId : SERVER_NAMES.keySet())
			{
				if (!GAME_SERVER_TABLE.containsKey(serverId))
				{
					GAME_SERVER_TABLE.put(serverId, gsi);
					gsi.setId(serverId);
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Hex to string.
	 * @param hex the hex value to convert.
	 * @return the string representation.
	 */
	private String hexToString(byte[] hex)
	{
		if (hex == null)
		{
			return "null";
		}
		return new BigInteger(hex).toString(16);
	}
	
	/**
	 * Inits the RSA keys.
	 */
	private void initRSAKeys()
	{
		try
		{
			final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
			keyGen.initialize(new RSAKeyGenParameterSpec(512, RSAKeyGenParameterSpec.F4));
			_keyPairs = new KeyPair[KEYS_SIZE];
			for (int i = 0; i < KEYS_SIZE; i++)
			{
				_keyPairs[i] = keyGen.genKeyPair();
			}
		}
		catch (Exception e)
		{
			LOG.error("{}: Error loading RSA keys for Game Server communication!", getClass().getSimpleName(), e);
		}
	}
	
	/**
	 * Load registered game servers.
	 */
	private void loadRegisteredGameServers()
	{
		try (Connection con = ConnectionFactory.getInstance().getConnection();
			Statement ps = con.createStatement();
			ResultSet rs = ps.executeQuery("SELECT * FROM gameservers"))
		{
			int id;
			while (rs.next())
			{
				id = rs.getInt("server_id");
				GAME_SERVER_TABLE.put(id, new GameServerInfo(id, stringToHex(rs.getString("hexid"))));
			}
		}
		catch (Exception e)
		{
			LOG.error("{}: Error loading registered game servers!", getClass().getSimpleName(), e);
		}
	}
	
	/**
	 * String to hex.
	 * @param string the string to convert.
	 * @return return the hex representation.
	 */
	private byte[] stringToHex(String string)
	{
		return new BigInteger(string, 16).toByteArray();
	}
	
	/**
	 * The Class GameServerInfo.
	 */
	public static class GameServerInfo
	{
		// auth
		private int _id;
		private final byte[] _hexId;
		private boolean _isAuthed;
		// status
		private GameServerThread _gst;
		private int _status;
		// network
		private final ArrayList<GameServerAddress> _addrs = new ArrayList<>(5);
		private int _port;
		// config
		private final boolean _isPvp = true;
		private int _serverType;
		private int _ageLimit;
		private boolean _isShowingBrackets;
		private int _maxPlayers;
		
		/**
		 * Instantiates a new game server info.
		 * @param id the id
		 * @param hexId the hex id
		 */
		public GameServerInfo(int id, byte[] hexId)
		{
			this(id, hexId, null);
		}
		
		/**
		 * Instantiates a new game server info.
		 * @param id the id
		 * @param hexId the hex id
		 * @param gst the gst
		 */
		public GameServerInfo(int id, byte[] hexId, GameServerThread gst)
		{
			_id = id;
			_hexId = hexId;
			_gst = gst;
			_status = ServerStatus.STATUS_DOWN;
		}
		
		/**
		 * Adds the server address.
		 * @param subnet the subnet
		 * @param addr the addr
		 * @throws UnknownHostException the unknown host exception
		 */
		public void addServerAddress(String subnet, String addr) throws UnknownHostException
		{
			_addrs.add(new GameServerAddress(subnet, addr));
		}
		
		/**
		 * Clear server addresses.
		 */
		public void clearServerAddresses()
		{
			_addrs.clear();
		}
		
		/**
		 * Gets the age limit.
		 * @return the age limit
		 */
		public int getAgeLimit()
		{
			return _ageLimit;
		}
		
		/**
		 * Gets the current player count.
		 * @return the current player count
		 */
		public int getCurrentPlayerCount()
		{
			if (_gst == null)
			{
				return 0;
			}
			return _gst.getPlayerCount();
		}
		
		/**
		 * Gets the external host.
		 * @return the external host
		 */
		public String getExternalHost()
		{
			try
			{
				return getServerAddress(InetAddress.getByName("0.0.0.0"));
			}
			catch (Exception e)
			{
				
			}
			return null;
		}
		
		/**
		 * Gets the game server thread.
		 * @return the game server thread
		 */
		public GameServerThread getGameServerThread()
		{
			return _gst;
		}
		
		/**
		 * Gets the hex id.
		 * @return the hex id
		 */
		public byte[] getHexId()
		{
			return _hexId;
		}
		
		/**
		 * Gets the id.
		 * @return the id
		 */
		public int getId()
		{
			return _id;
		}
		
		/**
		 * Gets the max players.
		 * @return the max players
		 */
		public int getMaxPlayers()
		{
			return _maxPlayers;
		}
		
		public String getName()
		{
			// this value can't be stored in a private variable because the ID can be changed by setId()
			return GameServerTable.getInstance().getServerNameById(_id);
		}
		
		/**
		 * Gets the port.
		 * @return the port
		 */
		public int getPort()
		{
			return _port;
		}
		
		/**
		 * Gets the server address.
		 * @param addr the addr
		 * @return the server address
		 */
		public String getServerAddress(InetAddress addr)
		{
			for (GameServerAddress a : _addrs)
			{
				if (a.equals(addr))
				{
					return a.getServerAddress();
				}
			}
			return null; // should not happens
		}
		
		/**
		 * Gets the server addresses.
		 * @return the server addresses
		 */
		public String[] getServerAddresses()
		{
			String[] result = new String[_addrs.size()];
			for (int i = 0; i < result.length; i++)
			{
				result[i] = _addrs.get(i).toString();
			}
			
			return result;
		}
		
		/**
		 * Gets the server type.
		 * @return the server type
		 */
		public int getServerType()
		{
			return _serverType;
		}
		
		/**
		 * Gets the status.
		 * @return the status
		 */
		public int getStatus()
		{
			return _status;
		}
		
		public String getStatusName()
		{
			switch (_status)
			{
				case 0:
					return "Auto";
				case 1:
					return "Good";
				case 2:
					return "Normal";
				case 3:
					return "Full";
				case 4:
					return "Down";
				case 5:
					return "GM Only";
				default:
					return "Unknown";
			}
		}
		
		/**
		 * Checks if is authed.
		 * @return true, if is authed
		 */
		public boolean isAuthed()
		{
			return _isAuthed;
		}
		
		/**
		 * Checks if is pvp.
		 * @return true, if is pvp
		 */
		public boolean isPvp()
		{
			return _isPvp;
		}
		
		/**
		 * Checks if is showing brackets.
		 * @return true, if is showing brackets
		 */
		public boolean isShowingBrackets()
		{
			return _isShowingBrackets;
		}
		
		/**
		 * Sets the age limit.
		 * @param val the new age limit
		 */
		public void setAgeLimit(int val)
		{
			_ageLimit = val;
		}
		
		/**
		 * Sets the authed.
		 * @param isAuthed the new authed
		 */
		public void setAuthed(boolean isAuthed)
		{
			_isAuthed = isAuthed;
		}
		
		/**
		 * Sets the down.
		 */
		public void setDown()
		{
			setAuthed(false);
			setPort(0);
			setGameServerThread(null);
			setStatus(ServerStatus.STATUS_DOWN);
		}
		
		/**
		 * Sets the game server thread.
		 * @param gst the new game server thread
		 */
		public void setGameServerThread(GameServerThread gst)
		{
			_gst = gst;
		}
		
		/**
		 * Sets the id.
		 * @param id the new id
		 */
		public void setId(int id)
		{
			_id = id;
		}
		
		/**
		 * Sets the max players.
		 * @param maxPlayers the new max players
		 */
		public void setMaxPlayers(int maxPlayers)
		{
			_maxPlayers = maxPlayers;
		}
		
		/**
		 * Sets the port.
		 * @param port the new port
		 */
		public void setPort(int port)
		{
			_port = port;
		}
		
		/**
		 * Sets the server type.
		 * @param val the new server type
		 */
		public void setServerType(int val)
		{
			_serverType = val;
		}
		
		/**
		 * Sets the showing brackets.
		 * @param val the new showing brackets
		 */
		public void setShowingBrackets(boolean val)
		{
			_isShowingBrackets = val;
		}
		
		/**
		 * Sets the status.
		 * @param status the new status
		 */
		public void setStatus(int status)
		{
			_status = status;
		}
		
		/**
		 * The Class GameServerAddress.
		 */
		private class GameServerAddress extends IPSubnet
		{
			private final String _serverAddress;
			
			/**
			 * Instantiates a new game server address.
			 * @param subnet the subnet
			 * @param address the address
			 * @throws UnknownHostException the unknown host exception
			 */
			public GameServerAddress(String subnet, String address) throws UnknownHostException
			{
				super(subnet);
				_serverAddress = address;
			}
			
			/**
			 * Gets the server address.
			 * @return the server address
			 */
			public String getServerAddress()
			{
				return _serverAddress;
			}
			
			@Override
			public String toString()
			{
				return _serverAddress + super.toString();
			}
		}
	}
	
	/**
	 * The Class SingletonHolder.
	 */
	private static class SingletonHolder
	{
		protected static final GameServerTable _instance = new GameServerTable();
	}
}
