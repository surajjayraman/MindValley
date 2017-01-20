package ielite.app.imageloadlibrary;

/**
 * Created by Suraj on 20-01-2017.
 */

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import static android.content.Context.ACTIVITY_SERVICE;
import static android.content.pm.ApplicationInfo.FLAG_LARGE_HEAP;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.KITKAT;

/** A memory cache which uses a least-recently used eviction policy. */
public class LruCache implements Cache {
    final LinkedHashMap<String, Bitmap> map;
    private final int maxSize;

    private int size;
    private int putCount;
    private int evictionCount;
    private int hitCount;
    private int missCount;
    static final char KEY_SEPARATOR = '\n';

    static int calculateMemoryCacheSize(Context context) {
        ActivityManager am = getService(context, ACTIVITY_SERVICE);
        boolean largeHeap = (context.getApplicationInfo().flags & FLAG_LARGE_HEAP) != 0;
        int memoryClass = largeHeap ? am.getLargeMemoryClass() : am.getMemoryClass();
        // Target ~15% of the available heap.
        return (int) (1024L * 1024L * memoryClass / 7);
    }

    static <T> T getService(Context context, String service) {
        return (T) context.getSystemService(service);
    }

    static int getBitmapBytes(Bitmap bitmap) {
        int result = SDK_INT >= KITKAT ? bitmap.getAllocationByteCount() : bitmap.getByteCount();
        if (result < 0) {
            throw new IllegalStateException("Negative size: " + bitmap);
        }
        return result;
    }

    /** Create a cache using an appropriate portion of the available RAM as the maximum size. */
    public LruCache(@NonNull Context context) {
        this(calculateMemoryCacheSize(context));
    }

    /** Create a cache with a given maximum size in bytes. */
    public LruCache(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("Max size must be positive.");
        }
        this.maxSize = maxSize;
        this.map = new LinkedHashMap<>(0, 0.75f, true);
    }

    @Override public Bitmap get(@NonNull String key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }

        Bitmap mapValue;
        synchronized (this) {
            mapValue = map.get(key);
            if (mapValue != null) {
                hitCount++;
                return mapValue;
            }
            missCount++;
        }

        return null;
    }

    @Override public void set(@NonNull String key, @NonNull Bitmap bitmap) {
        if (key == null || bitmap == null) {
            throw new NullPointerException("key == null || bitmap == null");
        }

        int addedSize = getBitmapBytes(bitmap);
        if (addedSize > maxSize) {
            return;
        }

        synchronized (this) {
            putCount++;
            size += addedSize;
            Bitmap previous = map.put(key, bitmap);
            if (previous != null) {
                size -= getBitmapBytes(previous);
            }
        }

        trimToSize(maxSize);
    }

    private void trimToSize(int maxSize) {
        while (true) {
            String key;
            Bitmap value;
            synchronized (this) {
                if (size < 0 || (map.isEmpty() && size != 0)) {
                    throw new IllegalStateException(
                            getClass().getName() + ".sizeOf() is reporting inconsistent results!");
                }

                if (size <= maxSize || map.isEmpty()) {
                    break;
                }

                Map.Entry<String, Bitmap> toEvict = map.entrySet().iterator().next();
                key = toEvict.getKey();
                value = toEvict.getValue();
                map.remove(key);
                size -= getBitmapBytes(value);
                evictionCount++;
            }
        }
    }

    /** Clear the cache. */
    public final void evictAll() {
        trimToSize(-1); // -1 will evict 0-sized elements
    }

    @Override public final synchronized int size() {
        return size;
    }

    @Override public final synchronized int maxSize() {
        return maxSize;
    }

    @Override public final synchronized void clear() {
        evictAll();
    }

    @Override public final synchronized void clearKeyUri(String uri) {
        int uriLength = uri.length();
        for (Iterator<Map.Entry<String, Bitmap>> i = map.entrySet().iterator(); i.hasNext();) {
            Map.Entry<String, Bitmap> entry = i.next();
            String key = entry.getKey();
            Bitmap value = entry.getValue();
            int newlineIndex = key.indexOf(KEY_SEPARATOR);
            if (newlineIndex == uriLength && key.substring(0, newlineIndex).equals(uri)) {
                i.remove();
                size -= getBitmapBytes(value);
            }
        }
    }

    /** Returns the number of times {@link #get} returned a value. */
    public final synchronized int hitCount() {
        return hitCount;
    }

    /** Returns the number of times {@link #get} returned {@code null}. */
    public final synchronized int missCount() {
        return missCount;
    }

    /** Returns the number of times {@link #set(String, Bitmap)} was called. */
    public final synchronized int putCount() {
        return putCount;
    }

    /** Returns the number of values that have been evicted. */
    public final synchronized int evictionCount() {
        return evictionCount;
    }
}