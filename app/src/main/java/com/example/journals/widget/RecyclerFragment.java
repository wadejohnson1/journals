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
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

/**
 * A fragment that displays a list of items by binding to a data source such as an array or cursor,
 * and exposes event handlers when the user selects an item.
 * <p/>
 * {@code RecyclerFragment} hosts a {@link RecyclerView} object that can be bound to different data
 * sources using a {@link Adapter}. Binding, screen layout, and row layout are discussed in the
 * following sections.
 * <p/>
 * <strong>Screen Layout</strong>
 * <p/>
 * {@code RecyclerFragment} has a default layout that consists of a single recycler view. However,
 * if you desire, you can customize the fragment layout by returning your own view hierarchy from
 * {@link #onCreateView}. To do this, your view hierarchy <em>must</em> contain a {@code
 * RecyclerView} object with the id "@android:id/list" (or {@link android.R.id#list} if it's in
 * code).
 * <p/>
 * <strong>Binding to Data</strong>
 * <p/>
 * You bind the {@code RecyclerFragment}'s recycler view object to data using a class that extends
 * the {@link Adapter} class. The adapter must take a {@link RecyclerFragmentViewHolder} or a
 * subclass to use with this fragment.
 * <p/>
 * You <b>must</b> use {@link #setAdapter(Adapter)} to associate the recycler view with an adapter.
 * Do not directly call {@link RecyclerView#setAdapter(Adapter)} or else important initialization
 * will be skipped.
 * <p/>
 * <strong>Choice Modes and Action Mode</strong>
 * <p/>
 * Choice modes and action mode are handled directly by this fragment and differ slightly from
 * {@link android.widget.AbsListView}. The three choice modes {@link ChoiceMode#NONE}, {@link
 * ChoiceMode#SINGLE} and {@link ChoiceMode#MULTIPLE} function the same as {@code AbsListView},
 * however the action mode can be activated from any choice mode using a long press as long as a
 * {@link ActionModeListener} has been set.
 */
public abstract class RecyclerFragment extends Fragment {

    /**
     * Saved instance state key for storing the activated adapter ID's.  It is only valid if the
     * adapter has stable ID's.
     * <p/>
     * Type: long[]
     */
    private static final String SAVE_STATE_ACTIVATED_IDS =
            "com:blyspot:blyspot:SAVE_STATE_ACTIVATED_IDS";
    /**
     * Saved instance state key for storing the activated adapter positions.
     * <p/>
     * Type: int[]
     */
    private static final String SAVE_STATE_ACTIVATED_POSITIONS =
            "com:blyspot:blyspot:SAVE_STATE_ACTIVATED_POSITIONS";
    /**
     * Saved instance state key for storing the current choice mode.
     * <p/>
     * Type: Serializable
     */
    private static final String SAVE_STATE_CHOICE_MODE =
            "com:blyspot:blyspot:SAVE_STATE_CHOICE_MODE";
    /**
     * Saved instance state key for recording whether the action mode is active.
     * <p/>
     * Type: boolean
     */
    private static final String SAVE_STATE_IS_IN_ACTION_MODE =
            "com:blyspot:blyspot:SAVE_STATE_IS_IN_ACTION_MODE";
    /**
     * Saved instance state key for storing the clickable adapter positions.
     */
    private static final String SAVE_STATE_NONCLICKABLE_POSITIONS =
            "com:blyspot:blyspot:SAVE_STATE_NONCLICKABLE_POSITIONS";

    /**
     * Handler used for requesting focus on a selected item.
     */
    private final Handler mHandler = new Handler();
    /**
     * Observer for changes in the adapter's data.
     * <p/>
     * Used to update activated/clickable positions when adapter data changes.
     */
    private final RecyclerFragmentAdapterDataObserver mObserver =
            new RecyclerFragmentAdapterDataObserver();
    /**
     * The currently activated item ID's.
     * <p/>
     * The map key is the adapter position containing the ID.
     */
    private final SparseArray<Long> mActivatedPositions = new SparseArray<>();
    /**
     * Controls the modal state of the recycler view. Null when inactive.
     */
    private ActionMode mActionMode = null;
    /**
     * Adapter to bind views to the recycler view.
     */
    private Adapter<? extends RecyclerFragmentViewHolder> mAdapter = null;
    /**
     * Controls if/how the user may choose/check items in the list
     */
    private ChoiceMode mChoiceMode = ChoiceMode.NONE;
    /**
     * Wrapper for the action mode callback.
     * <p/>
     * This fragment needs to perform a few extra actions around what application code does.
     */
    private MultiChoiceModeWrapper mMultiChoiceModeListener = null;
    /**
     * The recycler view managed by this fragment.
     */
    private RecyclerView mRecyclerView;
    /**
     * Runnable used for requesting focus on a selected item.
     */
    private final Runnable mRequestFocus = new Runnable() {

        public void run() {
            mRecyclerView.focusableViewAvailable(mRecyclerView);
        }

    };
    /**
     * The current adapter positions that are not clickable.
     * <p/>
     * The default clickable state if no mapping exists is {@code true}.
     */
    private Set<Integer> mNonClickablePositions = new HashSet<>();

