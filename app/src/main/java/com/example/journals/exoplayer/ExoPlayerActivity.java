/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.journals.exoplayer;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.journals.R;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer.DecoderInitializationException;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil.DecoderQueryException;
import com.google.android.exoplayer2.source.BehindLiveWindowException;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveVideoTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.DebugTextViewHelper;
import com.google.android.exoplayer2.ui.PlaybackControlView;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

/**
 * An activity that plays media using {@link SimpleExoPlayer}.
 */
public class ExoPlayerActivity extends Activity {

    /**
     *
     */
    public static final String EXTENSION_EXTRA = "extension";

    /**
     * Set whether debug messages should be logged.
     */
    private static final boolean DEBUG = false; // TODO set to false
    /**
     *
     */
    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();
    /**
     *
     */
    private static final CookieManager DEFAULT_COOKIE_MANAGER;

    static {
        DEFAULT_COOKIE_MANAGER = new CookieManager();
        DEFAULT_COOKIE_MANAGER.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
    }

    /**
     *
     */
    private final OnClickListener mOnClickListener = new OnClickListener();
    /**
     * Set whether the player needs a media source.
     */
    private boolean mPlayerNeedsSource;
    /**
     * Set whether the player should begin playback automatically.
     */
    private boolean mShouldAutoPlay;
    /**
     *
     */
    private Button mRetryButton;
    /**
     *
     */
    private DataSource.Factory mMediaDataSourceFactory;
    /**
     *
     */
    private DebugTextViewHelper mDebugViewHelper;
    /**
     *
     */
    private DefaultTrackSelector mTrackSelector;
    /**
     *
     */
    private EventLogger mEventLogger = null;
    /**
     *
     */
    private Handler mainHandler;
    /**
     *
     */
    private int mResumeWindow;
    /**
     *
     */
    private LinearLayout mDebugRootView;
    /**
     *
     */
    private long mResumePosition;
    /**
     * An {@link ExoPlayer} implementation that uses default {@link Renderer} components.
     */
    private SimpleExoPlayer mPlayer;
    /**
     * View for {@link SimpleExoPlayer} media playbacks.
     */
    private SimpleExoPlayerView mSimpleExoPlayerView;
    /**
     *
     */
    private TextView mDebugTextView;
    /**
     *
     */
    private TrackSelectionHelper mTrackSelectionHelper;

