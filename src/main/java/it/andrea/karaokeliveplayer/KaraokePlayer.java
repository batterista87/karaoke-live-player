package it.andrea.karaokeliveplayer;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

public class KaraokePlayer {

	private sealed interface SongListEntry permits SongEntry, SectionHeader {
	}

	private record SongEntry(File file) implements SongListEntry {
	}

	private record SectionHeader(String title) implements SongListEntry {
	}

	private MediaPlayer player;
	private Timeline timeline;
	private Path currentMp3;
	private double lyricsFontSize;
	private double lyricsLineSpacing;

	private Stage lyricsWindow = null;
	private ScrollPane lyricsPaneSecondary = null;

	private int popoutFontSize;
	private List<LyricsLine> currentLyrics;
	private int activeLyricsIndex = -1;

	// MAIN LYRICS FLOW (UNICO)
	private TextFlow flow = new TextFlow();

	// LYRICS della finestra principale (ORA globale)
	private ScrollPane lyricsPane;

	private static String formatTime(double millis) {
		int totalSec = (int) (millis / 1000);
		return String.format("%02d:%02d", totalSec / 60, totalSec % 60);
	}

	public void start(Stage stage) {

		ConfigManager config = new ConfigManager();

		lyricsFontSize = config.getLyricsFontSize();
		lyricsLineSpacing = config.getLyricsLineSpacing();
		popoutFontSize = config.getPopoutFontSize();

		// -------------------------------------------------------
		// LISTA GENERI
		// -------------------------------------------------------
		ListView<File> listGenres = new ListView<>();
		listGenres.setMinWidth(220);
		listGenres.setPrefWidth(220);
		listGenres.setMaxWidth(220);

		String savedRoot = config.getRootFolder();
		if (!savedRoot.isEmpty()) {
			File folder = new File(savedRoot);
			if (folder.exists() && folder.isDirectory()) {
				File[] subs = folder.listFiles(File::isDirectory);
				if (subs != null)
					listGenres.getItems().setAll(subs);
			}
		}

		listGenres.setStyle("""
				    -fx-control-inner-background: black;
				    -fx-selection-bar: #6666ff;
				    -fx-selection-bar-text: white;
				""");

		listGenres.setCellFactory(param -> new ListCell<>() {
			@Override
			protected void updateItem(File item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setText(null);
				} else {
					setText(item.getName());
					setFont(Font.font("Arial", FontWeight.BOLD, 20));
					setTextFill(Color.WHITE);
				}
			}
		});

		// -------------------------------------------------------
		// LISTA CANZONI
		// -------------------------------------------------------
		ListView<SongListEntry> listSongs = new ListView<>();
		listSongs.setMinWidth(420);
		listSongs.setPrefWidth(420);
		listSongs.setMaxWidth(420);

		listSongs.setStyle("""
				    -fx-control-inner-background: black;
				    -fx-selection-bar: #6666ff;
				    -fx-selection-bar-text: white;
				""");

		listSongs.setCellFactory(param -> new ListCell<>() {
			@Override
			protected void updateItem(SongListEntry item, boolean empty) {
				super.updateItem(item, empty);
				setDisable(false);
				setStyle("");
				if (empty || item == null) {
					setText(null);
				} else if (item instanceof SongEntry songEntry) {
					setText(songEntry.file().getName().replaceAll("\\.mp3$", "").replaceAll("\\.MP3$", ""));
					setFont(Font.font("Arial", FontWeight.BOLD, 20));
					setTextFill(Color.WHITE);
				} else if (item instanceof SectionHeader header) {
					setText(header.title());
					setFont(Font.font("Arial", FontWeight.BOLD, 16));
					setTextFill(Color.LIGHTGRAY);
					setStyle("-fx-opacity: 1;");
					setDisable(true);
				}
			}
		});
		listGenres.getStyleClass().add("focus-highlight");
		listSongs.getStyleClass().add("focus-highlight");

		// -------------------------------------------------------
		// LYRICS PANEL (PRINCIPALE)
		// -------------------------------------------------------
		flow.setMinWidth(0);
		flow.setPadding(new Insets(25));
		flow.setStyle("-fx-background-color:#000000;");
		flow.setLineSpacing(lyricsLineSpacing);

