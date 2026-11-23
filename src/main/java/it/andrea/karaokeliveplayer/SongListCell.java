package it.andrea.karaokeliveplayer;

import java.util.function.IntSupplier;

import javafx.scene.control.ListCell;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

final class SongListCell extends ListCell<SongListEntry> {

	private final IntSupplier songFontSize;
	private final IntSupplier headerFontSize;

	SongListCell(IntSupplier songFontSize, IntSupplier headerFontSize) {
		this.songFontSize = songFontSize;
		this.headerFontSize = headerFontSize;
	}

	@Override
	protected void updateItem(SongListEntry item, boolean empty) {
		super.updateItem(item, empty);
		setDisable(false);
		setStyle("");
		setOpacity(1);
		if (empty || item == null) {
			setText(null);
		} else if (item instanceof SongEntry songEntry) {
			setText(songEntry.file().getName().replaceAll("\\.mp3$", "").replaceAll("\\.MP3$", ""));
			setFont(Font.font("Arial", FontWeight.BOLD, songFontSize.getAsInt()));
			setTextFill(Color.WHITE);
		} else if (item instanceof SectionHeader header) {
			setText(header.title());
			setFont(Font.font("Arial", FontWeight.BOLD, headerFontSize.getAsInt()));
			setTextFill(Color.LIGHTGRAY);
			setStyle("-fx-opacity: 1;");
			setDisable(true);
		} else if (item instanceof SongListSpacer) {
			setText(" ");
			setDisable(true);
			setOpacity(0);
		}
	}
}
