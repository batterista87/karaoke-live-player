package it.andrea.karaokeliveplayer;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

public class ConfigManager {

    private static final String CONFIG_FILE = "config.properties";
    private final Properties props = new Properties();
    private final Path configPath;

    public ConfigManager() {
        configPath = Paths.get(System.getProperty("user.dir")).resolve(CONFIG_FILE);
        load();
    }

    private void load() {
        if (Files.exists(configPath)) {
            try (InputStream in = Files.newInputStream(configPath)) {
                props.load(in);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void save() {
        try (OutputStream out = Files.newOutputStream(configPath)) {
            props.store(out, "Karaoke Live Player configuration");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setRootFolder(String path) {
        props.setProperty("rootFolder", path);
    }

    public String getRootFolder() {
        return props.getProperty("rootFolder", "");
    }
    
    public void setLyricsFontSize(double size) {
        props.setProperty("lyricsFontSize", Double.toString(size));
    }

    public double getLyricsFontSize() {
        try {
            return Double.parseDouble(props.getProperty("lyricsFontSize", "20"));
        } catch (Exception e) {
            return 20;  // valore di fallback
        }
    }

    public void setLyricsLineSpacing(double size) {
        props.setProperty("lyricsLineSpacing", Double.toString(size));
    }

    public double getLyricsLineSpacing() {
        try {
            double size = Double.parseDouble(props.getProperty("lyricsLineSpacing", "8"));
            int normalized = (int) (Math.round(size / 2.0) * 2);
            return Math.max(0, Math.min(30, normalized));
        } catch (Exception e) {
            return 8;
        }
    }
    
    public int getPopoutFontSize() {
        return Integer.parseInt(props.getProperty("popoutFontSize", "54"));
    }

    public void setPopoutFontSize(int size) {
        props.setProperty("popoutFontSize", String.valueOf(size));
    }
    
}