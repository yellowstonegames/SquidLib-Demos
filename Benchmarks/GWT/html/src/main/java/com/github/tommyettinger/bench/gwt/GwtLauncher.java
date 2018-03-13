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
 * Benchmark for several different PRNG algorithms on GWT. Code mostly taken from
 * <a href="https://github.com/sjrd/gwt-sha512-benchmark">Sébastien Doeraene's SHA-512 benchmark for GWT</a>,
 * which is itself a port of
 * <a href="https://github.com/ARMmbed/mbedtls/blob/bfafadb45daf8d2114e3109e2f9021fc72ee36bb/library/sha512.c">an Apache-licensed SHA-512 benchmark in C</a>;
 * all the SHA code has been removed to just get a good basis for benchmarking. There was once a Benchmark class
 * included in GWT, but Google inexplicably removed it. If there's a better way to build a benchmark, I'd love to know.
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
        addIntTest(vp, new Zig32RNG(123456789, 987654321), "Zig32RNG");
        addIntTest(vp, new LightRNG(123456L), "LightRNG");
        addIntTest(vp, new RandomXS128(123456789L, 987654321L), "RandomXS128");
        addLongTest(vp, new Zig32RNG(123456789, 987654321), "Zig32RNG");
        addLongTest(vp, new LightRNG(123456L), "LightRNG");
        addLongTest(vp, new RandomXS128(123456789L, 987654321L), "RandomXS128");

        //addIntTest(vp, new Light32RNG(2132132130, 2123456789), "Light32RNG");
        //addLongTest(vp, new Light32RNG(2132132130, 2123456789), "Light32RNG");

        RootPanel.get().sinkEvents(Event.ONCLICK);
        RootPanel.get("embed-html").add(vp);
    }

    /**
     * Can be used to compare the three ints produced for each generator's nextInt() button with what a desktop JDK will
     * produce given the same seeds. Light32RNG doesn't produce consistent results between GWT and desktop, so it isn't
     * included in the GWT build (it's slower than Zig32RNG anyway).
     * @param args disregarded
     */
    public static void main(String[] args)
    {
        Zig32RNG a = new Zig32RNG(123456789, 987654321);
        Light32RNG b = new Light32RNG(2132132130, 2123456789);
        LightRNG c = new LightRNG(123456L);
        RandomXS128 d = new RandomXS128(123456789L, 987654321L);

        System.out.println("Zig32RNG " + a.nextInt());
        System.out.println("Light32RNG " + b.nextInt());
        System.out.println("LightRNG " + c.nextInt());
        System.out.println("RandomXS128 " + d.nextInt());

        System.out.println("Zig32RNG " + a.nextInt());
        System.out.println("Light32RNG " + b.nextInt());
        System.out.println("LightRNG " + c.nextInt());
        System.out.println("RandomXS128 " + d.nextInt());

        System.out.println("Zig32RNG " + a.nextInt());
        System.out.println("Light32RNG " + b.nextInt());
        System.out.println("LightRNG " + c.nextInt());
        System.out.println("RandomXS128 " + d.nextInt());
    }

    private void addIntTest(final VerticalPanel vp, final Zig32RNG rs, final String name)
    {
        final PushButton runBenchButton = new PushButton(name + ".nextInt(), " + rs.nextInt() + ", " + rs.nextInt() + ", " + rs.nextInt());
        final TextBox resultLabel = new TextBox();
        resultLabel.setReadOnly(false);
        resultLabel.setText("Not run yet");
        resultLabel.setPixelSize(500, 22);
        runBenchButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                resultLabel.setText("Running ...");
                runBenchButton.setEnabled(false);

                String benchmarkResult = reportInt(rs);

                resultLabel.setText(benchmarkResult);
                runBenchButton.setEnabled(true);
            }
        });
        runBenchButton.setEnabled(true);
        vp.add(runBenchButton);
        vp.add(resultLabel);
    }

    private void addIntTest(final VerticalPanel vp, final Light32RNG rs, final String name)
    {
        final PushButton runBenchButton = new PushButton(name + ".nextInt(), " + rs.nextInt() + ", " + rs.nextInt() + ", " + rs.nextInt());
        final TextBox resultLabel = new TextBox();
        resultLabel.setReadOnly(false);
        resultLabel.setText("Not run yet");
        resultLabel.setPixelSize(500, 22);
        runBenchButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                resultLabel.setText("Running ...");
                runBenchButton.setEnabled(false);

                String benchmarkResult = reportInt(rs);

                resultLabel.setText(benchmarkResult);
                runBenchButton.setEnabled(true);
            }
        });
        runBenchButton.setEnabled(true);
        vp.add(runBenchButton);
        vp.add(resultLabel);
    }

    private void addIntTest(final VerticalPanel vp, final LightRNG rs, final String name)
    {
        final PushButton runBenchButton = new PushButton(name + ".nextInt(), " + rs.nextInt() + ", " + rs.nextInt() + ", " + rs.nextInt());
        final TextBox resultLabel = new TextBox();
        resultLabel.setReadOnly(false);
        resultLabel.setText("Not run yet");
        resultLabel.setPixelSize(500, 22);
        runBenchButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                resultLabel.setText("Running ...");
                runBenchButton.setEnabled(false);

                String benchmarkResult = reportInt(rs);

                resultLabel.setText(benchmarkResult);
                runBenchButton.setEnabled(true);
            }
        });
        runBenchButton.setEnabled(true);
        vp.add(runBenchButton);
        vp.add(resultLabel);
    }

    private void addIntTest(final VerticalPanel vp, final RandomXS128 rs, final String name)
    {
        final PushButton runBenchButton = new PushButton(name + ".nextInt(), " + rs.nextInt() + ", " + rs.nextInt() + ", " + rs.nextInt());
        final TextBox resultLabel = new TextBox();
        resultLabel.setReadOnly(false);
        resultLabel.setText("Not run yet");
        resultLabel.setPixelSize(500, 22);
        runBenchButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                resultLabel.setText("Running ...");
                runBenchButton.setEnabled(false);

                String benchmarkResult = reportInt(rs);

                resultLabel.setText(benchmarkResult);
                runBenchButton.setEnabled(true);
            }
        });
        runBenchButton.setEnabled(true);
        vp.add(runBenchButton);
        vp.add(resultLabel);
    }

    private void addLongTest(final VerticalPanel vp, final Zig32RNG rs, final String name)
    {
        final PushButton runBenchButton = new PushButton(name + ".nextLong()");
        final TextBox resultLabel = new TextBox();
        resultLabel.setReadOnly(false);
        resultLabel.setText("Not run yet");
        resultLabel.setPixelSize(500, 22);
        runBenchButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                resultLabel.setText("Running ...");
                runBenchButton.setEnabled(false);

                String benchmarkResult = reportLong(rs);

                resultLabel.setText(benchmarkResult);
                runBenchButton.setEnabled(true);
            }
        });
        runBenchButton.setEnabled(true);
        vp.add(runBenchButton);
        vp.add(resultLabel);
    }

    private void addLongTest(final VerticalPanel vp, final Light32RNG rs, final String name)
    {
        final PushButton runBenchButton = new PushButton(name + ".nextLong()");
        final TextBox resultLabel = new TextBox();
        resultLabel.setReadOnly(false);
        resultLabel.setText("Not run yet");
        resultLabel.setPixelSize(500, 22);
        runBenchButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                resultLabel.setText("Running ...");
                runBenchButton.setEnabled(false);

                String benchmarkResult = reportLong(rs);

                resultLabel.setText(benchmarkResult);
                runBenchButton.setEnabled(true);
            }
        });
        runBenchButton.setEnabled(true);
        vp.add(runBenchButton);
        vp.add(resultLabel);
    }

    private void addLongTest(final VerticalPanel vp, final LightRNG rs, final String name)
    {
        final PushButton runBenchButton = new PushButton(name + ".nextLong()");
        final TextBox resultLabel = new TextBox();
        resultLabel.setReadOnly(false);
        resultLabel.setText("Not run yet");
        resultLabel.setPixelSize(500, 22);
        runBenchButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                resultLabel.setText("Running ...");
                runBenchButton.setEnabled(false);

                String benchmarkResult = reportLong(rs);

                resultLabel.setText(benchmarkResult);
                runBenchButton.setEnabled(true);
            }
        });
        runBenchButton.setEnabled(true);
        vp.add(runBenchButton);
        vp.add(resultLabel);
    }

    private void addLongTest(final VerticalPanel vp, final RandomXS128 rs, final String name)
    {
        final PushButton runBenchButton = new PushButton(name + ".nextLong()");
        final TextBox resultLabel = new TextBox();
        resultLabel.setReadOnly(false);
        resultLabel.setText("Not run yet");
        resultLabel.setPixelSize(500, 22);
        runBenchButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                resultLabel.setText("Running ...");
                runBenchButton.setEnabled(false);

                String benchmarkResult = reportLong(rs);

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
    private String runIntBenchmark(Zig32RNG rs, long timeMinimum, int runsMinimum) {
        int runs = 0;
        IntVLA samples = new IntVLA();
        long startTime, endTime, stopTime;
        stopTime = System.currentTimeMillis() + timeMinimum;
        int res = 0;
        do {
            startTime = System.currentTimeMillis();
            res ^= runInt(rs);
            endTime = System.currentTimeMillis();
            samples.add((int) (endTime - startTime));
        } while (++runs < runsMinimum || endTime < stopTime);

        return res + "; " + runs + " runs; " + meanAndSEM(samples);
    }
    /** Run the benchmark the specified number of milliseconds and return
     *  the mean execution time and SEM in milliseconds.
     */
    private String runIntBenchmark(Light32RNG rs, long timeMinimum, int runsMinimum) {
        int runs = 0;
        IntVLA samples = new IntVLA();
        long startTime, endTime, stopTime;
        stopTime = System.currentTimeMillis() + timeMinimum;
        int res = 0;
        do {
            startTime = System.currentTimeMillis();
            res ^= runInt(rs);
            endTime = System.currentTimeMillis();
            samples.add((int) (endTime - startTime));
        } while (++runs < runsMinimum || endTime < stopTime);

        return res + "; " + runs + " runs; " + meanAndSEM(samples);
    }
    /** Run the benchmark the specified number of milliseconds and return
     *  the mean execution time and SEM in milliseconds.
     */
    private String runIntBenchmark(LightRNG rs, long timeMinimum, int runsMinimum) {
        int runs = 0;
        IntVLA samples = new IntVLA();
        long startTime, endTime, stopTime;
        stopTime = System.currentTimeMillis() + timeMinimum;
        int res = 0;
        do {
            startTime = System.currentTimeMillis();
            res ^= runInt(rs);
            endTime = System.currentTimeMillis();
            samples.add((int) (endTime - startTime));
        } while (++runs < runsMinimum || endTime < stopTime);

        return res + "; " + runs + " runs; " + meanAndSEM(samples);
    }
    /** Run the benchmark the specified number of milliseconds and return
     *  the mean execution time and SEM in milliseconds.
     */
    private String runIntBenchmark(RandomXS128 rs, long timeMinimum, int runsMinimum) {
        int runs = 0;
        IntVLA samples = new IntVLA();
        long startTime, endTime, stopTime;
        stopTime = System.currentTimeMillis() + timeMinimum;
        int res = 0;
        do {
            startTime = System.currentTimeMillis();
            res ^= runInt(rs);
            endTime = System.currentTimeMillis();
            samples.add((int) (endTime - startTime));
        } while (++runs < runsMinimum || endTime < stopTime);

        return res + "; " + runs + " runs; " + meanAndSEM(samples);
    }

    /** Run the benchmark the specified number of milliseconds and return
     *  the mean execution time and SEM in milliseconds.
     */
    private String runLongBenchmark(Zig32RNG rs, long timeMinimum, int runsMinimum) {
        int runs = 0;
        IntVLA samples = new IntVLA();
        long startTime, endTime, stopTime;
        stopTime = System.currentTimeMillis() + timeMinimum;
        int res = 0;
        do {
            startTime = System.currentTimeMillis();
            res ^= runLong(rs);
            endTime = System.currentTimeMillis();
            samples.add((int) (endTime - startTime));
        } while (++runs < runsMinimum || endTime < stopTime);

        return res + "; " + runs + " runs; " + meanAndSEM(samples);
    }
    /** Run the benchmark the specified number of milliseconds and return
     *  the mean execution time and SEM in milliseconds.
     */
    private String runLongBenchmark(Light32RNG rs, long timeMinimum, int runsMinimum) {
        int runs = 0;
        IntVLA samples = new IntVLA();
        long startTime, endTime, stopTime;
        stopTime = System.currentTimeMillis() + timeMinimum;
        int res = 0;
        do {
            startTime = System.currentTimeMillis();
            res ^= runLong(rs);
            endTime = System.currentTimeMillis();
            samples.add((int) (endTime - startTime));
        } while (++runs < runsMinimum || endTime < stopTime);

        return res + "; " + runs + " runs; " + meanAndSEM(samples);
    }
    /** Run the benchmark the specified number of milliseconds and return
     *  the mean execution time and SEM in milliseconds.
     */
    private String runLongBenchmark(LightRNG rs, long timeMinimum, int runsMinimum) {
        int runs = 0;
        IntVLA samples = new IntVLA();
        long startTime, endTime, stopTime;
        stopTime = System.currentTimeMillis() + timeMinimum;
        int res = 0;
        do {
            startTime = System.currentTimeMillis();
            res ^= runLong(rs);
            endTime = System.currentTimeMillis();
            samples.add((int) (endTime - startTime));
        } while (++runs < runsMinimum || endTime < stopTime);

        return res + "; " + runs + " runs; " + meanAndSEM(samples);
    }
    /** Run the benchmark the specified number of milliseconds and return
     *  the mean execution time and SEM in milliseconds.
     */
    private String runLongBenchmark(RandomXS128 rs, long timeMinimum, int runsMinimum) {
        int runs = 0;
        IntVLA samples = new IntVLA();
        long startTime, endTime, stopTime;
        stopTime = System.currentTimeMillis() + timeMinimum;
        int res = 0;
        do {
            startTime = System.currentTimeMillis();
            res ^= runLong(rs);
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

    private String reportInt(Zig32RNG rs) {
        runIntBenchmark(rs, 100, 2); // warm up
        return runIntBenchmark(rs,2000, 5);
    }

    private String reportInt(Light32RNG rs) {
        runIntBenchmark(rs, 100, 2); // warm up
        return runIntBenchmark(rs,2000, 5);
    }

    private String reportInt(LightRNG rs) {
        runIntBenchmark(rs, 100, 2); // warm up
        return runIntBenchmark(rs,2000, 5);
    }

    private String reportInt(RandomXS128 rs) {
        runIntBenchmark(rs, 100, 2); // warm up
        return runIntBenchmark(rs,2000, 5);
    }

    private int runInt(Zig32RNG rs) {
        int xor = 0;
        for (int i = 0; i < 10000; i++) {
            xor ^= rs.nextInt();
        }
        return xor;
    }

    private int runInt(Light32RNG rs) {
        int xor = 0;
        for (int i = 0; i < 10000; i++) {
            xor ^= rs.nextInt();
        }
        return xor;
    }

    private int runInt(LightRNG rs) {
        int xor = 0;
        for (int i = 0; i < 10000; i++) {
            xor ^= rs.nextInt();
        }
        return xor;
    }
    private int runInt(RandomXS128 rs) {
        int xor = 0;
        for (int i = 0; i < 10000; i++) {
            xor ^= rs.nextInt();
        }
        return xor;
    }

    private String reportLong(Zig32RNG rs) {
        runLongBenchmark(rs, 100, 2); // warm up
        return runLongBenchmark(rs,2000, 5);
    }

    private String reportLong(Light32RNG rs) {
        runLongBenchmark(rs, 100, 2); // warm up
        return runLongBenchmark(rs,2000, 5);
    }

    private String reportLong(LightRNG rs) {
        runLongBenchmark(rs, 100, 2); // warm up
        return runLongBenchmark(rs,2000, 5);
    }

    private String reportLong(RandomXS128 rs) {
        runLongBenchmark(rs, 100, 2); // warm up
        return runLongBenchmark(rs,2000, 5);
    }

    private int runLong(Zig32RNG rs) {
        int xor = 0;
        for (int i = 0; i < 10000; i++) {
            xor ^= rs.nextLong();
        }
        return xor;
    }

    private int runLong(Light32RNG rs) {
        int xor = 0;
        for (int i = 0; i < 10000; i++) {
            xor ^= rs.nextLong();
        }
        return xor;
    }

    private int runLong(LightRNG rs) {
        int xor = 0;
        for (int i = 0; i < 10000; i++) {
            xor ^= rs.nextLong();
        }
        return xor;
    }
    private int runLong(RandomXS128 rs) {
        int xor = 0;
        for (int i = 0; i < 10000; i++) {
            xor ^= rs.nextLong();
        }
        return xor;
    }
}