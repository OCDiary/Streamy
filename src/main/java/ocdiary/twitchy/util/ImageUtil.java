package ocdiary.twitchy.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import ocdiary.twitchy.Twitchy;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author UpcraftLP
 */
public class ImageUtil {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final ResourceLocation NO_PREVIEW = new ResourceLocation(Twitchy.MODID, "textures/gui/no_preview.png");
    public static final Map<String, ResourceLocation> previews = new ConcurrentHashMap<>();
    public static volatile boolean shouldReloadPreviews;

    public static void clearPreviewCache()
    {
        for(String previewUrl : previews.keySet()) {
            ResourceLocation preview = previews.get(previewUrl);
            if(!preview.equals(NO_PREVIEW)) mc.getTextureManager().deleteTexture(preview);
        }
        previews.clear();
    }

    public static ResourceLocation loadImage(String url, String name) {
        ResourceLocation imageRL;
        try {
            BufferedImage image = ImageIO.read(new URL(url));
            DynamicTexture texture = new DynamicTexture(image);
            texture.loadTexture(mc.getResourceManager());
            //Twitchy.previewWidth = image.getWidth();
            //Twitchy.previewHeight = image.getHeight();
            imageRL = mc.getTextureManager().getDynamicTextureLocation(Twitchy.MODID + "_" + name, texture);
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
