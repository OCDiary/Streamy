package ocdiary.twitchy.util;

import net.minecraft.util.IStringSerializable;

import java.util.Locale;

/**
 * @author UpcraftLP
 */
public enum EnumIconSize implements IStringSerializable {

    SMALL(-1, 87, 26, 87),
    MEDIUM(0, 38, 25, 38),
    LARGE(0, 0, 24, 0);

    public final int outlineU, outlineV, overlayU, overlayV;

    EnumIconSize(int outlineU, int outlineV, int overlayU, int overlayV) {
        this.outlineU = outlineU;
        this.outlineV = outlineV;
        this.overlayU = overlayU;
        this.overlayV = overlayV;
    }

    @Override
    public String getName() {
        return this.name().toLowerCase(Locale.ROOT);
    }

}
