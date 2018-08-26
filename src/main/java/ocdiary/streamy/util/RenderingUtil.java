package ocdiary.streamy.util;

import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.config.GuiUtils;
import ocdiary.streamy.StreamyConfig;
import org.lwjgl.input.Mouse;

import java.awt.*;

/**
 * Created by bright_spark on 26/08/2018.
 */
public class RenderingUtil
{
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static Point getCurrentMousePosition()
    {
        ScaledResolution sr = new ScaledResolution(mc);
        int srHeight = sr.getScaledHeight();
        int mouseX = Mouse.getX() * sr.getScaledWidth() / mc.displayWidth;
        int mouseY = srHeight - Mouse.getY() * srHeight / mc.displayHeight - 1;
        return new Point(mouseX, mouseY);
    }

    public static boolean isMouseOver(int x, int y, int width, int height, Point mousePos)
    {
        int mx = mousePos.x, my = mousePos.y;
        return mx >= x && mx <= (x + width) && my >= y && my <= (y + height);
    }

    public static boolean isMouseOverIcon(Point mousePos)
    {
        return isMouseOver(StreamyConfig.ICON.posX, StreamyConfig.ICON.posY, StreamyConfig.ICON.iconSize.size, StreamyConfig.ICON.iconSize.size, mousePos);
    }

    public static boolean isValidGuiForRendering()
    {
        GuiScreen gui = mc.currentScreen;
        return gui == null || gui instanceof GuiMainMenu || gui instanceof GuiIngameMenu || gui instanceof GuiChat || gui instanceof InventoryEffectRenderer;
    }

    public static void drawTooltipBoxBackground(int x, int y, int width, int height, int zLevel)
    {
        int backgroundColor = 0xF0100010;
        int borderColorStart = 0x505000FF;
        int borderColorEnd = (borderColorStart & 0xFEFEFE) >> 1 | borderColorStart & 0xFF000000;
        GuiUtils.drawGradientRect(zLevel, x - 3, y - 4, x + width + 3, y - 3, backgroundColor, backgroundColor);
        GuiUtils.drawGradientRect(zLevel, x - 3, y + height + 3, x + width + 3, y + height + 4, backgroundColor, backgroundColor);
        GuiUtils.drawGradientRect(zLevel, x - 3, y - 3, x + width + 3, y + height + 3, backgroundColor, backgroundColor);
        GuiUtils.drawGradientRect(zLevel, x - 4, y - 3, x - 3, y + height + 3, backgroundColor, backgroundColor);
        GuiUtils.drawGradientRect(zLevel, x + width + 3, y - 3, x + width + 4, y + height + 3, backgroundColor, backgroundColor);
        GuiUtils.drawGradientRect(zLevel, x - 3, y - 3 + 1, x - 3 + 1, y + height + 3 - 1, borderColorStart, borderColorEnd);
        GuiUtils.drawGradientRect(zLevel, x + width + 2, y - 3 + 1, x + width + 3, y + height + 3 - 1, borderColorStart, borderColorEnd);
        GuiUtils.drawGradientRect(zLevel, x - 3, y - 3, x + width + 3, y - 3 + 1, borderColorStart, borderColorStart);
        GuiUtils.drawGradientRect(zLevel, x - 3, y + height + 2, x + width + 3, y + height + 3, borderColorEnd, borderColorEnd);

        MinecraftForge.EVENT_BUS.post(new RenderTooltipEvent.PostBackground(ItemStack.EMPTY, Lists.newArrayList(), x, y, mc.fontRenderer, width, height));
    }
}
