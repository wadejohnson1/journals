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

import android.database.Cursor;
import android.provider.BaseColumns;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

/**
 * Adapter that exposes data from a {@link Cursor} to a {@link RecyclerView} widget.
 * <p/>
 * The cursor must include a column with the same name as {@link BaseColumns#_ID} or this class will
 * not work. Additionally, using {@link android.database.MergeCursor} with this class will not work
 * if the merged cursors have overlapping values in their ID columns.
 */
public abstract class CursorAdapter<VH extends ViewHolder> extends Adapter<VH> {

    /**
     * Name of column containing unique ID for each row in the cursor.
     */
    private static final String COLUMN_ID = BaseColumns._ID;

    /**
     * The cursor managed by this adapter.
     */
    private Cursor mCursor;
    /**
     * The cursor column containing the row ID.
     */
    private int mRowIDColumn;

    /**
     * Create a new cursor adapter.
     *
     * @param cursor the cursor to be managed by this adapter, may be {@code null} if the cursor is
     *               not yet available
     */
    public CursorAdapter(@Nullable Cursor cursor) {
        mCursor = cursor;
        mRowIDColumn = (cursor != null) ? cursor.getColumnIndexOrThrow(COLUMN_ID) : -1;
        super.setHasStableIds(false);
    }

    /**
     * Get the cursor associated with this adapter.
     *
     * @return the cursor associated with this adapter, or {@code null} if no cursor is set
     */
    @Nullable
    public Cursor getCursor() {
        return mCursor;
    }

    @Override
    public int getItemCount() {
        return (mCursor != null) ? mCursor.getCount() : 0;
    }

    @Override
    public long getItemId(int position) {
        moveCursorToPosition(position);
        return mCursor.getLong(mRowIDColumn);
    }

    @Override
    public final void onBindViewHolder(@NonNull VH holder, int position) {
        moveCursorToPosition(position);
        onBindViewHolder(holder, mCursor);
    }

    /**
     * Called by RecyclerView to display the data at the specified position. This method should
     * update the contents of the RecyclerView.ViewHolder.itemView to reflect the item at the given
     * position.
     *
     * @param holder the ViewHolder which should be updated to represent the contents of the item at
     *               the given position in the data set
     * @param cursor the cursor from which to get the data; the cursor is already moved to the
     *               correct position
     */
    public abstract void onBindViewHolder(VH holder, Cursor cursor);

    @Override
    public final void onBindViewHolder(@NonNull VH holder, int position,
                                       @NonNull List<Object> payloads) {
        moveCursorToPosition(position);
        onBindViewHolder(holder, mCursor, payloads);
    }

    /**
     * Called by RecyclerView to display the data at the specified position. This method should
     * update the contents of the {@link ViewHolder#itemView} to reflect the item at the given
     * position.
     * <p/>
     * Note that unlike {@link android.widget.ListView}, RecyclerView will not call this method
     * again if the position of the item changes in the data set unless the item itself is
     * invalidated or the new position cannot be determined. For this reason, you should only use
     * the position of the cursor while acquiring the related data item inside this method and
     * should not keep a copy of it. If you need the position of an item later on (e.g. in a click
     * listener), use {@link ViewHolder#getAdapterPosition()} which will have the updated adapter
     * position.
     * <p/>
     * Partial bind vs full bind:
     * <p/>
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
     * @param cursor   the cursor from which to get the data; the cursor is already moved to the
     *                 correct position
     * @param payloads A non-null list of merged payloads. Can be empty list if requires full
     *                 update.
     */
    public void onBindViewHolder(VH holder, Cursor cursor, List<Object> payloads) {
        onBindViewHolder(holder, cursor);
    }

    /**
     * <i>Calling this method has no effect, this adapter always has stable ID's.</i>
     * <p/>
     * Indicates whether each item in the data set can be represented with a unique identifier of
     * type {@link java.lang.Long}.
     *
     * @param hasStableIds this parameter is ignored
     */
    @Override
    public final void setHasStableIds(boolean hasStableIds) {
    }

    /**
     * Swap in a new cursor, returning the old cursor. The returned old Cursor is <em>not</em>
     * closed.
     *
     * @param newCursor the new cursor to be used
     * @return the previously set cursor, or {@code null} if there was not one set previously or if
     * the new cursor is the same instance as the old cursor
     */
    @Nullable
    public Cursor swapCursor(@Nullable Cursor newCursor) {
        if (newCursor == mCursor) {
            return null;
        }
        final Cursor oldCursor = mCursor;
        mCursor = newCursor;
        mRowIDColumn = (newCursor != null) ? newCursor.getColumnIndexOrThrow(COLUMN_ID) : -1;
        notifyDataSetChanged();
        return oldCursor;
    }

    /**
     * Move the cursor to the specified position.
     *
     * @param position to position to more the cursor to
     * @throws IllegalStateException if the cursor is {@code null} or the cursor could not be moved
     *                               to the specified position
     */
    private void moveCursorToPosition(int position) throws IllegalStateException {
        if (mCursor == null) {
            throw new IllegalStateException("Cursor is not valid.");
        }
        if (!mCursor.moveToPosition(position)) {
            throw new IllegalStateException("Couldn't move cursor to position " + position);
        }
    }

}
