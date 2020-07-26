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
package com.example.journals.app;

import java.util.Iterator;

import androidx.annotation.NonNull;

/**
 * A list for testing purposes.
 */
public class List2<T> implements Iterable<T> {

    private ListItem<T> mListStart;
    private ListItem<T> mListEnd;
    private int mSize = 0;

    // Default constructor.
    public List2() {
    }

    public static void main(String[] args) {

        List2<String> list = new List2<>();
        list.put("A");
        list.put("B");
        list.put("C");
        list.put("D");
        Iterator<String> iterator = list.iterator();
        iterator.next();
        iterator.remove();

        System.out.println(list.toString());
        System.out.println("Start: " + list.mListStart + " End: " + list.mListEnd);
        list.reverse();
        System.out.println(list.toString());
        System.out.println("Start: " + list.mListStart + " End: " + list.mListEnd);

        //        final String text = "CAB FDE GIH JKL";
        //
        //        final StringBuilder builder = new StringBuilder();
        //        int pointer = 0;
        //        // Iterate through string.
        //        for (int i = 0; i < text.length(); i++) {
        //            if (text.charAt(i) == ' ') {
        //                char[] array = text.substring(pointer, i).toCharArray();
        //                Arrays.sort(array);
        //                builder.append(array.toString());
        //                builder.append(" ");
        //                // Update pointer for next substring.
        //                pointer = i + 1;
        //            }
        //        }
        //        char[] array = text.substring(pointer, text.length()).toCharArray();
        //        Arrays.sort(array);
        //        builder.append(array.toString());
        //
        //        // Build the string.
        //        System.out.println(builder.toString());
        //
        //
        //
        //
        //        int maxSum = Integer.MIN_VALUE;
        //        int currentSum = Integer.MIN_VALUE;
        //        final int[] intArray = new int[]{4, 7, -3, 2, -20, 6};
        //
        //        // Iterate through array.
        //        for (int i : intArray) {
        //
        //            // Increment or reset current value.
        //            if (currentSum > 0) {
        //                currentSum += i;
        //            } else {
        //                currentSum = i;
        //            }
        //
        //            // Update max value.
        //            if (currentSum > maxSum) {
        //                maxSum = currentSum;
        //            }
        //        }
        //
        //        // Display the result.
        //        System.out.println("The maximum sum is: " + maxSum);
        //
    }

    public T get(int index) {
        ListItem<T> listItem = mListStart;
        int current = 0;
        while (current < index) {
            listItem = listItem.next;
            current++;
        }
        return listItem.value;
    }

    public void put(T item) {
        final ListItem<T> listItem = new ListItem<>();
        listItem.value = item;
        if (mSize == 0) {
            mListStart = listItem;
        } else {
            mListEnd.next = listItem;
        }
        mListEnd = listItem;
        mSize++;
    }

    public int size() {
        return mSize;
    }

    // Reverse a list.
    // A-> B-> C-> D changes to A <-B <-C <-D.
    public void reverse() {
        if (mSize > 1) {
            ListItem<T> previous = mListStart;
            ListItem<T> current = mListStart.next;
            mListEnd = mListStart;
            mListEnd.next = null;
            while (current != null) {
                // Update start of list
                mListStart = current;
                // Reverse link direction.
                final ListItem<T> next = current.next;
                current.next = previous;
                // Iterate list items.
                previous = current;
                current = next;
            }
        }
    }

    @NonNull
    @Override
    public String toString() {
        if (mSize == 0) {
            return "";
        } else {
            final StringBuilder builder = new StringBuilder();
            for (T t : this) {
                builder.append(t.toString());
                builder.append(" -> ");
            }
            return builder.toString();
        }
    }

    @NonNull
    @Override
    public Iterator<T> iterator() {
        return new List2Iterator<>(this);
    }

    private static class List2Iterator<T> implements Iterator<T> {

        private List2<T> mList;
        private ListItem<T> mPrevious, mCurrent, mNext;
        private boolean isRemoved = false;

        public List2Iterator(List2<T> list) {
            mList = list;
            mNext = list.mListStart;
        }

        @Override
        public boolean hasNext() {
            return (mNext != null);
        }

        @Override
        public T next() {
            final T next = mNext.value;
            // Increment positions.
            mPrevious = mCurrent;
            mCurrent = mNext;
            mNext = mCurrent.next;
            // Update "remove" status.
            isRemoved = false;
            return next;
        }

        @Override
        public void remove() {
            if (isRemoved) {
                throw new IllegalArgumentException();
            }
            // First item in list.
            else if (mPrevious == null) {
                mList.mListStart = mNext;
                mCurrent.next = null;
                mCurrent = null;
                isRemoved = true;
            } else {
                mPrevious.next = mNext;
                mCurrent.next = null;
                isRemoved = true;
            }
        }

    }

    private static class ListItem<U> {

        public U value;
        public ListItem<U> next;

        @NonNull
        @Override
        public String toString() {
            if (value == null) {
                return "";
            } else {
                return value.toString();
            }
        }

    }

}
