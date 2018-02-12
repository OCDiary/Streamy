package ocdiary.twitchy;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Keyboard;

@Mod.EventBusSubscriber(modid = Twitchy.MODID, value = Side.CLIENT)
public class KeyHandler {

    public static KeyBinding keyDismiss = new KeyBinding("key.twitchy.dismiss", Keyboard.KEY_Z, "key.twitchy.category");

    @SubscribeEvent
    public static void onKey(InputEvent event) {
        if (keyDismiss.isPressed()) {
            Twitchy.isIconDismissed = !Twitchy.isIconDismissed;
        }
    }
}