    /**
     *
     */
    private static boolean isBehindLiveWindow(ExoPlaybackException e) {
        if (e.type != ExoPlaybackException.TYPE_SOURCE) {
            return false;
        }
        Throwable cause = e.getSourceException();
        while (cause != null) {
            if (cause instanceof BehindLiveWindowException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Show the controls on any key event.
        mSimpleExoPlayerView.showController();
        // If the event was not handled then see if the player view can handle it as a media key event.
        return super.dispatchKeyEvent(event) || mSimpleExoPlayerView.dispatchMediaKeyEvent(event);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initializePlayer();
        } else {
            showToast(R.string.storage_permission_denied);
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.exoplayer_activity);

        mShouldAutoPlay = true;
        clearResumePosition();
        mMediaDataSourceFactory = buildDataSourceFactory(true);
        mainHandler = new Handler();
        if (CookieHandler.getDefault() != DEFAULT_COOKIE_MANAGER) {
            CookieHandler.setDefault(DEFAULT_COOKIE_MANAGER);
        }

        View rootView = findViewById(R.id.root);
        rootView.setOnClickListener(mOnClickListener);
        mDebugRootView = (LinearLayout) findViewById(R.id.controls_root);
        mDebugTextView = (TextView) findViewById(R.id.debug_text_view);
        mRetryButton = (Button) findViewById(R.id.retry_button);
        mRetryButton.setOnClickListener(mOnClickListener);

        mSimpleExoPlayerView = (SimpleExoPlayerView) findViewById(R.id.player_view);
        mSimpleExoPlayerView.setControllerVisibilityListener(new VisibilityListener());
        mSimpleExoPlayerView.requestFocus();

    }

    @Override
    protected void onNewIntent(Intent intent) {
        releasePlayer();
        mShouldAutoPlay = true;
        clearResumePosition();
        setIntent(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (Build.VERSION.SDK_INT <= 23) {
            releasePlayer();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if ((Build.VERSION.SDK_INT <= 23 || mPlayer == null)) {
            initializePlayer();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (Build.VERSION.SDK_INT > 23) {
            initializePlayer();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (Build.VERSION.SDK_INT > 23) {
            releasePlayer();
        }
    }

    /**
     *
     */
    private void initializePlayer() {
        final Intent intent = getIntent();
        if (mPlayer == null) {
            TrackSelection.Factory videoTrackSelectionFactory =
                    new AdaptiveVideoTrackSelection.Factory(BANDWIDTH_METER);
            mTrackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
            mTrackSelectionHelper =
                    new TrackSelectionHelper(mTrackSelector, videoTrackSelectionFactory);
            mPlayer = ExoPlayerFactory
                    .newSimpleInstance(this, mTrackSelector, new DefaultLoadControl());
            mPlayer.addListener(new EventListener());

            // Show event logger debug messages.
            if (DEBUG) {
                mEventLogger = new EventLogger(mTrackSelector);
                mPlayer.addListener(mEventLogger);
                mPlayer.setAudioDebugListener(mEventLogger);
                mPlayer.setVideoDebugListener(mEventLogger);
                mPlayer.setMetadataOutput(mEventLogger);
            }

            mSimpleExoPlayerView.setPlayer(mPlayer);
            mPlayer.setPlayWhenReady(mShouldAutoPlay);
            mDebugViewHelper = new DebugTextViewHelper(mPlayer, mDebugTextView);
            mDebugViewHelper.start();
            mPlayerNeedsSource = true;
        }
        if (mPlayerNeedsSource) {
            final String action = intent.getAction();
            Uri uri;
            final String extension;
            if (Intent.ACTION_VIEW.equals(intent.getAction())) {
                uri = intent.getData();
                extension = intent.getStringExtra(EXTENSION_EXTRA);
            } else {
                showToast(getString(R.string.unexpected_intent_action, action));
                return;
            }
            if (Util.maybeRequestReadExternalStoragePermission(this, uri)) {
                // The player will be reinitialized if the permission is granted.
                return;
            }
            final MediaSource mediaSource = buildMediaSource(uri, extension);
            final boolean haveResumePosition = mResumeWindow != C.INDEX_UNSET;
            if (haveResumePosition) {
                mPlayer.seekTo(mResumeWindow, mResumePosition);
            }
            mPlayer.prepare(mediaSource, !haveResumePosition, false);
            mPlayerNeedsSource = false;
            updateButtonVisibilities();
        }
    }

    /**
     * @param uri               URI of media
     * @param overrideExtension extension to use in place of URI extension, can be {@code null}
     * @return a source of media
     */
    private MediaSource buildMediaSource(@NonNull Uri uri, @Nullable String overrideExtension) {
        int type = Util.inferContentType(
                !TextUtils.isEmpty(overrideExtension) ? "." + overrideExtension :
                        uri.getLastPathSegment());
        switch (type) {
            case C.TYPE_SS:
                return new SsMediaSource(uri, buildDataSourceFactory(false),
                        new DefaultSsChunkSource.Factory(mMediaDataSourceFactory), mainHandler,
                        mEventLogger);
            case C.TYPE_DASH:
                return new DashMediaSource(uri, buildDataSourceFactory(false),
                        new DefaultDashChunkSource.Factory(mMediaDataSourceFactory), mainHandler,
                        mEventLogger);
            case C.TYPE_HLS:
                return new HlsMediaSource(uri, mMediaDataSourceFactory, mainHandler, mEventLogger);
            case C.TYPE_OTHER:
                return new ExtractorMediaSource(uri, mMediaDataSourceFactory,
                        new DefaultExtractorsFactory(),
                        mainHandler, mEventLogger);
            default: {
                throw new IllegalStateException("Unsupported type: " + type);
            }
        }
    }

    /**
     *
     */
    private void releasePlayer() {
        if (mPlayer != null) {
            mDebugViewHelper.stop();
            mDebugViewHelper = null;
            mShouldAutoPlay = mPlayer.getPlayWhenReady();
            updateResumePosition();
            mPlayer.release();
            mPlayer = null;
            mTrackSelector = null;
            mTrackSelectionHelper = null;
            mEventLogger = null;
        }
    }

    /**
     *
     */
    private void updateResumePosition() {
        mResumeWindow = mPlayer.getCurrentWindowIndex();
        mResumePosition =
                mPlayer.isCurrentWindowSeekable() ? Math.max(0, mPlayer.getCurrentPosition())
                        : C.TIME_UNSET;
    }

    /**
     *
     */
    private void clearResumePosition() {
        mResumeWindow = C.INDEX_UNSET;
        mResumePosition = C.TIME_UNSET;
    }

    /**
     * Returns a new DataSource factory.
     *
     * @param useBandwidthMeter Whether to set {@link #BANDWIDTH_METER} as a listener to the new
     *                          DataSource factory.
     * @return A new DataSource factory.
     */
    private DataSource.Factory buildDataSourceFactory(boolean useBandwidthMeter) {
        final DefaultBandwidthMeter bandwidthMeter = useBandwidthMeter ? BANDWIDTH_METER : null;
        return new DefaultDataSourceFactory(ExoPlayerActivity.this, bandwidthMeter,
                buildHttpDataSourceFactory(useBandwidthMeter));
    }

    /**
     * Returns a new HttpDataSource factory.
     *
     * @param useBandwidthMeter Whether to set {@link #BANDWIDTH_METER} as a listener to the new
     *                          DataSource factory.
     * @return A new HttpDataSource factory.
     */
    private HttpDataSource.Factory buildHttpDataSourceFactory(boolean useBandwidthMeter) {
        final String userAgent =
                Util.getUserAgent(ExoPlayerActivity.this, getString(R.string.app_name));
        return new DefaultHttpDataSourceFactory(userAgent,
                useBandwidthMeter ? BANDWIDTH_METER : null);
    }

    /**
     *
     */
    private void updateButtonVisibilities() {
        mDebugRootView.removeAllViews();

        mRetryButton.setVisibility(mPlayerNeedsSource ? View.VISIBLE : View.GONE);
        mDebugRootView.addView(mRetryButton);

        if (mPlayer == null) {
            return;
        }

        MappedTrackInfo mappedTrackInfo = mTrackSelector.getCurrentMappedTrackInfo();
        if (mappedTrackInfo == null) {
            return;
        }

        for (int i = 0; i < mappedTrackInfo.length; i++) {
            TrackGroupArray trackGroups = mappedTrackInfo.getTrackGroups(i);
            if (trackGroups.length != 0) {
                Button button = new Button(this);
                int label;
                switch (mPlayer.getRendererType(i)) {
                    case C.TRACK_TYPE_AUDIO:
                        label = R.string.audio;
                        break;
                    case C.TRACK_TYPE_VIDEO:
                        label = R.string.video;
                        break;
                    case C.TRACK_TYPE_TEXT:
                        label = R.string.text;
                        break;
                    default:
                        continue;
                }
                button.setText(label);
                button.setTag(i);
                button.setOnClickListener(mOnClickListener);
                mDebugRootView.addView(button, mDebugRootView.getChildCount() - 1);
            }
        }
    }

    /**
     *
     */
    private void showControls() {
        mDebugRootView.setVisibility(View.VISIBLE);
    }

    /**
     * Show a toast with the specified message ID.
     *
     * @param messageId the ID of the message to display
     */
    private void showToast(@StringRes int messageId) {
        showToast(getString(messageId));
    }

    /**
     * Show a toast with the specified message.
     *
     * @param message the message to display
     */
    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    /**
     * ExoPlayer.EventListener implementation
     */
    private class EventListener implements ExoPlayer.EventListener {

        @Override
        public void onLoadingChanged(boolean isLoading) {
            // Do nothing.
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            if (playbackState == ExoPlayer.STATE_ENDED) {
                showControls();
            }
            updateButtonVisibilities();
        }

        @Override
        public void onPositionDiscontinuity() {
            if (mPlayerNeedsSource) {
                // This will only occur if the user has performed a seek whilst in the error state. Update the
                // resume position so that if the user then retries, playback will resume from the position to
                // which they seeked.
                updateResumePosition();
            }
        }

        @Override
        public void onTimelineChanged(Timeline timeline, Object manifest) {
            // Do nothing.
        }

        @Override
        public void onPlayerError(ExoPlaybackException e) {
            String errorString = null;
            if (e.type == ExoPlaybackException.TYPE_RENDERER) {
                Exception cause = e.getRendererException();
                if (cause instanceof DecoderInitializationException) {
                    // Special case for decoder initialization failures.
                    DecoderInitializationException decoderInitializationException =
                            (DecoderInitializationException) cause;
                    if (decoderInitializationException.decoderName == null) {
                        if (decoderInitializationException
                                .getCause() instanceof DecoderQueryException) {
                            errorString = getString(R.string.error_querying_decoders);
                        } else if (decoderInitializationException.secureDecoderRequired) {
                            errorString = getString(R.string.error_no_secure_decoder,
                                    decoderInitializationException.mimeType);
                        } else {
                            errorString = getString(R.string.error_no_decoder,
                                    decoderInitializationException.mimeType);
                        }
                    } else {
                        errorString = getString(R.string.error_instantiating_decoder,
                                decoderInitializationException.decoderName);
                    }
                }
            }
            if (errorString != null) {
                showToast(errorString);
            }
            mPlayerNeedsSource = true;
            if (isBehindLiveWindow(e)) {
                clearResumePosition();
                initializePlayer();
            } else {
                updateResumePosition();
                updateButtonVisibilities();
                showControls();
            }
        }

        @Override
        public void onTracksChanged(TrackGroupArray trackGroups,
                                    TrackSelectionArray trackSelections) {
            updateButtonVisibilities();
            MappedTrackInfo mappedTrackInfo = mTrackSelector.getCurrentMappedTrackInfo();
            if (mappedTrackInfo != null) {
                if (mappedTrackInfo.getTrackTypeRendererSupport(C.TRACK_TYPE_VIDEO)
                        == MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
                    showToast(R.string.error_unsupported_video);
                }
                if (mappedTrackInfo.getTrackTypeRendererSupport(C.TRACK_TYPE_AUDIO)
                        == MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
                    showToast(R.string.error_unsupported_audio);
                }
            }
        }

    }

    /**
     * OnClickListener methods
     */
    private class OnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            if (view == mRetryButton) {
                initializePlayer();
            } else if (view.getParent() == mDebugRootView) {
                if (mTrackSelector.getCurrentMappedTrackInfo() != null) {
                    mTrackSelectionHelper
                            .showSelectionDialog(ExoPlayerActivity.this, ((Button) view).getText(),
                                    mTrackSelector.getCurrentMappedTrackInfo(),
                                    (int) view.getTag());
                }
            }
        }

    }

    /**
     * PlaybackControlView.VisibilityListener implementation
     */
    private class VisibilityListener implements PlaybackControlView.VisibilityListener {

        @Override
        public void onVisibilityChange(int visibility) {
            mDebugRootView.setVisibility(visibility);
        }

    }

}
