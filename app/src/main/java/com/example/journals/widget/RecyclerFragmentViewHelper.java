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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;

import com.example.journals.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver;

/**
 * A {@link AdapterDataObserver} that manages a custom view.  When no adapter is set, an
 * indeterminant progress indicator is shown.  When the fragment's adapter is empty, a message is
 * displayed instead of the recycler view.
 * <p/>
 * To use this helper, call {@link #onCreateView(LayoutInflater, ViewGroup)} from your {@code
 * RecyclerFragment}'s {@link RecyclerFragment#onCreateView(LayoutInflater, ViewGroup, Bundle)}
 * method and return the supplied view.  Additionally, call {@link #setAdapter(Adapter)} any time
 * the recycler fragment adapter changes.
 */
public class RecyclerFragmentViewHelper extends AdapterDataObserver {

    /**
     * The layout ID for a recycler view without swipe to refresh.
     */
    public static final int LAYOUT_ID_NO_SWIPE = R.layout.recycler_fragment;
    /**
     * The layout ID for a recycler view with swipe to refresh.
     */
    public static final int LAYOUT_ID_WITH_SWIPE = R.layout.recycler_fragment_with_swipe;

    /**
     * Set whether the recycler view has swipe to refresh.
     */
    private final boolean mHasSwipe;
    /**
     * Context used to access resources.
     */
    private final Context mContext;
    /**
     * Recycler fragment associated with this helper.
     */
    private final RecyclerFragment mFragment;
    /**
     * Adapter associated with this helper.
     */
    private Adapter mAdapter;
    /**
     * Set whether the recycler view is being shown.
     */
    private boolean mIsListShown = true;
    /**
     * View that is displayed when an adapter has no items.
     */
    private View mEmptyView;
    /**
     * Container holding the recycler view.
     */
    private View mListContainer;
    /**
     * View that is displayed when no adapter is set on the recycler view.
     */
    private View mNoAdapterView;
    /**
     * The recycler view managed by this helper.
     */
    private View mRecyclerView;

    /**
     * Create a new view helper.
     *
     * @param context the context used to access resources
     */
    public RecyclerFragmentViewHelper(Context context, RecyclerFragment fragment,
                                      boolean hasSwipe) {
        mContext = context;
        mFragment = fragment;
        mHasSwipe = hasSwipe;
    }

    /**
     * Initialize the view managed by this helper.
     * <p/>
     * Call this method from within {@link RecyclerFragment#onCreateView(LayoutInflater, ViewGroup,
     * Bundle)}.
     *
     * @param inflater  inflater used to inflate the view associated with this helper
     * @param container parent view that the helper view will be attached to
     * @return the view for the fragment UI associated with this helper
     */
    public View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
        final View view =
                inflater.inflate(mHasSwipe ? LAYOUT_ID_WITH_SWIPE : LAYOUT_ID_NO_SWIPE, container,
                        false);
        // Get the commonly used views.
        mEmptyView = view.findViewById(R.id.recycler_emptyView);
        mListContainer = view.findViewById(R.id.recycler_listContainer);
        mNoAdapterView = view.findViewById(R.id.recycler_noAdapterView);
        mRecyclerView = view.findViewById(android.R.id.list);
        // Set the initial state of the views.
        ((RecyclerView) mRecyclerView).setHasFixedSize(true);
        final Adapter adapter = mFragment.getAdapter();
        setListShown((adapter != null), false);
        showListOrEmptyView(adapter);
        return view;
    }

    /**
     * Set the adapter associated with this helper.
     *
     * @param adapter the adapter to associate with this helper, or {@code null} to remove any
     *                existing adapter
     */
    public void setAdapter(@Nullable Adapter adapter) {
        if (mAdapter != null) {
            mAdapter.unregisterAdapterDataObserver(this);
        }
        // Show or hide the recycler view if switching from null to non-null adapter or vice versa.
        if ((mAdapter != null) == (adapter == null)) {
            final View view = mFragment.getView();
            setListShown((adapter != null), (view != null) && (view.getWindowToken() != null));
        }
        mAdapter = adapter;
        if (mAdapter != null) {
            mAdapter.registerAdapterDataObserver(this);
        }
        showListOrEmptyView(mAdapter);
    }

    @Override
    public void onChanged() {
        showListOrEmptyView(mAdapter);
    }

    @Override
    public void onItemRangeChanged(int positionStart, int itemCount) {
        showListOrEmptyView(mAdapter);
    }

    @Override
    public void onItemRangeInserted(int positionStart, int itemCount) {
        showListOrEmptyView(mAdapter);
    }

    @Override
    public void onItemRangeRemoved(int positionStart, int itemCount) {
        showListOrEmptyView(mAdapter);
    }

    /**
     * Set whether the recycler view is being displayed. You can make it not displayed if you are
     * waiting for the initial data to show in it. During this time an indeterminant progress
     * indicator will be shown instead.
     * <p/>
     * The default behavior is to start with the list not being shown, only showing it once an
     * adapter has been set on the recycler fragment. If the recycler view at that point had not
     * been shown, when it does get shown it will be done without the user ever seeing the hidden
     * state.
     *
     * @param shown   {@code true} to show the recycler view/empty view, {@code false} to show the
     *                progress indicator
     * @param animate {@code true} to show an animation to transition to the new state, {@code
     *                false} to show no animation
     */
    private void setListShown(boolean shown, boolean animate) {
        if (mIsListShown != shown) {
            mIsListShown = shown;
            if (shown) {
                if (animate) {
                    mNoAdapterView.startAnimation(AnimationUtils.loadAnimation(mContext,
                            android.R.anim.fade_out));
                    mListContainer.startAnimation(AnimationUtils.loadAnimation(mContext,
                            android.R.anim.fade_in));
                } else {
                    mNoAdapterView.clearAnimation();
                    mListContainer.clearAnimation();
                }
                mNoAdapterView.setVisibility(View.GONE);
                mListContainer.setVisibility(View.VISIBLE);
            } else {
                if (animate) {
                    mNoAdapterView.startAnimation(AnimationUtils.loadAnimation(mContext,
                            android.R.anim.fade_in));
                    mListContainer.startAnimation(AnimationUtils.loadAnimation(mContext,
                            android.R.anim.fade_out));
                } else {
                    mNoAdapterView.clearAnimation();
                    mListContainer.clearAnimation();
                }
                mNoAdapterView.setVisibility(View.VISIBLE);
                mListContainer.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Show the list or empty view depending on the number of items in the adapter.
     */
    private void showListOrEmptyView(Adapter adapter) {
        if (adapter != null) {
            final int count = adapter.getItemCount();
            boolean isRecyclerVisible = (mRecyclerView.getVisibility() == View.VISIBLE);
            if (isRecyclerVisible && (count == 0)) {
                mRecyclerView.setVisibility(View.GONE);
                mEmptyView.setVisibility(View.VISIBLE);
            } else if (!isRecyclerVisible && (count != 0)) {
                mRecyclerView.setVisibility(View.VISIBLE);
                mEmptyView.setVisibility(View.GONE);
            }
        }
    }

}
