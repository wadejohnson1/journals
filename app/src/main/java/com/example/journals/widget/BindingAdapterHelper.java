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

import java.util.List;

import androidx.recyclerview.widget.RecyclerView.ViewHolder;

/**
 * Cursor adapter that automatically passes view holder binding requests to the view holder itself.
 * <p/>
 * In the default implementation, view holders used with this adapter must implement the {@link
 * BindingViewHolder} interface or an exception will be thrown when the view holder is being bound.
 */
public class BindingAdapterHelper {

    /**
     * TODO
     *
     * @param holder
     * @param data
     */
    public void onBindViewHolder(ViewHolder holder, Object data) {
        if (BindingViewHolder.class.isInstance(holder)) {
            ((BindingViewHolder) holder).onBind(data);
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * TODO
     *
     * @param holder
     * @param data
     * @param payloads
     */
    public void onBindViewHolder(ViewHolder holder, Object data, List<Object> payloads) {
        if (BindingViewHolder.class.isInstance(holder)) {
            ((BindingViewHolder) holder).onBind(data, payloads);
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * Base interface for a view holder that uses a cursor to bind to its {@link
     * ViewHolder#itemView}.
     */
    public interface BindingViewHolder<T> {

        /**
         * Perform binding of this view holder's view using supplied data.
         *
         * @param data data to bind view holder to
         */
        void onBind(T data);

        /**
         * Perform binding of this view holder's view using supplied data.
         *
         * @param data     data to bind view holder to
         * @param payloads a non-null list of merged payloads; can be empty list if requires full
         *                 update
         */
        void onBind(T data, List<Object> payloads);

    }

}
