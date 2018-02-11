package ocdiary.twitchy.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import ocdiary.twitchy.Twitchy;
import ocdiary.twitchy.TwitchyConfig;

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
    private static final ResourceLocation NO_PROFILE = new ResourceLocation(Twitchy.MODID, "textures/gui/no_profile.png");
    public static final Map<String, ResourceLocation> previews = new ConcurrentHashMap<>();
    public static final Map<String, ResourceLocation> profiles = new ConcurrentHashMap<>();
    public static volatile boolean shouldReloadPreviews;

    public enum ImageCacheType {
        PREVIEW,
        PROFILE;

        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }
    }

    public static void clearPreviewCache() {
        previews.values().forEach(preview -> {
            if (!preview.equals(NO_PREVIEW)) mc.getTextureManager().deleteTexture(preview);
        });
        previews.clear();
        shouldReloadPreviews = false;
    }

    public static void clearProfileCache() {
        profiles.values().forEach(profile -> {
            if (!profile.equals(NO_PROFILE)) mc.getTextureManager().deleteTexture(profile);
        });
        profiles.clear();
    }

    public static ResourceLocation loadImage(String url, String name, ImageCacheType type) {
        ResourceLocation imageRL;
        try {
            BufferedImage image = ImageIO.read(new URL(url));
            DynamicTexture texture = new DynamicTexture(image);
            texture.loadTexture(mc.getResourceManager());
            imageRL = mc.getTextureManager().getDynamicTextureLocation(Twitchy.MODID + "_" + name + "_" + type, texture);
        } catch (Exception e) {
            e.printStackTrace();
            imageRL = NO_PREVIEW;
        }
        if (url != null) {
            switch (type) {
                case PREVIEW:
                    previews.put(url, imageRL);
                    break;
                case PROFILE:
                    profiles.put(url, imageRL);
                    break;
                default:
                    Twitchy.LOGGER.error("Unhandled ImageCacheType %s. Report this to the mod author!", type);
            }
        }
        return imageRL;
    }

    public static void invalidatePreviewCache() {
        shouldReloadPreviews = true;
    }

    public static boolean shouldShowIcon() {
        return TwitchyConfig.GENERAL.enabled && !Twitchy.isIconDismissed && (!Twitchy.isSelfStreaming || TwitchyConfig.GENERAL.streamerMode != EnumStreamerMode.FULL);
    }
}
