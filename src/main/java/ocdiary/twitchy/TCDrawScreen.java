package ocdiary.twitchy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ImageBufferDownload;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = Twitchy.MODID, value = Side.CLIENT)
public class TCDrawScreen {

    private static final ResourceLocation iconRL = new ResourceLocation(Twitchy.MODID, "textures/gui/twitch.png");
    private static final ResourceLocation noPreviewRL = new ResourceLocation(Twitchy.MODID, "textures/gui/noPreview.png");
    public static List<ResourceLocation> previews = new ArrayList<>();
    private static Minecraft mc = Minecraft.getMinecraft();

    private static Rectangle twitchRect = new Rectangle(TCConfig.icon.posX, TCConfig.icon.posY, 23, 23);

    private static int textU = 0;
    private static int textV = 0;
    private static int textLU = 24;
    private static int textLV = 0;

    private static String lastPreview = "";

    public static void updatePreview(String previewUrl)
    {
        previews.remove(getRLFromURL(previewUrl));
    }

    public static void updateIconSize()
    {
        switch(TCConfig.icon.iconSize)
        {
            case 1:
                textU = -1;
                textV = 87;
                textLU = 26;
                textLV = 87;
                break;
            case 2:
                textU = 0;
                textV = 38;
                textLU = 25;
                textLV = 38;
                break;
            default: //3
                textU = 0;
                textV = 0;
                textLU = 24;
                textLV = 0;
        }
    }

    private static void drawIcon()
    {
        if(!TCConfig.icon.persistantIcon && !Twitchy.isLive) return;
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.enableBlend();
        mc.getTextureManager().bindTexture(iconRL);
        GuiUtils.drawTexturedModalRect(twitchRect.x, twitchRect.y, textU, textV, twitchRect.width, twitchRect.height, 0);
        if(Twitchy.isLive)
            GuiUtils.drawTexturedModalRect(twitchRect.x, twitchRect.y, textLU, textLV, twitchRect.width, twitchRect.height, 0);
    }

    private static void drawTooltip(GuiScreen gui, int mouseX, int mouseY)
    {
        List<String> tooltip = new ArrayList<>();
        if (Twitchy.isLive) {
            if (GuiScreen.isShiftKeyDown()) {
                tooltip.add(I18n.format("twitchy.tooltip.title") + " " + TextFormatting.BLUE + Twitchy.streamTitle);
                tooltip.add(I18n.format("twitchy.tooltip.game") + " " + TextFormatting.DARK_GREEN + Twitchy.streamGame);
                tooltip.add(I18n.format("twitchy.tooltip.viewers") + " " + TextFormatting.DARK_RED + Twitchy.streamViewers);
            } else
                tooltip.add(new TextComponentTranslation("twitchy.tooltip.watch.1")
                        .appendText(" " + TextFormatting.GOLD + TCConfig.channel.twitchChannelId + TextFormatting.RESET + " ")
                        .appendSibling(new TextComponentTranslation("twitchy.tooltip.watch.2"))
                        .getFormattedText());
            if (!GuiScreen.isShiftKeyDown())
                tooltip.add(TextFormatting.AQUA + I18n.format("twitchy.tooltip.info", "SHIFT"));
        } else
            tooltip.add(TextFormatting.GOLD + TCConfig.channel.twitchChannelId + TextFormatting.RESET + I18n.format("twitchy.tooltip.offline"));

        GuiUtils.drawHoveringText(tooltip, mouseX, mouseY + 20, gui.width, gui.height, -1, mc.fontRenderer);

        if(Twitchy.isLive && GuiScreen.isShiftKeyDown()) {
            //TODO: Render medium stream preview
            String url = Twitchy.streamPreview; //"https://cdn.discordapp.com/attachments/286147745817952257/410606411039637505/TEST.png";
            ResourceLocation previewRL = getRLFromURL(url);
            if(!Twitchy.streamPreview.equals(lastPreview)) {
                if(!previews.contains(previewRL)) loadPreview(url, previewRL);
            }
            mc.getTextureManager().bindTexture(previewRL);
            int previewY = mouseY + 15 + mc.fontRenderer.FONT_HEIGHT * 3;
            Gui.drawScaledCustomSizeModalRect(mouseX + 8, previewY, 0, 0, 320, 180, 320, 180, 320, 180);
            //GuiUtils.drawTexturedModalRect(mouseX + 8, previewY, 0, 0, 320, 180, 0);
            //Gui.drawRect(mouseX + 8, previewY, mouseX + 320, previewY + 180, 0x88000000);
        }
    }

    @SubscribeEvent
    public static void drawScreen(GuiScreenEvent.DrawScreenEvent.Post event)
    {
        if (mc.player != null && TCConfig.icon.persistantIcon) {
            RenderHelper.disableStandardItemLighting();
            drawIcon();
            if (twitchRect.contains(event.getMouseX(), event.getMouseY()))
                drawTooltip(event.getGui(), event.getMouseX(), event.getMouseY());
            RenderHelper.enableStandardItemLighting();
        }
    }

    @SubscribeEvent
    public static void drawOverlay(RenderGameOverlayEvent.Post event)
    {
        if (event.getType() == RenderGameOverlayEvent.ElementType.ALL && mc.currentScreen == null)
            drawIcon();
    }

    @SubscribeEvent
    public static void mouseClick(GuiScreenEvent.MouseInputEvent.Pre event)
    {
        if (mc.player != null && Mouse.getEventButton() == 0 && Mouse.getEventButtonState()) {
            //Get mouse position
            ScaledResolution sr = new ScaledResolution(mc);
            int srHeight = sr.getScaledHeight();
            int mouseX = Mouse.getX() * sr.getScaledWidth() / mc.displayWidth;
            int mouseY = srHeight - Mouse.getY() * srHeight / mc.displayHeight - 1;

            if (twitchRect.contains(mouseX, mouseY))
                openTwitchStream();
        }
    }

    private static void openTwitchStream() {
        if (Desktop.isDesktopSupported()) {
            String url = "https://www.twitch.tv/" + TCConfig.channel.twitchChannelId;
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (IOException e) {
                Twitchy.LOGGER.error("Can't open browser", e);
            } catch (URISyntaxException e) {
                Twitchy.LOGGER.error("URL '" + url + "' is invalid. Report this to the mod author!", e);
            }
        } else {
            Twitchy.LOGGER.error("Can't open browser - Desktop is not supported");
        }
    }

    private static ResourceLocation getRLFromURL(String url) {
        int slashIndex = url.lastIndexOf("/");
        String imageName = url.substring(slashIndex);
        return new ResourceLocation(Twitchy.MODID, imageName);
    }

    private static void loadPreview(String url, ResourceLocation imageRL) {
        ThreadDownloadImageData imageData = new ThreadDownloadImageData(null, url, noPreviewRL, new ImageBufferDownload());
        TextureManager texturemanager = Minecraft.getMinecraft().getTextureManager();
        texturemanager.loadTexture(imageRL, imageData);
        previews.add(imageRL);
    }
}