		lyricsPane = new ScrollPane(flow); // ✅ ORA è globale
		lyricsPane.setMinWidth(0);
		lyricsPane.setFitToWidth(true);
		lyricsPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
		lyricsPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
		lyricsPane.setStyle("-fx-background:#000000;");

		// -------------------------------------------------------
		// CONTROLLI RIPRODUZIONE
		// -------------------------------------------------------
		Label lblNowPlaying = new Label("In riproduzione: ---");
		lblNowPlaying.setFont(Font.font("Arial", FontWeight.BOLD, 14));
		lblNowPlaying.setTextFill(Color.YELLOW);

		Slider slider = new Slider();

		Label lblTime = new Label("00:00");
		lblTime.setTextFill(Color.WHITE);
		lblTime.setFont(Font.font(16));

		Label lblTotal = new Label("00:00");
		lblTotal.setTextFill(Color.WHITE);
		lblTotal.setFont(Font.font(16));

		Button btnPlay = new Button("▶");
		btnPlay.setStyle("-fx-background-color:#28a745; -fx-text-fill:white; -fx-font-size:18px;");

		Button btnStop = new Button("■");
		btnStop.setStyle("-fx-background-color:#dc3545; -fx-text-fill:white; -fx-font-size:18px;");

		HBox mediaControls = new HBox(15, btnPlay, btnStop, lblTime, slider, lblTotal);
		mediaControls.setAlignment(Pos.CENTER);

		// --- FINE BLOCCO A ---

		// -------------------------------------------------------
		// VOLUME
		// -------------------------------------------------------
		Slider volumeSlider = new Slider(0, 100, 100);
		volumeSlider.setStyle("-fx-control-inner-background: #333;");
		volumeSlider.setPrefWidth(200);

		Label lblVolumeValue = new Label("100%");
		lblVolumeValue.setTextFill(Color.WHITE);
		lblVolumeValue.setFont(Font.font("Arial", 14));

		Label lblVolume = new Label("Volume");
		lblVolume.setTextFill(Color.WHITE);
		lblVolume.setFont(Font.font("Arial", 14));

		HBox volumeControls = new HBox(10, lblVolume, volumeSlider, lblVolumeValue);
		volumeControls.setAlignment(Pos.CENTER);

		volumeSlider.valueProperty().addListener((obs, oldV, newV) -> {
			int val = newV.intValue();
			lblVolumeValue.setText(val + "%");
			if (player != null)
				player.setVolume(val / 100.0);
		});

		VBox controlsColumn = new VBox(lblNowPlaying, mediaControls, volumeControls);
		controlsColumn.setSpacing(10);
		controlsColumn.setPadding(new Insets(10));
		controlsColumn.setStyle("-fx-background-color:#222;");

		// -------------------------------------------------------
		// COLONNA CENTRALE
		// -------------------------------------------------------
		VBox centerColumn = new VBox(listSongs, controlsColumn);
		centerColumn.setSpacing(10);
		centerColumn.setPadding(new Insets(10));
		VBox.setVgrow(listSongs, Priority.ALWAYS);

		// -------------------------------------------------------
		// LAYOUT 3 COLONNE: GENERI | CENTRO | LYRICS
		// -------------------------------------------------------
		HBox centerArea = new HBox(listGenres, centerColumn, lyricsPane);
		centerArea.setSpacing(10);
		centerArea.setPadding(new Insets(10));
		HBox.setHgrow(lyricsPane, Priority.ALWAYS);

		// -------------------------------------------------------
		// BOTTOM BAR: Scegli Cartella | Zoom + | Zoom - | Font popout | Apri popout |
		// Fullscreen
		// -------------------------------------------------------
		Button btnSelectFolder = new Button("📁 Scegli Cartella");
		btnSelectFolder.setStyle(
				"-fx-background-color:#333; -fx-background-radius:8; -fx-text-fill:white; -fx-font-size:14px; -fx-padding:6 14;");

		Button btnZoomIn = new Button("＋");
		btnZoomIn.setStyle(
				"-fx-background-color:#444; -fx-background-radius:6; -fx-text-fill:white; -fx-font-size:18px; -fx-padding:4 10;");

		Button btnZoomOut = new Button("－");
		btnZoomOut.setStyle(
				"-fx-background-color:#444; -fx-background-radius:6; -fx-text-fill:white; -fx-font-size:18px; -fx-padding:4 10;");

