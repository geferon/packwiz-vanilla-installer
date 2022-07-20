package link.infra.packwiz.vanillainstaller.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class Debouncer {
    private final ScheduledExecutorService sched = Executors.newScheduledThreadPool(1);
    private TimerTask delayed = null;
    private final Runnable callback;
    private final int interval;

    public Debouncer(Runnable c, int interval) {
        this.callback = c;
        this.interval = interval;
    }

    public void call() {
        if (delayed != null) delayed.extend();
        else {
            delayed = new TimerTask();
            sched.schedule(delayed, interval, TimeUnit.MILLISECONDS);
        }
    }

    public void finish() {
        terminate();
        callback.run();
    }

    public void terminate() {
        sched.shutdownNow();
    }

    // The task that wakes up when the wait time elapses
    private class TimerTask implements Runnable {
        private long dueTime;
        private final Object lock = new Object();

        public TimerTask() {
            extend();
        }

        public boolean extend() {
            synchronized (lock) {
                if (dueTime < 0) // Task has been shutdown
                    return false;
                dueTime = System.currentTimeMillis() + interval;
                return true;
            }
        }

        public void run() {
            synchronized (lock) {
                long remaining = dueTime - System.currentTimeMillis();
                if (remaining > 0) { // Re-schedule task
                    sched.schedule(this, remaining, TimeUnit.MILLISECONDS);
                } else { // Mark as terminated and invoke callback
                    dueTime = -1;
                    try {
                        callback.run();
                    } finally {
                        delayed = null;
                    }
                }
            }
        }
    }
}