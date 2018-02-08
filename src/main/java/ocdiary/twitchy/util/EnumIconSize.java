package ocdiary.twitchy.util;

import net.minecraft.util.IStringSerializable;

import java.util.Locale;

/**
 * @author UpcraftLP
 */
public enum EnumIconSize implements IStringSerializable {

    SMALL(-1, 87, 26, 87, 10, 10),
    MEDIUM(0, 38, 25, 38, 16, 17),
    LARGE(0, 0, 24, 0, 20, 21);

    public final int outlineU, outlineV, overlayU, overlayV, width, height;

    EnumIconSize(int outlineU, int outlineV, int overlayU, int overlayV, int width, int height) {
        this.outlineU = outlineU;
        this.outlineV = outlineV;
        this.overlayU = overlayU;
        this.overlayV = overlayV;
        this.width = width;
        this.height = height;
    }

    @Override
    public String getName() {
        return this.name().toLowerCase(Locale.ROOT);
    }

}
