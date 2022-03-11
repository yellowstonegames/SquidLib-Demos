package com.github.tommyettinger.bench.gwt;

import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.badlogic.gdx.backends.gwt.GwtBareApp;
import com.badlogic.gdx.math.RandomXS128;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.*;
import squidpony.squidmath.*;

import java.util.Random;


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
        return new GwtApplicationConfiguration(600, 0);
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
        vp.add(new Label(" * the RNG's calculated result, which requires 100,000 numbers to be generated,"));
        vp.add(new Label(" * the time it took in milliseconds to generate those 100,000 numbers,"));
        vp.add(new Label(" * the margin of error for the calculation, in ms as an offset from the time before it."));

        addIntTest(vp, new RNG(new Lathe32RNG(123456789)), "Lathe32RNG");
        addIntTest(vp, new RNG(new Starfish32RNG(123456789)), "Starfish32RNG");
        addIntTest(vp, new RNG(new MoverCounter32RNG(123456789)), "MoverCounter32RNG");
        addIntTest(vp, new RNG(new JSF32RNG(123456789)), "JSF32RNG");
        addIntTest(vp, new RNG(new Mover32RNG(123456789)), "Mover32RNG");
        addIntTest(vp, new RNG(new XoshiroStarStar32RNG(123456789)), "XoshiroStarStar32RNG");
        addIntTest(vp, new RNG(new XoshiroStarPhi32RNG(123456789)), "XoshiroStarPhi32RNG");
        addIntTest(vp, new RNG(new XoshiroAra32RNG(123456789)), "XoshiroAra32RNG");
        addIntTest(vp, new RNG(new Rumble32RNG(123456789)), "Rumble32RNG");
        addIntTest(vp, new RNG(new Chop32RNG(123456789)), "Chop32RNG");
        addIntTest(vp, new RNG(new Tyche32RNG(123456789)), "Tyche32RNG");
        addIntTest(vp, new GWTRNG(123456789), "GWTRNG");
        addIntTest(vp, new SilkRNG(123456789), "SilkRNG");
        addIntTest(vp, new RNG(new LightRNG(123456789)), "LightRNG");
        addIntTest(vp, new RNG(new MizuchiRNG(123456789)), "MizuchiRNG");
        addIntTest(vp, new RNG(new TangleRNG(123456789)), "TangleRNG");
        addIntTest(vp, new RNG(new TricycleRNG(123456789)), "TricycleRNG");
        addIntTest(vp, new RNG(new FourWheelRNG(123456789)), "FourWheelRNG");
        addIntTest(vp, new RNG(new StrangerRNG(123456789)), "StrangerRNG");
        addIntTest(vp, new RNG(new RomuTrioRNG(123456789)), "RomuTrioRNG");
        addIntTest(vp, new RNG(new TrimRNG(123456789)), "TrimRNG");
        addIntTest(vp, new RNG(new Trim2RNG(123456789)), "Trim2RNG");
        addIntTest(vp, new RNG(new XoshiroStarStar64RNG(123456789)), "XoshiroStarStar64RNG");
        addIntTest(vp, new RandomXS128(123456789), "RandomXS128");
        addLongTest(vp, new RNG(new Lathe32RNG(123456789)), "Lathe32RNG");
        addLongTest(vp, new RNG(new Starfish32RNG(123456789)), "Starfish32RNG");
        addLongTest(vp, new RNG(new MoverCounter32RNG(123456789)), "MoverCounter32RNG");
        addLongTest(vp, new RNG(new JSF32RNG(123456789)), "JSF32RNG");
        addLongTest(vp, new RNG(new Mover32RNG(123456789)), "Mover32RNG");
        addLongTest(vp, new RNG(new XoshiroStarStar32RNG(123456789)), "XoshiroStarStar32RNG");
        addLongTest(vp, new RNG(new XoshiroStarPhi32RNG(123456789)), "XoshiroStarPhi32RNG");
        addLongTest(vp, new RNG(new XoshiroAra32RNG(123456789)), "XoshiroAra32RNG");
        addLongTest(vp, new RNG(new Rumble32RNG(123456789)), "Rumble32RNG");
        addLongTest(vp, new RNG(new Chop32RNG(123456789)), "Chop32RNG");
        addLongTest(vp, new RNG(new Tyche32RNG(123456789)), "Tyche32RNG");
        addLongTest(vp, new GWTRNG(123456789), "GWTRNG");
        addLongTest(vp, new SilkRNG(123456789), "SilkRNG");
        addLongTest(vp, new RNG(new LightRNG(123456789)), "LightRNG");
        addLongTest(vp, new RNG(new MizuchiRNG(123456789)), "MizuchiRNG");
        addLongTest(vp, new RNG(new TangleRNG(123456789)), "TangleRNG");
        addLongTest(vp, new RNG(new TricycleRNG(123456789)), "TricycleRNG");
        addLongTest(vp, new RNG(new FourWheelRNG(123456789)), "FourWheelRNG");
        addLongTest(vp, new RNG(new StrangerRNG(123456789)), "StrangerRNG");
        addLongTest(vp, new RNG(new RomuTrioRNG(123456789)), "RomuTrioRNG");
        addLongTest(vp, new RNG(new TrimRNG(123456789)), "TrimRNG");
        addLongTest(vp, new RNG(new Trim2RNG(123456789)), "Trim2RNG");
        addLongTest(vp, new RNG(new XoshiroStarStar64RNG(123456789)), "XoshiroStarStar64RNG");
        addLongTest(vp, new RandomXS128(123456789), "RandomXS128");
        addBoundedIntTest(vp, new GWTRNG(123456789), "GWTRNG");
        addBoundedIntTest(vp, new SilkRNG(123456789), "SilkRNG");
        addBoundedIntTest(vp, new RNG(new Starfish32RNG(123456789)), "Lathe32RNG");
        addBoundedIntTest(vp, new RNG(new LightRNG(123456789)), "LightRNG");

