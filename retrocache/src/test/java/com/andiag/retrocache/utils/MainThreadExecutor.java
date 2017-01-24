package com.andiag.retrocache.utils;

import android.support.annotation.NonNull;

import java.util.concurrent.Executor;

/**
 * Created by Iago on 12/01/2017.
 */
public class MainThreadExecutor implements Executor {
    @Override
    public void execute(@NonNull Runnable command) {
        command.run();
    }
}
