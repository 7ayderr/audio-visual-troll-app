package com.system.weathermonitor;

import android.app.Application;

/**
 * Application entry — holds the process-wide {@link ExecutionEngine}.
 */
public class WeatherMonitorApp extends Application {

    private ExecutionEngine executionEngine;

    @Override
    public void onCreate() {
        super.onCreate();
        executionEngine = new ExecutionEngine(this);
    }

    public ExecutionEngine getExecutionEngine() {
        return executionEngine;
    }

    public static WeatherMonitorApp from(Application app) {
        return (WeatherMonitorApp) app.getApplicationContext();
    }
}
