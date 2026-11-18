/*
 * Starter code supplied for Adelaide University COMP3011 Assignment 2.
 * Students are free to modify this file for assessment purposes.
 * 
 * Authors:
 *   1. Simon Ratcliffe, in collaboration with GPT-5.6 Terra
 *   2. <student name and student number insert here upon modification>
 *
 * Copyright 2026 Simon Ratcliffe
 */
package comp3011;

import java.io.File;
import java.util.function.BiConsumer;
import java.util.List;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;

import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;


/**
 * The controller part of the video player model-view-controller architecture.
 * 
 * <p>
 * Wires up events from the {@link VideoPlayerView} to the {@link VideoPlayerModel}.
 * This includes button actions, keyboard shortcuts and display resizing.
 * </p>
 */
public class VideoPlayerController {
    private final VideoPlayerModel model;
    private final VideoPlayerView view;
    private boolean subtitlesEnabled;
    private File subtitleVideo;
    private SubtitleTimeline subtitleTimeline = SubtitleTimeline.empty();
    private long subtitleRequestGeneration;
    private boolean subtitleLoading;
    private boolean subtitleLoaded;
    private final AnimationTimer subtitleTimer = new AnimationTimer() {
        @Override public void handle(long now) {
            if (subtitlesEnabled) {
                view.setSubtitleText(subtitleTimeline.at(model.getCurrentPlaybackTimeSeconds()));
            }
        }
    };
    private final BiConsumer<Integer, Integer> videoSizeChangedHandler;

    public VideoPlayerController(
            boolean audioEnabled,
            List<FrameProcessor> frameProcessors,
            BiConsumer<Integer, Integer> videoSizeChangedHandler) {
        this.view = new VideoPlayerView();
        this.videoSizeChangedHandler = videoSizeChangedHandler;
        model = new VideoPlayerModel(
                audioEnabled,
                frameProcessors,
                this::onVideoSizeChanged,
                this::onFrameReady,
                this::onStatusChanged,
                this::onPlaybackStateChanged,
                this::onAudioOutputStateChanged);

        view.getStartOverButton().setOnAction(_ -> startOver());
        view.getBackFiveButton().setOnAction(_ -> backFiveSeconds());
        view.getPausePlayButton().setOnAction(_ -> togglePause());
        view.getForwardFiveButton().setOnAction(_ -> forwardFiveSeconds());
        view.getStopButton().setOnAction(_ -> stopAndExit());
        view.getAudioButton().setOnAction(_ -> toggleAudioOutput());
        view.getSubtitleButton().setOnAction(_ -> toggleSubtitles());
        view.showAudioEnabled(model.isAudioOutputEnabled());
        subtitleTimer.start();
    }

    public void installKeyboardShortcuts(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                stopAndExit();
                event.consume();
            } else if (event.getCode() == KeyCode.SPACE) {
                togglePause();
                event.consume();
            } else if (event.getCode() == KeyCode.LEFT) {
                backFiveSeconds();
                event.consume();
            } else if (event.getCode() == KeyCode.RIGHT) {
                forwardFiveSeconds();
                event.consume();
            } else if (event.getCode() == KeyCode.HOME) {
                startOver();
                event.consume();
            } else if (event.getCode() == KeyCode.A) {
                toggleAudioOutput();
                event.consume();
            } else if (event.getCode() == KeyCode.C) {
                toggleSubtitles();
                event.consume();
            }
        });
    }

    public VideoPlayerView getView() {
        return view;
    }

    public void play(File file) {
        subtitleVideo = file;
        subtitleRequestGeneration++;
        subtitleTimeline = SubtitleTimeline.empty();
        subtitleLoaded = false;
        subtitleLoading = false;
        view.setSubtitleText("");
        view.setPlaybackControlsDisabled(false);
        model.play(file);
    }

    /** Enable captions from a launch option (without simulating a click). */
    public void enableSubtitlesAtLaunch() {
        if (!subtitlesEnabled) toggleSubtitles();
    }

    public void shutdown() {
        subtitleRequestGeneration++;
        subtitleTimer.stop();
        model.shutdown();
    }

    private void toggleSubtitles() {
        subtitlesEnabled = !subtitlesEnabled;
        view.showSubtitlesEnabled(subtitlesEnabled);
        if (!subtitlesEnabled) {
            view.setSubtitleText("");
            return;
        }
        if (subtitleVideo == null) return;
        if (subtitleLoaded) {
            view.setSubtitleText(subtitleTimeline.at(model.getCurrentPlaybackTimeSeconds()));
            return;
        }
        // Clicking CC repeatedly must not start parallel FFmpeg/HTTP jobs.
        if (subtitleLoading) return;
        subtitleLoading = true;
        final long request = ++subtitleRequestGeneration;
        final File source = subtitleVideo;
        view.setStatus("Loading subtitles...");
        // Never run FFmpeg or an HTTP request on the JavaFX application thread.
        Thread.ofVirtual().name("subtitle-fetch").start(() -> {
            try {
                SubtitleTimeline received = SubtitleClient.fetch(source);
                Platform.runLater(() -> {
                    if (request != subtitleRequestGeneration || source != subtitleVideo) return;
                    subtitleLoading = false;
                    subtitleLoaded = true;
                    subtitleTimeline = received;
                    view.setStatus(received.isEmpty() ? "No speech subtitles available" : "Subtitles ready");
                    if (subtitlesEnabled) {
                        view.setSubtitleText(received.at(model.getCurrentPlaybackTimeSeconds()));
                    }
                });
            } catch (Exception error) {
                Platform.runLater(() -> {
                    if (request != subtitleRequestGeneration || source != subtitleVideo) return;
                    subtitleLoading = false;
                    // Keep subtitles enabled so the user may retry by toggling CC off/on.
                    String detail = error.getMessage();
                    if (detail == null || detail.isBlank()) detail = error.getClass().getSimpleName();
                    view.setStatus("Subtitles unavailable: " + detail);
                });
            }
        });
    }

    private void startOver() {
        model.startOver();
    }

    private void backFiveSeconds() {
        model.backFiveSeconds();
    }

    private void togglePause() {
        model.togglePause();
    }

    private void forwardFiveSeconds() {
        model.forwardFiveSeconds();
    }

    private void toggleAudioOutput() {
        model.toggleAudioOutput();
    }

    private void stopAndExit() {
        model.stopPlayback();
        javafx.application.Platform.exit();
    }

    private void onVideoSizeChanged(int width, int height) {
        videoSizeChangedHandler.accept(width, height);
    }

    private void onFrameReady(Image image) {
        view.setImage(image);
    }

    private void onStatusChanged(String status) {
        view.setStatus(status);
    }

    private void onPlaybackStateChanged(boolean playing, boolean paused) {
        if (playing && !paused) {
            view.showPauseAction();
        } else {
            view.showPlayAction();
        }
    }

    private void onAudioOutputStateChanged(boolean audioOutputEnabled) {
        view.showAudioEnabled(audioOutputEnabled);
    }
}
