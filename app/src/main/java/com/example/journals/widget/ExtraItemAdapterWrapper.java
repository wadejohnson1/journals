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

import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

/**
 * A wrapper class for an {@link Adapter} that manages an optional extra item in a dataset that is
 * not part of the original adapter.  Method calls that affect the original dataset are delegated to
 * the wrapped adapter object.
 * <p/>
 * The view type of the extra position set in {@link #getExtraItemViewType()} must be unique from
 * the underlying wrapped adapter's view types or undefined operation may occur.
 * <p/>
 * Any observers set on the wrapped adapter directly will not be known to this adapter and must be
 * handled internally.
 */
public abstract class ExtraItemAdapterWrapper<VH extends ViewHolder> extends Adapter<VH> {

    /**
     * The base adapter wrapped by this adapter.
     */
    private final Adapter<VH> mWrappedAdapter;
    /**
     * Set whether the adapter has an extra item.
     */
    private boolean mHasExtraItem = false;
    /**
     * Reference to the view holder representing the extra item.
     */
    private Reference<ViewHolder> mReference;
    /**
     * Observer for listening to changes to the wrapped adapter.
     */
    private WrappedAdapterObserver mObserver = null;

    /**
     * Create a new adapter wrapper.
     *
     * @param wrappedAdapter the base adapter to be wrapped by this adapter
     */
    public ExtraItemAdapterWrapper(@NonNull Adapter<VH> wrappedAdapter) {
        mWrappedAdapter = wrappedAdapter;
        // Match wrapped adapter state as best as possible for methods that cannot be overridden.
        super.setHasStableIds(mWrappedAdapter.hasStableIds());
    }

    /**
     * Get the base adapter wrapped by this adapter.
     *
     * @return the base adapter wrapped by this adapter
     */
    public Adapter<VH> getWrappedAdapter() {
        return mWrappedAdapter;
    }

    /**
     * Get the position of the extra item in the adapter.
     * <p/>
     * The result returned by this method when {@link #hasExtraItem()} returns {@code false} is
     * undefined in the base implementation.  Subclasses may choose to define this behavior if
     * desired.
     * <p/>
     * If the position of the extra item changes at some point while it is in the adapter, observers
     * should be notified with a call to {@link #notifyItemMoved(int, int)} or {@link
     * #notifyDataSetChanged()}.
     *
     * @return the position of the extra item in the adapter
     */
    public abstract int getExtraItemPosition();

    /**
     * Get whether the adapter has an extra item.
     *
     * @return {@code true} if the adapter has an extra item, {@code false} otherwise
     */
    public boolean hasExtraItem() {
        return mHasExtraItem;
    }

    /**
     * Set whether the adapter has an extra item.
     * <p/>
     * This method calls {@link #getExtraItemPosition()} to get the position of the extra item in
     * order to notify observers of item insertion or removal.  {@code getExtraItemPosition()} will
     * only be called while {@link #hasExtraItem()} returns {@code true}.
     *
     * @param hasExtraItem {@code true} if the adapter has an extra item, {@code false} otherwise
     */
    public void setHasExtraItem(boolean hasExtraItem) {
        if (hasExtraItem() != hasExtraItem) {
            if (hasExtraItem) {
                mHasExtraItem = true;
                notifyItemInserted(getExtraItemPosition());
            } else {
                final int oldPosition = getExtraItemPosition();
                mHasExtraItem = false;
                mReference = null;
                notifyItemRemoved(oldPosition);
            }
        }
    }

    /**
     * Called when RecyclerView needs a new view holder to represent the extra item.
     * <p/>
     * This new ViewHolder should be constructed with a new View that can represent the extra item.
     * You can either create a new view manually or inflate it from an XML layout file.
     * <p/>
     * The new ViewHolder will be used to display the extra item using {@link
     * #onBindExtraItemViewHolder(ViewHolder, int, List)}. Since it will be re-used to display
     * different items in the data set, it is a good idea to cache references to sub views of the
     * View to avoid unnecessary {@link View#findViewById(int)} calls.
     *
     * @param parent   the view group into which the new view will be added after it is bound to the
     *                 extra item
     * @param viewType the view type of the new view
     * @return a new view holder that holds a view of the extra item view type
     */
    public abstract VH onCreateExtraItemViewHolder(ViewGroup parent, int viewType);

