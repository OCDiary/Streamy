package ocdiary.streamy.handlers;

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
import net.minecraftforge.fml.client.config.IConfigElement;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import ocdiary.streamy.Streamy;
import ocdiary.streamy.StreamyConfig;
import ocdiary.streamy.util.*;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = Streamy.MODID, value = Side.CLIENT)
public class RenderingHandler {

    private static final ResourceLocation ICON = new ResourceLocation(Streamy.MODID, "textures/gui/icon.png");
    private static final ResourceLocation ICON_LIVE = new ResourceLocation(Streamy.MODID, "textures/gui/icon_live.png");
    private static final int ICON_FILE_SIZE = 300;
    private static final Minecraft mc = Minecraft.getMinecraft();

    /**
     * Padding around the profile pictures
     */
    private static final int PADDING = 2;
    /**
     * Size of the profile pictures downloaded
     */
    private static final int PROFILE_PIC_ORIGINAL_SIZE = 300;
    /**
     * Rendered size of the profile pictures
     */
    private static final int PROFILE_PIC_NEW_SIZE = 12;
    /**
     * Spacing between rendered profile pictures
     */
    private static final int PROFILE_PIC_SPACING = 3;
    private static final int LIST_SPACING = PROFILE_PIC_NEW_SIZE + PROFILE_PIC_SPACING;
    private static final int RENDER_Z_LEVEL = 300; //300 is minimum as vanilla inventory items are rendered at that level and we want to render above these.

    /**
     * Whether the streamer list is currently expanded
     */
    private static boolean expandList = false;

    private static boolean isDraggingIcon = false;
    private static Point draggingPos, draggingOffset;

    private static Point getIconPos() {
        //TODO: Use StreamyConfig.ICON.getPos() once Forge is updated
        return isDraggingIcon ? draggingPos : new Point(StreamyConfig.ICON.posX, StreamyConfig.ICON.posY);
    }

    private static void drawIcon() {
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.enableBlend();
        mc.getTextureManager().bindTexture(Streamy.isLive ? ICON_LIVE : ICON);
        Point iconPos = getIconPos();
        int size = StreamyConfig.ICON.size;
        Gui.drawScaledCustomSizeModalRect(iconPos.x, iconPos.y, 0, 0, ICON_FILE_SIZE, ICON_FILE_SIZE, size, size, ICON_FILE_SIZE, ICON_FILE_SIZE);
    }

    private static void drawStreamInfo(Point mousePos, StreamInfo info, boolean showPreview) {
        if (showPreview && info.streaming) {
            String url = info.previewUrl;
            EnumPreviewSize quality = StreamyConfig.PREVIEW.quality;
            if (!StringUtil.isNullOrEmpty(url)) {
                GlStateManager.pushMatrix();
                GlStateManager.translate(0.0F, 0.0F, RENDER_Z_LEVEL);

                ResourceLocation preview = ImageUtil.loadImage(url, info.broadcaster, ImageUtil.ImageCacheType.LIVE);
                mc.getTextureManager().bindTexture(preview);

                //Render in the center of the screen
                ScaledResolution sr = new ScaledResolution(mc);
                int x = (sr.getScaledWidth() / 2) - (StreamyConfig.PREVIEW.previewWidth / 2);
                int y = (sr.getScaledHeight() / 2) - (StreamyConfig.PREVIEW.previewHeight / 2);

                Gui.drawScaledCustomSizeModalRect(x, y, 0, 0, quality.width, quality.height, StreamyConfig.PREVIEW.previewWidth, StreamyConfig.PREVIEW.previewHeight, quality.width, quality.height);
                GlStateManager.popMatrix();
            }
        } else {
            List<String> tooltips = new ArrayList<>();
            Lists.newArrayList(I18n.format(Streamy.MODID + ".stream.broadcaster", TextFormatting.AQUA + info.broadcaster + TextFormatting.RESET, info.source.localName()));
            if (!info.streaming) {
                tooltips.addAll(Lists.newArrayList(
                        I18n.format(Streamy.MODID + ".stream.offline", TextFormatting.RED + info.broadcaster + TextFormatting.RESET),
                        ""));
                tooltips.add(TextFormatting.GRAY + I18n.format(Streamy.MODID + ".stream.channel", TextFormatting.WHITE + info.broadcaster + TextFormatting.GRAY, info.source.localName()) + TextFormatting.RESET);
            } else {
                tooltips.add(I18n.format(Streamy.MODID + ".stream.broadcaster", TextFormatting.AQUA + info.broadcaster + TextFormatting.RESET, info.source.localName()));
                if (!StreamyConfig.CHANNELS.disableTitle)
                    tooltips.add(I18n.format(Streamy.MODID + ".stream.title", TextFormatting.BLUE + info.title + TextFormatting.RESET));
                if (!StreamyConfig.CHANNELS.disableGame)
                    tooltips.add(I18n.format(Streamy.MODID + ".stream.game", TextFormatting.DARK_GREEN + info.game + TextFormatting.RESET));
                if (!StreamyConfig.CHANNELS.disableViewers)
                    tooltips.add(I18n.format(Streamy.MODID + ".stream.viewers", TextFormatting.DARK_RED.toString() + info.viewers + TextFormatting.RESET));
                tooltips.add("");
                tooltips.add(I18n.format(Streamy.MODID + ".stream.preview", TextFormatting.GOLD + "SHIFT" + TextFormatting.RESET));
                tooltips.add(TextFormatting.GRAY + I18n.format(Streamy.MODID + ".stream.watch", TextFormatting.WHITE + info.broadcaster + TextFormatting.GRAY) + TextFormatting.RESET);
            }
            RenderingUtil.drawHoveringText(tooltips, mousePos);
        }
    }

