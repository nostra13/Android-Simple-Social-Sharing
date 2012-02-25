package com.nostra13.socialsharing.twitter;

import java.util.LinkedList;
import java.util.List;

import android.util.Log;

/**
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 */
final class TaskExecutor {

	private static final String TAG = TaskExecutor.class.getSimpleName();

	private static final int ASYNC_NUM_THREADS = 1;

	private Task[] threads;
	private final List<Runnable> queue = new LinkedList<Runnable>();

	public static TaskExecutor newInstance() {
		return new TaskExecutor();
	}

	private TaskExecutor() {
		threads = new Task[ASYNC_NUM_THREADS];
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new Task("Twitter4J Async Dispatcher", i);
			threads[i].setDaemon(true);
			threads[i].start();
		}
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				if (active) {
					shutdown();
				}
			}
		});
	}

	public synchronized void invokeLater(Runnable task) {
		synchronized (queue) {
			queue.add(task);
		}
		synchronized (syncObject) {
			syncObject.notify();
		}
	}

	final Object syncObject = new Object();

	public Runnable poll() {
		while (active) {
			synchronized (queue) {
				if (queue.size() > 0) {
					Runnable task = queue.remove(0);
					if (task != null) {
						return task;
					}
				}
			}
			synchronized (syncObject) {
				try {
					syncObject.wait();
				} catch (InterruptedException ignore) {
				}
			}
		}
		return null;
	}

	private boolean active = true;

	public synchronized void shutdown() {
		if (active) {
			active = false;
			for (Task thread : threads) {
				thread.shutdown();
			}
			synchronized (syncObject) {
				syncObject.notify();
			}
		}
	}

	private class Task extends Thread {

		Task(String name, int index) {
			super(name + "[" + index + "]");
		}

		public void shutdown() {
			alive = false;
		}

		private boolean alive = true;

		public void run() {
			while (alive) {
				Runnable task = TaskExecutor.this.poll();
				if (task != null) {
					try {
						task.run();
					} catch (Exception ex) {
						Log.e(TAG, "Got an exception while running a taks:", ex);
					}
				}
			}
		}
	}
}