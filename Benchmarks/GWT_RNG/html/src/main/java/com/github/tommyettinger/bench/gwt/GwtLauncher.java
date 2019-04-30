package com.github.tommyettinger.bench.gwt;

import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.badlogic.gdx.backends.gwt.GwtBareApp;
import com.badlogic.gdx.math.RandomXS128;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.*;
import squidpony.squidmath.*;


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
        return new GwtApplicationConfiguration(600, 650);
    }


    /**
     * This is the entry point method.
     */
    @Override
    public void start() {
        VerticalPanel vp = new VerticalPanel();
        vp.setPixelSize(600, 780);
        vp.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

        vp.add(new Label("Click any button and wait a few seconds to see:"));
        vp.add(new Label(" * the RNG's calculated result, which requires 10,000 numbers to be generated,"));
        vp.add(new Label(" * the time it took in milliseconds to generate those 10,000 numbers,"));
        vp.add(new Label(" * the margin of error for the calculation, in ms as an offset from the time before it."));

        addIntTest(vp, new Lathe32RNG(123456789, 987654321), "Lathe32RNG");
        addIntTest(vp, new Starfish32RNG(123456789, 987654321), "Starfish32RNG");
        addIntTest(vp, new Otter32RNG(123456789, 987654321), "Otter32RNG");
        addIntTest(vp, new Lobster32RNG(123456789, 987654321), "Lobster32RNG");
        addIntTest(vp, new Cake32RNG(123456789, 987654321, 1), "Cake32RNG");
        addIntTest(vp, new Mover32RNG(1), "Mover32RNG");
        addIntTest(vp, new XoshiroStarStar32RNG(123456789), "XoshiroStarStar32RNG");
        addIntTest(vp, new XoshiroStarPhi32RNG(123456789), "XoshiroStarPhi32RNG");
        addIntTest(vp, new XoshiroAra32RNG(123456789), "XoshiroAra32RNG");
        addIntTest(vp, new LightRNG(123456L), "LightRNG");
        addIntTest(vp, new RandomXS128(123456789L, 987654321L), "RandomXS128");
        addIntTest(vp, new GWTRNG(123456789, 987654321), "GWTRNG");
        addIntTest(vp, new RNG(new Lathe32RNG(123456789, 987654321)), "Lathe32RNG wrapped in RNG");
        addIntTest(vp, new RNG(new LightRNG(123456L)), "LightRNG wrapped in RNG");
        addLongTest(vp, new Lathe32RNG(123456789, 987654321), "Lathe32RNG");
        addLongTest(vp, new Starfish32RNG(123456789, 987654321), "Starfish32RNG");
        addLongTest(vp, new Otter32RNG(123456789, 987654321), "Otter32RNG");
        addLongTest(vp, new Lobster32RNG(123456789, 987654321), "Lobster32RNG");
        addLongTest(vp, new Cake32RNG(123456789, 987654321, 1), "Cake32RNG");
        addLongTest(vp, new Mover32RNG(1), "Mover32RNG");
        addLongTest(vp, new XoshiroStarStar32RNG(123456789), "XoshiroStarStar32RNG");
        addLongTest(vp, new XoshiroStarPhi32RNG(123456789), "XoshiroStarPhi32RNG");
        addLongTest(vp, new XoshiroAra32RNG(123456789), "XoshiroAra32RNG");
        addLongTest(vp, new LightRNG(123456L), "LightRNG");
        addLongTest(vp, new RandomXS128(123456789L, 987654321L), "RandomXS128");
        addLongTest(vp, new GWTRNG(123456789, 987654321), "GWTRNG");
        addLongTest(vp, new RNG(new Lathe32RNG(123456789, 987654321)), "Lathe32RNG wrapped in RNG");
        addLongTest(vp, new RNG(new LightRNG(123456L)), "LightRNG wrapped in RNG");
        addBoundedIntTest(vp, new GWTRNG(123456789, 987654321), "GWTRNG");
        addBoundedIntTest(vp, new RNG(new Lathe32RNG(123456789, 987654321)), "Lathe32RNG wrapped in RNG");
        addBoundedIntTest(vp, new RNG(new LightRNG(123456L)), "LightRNG wrapped in RNG");

        vp.add(new Label("Note: Clicking multiple 'wrapped in RNG' buttons will slow down all such buttons."));
        vp.add(new Label("This has something to do with multiple implementations of RandomnessSource being used,"));
        vp.add(new Label(" and GWT has a hard time optimizing polymorphic code like this."));
        vp.add(new Label("This may also affect GWTRNG, which does not use RandomnessSource in the same way."));
        vp.add(new Label("It's probably best to stick to one RandomnessSource per app for this polymorphism reason."));

        //addIntTest(vp, new Light32RNG(2132132130, 2123456789), "Light32RNG");
        //addLongTest(vp, new Light32RNG(2132132130, 2123456789), "Light32RNG");
        
        RootPanel.get().sinkEvents(Event.ONCLICK);
        RootPanel.get("embed-html").add(vp);
    }

    /**
     * Can be used to compare the three ints produced for each generator's nextInt() button with what a desktop JDK will
     * produce given the same seeds. Light32RNG doesn't produce consistent results between GWT and desktop, so it isn't
     * included in the GWT build (it's slower than Lathe32RNG anyway).
     * @param args disregarded
     */
    public static void main(String[] args)
    {
        Cake32RNG a = new Cake32RNG(123456789, 987654321, 1);
        Lathe32RNG b = new Lathe32RNG(123456789, 987654321);
        LightRNG c = new LightRNG(123456L);
        RandomXS128 d = new RandomXS128(123456789L, 987654321L);
        Lobster32RNG e = new Lobster32RNG(123456789, 987654321);
        Mover32RNG f = new Mover32RNG(1);

        System.out.println("Cake32RNG  " + a.nextInt());
        System.out.println("Lathe32RNG " + b.nextInt());
        System.out.println("LightRNG " + c.nextInt());
        System.out.println("RandomXS128 " + d.nextInt());
        System.out.println("Lobster32RNG " + e.nextInt());
        System.out.println("Mover32RNG " + f.nextInt());

        System.out.println("Cake32RNG  " + a.nextInt());
        System.out.println("Lathe32RNG " + b.nextInt());
        System.out.println("LightRNG " + c.nextInt());
        System.out.println("RandomXS128 " + d.nextInt());
        System.out.println("Lobster32RNG " + e.nextInt());
        System.out.println("Mover32RNG " + f.nextInt());

        System.out.println("Cake32RNG  " + a.nextInt());
        System.out.println("Lathe32RNG " + b.nextInt());
        System.out.println("LightRNG " + c.nextInt());
        System.out.println("RandomXS128 " + d.nextInt());
        System.out.println("Lobster32RNG " + e.nextInt());
        System.out.println("Mover32RNG " + f.nextInt());

        
    }

    private void addIntTest(final VerticalPanel vp, final ThrustAlt32RNG rs, final String name)
    {
        final PushButton runBenchButton = new PushButton(name + ".nextInt(), " + rs.nextInt() + ", " + rs.nextInt() + ", " + rs.nextInt());
        final TextBox resultLabel = new TextBox();
        resultLabel.setReadOnly(false);
        resultLabel.setText("Not run yet");
        resultLabel.setPixelSize(560, 22);
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
    private void addIntTest(final VerticalPanel vp, final Lathe32RNG rs, final String name)
    {
        final PushButton runBenchButton = new PushButton(name + ".nextInt(), " + rs.nextInt() + ", " + rs.nextInt() + ", " + rs.nextInt());
        final TextBox resultLabel = new TextBox();
        resultLabel.setReadOnly(false);
        resultLabel.setText("Not run yet");
        resultLabel.setPixelSize(560, 22);
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
        resultLabel.setPixelSize(560, 22);
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
        resultLabel.setPixelSize(560, 22);
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

    private void addLongTest(final VerticalPanel vp, final ThrustAlt32RNG rs, final String name)
    {
        final PushButton runBenchButton = new PushButton(name + ".nextLong()");
        final TextBox resultLabel = new TextBox();
        resultLabel.setReadOnly(false);
        resultLabel.setText("Not run yet");
        resultLabel.setPixelSize(560, 22);
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

    private void addLongTest(final VerticalPanel vp, final Lathe32RNG rs, final String name)
    {
        final PushButton runBenchButton = new PushButton(name + ".nextLong()");
        final TextBox resultLabel = new TextBox();
        resultLabel.setReadOnly(false);
        resultLabel.setText("Not run yet");
        resultLabel.setPixelSize(560, 22);
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
        resultLabel.setPixelSize(560, 22);
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
        resultLabel.setPixelSize(560, 22);
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
    private String runIntBenchmark(ThrustAlt32RNG rs, long timeMinimum, int runsMinimum) {
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
    private String runIntBenchmark(Lathe32RNG rs, long timeMinimum, int runsMinimum) {
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
    private String runLongBenchmark(ThrustAlt32RNG rs, long timeMinimum, int runsMinimum) {
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
    private String runLongBenchmark(Lathe32RNG rs, long timeMinimum, int runsMinimum) {
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

    private String reportInt(ThrustAlt32RNG rs) {
        runIntBenchmark(rs, 100, 2); // warm up
        return runIntBenchmark(rs,2000, 5);
    }
    
    private String reportInt(Lathe32RNG rs) {
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

    private int runInt(ThrustAlt32RNG rs) {
        int xor = 0;
        for (int i = 0; i < 10000; i++) {
            xor ^= rs.nextInt();
        }
        return xor;
    }

    private int runInt(Lathe32RNG rs) {
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

    private String reportLong(ThrustAlt32RNG rs) {
        runLongBenchmark(rs, 100, 2); // warm up
        return runLongBenchmark(rs,2000, 5);
    }

    private String reportLong(Lathe32RNG rs) {
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

    private int runLong(ThrustAlt32RNG rs) {
        int xor = 0;
        for (int i = 0; i < 10000; i++) {
            xor ^= rs.nextLong();
        }
        return xor;
    }


    private int runLong(Lathe32RNG rs) {
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
    private void addIntTest(final VerticalPanel vp, final GWTRNG rs, final String name)
    {
        final PushButton runBenchButton = new PushButton(name + ".nextInt(), " + rs.nextInt() + ", " + rs.nextInt() + ", " + rs.nextInt());
        final TextBox resultLabel = new TextBox();
        resultLabel.setReadOnly(false);
        resultLabel.setText("Not run yet");
        resultLabel.setPixelSize(560, 22);
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

    private void addLongTest(final VerticalPanel vp, final GWTRNG rs, final String name)
    {
        final PushButton runBenchButton = new PushButton(name + ".nextLong()");
        final TextBox resultLabel = new TextBox();
        resultLabel.setReadOnly(false);
        resultLabel.setText("Not run yet");
        resultLabel.setPixelSize(560, 22);
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
    private void addBoundedIntTest(final VerticalPanel vp, final GWTRNG rs, final String name)
    {
        final PushButton runBenchButton = new PushButton(name + ".nextInt(100), " + rs.nextInt(100) + ", " + rs.nextInt(100) + ", " + rs.nextInt(100));
        final TextBox resultLabel = new TextBox();
        resultLabel.setReadOnly(false);
        resultLabel.setText("Not run yet");
        resultLabel.setPixelSize(560, 22);
        runBenchButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                resultLabel.setText("Running ...");
                runBenchButton.setEnabled(false);

                String benchmarkResult = reportIntBounded(rs);

                resultLabel.setText(benchmarkResult);
                runBenchButton.setEnabled(true);
            }
        });
        runBenchButton.setEnabled(true);
        vp.add(runBenchButton);
        vp.add(resultLabel);
    }


    private String runIntBenchmark(GWTRNG rs, long timeMinimum, int runsMinimum) {
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

    private String runLongBenchmark(GWTRNG rs, long timeMinimum, int runsMinimum) {
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
    private String runBoundedIntBenchmark(GWTRNG rs, long timeMinimum, int runsMinimum) {
        int runs = 0;
        IntVLA samples = new IntVLA();
        long startTime, endTime, stopTime;
        stopTime = System.currentTimeMillis() + timeMinimum;
        int res = 0;
        do {
            startTime = System.currentTimeMillis();
            res ^= runIntBounded(rs);
            endTime = System.currentTimeMillis();
            samples.add((int) (endTime - startTime));
        } while (++runs < runsMinimum || endTime < stopTime);

        return res + "; " + runs + " runs; " + meanAndSEM(samples);
    }


    private String reportInt(GWTRNG rs) {
        runIntBenchmark(rs, 100, 2); // warm up
        return runIntBenchmark(rs,2000, 5);
    }

    private int runInt(GWTRNG rs) {
        int xor = 0;
        for (int i = 0; i < 10000; i++) {
            xor ^= rs.nextInt();
        }
        return xor;
    }


    private String reportIntBounded(GWTRNG rs) {
        runBoundedIntBenchmark(rs, 100, 2); // warm up
        return runBoundedIntBenchmark(rs, 2000, 5);
    }

    private int runIntBounded(GWTRNG rs) {
        int xor = 0;
        for (int i = 0; i < 10000; i++) {
            xor ^= rs.nextInt(100);
        }
        return xor;
    }

    private String reportLong(GWTRNG rs) {
        runLongBenchmark(rs, 100, 2); // warm up
        return runLongBenchmark(rs,2000, 5);
    }

    private int runLong(GWTRNG rs) {
        int xor = 0;
        for (int i = 0; i < 10000; i++) {
            xor ^= rs.nextLong();
        }
        return xor;
    }

    private void addIntTest(final VerticalPanel vp, final Lobster32RNG rs, final String name)
    {
        final PushButton runBenchButton = new PushButton(name + ".nextInt(), " + rs.nextInt() + ", " + rs.nextInt() + ", " + rs.nextInt());
        final TextBox resultLabel = new TextBox();
        resultLabel.setReadOnly(false);
        resultLabel.setText("Not run yet");
        resultLabel.setPixelSize(560, 22);
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

    private void addLongTest(final VerticalPanel vp, final Lobster32RNG rs, final String name)
    {
        final PushButton runBenchButton = new PushButton(name + ".nextLong()");
        final TextBox resultLabel = new TextBox();
        resultLabel.setReadOnly(false);
        resultLabel.setText("Not run yet");
        resultLabel.setPixelSize(560, 22);
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
    private String runIntBenchmark(Lobster32RNG rs, long timeMinimum, int runsMinimum) {
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

    private String runLongBenchmark(Lobster32RNG rs, long timeMinimum, int runsMinimum) {
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

    private String reportInt(Lobster32RNG rs) {
        runIntBenchmark(rs, 100, 2); // warm up
        return runIntBenchmark(rs,2000, 5);
    }

    private int runInt(Lobster32RNG rs) {
        int xor = 0;
        for (int i = 0; i < 10000; i++) {
            xor ^= rs.nextInt();
        }
        return xor;
    }

    private String reportLong(Lobster32RNG rs) {
        runLongBenchmark(rs, 100, 2); // warm up
        return runLongBenchmark(rs,2000, 5);
    }

    private int runLong(Lobster32RNG rs) {
        int xor = 0;
        for (int i = 0; i < 10000; i++) {
            xor ^= rs.nextLong();
        }
        return xor;
    }








    private void addIntTest(final VerticalPanel vp, final Mover32RNG rs, final String name)
    {
        final PushButton runBenchButton = new PushButton(name + ".nextInt(), " + rs.nextInt() + ", " + rs.nextInt() + ", " + rs.nextInt());
        final TextBox resultLabel = new TextBox();
        resultLabel.setReadOnly(false);
        resultLabel.setText("Not run yet");
        resultLabel.setPixelSize(560, 22);
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

    private void addLongTest(final VerticalPanel vp, final Mover32RNG rs, final String name)
    {
        final PushButton runBenchButton = new PushButton(name + ".nextLong()");
        final TextBox resultLabel = new TextBox();
        resultLabel.setReadOnly(false);
        resultLabel.setText("Not run yet");
        resultLabel.setPixelSize(560, 22);
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
    private String runIntBenchmark(Mover32RNG rs, long timeMinimum, int runsMinimum) {
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

    private String runLongBenchmark(Mover32RNG rs, long timeMinimum, int runsMinimum) {
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

    private String reportInt(Mover32RNG rs) {
        runIntBenchmark(rs, 100, 2); // warm up
        return runIntBenchmark(rs,2000, 5);
    }

    private int runInt(Mover32RNG rs) {
        int xor = 0;
        for (int i = 0; i < 10000; i++) {
            xor ^= rs.nextInt();
        }
        return xor;
    }

    private String reportLong(Mover32RNG rs) {
        runLongBenchmark(rs, 100, 2); // warm up
        return runLongBenchmark(rs,2000, 5);
    }

    private int runLong(Mover32RNG rs) {
        int xor = 0;
        for (int i = 0; i < 10000; i++) {
            xor ^= rs.nextLong();
        }
        return xor;
    }

    private void addIntTest(final VerticalPanel vp, final Cake32RNG rs, final String name)
    {
        final PushButton runBenchButton = new PushButton(name + ".nextInt(), " + rs.nextInt() + ", " + rs.nextInt() + ", " + rs.nextInt());
        final TextBox resultLabel = new TextBox();
        resultLabel.setReadOnly(false);
        resultLabel.setText("Not run yet");
        resultLabel.setPixelSize(560, 22);
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

    private void addLongTest(final VerticalPanel vp, final Cake32RNG rs, final String name)
    {
        final PushButton runBenchButton = new PushButton(name + ".nextLong()");
        final TextBox resultLabel = new TextBox();
        resultLabel.setReadOnly(false);
        resultLabel.setText("Not run yet");
        resultLabel.setPixelSize(560, 22);
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
    private String runIntBenchmark(Cake32RNG rs, long timeMinimum, int runsMinimum) {
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

    private String runLongBenchmark(Cake32RNG rs, long timeMinimum, int runsMinimum) {
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

    private String reportInt(Cake32RNG rs) {
        runIntBenchmark(rs, 100, 2); // warm up
        return runIntBenchmark(rs,2000, 5);
    }

    private int runInt(Cake32RNG rs) {
        int xor = 0;
        for (int i = 0; i < 10000; i++) {
            xor ^= rs.nextInt();
        }
        return xor;
    }

    private String reportLong(Cake32RNG rs) {
        runLongBenchmark(rs, 100, 2); // warm up
        return runLongBenchmark(rs,2000, 5);
    }

    private int runLong(Cake32RNG rs) {
        int xor = 0;
        for (int i = 0; i < 10000; i++) {
            xor ^= rs.nextLong();
        }
        return xor;
    }








    private void addIntTest(final VerticalPanel vp, final XoshiroStarStar32RNG rs, final String name)
    {
        final PushButton runBenchButton = new PushButton(name + ".nextInt(), " + rs.nextInt() + ", " + rs.nextInt() + ", " + rs.nextInt());
        final TextBox resultLabel = new TextBox();
        resultLabel.setReadOnly(false);
        resultLabel.setText("Not run yet");
        resultLabel.setPixelSize(560, 22);
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

    private void addLongTest(final VerticalPanel vp, final XoshiroStarStar32RNG rs, final String name)
    {
        final PushButton runBenchButton = new PushButton(name + ".nextLong()");
        final TextBox resultLabel = new TextBox();
        resultLabel.setReadOnly(false);
        resultLabel.setText("Not run yet");
        resultLabel.setPixelSize(560, 22);
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
    private String runIntBenchmark(XoshiroStarStar32RNG rs, long timeMinimum, int runsMinimum) {
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

    private String runLongBenchmark(XoshiroStarStar32RNG rs, long timeMinimum, int runsMinimum) {
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

    private String reportInt(XoshiroStarStar32RNG rs) {
        runIntBenchmark(rs, 100, 2); // warm up
        return runIntBenchmark(rs,2000, 5);
    }

    private int runInt(XoshiroStarStar32RNG rs) {
        int xor = 0;
        for (int i = 0; i < 10000; i++) {
            xor ^= rs.nextInt();
        }
        return xor;
    }

    private String reportLong(XoshiroStarStar32RNG rs) {
        runLongBenchmark(rs, 100, 2); // warm up
        return runLongBenchmark(rs,2000, 5);
    }

    private int runLong(XoshiroStarStar32RNG rs) {
        int xor = 0;
        for (int i = 0; i < 10000; i++) {
            xor ^= rs.nextLong();
        }
        return xor;
    }


    private void addIntTest(final VerticalPanel vp, final XoshiroStarPhi32RNG rs, final String name)
    {
        final PushButton runBenchButton = new PushButton(name + ".nextInt(), " + rs.nextInt() + ", " + rs.nextInt() + ", " + rs.nextInt());
        final TextBox resultLabel = new TextBox();
        resultLabel.setReadOnly(false);
        resultLabel.setText("Not run yet");
        resultLabel.setPixelSize(560, 22);
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

    private void addLongTest(final VerticalPanel vp, final XoshiroStarPhi32RNG rs, final String name)
    {
        final PushButton runBenchButton = new PushButton(name + ".nextLong()");
        final TextBox resultLabel = new TextBox();
        resultLabel.setReadOnly(false);
        resultLabel.setText("Not run yet");
        resultLabel.setPixelSize(560, 22);
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
    private String runIntBenchmark(XoshiroStarPhi32RNG rs, long timeMinimum, int runsMinimum) {
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

    private String runLongBenchmark(XoshiroStarPhi32RNG rs, long timeMinimum, int runsMinimum) {
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

    private String reportInt(XoshiroStarPhi32RNG rs) {
        runIntBenchmark(rs, 100, 2); // warm up
        return runIntBenchmark(rs,2000, 5);
    }

    private int runInt(XoshiroStarPhi32RNG rs) {
        int xor = 0;
        for (int i = 0; i < 10000; i++) {
            xor ^= rs.nextInt();
        }
        return xor;
    }

    private String reportLong(XoshiroStarPhi32RNG rs) {
        runLongBenchmark(rs, 100, 2); // warm up
        return runLongBenchmark(rs,2000, 5);
    }

    private int runLong(XoshiroStarPhi32RNG rs) {
        int xor = 0;
        for (int i = 0; i < 10000; i++) {
            xor ^= rs.nextLong();
        }
        return xor;
    }

    private void addIntTest(final VerticalPanel vp, final Otter32RNG rs, final String name)
    {
        final PushButton runBenchButton = new PushButton(name + ".nextInt(), " + rs.nextInt() + ", " + rs.nextInt() + ", " + rs.nextInt());
        final TextBox resultLabel = new TextBox();
        resultLabel.setReadOnly(false);
        resultLabel.setText("Not run yet");
        resultLabel.setPixelSize(560, 22);
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

    private void addLongTest(final VerticalPanel vp, final Otter32RNG rs, final String name)
    {
        final PushButton runBenchButton = new PushButton(name + ".nextLong()");
        final TextBox resultLabel = new TextBox();
        resultLabel.setReadOnly(false);
        resultLabel.setText("Not run yet");
        resultLabel.setPixelSize(560, 22);
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
    private String runIntBenchmark(Otter32RNG rs, long timeMinimum, int runsMinimum) {
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

    private String runLongBenchmark(Otter32RNG rs, long timeMinimum, int runsMinimum) {
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

    private String reportInt(Otter32RNG rs) {
        runIntBenchmark(rs, 100, 2); // warm up
        return runIntBenchmark(rs,2000, 5);
    }

    private int runInt(Otter32RNG rs) {
        int xor = 0;
        for (int i = 0; i < 10000; i++) {
            xor ^= rs.nextInt();
        }
        return xor;
    }

    private String reportLong(Otter32RNG rs) {
        runLongBenchmark(rs, 100, 2); // warm up
        return runLongBenchmark(rs,2000, 5);
    }

    private int runLong(Otter32RNG rs) {
        int xor = 0;
        for (int i = 0; i < 10000; i++) {
            xor ^= rs.nextLong();
        }
        return xor;
    }

    private void addIntTest(final VerticalPanel vp, final XoshiroAra32RNG rs, final String name)
    {
        final PushButton runBenchButton = new PushButton(name + ".nextInt(), " + rs.nextInt() + ", " + rs.nextInt() + ", " + rs.nextInt());
        final TextBox resultLabel = new TextBox();
        resultLabel.setReadOnly(false);
        resultLabel.setText("Not run yet");
        resultLabel.setPixelSize(560, 22);
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

    private void addLongTest(final VerticalPanel vp, final XoshiroAra32RNG rs, final String name)
    {
        final PushButton runBenchButton = new PushButton(name + ".nextLong()");
        final TextBox resultLabel = new TextBox();
        resultLabel.setReadOnly(false);
        resultLabel.setText("Not run yet");
        resultLabel.setPixelSize(560, 22);
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
    private String runIntBenchmark(XoshiroAra32RNG rs, long timeMinimum, int runsMinimum) {
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

    private String runLongBenchmark(XoshiroAra32RNG rs, long timeMinimum, int runsMinimum) {
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

    private String reportInt(XoshiroAra32RNG rs) {
        runIntBenchmark(rs, 100, 2); // warm up
        return runIntBenchmark(rs,2000, 5);
    }

    private int runInt(XoshiroAra32RNG rs) {
        int xor = 0;
        for (int i = 0; i < 10000; i++) {
            xor ^= rs.nextInt();
        }
        return xor;
    }

    private String reportLong(XoshiroAra32RNG rs) {
        runLongBenchmark(rs, 100, 2); // warm up
        return runLongBenchmark(rs,2000, 5);
    }

    private int runLong(XoshiroAra32RNG rs) {
        int xor = 0;
        for (int i = 0; i < 10000; i++) {
            xor ^= rs.nextLong();
        }
        return xor;
    }


    private void addIntTest(final VerticalPanel vp, final Starfish32RNG rs, final String name)
    {
        final PushButton runBenchButton = new PushButton(name + ".nextInt(), " + rs.nextInt() + ", " + rs.nextInt() + ", " + rs.nextInt());
        final TextBox resultLabel = new TextBox();
        resultLabel.setReadOnly(false);
        resultLabel.setText("Not run yet");
        resultLabel.setPixelSize(560, 22);
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

    private void addLongTest(final VerticalPanel vp, final Starfish32RNG rs, final String name)
    {
        final PushButton runBenchButton = new PushButton(name + ".nextLong()");
        final TextBox resultLabel = new TextBox();
        resultLabel.setReadOnly(false);
        resultLabel.setText("Not run yet");
        resultLabel.setPixelSize(560, 22);
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
    private String runIntBenchmark(Starfish32RNG rs, long timeMinimum, int runsMinimum) {
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

    private String runLongBenchmark(Starfish32RNG rs, long timeMinimum, int runsMinimum) {
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

    private String reportInt(Starfish32RNG rs) {
        runIntBenchmark(rs, 100, 2); // warm up
        return runIntBenchmark(rs,2000, 5);
    }

    private int runInt(Starfish32RNG rs) {
        int xor = 0;
        for (int i = 0; i < 10000; i++) {
            xor ^= rs.nextInt();
        }
        return xor;
    }

    private String reportLong(Starfish32RNG rs) {
        runLongBenchmark(rs, 100, 2); // warm up
        return runLongBenchmark(rs,2000, 5);
    }

    private int runLong(Starfish32RNG rs) {
        int xor = 0;
        for (int i = 0; i < 10000; i++) {
            xor ^= rs.nextLong();
        }
        return xor;
    }

    private void addIntTest(final VerticalPanel vp, final RNG rs, final String name)
    {
        final PushButton runBenchButton = new PushButton(name + ".nextInt(), " + rs.nextInt() + ", " + rs.nextInt() + ", " + rs.nextInt());
        final TextBox resultLabel = new TextBox();
        resultLabel.setReadOnly(false);
        resultLabel.setText("Not run yet");
        resultLabel.setPixelSize(560, 22);
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

    private void addLongTest(final VerticalPanel vp, final RNG rs, final String name)
    {
        final PushButton runBenchButton = new PushButton(name + ".nextLong()");
        final TextBox resultLabel = new TextBox();
        resultLabel.setReadOnly(false);
        resultLabel.setText("Not run yet");
        resultLabel.setPixelSize(560, 22);
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
    private void addBoundedIntTest(final VerticalPanel vp, final RNG rs, final String name)
    {
        final PushButton runBenchButton = new PushButton(name + ".nextInt(100), " + rs.nextInt(100) + ", " + rs.nextInt(100) + ", " + rs.nextInt(100));
        final TextBox resultLabel = new TextBox();
        resultLabel.setReadOnly(false);
        resultLabel.setText("Not run yet");
        resultLabel.setPixelSize(560, 22);
        runBenchButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                resultLabel.setText("Running ...");
                runBenchButton.setEnabled(false);

                String benchmarkResult = reportIntBounded(rs);

                resultLabel.setText(benchmarkResult);
                runBenchButton.setEnabled(true);
            }
        });
        runBenchButton.setEnabled(true);
        vp.add(runBenchButton);
        vp.add(resultLabel);
    }


    private String runIntBenchmark(RNG rs, long timeMinimum, int runsMinimum) {
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

    private String runLongBenchmark(RNG rs, long timeMinimum, int runsMinimum) {
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
    private String runBoundedIntBenchmark(RNG rs, long timeMinimum, int runsMinimum) {
        int runs = 0;
        IntVLA samples = new IntVLA();
        long startTime, endTime, stopTime;
        stopTime = System.currentTimeMillis() + timeMinimum;
        int res = 0;
        do {
            startTime = System.currentTimeMillis();
            res ^= runIntBounded(rs);
            endTime = System.currentTimeMillis();
            samples.add((int) (endTime - startTime));
        } while (++runs < runsMinimum || endTime < stopTime);

        return res + "; " + runs + " runs; " + meanAndSEM(samples);
    }


    private String reportInt(RNG rs) {
        runIntBenchmark(rs, 100, 2); // warm up
        return runIntBenchmark(rs,2000, 5);
    }

    private int runInt(RNG rs) {
        int xor = 0;
        for (int i = 0; i < 10000; i++) {
            xor ^= rs.nextInt();
        }
        return xor;
    }


    private String reportIntBounded(RNG rs) {
        runBoundedIntBenchmark(rs, 100, 2); // warm up
        return runBoundedIntBenchmark(rs, 2000, 5);
    }

    private int runIntBounded(RNG rs) {
        int xor = 0;
        for (int i = 0; i < 10000; i++) {
            xor ^= rs.nextInt(100);
        }
        return xor;
    }

    private String reportLong(RNG rs) {
        runLongBenchmark(rs, 100, 2); // warm up
        return runLongBenchmark(rs,2000, 5);
    }

    private int runLong(RNG rs) {
        int xor = 0;
        for (int i = 0; i < 10000; i++) {
            xor ^= rs.nextLong();
        }
        return xor;
    }


}