package android.database;

import java.io.Closeable;

/**
 * Desktop subset of android.database.Cursor. Declared in Java (like Android's)
 * so getters return platform types and upstream Kotlin compiles unchanged.
 */
public interface Cursor extends Closeable {
    int FIELD_TYPE_NULL = 0;
    int FIELD_TYPE_INTEGER = 1;
    int FIELD_TYPE_FLOAT = 2;
    int FIELD_TYPE_STRING = 3;
    int FIELD_TYPE_BLOB = 4;

    int getCount();
    int getPosition();
    int getColumnCount();
    String[] getColumnNames();
    boolean isClosed();
    boolean moveToFirst();
    boolean moveToNext();
    boolean moveToPosition(int position);
    boolean moveToLast();
    boolean isAfterLast();
    int getColumnIndex(String columnName);
    int getColumnIndexOrThrow(String columnName);
    String getColumnName(int columnIndex);
    int getType(int columnIndex);
    boolean isNull(int columnIndex);
    String getString(int columnIndex);
    long getLong(int columnIndex);
    int getInt(int columnIndex);
    short getShort(int columnIndex);
    double getDouble(int columnIndex);
    float getFloat(int columnIndex);
    byte[] getBlob(int columnIndex);

    @Override
    void close();
}
