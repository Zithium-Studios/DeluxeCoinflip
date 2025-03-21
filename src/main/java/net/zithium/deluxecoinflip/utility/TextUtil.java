/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2022 Lewis D (ItsLewizzz). All rights reserved.
 */

package net.zithium.deluxecoinflip.utility;

import org.bukkit.ChatColor;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class TextUtil {

    private static final String N0NCE_ID = "%%__NONCE__%%";
    private static final String US3R_ID = "%%__USER__%%";
    private static final String US3R_ID2 = "%%__USER__%%321";
    private static final String[] SUFFIX = new String[]{"","k", "M", "B", "T"};
    private static final int MAX_LENGTH = 5;
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance(Locale.US);

    public static boolean isMCMarket() {
        String hash = "%__FILEHASH__%";
        return !(hash.charAt(0) + hash + hash.charAt(0)).equals("%%__FILEHASH__%%");
    }

    public static boolean isValidDownload() {
        String hash = "%__USER__%";
        return !(hash.charAt(0) + hash + hash.charAt(0)).equals("%%__USER__%%");
    }

    public static String format(double number) {
        String r = new DecimalFormat("##0E0").format(number);
        r = r.replaceAll("E\\d", SUFFIX[Character.getNumericValue(r.charAt(r.length() - 1)) / 3]);
        while(r.length() > MAX_LENGTH || r.matches("\\d+\\.[a-z]")){
            r = r.substring(0, r.length()-2) + r.substring(r.length() - 1);
        }
        return r;
    }

    public static String numberFormat(double amount) {
        return DECIMAL_FORMAT.format(amount);
    }

    public static String numberFormat(long amount) {
        return NUMBER_FORMAT.format(amount);
    }

    public static String fromList(List<?> list) {
        if (list == null || list.isEmpty()) return null;
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < list.size(); i++) {
            if(ChatColor.stripColor(list.get(i).toString()).isEmpty()) builder.append("\n&r");
            else builder.append(list.get(i).toString()).append(i + 1 != list.size() ? "\n" : "");
        }

        return builder.toString();
    }


}
