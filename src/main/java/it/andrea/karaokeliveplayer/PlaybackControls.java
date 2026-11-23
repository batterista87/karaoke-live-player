package it.andrea.karaokeliveplayer;

import javafx.beans.property.DoubleProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

final class PlaybackControls extends VBox {

	private final Slider progressSlider = new Slider();
	private final Label currentTime = new Label("00:00");
	private final Label totalTime = new Label("00:00");
	private final Slider volumeSlider = new Slider(0, 100, 100);
	private final Label volumeValue = new Label("100%");

	PlaybackControls(Button playButton, Button stopButton) {
		currentTime.setTextFill(Color.WHITE);
		currentTime.setFont(Font.font(16));
		totalTime.setTextFill(Color.WHITE);
		totalTime.setFont(Font.font(16));

		HBox mediaControls = new HBox(15, playButton, stopButton, currentTime, progressSlider, totalTime);
		mediaControls.setAlignment(Pos.CENTER);
		mediaControls.setMinWidth(400);
		mediaControls.setPrefWidth(400);
		mediaControls.setMaxWidth(400);

		volumeSlider.setStyle("-fx-control-inner-background: #333;");
		volumeSlider.setPrefWidth(200);
		Label volumeLabel = new Label("Volume");
		volumeLabel.setTextFill(Color.WHITE);
		volumeLabel.setFont(Font.font("Arial", 14));
		volumeValue.setTextFill(Color.WHITE);
		volumeValue.setFont(Font.font("Arial", 14));
		HBox volumeControls = new HBox(10, volumeLabel, volumeSlider, volumeValue);
		volumeControls.setAlignment(Pos.CENTER);

		volumeSlider.valueProperty().addListener((obs, oldValue, newValue) ->
				volumeValue.setText(newValue.intValue() + "%"));

		progressSlider.setFocusTraversable(false);
		volumeSlider.setFocusTraversable(false);
		currentTime.setFocusTraversable(false);
		totalTime.setFocusTraversable(false);
		volumeLabel.setFocusTraversable(false);
		volumeValue.setFocusTraversable(false);

		setSpacing(10);
		getChildren().setAll(mediaControls, volumeControls);
	}

	DoubleProperty volumeValueProperty() {
		return volumeSlider.valueProperty();
	}

	double getVolumeValue() {
		return volumeSlider.getValue();
	}

	void setVolumeValue(double value) {
		volumeSlider.setValue(value);
	}

	void setProgress(double value) {
		progressSlider.setValue(value);
	}

	void setDuration(double duration, String displayText) {
		progressSlider.setMax(duration);
		totalTime.setText(displayText);
	}

	void setCurrentTime(String displayText) {
		currentTime.setText(displayText);
	}

	void setTotalTime(String displayText) {
		totalTime.setText(displayText);
	}
}
