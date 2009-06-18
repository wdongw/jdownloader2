package jd.controlling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import jd.nutils.Formatter;

public class ByteBufferController {

    private ArrayList<ByteBufferEntry> bufferpool;

    public final static String MAXBUFFERSIZE = "MAX_BUFFER_SIZE_V3";

    private static ByteBufferController INSTANCE;

    protected Integer BufferFresh = new Integer(0);
    protected Integer BufferReused = new Integer(0);
    protected Integer BufferFree = new Integer(0);

    public synchronized static ByteBufferController getInstance() {
        if (INSTANCE == null) INSTANCE = new ByteBufferController();
        return INSTANCE;
    }

    public void printDebug() {
        JDLogger.getLogger().info("ByteBufferController: Fresh: " + Formatter.formatReadable(BufferFresh) + " Reused: " + Formatter.formatReadable(BufferReused) + " Free: " + Formatter.formatReadable(BufferFree));
    }

    private ByteBufferController() {
        bufferpool = new ArrayList<ByteBufferEntry>();
        Thread thread = new Thread() {
            public void run() {
                while (true) {
                    ByteBufferController.getInstance().printDebug();
                    try {
                        sleep(1000 * 60 * 10);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        };
        thread.start();
    }

    protected void increaseFresh(int size) {
        synchronized (BufferFresh) {
            BufferFresh += size;
        }
    }

    protected void decreaseFresh(int size) {
        synchronized (BufferFresh) {
            BufferFresh -= size;
        }
    }

    protected void increaseFree(int size) {
        synchronized (BufferFree) {
            BufferFree += size;
        }
    }

    protected void decreaseFree(int size) {
        synchronized (BufferFree) {
            BufferFree -= size;
        }
    }

    protected void increaseReused(int size) {
        synchronized (BufferReused) {
            BufferReused += size;
        }
    }

    protected void decreaseReused(int size) {
        synchronized (BufferReused) {
            BufferReused -= size;
        }
    }

    protected ByteBufferEntry getByteBufferEntry(int size) {
        ByteBufferEntry ret = null;
        synchronized (bufferpool) {
            for (ByteBufferEntry entry : bufferpool) {
                if (entry.capacity() >= size) {
                    // JDLogger.getLogger().severe("found bytebufferentry with "
                    // + entry.capacity() + " to serve request with " + size);
                    ret = entry;
                    bufferpool.remove(entry);
                    return ret.getbytebufferentry(size);
                }
            }
        }
        // JDLogger.getLogger().severe("no bytebufferentry found to serve request with "
        // + size);
        return null;
    }

    protected void putByteBufferEntry(ByteBufferEntry entry) {
        synchronized (bufferpool) {
            if (!bufferpool.contains(entry)) bufferpool.add(entry);
            Collections.sort(bufferpool, new Comparator<ByteBufferEntry>() {
                public int compare(ByteBufferEntry a, ByteBufferEntry b) {
                    return a.capacity() == b.capacity() ? 0 : a.capacity() > b.capacity() ? 1 : -1;
                }
            });
        }
    }
}
