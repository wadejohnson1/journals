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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;

import com.example.journals.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;

/**
 * A fragment that manages custom views around a {@link RecyclerView}.  When no adapter is set on
 * the recycler view, an indeterminant progress indicator is shown.  When the recycler view's
 * adapter is empty, a message is displayed instead of the recycler view.
 * <p>
 * If you would like the recycler view to have swipe to refresh capability, add the {@link
 * #ARG_HAS_SWIPE} argument passed into the fragment with a value of {@code true}.
 * <p>
 * You <b>must</b> use {@link #setAdapter(Adapter)} to associate the recycler view with an adapter.
 * Do not directly call {@link RecyclerView#setAdapter(Adapter)} or else important initialization
 * will be skipped.
 */
public abstract class RecyclerViewFragment extends AuthHandlerFragment {

    /**
     * The layout ID for a recycler view without swipe to refresh.
     */
    public static final int LAYOUT_ID_NO_SWIPE = R.layout.recycler_fragment;
    /**
     * The layout ID for a recycler view with swipe to refresh.
     */
    public static final int LAYOUT_ID_WITH_SWIPE = R.layout.recycler_fragment_with_swipe;
    /**
     * Argument designating whether the recycler view has swipe to refresh capability.
     * <p>
     * Type: boolean
     */
    public static final String ARG_HAS_SWIPE = "ARG_HAS_SWIPE";

    /**
     * Observer for listening to adapter data changes.
     */
    private final AdapterDataObserver mObserver = new AdapterDataObserver();
    /**
     * Adapter to bind views to the recycler view.
     */
    private Adapter mAdapter = null;
    /**
     * Set whether the recycler view is being shown.
     */
    private boolean mIsListShown = true;
    /**
     * The recycler view managed by this fragment.
     */
    private RecyclerView mRecyclerView;
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
     * Get the adapter associated with this fragment's recycler view.
     *
     * @return the adapter associated with this fragment's recycler view, or {@code null} if there
     * is no adapter set
     * @see #setAdapter(Adapter)
     */
    @Nullable
    public Adapter getAdapter() {
        return mAdapter;
    }

    /**
     * Get the recycler view associated with this fragment.
     *
     * @return the recycler view associated with this fragment; will be {@code null} prior to {@link
     * #onCreateView(LayoutInflater, ViewGroup, Bundle)}
     */
    @NonNull
    public RecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    @Override
    public final View onCreateView(LayoutInflater inflater, ViewGroup container,
                                   Bundle savedInstanceState) {
        final boolean hasSwipe = getArguments().getBoolean(ARG_HAS_SWIPE, true); // TODO make false
        final View view =
                inflater.inflate(hasSwipe ? LAYOUT_ID_WITH_SWIPE : LAYOUT_ID_NO_SWIPE, container,
                        false);
        // Get the commonly used views.
        mRecyclerView = (RecyclerView) view.findViewById(android.R.id.list);
        mEmptyView = view.findViewById(R.id.recycler_emptyView);
        mListContainer = view.findViewById(R.id.recycler_listContainer);
        mNoAdapterView = view.findViewById(R.id.recycler_noAdapterView);
        // Set the initial state of the views.
        mRecyclerView.setHasFixedSize(true);
        setAdapterInternal();
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove destroyed views.
        mRecyclerView = null;
        mEmptyView = null;
        mListContainer = null;
        mNoAdapterView = null;
        // Unregister adapter listener.
        if (mAdapter != null) {
            mAdapter.unregisterAdapterDataObserver(mObserver);
        }
    }

    /**
     * Set the adapter for the recycler view.
     * <p>
     * This method does nothing if the new adapter is the same instance as the old adapter.
     *
     * @param adapter the adapter for the recycler view, or {@code null} to remove the adapter
     */
    public void setAdapter(@Nullable Adapter adapter) {
        if (mAdapter != adapter) {
            if (mRecyclerView != null) {
                if (mAdapter != null) {
                    mAdapter.unregisterAdapterDataObserver(mObserver);
                }
                mAdapter = adapter;
                setAdapterInternal();
            } else {
                mAdapter = adapter;
            }
        }
    }

    /**
     * Set the adapter on the recycler view.
     */
    private void setAdapterInternal() {
        mRecyclerView.setAdapter(mAdapter);
        if (mAdapter != null) {
            mAdapter.registerAdapterDataObserver(mObserver);
        }
        setListShown((mAdapter != null), (mRecyclerView.getWindowToken() != null));
        showListOrEmptyView();
    }

    /**
     * Set whether the recycler view is being displayed. You can make it not displayed if you are
     * waiting for the initial data to show in it. During this time an indeterminant progress
     * indicator will be shown instead.
     * <p>
     * The default behavior is to start with the list not being shown, only showing it once an
     * adapter has been set on the recycler fragment. If the recycler view at that point had not
     * been shown, when it does get shown it will be done without the user ever seeing the hidden
     * state.
     *
     * @param isShown    {@code true} to show the recycler view/empty view, {@code false} to show
     *                   the progress indicator
     * @param isAnimated {@code true} to show an animation to transition to the new state, {@code
     *                   false} to show no animation
     */
    private void setListShown(boolean isShown, boolean isAnimated) {
        if (mIsListShown != isShown) {
            mIsListShown = isShown;
            if (isShown) {
                if (isAnimated) {
                    mNoAdapterView.startAnimation(AnimationUtils.loadAnimation(getContext(),
                            android.R.anim.fade_out));
                    mListContainer.startAnimation(AnimationUtils.loadAnimation(getContext(),
                            android.R.anim.fade_in));
                } else {
                    mNoAdapterView.clearAnimation();
                    mListContainer.clearAnimation();
                }
                mNoAdapterView.setVisibility(View.GONE);
                mListContainer.setVisibility(View.VISIBLE);
            } else {
                if (isAnimated) {
                    mNoAdapterView.startAnimation(AnimationUtils.loadAnimation(getContext(),
                            android.R.anim.fade_in));
                    mListContainer.startAnimation(AnimationUtils.loadAnimation(getContext(),
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
    private void showListOrEmptyView() {
        if (mAdapter != null) {
            final int count = mAdapter.getItemCount();
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

    /**
     * Class for listening to changes in the adapter data.
     */
    private class AdapterDataObserver extends RecyclerView.AdapterDataObserver {

        @Override
        public void onChanged() {
            showListOrEmptyView();
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            showListOrEmptyView();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            showListOrEmptyView();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            showListOrEmptyView();
        }

    }

}
