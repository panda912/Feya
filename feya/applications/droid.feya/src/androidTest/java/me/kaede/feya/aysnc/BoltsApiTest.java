/*
 * Copyright (c) 2017. Kaede (kidhaibara@gmail.com) All Rights Reserved.
 */

package me.kaede.feya.aysnc;

import android.os.Looper;
import android.util.Log;

import junit.framework.TestCase;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import bolts.AggregateException;
import bolts.CancellationToken;
import bolts.CancellationTokenSource;
import bolts.Capture;
import bolts.Continuation;
import bolts.Task;
import bolts.TaskCompletionSource;

/**
 * Bolts api demo
 * Bolts is a collection of low-level libraries designed to make developing mobile apps easier.
 * {@link "https://github.com/BoltsFramework/Bolts-Android"}
 *
 * @author Kaede
 * @since date 16/8/26
 */
public class BoltsApiTest extends TestCase {

    public static final String TAG = "Bolts";

    /**
     * {@link Task#call(Callable)}
     * {@link Task#getResult()}
     */
    public void testImmediateTask() {
        Task<Integer> integerTask = Task.call(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return 10086;
            }
        });
        int result = integerTask.getResult();
        assertTrue(result == 10086);
    }

    /**
     * {@link Task#callInBackground(Callable)}
     * {@link Task#waitForCompletion()}
     */
    public void testTaskInBackground() {
        Task<Integer> integerTask = Task.callInBackground(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                // assert in background
                assertTrue(Looper.myLooper() == null);
                Thread.sleep(1000);
                return 10086;
            }
        });

        assertTrue(integerTask.getResult() == null);

        try {
            integerTask.waitForCompletion();
            assertTrue(integerTask.getResult() == 10086);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * {@link Task#call(Callable, Executor)}
     */
    public void testTaskWithExecutor() {
        Task<Integer> integerTask = Task.call(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return 10086;
            }
        }, Task.UI_THREAD_EXECUTOR);

        assertTrue(integerTask.getResult() == null);

        try {
            integerTask.waitForCompletion();
            assertTrue(integerTask.getResult() == 10086);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    static final Executor NETWORK_EXECUTOR = Executors.newCachedThreadPool();

    public void testTaskWithCustomExecutor() {
        Task<String> task = Task.call(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                // assert in background
                assertTrue(Looper.myLooper() == null);
                Thread.sleep(1000);
                return 10086;
            }
        }, NETWORK_EXECUTOR).continueWith(new Continuation<Integer, String>() {
            @Override
            public String then(Task<Integer> task) throws Exception {
                // assert in ui thread
                assertTrue(Looper.myLooper() == Looper.getMainLooper());

                if (task.isFaulted()) {
                    // fail
                    return "fail";
                }
                if (task.isCancelled()) {
                    // canceled
                    return "canceled";
                }
                Integer integer = task.getResult();
                return "success get " + integer;
            }
        }, Task.UI_THREAD_EXECUTOR);

        try {
            task.waitForCompletion();
            assertEquals(task.getResult(), "success get 10086");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * {@link Task#continueWith(Continuation)}
     * {@link Continuation}
     * {@link Continuation#then(Task)}
     */
    public void testContinueWith() {
        String result = Task.call(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return 10086;
            }
        }).continueWith(new Continuation<Integer, String>() {
            @Override
            public String then(Task<Integer> task) throws Exception {
                if (task.isFaulted()) {
                    // fail
                    return "fail";
                }
                if (task.isCancelled()) {
                    // canceled
                    return "canceled";
                }
                Integer integer = task.getResult();
                return "success get " + integer;
            }
        }).getResult();

        assertEquals(result, "success get 10086");
    }

    /**
     * equal with {@link BoltsApiTest#testContinueWith}
     */
    public void testContinueWith2() {
        Callable<Integer> integerCallable = new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return 10086;
            }
        };

        Continuation<Integer, String> stringContinuation = new Continuation<Integer, String>() {
            @Override
            public String then(Task<Integer> task) throws Exception {
                if (task.isFaulted()) {
                    // fail
                    return "fail";
                }
                if (task.isCancelled()) {
                    // canceled
                    return "canceled";
                }
                Integer integer = task.getResult();
                return "success get " + integer;
            }
        };

        Task<Integer> integerTask = Task.call(integerCallable);
        Task<String> stringTask = integerTask.continueWith(stringContinuation);

        String result = stringTask.getResult();
        assertEquals(result, "success get 10086");
    }

    /**
     * {@link Task#continueWithTask(Continuation)}
     */
    public void testContinueWithTask() {
        String result = Task.call(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return 10086;
            }
        }).continueWithTask(new Continuation<Integer, Task<String>>() {
            @Override
            public Task<String> then(final Task<Integer> task) throws Exception {
                return Task.call(new Callable<String>() {
                    @Override
                    public String call() throws Exception {
                        if (task.isFaulted()) {
                            // fail
                            return "fail";
                        }
                        if (task.isCancelled()) {
                            // canceled
                            return "canceled";
                        }
                        Integer integer = task.getResult();
                        return "success get " + integer;
                    }
                });
            }
        }).getResult();

        assertEquals(result, "success get 10086");
    }

    /**
     * {@link Task#onSuccess(Continuation)}
     */
    public void testOnSuccess() {
        String result = Task.call(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return 10086;
            }
        }).onSuccess(new Continuation<Integer, String>() {
            @Override
            public String then(Task<Integer> task) throws Exception {
                if (task.isFaulted()) {
                    // fail
                    return "fail";
                }
                if (task.isCancelled()) {
                    // canceled
                    return "canceled";
                }
                Integer integer = task.getResult();
                return "success get " + integer;
            }
        }).getResult();

        assertEquals(result, "success get 10086");

        result = Task.call(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                throw new RuntimeException("fail");
            }
        }).onSuccess(new Continuation<Integer, String>() {
            @Override
            public String then(Task<Integer> task) throws Exception {
                if (task.isFaulted()) {
                    // fail
                    return "fail";
                }
                if (task.isCancelled()) {
                    // canceled
                    return "canceled";
                }
                Integer integer = task.getResult();
                return "success get " + integer;
            }
        }).getResult();

        assertEquals(result, null);
    }

    /**
     * {@link Task#onSuccessTask(Continuation)}
     */
    public void testOnSuccessTask() {
        String result = Task.call(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return 10086;
            }
        }).onSuccessTask(new Continuation<Integer, Task<String>>() {
            @Override
            public Task<String> then(final Task<Integer> task) throws Exception {
                return Task.call(new Callable<String>() {
                    @Override
                    public String call() throws Exception {
                        if (task.isFaulted()) {
                            // fail
                            return "fail";
                        }
                        if (task.isCancelled()) {
                            // canceled
                            return "canceled";
                        }
                        Integer integer = task.getResult();
                        return "success get " + integer;
                    }
                });
            }
        }).getResult();

        assertEquals(result, "success get 10086");

    }

    /**
     * execute parallel tasks.
     * callback when all tasks are done.
     * {@link Task#whenAll(Collection)}
     */
    public void testParallelTask() {
        Task<Integer> task1 = Task.callInBackground(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return 10086;
            }
        });

        Task<Integer> task2 = Task.callInBackground(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return 65535;
            }
        });

        final List<Task<?>> tasks = new ArrayList<>();
        tasks.add(task1);
        tasks.add(task2);

        try {
            // parallel call both task, and wait for both tasks done
            Task.whenAll(tasks).continueWith(new Continuation<Void, Object>() {
                @Override
                public Object then(Task<Void> task) throws Exception {
                    // task1 and task2 are both finished
                    for (int i = 0; i < tasks.size(); i++) {
                        Task<?> item = tasks.get(i);
                        assertTrue(item.getResult().equals(10086) || item.getResult().equals(65535));
                    }
                    return null;
                }
            }).waitForCompletion();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * execute parallel tasks.
     * callback when any task is done.
     * {@link Task#whenAny(Collection)}
     */
    public void testParallelTask2() {
        Task<Integer> task1 = Task.callInBackground(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return 10086;
            }
        });

        Task<Integer> task2 = Task.callInBackground(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return 65535;
            }
        });

        final List<Task<?>> tasks = new ArrayList<>();
        tasks.add(task1);
        tasks.add(task2);

        try {
            Task.whenAny(tasks).continueWith(new Continuation<Task<?>, Object>() {
                @Override
                public Object then(Task<Task<?>> task) throws Exception {
                    // task1 or task2 is finished
                    Task<?> taskFinished = task.getResult();
                    assertTrue(taskFinished.getResult().equals(10086) || taskFinished.getResult().equals(65535));
                    return null;
                }
            }).waitForCompletion();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * execute 3 parallel tasks with one failed.
     * get the error fo the failed task.
     */
    public void testParallelTask3() {
        Task<Integer> task1 = Task.callInBackground(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return 10086;
            }
        });

        Task<Integer> task2 = Task.callInBackground(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return 65535;
            }
        });

        Task<Integer> task3 = Task.callInBackground(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                throw new FileNotFoundException("404");
            }
        });

        final List<Task<?>> tasks = new ArrayList<>();
        tasks.add(task1);
        tasks.add(task2);
        tasks.add(task3);

        try {
            // parallel call both task, and wait for both tasks done
            Task.whenAll(tasks).continueWith(new Continuation<Void, Object>() {
                @Override
                public Object then(Task<Void> task) throws Exception {
                    // all tasks are completed with one error
                    assertTrue(task.isCompleted());
                    assertTrue(task.isFaulted());
                    assertTrue(!task.isCancelled());

                    assertTrue(task.getError() instanceof FileNotFoundException);
                    return null;
                }
            }).waitForCompletion();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * execute 3 parallel tasks with 2 failed.
     * get each error of the failed tasks.
     */
    public void testParallelTask4() {
        Task<Integer> task1 = Task.callInBackground(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return 10086;
            }
        });

        Task<Integer> task2 = Task.callInBackground(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                throw new FileNotFoundException("404");
            }
        });

        Task<Integer> task3 = Task.callInBackground(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                throw new FileNotFoundException("302");
            }
        });

        final List<Task<?>> tasks = new ArrayList<>();
        tasks.add(task1);
        tasks.add(task2);
        tasks.add(task3);

        try {
            // parallel call both task, and wait for both tasks done
            Task.whenAll(tasks).continueWith(new Continuation<Void, Object>() {
                @Override
                public Object then(Task<Void> task) throws Exception {
                    // all tasks are completed with one error
                    assertTrue(task.isCompleted());
                    assertTrue(task.isFaulted());
                    assertTrue(!task.isCancelled());

                    assertTrue(task.getError() instanceof AggregateException);
                    AggregateException aggregateException = (AggregateException) task.getError();
                    List<Throwable> errors = aggregateException.getInnerThrowables();
                    assertEquals(2, errors.size());

                    for (int i = 0; i < errors.size(); i++) {
                        Throwable throwable = errors.get(i);
                        assertTrue(throwable instanceof  FileNotFoundException);
                    }
                    return null;
                }
            }).waitForCompletion();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * looper task
     * {@link Task#continueWhile(Callable, Continuation)}
     */
    public void testLooperTask() {
        final AtomicInteger count = new AtomicInteger(0);

        try {
            Task.forResult(null).continueWhile(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return count.get() < 10;
                }
            }, new Continuation<Void, Task<Void>>() {
                @Override
                public Task<Void> then(Task<Void> task) throws Exception {
                    count.incrementAndGet();
                    return null;
                }
            }).continueWith(new Continuation<Void, Object>() {
                @Override
                public Object then(Task<Void> task) throws Exception {
                    assertEquals(10, count.get());
                    return null;
                }
            }).waitForCompletion();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * cancel task
     * pls take care that canceling a async task need to sync multi-thread,
     * which is easily causing dead lock
     *
     * {@link TaskCompletionSource}
     * {@link CancellationTokenSource}
     * also see {@link BoltsApiTest#testTaskWrapper}
     */
    public void testCancelTask() {
        final CancellationTokenSource cts = new CancellationTokenSource();
        final TaskCompletionSource<String> tcs = new TaskCompletionSource<>();

        Task.callInBackground(new Callable<String>() {
            @Override
            public String call() throws Exception {
                int i = 0;

                while (!cts.isCancellationRequested()) {
                    Thread.sleep(1000);
                    Log.i(TAG, "[testCancelTask] i =" + String.valueOf(++i));
                }

                Log.i(TAG, "[testCancelTask] cts is canceled");
                tcs.setCancelled();
                return null;
            }
        });

        assertTrue(!cts.isCancellationRequested());
        assertTrue(!tcs.getTask().isCompleted());
        assertTrue(!tcs.getTask().isCancelled());

        try {
            Task.delay(3000).continueWith(new Continuation<Void, Void>() {
                @Override
                public Void then(Task<Void> task) throws Exception {
                    cts.cancel();
                    return null;
                }
            }).waitForCompletion();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            Task.delay(2000).continueWith(new Continuation<Void, Void>() {
                @Override
                public Void then(Task<Void> task) throws Exception {
                    assertTrue(cts.isCancellationRequested());
                    assertTrue(tcs.getTask().isCompleted());
                    assertTrue(tcs.getTask().isCancelled());
                    assertEquals(tcs.getTask().getResult(), null);
                    return null;
                }
            }).waitForCompletion();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void testCancelTask2() {
        final CancellationTokenSource cts = new CancellationTokenSource();
        final TaskCompletionSource<String> tcs = new TaskCompletionSource<>();

        Task.callInBackground(new Callable<String>() {
            @Override
            public String call() throws Exception {
                int i = 0;
                CancellationToken ctsToken = cts.getToken();

                while (!ctsToken.isCancellationRequested()) {
                    Thread.sleep(1000);
                    Log.i(TAG, "[testCancelTask.task1] i =" + String.valueOf(++i));
                }

                Log.i(TAG, "[testCancelTask.task1] cts is canceled");
                tcs.setCancelled();
                return null;
            }
        });

        Task.callInBackground(new Callable<String>() {
            @Override
            public String call() throws Exception {
                int i = 10;
                CancellationToken ctsToken = cts.getToken();
                ctsToken.register(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "[testCancelTask.task2] do something when cts is canceled");
                    }
                });

                while (!ctsToken.isCancellationRequested()) {
                    Thread.sleep(1000);
                    Log.i(TAG, "[testCancelTask.task2] i =" + String.valueOf(--i));
                }

                Log.i(TAG, "[testCancelTask.task2] cts is canceled");
                tcs.setCancelled();
                return null;
            }
        });

        assertTrue(!cts.isCancellationRequested());
        assertTrue(!tcs.getTask().isCompleted());
        assertTrue(!tcs.getTask().isCancelled());

        try {
            Task.delay(3000).continueWith(new Continuation<Void, Void>() {
                @Override
                public Void then(Task<Void> task) throws Exception {
                    cts.cancel();
                    return null;
                }
            }).waitForCompletion();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            Task.delay(2000).continueWith(new Continuation<Void, Void>() {
                @Override
                public Void then(Task<Void> task) throws Exception {
                    assertTrue(cts.isCancellationRequested());
                    assertTrue(tcs.getTask().isCompleted());
                    assertTrue(tcs.getTask().isCancelled());
                    assertEquals(tcs.getTask().getResult(), null);
                    return null;
                }
            }).waitForCompletion();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void testMutableVariable() {
        final Capture<Integer> integerCapture = new Capture<>(0);

        Task<Void> task1 = Task.callInBackground(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                Log.i(TAG, "[testMutableVariable] execute task 1");
                integerCapture.set(integerCapture.get() + 1);
                return null;
            }
        });
        Task<Void> task2 = Task.callInBackground(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                Log.i(TAG, "[testMutableVariable] execute task 2");
                integerCapture.set(integerCapture.get() - 1);
                return null;
            }
        });
        Task<Void> task3 = Task.callInBackground(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                Log.i(TAG, "[testMutableVariable] execute task 3");
                integerCapture.set(integerCapture.get() + 1);
                return null;
            }
        });

        final List<Task<?>> tasks = new ArrayList<>();
        tasks.add(task1);
        tasks.add(task2);
        tasks.add(task3);

        Task.whenAll(tasks).continueWith(new Continuation<Void, Object>() {
            @Override
            public Object then(Task<Void> task) throws Exception {
                assertEquals(integerCapture.get().intValue(), 1);
                return null;
            }
        });
    }


    // special apis

    /**
     * {@link Task#delay(long)}
     */
    public void testDelayTask() {
        final long millis = System.currentTimeMillis();
        try {
            Task.delay(3000).continueWith(new Continuation<Void, Object>() {
                @Override
                public Object then(Task<Void> task) throws Exception {
                    assertTrue((System.currentTimeMillis() - millis) >= 3000);
                    return null;
                }
            }).waitForCompletion();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * create task from existing result
     * {@link Task#forResult(Object)}
     * {@link Task#forError(Exception)}
     */
    public void testCreateTaskForResult() {
        Task<Integer> integerTask = Task.forResult(10086);
        assertTrue(integerTask.getResult() == 10086);

        Task<Object> failTask = Task.forError(new FileNotFoundException("404"));
        assertTrue(failTask.getError() instanceof FileNotFoundException);

        Task<Integer> canceledTask = Task.<Integer>cancelled();
        assertTrue(canceledTask.isCancelled());
    }

    /**
     * {@link Task#cast()}
     */
    public void testCastTask() {
        Task<Integer> task = Task.call(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return 10086;
            }
        });

        try {
            task.waitForCompletion();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Task<?> supperTask = task;
        assertNotNull(supperTask);

        Task<Integer> childTask = supperTask.cast();
        assertTrue(childTask.getResult() == 10086);
    }

    /**
     * {@link Task#makeVoid()}
     */
    public void testMakeVoidTask() {
        Task<Integer> task = Task.call(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return 10086;
            }
        });

        Task<Void> voidTask = task.makeVoid();
        assertTrue(voidTask.getResult() == null);
    }

    /**
     * isolating the Task's completion mechanisms from the consumer.
     * {@link TaskCompletionSource}
     * or {@link bolts.Task.TaskCompletionSource}
     */
    public void testTaskWrapper() {
        TaskCompletionSource<Void> tc = new TaskCompletionSource<>();
        Task<Void> task = tc.getTask();
        assertTrue(!task.isCompleted());
        assertTrue(!task.isFaulted());
        assertTrue(!task.isCancelled());

        tc.setCancelled();
        assertTrue(task.isCompleted());
        assertTrue(!task.isFaulted());
        assertTrue(task.isCancelled());

        tc = new TaskCompletionSource<>();
        task = tc.getTask();
        tc.setError(new FileNotFoundException("404"));
        assertTrue(task.isCompleted());
        assertTrue(task.isFaulted());
        assertTrue(!task.isCancelled());

        tc = new TaskCompletionSource<>();
        task = tc.getTask();
        tc.setResult(null);
        assertTrue(task.isCompleted());
        assertTrue(!task.isFaulted());
        assertTrue(!task.isCancelled());
    }
}
