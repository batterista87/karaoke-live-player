package it.andrea.karaokeliveplayer;

import java.io.File;

sealed interface SongListEntry permits SongEntry, SectionHeader, SongListSpacer {
}

record SongEntry(File file) implements SongListEntry {
}

record SectionHeader(String title) implements SongListEntry {
}

record SongListSpacer() implements SongListEntry {
}
