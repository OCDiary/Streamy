package ocdiary.twitchy;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = Twitchy.MODID, value = Side.CLIENT)
public class KeyHandler {
    @SubscribeEvent
    public static void onKey(InputEvent event) {
        if (Twitchy.keyDismiss.isPressed()) {
            Twitchy.isIconDismissed = !Twitchy.isIconDismissed;
        }
    }
}