    private static Point getListStartPos() {
        Point iconPos = getIconPos();
        int size = StreamyConfig.ICON.size;
        int iconCenterX = iconPos.x + (size / 2);
        int iconCenterY = iconPos.y + (size / 2);
        Point pos = new Point(iconCenterX - (PROFILE_PIC_NEW_SIZE / 2), iconCenterY - (PROFILE_PIC_NEW_SIZE / 2));

        int distToMove = (size - (size - PROFILE_PIC_NEW_SIZE) / 2) + PADDING * 3;
        pos = StreamyConfig.ICON.expandDirection.translate(pos, distToMove);
        return new Point(pos);
    }

    private static Point getNextListPos(Point pos) {
        return StreamyConfig.ICON.expandDirection.translate(pos, LIST_SPACING);
    }

    private static void updateConfigIconPos() {
        IConfigElement configPosX = StreamyConfig.getConfig("streamy.icon.posX");
        configPosX.set(draggingPos.x);
        StreamyConfig.configChanged("pos", mc.world != null, configPosX.requiresMcRestart());
        IConfigElement configPosY = StreamyConfig.getConfig("streamy.icon.posY");
        configPosY.set(draggingPos.y);
        StreamyConfig.configChanged("pos", mc.world != null, configPosY.requiresMcRestart());
    }

