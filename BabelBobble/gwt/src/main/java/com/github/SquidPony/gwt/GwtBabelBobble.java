package com.github.SquidPony.gwt;

import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.badlogic.gdx.backends.gwt.GwtBareApp;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.*;
import squidpony.FakeLanguageGen;
import squidpony.NaturalLanguageCipher;
import squidpony.SquidStorage;
import squidpony.StringKit;
import squidpony.squidmath.CrossHash;
import squidpony.squidmath.StatefulRNG;

import static squidpony.FakeLanguageGen.*;

/**
 * Babel app in pure GWT so users can copy/paste in and out of it.
 * Created by Tommy Ettinger on 10/24/2016.
 */
public class GwtBabelBobble extends GwtBareApp {

    public StatefulRNG rng;
    public FakeLanguageGen lang;
    public NaturalLanguageCipher cipher;
    public TextArea currentArea, langArea;
    public SquidStorage storage;
    public String cipheredText, currentText;
    public long currentSeed;
    public TextBox seedField;
    public Slider arabicSlider, englishSlider, fantasySlider, frenchSlider, greek1Slider, greek2Slider, hindiSlider,
            japaneseSlider, lovecraftSlider, russian1Slider, russian2Slider, somaliSlider, swahiliSlider, randomSlider;
    public static final String mars =  "I have never told this story, nor shall mortal man see this manuscript until after I have passed " +
                    "over for eternity. I know that the average human mind will not believe what it cannot grasp, and so " +
                    "I do not purpose being pilloried by the public, the pulpit, and the press, and held up as a colossal " +
                    "liar when I am but telling the simple truths which some day science will substantiate. Possibly the " +
                    "suggestions which I gained upon Mars, and the knowledge which I can set down in this chronicle will " +
                    "aid in an earlier understanding of the mysteries of our sister planet; mysteries to you, but no " +
                    "longer mysteries to me.\n\nA Princess of Mars, Edgar Rice Burroughs\n\n",
            oz = "The little girl gave a cry of amazement and looked about her, her eyes growing bigger and bigger at the " +
                    "wonderful sights she saw.\n" +
                    "The cyclone had set the house down very gently — for a cyclone — in the midst of a country of marvelous " +
                    "beauty. There were lovely patches of greensward all about, with stately trees bearing rich and luscious " +
                    "fruits. Banks of gorgeous flowers were on every hand, and birds with rare and brilliant plumage sang " +
                    "and fluttered in the trees and bushes. A little way off was a small brook, rushing and sparkling along " +
                    "between green banks, and murmuring in a voice very grateful to a little girl who had lived so long on " +
                    "the dry, gray prairies.\n\nThe Wonderful Wizard of Oz, Frank L. Baum\n\n";

    @Override
    public GwtApplicationConfiguration getConfig() {
        return new GwtApplicationConfiguration(1000, 700);
    }

