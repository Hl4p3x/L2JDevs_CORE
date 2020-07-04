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
package org.l2jdevs.gameserver.model.items.enchant;

import org.l2jdevs.gameserver.model.StatsSet;
import org.l2jdevs.gameserver.model.items.type.EtcItemType;

/**
 * @author UnAfraid
 */
public final class EnchantSupportItem extends AbstractEnchantItem
{
	private final boolean _isWeapon;
	
	public EnchantSupportItem(StatsSet set)
	{
		super(set);
		_isWeapon = getItem().getItemType() == EtcItemType.SCRL_INC_ENCHANT_PROP_WP;
	}
	
	@Override
	public boolean isWeapon()
	{
		return _isWeapon;
	}
}
