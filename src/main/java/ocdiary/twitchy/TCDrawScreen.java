package ocdiary.twitchy;

import com.google.common.collect.Lists;
import io.netty.util.internal.StringUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import ocdiary.twitchy.util.EnumIconSize;
import ocdiary.twitchy.util.ImageUtil;
import ocdiary.twitchy.util.StreamInfo;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@Mod.EventBusSubscriber(modid = Twitchy.MODID, value = Side.CLIENT)
public class TCDrawScreen {

    private static final ResourceLocation TWITCH_ICON = new ResourceLocation(Twitchy.MODID, "textures/gui/twitch.png");
    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final int BORDER = 2; //TODO adjust
    private static final int PROFILE_PIC_ORIGINAL_SIZE = 300;
    private static final int PROFILE_PIC_NEW_SIZE = 8;

    private static boolean expandList = false; //TODO move to config to save value?

    private static void drawIcon()
    {
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.enableBlend();
        mc.getTextureManager().bindTexture(TWITCH_ICON);
        EnumIconSize iconSize = TCConfig.ICON.iconSize;
        GuiUtils.drawTexturedModalRect(TCConfig.ICON.posX, TCConfig.ICON.posY, iconSize.outlineU, iconSize.outlineV, iconSize.width, iconSize.height, 0);
        if(Twitchy.isLive) GuiUtils.drawTexturedModalRect(TCConfig.ICON.posX, TCConfig.ICON.posY, iconSize.overlayU, iconSize.overlayV, iconSize.width, iconSize.height, 0);
    }

    private static void drawStreamInfo(int x, int y, int mouseX, int mouseY, StreamInfo info, boolean showPreview, int maxTextWidth) {
        if(showPreview) {
            String url = info.previewUrl;
            if(!StringUtil.isNullOrEmpty(url)) {
                GlStateManager.pushMatrix();
                int zLevel = 300; //300 is minimum as vanilla inventory items are rendered at that level and we want to render above these.
                GlStateManager.translate(0.0F, 0.0F, zLevel);

                ResourceLocation preview;
                if(!ImageUtil.previews.containsKey(url)) {
                    preview = ImageUtil.loadImage(url, info.broadcaster + "_preview");
                }
                else preview = ImageUtil.previews.get(url);
                mc.getTextureManager().bindTexture(preview);

                Gui.drawScaledCustomSizeModalRect(x, y, 0, 0, Twitchy.previewWidth, Twitchy.previewHeight, TCConfig.previewWidth, TCConfig.previewHeight, Twitchy.previewWidth, Twitchy.previewHeight);
                GlStateManager.popMatrix();
            }
            GuiUtils.drawHoveringText(Lists.newArrayList(), mouseX, mouseY + 15 + Twitchy.previewHeight, mc.displayWidth, mc.displayHeight, Math.min(maxTextWidth, Twitchy.previewWidth) + BORDER, mc.fontRenderer);
        }
        else {
            List<String> tooltips = Lists.newArrayList(
                    I18n.format("twitchy.tooltip.broadcaster", TextFormatting.AQUA + info.broadcaster + TextFormatting.RESET.toString()),
                    I18n.format("twitchy.tooltip.title", TextFormatting.BLUE.toString() + info.title + TextFormatting.RESET.toString()),
                    I18n.format("twitchy.tooltip.game", TextFormatting.DARK_GREEN.toString() + info.game + TextFormatting.RESET.toString()),
                    I18n.format("twitchy.tooltip.viewers", TextFormatting.DARK_RED.toString() + info.viewers + TextFormatting.RESET.toString()),
                    "",
                    TextFormatting.GRAY + I18n.format("twitchy.tooltip.watch", TextFormatting.WHITE.toString() + info.broadcaster + TextFormatting.GRAY.toString()) + TextFormatting.RESET,
                    I18n.format("twitchy.tooltip.preview", TextFormatting.GOLD.toString() + "SHIFT" + TextFormatting.RESET.toString())
            );
            GuiUtils.drawHoveringText(tooltips, mouseX, mouseY + 20, mc.displayWidth, mc.displayHeight, maxTextWidth, mc.fontRenderer);
        }
    }

