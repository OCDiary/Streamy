package ocdiary.streamy.util;

import net.minecraft.util.IStringSerializable;

import java.util.Locale;

/**
 * @author UpcraftLP
 */
public enum EnumIconSize implements IStringSerializable {

    SMALL(0, 88, 27, 88, 10, 10),
    MEDIUM(0, 39, 25, 39, 16, 17),
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
