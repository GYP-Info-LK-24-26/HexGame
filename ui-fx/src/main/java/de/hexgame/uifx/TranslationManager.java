package de.hexgame.uifx;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class TranslationManager {
    private static TranslationManager instance;
    private Map<String, String> translations = new HashMap<>();
    private String currentLang = "en_us";

    private TranslationManager() {}

    public static TranslationManager get() {
        if (instance == null) instance = new TranslationManager();
        return instance;
    }

    public void load(String lang) {
        this.currentLang = lang;
        String path = "lang/" + lang + ".json";
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                System.err.println("Language file not found: " + path);
                return;
            }
            Type type = new TypeToken<Map<String, String>>() {}.getType();
            translations = new Gson().fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), type);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load language file: " + path, e);
        }
    }

    public String translate(String key) {
        return translations.getOrDefault(key, key);
    }

    public String getCurrentLang() {
        return currentLang;
    }
}
