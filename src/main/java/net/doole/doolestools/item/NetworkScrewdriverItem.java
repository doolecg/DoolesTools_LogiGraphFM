package net.doole.doolestools.item;

import net.doole.doolestools.util.NetworkDismantle;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public class NetworkScrewdriverItem extends Item {
    public NetworkScrewdriverItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (NetworkDismantle.tryDismantle(context.getLevel(), context.getClickedPos(), context.getPlayer(), context.getItemInHand())) {
            return InteractionResult.SUCCESS;
        }
        return super.useOn(context);
    }
}
