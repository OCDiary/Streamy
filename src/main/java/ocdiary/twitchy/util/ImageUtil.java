package ocdiary.twitchy.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraft.util.ResourceLocation;
import ocdiary.twitchy.Twitchy;
import ocdiary.twitchy.TwitchyConfig;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AgeFileFilter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author UpcraftLP
 */
public class ImageUtil {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final ResourceLocation NO_PREVIEW = new ResourceLocation(Twitchy.MODID, "textures/gui/no_preview.png");
    private static final ResourceLocation NO_PROFILE = TextureMap.LOCATION_MISSING_TEXTURE; //TODO add proper missing texture
    private static final Map<String, ResourceLocation> previews = new ConcurrentHashMap<>();
    private static final Map<String, ResourceLocation> profiles = new ConcurrentHashMap<>();
    private static File cacheDir;

    public static void init() {
        cacheDir = new File(Minecraft.getMinecraft().mcDataDir, Twitchy.MODID + "/profileCache");
        if(!cacheDir.exists()) {
            Twitchy.LOGGER.info("no profile cache found, creating cache directory at {}.", ImageUtil.cacheDir.getAbsolutePath());
            ImageUtil.cacheDir.mkdirs();
        }
        clearOldFiles();
    }

    public enum ImageCacheType {
        LIVE,
        CACHED;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static void clearPreviewCache() {
        synchronized (previews) {
            previews.values().forEach(preview -> {
                if (preview != NO_PREVIEW) mc.getTextureManager().deleteTexture(preview);
            });
            previews.clear();
        }
    }

    public static void clearOldFiles() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -1);
        Date oldestFile = calendar.getTime();
        Iterator<File> iterator = FileUtils.iterateFiles(cacheDir, new AgeFileFilter(oldestFile), null);
        while (iterator.hasNext()) {
            iterator.next().delete();
        }
    }

    /**
     * used to load a texture from the web and cache it.
     * @param url the url to download the image from, also acts as unique identifier for the image
     * @param name the name of the image
     * @param type either {@Code CACHED} or {@Code LIVE}, cached means to save the downloaded image as file and only refresh it every 30 days.
     * @return the {@Link ResourceLocation} that this image can be accessed with
     */
    public static ResourceLocation loadImage(String url, String name, ImageCacheType type) {
        if(type == ImageCacheType.CACHED && profiles.containsKey(url)) return profiles.get(url);
        else if(type == ImageCacheType.LIVE && previews.containsKey(url)) return previews.get(url);

        for(char c : ChatAllowedCharacters.ILLEGAL_FILE_CHARACTERS) {
            name = name.replace(c, '_');
        }
        TextureManager textureManager = mc.getTextureManager();
        ResourceLocation imageRL = null;

        //try to load from file
        if(type == ImageCacheType.CACHED) {
            File image = new File(cacheDir, name + ".png");
            try {
                if (image.exists() && !image.isDirectory()) {
                    imageRL = textureManager.getDynamicTextureLocation(Twitchy.MODID + "_" + name + "_" + type, new DynamicTexture(ImageIO.read(image)));
                }
            } catch (Exception e) {
                image.delete();
                Twitchy.LOGGER.error("error reading cached profile image, trying to get texture from the web...");
            }
        }

        //else get from web
        if(imageRL == null) {
            try {
                BufferedImage bufferedImage = ImageIO.read(new URL(url));
                DynamicTexture texture = new DynamicTexture(bufferedImage);
                texture.loadTexture(mc.getResourceManager());
                imageRL = textureManager.getDynamicTextureLocation(Twitchy.MODID + "_" + name + "_" + type, texture);
                if(type == ImageCacheType.CACHED) {
                    File image = new File(cacheDir, name + ".png");
                    ImageIO.write(bufferedImage, "png", image);
                    Twitchy.LOGGER.info("Successfully downloaded profile image for {} and saved to {}.", name, image.getName());
                }
            } catch (Exception e) {
                Twitchy.LOGGER.error("Exception getting image for {} from {}, type {}", name, url, type);
                e.printStackTrace();
                switch (type) {
                    case LIVE:
                        imageRL = NO_PREVIEW;
                        break;
                    case CACHED:
                        imageRL = NO_PROFILE;
                        break;
                }
            }
        }

        //store texture
        switch (type) {
            case LIVE:
                synchronized (previews) {
                    previews.put(url, imageRL);
                }
                break;
            case CACHED:
                synchronized (profiles) {
                    profiles.put(url, imageRL);
                }
                break;
        }
        return imageRL;
    }

    public static boolean shouldShowIcon() {
        return TwitchyConfig.GENERAL.enabled && !Twitchy.isIconDismissed && !(Twitchy.isSelfStreaming && TwitchyConfig.GENERAL.streamerMode == EnumStreamerMode.FULL);
    }
}
