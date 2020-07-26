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

import android.database.CharArrayBuffer;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.provider.BaseColumns;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;

/**
 * A cursor wrapper that manages a field representing {@link BaseColumns#_COUNT} that is not part of
 * the wrapped cursor.
 */
public class CountCursorWrapper extends CursorWrapper {

    /**
     * The position of the {@link BaseColumns#_COUNT} column in the cursor.
     */
    private final int mColumnIndex;

    /**
     * Create a new adapter wrapper.
     *
     * @param wrappedCursor the base cursor to be wrapped by this cursor
     * @param columnIndex   the position to place the {@link BaseColumns#_COUNT} column in the
     *                      cursor
     */
    public CountCursorWrapper(@NonNull Cursor wrappedCursor, int columnIndex) {
        super(wrappedCursor);
        if ((columnIndex >= 0) && (columnIndex <= wrappedCursor.getColumnCount())) {
            mColumnIndex = columnIndex;
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public int getColumnCount() {
        return super.getColumnCount() + 1;
    }

    @Override
    public int getColumnIndex(String columnName) {
        if (BaseColumns._COUNT.equals(columnName)) {
            return mColumnIndex;
        } else {
            final int index = super.getColumnIndex(columnName);
            return (index < mColumnIndex) ? index : index + 1;
        }
    }

    @Override
    public int getColumnIndexOrThrow(String columnName) throws IllegalArgumentException {
        if (BaseColumns._COUNT.equals(columnName)) {
            return mColumnIndex;
        } else {
            final int index = super.getColumnIndexOrThrow(columnName);
            return (index < mColumnIndex) ? index : index + 1;
        }
    }

    @Override
    public String getColumnName(int columnIndex) {
        if (columnIndex == mColumnIndex) {
            return BaseColumns._COUNT;
        } else {
            return super
                    .getColumnName((columnIndex > mColumnIndex) ? columnIndex - 1 : columnIndex);
        }
    }

    @Override
    public String[] getColumnNames() {
        final String[] originalNames = super.getColumnNames();
        final List<String> columnNames = new ArrayList<>(originalNames.length + 1);
        columnNames.addAll(Arrays.asList(originalNames));
        columnNames.add(mColumnIndex, BaseColumns._COUNT);
        return columnNames.toArray(new String[originalNames.length + 1]);
    }

    @Override
    public double getDouble(int columnIndex) {
        if (columnIndex == mColumnIndex) {
            return super.getCount();
        } else {
            return super.getDouble((columnIndex > mColumnIndex) ? columnIndex - 1 : columnIndex);
        }
    }

    @Override
    public float getFloat(int columnIndex) {
        if (columnIndex == mColumnIndex) {
            return super.getCount();
        } else {
            return super.getFloat((columnIndex > mColumnIndex) ? columnIndex - 1 : columnIndex);
        }
    }

    @Override
    public int getInt(int columnIndex) {
        if (columnIndex == mColumnIndex) {
            return super.getCount();
        } else {
            return super.getInt((columnIndex > mColumnIndex) ? columnIndex - 1 : columnIndex);
        }
    }

    @Override
    public long getLong(int columnIndex) {
        if (columnIndex == mColumnIndex) {
            return super.getCount();
        } else {
            return super.getLong((columnIndex > mColumnIndex) ? columnIndex - 1 : columnIndex);
        }
    }

    @Override
    public short getShort(int columnIndex) {
        if (columnIndex == mColumnIndex) {
            throw new IllegalArgumentException();
        } else {
            return super.getShort((columnIndex > mColumnIndex) ? columnIndex - 1 : columnIndex);
        }
    }

    @Override
    public String getString(int columnIndex) {
        if (columnIndex == mColumnIndex) {
            return Integer.toString(super.getCount());
        } else {
            return super.getString((columnIndex > mColumnIndex) ? columnIndex - 1 : columnIndex);
        }
    }

    @Override
    public void copyStringToBuffer(int columnIndex, CharArrayBuffer buffer) {
        if (columnIndex == mColumnIndex) {
            final String count = Integer.toString(super.getCount());
            if ((buffer.data == null) || (buffer.data.length < count.length())) {
                buffer.data = new char[count.length()];
            }
            count.getChars(0, count.length(), buffer.data, 0);
            buffer.sizeCopied = count.length();
        } else {
            super.copyStringToBuffer((columnIndex > mColumnIndex) ? columnIndex - 1 : columnIndex,
                    buffer);
        }
    }

    @Override
    public byte[] getBlob(int columnIndex) {
        if (columnIndex == mColumnIndex) {
            throw new IllegalArgumentException();
        } else {
            return super.getBlob((columnIndex > mColumnIndex) ? columnIndex - 1 : columnIndex);
        }
    }

    @Override
    public int getType(int columnIndex) {
        if (columnIndex == mColumnIndex) {
            return Cursor.FIELD_TYPE_INTEGER;
        } else {
            return super.getType((columnIndex > mColumnIndex) ? columnIndex - 1 : columnIndex);
        }
    }

    @Override
    public boolean isNull(int columnIndex) {
        if (columnIndex == mColumnIndex) {
            return false;
        } else {
            return super.isNull((columnIndex > mColumnIndex) ? columnIndex - 1 : columnIndex);
        }
    }

}
