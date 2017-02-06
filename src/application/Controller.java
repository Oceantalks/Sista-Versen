package application;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ResourceBundle;

import org.apache.commons.vfs2.FileNotFoundException;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.mp3.Mp3Parser;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.MapChangeListener.Change;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;
import javafx.scene.media.MediaPlayer.Status;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

public class Controller implements Initializable {

	@FXML
	private MediaView mv;

	private MediaPlayer mp;

	private Media mo;

	private final boolean repeat = false;

	private boolean stopRequested = false;

	private boolean atEndOfMedia = false;

	private Duration duration;

	@FXML
	private ImageView image;

	@FXML
	private Label artist;

	@FXML
	private Label album;

	@FXML
	private Label title;

	@FXML
	private Label playTime;

	@FXML
	private Slider timeSlider;

	@FXML
	private Slider volumeSlider;

	@FXML
	private Button play;

	@FXML
	private Button rewind;

	@FXML
	private Button forward;

	private Status status;

	@FXML
	private Button addNewTrack;

	@FXML
	private ListView<String> trackListView;

	// Sets up the Control
	@Override
	public void initialize(URL url, ResourceBundle rb) {

	}

	// Here we our play function depending on what status we act accordingly
	@FXML
	public void play(ActionEvent e) {
		status = mp.getStatus();

		if (status == Status.UNKNOWN || status == Status.HALTED) {
			// don't do anything in these states
			return;
		}

		if (status == Status.PAUSED || status == Status.READY || status == Status.STOPPED || mp.getRate() == 2.0) {
			// rewind the movie if we're sitting at the end
			if (atEndOfMedia) {
				mp.seek(mp.getStartTime());
				atEndOfMedia = false;
			}
			mp.setRate(1.0);
			mp.play();
			play.setStyle("-fx-background-image: url('application/Image/1467320169_audio-video-outline-pause.png');");

		} else {
			mp.pause();
		}

		mp.currentTimeProperty().addListener(new InvalidationListener() {
			public void invalidated(Observable ov) {
				updateValues();
			}
		});

		mp.setOnPlaying(new Runnable() {
			public void run() {
				if (stopRequested) {
					mp.pause();
					stopRequested = false;
				} else {
					play.setStyle(
							"-fx-background-image: url('application/Image/1467320169_audio-video-outline-pause.png');");
				}
			}
		});

		mp.setOnPaused(new Runnable() {
			public void run() {
				System.out.println("onPaused");
				play.setStyle(
						"-fx-background-image: url('application/Image/1467320101_audio-video-outline-play.png');");
			}
		});

		mp.setCycleCount(repeat ? MediaPlayer.INDEFINITE : 1);
		mp.setOnEndOfMedia(new Runnable() {
			public void run() {
				if (!repeat) {
					play.setStyle(
							"-fx-background-image: url('application/Image/1467320101_audio-video-outline-play.png');");
					stopRequested = true;
					atEndOfMedia = true;
				}
			}
		});

		// Loads the selected music in list
		trackListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {

			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				play.setStyle(
						"-fx-background-image: url('application/Image/1467320101_audio-video-outline-play.png');");
				status = mp.getStatus();
				if (status == Status.PLAYING) {
					mp.stop();
				}
				String path = new File(newValue).getAbsolutePath();
				// mv = new MediaView();
				extractMetaData(path);
				mo = new Media(new File(path).toURI().toString());
				mo.getMetadata().addListener((Change<? extends String, ? extends Object> ch) -> {
					if (ch.wasAdded()) {
						handleMetadata(ch.getKey(), ch.getValueAdded());
					}
				});
				mp = new MediaPlayer(mo);
				if (mv != null) {
					mv.setMediaPlayer(mp);
					duration = mp.getTotalDuration();

					mp.setOnReady(new Runnable() {
						public void run() {
							updateValues();
						}
					});

				}
			}
		});
	}

	// speeds the play rate
	@FXML
	private void forward(ActionEvent event) {
		play.setStyle("-fx-background-image: url('application/Image/1467320101_audio-video-outline-play.png');");
		mp.setRate(2.0);
	}

	// Sets the media file to the start of media
	@FXML
	private void rewind(ActionEvent event) {
		play.setStyle("-fx-background-image: url('application/Image/1467320101_audio-video-outline-play.png');");
		mp.seek(mp.getStartTime());
		mp.stop();

	}

	// adds a new media file to list view gets loaded at the same time
	@FXML
	private void addNewTrack(ActionEvent event) {
		String path = new File(add()).getAbsolutePath();
		extractMetaData(path);
		mo = new Media(new File(path).toURI().toString());

		// Checks if the media contains any metadata
		mo.getMetadata().addListener((Change<? extends String, ? extends Object> ch) -> {
			if (ch.wasAdded()) {
				handleMetadata(ch.getKey(), ch.getValueAdded());
			}
		});
		mp = new MediaPlayer(mo);
		if (mv != null) {
			mv.setMediaPlayer(mp);
			duration = mp.getTotalDuration();
			play.setStyle("-fx-background-image: url('application/Image/1467320101_audio-video-outline-play.png');");
			mp.setOnReady(new Runnable() {
				public void run() {
					updateValues();
				}
			});
		}

		if (timeSlider != null) {
			timeSlider.valueProperty().addListener(new InvalidationListener() {
				public void invalidated(Observable ov) {
					if (timeSlider.isValueChanging()) {
						// multiply duration by percentage calculated by slider
						// position
						mp.seek(duration.multiply(timeSlider.getValue() / 100.0));
					}
				}
			});
		}

		if (volumeSlider != null) {
			volumeSlider.setValue((int) Math.round(mp.getVolume() * 100));
			volumeSlider.valueProperty().addListener(new InvalidationListener() {
				public void invalidated(Observable ov) {
					if (volumeSlider.isValueChanging()) {
						mp.setVolume(volumeSlider.getValue() / 100.0);
					}
				}
			});
		}
	}

	public void extractMetaData(String s) {

		try {

			InputStream input = new FileInputStream(new File(s));
			ContentHandler handler = new DefaultHandler();
			Metadata metadata = new Metadata();
			Parser parser = new Mp3Parser();
			ParseContext parseCtx = new ParseContext();
			parser.parse(input, handler, metadata, parseCtx);
			input.close();

			// List all metadata
			String[] metadataNames = metadata.names();

			for (String name : metadataNames) {
				System.out.println(name + ": " + metadata.get(name));
			}

			// Retrieve the necessary info from metadata
			// Names - title, xmpDM:artist etc. - mentioned below may differ
			// based
			System.out.println("----------------------------------------------");
			System.out.println("Title: " + metadata.get("title"));
			System.out.println("Artists: " + metadata.get("xmpDM:artist"));
			System.out.println("Composer : " + metadata.get("xmpDM:composer"));
			System.out.println("Genre : " + metadata.get("xmpDM:genre"));
			System.out.println("Album : " + metadata.get("xmpDM:album"));

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (TikaException e) {
			e.printStackTrace();
		}
	}

	// updates the values for the volume, timeSlider and the current duration.
	protected void updateValues() {
		if (playTime != null && timeSlider != null && volumeSlider != null) {
			Platform.runLater(new Runnable() {
				public void run() {
					duration = mp.getTotalDuration();
					Duration currentTime = mp.currentTimeProperty().getValue();
					String theTime = formatTime(currentTime, duration);
					playTime.setText(theTime);
					timeSlider.setDisable(duration.isUnknown());
					if (!timeSlider.isDisabled() && duration.greaterThan(Duration.ZERO)
							&& !timeSlider.isValueChanging()) {
						timeSlider.setValue((currentTime.toMillis() / duration.toMillis()) * 100.0);
					}
					if (!volumeSlider.isValueChanging()) {
						volumeSlider.setValue((int) Math.round(mp.getVolume() * 100));
					}
				}
			});
		}
	}

	// Formats the elapsed and the total duration of the music file
	private static String formatTime(Duration elapsed, Duration duration) {
		int intElapsed = (int) Math.floor(elapsed.toSeconds());
		int elapsedHours = intElapsed / (60 * 60);
		if (elapsedHours > 0) {
			intElapsed -= elapsedHours * 60 * 60;
		}
		int elapsedMinutes = intElapsed / 60;
		int elapsedSeconds = intElapsed - elapsedHours * 60 * 60 - elapsedMinutes * 60;

		boolean flag = true;
		System.out.println(elapsed);
		if (duration.greaterThan(Duration.ZERO)) {
			System.out.println(flag);
			int intDuration = (int) Math.floor(duration.toSeconds());
			System.out.println(duration);
			int durationHours = intDuration / (60 * 60);
			if (durationHours > 0) {
				intDuration -= durationHours * 60 * 60;
			}
			int durationMinutes = intDuration / 60;
			int durationSeconds = intDuration - durationHours * 60 * 60 - durationMinutes * 60;
			if (durationHours > 0) {
				System.out.println(elapsedSeconds);
				System.out.println(elapsedMinutes);
				System.out.println(durationSeconds);
				System.out.println(durationMinutes);
				return String.format("%d:%02d:%02d/%d:%02d:%02d", elapsedHours, elapsedMinutes, elapsedSeconds,
						durationHours, durationMinutes, durationSeconds);
			} else {
				System.out.println(elapsedSeconds);
				System.out.println(elapsedMinutes);
				System.out.println(durationSeconds);
				System.out.println(durationMinutes);
				return String.format("%02d:%02d/%02d:%02d", elapsedMinutes, elapsedSeconds, durationMinutes,
						durationSeconds);
			}
		} else {
			if (elapsedHours > 0) {
				return String.format("%d:%02d:%02d", elapsedHours, elapsedMinutes, elapsedSeconds);
			} else {
				return String.format("%02d:%02d", elapsedMinutes, elapsedSeconds);
			}
		}
	}

	// add function makes the library available so we can choose media.
	private String add() {
		FileChooser fc = new FileChooser();
		fc.getExtensionFilters().addAll(new ExtensionFilter("MP3 Files only", "*.mp3"));
		File selectedFile = fc.showOpenDialog(null);
		if (selectedFile != null) {
			trackListView.getItems().add(selectedFile.getAbsolutePath());
		} else {
			System.out.println("File is not valid");
		}
		System.out.println("New Track Added");
		return selectedFile.getAbsolutePath();
	}

	// handle the meta data and sets the labels accordingly
	private void handleMetadata(String key, Object value) {
		// As previously shown...
		switch (key) {
		case "album":
			album.setText(value.toString());
			break;
		case "artist":
			artist.setText(value.toString());
			break;
		case "title":
			title.setText(value.toString());
			break;
		case "image":
			image.setImage((Image) value);
		default:
			break;
		}
	}
}
