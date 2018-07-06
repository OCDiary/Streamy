package ocdiary.streamy;

import com.google.common.collect.Lists;
import io.netty.util.internal.StringUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.InventoryEffectRenderer;
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
import net.minecraftforge.fml.client.config.IConfigElement;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import ocdiary.streamy.streams.Streams;
import ocdiary.streamy.util.*;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = Streamy.MODID, value = Side.CLIENT)
public class RenderingHandler {

    private static final ResourceLocation TWITCH_ICON = new ResourceLocation(Streamy.MODID, "textures/gui/twitch.png");
    private static final Minecraft mc = Minecraft.getMinecraft();

    /** Padding around the profile pictures */
    private static final int PADDING = 2;
    /** Size of the profile pictures downloaded */
    private static final int PROFILE_PIC_ORIGINAL_SIZE = 300;
    /** Rendered size of the profile pictures */
    private static final int PROFILE_PIC_NEW_SIZE = 12;
    /** Spacing between rendered profile pictures */
    private static final int PROFILE_PIC_SPACING = 3;
    private static final int LIST_SPACING = PROFILE_PIC_NEW_SIZE + PROFILE_PIC_SPACING;
    private static final int RENDER_Z_LEVEL = 300; //300 is minimum as vanilla inventory items are rendered at that level and we want to render above these.

    /** Whether the streamer list is currently expanded */
    private static boolean expandList = false;

    private static void drawIcon() {
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.enableBlend();
        mc.getTextureManager().bindTexture(TWITCH_ICON);
        EnumIconSize iconSize = StreamyConfig.ICON.iconSize;
        GuiUtils.drawTexturedModalRect(StreamyConfig.ICON.posX, StreamyConfig.ICON.posY, iconSize.outlineU, iconSize.outlineV, iconSize.size, iconSize.size, 0);
        if (Streamy.isLive)
            GuiUtils.drawTexturedModalRect(StreamyConfig.ICON.posX, StreamyConfig.ICON.posY, iconSize.overlayU, iconSize.overlayV, iconSize.size, iconSize.size, 0);
    }

    private static void drawStreamInfo(int x, int y, Point mousePos, StreamInfo info, boolean showPreview, int maxTextWidth) {
        int mouseX = mousePos.x, mouseY = mousePos.y;
        if (showPreview && info.streaming) {
            String url = info.previewUrl;
            EnumPreviewSize quality = StreamyConfig.PREVIEW.quality;
            if (!StringUtil.isNullOrEmpty(url)) {
                GlStateManager.pushMatrix();
                GlStateManager.translate(0.0F, 0.0F, RENDER_Z_LEVEL);

                ResourceLocation preview = ImageUtil.loadImage(url, info.broadcaster, ImageUtil.ImageCacheType.LIVE);
                mc.getTextureManager().bindTexture(preview);

                Gui.drawScaledCustomSizeModalRect(x, y, 0, 0, quality.width, quality.height, StreamyConfig.PREVIEW.previewWidth, StreamyConfig.PREVIEW.previewHeight, quality.width, quality.height);
                GlStateManager.popMatrix();
            }
            GuiUtils.drawHoveringText(Lists.newArrayList(), mouseX, mouseY + 15 + quality.height, mc.displayWidth, mc.displayHeight, Math.min(maxTextWidth, quality.width) + PADDING, mc.fontRenderer);
        } else {
            List<String> tooltips = new ArrayList<>();
            Lists.newArrayList(I18n.format(Streamy.MODID + ".stream.broadcaster", TextFormatting.AQUA + info.broadcaster + TextFormatting.RESET.toString()));
            if (!info.streaming) {
                tooltips.addAll(Lists.newArrayList(
                        I18n.format(Streamy.MODID + ".stream.offline", TextFormatting.RED + info.broadcaster + TextFormatting.RESET.toString()),
                        ""));
            } else {
                tooltips.add(I18n.format(Streamy.MODID + ".stream.broadcaster", TextFormatting.AQUA + info.broadcaster + TextFormatting.RESET.toString()));
                if (!StreamyConfig.CHANNELS.disableTitle) tooltips.add(I18n.format(Streamy.MODID + ".stream.title", TextFormatting.BLUE.toString() + info.title + TextFormatting.RESET.toString()));
                if (!StreamyConfig.CHANNELS.disableGame) tooltips.add(I18n.format(Streamy.MODID + ".stream.game", TextFormatting.DARK_GREEN.toString() + info.game + TextFormatting.RESET.toString()));
                if (!StreamyConfig.CHANNELS.disableViewers) tooltips.add(I18n.format(Streamy.MODID + ".stream.viewers", TextFormatting.DARK_RED.toString() + info.viewers + TextFormatting.RESET.toString()));
                tooltips.add("");
                tooltips.add(I18n.format(Streamy.MODID + ".stream.preview", TextFormatting.GOLD.toString() + "SHIFT" + TextFormatting.RESET.toString()));
            }
            tooltips.add(TextFormatting.GRAY + I18n.format(Streamy.MODID + ".stream.watch", TextFormatting.WHITE.toString() + info.broadcaster + TextFormatting.GRAY.toString()) + TextFormatting.RESET);
            GuiUtils.drawHoveringText(tooltips, mouseX, mouseY + 20, mc.displayWidth, mc.displayHeight, maxTextWidth, mc.fontRenderer);
        }
    }

