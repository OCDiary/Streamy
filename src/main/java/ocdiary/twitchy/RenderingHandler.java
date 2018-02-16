package ocdiary.twitchy;

import com.google.common.collect.Lists;
import io.netty.util.internal.StringUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Twitchy.MODID, value = Side.CLIENT)
public class RenderingHandler {

    private static final ResourceLocation TWITCH_ICON = new ResourceLocation(Twitchy.MODID, "textures/gui/twitch.png");
    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final int BORDER = 2;
    private static final int PROFILE_PIC_ORIGINAL_SIZE = 300;
    private static final int PROFILE_PIC_NEW_SIZE = 12;
    private static final int PREVIEW_Z_LEVEL = 300; //300 is minimum as vanilla inventory items are rendered at that level and we want to render above these.

    private static boolean expandList = false; //TODO move to config to save value?

    private static void drawIcon() {
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.enableBlend();
        mc.getTextureManager().bindTexture(TWITCH_ICON);
        EnumIconSize iconSize = TwitchyConfig.ICON.iconSize;
        GuiUtils.drawTexturedModalRect(TwitchyConfig.ICON.posX, TwitchyConfig.ICON.posY, iconSize.outlineU, iconSize.outlineV, iconSize.width, iconSize.height, 0);
        if (Twitchy.isLive)
            GuiUtils.drawTexturedModalRect(TwitchyConfig.ICON.posX, TwitchyConfig.ICON.posY, iconSize.overlayU, iconSize.overlayV, iconSize.width, iconSize.height, 0);
    }

    private static void drawStreamInfo(int x, int y, Point mousePos, StreamInfo info, boolean showPreview, int maxTextWidth) {
        int mouseX = mousePos.x, mouseY = mousePos.y;
        if (showPreview && info.streaming) {
            String url = info.previewUrl;
            EnumPreviewSize quality = TwitchyConfig.PREVIEW.quality;
            if (!StringUtil.isNullOrEmpty(url)) {
                GlStateManager.pushMatrix();
                GlStateManager.translate(0.0F, 0.0F, PREVIEW_Z_LEVEL);

                ResourceLocation preview = ImageUtil.loadImage(url, info.broadcaster, ImageUtil.ImageCacheType.LIVE);
                mc.getTextureManager().bindTexture(preview);

                Gui.drawScaledCustomSizeModalRect(x, y, 0, 0, quality.width, quality.height, TwitchyConfig.PREVIEW.previewWidth, TwitchyConfig.PREVIEW.previewHeight, quality.width, quality.height);
                GlStateManager.popMatrix();
            }
            drawHoveringText(Lists.newArrayList(), mouseX, mouseY + 15 + quality.height, mc.displayWidth, mc.displayHeight, Math.min(maxTextWidth, quality.width) + BORDER, mc.fontRenderer);
        } else {
            List<String> tooltips = new ArrayList<>();
            Lists.newArrayList(I18n.format("twitchy.stream.broadcaster", TextFormatting.AQUA + info.broadcaster + TextFormatting.RESET.toString()));
            if (!info.streaming) {
                tooltips.addAll(Lists.newArrayList(
                        I18n.format("twitchy.stream.offline", TextFormatting.RED + info.broadcaster + TextFormatting.RESET.toString()),
                        ""));
            } else {
                tooltips.add(I18n.format("twitchy.stream.broadcaster", TextFormatting.AQUA + info.broadcaster + TextFormatting.RESET.toString()));
                if (!TwitchyConfig.CHANNELS.disableTitle)
                    tooltips.add(I18n.format("twitchy.stream.title", TextFormatting.BLUE.toString() + info.title + TextFormatting.RESET.toString()));
                if (!TwitchyConfig.CHANNELS.disableGame)
                    tooltips.add(I18n.format("twitchy.stream.game", TextFormatting.DARK_GREEN.toString() + info.game + TextFormatting.RESET.toString()));
                if (!TwitchyConfig.CHANNELS.disableViewers)
                    tooltips.add(I18n.format("twitchy.stream.viewers", TextFormatting.DARK_RED.toString() + info.viewers + TextFormatting.RESET.toString()));
                tooltips.add("");
                tooltips.add(I18n.format("twitchy.stream.preview", TextFormatting.GOLD.toString() + "SHIFT" + TextFormatting.RESET.toString()));
            }
            tooltips.add(TextFormatting.GRAY + I18n.format("twitchy.stream.watch", TextFormatting.WHITE.toString() + info.broadcaster + TextFormatting.GRAY.toString()) + TextFormatting.RESET);
            drawHoveringText(tooltips, mouseX, mouseY + 20, mc.displayWidth, mc.displayHeight, maxTextWidth, mc.fontRenderer);
        }
    }

