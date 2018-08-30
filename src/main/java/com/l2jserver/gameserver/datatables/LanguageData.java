/*
 * Copyright (C) 2004-2018 L2J Server
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
package com.l2jserver.gameserver.datatables;

import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;

/**
 * Class for language data.
 * @author U3Games
 */
public final class LanguageData
{
	// Logger
	private final Logger LOGGER = Logger.getLogger(LanguageData.class.getName());
	
	// Default
	private final String DIRECTORY = "config/Language";
	private final String DEFAULT_LANG = "en";
	
	// Maps
	private final Map<Integer, String> _playerCurrentLang = new HashMap<>();
	private final Map<String, String> _msgMap = new HashMap<>();
	private final Map<String, String> _languages = new HashMap<>();
	
	// Mist
	private Document doc;
	private Node n;
	
	public LanguageData()
	{
		try
		{
			final File dir = new File(DIRECTORY);
			for (File file : dir.listFiles((FileFilter) pathname ->
			{
				if (pathname.getName().endsWith(".xml"))
				{
					return true;
				}
				return false;
			}))
			{
				if (file.getName().startsWith("lang_"))
				{
					loadXml(file, file.getName().substring(5, file.getName().indexOf(".xml")));
				}
			}
			
			LOGGER.info(LanguageData.class.getSimpleName() + ": Loaded " + _languages.size() + " languages.");
		}
		catch (Exception e)
		{
			LOGGER.warning(LanguageData.class.getSimpleName() + ": -> Error while loading language files: " + e);
			e.printStackTrace();
		}
	}
	
	private void loadXml(File file, String lang)
	{
		int count = 0;
		String langName = "";
		
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setIgnoringComments(true);
		doc = null;
		
		if (file.exists())
		{
			try
			{
				doc = factory.newDocumentBuilder().parse(file);
			}
			catch (Exception e)
			{
				LOGGER.warning(LanguageData.class.getSimpleName() + ": -> Could not load language (" + lang + ") file for event engine: " + e);
				e.printStackTrace();
			}
			
			n = doc.getFirstChild();
			final NamedNodeMap docAttr = n.getAttributes();
			if (docAttr.getNamedItem("lang") != null)
			{
				langName = docAttr.getNamedItem("lang").getNodeValue();
			}
			
			if (!_languages.containsKey(lang))
			{
				_languages.put(lang, langName);
			}
			
			for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
			{
				if (d.getNodeName().equals("message"))
				{
					final NamedNodeMap attrs = d.getAttributes();
					final String id = attrs.getNamedItem("id").getNodeValue();
					final String text = attrs.getNamedItem("text").getNodeValue();
					_msgMap.put(lang + "_" + id, text);
					count++;
				}
			}
		}
		
		LOGGER.info("Loaded language file for language: " + lang.toUpperCase() + " with " + count + " messages.");
	}
	
	/**
	 * @param player
	 * @param text
	 * @return String
	 */
	public String getMsgByLang(L2PcInstance player, String text)
	{
		final String lang = getLanguage(player);
		final StringBuilder msg = new StringBuilder(50);
		final StringTokenizer st = new StringTokenizer(text, " ");
		
		while (st.hasMoreTokens())
		{
			final String textLang = st.nextToken();
			if (_msgMap.containsKey(lang + "_" + textLang))
			{
				msg.append(_msgMap.get(lang + "_" + textLang));
			}
			else if (_msgMap.containsKey(lang + "_" + textLang))
			{
				msg.append(_msgMap.get("en_" + textLang));
			}
			else
			{
				msg.append(textLang);
			}
		}
		
		return msg.toString();
	}
	
	/**
	 * @param character
	 * @param text
	 * @return String
	 */
	public String getMsgByLang(L2Character character, String text)
	{
		final String lang = getLanguage(character);
		final StringBuilder msg = new StringBuilder(50);
		final StringTokenizer st = new StringTokenizer(text, " ");
		
		while (st.hasMoreTokens())
		{
			final String textLang = st.nextToken();
			if (_msgMap.containsKey(lang + "_" + textLang))
			{
				msg.append(_msgMap.get(lang + "_" + textLang));
			}
			else if (_msgMap.containsKey(lang + "_" + textLang))
			{
				msg.append(_msgMap.get("en_" + textLang));
			}
			else
			{
				msg.append(textLang);
			}
		}
		
		return msg.toString();
	}
	
	/**
	 * @param text
	 * @return String
	 */
	public String getMsgByLang(String text)
	{
		final String lang = getLanguage(null);
		final StringBuilder msg = new StringBuilder(50);
		final StringTokenizer st = new StringTokenizer(text, " ");
		
		while (st.hasMoreTokens())
		{
			final String textLang = st.nextToken();
			if (_msgMap.containsKey(lang + "_" + textLang))
			{
				msg.append(_msgMap.get(lang + "_" + textLang));
			}
			else if (_msgMap.containsKey(lang + "_" + textLang))
			{
				msg.append(_msgMap.get("en_" + textLang));
			}
			else
			{
				msg.append(textLang);
			}
		}
		
		return msg.toString();
	}
	
	/**
	 * @param player
	 * @param lang
	 */
	public void setLanguage(L2PcInstance player, String lang)
	{
		_playerCurrentLang.put(player.getObjectId(), lang);
	}
	
	/**
	 * @param player
	 * @return String
	 */
	public String getLanguage(L2PcInstance player)
	{
		if (_playerCurrentLang.containsKey(player.getObjectId()))
		{
			return _playerCurrentLang.get(player.getObjectId());
		}
		
		return DEFAULT_LANG;
	}
	
	/**
	 * @param character
	 * @return String
	 */
	private String getLanguage(L2Character character)
	{
		if (_playerCurrentLang.containsKey(character.getObjectId()))
		{
			return _playerCurrentLang.get(character.getObjectId());
		}
		
		return DEFAULT_LANG;
	}
	
	/**
	 * @return
	 */
	public Map<String, String> getLanguages()
	{
		return _languages;
	}
	
	public static LanguageData getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final LanguageData _instance = new LanguageData();
	}
}