    private static boolean isValidGuiForRendering(GuiScreen gui)
    {
        return gui == null || gui instanceof GuiMainMenu || gui instanceof GuiIngameMenu || gui instanceof GuiChat || gui instanceof InventoryEffectRenderer;
    }

    private static Point getListStartPos() {
        int iconCenterX = StreamyConfig.ICON.posX + (StreamyConfig.ICON.iconSize.size / 2);
        int iconCenterY = StreamyConfig.ICON.posY + (StreamyConfig.ICON.iconSize.size / 2);
        Point pos = new Point(iconCenterX - (PROFILE_PIC_NEW_SIZE / 2), iconCenterY - (PROFILE_PIC_NEW_SIZE / 2));

        int distToMove = (StreamyConfig.ICON.iconSize.size - (StreamyConfig.ICON.iconSize.size - PROFILE_PIC_NEW_SIZE) / 2) + PADDING * 3;
        pos = StreamyConfig.ICON.expandDirection.translate(pos, distToMove);
        return new Point(pos);
    }

    private static Point getNextListPos(Point pos) {
        return StreamyConfig.ICON.expandDirection.translate(pos, LIST_SPACING);
    }

    @SubscribeEvent
    public static void drawScreen(TickEvent.RenderTickEvent event) {
        if (!ImageUtil.shouldShowIcon() || event.phase != TickEvent.Phase.END || !isValidGuiForRendering(mc.currentScreen)) return;
        Point mousePos = getCurrentMousePosition();
        EnumIconSize icon = StreamyConfig.ICON.iconSize;
        int maxTextWidth = new ScaledResolution(mc).getScaledWidth() - mousePos.x - 16;
        if (Streamy.isLive || StreamyConfig.ICON.iconState == EnumIconVisibility.ALWAYS) {
            drawIcon(); //draw the twitch icon
            if (expandList) {
                //Draw the list of streamers
                EnumDirection direction = StreamyConfig.ICON.expandDirection;
                Point localPos = getListStartPos();

                List<StreamInfo> streamers = StreamerUtil.getStreamers();
                if (!streamers.isEmpty()) {
                    //Draw list background
                    Rectangle bgRect = null;
                    int listW = PROFILE_PIC_NEW_SIZE;
                    int listL = PROFILE_PIC_NEW_SIZE * streamers.size() + (streamers.size() - 1) * PROFILE_PIC_SPACING;
                    switch (direction) {
                        case UP:
                            bgRect = new Rectangle(localPos.x, localPos.y - listL + PROFILE_PIC_NEW_SIZE, listW, listL);
                            break;
                        case RIGHT:
                            bgRect = new Rectangle(localPos.x, localPos.y, listL, listW);
                            break;
                        case DOWN:
                            bgRect = new Rectangle(localPos.x, localPos.y, listW, listL);
                            break;
                        case LEFT:
                            bgRect = new Rectangle(localPos.x - listL + PROFILE_PIC_NEW_SIZE, localPos.y, listL, listW);
                            break;
                    }
                    drawTooltipBoxBackground(bgRect.x, bgRect.y, bgRect.width, bgRect.height, 0);
                    //Draw list streamer icons
                    for (StreamInfo info : streamers) {
                        ResourceLocation profilePic = ImageUtil.loadImage(info.profilePicUrl, info.broadcaster, ImageUtil.ImageCacheType.CACHED);
                        mc.renderEngine.bindTexture(profilePic);
                        Gui.drawScaledCustomSizeModalRect(localPos.x, localPos.y, 0, 0, PROFILE_PIC_ORIGINAL_SIZE, PROFILE_PIC_ORIGINAL_SIZE, PROFILE_PIC_NEW_SIZE, PROFILE_PIC_NEW_SIZE, PROFILE_PIC_ORIGINAL_SIZE, PROFILE_PIC_ORIGINAL_SIZE);
                        localPos = getNextListPos(localPos);
                    }

                    //important: need to draw the tooltip AFTER all icons have been drawn
                    localPos = getListStartPos();
                    for (StreamInfo info : streamers) {
                        if (isMouseOver(localPos.x, localPos.y, PROFILE_PIC_NEW_SIZE, PROFILE_PIC_NEW_SIZE, mousePos)) {
                            drawStreamInfo(localPos.x, localPos.y, mousePos, info, GuiScreen.isShiftKeyDown(), maxTextWidth);
                            break;
                        }
                        localPos = getNextListPos(localPos);
                    }
                }
            }
        }
        //Draw tooltip for icon
        if (isMouseOver(StreamyConfig.ICON.posX, StreamyConfig.ICON.posY, icon.size, icon.size, mousePos)) {
            String key = expandList ? "collapse" : "expand";
            List<String> tooltips = Lists.newArrayList(
                    new TextComponentTranslation(Streamy.MODID + ".icon." + key).setStyle(new Style().setColor(TextFormatting.AQUA)).getFormattedText(),
                    new TextComponentTranslation(Streamy.MODID + ".icon.info.right", TextFormatting.YELLOW.toString() + "ALT + Right-Click").getFormattedText(),
                    new TextComponentTranslation(Streamy.MODID + ".icon.info", TextFormatting.BLUE.toString() + "ALT + Click").getFormattedText()
            );
            GuiUtils.drawHoveringText(tooltips, mousePos.x, mousePos.y + 5, mc.displayWidth, mc.displayHeight, maxTextWidth, mc.fontRenderer);
        }
    }

