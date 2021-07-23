package org.fuyi.gdal.cad.core;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ExecutorTemplate {

    private static final int DEFAULT_CORE_SIZE = 9;
    private static final int DEFAULT_MAX_SIZE = 100;


    public static Executor executor = new ThreadPoolExecutor(DEFAULT_CORE_SIZE, DEFAULT_MAX_SIZE,
            0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());

}
