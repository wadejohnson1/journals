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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

/**
 * Adapter that exposes data from a {@link List} to a {@link RecyclerView} widget.
 */
public abstract class ListAdapter<T, VH extends ViewHolder> extends Adapter<VH> {

    /**
     * The list managed by this adapter.
     */
    private List<T> mList;

    /**
     * Create a new list adapter.
     *
     * @param list the list to be managed by this adapter, may be {@code null} if the data is not
     *             yet available
     */
    public ListAdapter(@Nullable List<T> list) {
        mList = list;
    }

    /**
     * Get the list associated with this adapter.
     *
     * @return the list associated with this adapter, or {@code null} if no list is set
     */
    @Nullable
    public List<T> getList() {
        return mList;
    }

    @Override
    public int getItemCount() {
        return (mList != null) ? mList.size() : 0;
    }

    @Override
    public final void onBindViewHolder(@NonNull VH holder, int position) {
        onBindViewHolder(holder, mList.get(position));
    }

    /**
     * Called by RecyclerView to display the data at the specified position. This method should
     * update the contents of the RecyclerView.ViewHolder.itemView to reflect the item at the given
     * position.
     *
     * @param holder the ViewHolder which should be updated to represent the contents of the item at
     *               the given position in the data set
     * @param data   the data item in the list
     */
    public abstract void onBindViewHolder(VH holder, T data);

    @Override
    public final void onBindViewHolder(@NonNull VH holder, int position,
                                       @NonNull List<Object> payloads) {
        onBindViewHolder(holder, mList.get(position), payloads);
    }

    /**
     * Called by RecyclerView to display the data at the specified position. This method should
     * update the contents of the {@link ViewHolder#itemView} to reflect the item at the given
     * position.
     * <p>
     * Note that unlike {@link android.widget.ListView}, RecyclerView will not call this method
     * again if the position of the item changes in the data set unless the item itself is
     * invalidated or the new position cannot be determined. For this reason, you should only use
     * the position of the list while acquiring the related data item inside this method and should
     * not keep a copy of it. If you need the position of an item later on (e.g. in a click
     * listener), use {@link ViewHolder#getAdapterPosition()} which will have the updated adapter
     * position.
     * <p>
     * Partial bind vs full bind:
     * <p>
     * The payloads parameter is a merge list from {@link #notifyItemChanged(int, Object)} or {@link
     * #notifyItemRangeChanged(int, int, Object)}.  If the payloads list is not empty, the
     * ViewHolder is currently bound to old data and Adapter may run an efficient partial update
     * using the payload info.  If the payload is empty,  Adapter must run a full bind. Adapter
     * should not assume that the payload passed in notify methods will be received by
     * onBindViewHolder().  For example when the view is not attached to the screen, the payload in
     * notifyItemChange() will be simply dropped.
     *
     * @param holder   The ViewHolder which should be updated to represent the contents of the item
     *                 at the given position in the data set.
     * @param data     the data item from the list
     * @param payloads A non-null list of merged payloads. Can be empty list if requires full
     *                 update.
     */
    public void onBindViewHolder(VH holder, T data, List<Object> payloads) {
        onBindViewHolder(holder, data);
    }

    /**
     * Swap in a new list, returning the old list.
     *
     * @param newList the new list to be used
     * @return the previously set list, or {@code null} if there was not one set previously or if
     * the new list is the same instance as the old list
     */
    @Nullable
    public List<T> swapList(@Nullable List<T> newList) {
        if (newList == mList) {
            return null;
        } else {
            final List<T> oldList = mList;
            mList = newList;
            notifyDataSetChanged();
            return oldList;
        }
    }

}