    public void start()
    {
        storage = new SquidStorage("babel");
        currentText = storage.get("data", "text", String.class);
        if(currentText == null || currentText.equals(""))
            currentText = mars + oz;
        seedField = new TextBox();
        seedField.setPixelSize(300, 20);
        EventListener handler = new EventListener() {
            @Override
            public void onBrowserEvent(Event event) {
                switch (event.getTypeInt())
                {
                    case Event.ONCHANGE: seedField.setText(toSeed());
                        break;
                }
            }
        };
        arabicSlider = new Slider(handler);
        englishSlider = new Slider(handler);
        fantasySlider = new Slider(handler);
        frenchSlider = new Slider(handler);
        greek1Slider = new Slider(handler);
        greek2Slider = new Slider(handler);
        hindiSlider = new Slider(handler);
        japaneseSlider = new Slider(handler);
        lovecraftSlider = new Slider(handler);
        russian1Slider = new Slider(handler);
        russian2Slider = new Slider(handler);
        somaliSlider = new Slider(handler);
        swahiliSlider = new Slider(handler);
        randomSlider = new Slider(handler);

        rng = new StatefulRNG(); //CrossHash.Lightning.hash64("Let's get babbling!")
        currentArea = new TextArea();
        currentArea.setText(currentText);
        currentArea.setVisibleLines(16);
        currentArea.setWidth("500px");
        currentArea.setHeight("500px");
        langArea = new TextArea();
        langArea.setVisibleLines(16);
        langArea.setWidth("500px");
        langArea.setHeight("500px");
        langArea.setReadOnly(true);
        root.add(new Label("Random language cipher demo"));
        HorizontalPanel texts = new HorizontalPanel();
        texts.setSize("1000px", "500px");
        texts.add(currentArea);
        texts.add(langArea);
        root.add(texts);
        VerticalPanel sliders = new VerticalPanel();
        sliders.add(row("Arabic", arabicSlider, "English", englishSlider));
        sliders.add(row("'Fantasy'", fantasySlider, "French", frenchSlider));
        sliders.add(row("Greek (1)", greek1Slider, "Greek (2)", greek2Slider));
        sliders.add(row("Hindi", hindiSlider, "Japanese", japaneseSlider));
        sliders.add(row("'Lovecraft'", lovecraftSlider, "Russian (1)", russian1Slider));
        sliders.add(row("Russian (2)", russian2Slider, "Somali", somaliSlider));
        sliders.add(row("Swahili", swahiliSlider, "RANDOM", randomSlider));
        sliders.sinkEvents(Event.ONCHANGE);
        root.add(sliders);
        HorizontalPanel seeds = new HorizontalPanel();
        seeds.add(new Label("Current seed for generating language: "));
        String txt = storage.get("data", "seed", String.class);
        if(txt == null || txt.equals(""))
            txt = fixSeed(Long.toString(rng.nextLong(), 36) + Long.toString(rng.nextLong(), 36));
        setFromSeed(txt);
        seedField.setText(txt);
        lang = mixMany();
        cipher = new NaturalLanguageCipher(lang);
        cipheredText = cipher.cipher(currentText);
        langArea.setText(cipheredText);

        seeds.add(seedField);
        root.add(seeds);
        HorizontalPanel buttonRow = new HorizontalPanel();
        final PushButton textButton = new PushButton("Randomize Seed", "Randomize Seed");
        textButton.setSize("300px", "28px");
        textButton.setText("Randomize Seed");
        textButton.setEnabled(true);
        textButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                String tx = fixSeed(Long.toString(rng.nextLong(), 36) + Long.toString(rng.nextLong(), 36));
                setFromSeed(tx);
                seedField.setText(tx);
                lang = mixMany();
                cipher = new NaturalLanguageCipher(lang);
                cipheredText = cipher.cipher(currentText = currentArea.getText());
                langArea.setText(cipheredText);

                storage.put("text", currentArea.getText());
                storage.put("seed", seedField.getText());
                storage.store("data");
            }
        });
        buttonRow.add(textButton);
        final PushButton generateButton = new PushButton("Generate", "Generate");
        //generateButton.setStyleName("purple-button", true);
        generateButton.setSize("300px", "28px");
        generateButton.setText("Generate");
        generateButton.setEnabled(true);
        //generateButton.sinkEvents(Event.ONCLICK);
        generateButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                try {
                    String txt = seedField.getText();
                    setFromSeed(txt);
                    lang = mixMany();
                    cipher = new NaturalLanguageCipher(lang);
                    cipheredText = cipher.cipher(currentText = currentArea.getText());
                    langArea.setText(cipheredText);
                } catch (Exception numberFormatOrArrayOOB) {
                    String txt = fixSeed(seedField.getText());
                    seedField.setText(txt);
                    setFromSeed(txt);
                    lang = mixMany();
                    cipher = new NaturalLanguageCipher(lang);
                    cipheredText = cipher.cipher(currentText = currentArea.getText());
                    langArea.setText(cipheredText);
                }

                storage.put("text", currentArea.getText());
                storage.put("seed", seedField.getText());
                storage.store("data");
            }
        });
        buttonRow.add(generateButton);
        RootPanel.get().sinkEvents(Event.ONCLICK); //Event.ONCHANGE |
        buttonRow.setWidth("1000px");
        buttonRow.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        RootPanel.get("embed-html").add(buttonRow);

        /*
                RootPanel.get().sinkEvents(Event.ONCLICK); //Event.ONCHANGE |
        buttonRow.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        RootPanel.get().add(buttonRow);

         */

        //RootPanel.get().setStyleName("extra-panel", true);
        //buttonRow.sinkEvents(Event.ONCLICK);
        //RootPanel.get().add(root);
    }
    public String fixSeed(String txt)
    {
        return (StringKit.hex(CrossHash.Storm.chi.hash64(txt))
                + StringKit.hex(CrossHash.Storm.upsilon.hash(txt))
                + StringKit.hex((short) CrossHash.Storm.sigma.hash(txt)) + "00").toUpperCase();
    }
    public void setFromSeed(String txt)
    {
        currentSeed = Long.parseLong(txt.substring(1, 16), 16) | ((long)Character.digit(txt.charAt(0), 16) << 60);
        rng.setState(currentSeed);
        arabicSlider.setCurrent(Character.digit(txt.charAt(16), 16));
        englishSlider.setCurrent(Character.digit(txt.charAt(17), 16));
        fantasySlider.setCurrent(Character.digit(txt.charAt(18), 16));
        frenchSlider.setCurrent(Character.digit(txt.charAt(19), 16));
        greek1Slider.setCurrent(Character.digit(txt.charAt(20), 16));
        hindiSlider.setCurrent(Character.digit(txt.charAt(21), 16));
        japaneseSlider.setCurrent(Character.digit(txt.charAt(22), 16));
        lovecraftSlider.setCurrent(Character.digit(txt.charAt(23), 16));
        russian1Slider.setCurrent(Character.digit(txt.charAt(24), 16));
        somaliSlider.setCurrent(Character.digit(txt.charAt(25), 16));
        swahiliSlider.setCurrent(Character.digit(txt.charAt(26), 16));
        randomSlider.setCurrent(Character.digit(txt.charAt(27), 16));
        greek2Slider.setCurrent(Character.digit(txt.charAt(28), 16));
        russian2Slider.setCurrent(Character.digit(txt.charAt(29), 16));

    }
    public String toSeed()
    {
        return (StringKit.hex(currentSeed) +
                Character.forDigit(arabicSlider.getCurrent(), 16) +
                Character.forDigit(englishSlider.getCurrent(), 16) +
                Character.forDigit(fantasySlider.getCurrent(), 16) +
                Character.forDigit(frenchSlider.getCurrent(), 16) +
                Character.forDigit(greek1Slider.getCurrent(), 16) +
                Character.forDigit(hindiSlider.getCurrent(), 16) +
                Character.forDigit(japaneseSlider.getCurrent(), 16) +
                Character.forDigit(lovecraftSlider.getCurrent(), 16) +
                Character.forDigit(russian1Slider.getCurrent(), 16) +
                Character.forDigit(somaliSlider.getCurrent(), 16) +
                Character.forDigit(swahiliSlider.getCurrent(), 16) +
                Character.forDigit(randomSlider.getCurrent(), 16) +
                Character.forDigit(greek2Slider.getCurrent(), 16) +
                Character.forDigit(russian2Slider.getCurrent(), 16)).toUpperCase();
    }

    public FakeLanguageGen mixMany() {
        long sd = rng.nextLong();
        return FakeLanguageGen.mixAll(
                FakeLanguageGen.randomLanguage(sd), randomSlider.getCurrent(),
                ARABIC_ROMANIZED, arabicSlider.getCurrent(),
                ENGLISH, englishSlider.getCurrent(),
                FANCY_FANTASY_NAME, fantasySlider.getCurrent(),
                FRENCH, frenchSlider.getCurrent(),
                GREEK_ROMANIZED, greek1Slider.getCurrent(),
                GREEK_AUTHENTIC, greek2Slider.getCurrent(),
                HINDI_ROMANIZED, hindiSlider.getCurrent(),
                JAPANESE_ROMANIZED, japaneseSlider.getCurrent(),
                LOVECRAFT, lovecraftSlider.getCurrent(),
                RUSSIAN_ROMANIZED, russian1Slider.getCurrent(),
                RUSSIAN_AUTHENTIC, russian2Slider.getCurrent(),
                SOMALI, somaliSlider.getCurrent(),
                SWAHILI, swahiliSlider.getCurrent()
        );
    }

    public HorizontalPanel labeledWidget(String text, Slider widget)
    {
        HorizontalPanel hp = new HorizontalPanel();
        hp.setWidth("500px");
        Label l = new Label(text);
        l.setWidth("200px");
        widget.setWidth("300px");
        hp.add(l);
        hp.add(widget);
        return hp;
    }

    public HorizontalPanel row(String text0, Slider widget0, String text1, Slider widget1)
    {
        HorizontalPanel hp = new HorizontalPanel();
        hp.setWidth("1000px");
        hp.add(labeledWidget(text0, widget0));
        hp.add(labeledWidget(text1, widget1));
        return hp;
    }

}
