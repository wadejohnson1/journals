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

import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.util.AttributeSet;
import android.widget.Checkable;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Adapter that exposes data from a {@link Cursor} to a {@link RecyclerView} widget.
 * <p/>
 * The cursor must include a column with the same name as {@link BaseColumns#_ID} or this class will
 * not work. Additionally, using {@link android.database.MergeCursor} with this class will not work
 * if the merged cursors have overlapping values in their ID columns.
 */
public class CheckableImageView extends AppCompatImageView implements Checkable {

    /**
     * Attribute for setting checked state.
     */
    private static final int[] STATE_CHECKED = {android.R.attr.state_checked};

    /**
     * Set whether the view is in a checked state.
     */
    private boolean mIsChecked = false;
    /**
     * Listener for checked state changes.
     */
    private OnCheckedStateChangedListener mListener = null;

    public CheckableImageView(Context context) {
        super(context);
    }

    public CheckableImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CheckableImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * Get the checked state change listener.
     *
     * @return the current state changed listener, or {@code null} if no listener is set
     */
    public OnCheckedStateChangedListener getOnCheckedStateChangedListener() {
        return mListener;
    }

    @Override
    public boolean isChecked() {
        return mIsChecked;
    }

    @Override
    public int[] onCreateDrawableState(final int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (isChecked())
            mergeDrawableStates(drawableState, STATE_CHECKED);
        return drawableState;
    }

    @Override
    public void setChecked(boolean isChecked) {
        if (mIsChecked != isChecked) {
            mIsChecked = isChecked;
            refreshDrawableState();
            // Notify the listener.
            if (mListener != null) {
                mListener.onCheckStateChanged(this, isChecked);
            }
        }
    }

    /**
     * Set the checked state change listener.
     *
     * @param listener the listener to receive checked state changes, or {@code null} to remove the
     *                 listener
     */
    public void setOnCheckedStateChangedListener(@Nullable OnCheckedStateChangedListener listener) {
        mListener = listener;
    }

    @Override
    public void toggle() {
        setChecked(!mIsChecked);
    }

    public interface OnCheckedStateChangedListener {

        /**
         * Called when the checked state of the image view changes.
         *
         * @param view      the view whose checked state changed
         * @param isChecked {@code true} if the view is checked, {@code false} otherwise
         */
        void onCheckStateChanged(CheckableImageView view, boolean isChecked);

    }

}
