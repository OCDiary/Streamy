package ocdiary.streamy.util;

import java.util.Locale;

/**
 * @author UpcraftLP
 */
public enum EnumPreviewSize {

    SMALL(80, 45),
    MEDIUM(320, 180),
    LARGE(640, 360),
    HD(1280, 720),
    FULL_HD(1920, 1080);

    public final int width, height;

    EnumPreviewSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public String getKey() {
        switch (this) {
            case SMALL:
            case MEDIUM:
            case LARGE:
                return this.name().toLowerCase(Locale.ROOT);
            default:
                return "template";
        }
    }

}
