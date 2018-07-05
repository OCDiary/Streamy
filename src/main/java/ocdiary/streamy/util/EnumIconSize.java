package ocdiary.streamy.util;

import net.minecraft.util.IStringSerializable;

import java.util.Locale;

/**
 * @author UpcraftLP
 */
public enum EnumIconSize implements IStringSerializable {

    SMALL(0, 88, 27, 88, 10),
    MEDIUM(0, 39, 25, 39, 17),
    LARGE(0, 0, 24, 0, 21);

    public final int outlineU, outlineV, overlayU, overlayV, size;

    EnumIconSize(int outlineU, int outlineV, int overlayU, int overlayV, int size) {
        this.outlineU = outlineU;
        this.outlineV = outlineV;
        this.overlayU = overlayU;
        this.overlayV = overlayV;
        this.size = size;
    }

    @Override
    public String getName() {
        return this.name().toLowerCase(Locale.ROOT);
    }

}
