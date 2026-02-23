package de.hexgame.uifx;

import java.io.*;
import java.util.Properties;

public class ConfigManager {
    private static final String CONFIG_FILE = "hexgame-fx.properties";
    private static ConfigManager instance;
    private final Properties properties = new Properties();

    private ConfigManager() {
        load();
    }

    public static ConfigManager get() {
        if (instance == null) instance = new ConfigManager();
        return instance;
    }

    private void load() {
        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                properties.load(fis);
            } catch (IOException e) {
                System.err.println("Failed to load config: " + e.getMessage());
            }
        }
    }

    public void save() {
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            properties.store(fos, "HexGame FX Configuration");
        } catch (IOException e) {
            System.err.println("Failed to save config: " + e.getMessage());
        }
    }

    public String getLang() {
        return properties.getProperty("lang", "en_us");
    }

    public void setLang(String lang) {
        properties.setProperty("lang", lang);
    }
}
