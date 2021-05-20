package me.kuku.onemanager.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApiUtils {

    private static final Map<String, Pattern> patternMap = new HashMap<>();

    public static String regex(String regex, String text){
        Pattern pattern;
        if (patternMap.containsKey(regex))
            pattern = patternMap.get(regex);
        else {
            pattern = Pattern.compile(regex);
            patternMap.put(regex, pattern);
        }
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()){
            return matcher.group();
        }
        return null;
    }

    public static String regex(String first, String last, String text){
        String regex = String.format("(?<=%s).*?(?=%s)", first, last);
        return regex(regex, text);
    }

    private static String random(String str, int length){
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < length; i++){
            int at = (int) (Math.random() * str.length());
            result.append(str.charAt(at));
        }
        return result.toString();
    }

    public static String randomStr(int length){
        return random("1234567890abcdefghijklmnopqrstuvwxyz", length);
    }

    public static String randomNum(int length){
        return random("1234567890", length);
    }

    public static Long randomLong(long min, long max){
        return ((long) (Math.random() * max)) % (max - min + 1) + min;
    }

    public static int randomInt(int min, int max){
        return ((int) (Math.random() * max)) % (max - min + 1) + min;
    }

    public static String removeLastLine(StringBuilder sb){
        return sb.deleteCharAt(sb.length() - 1).toString();
    }
}
