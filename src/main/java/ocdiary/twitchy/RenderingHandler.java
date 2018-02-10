package ocdiary.twitchy;

import com.google.common.collect.Lists;
import io.netty.util.internal.StringUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import ocdiary.twitchy.util.*;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = Twitchy.MODID, value = Side.CLIENT)
public class RenderingHandler
{

    private static final ResourceLocation TWITCH_ICON = new ResourceLocation(Twitchy.MODID, "textures/gui/twitch.png");
    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final int BORDER = 2; //TODO adjust
    private static final int PROFILE_PIC_ORIGINAL_SIZE = 300;
    private static final int PROFILE_PIC_NEW_SIZE = 12;

    private static boolean expandList = false; //TODO move to config to save value?

    private static void drawIcon()
    {
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.enableBlend();
        mc.getTextureManager().bindTexture(TWITCH_ICON);
        EnumIconSize iconSize = TwitchyConfig.ICON.iconSize;
        GuiUtils.drawTexturedModalRect(TwitchyConfig.ICON.posX, TwitchyConfig.ICON.posY, iconSize.outlineU, iconSize.outlineV, iconSize.width, iconSize.height, 0);
        if(Twitchy.isLive) GuiUtils.drawTexturedModalRect(TwitchyConfig.ICON.posX, TwitchyConfig.ICON.posY, iconSize.overlayU, iconSize.overlayV, iconSize.width, iconSize.height, 0);
    }

    private static void drawStreamInfo(int x, int y, int mouseX, int mouseY, StreamInfo info, boolean showPreview, int maxTextWidth) {
        if(showPreview && info.streaming) {
            String url = info.previewUrl;
            EnumPreviewSize quality = TwitchyConfig.quality;
            if(!StringUtil.isNullOrEmpty(url)) {
                GlStateManager.pushMatrix();
                int zLevel = 300; //300 is minimum as vanilla inventory items are rendered at that level and we want to render above these.
                GlStateManager.translate(0.0F, 0.0F, zLevel);

                ResourceLocation preview;
                if(!ImageUtil.previews.containsKey(url))
                    preview = ImageUtil.loadImage(url, info.broadcaster, ImageUtil.ImageCacheType.PREVIEW);
                else
                    preview = ImageUtil.previews.get(url);
                mc.getTextureManager().bindTexture(preview);

                Gui.drawScaledCustomSizeModalRect(x, y, 0, 0, quality.width, quality.height, TwitchyConfig.previewWidth, TwitchyConfig.previewHeight, quality.width, quality.height);
                GlStateManager.popMatrix();
            }
            GuiUtils.drawHoveringText(Lists.newArrayList(), mouseX, mouseY + 15 + quality.height, mc.displayWidth, mc.displayHeight, Math.min(maxTextWidth, quality.width) + BORDER, mc.fontRenderer);
        }
        else {
            List<String> tooltips = new ArrayList<>();
            Lists.newArrayList(I18n.format("twitchy.tooltip.broadcaster", TextFormatting.AQUA + info.broadcaster + TextFormatting.RESET.toString()));
            if(!info.streaming) {
                tooltips.addAll(Lists.newArrayList(
                        I18n.format("twitchy.tooltip.offline", TextFormatting.RED + info.broadcaster + TextFormatting.RESET.toString()),
                        ""));
            }
            else {
                tooltips.addAll(Lists.newArrayList(
                        I18n.format("twitchy.tooltip.broadcaster", TextFormatting.AQUA + info.broadcaster + TextFormatting.RESET.toString()),
                        I18n.format("twitchy.tooltip.title", TextFormatting.BLUE.toString() + info.title + TextFormatting.RESET.toString()),
                        I18n.format("twitchy.tooltip.game", TextFormatting.DARK_GREEN.toString() + info.game + TextFormatting.RESET.toString()),
                        I18n.format("twitchy.tooltip.viewers", TextFormatting.DARK_RED.toString() + info.viewers + TextFormatting.RESET.toString()),
                        "",
                        I18n.format("twitchy.tooltip.preview", TextFormatting.GOLD.toString() + "SHIFT" + TextFormatting.RESET.toString())
                ));
            }
            tooltips.add(TextFormatting.GRAY + I18n.format("twitchy.tooltip.watch", TextFormatting.WHITE.toString() + info.broadcaster + TextFormatting.GRAY.toString()) + TextFormatting.RESET);
            GuiUtils.drawHoveringText(tooltips, mouseX, mouseY + 20, mc.displayWidth, mc.displayHeight, maxTextWidth, mc.fontRenderer);
        }
    }

