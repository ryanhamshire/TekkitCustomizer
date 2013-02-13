/*
    TekkitCustomizer Server Plugin for Minecraft
    Copyright (C) 2012 Ryan Hamshire

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
 
package me.ryanhamshire.TekkitCustomizer;

import java.util.ArrayList;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

//this main thread task occassionally scans player inventories for banned items
//and the world for banned blocks
//this scan runs once per minute
class ContrabandScannerTask implements Runnable 
{
	private int nextChunkPercentile = 0;
	private int nextPlayerPercentile = 0;
	
	@Override
	public void run()
	{
		//check player inventories
		if(TekkitCustomizer.instance.config_ownershipBanned.size() > 0)
		{
			Server server = TekkitCustomizer.instance.getServer();
			Player [] players = server.getOnlinePlayers();
			if(players.length == 0) return;
			
			//scan 5% of players each pass
			int firstPlayer = (int)(players.length * (nextPlayerPercentile / 100f));
			int lastPlayer = (int)(players.length * ((nextPlayerPercentile + 5) / 100f));
			
			if(lastPlayer == firstPlayer) lastPlayer = players.length;
			
			//for each player to be scanned
			for(int j = firstPlayer; j < lastPlayer; j++)
			{
				Player player = players[j];
				
				//scan all this player's inventory for contraband items
				PlayerInventory inventory = player.getInventory();
				for(int i = 0; i < inventory.getSize(); i++)
				{
					ItemStack itemStack = inventory.getItem(i);
					if(itemStack == null) continue;
					
					MaterialInfo bannedInfo = TekkitCustomizer.instance.isBanned(ActionType.Ownership, player, itemStack.getTypeId(), itemStack.getData().getData(), player.getLocation());
					if(bannedInfo != null)
					{
						inventory.setItem(i, new ItemStack(Material.AIR));
						TekkitCustomizer.AddLogEntry("Confiscated " + bannedInfo.toString() + " from " + player.getName() + ".");
					}
				}
				
				ItemStack [] armor = inventory.getArmorContents();
				for(int i = 0; i < armor.length; i++)
				{
					ItemStack itemStack = armor[i];
					if(itemStack == null) continue;
					
					MaterialInfo bannedInfo = TekkitCustomizer.instance.isBanned(ActionType.Ownership, player, itemStack.getTypeId(), itemStack.getData().getData(), player.getLocation());
					if(bannedInfo != null)
					{
						itemStack.setType(Material.AIR);
						itemStack.setAmount(0);
						armor[i] = itemStack;
						TekkitCustomizer.AddLogEntry("Confiscated " + bannedInfo.toString() + " from " + player.getName() + ".");
					}
				}
				
				inventory.setArmorContents(armor);
			}
		}
		
		nextPlayerPercentile += 5;
		if(nextPlayerPercentile >= 100) nextPlayerPercentile = 0;	
	}	
}
