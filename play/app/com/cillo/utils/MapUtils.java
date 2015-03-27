package com.cillo.utils;

import java.util.Map;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class MapUtils {

    private static java.lang.reflect.Type tt = new TypeToken<Map<String, String>>() {}.getType();
    private static Gson gson = new Gson();

    public static Map<String, String> deserializeMap(String s) {
        return gson.fromJson(s, tt);
    }

    public static String serializeMap(Map<String, String> m) {
        return gson.toJson(m);
    }

}