package net.defekt.minesweeper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Chars {

    private static final Map<Byte, String> fieldMap = new ConcurrentHashMap<Byte, String>() {
        {
            put((byte) 0, " ");
            put((byte) 10, "-");
            put((byte) 11, "P");
            put((byte) 12, "P");
            put((byte) 13, " ");
            put((byte) 14, "x");
            put((byte) 15, "B");
        }
    };

    public static String getField(byte type) {
        return type > 0 && type < 10 ? Byte.toString(type) : fieldMap.getOrDefault(type, " ");
    }

    private static String letters = "abcdefghijklmnopqrstuvwxyz";

    static {
        letters = letters.toUpperCase() + letters;
    }

    public static String getLetter(int index) {
        int ix = index % letters.length();
        return letters.substring(ix, ix + 1);
    }

    public static String pad(int number, int len) {
        StringBuilder pad = new StringBuilder(Integer.toString(number));
        while (pad.length() < len)
            pad.append(" ");
        return pad.toString();
    }

    public static String padSpace(int len) {
        StringBuilder pad = new StringBuilder("");
        while (pad.length() < len)
            pad.append(" ");
        return pad.toString();
    }
}
