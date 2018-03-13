package com.github.tommyettinger.bench.gwt;

import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.badlogic.gdx.backends.gwt.GwtBareApp;
import com.badlogic.gdx.math.RandomXS128;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.*;
import squidpony.squidmath.IntVLA;
import squidpony.squidmath.Light32RNG;
import squidpony.squidmath.LightRNG;


/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class GwtLauncher extends GwtBareApp {

    @Override
    public GwtApplicationConfiguration getConfig() {
        return new GwtApplicationConfiguration(512, 512);
    }


    /**
     * This is the entry point method.
     */
    @Override
    public void start() {
        VerticalPanel vp = new VerticalPanel();
        vp.setPixelSize(512, 512);
        vp.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        addRandomness(vp, new Zig32RNG(123, 456), "Zig32RNG");
        addRandomness(vp, new Light32RNG(123, 123456789), "Light32RNG");
        addRandomness(vp, new LightRNG(123456L), "LightRNG");
        addRandomness(vp, new RandomXS128(123456789L, 987654321L), "RandomXS128");
        RootPanel.get().sinkEvents(Event.ONCLICK);
        RootPanel.get("embed-html").add(vp);
    }

    private void addRandomness(final VerticalPanel vp, final Zig32RNG rs, final String name)
    {
        final PushButton runBenchButton = new PushButton("Run " + name);
        final TextBox resultLabel = new TextBox();
        resultLabel.setReadOnly(false);
        resultLabel.setText("Not run yet");
        resultLabel.setPixelSize(500, 22);
        runBenchButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                resultLabel.setText("Running ...");
                runBenchButton.setEnabled(false);

                String benchmarkResult = report(rs);

                resultLabel.setText(benchmarkResult);
                runBenchButton.setEnabled(true);
            }
        });
        runBenchButton.setEnabled(true);
        vp.add(runBenchButton);
        vp.add(resultLabel);
    }

    private void addRandomness(final VerticalPanel vp, final Light32RNG rs, final String name)
    {
        final PushButton runBenchButton = new PushButton("Run " + name);
        final TextBox resultLabel = new TextBox();
        resultLabel.setReadOnly(false);
        resultLabel.setText("Not run yet");
        resultLabel.setPixelSize(500, 22);
        runBenchButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                resultLabel.setText("Running ...");
                runBenchButton.setEnabled(false);

                String benchmarkResult = report(rs);

                resultLabel.setText(benchmarkResult);
                runBenchButton.setEnabled(true);
            }
        });
        runBenchButton.setEnabled(true);
        vp.add(runBenchButton);
        vp.add(resultLabel);
    }

    private void addRandomness(final VerticalPanel vp, final LightRNG rs, final String name)
    {
        final PushButton runBenchButton = new PushButton("Run " + name);
        final TextBox resultLabel = new TextBox();
        resultLabel.setReadOnly(false);
        resultLabel.setText("Not run yet");
        resultLabel.setPixelSize(500, 22);
        runBenchButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                resultLabel.setText("Running ...");
                runBenchButton.setEnabled(false);

                String benchmarkResult = report(rs);

                resultLabel.setText(benchmarkResult);
                runBenchButton.setEnabled(true);
            }
        });
        runBenchButton.setEnabled(true);
        vp.add(runBenchButton);
        vp.add(resultLabel);
    }

    private void addRandomness(final VerticalPanel vp, final RandomXS128 rs, final String name)
    {
        final PushButton runBenchButton = new PushButton("Run " + name);
        final TextBox resultLabel = new TextBox();
        resultLabel.setReadOnly(false);
        resultLabel.setText("Not run yet");
        resultLabel.setPixelSize(500, 22);
        runBenchButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                resultLabel.setText("Running ...");
                runBenchButton.setEnabled(false);

                String benchmarkResult = report(rs);

                resultLabel.setText(benchmarkResult);
                runBenchButton.setEnabled(true);
            }
        });
        runBenchButton.setEnabled(true);
        vp.add(runBenchButton);
        vp.add(resultLabel);
    }
    
    private static String meanAndSEMString(double mean, double sem)
    {
        return mean + " ms +- " + sem + " ms";
    }
    /** Run the benchmark the specified number of milliseconds and return
     *  the mean execution time and SEM in milliseconds.
     */
    private String runBenchmark(Zig32RNG rs, long timeMinimum, int runsMinimum) {
        int runs = 0;
        IntVLA samples = new IntVLA();
        long startTime, endTime, stopTime;
        stopTime = System.currentTimeMillis() + timeMinimum;
        int res = 0;
        do {
            startTime = System.currentTimeMillis();
            res ^= run(rs);
            endTime = System.currentTimeMillis();
            samples.add((int) (endTime - startTime));
        } while (++runs < runsMinimum || endTime < stopTime);

        return res + "; " + runs + " runs; " + meanAndSEM(samples);
    }
    /** Run the benchmark the specified number of milliseconds and return
     *  the mean execution time and SEM in milliseconds.
     */
    private String runBenchmark(Light32RNG rs, long timeMinimum, int runsMinimum) {
        int runs = 0;
        IntVLA samples = new IntVLA();
        long startTime, endTime, stopTime;
        stopTime = System.currentTimeMillis() + timeMinimum;
        int res = 0;
        do {
            startTime = System.currentTimeMillis();
            res ^= run(rs);
            endTime = System.currentTimeMillis();
            samples.add((int) (endTime - startTime));
        } while (++runs < runsMinimum || endTime < stopTime);

        return res + "; " + runs + " runs; " + meanAndSEM(samples);
    }
    /** Run the benchmark the specified number of milliseconds and return
     *  the mean execution time and SEM in milliseconds.
     */
    private String runBenchmark(LightRNG rs, long timeMinimum, int runsMinimum) {
        int runs = 0;
        IntVLA samples = new IntVLA();
        long startTime, endTime, stopTime;
        stopTime = System.currentTimeMillis() + timeMinimum;
        int res = 0;
        do {
            startTime = System.currentTimeMillis();
            res ^= run(rs);
            endTime = System.currentTimeMillis();
            samples.add((int) (endTime - startTime));
        } while (++runs < runsMinimum || endTime < stopTime);

        return res + "; " + runs + " runs; " + meanAndSEM(samples);
    }
    /** Run the benchmark the specified number of milliseconds and return
     *  the mean execution time and SEM in milliseconds.
     */
    private String runBenchmark(RandomXS128 rs, long timeMinimum, int runsMinimum) {
        int runs = 0;
        IntVLA samples = new IntVLA();
        long startTime, endTime, stopTime;
        stopTime = System.currentTimeMillis() + timeMinimum;
        int res = 0;
        do {
            startTime = System.currentTimeMillis();
            res ^= run(rs);
            endTime = System.currentTimeMillis();
            samples.add((int) (endTime - startTime));
        } while (++runs < runsMinimum || endTime < stopTime);

        return res + "; " + runs + " runs; " + meanAndSEM(samples);
    }

    private static String meanAndSEM(IntVLA samples) {
        final int sz = samples.size;
        int sum = 0;
        for (int i = 0; i < sz; i++) {
            sum += samples.get(i);
        }
        double mean = sum / (double) sz;
        double sem = standardErrorOfTheMean(samples, mean);
        return meanAndSEMString(mean, sem);
    }

    private static double standardErrorOfTheMean(IntVLA samples,
                                                 double mean) {
        final int sz = samples.size;
        double n = (double) sz;
        double sumSqs = 0f;
        double t;
        for (int i = 0; i < sz; i++) {
            t = samples.get(i) - mean;
            sumSqs += t * t;
        }
        return Math.sqrt(sumSqs / (n * (n - 1)));
    }

    private String report(Zig32RNG rs) {
        runBenchmark(rs, 100, 2); // warm up
        return runBenchmark(rs,2000, 5);
    }

    private String report(Light32RNG rs) {
        runBenchmark(rs, 100, 2); // warm up
        return runBenchmark(rs,2000, 5);
    }

    private String report(LightRNG rs) {
        runBenchmark(rs, 100, 2); // warm up
        return runBenchmark(rs,2000, 5);
    }

    private String report(RandomXS128 rs) {
        runBenchmark(rs, 100, 2); // warm up
        return runBenchmark(rs,2000, 5);
    }

    private int run(Zig32RNG rs) {
        int xor = 0;
        for (int i = 0; i < 10000; i++) {
            xor ^= rs.nextInt();
        }
        return xor;
    }

    private int run(Light32RNG rs) {
        int xor = 0;
        for (int i = 0; i < 10000; i++) {
            xor ^= rs.nextInt();
        }
        return xor;
    }

    private int run(LightRNG rs) {
        int xor = 0;
        for (int i = 0; i < 10000; i++) {
            xor ^= rs.nextInt();
        }
        return xor;
    }
    private int run(RandomXS128 rs) {
        int xor = 0;
        for (int i = 0; i < 10000; i++) {
            xor ^= rs.nextInt();
        }
        return xor;
    }
}