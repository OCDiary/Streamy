package mdct.streamy.util;

import java.awt.*;

public enum EnumDirection {
    UP("Up"),
    RIGHT("Right"),
    DOWN("Down"),
    LEFT("Left");

    private final String dirName;

    EnumDirection(String name) {
        dirName = name;
    }

    private Point translate(Point point, int x, int y) {
        point.translate(x, y);
        return point;
    }

    public Point translate(Point point, int amount) {
        switch (this) {
            case UP:
                return translate(point, 0, -amount);
            case RIGHT:
                return translate(point, amount, 0);
            case DOWN:
                return translate(point, 0, amount);
            case LEFT:
                return translate(point, -amount, 0);
            default:
                throw new RuntimeException(String.format("Unhandled EnumDirection %s!", name()));
        }
    }

    @Override
    public String toString() {
        return dirName;
    }
}
