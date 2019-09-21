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
package org.l2jdevs.gameserver.datatables;

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

import org.l2jdevs.Config;
import org.l2jdevs.gameserver.model.actor.instance.L2PcInstance;

/**
 * Class for language data.
 * @author U3Games
 */
public final class LanguageData
{
	// Logger for class
	private final Logger LOGGER = Logger.getLogger(LanguageData.class.getName());
	
	// Folder of languages
	private final String DIRECTORY = "config/Language";
	
	// Maps
	private final Map<String, String> _msgServer = new HashMap<>();
	private final Map<String, String> _languages = new HashMap<>();
	
	// Mist
	private Document doc;
	private Node n;
	
	public LanguageData()
	{
		loadFiles();
	}
	
	private void loadFiles()
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
				LOGGER.warning(LanguageData.class.getSimpleName() + ": -> Could not load language (" + lang + ") in file lang: " + e);
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
					_msgServer.put(lang + "_" + id, text);
					count++;
				}
			}
		}
		
		LOGGER.info("Loaded language file: " + lang.toUpperCase() + " with " + count + " messages.");
	}
	
	/**
	 * Get message key of player to send translation.
	 * @param player
	 * @param text
	 */
	public String getMsg(L2PcInstance player, String text)
	{
		String lang = player.getLang();
		if (lang == null)
		{
			lang = Config.L2JMOD_MULTILANG_DEFAULT;
		}
		
		final StringBuilder msg = new StringBuilder(50);
		final StringTokenizer st = new StringTokenizer(text, " ");
		while (st.hasMoreTokens())
		{
			final String textLang = st.nextToken();
			if (_msgServer.containsKey(lang + "_" + textLang))
			{
				msg.append(_msgServer.get(lang + "_" + textLang));
			}
			else if (_msgServer.containsKey("en_" + textLang))
			{
				msg.append(_msgServer.get("en_" + textLang));
			}
			else
			{
				LOGGER.warning("Lang System failed! Message " + textLang + " not found.");
				msg.append(textLang);
			}
		}
		
		return msg.toString();
	}
	
	/**
	 * Get values of player
	 * @param activeChar
	 * @param lang
	 */
	public void setPlayerLang(L2PcInstance activeChar, String lang)
	{
		if (activeChar != null)
		{
			if (lang == null)
			{
				activeChar.setLang(Config.L2JMOD_MULTILANG_DEFAULT);
			}
			else
			{
				activeChar.setLang(lang);
			}
		}
		
		return;
	}
	
	/**
	 * Get all languages.
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