    private static void drawTooltipBoxBackground(int x, int y, int width, int height, int zLevel) {
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
        if (StreamyConfig.GENERAL.enabled && (!Streamy.isSelfStreaming || StreamyConfig.GENERAL.streamerMode != EnumStreamerMode.FULL)) {
            if (StreamyConfig.GENERAL.enableAltRightClickDismiss && Mouse.getEventButton() == 1 && Mouse.getEventButtonState() && GuiScreen.isAltKeyDown())
                Streamy.isIconDismissed = !Streamy.isIconDismissed;
        }
        if (!ImageUtil.shouldShowIcon()) return; //This covers all checks if the mod is active
        if (Mouse.getEventButtonState()) {
            Point mousePos = getCurrentMousePosition();
            switch (Mouse.getEventButton()) {
                case 0:
                    if (isMouseOver(StreamyConfig.ICON.posX, StreamyConfig.ICON.posY, StreamyConfig.ICON.iconSize.size, StreamyConfig.ICON.iconSize.size, mousePos)) {
                        if (GuiScreen.isAltKeyDown())
                            //Show mod configs
                            mc.displayGuiScreen(FMLClientHandler.instance().getGuiFactoryFor(FMLCommonHandler.instance().findContainerFor(Streamy.MODID)).createConfigGui(mc.currentScreen));
                        else if (GuiScreen.isCtrlKeyDown())
                        {
                            //Cycle to next expansion direction
                            IConfigElement config = Streamy.getConfig("streamy.icon.expandDirection");
                            int configIndex = 0;
                            String currentValue = config.get().toString();
                            String[] values = config.getValidValues();
                            for (int i = 0; i < values.length; i++) {
                                if (values[i].equalsIgnoreCase(currentValue)) {
                                    configIndex = i + 1;
                                    break;
                                }
                            }
                            if(configIndex >= values.length)
                                configIndex = 0;
                            config.set(values[configIndex]);
                            Streamy.configChanged("expandDirection",mc.world != null, config.requiresMcRestart());
                        }
                        else
                            //Toggle streamer list
                            expandList = !expandList;
                    }
                    if (expandList) {
                        Point pos = getListStartPos();
                        for (StreamInfo info : StreamerUtil.getStreamers()) {
                            if (isMouseOver(pos.x, pos.y, PROFILE_PIC_NEW_SIZE, PROFILE_PIC_NEW_SIZE, mousePos)) {
                                Streams.getStream(info).openStreamURL(info.broadcaster);
                            }
                            pos = getNextListPos(pos);
                        }
                    }

                    break;
                case 1:
                    if (StreamyConfig.GENERAL.enableAltRightClickDismiss) {
                        if (isMouseOver(StreamyConfig.ICON.posX, StreamyConfig.ICON.posY, StreamyConfig.ICON.iconSize.size, StreamyConfig.ICON.iconSize.size, mousePos)) {
                            Streamy.isIconDismissed = true;
                        }
                    }
                    break;
            }
        }
    }
}