    @NonNull
    @Override
    public final VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == getExtraItemViewType()) {
            return onCreateExtraItemViewHolder(parent, viewType);
        } else {
            return mWrappedAdapter.onCreateViewHolder(parent, viewType);
        }
    }

    /**
     * Called by RecyclerView to display the extra item data. This method should update the contents
     * of the {@link ViewHolder#itemView} to reflect the extra item.
     *
     * @param holder   the view holder containing the extra item view
     * @param position the position of the extra item in the adapter
     */
    public abstract void onBindExtraItemViewHolder(VH holder, int position);

    @Override
    public final void onBindViewHolder(@NonNull VH holder, int position) {
        if (hasExtraItem() && (position == getExtraItemPosition())) {
            mReference = new WeakReference<ViewHolder>(holder);
            onBindExtraItemViewHolder(holder, position);
        } else {
            mWrappedAdapter.onBindViewHolder(holder, position);
        }
    }

    /**
     * Called by RecyclerView to display the extra item data. This method should update the contents
     * of the {@link ViewHolder#itemView} to reflect the extra item.
     *
     * @param holder   the view holder containing the extra item view
     * @param position the position of the extra item in the adapter
     * @param payloads a non-null list of merged payloads; can be empty list if requires full
     *                 update
     */
    public void onBindExtraItemViewHolder(VH holder, int position, List<Object> payloads) {
        onBindExtraItemViewHolder(holder, position);
    }

    @Override
    public final void onBindViewHolder(@NonNull VH holder, int position,
                                       @NonNull List<Object> payloads) {
        if (hasExtraItem() && (position == getExtraItemPosition())) {
            mReference = new WeakReference<ViewHolder>(holder);
            onBindExtraItemViewHolder(holder, position, payloads);
        } else {
            mWrappedAdapter.onBindViewHolder(holder, position, payloads);
        }
    }

    /**
     * Get the view type of the extra item.
     * <p/>
     * The view type of the extra item must be unique from the underlying wrapped adapter's view
     * types or undefined operation may occur.
     *
     * @return the view type of the extra item
     */
    public abstract int getExtraItemViewType();

    @Override
    public final int getItemViewType(int position) {
        if (hasExtraItem() && (position == getExtraItemPosition())) {
            return getExtraItemViewType();
        } else {
            return mWrappedAdapter.getItemViewType(position);
        }
    }

    @Override
    public void setHasStableIds(boolean hasStableIds) {
        mWrappedAdapter.setHasStableIds(hasStableIds);
        // Match wrapped adapter state so hasStableIds() returns the same value.
        super.setHasStableIds(mWrappedAdapter.hasStableIds());
    }

    /**
     * Get the stable ID of the extra item.
     * <p/>
     * Subclasses should override if the adapter has stable IDs and they might conflict with the
     * default extra item ID.
     */
    public long getExtraItemId() {
        return RecyclerView.NO_ID;
    }

    @Override
    public final long getItemId(int position) {
        if (hasExtraItem() && (position == getExtraItemPosition())) {
            return getExtraItemId();
        } else {
            return mWrappedAdapter.getItemId(position);
        }
    }

    @Override
    public final int getItemCount() {
        final int count = mWrappedAdapter.getItemCount();
        return (hasExtraItem()) ? count + 1 : count;
    }

    /**
     * Called when the extra item's view created by this adapter has been recycled.
     * <p/>
     * A view is recycled when a {@link RecyclerView.LayoutManager} decides that it no longer needs
     * to be attached to its parent {@link RecyclerView}. This can be because it has fallen out of
     * visibility or a set of cached views represented by views still attached to the parent
     * RecyclerView. If an item view has large or expensive data bound to it such as large bitmaps,
     * this may be a good place to release those resources.
     * <p/>
     * RecyclerView calls this method right before clearing ViewHolder's internal data and sending
     * it to RecycledViewPool. This way, if ViewHolder was holding valid information before being
     * recycled, you can call {@link ViewHolder#getAdapterPosition()} to get its adapter position.
     *
     * @param holder The ViewHolder for the extra item view being recycled
     */
    public void onExtraItemViewRecycled(VH holder) {
        super.onViewRecycled(holder);
    }

    @Override
    public final void onViewRecycled(@NonNull VH holder) {
        final ViewHolder extraItem = (mReference == null) ? null : mReference.get();
        if (extraItem == holder) {
            onExtraItemViewRecycled(holder);
        } else {
            mWrappedAdapter.onViewRecycled(holder);
        }
    }

    /**
     * Called by the RecyclerView if a ViewHolder created by this Adapter cannot be recycled due to
     * its transient state. Upon receiving this callback, Adapter can clear the animation(s) that
     * effect the View's transient state and return <code>true</code> so that the View can be
     * recycled. Keep in mind that the View in question is already removed from the RecyclerView.
     * <p/>
     * In some cases, it is acceptable to recycle a View although it has transient state. Most of
     * the time, this is a case where the transient state will be cleared in {@link
     * #onBindViewHolder(ViewHolder, int)} call when View is rebound to a new position. For this
     * reason, RecyclerView leaves the decision to the Adapter and uses the return value of this
     * method to decide whether the View should be recycled or not.
     * <p/>
     * Note that when all animations are created by {@link RecyclerView.ItemAnimator}, you should
     * never receive this callback because RecyclerView keeps those Views as children until their
     * animations are complete. This callback is useful when children of the item views create
     * animations which may not be easy to implement using an {@link RecyclerView.ItemAnimator}.
     * <p/>
     * You should <em>never</em> fix this issue by calling <code>holder.itemView.setHasTransientState(false);</code>
     * unless you've previously called <code>holder.itemView.setHasTransientState(true);</code>.
     * Each <code>View.setHasTransientState(true)</code> call must be matched by a
     * <code>View.setHasTransientState(false)</code> call, otherwise, the state of the View may
     * become inconsistent. You should always prefer to end or cancel animations that are triggering
     * the transient state instead of handling it manually.
     *
     * @param holder The ViewHolder containing the View that could not be recycled due to its
     *               transient state.
     * @return True if the View should be recycled, false otherwise. Note that if this method
     * returns <code>true</code>, RecyclerView <em>will ignore</em> the transient state of the View
     * and recycle it regardless. If this method returns <code>false</code>, RecyclerView will check
     * the View's transient state again before giving a final decision. Default implementation
     * returns false.
     */
    public boolean onFailedToRecycleExtraItemView(VH holder) {
        return super.onFailedToRecycleView(holder);
    }

    @Override
    public final boolean onFailedToRecycleView(@NonNull VH holder) {
        final ViewHolder extraItem = (mReference == null) ? null : mReference.get();
        if (extraItem == holder) {
            return onFailedToRecycleExtraItemView(holder);
        } else {
            return mWrappedAdapter.onFailedToRecycleView(holder);
        }
    }

    /**
     * Called when the extra item view created by this adapter has been attached to a window. <p>
     * This can be used as a reasonable signal that the view is about to be seen by the user. If the
     * adapter previously freed any resources in {@link #onViewDetachedFromWindow(ViewHolder)
     * onViewDetachedFromWindow} those resources should be restored here.</p>
     *
     * @param holder Holder of the view being attached
     */
    public void onExtraItemViewAttachedToWindow(VH holder) {
        super.onViewAttachedToWindow(holder);
    }

    @Override
    public final void onViewAttachedToWindow(@NonNull VH holder) {
        final ViewHolder extraItem = (mReference == null) ? null : mReference.get();
        if (extraItem == holder) {
            onExtraItemViewAttachedToWindow(holder);
        } else {
            mWrappedAdapter.onViewAttachedToWindow(holder);
        }
    }

    /**
     * Called when the extra item view created by this adapter has been detached from its window.
     * <p/>
     * Becoming detached from the window is not necessarily a permanent condition; the consumer of
     * an Adapter's views may choose to cache views offscreen while they are not visible, attaching
     * an detaching them as appropriate.
     *
     * @param holder Holder of the view being detached
     */
    public void onExtraItemViewDetachedFromWindow(VH holder) {
        super.onViewDetachedFromWindow(holder);
    }

    @Override
    public final void onViewDetachedFromWindow(@NonNull VH holder) {
        final ViewHolder extraItem = (mReference == null) ? null : mReference.get();
        if (extraItem == holder) {
            onExtraItemViewDetachedFromWindow(holder);
        } else {
            mWrappedAdapter.onViewDetachedFromWindow(holder);
        }
    }

    @Override
    public void registerAdapterDataObserver(@NonNull AdapterDataObserver observer) {
        final boolean hadObservers = hasObservers();
        super.registerAdapterDataObserver(observer);
        // Register observer for changes reported by the wrapped adapter.
        if (!hadObservers && hasObservers()) {
            mObserver = new WrappedAdapterObserver();
            mWrappedAdapter.registerAdapterDataObserver(mObserver);
        }
    }

    @Override
    public void unregisterAdapterDataObserver(@NonNull AdapterDataObserver observer) {
        super.unregisterAdapterDataObserver(observer);
        if (!hasObservers()) {
            mWrappedAdapter.unregisterAdapterDataObserver(mObserver);
            mObserver = null;
        }
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        mWrappedAdapter.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        mWrappedAdapter.onDetachedFromRecyclerView(recyclerView);
    }

    /**
     * Observer for forwarding changes reported by the wrapped adapter to this adapter's observers.
     */
    private class WrappedAdapterObserver extends AdapterDataObserver {

        @Override
        public void onChanged() {
            notifyDataSetChanged();
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            notifyItemRangeChanged(positionStart, itemCount);
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
            notifyItemRangeChanged(positionStart, itemCount, payload);
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            notifyItemRangeInserted(positionStart, itemCount);
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            notifyItemRangeRemoved(positionStart, itemCount);
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            notifyItemMoved(fromPosition, toPosition);
        }

    }

}
