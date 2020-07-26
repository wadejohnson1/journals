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
import android.util.AttributeSet;

import com.android.volley.toolbox.NetworkImageView;

/**
 * A view that can call listeners when its size changes.
 */
public class SizeListenerImageView extends NetworkImageView {

    /**
     * Listeners for size changed events.
     */
    private OnSizeChangedListener mListener = null;

    public SizeListenerImageView(Context context) {
        super(context);
    }

    public SizeListenerImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SizeListenerImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Set the specified listener for this view.
     *
     * @param listener the listener to set, or {@code null} to remove a listener
     */
    public void setOnSizeChangedListener(OnSizeChangedListener listener) {
        mListener = listener;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (mListener != null) {
            mListener.onSizeChanged(this, w, h, oldw, oldh);
        }
    }

    /**
     * A listener for receiving notification of view size changes.
     */
    public interface OnSizeChangedListener {

        /**
         * Callback performed when the size of the image view changes.
         *
         * @param view the view that changed
         * @param w    the new view width
         * @param h    the new view height
         * @param oldw the old view width
         * @param oldh the old view height
         */
        void onSizeChanged(SizeListenerImageView view, int w, int h, int oldw, int oldh);

    }

}