    /**
     * Get the ID's of the currently activated adapter positions.
     * <p/>
     * The result is only valid if the adapter has stable IDs.
     *
     * @return an array of the ID's of the activated adapter positions; the elements in the returned
     * array are not ordered
     */
    @NonNull
    public long[] getActivatedAdapterIds() {
        final long[] ids = new long[mActivatedPositions.size()];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = mActivatedPositions.valueAt(i);
        }
        return ids;
    }

    /**
     * Get the number of adapter positions currently activated.
     *
     * @return the number of adapter positions currently activated
     */
    public int getActivatedAdapterPositionCount() {
        return mActivatedPositions.size();
    }

    /**
     * Get the currently activated adapter positions.
     *
     * @return an array of the activated adapter positions; the elements in the returned array are
     * not ordered
     */
    @NonNull
    public int[] getActivatedAdapterPositions() {
        final int[] positions = new int[mActivatedPositions.size()];
        for (int i = 0; i < positions.length; i++) {
            positions[i] = mActivatedPositions.keyAt(i);
        }
        return positions;
    }

    /**
     * Get the adapter associated with this fragment's recycler view.
     *
     * @return the adapter associated with this fragment's recycler view, or {@code null} if there
     * is no adapter set
     * @see #setAdapter(Adapter)
     */
    @Nullable
    public Adapter<? extends RecyclerFragmentViewHolder> getAdapter() {
        return mAdapter;
    }

    /**
     * Get the current choice mode.
     *
     * @return the current choice mode
     * @see #setChoiceMode(ChoiceMode)
     */
    @NonNull
    public ChoiceMode getChoiceMode() {
        return mChoiceMode;
    }

    /**
     * Get the recycler view associated with this fragment.
     * <p/>
     * This method will throw an exception if the fragment's view has not been createdAt yet.
     *
     * @return the recycler view associated with this fragment
     */
    @NonNull
    public RecyclerView getRecyclerView() {
        ensureList();
        return mRecyclerView;
    }

    /**
     * Get whether an ID in the adapter is currently activated.
     *
     * @param id the ID in the adapter
     * @return {@code true} if {@code id} is activated, {@code false} otherwise
     */
    public boolean isActivatedAdapterId(long id) {
        final int size = mActivatedPositions.size();
        for (int i = 0; i < size; i++) {
            if (mActivatedPositions.valueAt(i) == id) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get whether a position in the adapter is currently activated.
     *
     * @param position the position in the adapter
     * @return {@code true} if {@code position} is activated, {@code false} otherwise
     */
    public boolean isActivatedAdapterPosition(int position) {
        // Check for invalid position.
        if ((mAdapter == null) || (position < 0) || (position >= mAdapter.getItemCount())) {
            throw new IllegalArgumentException();
        } else {
            return (mActivatedPositions.indexOfKey(position) >= 0);
        }
    }

    /**
     * Get the clickable state of the specified adapter position.
     *
     * @param position the position in the adapter to set the clickable state
     * @return {@code true} if the adapter position as clickable, {@code false} otherwise
     */
    public boolean isClickableAdapterPosition(int position) {
        // Check for invalid position.
        if ((mAdapter == null) || (position < 0) || (position >= mAdapter.getItemCount())) {
            throw new IllegalArgumentException();
        } else {
            return !mNonClickablePositions.contains(position);
        }
    }

    /**
     * Provide default implementation to return a simple recycler view. Subclasses can override to
     * replace with their own layout. If doing so, the returned view hierarchy <em>must</em> have a
     * {@code RecyclerView} whose ID is {@link android.R.id#list}.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in
     *                           the fragment
     * @param container          If non-null, this is the parent view that the fragment's UI should
     *                           be attached to.  The fragment should not add the view itself, but
     *                           this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous
     *                           saved state as given here.
     * @return the view for the fragment's UI, or {@code null}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final RecyclerView view = new RecyclerView(container.getContext());
        view.setId(android.R.id.list);
        view.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));
        return view;
    }

    @Override
    public void onDestroyView() {
        mHandler.removeCallbacks(mRequestFocus);
        mRecyclerView = null;
        super.onDestroyView();
    }

    /**
     * Called when an item in the list is clicked.
     *
     * @param recyclerView the recycler view where the click happened
     * @param holder       the view holder containing the view that was clicked
     */
    public abstract void onItemClick(@NonNull RecyclerView recyclerView,
                                     @NonNull RecyclerFragmentViewHolder holder);

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ensureList();
    }

    /**
     * Manually set the specified adapter position to activated.  How this affects the displayed
     * view depends on the current choice mode or whether an action mode is active.
     *
     * @param position the position in the adapter to activate
     */
    public void setActivatedAdapterPosition(int position) {
        // Check for invalid position.
        if ((mAdapter == null) || (position < 0) || (position >= mAdapter.getItemCount())) {
            throw new IllegalArgumentException();
        }
        // Determine current choice mode.
        // If action mode is active, choice mode acts as MULTIPLE.
        final ChoiceMode choiceMode = (mActionMode == null) ? mChoiceMode : ChoiceMode.MULTIPLE;
        // Change the selection status of the items in the layout.
        switch (choiceMode) {
            case NONE:
                // Do nothing.
                break;
            case SINGLE:
                boolean isFound = false;
                if (mActivatedPositions.size() > 0) {
                    // Deactivate previously activated item if different from current item.
                    final int oldPosition = mActivatedPositions.keyAt(0);
                    if (oldPosition != position) {
                        mActivatedPositions.delete(oldPosition);
                        mAdapter.notifyItemChanged(oldPosition);
                    } else {
                        isFound = true;
                    }
                }
                // Add the selected position if it is not already set.
                if (!isFound) {
                    mActivatedPositions.put(position, mAdapter.getItemId(position));
                    mAdapter.notifyItemChanged(position);
                }
                break;
            case MULTIPLE:
                // Add the item if it is not selected.
                if (mActivatedPositions.indexOfKey(position) < 0) {
                    mActivatedPositions.put(position, mAdapter.getItemId(position));
                    mAdapter.notifyItemChanged(position);
                    if (mActionMode != null) {
                        mMultiChoiceModeListener
                                .onAdapterPositionActivatedStateChanged(mActionMode, position,
                                        true);
                    }
                }
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     * Set the adapter for the recycler view.
     * <p/>
     * This method does nothing if the new adapter is the same instance as the old adapter.
     *
     * @param adapter the adapter for the recycler view, or {@code null} to remove the adapter
     */
    public void setAdapter(@Nullable Adapter<? extends RecyclerFragmentViewHolder> adapter) {
        if (mAdapter != adapter) {
            // Remove observer from old adapter.
            if (mAdapter != null) {
                mAdapter.unregisterAdapterDataObserver(mObserver);
            }
            // Clear the list of activated items.
            mActivatedPositions.clear();
            // Set the new adapter.
            mAdapter = adapter;
            // Register observer with new adapter.
            if (mAdapter != null) {
                mAdapter.registerAdapterDataObserver(mObserver);
            }
            // Set adapter on the recycler view.
            if (mRecyclerView != null) {
                mRecyclerView.setAdapter(adapter);
            }
        }
    }

    /**
     * Defines the choice behavior for the List. By default, recycler fragments do not have any
     * choice behavior ({@link ChoiceMode#NONE}). By setting the choice mode to {@link
     * ChoiceMode#SINGLE}, the recycler fragment allows up to one item to be in an activated state.
     * By setting the choice mode to {@link ChoiceMode#MULTIPLE}, the recycler fragment allows any
     * number of items to be in an activated state.
     *
     * @param choiceMode the choice behavior of the list
     */
    public void setChoiceMode(@NonNull ChoiceMode choiceMode) {
        mChoiceMode = choiceMode;
        // Clear selections if not in action mode.
        // If action mode active, choice mode will not take effect until it's closed.
        if (mActionMode == null) {
            if (mAdapter != null) {
                clearActivatedPositionsAndNotify();
            } else {
                mActivatedPositions.clear();
            }
        }
    }

    /**
     * Set the clickable state of the specified adapter position.
     *
     * @param position    the position in the adapter to set the clickable state
     * @param isClickable {@code true} to set the adapter position as clickable, {@code false}
     *                    otherwise
     */
    public void setClickableAdapterPosition(int position, boolean isClickable) {
        // Check for invalid position.
        if ((mAdapter == null) || (position < 0) || (position >= mAdapter.getItemCount())) {
            throw new IllegalArgumentException();
        } else if (isClickable && mNonClickablePositions.remove(position)) {
            mAdapter.notifyItemChanged(position);
        } else if (!isClickable && mNonClickablePositions.add(position)) {
            mAdapter.notifyItemChanged(position);
        }
    }

    /**
     * Set a {@link ActionModeListener} to be notified of events when the recycler view is in action
     * mode.
     *
     * @param listener listener to be called when the recycler view is in action mode, or {@code
     *                 null} to prevent recycler view from entering action mode
     * @throws IllegalStateException if this method is called while action mode is active
     */
    public void setMultiChoiceModeListener(ActionModeListener listener) throws
            IllegalStateException {
        if (mActionMode != null) {
            throw new IllegalStateException();
        } else {
            mMultiChoiceModeListener =
                    (listener != null) ? new MultiChoiceModeWrapper(listener) : null;
        }
    }

    /**
     * Get the choice mode and action mode saved state to manually restore with {@link
     * #restoreInstanceState}.
     * <p/>
     * Use this method if the items in the adapter are expected to be unchanged at the time this
     * method is called and upon calling {@code restoreInstanceState()}.
     *
     * @return a bundle containing choice mode and action mode saved state
     */
    protected Bundle getInstanceState() {
        final Bundle bundle = new Bundle();
        bundle.putLongArray(SAVE_STATE_ACTIVATED_IDS, getActivatedAdapterIds());
        bundle.putIntArray(SAVE_STATE_ACTIVATED_POSITIONS, getActivatedAdapterPositions());
        bundle.putSerializable(SAVE_STATE_CHOICE_MODE, mChoiceMode);
        bundle.putBoolean(SAVE_STATE_IS_IN_ACTION_MODE, mActionMode != null);
        bundle.putIntArray(SAVE_STATE_NONCLICKABLE_POSITIONS, getNonClickableAdapterPositions());
        return bundle;
    }

    /**
     * Restore choice mode and action mode saved state manually saved via {@link
     * #getInstanceState()}.  An adapter must be set on the recycler view before calling this
     * method.
     * <p/>
     * After calling this method, the adapter will be automatically prompted to refresh its views.
     * <p/>
     * If the recycler view was in action mode when the original state was saved, the action mode
     * will be restarted.
     *
     * @param savedState the saved state previously returned from {@code getInstanceState()}
     */
    protected void restoreInstanceState(Bundle savedState) {
        if (mAdapter == null) {
            throw new IllegalStateException();
        } else {
            // Restore the activated positions.
            mActivatedPositions.clear();
            final int[] activatedPositions = savedState.getIntArray(SAVE_STATE_ACTIVATED_POSITIONS);
            final long[] ids = savedState.getLongArray(SAVE_STATE_ACTIVATED_IDS);
            for (int i = 0; i < activatedPositions.length; i++) {
                mActivatedPositions.put(activatedPositions[i], ids[i]);
            }
            // Restore the clickable positions.
            mNonClickablePositions.clear();
            final int[] nonClickablePositions =
                    savedState.getIntArray(SAVE_STATE_NONCLICKABLE_POSITIONS);
            for (int i : nonClickablePositions) {
                mNonClickablePositions.add(i);
            }
            // Restore choice mode.
            mChoiceMode = (ChoiceMode) savedState.getSerializable(SAVE_STATE_CHOICE_MODE);
            // Restart the action mode.
            if (savedState.getBoolean(SAVE_STATE_IS_IN_ACTION_MODE)) {
                mActionMode = ((AppCompatActivity) getActivity())
                        .startSupportActionMode(mMultiChoiceModeListener);
            }
        }
    }

    /**
     * Clear all activated positions and notify observers of the change.
     */
    private void clearActivatedPositionsAndNotify() {
        final int size = mActivatedPositions.size();
        if (size > 0) {
            final SparseArray<Long> items = mActivatedPositions.clone();
            mActivatedPositions.clear();
            for (int i = 0; i < size; i++) {
                mAdapter.notifyItemChanged(items.keyAt(i));
            }
        }
    }

    /**
     * Ensure the recycler view's state has been built successfully.
     */
    private void ensureList() {
        if (mRecyclerView == null) {
            final View root = getView();
            if (root == null) {
                throw new IllegalStateException("Content view not yet createdAt.");
            } else {
                final View recyclerView = root.findViewById(android.R.id.list);
                if (recyclerView instanceof RecyclerView) {
                    mRecyclerView = (RecyclerView) recyclerView;
                } else {
                    throw new RuntimeException(
                            "Content has view with id attribute 'android.R.id.list' that is not " +
                                    "a RecyclerView class.");
                }
                if (mRecyclerView.getAdapter() != mAdapter) {
                    mRecyclerView.setAdapter(mAdapter);
                }
                mHandler.post(mRequestFocus);
            }
        }
    }

    /**
     * Get the adapter positions that are not clickable.
     *
     * @return an array of the adapter positions that are not clickable; the elements in the
     * returned array are not ordered
     */
    @NonNull
    private int[] getNonClickableAdapterPositions() {
        final int[] positions = new int[mNonClickablePositions.size()];
        int index = 0;
        for (int i : mNonClickablePositions) {
            positions[index] = i;
            index++;
        }
        return positions;
    }

    /**
     * An item in the recycler view was clicked.
     *
     * @param holder the view holder containing the view that was clicked
     */
    private void onItemClick(RecyclerFragmentViewHolder holder) {
        final int position = holder.getAdapterPosition();
        if (position != RecyclerView.NO_POSITION) {
            // Determine if action mode is active at the start of this method.
            final boolean wasInActionMode = (mActionMode != null);
            // Determine current choice mode.
            // If action mode is active, choice mode acts as MULTIPLE.
            final ChoiceMode choiceMode = (mActionMode == null) ? mChoiceMode : ChoiceMode.MULTIPLE;
            // Change the selection status of the items in the layout.
            switch (choiceMode) {
                case NONE:
                    // Do nothing.
                    break;
                case SINGLE:
                    final int newPosition = holder.getAdapterPosition();
                    boolean isFound = false;
                    if (mActivatedPositions.size() > 0) {
                        // Unactivate previously activated item if different from current item.
                        final int oldPosition = mActivatedPositions.keyAt(0);
                        if (oldPosition != newPosition) {
                            mActivatedPositions.delete(oldPosition);
                            mAdapter.notifyItemChanged(oldPosition);
                        } else {
                            isFound = true;
                        }
                    }
                    // Add the activated position if it is not already set.
                    if (!isFound) {
                        mActivatedPositions.put(newPosition, holder.getItemId());
                        mAdapter.notifyItemChanged(newPosition);
                    }
                    break;
                case MULTIPLE:
                    final boolean wasActivated = (mActivatedPositions.indexOfKey(position) >= 0);
                    // Remove the item if it is currently activated.
                    if (wasActivated) {
                        mActivatedPositions.delete(position);
                    }
                    // Add the item if it is not currently activated.
                    else {
                        mActivatedPositions.put(position, holder.getItemId());
                    }
                    mAdapter.notifyItemChanged(position);
                    if (mActionMode != null) {
                        mMultiChoiceModeListener.onAdapterPositionActivatedStateChanged(mActionMode,
                                holder.getAdapterPosition(), !wasActivated);
                    }
                    break;
                default:
                    throw new IllegalArgumentException();
            }
            // If the action mode was not active at the start report the item as clicked.
            if (!wasInActionMode) {
                onItemClick(mRecyclerView, holder);
            }
        }
    }

    /**
     * An item in the recycler view was long clicked.
     *
     * @param holder the view holder containing the view that was clicked
     * @return {@code true} if the long click is processed, {@code false} otherwise
     */
    private boolean onItemLongClick(RecyclerFragmentViewHolder holder) {
        final int position = holder.getAdapterPosition();
        // Start the action mode if not active.
        if ((position != RecyclerView.NO_POSITION) && (mActionMode == null) &&
                (mMultiChoiceModeListener != null)) {
            clearActivatedPositionsAndNotify();
            // Start the action mode and select first item.
            mActionMode = ((AppCompatActivity) getActivity())
                    .startSupportActionMode(mMultiChoiceModeListener);
            mActivatedPositions.put(position, holder.getItemId());
            mAdapter.notifyItemChanged(position);
            // Notify action listener of item selection change.
            mMultiChoiceModeListener
                    .onAdapterPositionActivatedStateChanged(mActionMode, position, true);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Defines the choice behavior for the recycler view. By default, there is not any choice
     * behavior ({@code ChoiceMode.NONE}). {@code ChoiceMode.SINGLE} allows up to one item to  be in
     * a chosen state and {@code ChoiceMode.MULTIPLE} allows any number of items to be chosen.
     */
    public enum ChoiceMode {
        /**
         * Normal list that does not indicate choices.
         */
        NONE,
        /**
         * The list allows up to one choice.
         */
        SINGLE,
        /**
         * The list allows multiple choices.
         */
        MULTIPLE
    }

    /**
     * A listener that receives action event callbacks when the recycler view is in action mode.
     * <p/>
     * This listener acts as the {@link ActionMode.Callback} for the selection mode and also
     * receives {@link #onAdapterPositionActivatedStateChanged(ActionMode, int, boolean)} events
     * when the user changes the activated state of adapter positions.
     */
    public interface ActionModeListener extends ActionMode.Callback {

        /**
         * Called when an the activated state of an adapter position changes during action mode.
         *
         * @param mode        The {@link ActionMode} providing the selection mode
         * @param position    the position in the adapter that was changed
         * @param isActivated {@code true} if the item is now activated, {@code false} if the item
         *                    is no longer activated
         */
        void onAdapterPositionActivatedStateChanged(ActionMode mode, int position,
                                                    boolean isActivated);

    }

    /**
     * A view holder for handling items in a {@link RecyclerFragment}.
     * <p/>
     * Subclasses must not override the {@code itemView}'s {@link OnClickListener} or {@link
     * OnLongClickListener} for proper functionality of choice modes and action mode.
     */
    public static class RecyclerFragmentViewHolder extends ViewHolder {

        /**
         * Recycler fragment to act as a callback for clicks and long clicks.
         */
        private final RecyclerFragment mFragment;

        /**
         * Create a new view holder.
         *
         * @param itemView the view being managed by this view holder.
         * @param fragment the recycler fragment that will manage this view holder
         */
        public RecyclerFragmentViewHolder(View itemView, @NonNull RecyclerFragment fragment) {
            super(itemView);
            mFragment = fragment;
            // Set up click listeners.
            itemView.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View view) {
                    mFragment.onItemClick(RecyclerFragmentViewHolder.this);
                }

            });
            itemView.setOnLongClickListener(new OnLongClickListener() {

                @Override
                public boolean onLongClick(View view) {
                    return mFragment.onItemLongClick(RecyclerFragmentViewHolder.this);
                }

            });
        }

        /**
         * Perform base binding of this view holder's {@link #itemView} with the {@code
         * RecyclerFragment}.
         * <p/>
         * Adapters using this view holder must call this method in {@link
         * Adapter#onBindViewHolder(ViewHolder, int)} and {@link
         * Adapter#onBindViewHolder(ViewHolder, int, List)}.
         */
        public void onBindViewHolder() {
            final int position = getAdapterPosition();
            itemView.setActivated(mFragment.isActivatedAdapterPosition(position));
            final boolean isClickable = mFragment.isClickableAdapterPosition(position);
            itemView.setClickable(isClickable);
            itemView.setLongClickable(isClickable);
        }

    }

    /**
     * A class used to wrap a {@link ActionModeListener} in order to provide additional underlying
     * functionality when action mode is active.
     */
    private class MultiChoiceModeWrapper implements ActionModeListener {

        /**
         * The listener that is wrapped by this object.
         */
        private final ActionModeListener mWrapped;

        /**
         * Construct a new wrapper with the specified listener.
         *
         * @param wrapped the listener to wrap
         */
        public MultiChoiceModeWrapper(@NonNull ActionModeListener wrapped) {
            mWrapped = wrapped;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            return mWrapped.onCreateActionMode(mode, menu);
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return mWrapped.onPrepareActionMode(mode, menu);
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return mWrapped.onActionItemClicked(mode, item);
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mWrapped.onDestroyActionMode(mode);
            mActionMode = null;
            clearActivatedPositionsAndNotify();
        }

        @Override
        public void onAdapterPositionActivatedStateChanged(ActionMode mode, int position,
                                                           boolean isActivated) {
            mWrapped.onAdapterPositionActivatedStateChanged(mode, position, isActivated);
            // Close action mode if no items selected.
            if (mActivatedPositions.size() == 0) {
                mode.finish();
            }
        }

    }

    /**
     * Class used for updating the recycler fragment based on adapter data changes.
     */
    private class RecyclerFragmentAdapterDataObserver extends AdapterDataObserver {

        @Override
        public void onChanged() {
            // Must assume data has completely changed, clear saved position states.
            mActivatedPositions.clear();
            mNonClickablePositions.clear();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            // Update activated positions.
            if (mActivatedPositions.size() > 0) {
                final SparseArray<Long> newPositions = mActivatedPositions.clone();
                mActivatedPositions.clear();
                final int size = newPositions.size();
                for (int i = 0; i < size; i++) {
                    final int position = newPositions.keyAt(i);
                    mActivatedPositions
                            .append((position < positionStart) ? position : position + itemCount,
                                    newPositions.valueAt(i));
                }
            }
            // Update clickable positions.
            if (mNonClickablePositions.size() > 0) {
                final Set<Integer> newPositions = new HashSet<>();
                for (int i : mNonClickablePositions) {
                    final int newPosition = (i < positionStart) ? i : i + itemCount;
                    newPositions.add(newPosition);
                }
                mNonClickablePositions = newPositions;
            }
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            final int smallestPosition = Math.min(fromPosition, toPosition);
            final int largestPosition = Math.max(fromPosition, toPosition);
            // Update activated positions.
            if (mActivatedPositions.size() > 0) {
                final SparseArray<Long> items = mActivatedPositions.clone();
                mActivatedPositions.clear();
                final int size = items.size();
                for (int i = 0; i < size; i++) {
                    final int position = items.keyAt(i);
                    // Current position is before or after the affected positions.
                    if ((position < smallestPosition) ||
                            (position >= (largestPosition + itemCount))) {
                        mActivatedPositions.append(position, items.valueAt(i));
                    }
                    // Current position is within the range being moved.
                    else if ((position >= fromPosition) &&
                            (position < (fromPosition + itemCount))) {
                        mActivatedPositions
                                .put(position + toPosition - fromPosition, items.valueAt(i));
                    }
                    // Current position is affected, but not within the range being moved.
                    else {
                        mActivatedPositions
                                .put(position + fromPosition - toPosition, items.valueAt(i));
                    }
                }
            }
            // Update clickable positions.
            if (mNonClickablePositions.size() > 0) {
                final Set<Integer> newPositions = new HashSet<>();
                for (int i : mNonClickablePositions) {
                    if ((i < smallestPosition) || (i >= (largestPosition + itemCount))) {
                        newPositions.add(i);
                    } else if ((i >= fromPosition) && (i < (fromPosition + itemCount))) {
                        newPositions.add(i + toPosition - fromPosition);
                    } else {
                        newPositions.add(i + fromPosition - toPosition);
                    }
                }
                mNonClickablePositions = newPositions;
            }
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            // Update activated positions.
            if (mActivatedPositions.size() > 0) {
                final SparseArray<Long> newPositions = mActivatedPositions.clone();
                mActivatedPositions.clear();
                final int size = newPositions.size();
                for (int i = 0; i < size; i++) {
                    final int position = newPositions.keyAt(i);
                    if (position < positionStart) {
                        mActivatedPositions.append(position, newPositions.valueAt(i));
                    } else if (position >= (positionStart + itemCount)) {
                        mActivatedPositions
                                .append(position - itemCount, newPositions.valueAt(i));
                    }
                }
            }
            // Update clickable positions.
            if (mNonClickablePositions.size() > 0) {
                final Set<Integer> newPositions = new HashSet<>();
                for (int i : mNonClickablePositions) {
                    if (i < positionStart) {
                        newPositions.add(i);
                    } else if (i >= (positionStart + itemCount)) {
                        newPositions.add(i - itemCount);
                    }
                }
                mNonClickablePositions = newPositions;
            }
        }

    }

}