    @SubscribeEvent
    public static void drawScreen(TickEvent.RenderTickEvent event) {
        if (!ImageUtil.shouldShowIcon() || event.phase != TickEvent.Phase.END || !RenderingUtil.isValidGuiForRendering())
            return;
        Point mousePos = RenderingUtil.getCurrentMousePosition();
        if (isDraggingIcon)
            draggingPos = new Point(mousePos.x - draggingOffset.x, mousePos.y - draggingOffset.y);
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
                    RenderingUtil.drawTooltipBoxBackground(bgRect.x, bgRect.y, bgRect.width, bgRect.height, 0);
                    //Draw list streamer icons
                    for (StreamInfo info : streamers) {
                        ResourceLocation profilePic = ImageUtil.loadImage(info.profilePicUrl, info.broadcaster, ImageUtil.ImageCacheType.CACHED);
                        mc.renderEngine.bindTexture(profilePic);
                        Gui.drawScaledCustomSizeModalRect(localPos.x, localPos.y, 0, 0, PROFILE_PIC_ORIGINAL_SIZE, PROFILE_PIC_ORIGINAL_SIZE, PROFILE_PIC_NEW_SIZE, PROFILE_PIC_NEW_SIZE, PROFILE_PIC_ORIGINAL_SIZE, PROFILE_PIC_ORIGINAL_SIZE);
                        localPos = getNextListPos(localPos);
                    }

                    //important: need to draw the tooltip AFTER all icons have been drawn
                    if (!isDraggingIcon) {
                        localPos = getListStartPos();
                        for (StreamInfo info : streamers) {
                            if (RenderingUtil.isMouseOver(localPos.x, localPos.y, PROFILE_PIC_NEW_SIZE, PROFILE_PIC_NEW_SIZE, mousePos)) {
                                drawStreamInfo(mousePos, info, GuiScreen.isShiftKeyDown());
                                break;
                            }
                            localPos = getNextListPos(localPos);
                        }
                    }
                }
            }
        }
        //Draw tooltip for icon
        if (!isDraggingIcon && RenderingUtil.isMouseOverIcon(mousePos)) {
            String key = expandList ? "collapse" : "expand";
            List<String> tooltips = Lists.newArrayList(
                    new TextComponentTranslation(Streamy.MODID + ".icon." + key).setStyle(new Style().setColor(TextFormatting.AQUA)).getFormattedText(),
                    new TextComponentTranslation(Streamy.MODID + ".icon.toggle").getFormattedText(),
                    new TextComponentTranslation(Streamy.MODID + ".icon.config").getFormattedText(),
                    new TextComponentTranslation(Streamy.MODID + ".icon.rotate").getFormattedText(),
                    new TextComponentTranslation(Streamy.MODID + ".icon.drag").getFormattedText()
            );
            RenderingUtil.drawHoveringText(tooltips, mousePos);
        }
    }

    @SubscribeEvent
    public static void mouseClick(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (StreamyConfig.GENERAL.enabled && (!Streamy.isSelfStreaming || StreamyConfig.GENERAL.streamerMode != EnumStreamerMode.FULL)) {
            if (StreamyConfig.GENERAL.enableAltRightClickDismiss && Mouse.getEventButton() == 1 && Mouse.getEventButtonState() && GuiScreen.isAltKeyDown())
                Streamy.isIconDismissed = !Streamy.isIconDismissed;
        }
        if (!ImageUtil.shouldShowIcon()) return; //This covers all checks if the mod is active
        if (Mouse.getEventButtonState()) {
            Point mousePos = RenderingUtil.getCurrentMousePosition();
            switch (Mouse.getEventButton()) {
                case 0:
                    if (RenderingUtil.isMouseOverIcon(mousePos)) {
                        if (GuiScreen.isAltKeyDown())
                            //Show mod configs
                            mc.displayGuiScreen(FMLClientHandler.instance().getGuiFactoryFor(FMLCommonHandler.instance().findContainerFor(Streamy.MODID)).createConfigGui(mc.currentScreen));
                        else if (GuiScreen.isCtrlKeyDown()) {
                            //Cycle to next expansion direction
                            IConfigElement config = StreamyConfig.getConfig("streamy.icon.expandDirection");
                            int configIndex = 0;
                            String currentValue = config.get().toString();
                            String[] values = config.getValidValues();
                            for (int i = 0; i < values.length; i++) {
                                if (values[i].equalsIgnoreCase(currentValue)) {
                                    configIndex = i + 1;
                                    break;
                                }
                            }
                            if (configIndex >= values.length)
                                configIndex = 0;
                            config.set(values[configIndex]);
                            StreamyConfig.configChanged("expandDirection", mc.world != null, config.requiresMcRestart());
                        } else if (GuiScreen.isShiftKeyDown()) {
                            //Start dragging icon around screen
                            draggingPos = getIconPos();
                            draggingOffset = new Point(mousePos.x - draggingPos.x, mousePos.y - draggingPos.y);
                            isDraggingIcon = true;
                        } else
                            //Toggle streamer list
                            expandList = !expandList;
                    }
                    if (expandList) {
                        Point pos = getListStartPos();
                        for (StreamInfo info : StreamerUtil.getStreamers()) {
                            if (RenderingUtil.isMouseOver(pos.x, pos.y, PROFILE_PIC_NEW_SIZE, PROFILE_PIC_NEW_SIZE, mousePos)) {
                                info.source.getStream().openStreamURL(info.broadcaster);
                            }
                            pos = getNextListPos(pos);
                        }
                    }

                    break;
                case 1:
                    if (StreamyConfig.GENERAL.enableAltRightClickDismiss) {
                        if (RenderingUtil.isMouseOverIcon(mousePos)) {
                            Streamy.isIconDismissed = true;
                        }
                    }
                    break;
            }
        } else if (Mouse.getEventButton() == 0 && isDraggingIcon) {
            isDraggingIcon = false;
            updateConfigIconPos();
        }
    }

    @SubscribeEvent
    public static void keyboardInput(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        //Stop dragging if no shift key is being pressed anymore
        if (isDraggingIcon &&
                (Keyboard.getEventKey() == Keyboard.KEY_LSHIFT || Keyboard.getEventKey() == Keyboard.KEY_RSHIFT) &&
                (!Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) && !Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))) {
            isDraggingIcon = false;
            updateConfigIconPos();
        }
    }
}