//        vp.add(new Label("Note: Clicking multiple 'wrapped in RNG' buttons will slow down all such buttons."));
//        vp.add(new Label("This has something to do with multiple implementations of RandomnessSource being used,"));
//        vp.add(new Label(" and GWT has a hard time optimizing polymorphic code like this."));
//        vp.add(new Label("This may also affect GWTRNG, which does not use RandomnessSource in the same way."));
//        vp.add(new Label("It's probably best to stick to one RandomnessSource per app for this polymorphism reason."));

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
        JSF32RNG a = new JSF32RNG(123456789);
        Lathe32RNG b = new Lathe32RNG(123456789, 987654321);
        LightRNG c = new LightRNG(123456L);
        RandomXS128 d = new RandomXS128(123456789L, 987654321L);
        SilkRNG e = new SilkRNG(123456789, 987654321);
        Mover32RNG f = new Mover32RNG(1);

        System.out.println("JSF32RNG  " + a.nextInt());
        System.out.println("Lathe32RNG " + b.nextInt());
        System.out.println("LightRNG " + c.nextInt());
        System.out.println("RandomXS128 " + d.nextInt());
        System.out.println("SilkRNG " + e.nextInt());
        System.out.println("Mover32RNG " + f.nextInt());

        System.out.println("JSF32RNG  " + a.nextInt());
        System.out.println("Lathe32RNG " + b.nextInt());
        System.out.println("LightRNG " + c.nextInt());
        System.out.println("RandomXS128 " + d.nextInt());
        System.out.println("SilkRNG " + e.nextInt());
        System.out.println("Mover32RNG " + f.nextInt());

        System.out.println("JSF32RNG  " + a.nextInt());
        System.out.println("Lathe32RNG " + b.nextInt());
        System.out.println("LightRNG " + c.nextInt());
        System.out.println("RandomXS128 " + d.nextInt());
        System.out.println("SilkRNG " + e.nextInt());
        System.out.println("Mover32RNG " + f.nextInt());

        
    }

    private void addIntTest(final VerticalPanel vp, final IRNG rs, final String name)
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

    private void addIntTest(final VerticalPanel vp, final Random rs, final String name)
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

    private static String meanAndSEMString(double mean, double sem)
    {
        return mean + " ms +- " + sem + " ms";
    }
    /** Run the benchmark the specified number of milliseconds and return
     *  the mean execution time and SEM in milliseconds.
     */
    private String runIntBenchmark(IRNG rs, long timeMinimum, int runsMinimum) {
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
    private String runIntBenchmark(Random rs, long timeMinimum, int runsMinimum) {
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
    private String runLongBenchmark(IRNG rs, long timeMinimum, int runsMinimum) {
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
    private String runLongBenchmark(Random rs, long timeMinimum, int runsMinimum) {
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
    
    private String reportInt(IRNG rs) {
        runIntBenchmark(rs, 100, 2); // warm up
        return runIntBenchmark(rs,2000, 5);
    }

    private String reportInt(Random rs) {
        runIntBenchmark(rs, 100, 2); // warm up
        return runIntBenchmark(rs,2000, 5);
    }

    private int runInt(IRNG rs) {
        int xor = 0;
        for (int i = 0; i < 100000; i++) {
            xor ^= rs.nextInt();
        }
        return xor;
    }

    private int runInt(Random rs) {
        int xor = 0;
        for (int i = 0; i < 100000; i++) {
            xor ^= rs.nextInt();
        }
        return xor;
    }

    private String reportLong(IRNG rs) {
        runLongBenchmark(rs, 100, 2); // warm up
        return runLongBenchmark(rs,2000, 5);
    }

    private String reportLong(Random rs) {
        runLongBenchmark(rs, 100, 2); // warm up
        return runLongBenchmark(rs,2000, 5);
    }

    private int runLong(IRNG rs) {
        long xor = 0L;
        for (int i = 0; i < 100000; i++) {
            xor ^= rs.nextLong();
        }
        return (int) xor;
    }

    private int runLong(Random rs) {
        long xor = 0L;
        for (int i = 0; i < 100000; i++) {
            xor ^= rs.nextLong();
        }
        return (int) xor;
    }

    private void addBoundedIntTest(final VerticalPanel vp, final IRNG rs, final String name)
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

    private void addBoundedIntTest(final VerticalPanel vp, final Random rs, final String name)
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


    private String runBoundedIntBenchmark(IRNG rs, long timeMinimum, int runsMinimum) {
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

    private String reportIntBounded(IRNG rs) {
        runBoundedIntBenchmark(rs, 100, 2); // warm up
        return runBoundedIntBenchmark(rs, 2000, 5);
    }

    private int runIntBounded(IRNG rs) {
        int xor = 0;
        for (int i = 0; i < 100000; i++) {
            xor ^= rs.nextInt(100);
        }
        return xor;
    }

    private String reportIntBounded(Random rs) {
        runBoundedIntBenchmark(rs, 100, 2); // warm up
        return runBoundedIntBenchmark(rs, 2000, 5);
    }

    private void addLongTest(final VerticalPanel vp, final IRNG rs, final String name)
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

    private void addLongTest(final VerticalPanel vp, final Random rs, final String name)
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

    private String runBoundedIntBenchmark(Random rs, long timeMinimum, int runsMinimum) {
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

    private int runIntBounded(Random rs) {
        int xor = 0;
        for (int i = 0; i < 100000; i++) {
            xor ^= rs.nextInt(100);
        }
        return xor;
    }
}
