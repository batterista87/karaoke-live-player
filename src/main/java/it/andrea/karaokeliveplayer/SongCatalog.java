package it.andrea.karaokeliveplayer;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

final class SongCatalog {

	private SongCatalog() {
	}

	static List<SongListEntry> load(File genre, boolean includeSubfolders) {
		File[] mp3Files = genre.listFiles((directory, name) -> name.toLowerCase().endsWith(".mp3"));
		if (mp3Files == null) {
			System.err.println("Impossibile leggere la cartella dei brani: " + genre.getAbsolutePath());
			return List.of();
		}

		List<SongListEntry> entries = new ArrayList<>();
		List<File[]> subfolderSongs = new ArrayList<>();
		List<File> populatedSubfolders = new ArrayList<>();
		if (includeSubfolders) {
			File[] subfolders = genre.listFiles(File::isDirectory);
			if (subfolders != null) {
				Arrays.sort(subfolders, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER)
						.thenComparing(File::getName));
				for (File subfolder : subfolders) {
					File[] nestedMp3s = subfolder.listFiles((directory, name) -> name.toLowerCase().endsWith(".mp3"));
					if (nestedMp3s == null) {
						System.err.println("Impossibile leggere la sottocartella dei brani: "
								+ subfolder.getAbsolutePath());
					} else if (nestedMp3s.length > 0) {
						populatedSubfolders.add(subfolder);
						subfolderSongs.add(nestedMp3s);
					}
				}
			} else {
				System.err.println("Impossibile leggere le sottocartelle di: " + genre.getAbsolutePath());
			}
		}

		if (populatedSubfolders.isEmpty()) {
			for (File mp3 : mp3Files)
				entries.add(new SongEntry(mp3));
		} else {
			if (mp3Files.length > 0) {
				entries.add(new SectionHeader(genre.getName()));
				for (File mp3 : mp3Files)
					entries.add(new SongEntry(mp3));
				entries.add(new SongListSpacer());
			}
			for (int i = 0; i < populatedSubfolders.size(); i++) {
				entries.add(new SectionHeader(populatedSubfolders.get(i).getName()));
				for (File mp3 : subfolderSongs.get(i))
					entries.add(new SongEntry(mp3));
			}
		}
		return entries;
	}
}
