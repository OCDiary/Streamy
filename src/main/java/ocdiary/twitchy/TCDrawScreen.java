package ocdiary.twitchy;

import io.netty.util.internal.StringUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
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
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = Twitchy.MODID, value = Side.CLIENT)
public class TCDrawScreen {

    private static final ResourceLocation TWITCH_ICON = new ResourceLocation(Twitchy.MODID, "textures/gui/twitch.png");
    private static final ResourceLocation NO_PREVIEW = new ResourceLocation(Twitchy.MODID, "textures/gui/no_preview.png");
    private static final Map<String, ResourceLocation> previews = new ConcurrentHashMap<>();
    private static final Minecraft mc = Minecraft.getMinecraft();

    private static Rectangle twitchRect = new Rectangle(TCConfig.ICON.posX, TCConfig.ICON.posY, 23, 23);

    public static volatile boolean shouldReloadPreviews;

    private static void clearPreviewCache()
    {
        for(String previewUrl : previews.keySet()) {
            ResourceLocation preview = previews.get(previewUrl);
            if(!preview.equals(NO_PREVIEW)) mc.getTextureManager().deleteTexture(preview);
        }
        previews.clear();
    }

    private static void drawIcon()
    {
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.enableBlend();
        mc.getTextureManager().bindTexture(TWITCH_ICON);
        EnumIconSize iconSize = TCConfig.ICON.iconSize;
        GuiUtils.drawTexturedModalRect(twitchRect.x, twitchRect.y, iconSize.outlineU, iconSize.outlineV, twitchRect.width, twitchRect.height, 0);
        if(Twitchy.isLive) GuiUtils.drawTexturedModalRect(twitchRect.x, twitchRect.y, iconSize.overlayU, iconSize.overlayV, twitchRect.width, twitchRect.height, 0);
        //TODO draw collapsible menu
    }

    private static void drawTooltip(int mouseX, int mouseY, StreamInfo info)
    {
        List<ITextComponent> tooltip = new ArrayList<>();
        if (Twitchy.isLive) {
            if (GuiScreen.isShiftKeyDown()) {
                tooltip.add(new TextComponentTranslation("twitchy.tooltip.broadcaster", TextFormatting.AQUA + info.broadcaster));
                tooltip.add(new TextComponentTranslation("twitchy.tooltip.title", TextFormatting.BLUE.toString() + info.title));
                tooltip.add(new TextComponentTranslation("twitchy.tooltip.game", TextFormatting.DARK_GREEN.toString() + info.game));
                tooltip.add(new TextComponentTranslation("twitchy.tooltip.viewers", TextFormatting.DARK_RED.toString() + info.viewers));
            } else tooltip.add(new TextComponentTranslation("twitchy.tooltip.watch", TextFormatting.GOLD.toString() + info.broadcaster));
            if (!GuiScreen.isShiftKeyDown()) tooltip.add(new TextComponentTranslation("twitchy.tooltip.info", TextFormatting.AQUA.toString() + Keyboard.getKeyName(Keyboard.KEY_LSHIFT)));
        } else tooltip.add(new TextComponentTranslation("twitchy.tooltip.offline"));
        List<String> text = new ArrayList<>();
        tooltip.forEach(textComponent -> text.add(textComponent.getFormattedText()));
        int maxTextWidth = new ScaledResolution(mc).getScaledWidth() - mouseX - 16;

        if(Twitchy.isLive && GuiScreen.isShiftKeyDown()) {
            String url = info.previewUrl;
            if(!StringUtil.isNullOrEmpty(url)) {
                GlStateManager.pushMatrix();
                int zLevel = 300; //300 is minimum as vanilla inventory items are rendered at that level and we want to render above these.
                GlStateManager.translate(0.0F, 0.0F, zLevel);

                ResourceLocation preview;
                if(!previews.containsKey(url)) {
                    preview = loadPreview(url);
                }
                else preview = previews.get(url);
                mc.getTextureManager().bindTexture(preview);
                int textHeight = 0;
                for(String str : text) {
                    textHeight += mc.fontRenderer.getWordWrappedHeight(str, maxTextWidth) + 3;
                }
                Gui.drawScaledCustomSizeModalRect(mouseX + 8, mouseY + 5 + textHeight, 0, 0, Twitchy.previewWidth, Twitchy.previewHeight, TCConfig.previewWidth, TCConfig.previewHeight, Twitchy.previewWidth, Twitchy.previewHeight);
                GlStateManager.popMatrix();
            }
        }
        GuiUtils.drawHoveringText(text, mouseX, mouseY + 20, mc.displayWidth, mc.displayHeight, maxTextWidth, mc.fontRenderer);
    }

    @SubscribeEvent
    public static void drawScreen(TickEvent.RenderTickEvent event)
    {
        if(event.phase != TickEvent.Phase.END) return;
        if(shouldReloadPreviews) TCDrawScreen.clearPreviewCache();
        if (Twitchy.isLive || TCConfig.ICON.iconState == TCConfig.Icon.State.ALWAYS) {
            drawIcon();
            Point mousePos = getCurrentMousePosition();
            if (twitchRect.contains(mousePos.x, mousePos.y)) drawTooltip(mousePos.x, mousePos.y);
        }
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
            if (twitchRect.contains(mousePos.x, mousePos.y)) {
                if(Twitchy.isLive) {
                    openTwitchStream(TCConfig.CHANNELS.channel);
                }
                else {
                    mc.displayGuiScreen(FMLClientHandler.instance().getGuiFactoryFor(FMLCommonHandler.instance().findContainerFor(Twitchy.MODID)).createConfigGui(mc.currentScreen));
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

    private static ResourceLocation loadPreview(String url) {
        ResourceLocation imageRL;
        try {
            BufferedImage image = ImageIO.read(new URL(url));
            DynamicTexture texture = new DynamicTexture(image);
            texture.loadTexture(mc.getResourceManager());
            Twitchy.previewWidth = image.getWidth();
            Twitchy.previewHeight = image.getHeight();
            imageRL = mc.getTextureManager().getDynamicTextureLocation(Twitchy.MODID + "_preview", texture);
        }
        catch (Exception e) {
            e.printStackTrace();
            imageRL = NO_PREVIEW;
        }
        if(url != null) previews.put(url, imageRL);
        return imageRL;
    }

    public static void invalidatePreviewCache() {
        shouldReloadPreviews = true;
    }
}
