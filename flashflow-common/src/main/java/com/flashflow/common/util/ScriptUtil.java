package com.flashflow.common.util;

import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Lua 脚本加载工具（带 ConcurrentHashMap 缓存，避免重复 IO）
 */
public final class ScriptUtil {

    private static final Map<String, String> CACHE = new ConcurrentHashMap<>();

    private ScriptUtil() {}

    public static String load(String path) {
        return CACHE.computeIfAbsent(path, ScriptUtil::readScript);
    }

    private static String readScript(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (Exception e) {
            throw new RuntimeException("加载 Lua 脚本失败: " + path, e);
        }
    }
}