    @SubscribeEvent
    public static void drawScreen(TickEvent.RenderTickEvent event)
    {
        if(event.phase != TickEvent.Phase.END) return;
        if(ImageUtil.shouldReloadPreviews) ImageUtil.clearPreviewCache();
        Point mousePos = getCurrentMousePosition();
        int mouseX = mousePos.x;
        int mouseY = mousePos.y;
        EnumIconSize icon = TCConfig.ICON.iconSize;
        int maxTextWidth = new ScaledResolution(mc).getScaledWidth() - mouseX - 16;
        if (Twitchy.isLive || TCConfig.ICON.iconState == TCConfig.Icon.State.ALWAYS) {
            drawIcon(); //draw the twitch icon

            if (Twitchy.isLive && expandList) {
                int x = TCConfig.ICON.posX;
                int y = TCConfig.ICON.posY + icon.height + BORDER;
                //TODO draw background

                synchronized (Twitchy.LIVE_STREAMERS) {
                    int i = 0;
                    for (String broadcaster : Twitchy.LIVE_STREAMERS.keySet()) {
                        int localX = x + BORDER + 2;
                        int localY = y + (PROFILE_PIC_NEW_SIZE + 2) * i++;
                        StreamInfo info = Twitchy.LIVE_STREAMERS.get(broadcaster);
                        ResourceLocation profilePic = ImageUtil.loadImage(info.profilePicUrl, broadcaster + "_profile");
                        mc.renderEngine.bindTexture(profilePic);
                        Gui.drawScaledCustomSizeModalRect(localX, localY, 0, 0, PROFILE_PIC_ORIGINAL_SIZE, PROFILE_PIC_ORIGINAL_SIZE, PROFILE_PIC_NEW_SIZE, PROFILE_PIC_NEW_SIZE, PROFILE_PIC_ORIGINAL_SIZE, PROFILE_PIC_ORIGINAL_SIZE);
                    }

                    i = 0;
                    //important: need to draw the tooltip AFTER all icons have been drawn
                    for (String broadcaster : Twitchy.LIVE_STREAMERS.keySet()) {
                        int localX = x + BORDER + 2;
                        int localY = y + (PROFILE_PIC_NEW_SIZE + 2) * i++;
                        if (isMouseOver(localX, localY, PROFILE_PIC_NEW_SIZE, PROFILE_PIC_NEW_SIZE, mouseX, mouseY)) {
                            StreamInfo info = Twitchy.LIVE_STREAMERS.get(broadcaster);
                            drawStreamInfo(localX, localY, mouseX, mouseY, info, GuiScreen.isShiftKeyDown(), maxTextWidth);
                        }
                    }
                }
            }
        }
        if (isMouseOver(TCConfig.ICON.posX, TCConfig.ICON.posY, icon.width, icon.height, mouseX, mouseY)) {
            String key = expandList ? "collapse" : "expand";
            List<String> tooltips = Lists.newArrayList(
                    new TextComponentTranslation("twitchy.tooltip." + key).setStyle(new Style().setColor(TextFormatting.AQUA)).getFormattedText(),
                    new TextComponentTranslation("twitchy.tooltip.info", TextFormatting.BLUE.toString() + "ALT + Click").getFormattedText()
            );
            GuiUtils.drawHoveringText(tooltips, mousePos.x, mousePos.y + 5, mc.displayWidth, mc.displayHeight, maxTextWidth, mc.fontRenderer);
        }
    }

    private static boolean isMouseOver(int x, int y, int width, int height, int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= (x + width) && mouseY >= y && mouseY <= (y + height);
    }

    private static Point getCurrentMousePosition() {
        ScaledResolution sr = new ScaledResolution(mc);
        int srHeight = sr.getScaledHeight();
        int mouseX = Mouse.getX() * sr.getScaledWidth() / mc.displayWidth;
        int mouseY = srHeight - Mouse.getY() * srHeight / mc.displayHeight - 1;
        return new Point(mouseX, mouseY);
    }

    @SubscribeEvent
    public static void mouseClick(GuiScreenEvent.MouseInputEvent.Pre event)
    {
        if (Mouse.getEventButton() == 0 && Mouse.getEventButtonState()) {
            Point mousePos = getCurrentMousePosition();
            if (isMouseOver(TCConfig.ICON.posX, TCConfig.ICON.posY, TCConfig.ICON.iconSize.width, TCConfig.ICON.iconSize.height, mousePos.x, mousePos.y)) {
                if(GuiScreen.isAltKeyDown()) mc.displayGuiScreen(FMLClientHandler.instance().getGuiFactoryFor(FMLCommonHandler.instance().findContainerFor(Twitchy.MODID)).createConfigGui(mc.currentScreen));
                else expandList = !expandList;
            }
            if(expandList && Twitchy.isLive) {
                int i = 0;
                int y = TCConfig.ICON.posY + TCConfig.ICON.iconSize.height + BORDER;
                for(String broadcaster : Twitchy.LIVE_STREAMERS.keySet()) {
                    int localX = TCConfig.ICON.posX + BORDER + 2;
                    int localY = y + (PROFILE_PIC_NEW_SIZE + 2) * i++;
                    if(isMouseOver(localX, localY, PROFILE_PIC_NEW_SIZE, PROFILE_PIC_NEW_SIZE, mousePos.x, mousePos.y)) {
                        openTwitchStream(broadcaster);
                    }
                }
            }
        }
    }

    private static void openTwitchStream(String channel) {
        String url = "https://twitch.tv/" + channel;
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            }
            else {
                Twitchy.LOGGER.error("Can't open browser - Desktop not supported!");
            }
        } catch (URISyntaxException e) {
            Twitchy.LOGGER.error("URL \"" + url + "\" is invalid. Report this to the mod author!");
        } catch (Exception e) {
            Twitchy.LOGGER.error("Can't open browser", e);
        }
    }

}