    @SubscribeEvent
    public static void drawScreen(TickEvent.RenderTickEvent event) {
        if (!ImageUtil.shouldShowIcon() || event.phase != TickEvent.Phase.END) return;
        Point mousePos = getCurrentMousePosition();
        EnumIconSize icon = TwitchyConfig.ICON.iconSize;
        int maxTextWidth = new ScaledResolution(mc).getScaledWidth() - mousePos.x - 16;
        if (Twitchy.isLive || TwitchyConfig.ICON.iconState == EnumIconVisibility.ALWAYS) {
            drawIcon(); //draw the twitch icon
            if (expandList) {
                int x = TwitchyConfig.ICON.posX;
                int y = TwitchyConfig.ICON.posY + icon.height + BORDER * 3;
                synchronized (Twitchy.LIVE_STREAMERS) {
                    int localX = x + BORDER + 2;
                    Map<String, StreamInfo> streamers = StreamerUtil.getStreamers();
                    List<String> broadcasters = StreamerUtil.sortChannelNames(streamers.keySet());
                    if (!broadcasters.isEmpty()) {
                        drawTooltipBoxBackground(localX + BORDER / 2, y + BORDER / 2, PROFILE_PIC_NEW_SIZE - BORDER, PROFILE_PIC_NEW_SIZE * streamers.size() + (streamers.size() - 1) * 3 - BORDER, 0);
                        for (int i = 0; i < broadcasters.size(); i++) {
                            String broadcaster = broadcasters.get(i);
                            int localY = y + (PROFILE_PIC_NEW_SIZE + 3) * i;
                            StreamInfo info = streamers.get(broadcaster);
                            ResourceLocation profilePic = ImageUtil.loadImage(info.profilePicUrl, broadcaster, ImageUtil.ImageCacheType.CACHED);
                            mc.renderEngine.bindTexture(profilePic);
                            Gui.drawScaledCustomSizeModalRect(localX, localY, 0, 0, PROFILE_PIC_ORIGINAL_SIZE, PROFILE_PIC_ORIGINAL_SIZE, PROFILE_PIC_NEW_SIZE, PROFILE_PIC_NEW_SIZE, PROFILE_PIC_ORIGINAL_SIZE, PROFILE_PIC_ORIGINAL_SIZE);
                        }

                        //important: need to draw the tooltip AFTER all icons have been drawn
                        for (int i = 0; i < broadcasters.size(); i++) {
                            String broadcaster = broadcasters.get(i);
                            int localY = y + (PROFILE_PIC_NEW_SIZE + 3) * i;
                            if (isMouseOver(localX, localY, PROFILE_PIC_NEW_SIZE, PROFILE_PIC_NEW_SIZE, mousePos)) {
                                StreamInfo info = streamers.get(broadcaster);
                                drawStreamInfo(localX, localY, mousePos, info, GuiScreen.isShiftKeyDown(), maxTextWidth);
                                break;
                            }
                        }
                    }
                }
            }
        }
        if (isMouseOver(TwitchyConfig.ICON.posX, TwitchyConfig.ICON.posY, icon.width, icon.height, mousePos)) {
            String key = expandList ? "collapse" : "expand";
            List<String> tooltips = Lists.newArrayList(
                    new TextComponentTranslation("twitchy.icon." + key).setStyle(new Style().setColor(TextFormatting.AQUA)).getFormattedText(),
                    new TextComponentTranslation("twitchy.icon.info.right", TextFormatting.YELLOW.toString() + "ALT + Right-Click").getFormattedText(),
                    new TextComponentTranslation("twitchy.icon.info", TextFormatting.BLUE.toString() + "ALT + Click").getFormattedText()
            );
            drawHoveringText(tooltips, mousePos.x, mousePos.y + 5, mc.displayWidth, mc.displayHeight, maxTextWidth, mc.fontRenderer);
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

    private static boolean isMouseOver(int x, int y, int width, int height, Point mousePos) {
        int mx = mousePos.x, my = mousePos.y;
        return mx >= x && mx <= (x + width) && my >= y && my <= (y + height);
    }

    private static Point getCurrentMousePosition() {
        ScaledResolution sr = new ScaledResolution(mc);
        int srHeight = sr.getScaledHeight();
        int mouseX = Mouse.getX() * sr.getScaledWidth() / mc.displayWidth;
        int mouseY = srHeight - Mouse.getY() * srHeight / mc.displayHeight - 1;
        return new Point(mouseX, mouseY);
    }

    @SubscribeEvent
    public static void mouseClick(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (TwitchyConfig.GENERAL.enabled && (!Twitchy.isSelfStreaming || TwitchyConfig.GENERAL.streamerMode != EnumStreamerMode.FULL)) {
            if (TwitchyConfig.GENERAL.enableAltRightClickDismiss && Mouse.getEventButton() == 1 && Mouse.getEventButtonState() && GuiScreen.isAltKeyDown())
                Twitchy.isIconDismissed = !Twitchy.isIconDismissed;
        }
        if (!ImageUtil.shouldShowIcon()) return; //This covers all checks if the mod is active
        if (Mouse.getEventButtonState()) {
            Point mousePos = getCurrentMousePosition();
            switch (Mouse.getEventButton()) {
                case 0:
                    if (isMouseOver(TwitchyConfig.ICON.posX, TwitchyConfig.ICON.posY, TwitchyConfig.ICON.iconSize.width, TwitchyConfig.ICON.iconSize.height, mousePos)) {
                        if (GuiScreen.isAltKeyDown())
                            mc.displayGuiScreen(FMLClientHandler.instance().getGuiFactoryFor(FMLCommonHandler.instance().findContainerFor(Twitchy.MODID)).createConfigGui(mc.currentScreen));
                        else expandList = !expandList;
                    }
                    if (expandList && Twitchy.isLive) {
                        int i = 0;
                        int y = TwitchyConfig.ICON.posY + TwitchyConfig.ICON.iconSize.height + BORDER * 3;
                        Map<String, StreamInfo> streamers = StreamerUtil.getStreamers();
                        List<String> broadcasters = StreamerUtil.sortChannelNames(streamers.keySet());
                        for (String broadcaster : broadcasters) {
                            int localX = TwitchyConfig.ICON.posX + BORDER + 2;
                            int localY = y + (PROFILE_PIC_NEW_SIZE + 3) * i++;
                            if (isMouseOver(localX, localY, PROFILE_PIC_NEW_SIZE, PROFILE_PIC_NEW_SIZE, mousePos)) {
                                StreamerUtil.openTwitchStream(broadcaster.toLowerCase(Locale.ROOT));
                            }
                        }
                    }

                    break;
                case 1:
                    if(TwitchyConfig.GENERAL.enableAltRightClickDismiss) {
                        if (isMouseOver(TwitchyConfig.ICON.posX, TwitchyConfig.ICON.posY, TwitchyConfig.ICON.iconSize.width, TwitchyConfig.ICON.iconSize.height, mousePos)) {
                            Twitchy.isIconDismissed = true;
                        }
                    }
                    break;
            }
        }
    }

    /**
     * THIS IS TEMPORARY UNTIL QUARK FIXES THE MISSING GUI NULL CHECK
     *
     * This is just copied from GuiUtils and has had all of the event firing removed
     */
    public static void drawHoveringText(List<String> textLines, int mouseX, int mouseY, int screenWidth, int screenHeight, int maxTextWidth, FontRenderer font)
    {
        if (!textLines.isEmpty())
        {
            GlStateManager.disableRescaleNormal();
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            int tooltipTextWidth = 0;

            for (String textLine : textLines)
            {
                int textLineWidth = font.getStringWidth(textLine);

                if (textLineWidth > tooltipTextWidth)
                {
                    tooltipTextWidth = textLineWidth;
                }
            }

            boolean needsWrap = false;

            int titleLinesCount = 1;
            int tooltipX = mouseX + 12;
            if (tooltipX + tooltipTextWidth + 4 > screenWidth)
            {
                tooltipX = mouseX - 16 - tooltipTextWidth;
                if (tooltipX < 4) // if the tooltip doesn't fit on the screen
                {
                    if (mouseX > screenWidth / 2)
                    {
                        tooltipTextWidth = mouseX - 12 - 8;
                    }
                    else
                    {
                        tooltipTextWidth = screenWidth - 16 - mouseX;
                    }
                    needsWrap = true;
                }
            }

            if (maxTextWidth > 0 && tooltipTextWidth > maxTextWidth)
            {
                tooltipTextWidth = maxTextWidth;
                needsWrap = true;
            }

            if (needsWrap)
            {
                int wrappedTooltipWidth = 0;
                List<String> wrappedTextLines = new ArrayList<>();
                for (int i = 0; i < textLines.size(); i++)
                {
                    String textLine = textLines.get(i);
                    List<String> wrappedLine = font.listFormattedStringToWidth(textLine, tooltipTextWidth);
                    if (i == 0)
                    {
                        titleLinesCount = wrappedLine.size();
                    }

                    for (String line : wrappedLine)
                    {
                        int lineWidth = font.getStringWidth(line);
                        if (lineWidth > wrappedTooltipWidth)
                        {
                            wrappedTooltipWidth = lineWidth;
                        }
                        wrappedTextLines.add(line);
                    }
                }
                tooltipTextWidth = wrappedTooltipWidth;
                textLines = wrappedTextLines;

                if (mouseX > screenWidth / 2)
                {
                    tooltipX = mouseX - 16 - tooltipTextWidth;
                }
                else
                {
                    tooltipX = mouseX + 12;
                }
            }

            int tooltipY = mouseY - 12;
            int tooltipHeight = 8;

            if (textLines.size() > 1)
            {
                tooltipHeight += (textLines.size() - 1) * 10;
                if (textLines.size() > titleLinesCount) {
                    tooltipHeight += 2; // gap between title lines and next lines
                }
            }

            if (tooltipY < 4)
            {
                tooltipY = 4;
            }
            else if (tooltipY + tooltipHeight + 4 > screenHeight)
            {
                tooltipY = screenHeight - tooltipHeight - 4;
            }

            final int zLevel = 300;
            int backgroundColor = 0xF0100010;
            int borderColorStart = 0x505000FF;
            int borderColorEnd = (borderColorStart & 0xFEFEFE) >> 1 | borderColorStart & 0xFF000000;
            GuiUtils.drawGradientRect(zLevel, tooltipX - 3, tooltipY - 4, tooltipX + tooltipTextWidth + 3, tooltipY - 3, backgroundColor, backgroundColor);
            GuiUtils.drawGradientRect(zLevel, tooltipX - 3, tooltipY + tooltipHeight + 3, tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 4, backgroundColor, backgroundColor);
            GuiUtils.drawGradientRect(zLevel, tooltipX - 3, tooltipY - 3, tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 3, backgroundColor, backgroundColor);
            GuiUtils.drawGradientRect(zLevel, tooltipX - 4, tooltipY - 3, tooltipX - 3, tooltipY + tooltipHeight + 3, backgroundColor, backgroundColor);
            GuiUtils.drawGradientRect(zLevel, tooltipX + tooltipTextWidth + 3, tooltipY - 3, tooltipX + tooltipTextWidth + 4, tooltipY + tooltipHeight + 3, backgroundColor, backgroundColor);
            GuiUtils.drawGradientRect(zLevel, tooltipX - 3, tooltipY - 3 + 1, tooltipX - 3 + 1, tooltipY + tooltipHeight + 3 - 1, borderColorStart, borderColorEnd);
            GuiUtils.drawGradientRect(zLevel, tooltipX + tooltipTextWidth + 2, tooltipY - 3 + 1, tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 3 - 1, borderColorStart, borderColorEnd);
            GuiUtils.drawGradientRect(zLevel, tooltipX - 3, tooltipY - 3, tooltipX + tooltipTextWidth + 3, tooltipY - 3 + 1, borderColorStart, borderColorStart);
            GuiUtils.drawGradientRect(zLevel, tooltipX - 3, tooltipY + tooltipHeight + 2, tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 3, borderColorEnd, borderColorEnd);

            for (int lineNumber = 0; lineNumber < textLines.size(); ++lineNumber)
            {
                String line = textLines.get(lineNumber);
                font.drawStringWithShadow(line, (float)tooltipX, (float)tooltipY, -1);

                if (lineNumber + 1 == titleLinesCount)
                {
                    tooltipY += 2;
                }

                tooltipY += 10;
            }

            GlStateManager.enableLighting();
            GlStateManager.enableDepth();
            RenderHelper.enableStandardItemLighting();
            GlStateManager.enableRescaleNormal();
        }
    }
}
