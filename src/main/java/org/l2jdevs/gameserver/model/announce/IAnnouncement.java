/*
 * Copyright © 2004-2019 L2JDevs
 * 
 * This file is part of L2JDevs.
 * 
 * L2JDevs is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2JDevs is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jdevs.gameserver.model.announce;

import org.l2jdevs.gameserver.model.interfaces.IDeletable;
import org.l2jdevs.gameserver.model.interfaces.IStorable;
import org.l2jdevs.gameserver.model.interfaces.IUpdatable;

/**
 * @author UnAfraid
 */
public interface IAnnouncement extends IStorable, IUpdatable, IDeletable
{
	public String getAuthor();
	
	public String getContent();
	
	public int getId();
	
	public AnnouncementType getType();
	
	public boolean isValid();
	
	public void setAuthor(String author);
	
	public void setContent(String content);
	
	public void setType(AnnouncementType type);
}