    private static String getPlayerStreamerName()
    {
        String username = TwitchyConfig.CHANNELS.streamerModeNameOverride;
        if(StringUtils.isNullOrEmpty(username))
            username = mc.player.getName();
        return username;
    }

    private static boolean streamerFilter(StreamInfo streamer)
    {
        //Do not show channel if showOfflineChannels == false and streamer is offline
        if(!TwitchyConfig.CHANNELS.showOfflineChannels && !streamer.streaming)
            return false;
        //If streamerMode == OFF, then show streamer
        if(TwitchyConfig.CHANNELS.streamerMode == EnumStreamerMode.OFF)
            return true;
        //If username is equal to the streamer name, then don't show
        return !streamer.broadcaster.equalsIgnoreCase(getPlayerStreamerName());
    }

    @SubscribeEvent
    public static void drawScreen(TickEvent.RenderTickEvent event)
    {
        if(!TwitchyConfig.enabled || event.phase != TickEvent.Phase.END || TwitchyConfig.CHANNELS.streamerMode == EnumStreamerMode.FULL) return;
        if(ImageUtil.shouldReloadPreviews) ImageUtil.clearPreviewCache();
        Point mousePos = getCurrentMousePosition();
        int mouseX = mousePos.x;
        int mouseY = mousePos.y;
        EnumIconSize icon = TwitchyConfig.ICON.iconSize;
        int maxTextWidth = new ScaledResolution(mc).getScaledWidth() - mouseX - 16;
        if (Twitchy.isLive || TwitchyConfig.ICON.iconState == EnumIconVisibility.ALWAYS) {
            drawIcon(); //draw the twitch icon

            if (expandList) {
                int x = TwitchyConfig.ICON.posX;
                int y = TwitchyConfig.ICON.posY + icon.height + BORDER * 3;
                synchronized (Twitchy.LIVE_STREAMERS) {
                    int localX = x + BORDER + 2;
                    Map<String, StreamInfo> streamers = Twitchy.LIVE_STREAMERS.entrySet().stream()
                                    .filter(entry -> streamerFilter(entry.getValue()))
                                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                    List<String> broadcasters = Lists.newArrayList(streamers.keySet());
                    if(TwitchyConfig.CHANNELS.sortChannels) broadcasters.sort(String::compareToIgnoreCase);

                    drawTooltipBoxBackground(localX + BORDER / 2, y + BORDER / 2, PROFILE_PIC_NEW_SIZE - BORDER, PROFILE_PIC_NEW_SIZE * streamers.size() + (streamers.size() - 1) * 3 - BORDER, 0);

                    for (int i = 0; i < broadcasters.size(); i++) {
                        String broadcaster = broadcasters.get(i);
                        int localY = y + (PROFILE_PIC_NEW_SIZE + 3) * i;
                        StreamInfo info = streamers.get(broadcaster);
                        ResourceLocation profilePic;
                        if(!ImageUtil.profiles.containsKey(info.profilePicUrl))
                            profilePic = ImageUtil.loadImage(info.profilePicUrl, broadcaster, ImageUtil.ImageCacheType.PROFILE);
                        else
                            profilePic = ImageUtil.profiles.get(info.profilePicUrl);
                        mc.renderEngine.bindTexture(profilePic);
                        Gui.drawScaledCustomSizeModalRect(localX, localY, 0, 0, PROFILE_PIC_ORIGINAL_SIZE, PROFILE_PIC_ORIGINAL_SIZE, PROFILE_PIC_NEW_SIZE, PROFILE_PIC_NEW_SIZE, PROFILE_PIC_ORIGINAL_SIZE, PROFILE_PIC_ORIGINAL_SIZE);
                    }

                    //important: need to draw the tooltip AFTER all icons have been drawn
                    for (int i = 0; i < broadcasters.size(); i++) {
                        String broadcaster = broadcasters.get(i);
                        int localY = y + (PROFILE_PIC_NEW_SIZE + 3) * i;
                        if (isMouseOver(localX, localY, PROFILE_PIC_NEW_SIZE, PROFILE_PIC_NEW_SIZE, mouseX, mouseY)) {
                            StreamInfo info = streamers.get(broadcaster);
                            drawStreamInfo(localX, localY, mouseX, mouseY, info, GuiScreen.isShiftKeyDown(), maxTextWidth);
                        }
                    }
                }
            }
        }
        if (isMouseOver(TwitchyConfig.ICON.posX, TwitchyConfig.ICON.posY, icon.width, icon.height, mouseX, mouseY)) {
            String key = expandList ? "collapse" : "expand";
            List<String> tooltips = Lists.newArrayList(
                    new TextComponentTranslation("twitchy.tooltip." + key).setStyle(new Style().setColor(TextFormatting.AQUA)).getFormattedText(),
                    new TextComponentTranslation("twitchy.tooltip.info", TextFormatting.BLUE.toString() + "ALT + Click").getFormattedText()
            );
            GuiUtils.drawHoveringText(tooltips, mousePos.x, mousePos.y + 5, mc.displayWidth, mc.displayHeight, maxTextWidth, mc.fontRenderer);
        }
    }

