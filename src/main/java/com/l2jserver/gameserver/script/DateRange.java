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
package com.l2jserver.gameserver.script;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Luis Arias, Sacrifice
 */
public class DateRange
{
	private static final Logger LOG = LoggerFactory.getLogger(DateRange.class);
	
	private final Date startDate, endDate;
	
	public DateRange(Date from, Date to)
	{
		startDate = from;
		endDate = to;
	}
	
	public static DateRange parse(String dateRange, DateFormat format)
	{
		final String[] date = dateRange.split("-");
		if (date.length == 2)
		{
			try
			{
				final Date start = format.parse(date[0]);
				final Date end = format.parse(date[1]);
				return new DateRange(start, end);
			}
			catch (ParseException e)
			{
				LOG.warn("Invalid Date Format. {}", e);
			}
		}
		return new DateRange(null, null);
	}
	
	public Date getEndDate()
	{
		return endDate;
	}
	
	public Date getStartDate()
	{
		return startDate;
	}
	
	public boolean isValid()
	{
		return (startDate != null) && (endDate != null) && startDate.before(endDate);
	}
	
	public boolean isWithinRange(Date date)
	{
		return (date.equals(startDate) || date.after(startDate)) //
			&& (date.equals(endDate) || date.before(endDate));
	}
	
	@Override
	public String toString()
	{
		return "DateRange: From: " + getStartDate() + " To: " + getEndDate();
	}
}