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

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.MediaController.MediaPlayerControl;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.journals.R;

import java.util.Formatter;
import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;

/**
 * An activity providing media playback.
 */
public class MediaActivity extends AppCompatActivity {

    /**
     * Default timeout for hiding the system UI and hidable views.
     */
    public static final int DEFAULT_TIMEOUT = 3000;

    /**
     * Time (in milliseconds) to rewind video when rewind button is pressed.
     */
    private static final int TIME_REWIND = 5000;
    /**
     * Time (in milliseconds) to advance video when fast forward button is pressed.
     */
    private static final int TIME_FAST_FORWARD = 10000;
    /**
     * Handler message for hiding the system UI and hidable views.
     */
    private static final int FADE_OUT = 1;
    /**
     * Handler message for updating seek bar progress.
     */
    private static final int SHOW_PROGRESS = 2;
    /**
     * Flag used for showing the system UI.
     */
    private static final int SYSTEM_UI_FLAGS_VISIBLE =
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
    /**
     * Flag used for hiding the system UI.
     */
    private static final int SYSTEM_UI_FLAGS_INVISIBLE =
            SYSTEM_UI_FLAGS_VISIBLE | View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;

    /**
     * Set whether the seek bar is currently being updated by the user.
     */
    private boolean mIsDragging;
    /**
     * Set whether the system UI and hidable views are visible.
     */
    private boolean mIsVisible = true;
    /**
     *
     */
    private final StringBuilder mFormatBuilder = new StringBuilder();
    /**
     *
     */
    private final Formatter mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());
    /**
     *
     */
    private SeekBar mSeekBar;
    /**
     *
     */
    private ImageButton mPauseButton;
    /**
     *
     */
    private ImageButton mRewindButton;
    private ImageButton mFastForwardButton;
    /**
     * The last system UI visibility set by this activity.
     */
    private int mLastSystemUiVisibility = 0;
    /**
     * View used to display media controller.
     */
    private View mMediaController;
    /**
     *
     */
    private MediaPlayerControl mPlayer;
    /**
     * Handler used for updating UI components.
     */
    private final Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case FADE_OUT:
                    hideDecorViews();
                    break;
                case SHOW_PROGRESS:
                    if (!mIsDragging && mIsVisible && mPlayer.isPlaying()) {
                        msg = obtainMessage(SHOW_PROGRESS);
                        sendMessageDelayed(msg, 1000 - (setProgress() % 1000));
                    }
                    break;
            }
        }

    };
    /**
     *
     */
    private TextView mCurrentTime;
    /**
     *
     */
    private TextView mEndTime;
    /**
     * View used to display media.
     */
    private VideoView mVideoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.media_activity);

        // *****Set the window mode and system UI visibility.***** //
        getWindow().getDecorView().setSystemUiVisibility(SYSTEM_UI_FLAGS_VISIBLE);
        final View root = findViewById(R.id.layout_video);
        root.setOnClickListener(new SystemUiVisibilityClickListener());
        root.setOnSystemUiVisibilityChangeListener(new OnSystemUiVisibilityChangeListener());

        // *****Set up media controller buttons.***** //
        final OnMediaButtonClickListener listener = new OnMediaButtonClickListener();
        // Set up the rewind button.
        mRewindButton = findViewById(R.id.rewind);
        if (mRewindButton != null) {
            mRewindButton.setOnClickListener(listener);
        }
        // Set up the pause/play button.
        mPauseButton = findViewById(R.id.play_pause);
        if (mPauseButton != null) {
            mPauseButton.requestFocus();
            mPauseButton.setOnClickListener(listener);
        }
        // Set up the fast forward button.
        mFastForwardButton = findViewById(R.id.fastForward);
        if (mFastForwardButton != null) {
            mFastForwardButton.setOnClickListener(listener);
        }
        // Set up the seek bar.
        mSeekBar = findViewById(R.id.progress);
        if (mSeekBar != null) {
            mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener());
            mSeekBar.setMax(1000);
        }
        // Set up the time text.
        mEndTime = findViewById(R.id.time);
        mCurrentTime = findViewById(R.id.time_current);

        // *****Set up the video playback.***** //
        mVideoView = findViewById(R.id.video);
        mMediaController = findViewById(R.id.media_controller);
        mVideoView.setMediaController(this);
        mVideoView.setVideoURI(getIntent().getData());
        mVideoView.start();
    }

    /**
     * Get whether the system UI and hidable views are visible
     *
     * @return whether the system UI and hidable views are visible
     */
    public boolean isShowing() {
        return mIsVisible;
    }

    /**
     *
     */
    public void setEnabled(boolean isEnabled) {
        if (mRewindButton != null) {
            mRewindButton.setEnabled(isEnabled);
        }
        if (mPauseButton != null) {
            mPauseButton.setEnabled(isEnabled);
        }
        if (mFastForwardButton != null) {
            mFastForwardButton.setEnabled(isEnabled);
        }
        //        if (mNextButton != null) { TODO
        //            mNextButton.setEnabled(enabled && mNextListener != null);
        //        }
        //        if (mPrevButton != null) {
        //            mPrevButton.setEnabled(enabled && mPrevListener != null);
        //        }
        if (mSeekBar != null) {
            mSeekBar.setEnabled(isEnabled);
        }
    }

    /**
     * Set the media player control for this activity.
     *
     * @param player the media player control
     */
    public void setMediaPlayer(MediaPlayerControl player) {
        mPlayer = player;
        updatePausePlay();
    }

    /**
     * Show the system UI and hidable layout components on the screen.  They will go away
     * automatically after the specified period of inactivity.
     *
     * @param timeout the timeout in milliseconds before hiding the views, or <= 0 to show the
     *                controller until {@link #hideDecorViews()} is called
     */
    public void showDecorViews(int timeout) {
        if (!mIsVisible) {
            setProgress();
            if (mPauseButton != null) {
                mPauseButton.requestFocus();
            }
            getWindow().getDecorView().setSystemUiVisibility(SYSTEM_UI_FLAGS_VISIBLE);
            mMediaController.setVisibility(View.VISIBLE);
            mIsVisible = true;
        }
        updatePausePlay();

        // cause the progress bar to be updated even if mShowing
        // was already true.  This happens, for example, if we're
        // paused with the progress bar showing the user hits play.
        mHandler.sendEmptyMessage(SHOW_PROGRESS);
        // Hide the views after the specified amount of time.
        if (timeout > 0) {
            mHandler.removeMessages(FADE_OUT);
            Message msg = mHandler.obtainMessage(FADE_OUT);
            mHandler.sendMessageDelayed(msg, timeout);
        }
    }

    /**
     * Remove the system UI and hidable layout components from the screen.
     */
    public void hideDecorViews() {
        if (mIsVisible) {
            mHandler.removeMessages(SHOW_PROGRESS);
            getWindow().getDecorView().setSystemUiVisibility(SYSTEM_UI_FLAGS_INVISIBLE);
            mMediaController.setVisibility(View.INVISIBLE);
            mIsVisible = false;
        }
    }

    /**
     * Pause or resume the media player.
     */
    private void doPauseResume() {
        if (mPlayer.isPlaying()) {
            mPlayer.pause();
        } else {
            mPlayer.start();
        }
        updatePausePlay();
    }

    /**
     * Set the visibility of system UI and hidable layout components.
     *
     * @param isVisible {@code true} to make the system UI and hidable layout components visible,
     *                  {@code false} to hide the system UI and hidable layout components
     */
    private void setDecorVisibility(boolean isVisible) {
        if (mIsVisible != isVisible) {
            final int visibility = isVisible ? SYSTEM_UI_FLAGS_VISIBLE : SYSTEM_UI_FLAGS_INVISIBLE;
            // If we are now visible, schedule a timer for us to go invisible.
            //        else {
            //            mHandler.removeCallbacks(mSystemUiHider);
            //            if (mVideoView.isPlaying()) {
            //                // If the menus are open or play is paused, we will not auto-hide.
            //                mHandler.postDelayed(mSystemUiHider, 3000);
            //            }
            //        }
            // Set the new desired visibility.
            getWindow().getDecorView().setSystemUiVisibility(visibility);
            mMediaController.setVisibility(isVisible ? View.VISIBLE : View.INVISIBLE);
        }
    }

    /**
     * Get a string representation of the specified time.
     *
     * @param time the time (in milliseconds) to convert
     * @return a string representation of {@code time}
     */
    private String stringForTime(int time) {
        final int totalSeconds = time / 1000;
        final int seconds = totalSeconds % 60;
        final int minutes = (totalSeconds / 60) % 60;
        final int hours = totalSeconds / 3600;
        mFormatBuilder.setLength(0);
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    private int setProgress() {
        if (mPlayer == null || mIsDragging) {
            return 0;
        }
        int position = mPlayer.getCurrentPosition();
        int duration = mPlayer.getDuration();
        if (mSeekBar != null) {
            if (duration > 0) {
                // use long to avoid overflow
                long pos = 1000L * position / duration;
                mSeekBar.setProgress((int) pos);
            }
            int percent = mPlayer.getBufferPercentage();
            mSeekBar.setSecondaryProgress(percent * 10);
        }

        if (mEndTime != null) {
            mEndTime.setText(stringForTime(duration));
        }
        if (mCurrentTime != null) {
            mCurrentTime.setText(stringForTime(position));
        }
        return position;
    }

    /**
     * Update the pause/play button icon.
     */
    private void updatePausePlay() {
        if (mPauseButton != null) {
            if (mPlayer.isPlaying()) {
                mPauseButton.setImageResource(R.drawable.ic_pause_white_24dp);
                mPauseButton.setContentDescription(getString(R.string.contentDescription_pause));
            } else {
                mPauseButton.setImageResource(R.drawable.ic_play_arrow_white_24dp);
                mPauseButton.setContentDescription(getString(R.string.contentDescription_play));
            }
        }
    }

    /**
     * Class for updating the system UI visibility to make sure the layout remains stable after each
     * UI visibility change.
     */
    private class OnSystemUiVisibilityChangeListener
            implements View.OnSystemUiVisibilityChangeListener {

        @Override
        public void onSystemUiVisibilityChange(int visibility) {
            // Detect when we go out of nav-hidden mode, to clear our state
            // back to having the full UI chrome up.  Only do this when
            // the state is changing and nav is no longer hidden.
            int difference = mLastSystemUiVisibility ^ visibility;
            mLastSystemUiVisibility = visibility;
            if ((difference & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) != 0
                    && (visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {
                setDecorVisibility(true);
            }
        }

    }

    /**
     * Class for updating the system UI visibility when a view is clicked.
     */
    private class SystemUiVisibilityClickListener implements OnClickListener {

        @Override
        public void onClick(View view) {
            final int visibility = getWindow().getDecorView().getSystemUiVisibility();
            setDecorVisibility((visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) != 0);
        }

    }

    /**
     * Class for handling media button clicks.
     */
    private class OnMediaButtonClickListener implements OnClickListener {

        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.rewind:
                    mPlayer.seekTo(mPlayer.getCurrentPosition() - TIME_REWIND);
                    setProgress();
                    showDecorViews(mPlayer.isPlaying() ? DEFAULT_TIMEOUT : 0);
                    break;
                case R.id.play_pause:
                    doPauseResume();
                    showDecorViews(mPlayer.isPlaying() ? DEFAULT_TIMEOUT : 0);
                    break;
                case R.id.fastForward:
                    mPlayer.seekTo(mPlayer.getCurrentPosition() + TIME_FAST_FORWARD);
                    setProgress();
                    showDecorViews(mPlayer.isPlaying() ? DEFAULT_TIMEOUT : 0);
                    break;
                default:
                    throw new IllegalArgumentException();
            }
        }

    }

    /**
     * There are two scenarios that can trigger the seekbar listener to trigger: The first is the
     * user using the touchpad to adjust the position of the seekbar's thumb. In this case
     * onStartTrackingTouch is called followed by a number of onProgressChanged notifications,
     * concluded by onStopTrackingTouch. We're setting the field "mDragging" to true for the
     * duration of the dragging session to avoid jumps in the position in case of ongoing playback.
     * The second scenario involves the user operating the scroll ball, in this case there WON'T BE
     * onStartTrackingTouch/onStopTrackingTouch notifications,  we will simply apply the updated
     * position without suspending regular updates.
     */
    private class OnSeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {

        @Override
        public void onStartTrackingTouch(SeekBar bar) {
            showDecorViews(0);
            mIsDragging = true;
            // By removing these pending progress messages we make sure
            // that a) we won't update the progress while the user adjusts
            // the seekbar and b) once the user is done dragging the thumb
            // we will post one of these messages to the queue again and
            // this ensures that there will be exactly one message queued up.
            mHandler.removeMessages(SHOW_PROGRESS);
        }

        @Override
        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            // We're not interested in programmatically generated changes to
            // the progress bar's position.
            if (fromuser) {
                long duration = mPlayer.getDuration();
                long newposition = (duration * progress) / 1000L;
                mPlayer.seekTo((int) newposition);
                if (mCurrentTime != null) {
                    mCurrentTime.setText(stringForTime((int) newposition));
                }
            }
        }

        @Override
        public void onStopTrackingTouch(SeekBar bar) {
            mIsDragging = false;
            setProgress();
            updatePausePlay();
            showDecorViews(DEFAULT_TIMEOUT);
            // Ensure that progress is properly updated in the future,
            // the call to show() does not guarantee this because it is a
            // no-op if we are already showing.
            mHandler.sendEmptyMessage(SHOW_PROGRESS);
        }

    }

}