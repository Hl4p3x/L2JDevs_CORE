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
package org.l2jdevs.gameserver.model;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.l2jdevs.gameserver.datatables.LanguageData;
import org.l2jdevs.gameserver.enums.PetitionState;
import org.l2jdevs.gameserver.enums.PetitionType;
import org.l2jdevs.gameserver.idfactory.IdFactory;
import org.l2jdevs.gameserver.instancemanager.PetitionManager;
import org.l2jdevs.gameserver.model.actor.instance.L2PcInstance;
import org.l2jdevs.gameserver.network.SystemMessageId;
import org.l2jdevs.gameserver.network.serverpackets.CreatureSay;
import org.l2jdevs.gameserver.network.serverpackets.L2GameServerPacket;
import org.l2jdevs.gameserver.network.serverpackets.PetitionVotePacket;
import org.l2jdevs.gameserver.network.serverpackets.SystemMessage;

/**
 * Petition
 * @author xban1x
 */
public final class Petition
{
	private final long _submitTime = System.currentTimeMillis();
	private final int _id;
	private final PetitionType _type;
	private PetitionState _state = PetitionState.PENDING;
	private final String _content;
	private final List<CreatureSay> _messageLog = new CopyOnWriteArrayList<>();
	private final L2PcInstance _petitioner;
	private L2PcInstance _responder;
	
	public Petition(L2PcInstance petitioner, String petitionText, int petitionType)
	{
		_id = IdFactory.getInstance().getNextId();
		_type = PetitionType.values()[--petitionType];
		_content = petitionText;
		_petitioner = petitioner;
	}
	
	public boolean addLogMessage(CreatureSay cs)
	{
		return _messageLog.add(cs);
	}
	
	public boolean endPetitionConsultation(PetitionState endState)
	{
		setState(endState);
		
		if ((getResponder() != null) && getResponder().isOnline())
		{
			if (endState == PetitionState.RESPONDER_REJECT)
			{
				getPetitioner().sendMessage(LanguageData.getInstance().getMsg(getPetitioner(), "petition_fail"));
			}
			else
			{
				// Ending petition consultation with <Player>.
				SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.PETITION_ENDED_WITH_C1);
				sm.addString(getPetitioner().getName());
				getResponder().sendPacket(sm);
				
				if (endState == PetitionState.PETITIONER_CANCEL)
				{
					// Receipt No. <ID> petition cancelled.
					sm = SystemMessage.getSystemMessage(SystemMessageId.RECENT_NO_S1_CANCELED);
					sm.addInt(getId());
					getResponder().sendPacket(sm);
				}
			}
		}
		
		// End petition consultation and inform them, if they are still online. And if petitioner is online, enable Evaluation button
		if ((getPetitioner() != null) && getPetitioner().isOnline())
		{
			getPetitioner().sendPacket(SystemMessageId.THIS_END_THE_PETITION_PLEASE_PROVIDE_FEEDBACK);
			getPetitioner().sendPacket(PetitionVotePacket.STATIC_PACKET);
		}
		
		PetitionManager.getInstance().getCompletedPetitions().put(getId(), this);
		return (PetitionManager.getInstance().getPendingPetitions().remove(getId()) != null);
	}
	
	public String getContent()
	{
		return _content;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public List<CreatureSay> getLogMessages()
	{
		return _messageLog;
	}
	
	public L2PcInstance getPetitioner()
	{
		return _petitioner;
	}
	
	public L2PcInstance getResponder()
	{
		return _responder;
	}
	
	public PetitionState getState()
	{
		return _state;
	}
	
	public long getSubmitTime()
	{
		return _submitTime;
	}
	
	public String getTypeAsString()
	{
		return _type.toString().replace("_", " ");
	}
	
	public void sendPetitionerPacket(L2GameServerPacket responsePacket)
	{
		if ((getPetitioner() == null) || !getPetitioner().isOnline())
		{
			// Allows petitioners to see the results of their petition when
			// they log back into the game.
			
			// endPetitionConsultation(PetitionState.Petitioner_Missing);
			return;
		}
		
		getPetitioner().sendPacket(responsePacket);
	}
	
	public void sendResponderPacket(L2GameServerPacket responsePacket)
	{
		if ((getResponder() == null) || !getResponder().isOnline())
		{
			endPetitionConsultation(PetitionState.RESPONDER_MISSING);
			return;
		}
		
		getResponder().sendPacket(responsePacket);
	}
	
	public void setResponder(L2PcInstance respondingAdmin)
	{
		if (getResponder() != null)
		{
			return;
		}
		
		_responder = respondingAdmin;
	}
	
	public void setState(PetitionState state)
	{
		_state = state;
	}
}
