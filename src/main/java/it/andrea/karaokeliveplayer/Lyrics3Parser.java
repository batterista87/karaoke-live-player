package it.andrea.karaokeliveplayer;

import java.util.ArrayList;
import java.util.List;

public class Lyrics3Parser {

    public static List<LyricsLine> parse(String raw) {

        List<LyricsLine> list = new ArrayList<>();
        if (raw == null || raw.isEmpty()) return list;

        String[] lines = raw.split("\\r?\\n");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            long ts = 0;
            String text = line;

            if (line.startsWith("[") && line.indexOf("]") > 0) {
                int close = line.indexOf("]");
                String t = line.substring(1, close);
                text = line.substring(close + 1).trim();

                String[] mmss = t.split(":");
                if (mmss.length == 2) {
                    int mm = Integer.parseInt(mmss[0]);
                    int ss = Integer.parseInt(mmss[1]);
                    ts = (mm * 60 + ss) * 1000L;
                }
            }

            list.add(new LyricsLine(ts, text));
        }

        return list;
    }
}