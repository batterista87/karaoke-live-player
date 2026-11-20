package it.andrea.karaokeliveplayer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class Lyrics3Reader {

	public static String readLyrics(Path path) throws IOException {

	    byte[] data = Files.readAllBytes(path);

	    // Cerca LYRICSBEGIN
	    byte[] beginPattern = "LYRICSBEGIN".getBytes(StandardCharsets.ISO_8859_1);
	    int begin = indexOf(data, beginPattern);
	    if (begin < 0) return "";

	    // Cerca LYRICS200
	    byte[] endPattern = "LYRICS200".getBytes(StandardCharsets.ISO_8859_1);
	    int end = indexOf(data, endPattern, begin);
	    if (end < 0) return "";

	    // Estraggo il blocco tra BEGIN e 200
	    int blockStart = begin + beginPattern.length;
	    int blockEnd = end;
	    byte[] block = Arrays.copyOfRange(data, blockStart, blockEnd);

	    int pos = 0;

	    while (pos + 3 + 5 <= block.length) {

	        // TAG (3 byte)
	        String tag = new String(block, pos, 3, StandardCharsets.ISO_8859_1);
	        pos += 3;

	        // LENGTH (5 byte numerici)
	        String lenStr = new String(block, pos, 5, StandardCharsets.ISO_8859_1);
	        pos += 5;

	        if (!lenStr.matches("\\d{5}"))
	            return "";  // fallback

	        int len = Integer.parseInt(lenStr);

	        // Se il campo eccede il blocco → file malformato
	        if (pos + len > block.length)
	            return "";  // fallback

	        // Se è il campo LYR → estrai lyrics
	        if (tag.equals("LYR")) {
	            return new String(block, pos, len, StandardCharsets.ISO_8859_1).trim();
	        }

	        // Salta il contenuto del campo
	        pos += len;
	    }

	    // Nessun LYR trovato → stringa vuota
	    return "";
	}

    private static int indexOf(byte[] data, byte[] pat) {
        return indexOf(data, pat, 0);
    }

    private static int indexOf(byte[] data, byte[] pat, int offset) {
        outer:
        for (int i = offset; i <= data.length - pat.length; i++) {
            for (int j = 0; j < pat.length; j++)
                if (data[i + j] != pat[j]) continue outer;
            return i;
        }
        return -1;
    }
}