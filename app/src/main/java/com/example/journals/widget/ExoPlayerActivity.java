/*
 * MIT License
 *
 * Copyright (c) 2020 Wade Johnson
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.example.journals.widget;

import android.app.Activity;

import com.google.android.exoplayer2.SimpleExoPlayer;

/**
 * An activity that plays media using {@link SimpleExoPlayer}.
 */
public class ExoPlayerActivity extends Activity {// implements OnClickListener,
//        MappingTrackSelector.EventListener, PlaybackControlView.VisibilityListener {
//
//    public static final String DRM_SCHEME_UUID_EXTRA = "drm_scheme_uuid";
//    public static final String DRM_LICENSE_URL = "drm_license_url";
//    public static final String DRM_KEY_REQUEST_PROPERTIES = "drm_key_request_properties";
//    public static final String PREFER_EXTENSION_DECODERS = "prefer_extension_decoders";
//
//    public static final String ACTION_VIEW = "com.google.android.exoplayer.demo.action.VIEW";
//    public static final String EXTENSION_EXTRA = "extension";
//
//    public static final String ACTION_VIEW_LIST =
//            "com.google.android.exoplayer.demo.action.VIEW_LIST";
//    public static final String URI_LIST_EXTRA = "uri_list";
//    public static final String EXTENSION_LIST_EXTRA = "extension_list";
//
//    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();
//
//
//    private Handler mainHandler;
//    private SimpleExoPlayerView simpleExoPlayerView;
//    private LinearLayout debugRootView;
//    private TextView debugTextView;
//    private Button retryButton;
//
//    private String userAgent;
//    private DataSource.Factory mediaDataSourceFactory;
//    private SimpleExoPlayer player;
//    private MappingTrackSelector trackSelector;
//    private TrackSelectionHelper trackSelectionHelper;
//    private DebugTextViewHelper debugViewHelper;
//    private boolean playerNeedsSource;
//
//    private boolean shouldAutoPlay;
//    private boolean shouldRestorePosition;
//    private int playerWindow;
//    private long playerPosition;
//
//    // Activity lifecycle
//
//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.exoplayer_activity);
//
//        shouldAutoPlay = true;
//        userAgent = Util.getUserAgent(this, "ExoPlayerDemo");
//        mediaDataSourceFactory = buildDataSourceFactory(true);
//        mainHandler = new Handler();
//        if (CookieHandler.getDefault() != DEFAULT_COOKIE_MANAGER) {
//            CookieHandler.setDefault(DEFAULT_COOKIE_MANAGER);
//        }
//
//        View rootView = findViewById(R.id.root);
//        rootView.setOnClickListener(this);
//        debugRootView = (LinearLayout) findViewById(R.id.controls_root);
//        debugTextView = (TextView) findViewById(R.id.debug_text_view);
//        retryButton = (Button) findViewById(R.id.retry_button);
//        retryButton.setOnClickListener(this);
//
//        simpleExoPlayerView = (SimpleExoPlayerView) findViewById(R.id.player_view);
//        simpleExoPlayerView.setControllerVisibilityListener(this);
//        simpleExoPlayerView.requestFocus();
//    }
//
//    @Override
//    public void onNewIntent(Intent intent) {
//        releasePlayer();
//        shouldRestorePosition = false;
//        setIntent(intent);
//    }
//
//    @Override
//    public void onStart() {
//        super.onStart();
//        if (Util.SDK_INT > 23) {
//            initializePlayer();
//        }
//    }
//
//    @Override
//    public void onResume() {
//        super.onResume();
//        if ((Util.SDK_INT <= 23 || player == null)) {
//            initializePlayer();
//        }
//    }
//
//    @Override
//    public void onPause() {
//        super.onPause();
//        if (Util.SDK_INT <= 23) {
//            releasePlayer();
//        }
//    }
//
//    @Override
//    public void onStop() {
//        super.onStop();
//        if (Util.SDK_INT > 23) {
//            releasePlayer();
//        }
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, String[] permissions,
//                                           int[] grantResults) {
//        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//            initializePlayer();
//        } else {
//            showToast(R.string.storage_permission_denied);
//            finish();
//        }
//    }
//
//    // OnClickListener methods
//
//    @Override
//    public void onClick(View view) {
//        if (view == retryButton) {
//            initializePlayer();
//        } else if (view.getParent() == debugRootView) {
//            trackSelectionHelper.showSelectionDialog(this, ((Button) view).getText(),
//                    trackSelector.getTrackInfo(), (int) view.getTag());
//        }
//    }
//
//    // PlaybackControlView.VisibilityListener implementation
//
//    @Override
//    public void onVisibilityChange(int visibility) {
//        debugRootView.setVisibility(visibility);
//    }
//
//    // Internal methods
//
//    private void initializePlayer() {
//        Intent intent = getIntent();
//        if (player == null) {
//            boolean preferExtensionDecoders =
//                    intent.getBooleanExtra(PREFER_EXTENSION_DECODERS, false);
//            UUID drmSchemeUuid = intent.hasExtra(DRM_SCHEME_UUID_EXTRA)
//                    ? UUID.fromString(intent.getStringExtra(DRM_SCHEME_UUID_EXTRA)) : null;
//            DrmSessionManager drmSessionManager = null;
//            if (drmSchemeUuid != null) {
//                String drmLicenseUrl = intent.getStringExtra(DRM_LICENSE_URL);
//                String[] keyRequestPropertiesArray =
//                        intent.getStringArrayExtra(DRM_KEY_REQUEST_PROPERTIES);
//                Map<String, String> keyRequestProperties;
//                if (keyRequestPropertiesArray == null || keyRequestPropertiesArray.length < 2) {
//                    keyRequestProperties = null;
//                } else {
//                    keyRequestProperties = new HashMap<>();
//                    for (int i = 0; i < keyRequestPropertiesArray.length - 1; i += 2) {
//                        keyRequestProperties.put(keyRequestPropertiesArray[i],
//                                keyRequestPropertiesArray[i + 1]);
//                    }
//                }
//                try {
//                    drmSessionManager = buildDrmSessionManager(drmSchemeUuid, drmLicenseUrl,
//                            keyRequestProperties);
//                } catch (UnsupportedDrmException e) {
//                    int errorStringId = Util.SDK_INT < 18 ? R.string.error_drm_not_supported
//                            : (e.reason == UnsupportedDrmException.REASON_UNSUPPORTED_SCHEME
//                            ? R.string.error_drm_unsupported_scheme : R.string.error_drm_unknown);
//                    showToast(errorStringId);
//                    return;
//                }
//            }
//
//            TrackSelection.Factory videoTrackSelectionFactory =
//                    new AdaptiveVideoTrackSelection.Factory(BANDWIDTH_METER);
//            trackSelector = new DefaultTrackSelector(mainHandler, videoTrackSelectionFactory);
//            trackSelector.addListener(this);
//            trackSelectionHelper =
//                    new TrackSelectionHelper(trackSelector, videoTrackSelectionFactory);
//            player = ExoPlayerFactory
//                    .newSimpleInstance(this, trackSelector, new DefaultLoadControl(),
//                            drmSessionManager, preferExtensionDecoders);
//            player.addListener(new EventListener());
//            simpleExoPlayerView.setPlayer(player);
//            if (shouldRestorePosition) {
//                if (playerPosition == C.TIME_UNSET) {
//                    player.seekToDefaultPosition(playerWindow);
//                } else {
//                    player.seekTo(playerWindow, playerPosition);
//                }
//            }
//            player.setPlayWhenReady(shouldAutoPlay);
//            debugViewHelper = new DebugTextViewHelper(player, debugTextView);
//            debugViewHelper.start();
//            playerNeedsSource = true;
//        }
//        if (playerNeedsSource) {
//            String action = intent.getAction();
//            Uri[] uris;
//            String[] extensions;
//            if (ACTION_VIEW.equals(action)) {
//                uris = new Uri[]{intent.getData()};
//                extensions = new String[]{intent.getStringExtra(EXTENSION_EXTRA)};
//            } else if (ACTION_VIEW_LIST.equals(action)) {
//                String[] uriStrings = intent.getStringArrayExtra(URI_LIST_EXTRA);
//                uris = new Uri[uriStrings.length];
//                for (int i = 0; i < uriStrings.length; i++) {
//                    uris[i] = Uri.parse(uriStrings[i]);
//                }
//                extensions = intent.getStringArrayExtra(EXTENSION_LIST_EXTRA);
//                if (extensions == null) {
//                    extensions = new String[uriStrings.length];
//                }
//            } else {
//                showToast(getString(R.string.unexpected_intent_action, action));
//                return;
//            }
//            if (Util.maybeRequestReadExternalStoragePermission(this, uris)) {
//                // The player will be reinitialized if the permission is granted.
//                return;
//            }
//            MediaSource[] mediaSources = new MediaSource[uris.length];
//            for (int i = 0; i < uris.length; i++) {
//                mediaSources[i] = buildMediaSource(uris[i], extensions[i]);
//            }
//            MediaSource mediaSource = mediaSources.length == 1 ? mediaSources[0]
//                    : new ConcatenatingMediaSource(mediaSources);
//            player.prepare(mediaSource, !shouldRestorePosition);
//            playerNeedsSource = false;
//            updateButtonVisibilities();
//        }
//    }
//
//    private MediaSource buildMediaSource(Uri uri, String overrideExtension) {
//        int type = Util.inferContentType(
//                !TextUtils.isEmpty(overrideExtension) ? "." + overrideExtension
//                        : uri.getLastPathSegment());
//        switch (type) {
//            case Util.TYPE_OTHER:
//                return new ExtractorMediaSource(uri, mediaDataSourceFactory,
//                        new DefaultExtractorsFactory(),                        mainHandler, null);
//            default: {
//                throw new IllegalStateException("Unsupported type: " + type);
//            }
//        }
//    }
//
//    private DrmSessionManager buildDrmSessionManager(UUID uuid, String licenseUrl,
//                                                     Map<String, String> keyRequestProperties)
//            throws UnsupportedDrmException {
//        if (Util.SDK_INT < 18) {
//            return null;
//        }
//        HttpMediaDrmCallback drmCallback = new HttpMediaDrmCallback(licenseUrl,
//                buildHttpDataSourceFactory(false), keyRequestProperties);
//        return new StreamingDrmSessionManager<>(uuid,
//                FrameworkMediaDrm.newInstance(uuid), drmCallback, null, mainHandler, null);
//    }
//
//    private void releasePlayer() {
//        if (player != null) {
//            debugViewHelper.stop();
//            debugViewHelper = null;
//            shouldAutoPlay = player.getPlayWhenReady();
//            shouldRestorePosition = false;
//            Timeline timeline = player.getCurrentTimeline();
//            if (timeline != null) {
//                playerWindow = player.getCurrentWindowIndex();
//                Timeline.Window window = timeline.getWindow(playerWindow, new Timeline.Window());
//                if (!window.isDynamic) {
//                    shouldRestorePosition = true;
//                    playerPosition = window.isSeekable ? player.getCurrentPosition() : C.TIME_UNSET;
//                }
//            }
//            player.release();
//            player = null;
//            trackSelector = null;
//            trackSelectionHelper = null;
//        }
//    }
//
//    /**
//     * Returns a new DataSource factory.
//     *
//     * @param useBandwidthMeter Whether to set {@link #BANDWIDTH_METER} as a listener to the new
//     *                          DataSource factory.
//     * @return A new DataSource factory.
//     */
//    private DataSource.Factory buildDataSourceFactory(boolean useBandwidthMeter) {
//        return new DefaultDataSourceFactory(this, useBandwidthMeter ? BANDWIDTH_METER : null,
//                buildHttpDataSourceFactory(useBandwidthMeter));
//    }
//
//    /**
//     * Returns a new HttpDataSource factory.
//     *
//     * @param useBandwidthMeter Whether to set {@link #BANDWIDTH_METER} as a listener to the new
//     *                          DataSource factory.
//     * @return A new HttpDataSource factory.
//     */
//    private HttpDataSource.Factory buildHttpDataSourceFactory(boolean useBandwidthMeter) {
//        return new DefaultHttpDataSourceFactory(userAgent,
//                useBandwidthMeter ? BANDWIDTH_METER : null);
//    }
//
//    // ExoPlayer.EventListener implementation
//
//    private class EventListener implements ExoPlayer.EventListener {
//
//        @Override
//        public void onLoadingChanged(boolean isLoading) {
//            // Do nothing.
//        }
//
//        @Override
//        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
//            if (playbackState == ExoPlayer.STATE_ENDED) {
//                showControls();
//            }
//            updateButtonVisibilities();
//        }
//
//        @Override
//        public void onPositionDiscontinuity() {
//            // Do nothing.
//        }
//
//        @Override
//        public void onTimelineChanged(Timeline timeline, Object manifest) {
//            // Do nothing.
//        }
//
//        @Override
//        public void onPlayerError(ExoPlaybackException e) {
//            String errorString = null;
//            if (e.type == ExoPlaybackException.TYPE_RENDERER) {
//                Exception cause = e.getRendererException();
//                if (cause instanceof DecoderInitializationException) {
//                    // Special case for decoder initialization failures.
//                    DecoderInitializationException decoderInitializationException =
//                            (DecoderInitializationException) cause;
//                    if (decoderInitializationException.decoderName == null) {
//                        if (decoderInitializationException
//                                .getCause() instanceof DecoderQueryException) {
//                            errorString = getString(R.string.error_querying_decoders);
//                        } else if (decoderInitializationException.secureDecoderRequired) {
//                            errorString = getString(R.string.error_no_secure_decoder,
//                                    decoderInitializationException.mimeType);
//                        } else {
//                            errorString = getString(R.string.error_no_decoder,
//                                    decoderInitializationException.mimeType);
//                        }
//                    } else {
//                        errorString = getString(R.string.error_instantiating_decoder,
//                                decoderInitializationException.decoderName);
//                    }
//                }
//            }
//            if (errorString != null) {
//                showToast(errorString);
//            }
//            playerNeedsSource = true;
//            updateButtonVisibilities();
//            showControls();
//        }
//
//    }
//
//    // MappingTrackSelector.EventListener implementation
//
//    @Override
//    public void onTracksChanged(TrackInfo trackInfo) {
//        updateButtonVisibilities();
//        if (trackInfo.hasOnlyUnplayableTracks(C.TRACK_TYPE_VIDEO)) {
//            showToast(R.string.error_unsupported_video);
//        }
//        if (trackInfo.hasOnlyUnplayableTracks(C.TRACK_TYPE_AUDIO)) {
//            showToast(R.string.error_unsupported_audio);
//        }
//    }
//
//    // User controls
//
//    private void updateButtonVisibilities() {
//        debugRootView.removeAllViews();
//
//        retryButton.setVisibility(playerNeedsSource ? View.VISIBLE : View.GONE);
//        debugRootView.addView(retryButton);
//
//        if (player == null) {
//            return;
//        }
//
//        TrackInfo trackInfo = trackSelector.getTrackInfo();
//        if (trackInfo == null) {
//            return;
//        }
//
//        int rendererCount = trackInfo.rendererCount;
//        for (int i = 0; i < rendererCount; i++) {
//            TrackGroupArray trackGroups = trackInfo.getTrackGroups(i);
//            if (trackGroups.length != 0) {
//                Button button = new Button(this);
//                int label;
//                switch (player.getRendererType(i)) {
//                    case C.TRACK_TYPE_AUDIO:
//                        label = R.string.audio;
//                        break;
//                    case C.TRACK_TYPE_VIDEO:
//                        label = R.string.video;
//                        break;
//                    case C.TRACK_TYPE_TEXT:
//                        label = R.string.text;
//                        break;
//                    default:
//                        continue;
//                }
//                button.setText(label);
//                button.setTag(i);
//                button.setOnClickListener(this);
//                debugRootView.addView(button);
//            }
//        }
//    }
//
//    private void showControls() {
//        debugRootView.setVisibility(View.VISIBLE);
//    }
//
//    private void showToast(int messageId) {
//        showToast(getString(messageId));
//    }
//
//    private void showToast(String message) {
//        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
//    }

}