    private static void drawTooltipBoxBackground(int x, int y, int width, int height, int zLevel) {
        int backgroundColor = 0xF0100010;
        int borderColorStart = 0x505000FF;
        int borderColorEnd = (borderColorStart & 0xFEFEFE) >> 1 | borderColorStart & 0xFF000000;
        RenderTooltipEvent.Color colorEvent = new RenderTooltipEvent.Color(ItemStack.EMPTY, Lists.newArrayList(), x, y, mc.fontRenderer, backgroundColor, borderColorStart, borderColorEnd);
        MinecraftForge.EVENT_BUS.post(colorEvent);
        backgroundColor = colorEvent.getBackground();
        borderColorStart = colorEvent.getBorderStart();
        borderColorEnd = colorEvent.getBorderEnd();
        GuiUtils.drawGradientRect(zLevel, x - 3, y - 4, x + width + 3, y - 3, backgroundColor, backgroundColor);
        GuiUtils.drawGradientRect(zLevel, x - 3, y + height + 3, x + width + 3, y + height + 4, backgroundColor, backgroundColor);
        GuiUtils.drawGradientRect(zLevel, x - 3, y - 3, x + width + 3, y + height + 3, backgroundColor, backgroundColor);
        GuiUtils.drawGradientRect(zLevel, x - 4, y - 3, x - 3, y + height + 3, backgroundColor, backgroundColor);
        GuiUtils.drawGradientRect(zLevel, x + width + 3, y - 3, x + width + 4, y + height + 3, backgroundColor, backgroundColor);
        GuiUtils.drawGradientRect(zLevel, x - 3, y - 3 + 1, x - 3 + 1, y + height + 3 - 1, borderColorStart, borderColorEnd);
        GuiUtils.drawGradientRect(zLevel, x + width + 2, y - 3 + 1, x + width + 3, y + height + 3 - 1, borderColorStart, borderColorEnd);
        GuiUtils.drawGradientRect(zLevel, x - 3, y - 3, x + width + 3, y - 3 + 1, borderColorStart, borderColorStart);
        GuiUtils.drawGradientRect(zLevel, x - 3, y + height + 2, x + width + 3, y + height + 3, borderColorEnd, borderColorEnd);

        MinecraftForge.EVENT_BUS.post(new RenderTooltipEvent.PostBackground(ItemStack.EMPTY, colorEvent.getLines(), x, y, colorEvent.getFontRenderer(), width, height));

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
        if(!TwitchyConfig.enabled || TwitchyConfig.CHANNELS.streamerMode == EnumStreamerMode.FULL) return;
        if (Mouse.getEventButton() == 0 && Mouse.getEventButtonState()) {
            Point mousePos = getCurrentMousePosition();
            if (isMouseOver(TwitchyConfig.ICON.posX, TwitchyConfig.ICON.posY, TwitchyConfig.ICON.iconSize.width, TwitchyConfig.ICON.iconSize.height, mousePos.x, mousePos.y)) {
                if(GuiScreen.isAltKeyDown()) mc.displayGuiScreen(FMLClientHandler.instance().getGuiFactoryFor(FMLCommonHandler.instance().findContainerFor(Twitchy.MODID)).createConfigGui(mc.currentScreen));
                else expandList = !expandList;
            }
            if(expandList && Twitchy.isLive) {
                int i = 0;
                int y = TwitchyConfig.ICON.posY + TwitchyConfig.ICON.iconSize.height + BORDER * 3;
                Map<String, StreamInfo> streamInfo = Twitchy.LIVE_STREAMERS.entrySet().stream()
                        .filter(entry -> streamerFilter(entry.getValue()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                List<String> streamers = new ArrayList<>(streamInfo.keySet());
                if(TwitchyConfig.CHANNELS.sortChannels) streamers.sort(String::compareToIgnoreCase);
                for(String broadcaster : streamers) {
                    int localX = TwitchyConfig.ICON.posX + BORDER + 2;
                    int localY = y + (PROFILE_PIC_NEW_SIZE + 3) * i++;
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
