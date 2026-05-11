package com.hewei.hzyjy.xunzhi.interview.shared;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Safely normalizes Fastjson values into plain Java Map/List structures.
 */
public final class InterviewJsonValueNormalizer {

    private InterviewJsonValueNormalizer() {
    }

    public static Map<String, Object> asMap(Object value) {
        Object normalized = normalize(value);
        if (!(normalized instanceof Map<?, ?> rawMap)) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            map.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return map;
    }

    public static List<Object> asList(Object value) {
        Object normalized = normalize(value);
        if (!(normalized instanceof List<?> rawList)) {
            return null;
        }
        return new ArrayList<>(rawList);
    }

    public static Object normalize(Object value) {
        return normalize(value, new IdentityHashMap<>());
    }

    private static Object normalize(Object value, IdentityHashMap<Object, Boolean> visiting) {
        if (value == null || isScalar(value)) {
            return value;
        }

        if (visiting.containsKey(value)) {
            return null;
        }

        if (value instanceof JSONObject jsonObject) {
            visiting.put(value, Boolean.TRUE);
            Map<String, Object> map = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
                map.put(entry.getKey(), normalize(entry.getValue(), visiting));
            }
            visiting.remove(value);
            return map;
        }

        if (value instanceof JSONArray jsonArray) {
            visiting.put(value, Boolean.TRUE);
            List<Object> list = new ArrayList<>(jsonArray.size());
            for (Object item : jsonArray) {
                list.add(normalize(item, visiting));
            }
            visiting.remove(value);
            return list;
        }

        if (value instanceof Map<?, ?> rawMap) {
            visiting.put(value, Boolean.TRUE);
            Map<String, Object> map = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                map.put(String.valueOf(entry.getKey()), normalize(entry.getValue(), visiting));
            }
            visiting.remove(value);
            return map;
        }

        if (value instanceof List<?> rawList) {
            visiting.put(value, Boolean.TRUE);
            List<Object> list = new ArrayList<>(rawList.size());
            for (Object item : rawList) {
                list.add(normalize(item, visiting));
            }
            visiting.remove(value);
            return list;
        }

        return value;
    }

    private static boolean isScalar(Object value) {
        return value instanceof String
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Character
                || value.getClass().isEnum();
    }
}
