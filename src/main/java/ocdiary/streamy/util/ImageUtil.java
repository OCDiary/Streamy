package ocdiary.streamy.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraft.util.ResourceLocation;
import ocdiary.streamy.Streamy;
import ocdiary.streamy.StreamyConfig;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AgeFileFilter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author UpcraftLP
 */
public class ImageUtil {

    private static final Minecraft mc = Minecraft.getMinecraft();
    public static final ResourceLocation NO_PREVIEW = new ResourceLocation(Streamy.MODID, "textures/gui/preview_failed.png");
    public static final ResourceLocation NO_PROFILE = new ResourceLocation(Streamy.MODID, "textures/gui/icon_failed.png");
    public static final ResourceLocation LOADING_PREVIEW = new ResourceLocation(Streamy.MODID, "textures/gui/preview_loading.png");
    public static final ResourceLocation LOADING_PROFILE = new ResourceLocation(Streamy.MODID, "textures/gui/icon_loading.png");

    private static final Map<String, ResourceLocation> previews = new ConcurrentHashMap<>();
    private static final Map<String, ResourceLocation> profiles = new ConcurrentHashMap<>();

    private static final ExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadExecutor();

    private static File cacheDir;

    public static void init() {
        cacheDir = new File(Minecraft.getMinecraft().mcDataDir, Streamy.MODID + "/profileCache");
        if (!cacheDir.exists()) {
            Streamy.LOGGER.info("no profile cache found, creating cache directory at {}.", ImageUtil.cacheDir.getAbsolutePath());
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
            previews.clear();
        }
    }

    private static void clearOldFiles() {
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
     *
     * @param url  the url to download the image from, also acts as unique identifier for the image
     * @param name the name of the image
     * @param type cached means to save the downloaded image as file and only refresh it every 30 days.
     * @return the {@link ResourceLocation} that this image can be accessed with
     */
    public static ResourceLocation loadImage(String url, String name, ImageCacheType type) {
        if (type == ImageCacheType.CACHED && profiles.containsKey(url)) return profiles.get(url);
        else if (type == ImageCacheType.LIVE && previews.containsKey(url)) return previews.get(url);
        for (char c : ChatAllowedCharacters.ILLEGAL_FILE_CHARACTERS) {
            name = name.replace(c, '_');
        }

        ResourceLocation imageRL = null;
        TextureManager textureManager = mc.getTextureManager();


        //try to load from file
        if (type == ImageCacheType.CACHED) {
            File image = new File(cacheDir, name + ".png");
            try {
                if (image.exists() && !image.isDirectory()) {
                    imageRL = textureManager.getDynamicTextureLocation(Streamy.MODID + "_" + name + "_" + type, new DynamicTexture(ImageIO.read(image)));
                }
            } catch (Exception e) {
                image.delete();
                Streamy.LOGGER.error("error reading cached profile image, trying to get texture from the web...");
                imageRL = null;
            }
        }

        //else get from web
        if (imageRL == null) {
            final String name2 = name;
            EXECUTOR_SERVICE.execute(() -> {
                try {
                    BufferedImage bufferedImage = ImageIO.read(new URL(url));
                    if (type == ImageCacheType.CACHED) {
                        File image = new File(cacheDir, name2 + ".png");
                        ImageIO.write(bufferedImage, "png", image);
                        Streamy.LOGGER.debug("Successfully downloaded profile image for {} and saved to {}.", name2, image.getName());
                    }
                    Minecraft.getMinecraft().addScheduledTask(() -> {
                        DynamicTexture texture = new DynamicTexture(bufferedImage);
                        ResourceLocation location = textureManager.getDynamicTextureLocation(Streamy.MODID + "_" + name2 + "_" + type, texture);
                        storeLocation(url, location, type);
                        Streamy.LOGGER.debug("Successfully uploaded texture for {} to the texture atlas as {}.", name2, location.toString());
                    });
                } catch (Exception e) {
                    Streamy.LOGGER.error("Exception getting image for {} from {}, type {}", name2, url, type);
                    ResourceLocation resourceLocation;
                    switch (type) {
                        case CACHED:
                            resourceLocation = NO_PROFILE;
                            break;
                        case LIVE:
                            resourceLocation = NO_PREVIEW;
                            break;
                        default:
                            resourceLocation = TextureMap.LOCATION_MISSING_TEXTURE;
                            break;
                    }
                    storeLocation(url, resourceLocation, type);
                }
            });
            switch (type) {
                case CACHED:
                    imageRL = LOADING_PROFILE;
                    break;
                case LIVE:
                    imageRL = LOADING_PREVIEW;
                    break;
            }
        }
        storeLocation(url, imageRL, type);
        return imageRL;
    }

    private static void storeLocation(String url, ResourceLocation location, ImageCacheType type) {
        switch (type) {
            case LIVE:
                synchronized (previews) {
                    previews.put(url, location);
                }
                break;
            case CACHED:
                synchronized (profiles) {
                    profiles.put(url, location);
                }
                break;
        }
    }

    public static boolean shouldShowIcon() {
        return StreamyConfig.GENERAL.enabled && !Streamy.isIconDismissed && !(Streamy.isSelfStreaming && StreamyConfig.GENERAL.streamerMode == EnumStreamerMode.FULL);
    }
}