		Button btnPopoutLyrics = new Button("Apri finestra Lyrics");
		btnPopoutLyrics.setStyle("-fx-background-color:#555; -fx-text-fill:white; -fx-font-size:14px;");

		ComboBox<Integer> cmbPopoutFont = new ComboBox<>();
		for (int i = 40; i <= 80; i += 2)
			cmbPopoutFont.getItems().add(i);
		cmbPopoutFont.setValue(popoutFontSize);

		cmbPopoutFont.valueProperty().addListener((obs, oldV, newV) -> {
			if (newV == null)
				return;
			lyricsFontSize = newV;
			popoutFontSize = newV;
			config.setLyricsFontSize(lyricsFontSize);
			config.setPopoutFontSize(popoutFontSize);
			config.save();
			applyLyricsZoom();
		});

		ComboBox<Integer> cmbLyricsLineSpacing = new ComboBox<>();
		for (int i = 0; i <= 30; i += 2)
			cmbLyricsLineSpacing.getItems().add(i);
		cmbLyricsLineSpacing.setValue((int) lyricsLineSpacing);

		cmbLyricsLineSpacing.valueProperty().addListener((obs, oldV, newV) -> {
			if (newV == null)
				return;
			lyricsLineSpacing = newV;
			flow.setLineSpacing(lyricsLineSpacing);
			config.setLyricsLineSpacing(lyricsLineSpacing);
			config.save();
		});
		cmbLyricsLineSpacing.setFocusTraversable(false);

		Button btnFull = new Button("⛶");
		btnFull.setStyle("-fx-font-size:20px; -fx-padding:6; -fx-background-color:#444; -fx-text-fill:white;");

		Region bottomSpacer = new Region();
		HBox.setHgrow(bottomSpacer, Priority.ALWAYS);

		HBox bottomBar = new HBox(10, btnSelectFolder, btnZoomIn, btnZoomOut, new Label("Font Lyrics:"), cmbPopoutFont,
				new Label("Spaziatura righe:"), cmbLyricsLineSpacing, btnPopoutLyrics, bottomSpacer, btnFull);
		bottomBar.setPadding(new Insets(10));
		bottomBar.setStyle("-fx-background-color:#222;");
		bottomBar.setAlignment(Pos.CENTER_LEFT);

		// -----------------------------------------------------------
		// FOCUS CONTROL: SOLO listGenres e listSongs devono poter
		// prendere il focus tramite TAB
		// -----------------------------------------------------------

		// ✅ Queste sono le uniche due listbox che devono essere tabbabili
		listGenres.setFocusTraversable(true);
		listSongs.setFocusTraversable(true);

		// ✅ Tutto il resto NON deve prendere focus
		btnPlay.setFocusTraversable(false);
		btnStop.setFocusTraversable(false);

		btnSelectFolder.setFocusTraversable(false);
		btnZoomIn.setFocusTraversable(false);
		btnZoomOut.setFocusTraversable(false);
		btnPopoutLyrics.setFocusTraversable(false);
		btnFull.setFocusTraversable(false);

		slider.setFocusTraversable(false);
		volumeSlider.setFocusTraversable(false);

		flow.setFocusTraversable(false);
		lyricsPane.setFocusTraversable(false);

		lblTime.setFocusTraversable(false);
		lblTotal.setFocusTraversable(false);
		lblNowPlaying.setFocusTraversable(false);
		lblVolume.setFocusTraversable(false);
		lblVolumeValue.setFocusTraversable(false);

		cmbPopoutFont.setFocusTraversable(false);

		// -------------------------------------------------------
		// ROOT + SCENA
		// -------------------------------------------------------
		BorderPane root = new BorderPane();
		root.setStyle("-fx-background-color:#000000;");
		root.setCenter(centerArea);
		root.setBottom(bottomBar);

		Scene scene = new Scene(root, 1280, 720);
		scene.getStylesheets()
				.add("data:," + ".focus-highlight:focused {" + "    -fx-border-color: yellow;"
						+ "    -fx-border-width: 3px;" + "    -fx-border-radius: 6px;" + "    -fx-background-insets: 0;"
						+ "    -fx-effect: dropshadow(gaussian, yellow, 10, 0.8, 0, 0);" + "}");
		stage.setScene(scene);
		stage.setTitle("Karaoke Live Player");
		stage.show();
		stage.setOnCloseRequest(e -> {
			try {
				disposeCurrentAudio();
			} finally {
				try {
					if (lyricsWindow != null)
						lyricsWindow.close();
				} finally {
					Platform.exit();
				}
			}
		});

