package com.github.tommyettinger.bench.gwt;
import squidpony.NaturalLanguageCipher;
import squidpony.Thesaurus;
import squidpony.annotation.GwtIncompatible;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.badlogic.gdx.backends.gwt.GwtBareApp;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.*;
import squidpony.FakeLanguageGen;
import squidpony.squidmath.*;


/**
 * Benchmark for several different text generation algorithms on GWT. Code mostly taken from
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


    public static final GWTRNG srng = new GWTRNG(123L);
    public static final Thesaurus thesaurus = new Thesaurus(123L);

    /**
     * This is the entry point method.
     */
    @Override
    public void start() {
//        Gdx.app.setLogLevel(LOG_INFO);
        thesaurus.addKnownCategories();
        VerticalPanel vp = new VerticalPanel();
        vp.setPixelSize(600, 780);
        vp.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

        vp.add(new Label("Click any button and wait a few seconds to see:"));
        vp.add(new Label(" * the calculated result, which requires 100 sentences to be generated,"));
        vp.add(new Label(" * the time it took in milliseconds to generate those 100 sentences,"));
        vp.add(new Label(" * the margin of error for the calculation, in ms as an offset from the time before it."));

        addLangTest(vp, FakeLanguageGen.SIMPLISH, "Fake Generated English");
        addLangTest(vp, FakeLanguageGen.GREEK_AUTHENTIC, "Fake Generated Greek ");
        addLangTest(vp, FakeLanguageGen.HINDI_ROMANIZED, "Fake Generated Hindi");
        addCipherTest(vp, new NaturalLanguageCipher(FakeLanguageGen.SIMPLISH), "Cipher to Fake English");
        
        RootPanel.get().sinkEvents(Event.ONCLICK);
        RootPanel.get("embed-html").add(vp);
    }

    @GwtIncompatible
    public static void main(String[] args)
    {
        Gdx.app = new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
//                Gdx.app.setLogLevel(LOG_INFO);
                srng.setState(123L);
                System.out.println(FakeLanguageGen.SIMPLISH.sentence(srng, 3, 6));
                srng.setState(123L);
                System.out.println(FakeLanguageGen.GREEK_AUTHENTIC.sentence(srng, 3, 6));
                srng.setState(123L);
                System.out.println(FakeLanguageGen.HINDI_ROMANIZED.sentence(srng, 3, 6));
                srng.setState(123L);
                NaturalLanguageCipher gen = new NaturalLanguageCipher(FakeLanguageGen.SIMPLISH);
                gen.setCacheLevel(0);
                thesaurus.addKnownCategories();
                String nation = thesaurus.makeNationName(gen.language);
                System.out.println("nation: " + nation + ", ciphered: " + gen.cipher(nation));
                Gdx.app.exit();
            }
        });
    }

    private static String meanAndSEMString(double mean, double sem)
    {
        return mean + " ms +- " + sem + " ms";
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

    private void addLangTest(final VerticalPanel vp, final FakeLanguageGen gen, final String name)
    {
        srng.setState(123L);
        final PushButton runBenchButton = new PushButton(name + ".sentence(), " + gen.sentence(srng, 3, 6));
        final TextBox resultLabel = new TextBox();
        resultLabel.setReadOnly(false);
        resultLabel.setText("Not run yet");
        resultLabel.setPixelSize(560, 22);
        runBenchButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                resultLabel.setText("Running ...");
                runBenchButton.setEnabled(false);

                String benchmarkResult = reportLang(gen);

                resultLabel.setText(benchmarkResult);
                runBenchButton.setEnabled(true);
            }
        });
        runBenchButton.setEnabled(true);
        vp.add(runBenchButton);
        vp.add(resultLabel);
    }

    /** Run the benchmark the specified number of milliseconds and return
     *  the mean execution time and SEM in milliseconds.
     */
    private String runLangBenchmark(FakeLanguageGen gen, long timeMinimum, int runsMinimum) {
        int runs = 0;
        IntVLA samples = new IntVLA();
        long startTime, endTime, stopTime;
        stopTime = System.currentTimeMillis() + timeMinimum;
        int res = 0;
        do {
            startTime = System.currentTimeMillis();
            res ^= runLang(gen);
            endTime = System.currentTimeMillis();
            samples.add((int) (endTime - startTime));
        } while (++runs < runsMinimum || endTime < stopTime);

        return res + "; " + runs + " runs; " + meanAndSEM(samples);
    }

    private String reportLang(FakeLanguageGen gen) {
        FakeLanguageGen.srng.setState(91234567890L);
        runLangBenchmark(gen, 100, 2); // warm up
        return runLangBenchmark(gen,2000, 5);
    }

    private int runLang(FakeLanguageGen gen) {
        int xor = 0;
        for (int i = 0; i < 100; i++) {
            xor ^= gen.sentence(3, 6).hashCode();
        }
        return xor;
    }


    private void addCipherTest(final VerticalPanel vp, final NaturalLanguageCipher gen, final String name)
    {
        srng.setState(123L);
        gen.setCacheLevel(0);
        String nation = thesaurus.makeNationName(gen.language);
        final PushButton runBenchButton = new PushButton(name + " nation: " + nation + ", ciphered: " + gen.cipher(nation));
        final TextBox resultLabel = new TextBox();
        resultLabel.setReadOnly(false);
        resultLabel.setText("Not run yet");
        resultLabel.setPixelSize(560, 22);
        runBenchButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                resultLabel.setText("Running ...");
                runBenchButton.setEnabled(false);

                String benchmarkResult = reportCipher(gen);

                resultLabel.setText(benchmarkResult);
                runBenchButton.setEnabled(true);
            }
        });
        runBenchButton.setEnabled(true);
        vp.add(runBenchButton);
        vp.add(resultLabel);
    }

    /** Run the benchmark the specified number of milliseconds and return
     *  the mean execution time and SEM in milliseconds.
     */
    private String runCipherBenchmark(NaturalLanguageCipher gen, long timeMinimum, int runsMinimum) {
        int runs = 0;
        IntVLA samples = new IntVLA();
        long startTime, endTime, stopTime;
        stopTime = System.currentTimeMillis() + timeMinimum;
        int res = 0;
        do {
            startTime = System.currentTimeMillis();
            res ^= runCipher(gen);
            endTime = System.currentTimeMillis();
            samples.add((int) (endTime - startTime));
        } while (++runs < runsMinimum || endTime < stopTime);

        return res + "; " + runs + " runs; " + meanAndSEM(samples);
    }

    private String reportCipher(NaturalLanguageCipher gen) {
        srng.setState(1234567890L);
        runCipherBenchmark(gen, 100, 2); // warm up
        return runCipherBenchmark(gen,2000, 5);
    }

    private int runCipher(NaturalLanguageCipher gen) {
        int xor = 0;
        for (int i = 0; i < 100; i++) {
            xor ^= gen.lookup(srng.getRandomElement(Thesaurus.noun.randomValue(srng))).hashCode();
        }
        return xor;
    }

}