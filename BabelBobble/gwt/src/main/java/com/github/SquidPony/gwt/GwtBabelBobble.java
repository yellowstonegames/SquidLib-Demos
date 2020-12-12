package com.github.SquidPony.gwt;

import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.badlogic.gdx.backends.gwt.GwtBareApp;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.*;
import squidpony.*;
import squidpony.squidmath.*;

import java.util.ArrayList;

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
    public Slider[] sliderBunch;
    public OrderedSet<String> names = Maker.makeOS(
            "Simple", "Arabic", "English", "French", "Spanish",
            "Norse", "Greek", "Ελληνικά",
            "Russian", "Русский", "Hindi",
            "Chinese",  "Japanese", "Korean",
            "Vietnamese", "Mongolian", "Malay", "Maori",
            "Inuktitut", "Crow", "Cherokee", "Nahuatl",
            "Somali", "Swahili", "Ancient Egyptian",
            "Mónazôr", "Nyeighi", "Xorgogh", "Áhìphon", "Edyilon",
            "Ilthiê", "Dwupdagorg", "Kiathaegioth", "Trarark", "Tsarrit",
            "Riadííí", "Luuvohrm", "Khluzrelkeb",
            "Qraisojlea", "Zagtsarg", "Nralthóshos", "Hmoieloreo", "Vuŕeħid",
            "Random 1", "Random 2");
    public ArrayList<FakeLanguageGen> languages = Maker.makeList(
            SIMPLISH, ARABIC_ROMANIZED, ENGLISH, FRENCH, SPANISH,
            NORSE_SIMPLIFIED, GREEK_ROMANIZED, GREEK_AUTHENTIC,
            RUSSIAN_ROMANIZED, RUSSIAN_AUTHENTIC, HINDI_ROMANIZED,
            CHINESE_ROMANIZED, JAPANESE_ROMANIZED, KOREAN_ROMANIZED,
            VIETNAMESE, MONGOLIAN, MALAY, MAORI,
            INUKTITUT, CROW, CHEROKEE_ROMANIZED, NAHUATL,
            SOMALI, SWAHILI, ANCIENT_EGYPTIAN,
            FANCY_FANTASY_NAME, LOVECRAFT, DEMONIC, INFERNAL, CELESTIAL,
            ELF, GOBLIN, DRAGON, KOBOLD, INSECT,
            IMP, DEEP_SPEECH, HLETKIP,
            ALIEN_A, ALIEN_E, ALIEN_I, ALIEN_O, ALIEN_U,
            FakeLanguageGen.randomLanguage(9999L), FakeLanguageGen.randomLanguage(-99999999L));
    public Object[] mixer;
//    public Slider arabicSlider, englishSlider, fantasySlider, frenchSlider, greek1Slider, greek2Slider, hindiSlider,
//            japaneseSlider, lovecraftSlider, russian1Slider, russian2Slider, somaliSlider, swahiliSlider, norseSlider,
//            inuktitutSlider, nahuatlSlider, random1Slider, random2Slider;
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
    
    @Override
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
        sliderBunch = new Slider[languages.size()];
        mixer = new Object[languages.size() << 1];
        for (int i = 0; i < sliderBunch.length; i++) {
            sliderBunch[i] = new Slider(handler);
            mixer[i << 1] = languages.get(i);
            mixer[i << 1 | 1] = 0;
        }
//        arabicSlider = new Slider(handler);
//        englishSlider = new Slider(handler);
//        fantasySlider = new Slider(handler);
//        frenchSlider = new Slider(handler);
//        greek1Slider = new Slider(handler);
//        greek2Slider = new Slider(handler);
//        hindiSlider = new Slider(handler);
//        japaneseSlider = new Slider(handler);
//        lovecraftSlider = new Slider(handler);
//        russian1Slider = new Slider(handler);
//        russian2Slider = new Slider(handler);
//        somaliSlider = new Slider(handler);
//        swahiliSlider = new Slider(handler);
//        norseSlider = new Slider(handler);
//        inuktitutSlider = new Slider(handler);
//        nahuatlSlider = new Slider(handler);
//        random1Slider = new Slider(handler);
//        random2Slider = new Slider(handler);
        rng = new StatefulRNG(); //CrossHash.Lightning.hash64("Let's get babbling!")

        currentArea = new TextArea();
        currentArea.setText(currentText);
        currentArea.setVisibleLines(16);
        currentArea.setWidth("500px");
        currentArea.setHeight("350px");
        langArea = new TextArea();
        langArea.setVisibleLines(16);
        langArea.setWidth("500px");
        langArea.setHeight("350px");
        langArea.setReadOnly(true);
        root.add(new Label("Random language cipher demo"));
        HorizontalPanel texts = new HorizontalPanel();
        texts.setSize("1000px", "350px");
        texts.add(currentArea);
        texts.add(langArea);
        root.add(texts);
        VerticalPanel sliders = new VerticalPanel();
        for (int i = 0; i < sliderBunch.length - 1; i+=2) {
            sliders.add(row(names.getAt(i), sliderBunch[i], names.getAt(i+1), sliderBunch[i+1]));
        }
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
        buttonRow.setHeight("32px");
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
        char[] c = (StringKit.hex(CrossHash.Curlup.chi.hash64(txt))
                + StringKit.hex(CrossHash.Curlup.upsilon.hash64(txt))
                + StringKit.hex(CrossHash.Curlup.sigma.hash64(txt))
                + StringKit.hex(CrossHash.Curlup.omega.hash64(txt))).substring(0, languages.size() + 16).toCharArray();
        c[16 + 7] = '0';
        c[16 + 9] = '0';
        return String.valueOf(c);
    }
    public void setFromSeed(String txt)
    {
        if(txt.length() < 16 + sliderBunch.length) txt = StringKit.padRight(txt, '0', 16 + sliderBunch.length);
        currentSeed = StringKit.longFromHex(txt, 0, 16);
        // Long.parseLong(txt.substring(1, 16), 16) | ((long)Character.digit(txt.charAt(0), 16) << 60);
        rng.setState(currentSeed);
        for (int i = 0; i < sliderBunch.length; i++) {
            sliderBunch[i].setCurrent(Character.digit(txt.charAt(16 + i), 16));
        }
    }
    public String toSeed()
    {
        StringBuilder s = new StringBuilder(StringKit.hex(currentSeed));
        for (int i = 0; i < sliderBunch.length; i++) {
            s.append(Character.forDigit(sliderBunch[i].getCurrent(), 16));
        }
        return s.toString().toUpperCase();
    }

    public FakeLanguageGen mixMany() {
        long sd = rng.nextLong();
        mixer[mixer.length - 4] = FakeLanguageGen.randomLanguage(sd);
        mixer[mixer.length - 2] = FakeLanguageGen.randomLanguage(LightRNG.determine(sd));
        for (int i = 0; i < sliderBunch.length; i++) {
            mixer[i << 1 | 1] = sliderBunch[i].getCurrent();
        }
        return FakeLanguageGen.mixAll(mixer);
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