		// -------------------------------------------------------
		// HOTKEYS
		// -------------------------------------------------------
		scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
			if (e.getTarget() instanceof Node target) {
				if (isWithin(target, lyricsPane)) {
					e.consume();
					return;
				}
				if (isWithin(target, listSongs)
						&& (e.getCode() == KeyCode.UP || e.getCode() == KeyCode.DOWN)) {
					moveSongSelection(listSongs, e.getCode() == KeyCode.UP ? -1 : 1);
					e.consume();
					return;
				}
			}

			if (e.getCode() == KeyCode.SPACE) {
				if (player != null) {
					disposeCurrentAudio();
					slider.setValue(0);
					lblNowPlaying.setText("In riproduzione: ---");
					lblTime.setText("00:00");
					lblTotal.setText("--:--");
				}
				e.consume();
			}

			if (e.getCode() == KeyCode.F11) {
				stage.setFullScreen(!stage.isFullScreen());
				e.consume();
			}

			// Volume giù - A
			if (e.getCode() == KeyCode.A) {
				double v = Math.max(0, volumeSlider.getValue() - 2);
				volumeSlider.setValue(v);
				e.consume();
			}

			// Volume su - S
			if (e.getCode() == KeyCode.S) {
				double v = Math.min(100, volumeSlider.getValue() + 2);
				volumeSlider.setValue(v);
				e.consume();
			}
		});

		// --- FINE BLOCCO B ---

		// -------------------------------------------------------
		// POP-OUT LYRICS WINDOW
		// -------------------------------------------------------
		btnPopoutLyrics.setOnAction(ev -> {

			if (lyricsWindow != null) {
				lyricsWindow.requestFocus();
				return;
			}

			// ✅ disabilita zoom finestra principale
			btnZoomIn.setDisable(true);
			btnZoomOut.setDisable(true);

			lyricsWindow = new Stage();
			lyricsWindow.setTitle("Lyrics - Monitor");

			// ✅ crea ScrollPane popout e sposta il flow
			lyricsPaneSecondary = new ScrollPane(flow);
			lyricsPaneSecondary.setMinWidth(0);
			lyricsPaneSecondary.setFocusTraversable(false);
			lyricsPaneSecondary.setFitToWidth(true);
			lyricsPaneSecondary.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
			lyricsPaneSecondary.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); // ✅ Rimuove scrollbar verticale
			lyricsPaneSecondary.setStyle("-fx-background:black;");

			// ✅ rimuove il flow dalla finestra principale
			lyricsPane.setContent(null);

			// ✅ sincronizzazione immediata (scroll + highlight + font)
			double now = (player != null) ? player.getCurrentTime().toMillis() : 0;

			// scroll identico a quello principale
			lyricsPaneSecondary.setVvalue(lyricsPane.getVvalue());

			// applica font popout selezionato
			flow.getChildren().forEach(n -> {
				if (n instanceof Text t)
					t.setFont(Font.font("Arial", popoutFontSize));
			});

			// highlight + scroll subito
			updateLyricsHighlightAndScroll(now);

			// crea scena popout
			Scene sc = new Scene(lyricsPaneSecondary, 900, 700);
			sc.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
				if (e.getTarget() instanceof Node target && isWithin(target, lyricsPaneSecondary))
					e.consume();
			});
			lyricsWindow.setScene(sc);
			lyricsWindow.show();

			// assicurati che dopo il render lo scroll sia corretto
			Platform.runLater(() -> updateLyricsHighlightAndScroll(now));

			// -------------------------------------------------------
			// CHIUSURA DELLA POP-OUT
			// -------------------------------------------------------
			lyricsWindow.setOnCloseRequest(e -> {

				// ✅ ripristina font della finestra principale
				flow.getChildren().forEach(n -> {
					if (n instanceof Text t)
						t.setFont(Font.font("Arial", lyricsFontSize));
				});

				// ✅ rimetti il flow nella main window
				lyricsPane.setContent(flow);

				lyricsWindow = null;
				lyricsPaneSecondary = null;

				// ✅ riattiva zoom
				btnZoomIn.setDisable(false);
				btnZoomOut.setDisable(false);
			});
		});

		// -------------------------------------------------------
		// PLAY
		// -------------------------------------------------------
		btnPlay.setOnAction(e -> {
			if (currentMp3 != null)
				playMp3(currentMp3, slider, volumeSlider, lblTime, lblTotal, lblNowPlaying);
		});

		// -------------------------------------------------------
		// STOP
		// -------------------------------------------------------
		btnStop.setOnAction(e -> {
			if (player != null) {
				disposeCurrentAudio();
				slider.setValue(0);
				lblTime.setText("00:00");
				lblNowPlaying.setText("In riproduzione: ---");
			}
		});

		// -------------------------------------------------------
		// EVENTI LISTE
		// -------------------------------------------------------
		listGenres.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
			if (n != null)
				loadSongs(listGenres, listSongs);
		});

		listSongs.setOnMouseClicked(e -> {
			if (e.getClickCount() == 2 && isSongCell(e.getPickResult().getIntersectedNode())
					&& listSongs.getSelectionModel().getSelectedItem() instanceof SongEntry) {
				listGenres.requestFocus();
				playSelected(listSongs, slider, volumeSlider, lblTime, lblTotal, lblNowPlaying);
			}
		});

		listSongs.setOnKeyPressed(e -> {
			if (e.getCode() == KeyCode.ENTER
					&& listSongs.getSelectionModel().getSelectedItem() instanceof SongEntry) {
				listGenres.requestFocus();
				playSelected(listSongs, slider, volumeSlider, lblTime, lblTotal, lblNowPlaying);
			}
		});

		// -------------------------------------------------------
		// ZOOM LYRICS (MAIN WINDOW)
		// -------------------------------------------------------
		btnZoomIn.setOnAction(e -> {
			lyricsFontSize += 2;
			applyLyricsZoom();
			config.setLyricsFontSize(lyricsFontSize);
			config.save();
		});

		btnZoomOut.setOnAction(e -> {
			if (lyricsFontSize > 10) {
				lyricsFontSize -= 2;
				applyLyricsZoom();
				config.setLyricsFontSize(lyricsFontSize);
				config.save();
			}
		});

		// -------------------------------------------------------
		// SCEGLI CARTELLA
		// -------------------------------------------------------
		btnSelectFolder.setOnAction(e -> {
			DirectoryChooser chooser = new DirectoryChooser();
			File selected = chooser.showDialog(stage);
			if (selected != null) {
				config.setRootFolder(selected.getAbsolutePath());
				config.save();
				File[] subs = selected.listFiles(File::isDirectory);
				if (subs != null)
					listGenres.getItems().setAll(subs);
			}
		});

	} // ✅ FINE METODO START

	// --- FINE BLOCCO C ---

	// =====================================================================
	// METODI DI SUPPORTO
	// =====================================================================

	private void applyLyricsZoom() {
		flow.getChildren().forEach(n -> {
			if (n instanceof Text t)
				t.setFont(Font.font("Arial", lyricsFontSize));
		});
	}

	private static boolean isWithin(Node node, Node ancestor) {
		for (Node current = node; current != null; current = current.getParent()) {
			if (current == ancestor)
				return true;
		}
		return false;
	}

	private static boolean isSongCell(Node node) {
		for (Node current = node; current != null; current = current.getParent()) {
			if (current instanceof ListCell<?> cell)
				return cell.getItem() instanceof SongEntry;
		}
		return false;
	}

	private static void moveSongSelection(ListView<SongListEntry> listSongs, int direction) {
		int selectedIndex = listSongs.getSelectionModel().getSelectedIndex();
		int index = selectedIndex < 0
				? (direction > 0 ? 0 : listSongs.getItems().size() - 1)
				: selectedIndex + direction;

		while (index >= 0 && index < listSongs.getItems().size()) {
			if (listSongs.getItems().get(index) instanceof SongEntry) {
				listSongs.getSelectionModel().clearAndSelect(index);
				return;
			}
			index += direction;
		}
	}

	private void playSelected(ListView<SongListEntry> listSongs, Slider slider, Slider volumeSlider, Label lblTime,
			Label lblTotal, Label lblNowPlaying) {

		SongListEntry selected = listSongs.getSelectionModel().getSelectedItem();
		if (selected instanceof SongEntry songEntry)
			playMp3(songEntry.file().toPath(), slider, volumeSlider, lblTime, lblTotal, lblNowPlaying);
	}

	private void loadSongs(ListView<File> genres, ListView<SongListEntry> songs) {
		File genre = genres.getSelectionModel().getSelectedItem();
		if (genre != null) {
			File[] mp3Files = genre.listFiles((d, n) -> n.toLowerCase().endsWith(".mp3"));
			if (mp3Files == null) {
				songs.getItems().clear();
				System.err.println("Impossibile leggere la cartella dei brani: " + genre.getAbsolutePath());
				return;
			}

			List<SongListEntry> entries = new ArrayList<>();
			File[] subfolders = genre.listFiles(File::isDirectory);
			List<File[]> subfolderSongs = new ArrayList<>();
			List<File> populatedSubfolders = new ArrayList<>();
			if (subfolders != null) {
				Arrays.sort(subfolders, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER)
						.thenComparing(File::getName));
				for (File subfolder : subfolders) {
					File[] nestedMp3s = subfolder.listFiles((d, n) -> n.toLowerCase().endsWith(".mp3"));
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

			if (populatedSubfolders.isEmpty()) {
				for (File mp3 : mp3Files)
					entries.add(new SongEntry(mp3));
			} else {
				if (mp3Files.length > 0) {
					entries.add(new SectionHeader(genre.getName()));
					for (File mp3 : mp3Files)
						entries.add(new SongEntry(mp3));
				}
				for (int i = 0; i < populatedSubfolders.size(); i++) {
					entries.add(new SectionHeader(populatedSubfolders.get(i).getName()));
					for (File mp3 : subfolderSongs.get(i))
						entries.add(new SongEntry(mp3));
				}
			}
			songs.getItems().setAll(entries);
		}
	}

	private void playMp3(Path mp3, Slider slider, Slider volumeSlider, Label lblTime, Label lblTotal,
			Label lblNowPlaying) {

		try {
// -------------------------------------------------------
// STOP & CLEANUP PRECEDENTE PLAYER + TIMELINE
// -------------------------------------------------------
			disposeCurrentAudio();

			currentMp3 = mp3;
			lyricsPane.setVvalue(0);

			lblNowPlaying.setText("In riproduzione: " + mp3.getFileName().toString().replace(".mp3", "").replace(".MP3", ""));

// -------------------------------------------------------
// CARICA LYRICS
// -------------------------------------------------------
			String raw = Lyrics3Reader.readLyrics(mp3);
			currentLyrics = Lyrics3Parser.parse(raw);
			activeLyricsIndex = -1;

			flow.getChildren().clear();
			Font lyricsFont = Font.font("Arial", lyricsWindow != null ? popoutFontSize : lyricsFontSize);
			for (LyricsLine l : currentLyrics) {
				Text t = new Text(l.text + "\n");
				t.setFill(Color.rgb(180, 180, 180));
				t.setFont(lyricsFont);
				flow.getChildren().add(t);
			}

// -------------------------------------------------------
// CREA NUOVO MEDIAPLAYER
// -------------------------------------------------------
			MediaPlayer newPlayer = new MediaPlayer(new Media(mp3.toUri().toString()));
			player = newPlayer;

// Quando il player è pronto
			newPlayer.setOnReady(() -> {
				if (player != newPlayer)
					return;
				double total = newPlayer.getTotalDuration().toMillis();
				slider.setMax(total);
				lblTotal.setText(formatTime(total));
				newPlayer.setVolume(volumeSlider.getValue() / 100.0);
			});

// Quando il brano finisce
			newPlayer.setOnEndOfMedia(() -> {
				if (player != newPlayer)
					return;
				disposeCurrentAudio();
				slider.setValue(0);
				lblTime.setText("00:00");
				lblNowPlaying.setText("In riproduzione: ---");
			});

// Gestione errori
			newPlayer.setOnError(() -> {
				if (player != newPlayer)
					return;
				System.err.println("Errore MediaPlayer: " + newPlayer.getError());
				disposeCurrentAudio();
			});

// -------------------------------------------------------
// TIMELINE SICURA
// -------------------------------------------------------
			timeline = new Timeline(new KeyFrame(Duration.millis(100), e -> {

				// Protezione: se il player non sta riproducendo, non aggiornare
				if (player != newPlayer || newPlayer.getStatus() != MediaPlayer.Status.PLAYING)
					return;

				double now = newPlayer.getCurrentTime().toMillis();

				// Aggiorna slider
				slider.setValue(now);

				// Aggiorna tempo trascorso
				lblTime.setText(formatTime(now));

				// Aggiorna lyrics + scroll
				updateLyricsHighlightAndScroll(now);

			}));

			timeline.setCycleCount(Animation.INDEFINITE);
			timeline.play();

// -------------------------------------------------------
// AVVIA RIPRODUZIONE
// -------------------------------------------------------
			newPlayer.seek(Duration.ZERO);
			newPlayer.play();

		} catch (Exception ex) {
			ex.printStackTrace();
			disposeCurrentAudio();
		}
	}

	private void disposeCurrentAudio() {
		Timeline oldTimeline = timeline;
		timeline = null;
		if (oldTimeline != null) {
			try {
				oldTimeline.stop();
			} catch (RuntimeException ex) {
				System.err.println("Errore durante l'arresto della timeline: " + ex.getMessage());
			}
		}

		MediaPlayer oldPlayer = player;
		player = null;

		if (oldPlayer != null) {
			try {
				oldPlayer.setOnReady(null);
			} catch (RuntimeException ex) {
				System.err.println("Errore durante la rimozione dell'handler ready: " + ex.getMessage());
			}
			try {
				oldPlayer.setOnEndOfMedia(null);
			} catch (RuntimeException ex) {
				System.err.println("Errore durante la rimozione dell'handler end: " + ex.getMessage());
			}
			try {
				oldPlayer.setOnError(null);
			} catch (RuntimeException ex) {
				System.err.println("Errore durante la rimozione dell'handler error: " + ex.getMessage());
			}

			try {
				oldPlayer.stop();
			} catch (RuntimeException ex) {
				System.err.println("Errore durante l'arresto del MediaPlayer: " + ex.getMessage());
			}

			try {
				oldPlayer.dispose();
			} catch (RuntimeException ex) {
				System.err.println("Errore durante lo rilascio del MediaPlayer: " + ex.getMessage());
			}
		}
	}

	private void updateLyricsHighlightAndScroll(double now) {

		if (currentLyrics == null || currentLyrics.isEmpty())
			return;

		ScrollPane activePane = (lyricsWindow != null) ? lyricsPaneSecondary : lyricsPane;
		TextFlow activeFlow = (TextFlow) activePane.getContent();

		int activeIndex = findActiveLyricsIndex(now);
		if (activeIndex != activeLyricsIndex) {
			if (activeLyricsIndex >= 0)
				((Text) activeFlow.getChildren().get(activeLyricsIndex)).setFill(Color.rgb(180, 180, 180));
			if (activeIndex >= 0)
				((Text) activeFlow.getChildren().get(activeIndex)).setFill(Color.YELLOW);
			activeLyricsIndex = activeIndex;
		}

		if (activeIndex < 0)
			return;

		// --- SCROLL PRECISO ---

		Text activeNode = (Text) activeFlow.getChildren().get(activeIndex);

		// Coordinate reali della riga attiva
		double y = activeNode.getBoundsInParent().getMinY();

		// Altezza totale del contenuto
		double contentHeight = activeFlow.getBoundsInLocal().getHeight();

		// Altezza visibile
		double viewport = activePane.getViewportBounds().getHeight();
		double scrollRange = contentHeight - viewport;
		if (scrollRange <= 0) {
			activePane.setVvalue(0);
			return;
		}

		// Calcolo del target (0..1)
		double target = (y - viewport * 0.3) / scrollRange;
		target = Math.max(0, Math.min(1, target));

		// Smooth scroll
		double current = activePane.getVvalue();
		double newValue = current + (target - current) * 0.15;

		activePane.setVvalue(newValue);
	}

	private int findActiveLyricsIndex(double now) {
		if (Double.isNaN(now))
			return -1;

		int low = 0;
		int high = currentLyrics.size();
		while (low < high) {
			int middle = low + (high - low) / 2;
			if (currentLyrics.get(middle).timestampMs <= now)
				low = middle + 1;
			else
				high = middle;
		}
		return low == 0 ? -1 : low - 1;
	}
}