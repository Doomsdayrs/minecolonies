package com.minecolonies.items;

import com.minecolonies.colony.CitizenData;
import com.minecolonies.colony.Colony;
import com.minecolonies.colony.ColonyManager;
import com.minecolonies.colony.buildings.AbstractBuilding;
import com.minecolonies.colony.buildings.BuildingGuardTower;
import com.minecolonies.util.BlockPosUtil;
import com.minecolonies.util.LanguageHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

/**
 * Guard Scepter Item class. Used to give tasks to guards.
 */
public class ItemScepterGuard extends AbstractItemMinecolonies
{
    /**
     * The compound tag for the last pos the tool has been clicked.
     */
    private static final String TAG_LAST_POS = "lastPos";

    /**
     * Caliper constructor. Sets max stack to 1, like other tools.
     */
    public ItemScepterGuard()
    {
        super("scepterGuard");
        this.setMaxDamage(2);

        maxStackSize = 1;
    }

    @NotNull
    @Override
    public EnumActionResult onItemUse(ItemStack scepter, EntityPlayer playerIn, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
    {
        //todo watch how interaction with server is, might facilitate this.
        // if server world, do nothing
        if (worldIn.isRemote)
        {
            return EnumActionResult.FAIL;
        }

        if (!scepter.hasTagCompound())
        {
            scepter.setTagCompound(new NBTTagCompound());
        }
        NBTTagCompound compound = scepter.getTagCompound();

        if(compound.hasKey(TAG_LAST_POS))
        {
            BlockPos lastPos = BlockPosUtil.readFromNBT(compound, TAG_LAST_POS);
            if(lastPos.equals(pos))
            {
                playerIn.inventory.removeStackFromSlot(playerIn.inventory.currentItem);
                LanguageHandler.sendPlayerMessage(playerIn, LanguageHandler.format("com.minecolonies.job.guard.toolDoubleClick"));
                return EnumActionResult.FAIL;
            }
        }
        return handleItemUsage(worldIn, pos, compound, playerIn);
    }

    /**
     * Handles the usage of the item.
     * @param worldIn the world it is used in.
     * @param pos the position.
     * @param compound the compound.
     * @param playerIn the player using it.
     * @return if it has been successful.
     */
    @NotNull
    private static EnumActionResult handleItemUsage(World worldIn, BlockPos pos, NBTTagCompound compound, EntityPlayer playerIn)
    {
        Colony colony = ColonyManager.getClosestColony(worldIn, pos);
        if(colony == null)
        {
            return EnumActionResult.FAIL;
        }

        BlockPos guardTower = BlockPosUtil.readFromNBT(compound, "pos");
        AbstractBuilding hut = colony.getBuilding(guardTower);
        if(hut == null || !(hut instanceof BuildingGuardTower))
        {
            return EnumActionResult.FAIL;
        }

        BuildingGuardTower.Task task = BuildingGuardTower.Task.values()[compound.getInteger("task")];
        final CitizenData citizen = ((BuildingGuardTower) hut).getWorker();

        String name = "";
        if(citizen != null)
        {
            name = " " + citizen.getName();
        }

        if(task.equals(BuildingGuardTower.Task.GUARD))
        {
            LanguageHandler.sendPlayerMessage(playerIn, LanguageHandler.format("com.minecolonies.job.guard.toolClickGuard", pos, name));
            ((BuildingGuardTower) hut).setGuardTarget(pos);
            playerIn.inventory.removeStackFromSlot(playerIn.inventory.currentItem);
        }
        else
        {
            if(!compound.hasKey(TAG_LAST_POS))
            {
                ((BuildingGuardTower) hut).resetPatrolTargets();
            }
            ((BuildingGuardTower) hut).addPatrolTargets(pos);
            LanguageHandler.sendPlayerMessage(playerIn, LanguageHandler.format("com.minecolonies.job.guard.toolClickPatrol", pos, name));
        }
        BlockPosUtil.writeToNBT(compound, TAG_LAST_POS, pos);

        return EnumActionResult.SUCCESS;
    }
}
