package it.andrea.karaokeliveplayer;

public class LyricsLine {
    public long timestampMs;
    public String text;

    public LyricsLine(long timestampMs, String text) {
        this.timestampMs = timestampMs;
        this.text = text;
    }
}