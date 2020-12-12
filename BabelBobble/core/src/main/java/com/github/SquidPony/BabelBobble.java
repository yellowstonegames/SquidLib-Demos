package com.github.SquidPony;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.VisUI.SkinScale;
import com.kotcrab.vis.ui.widget.*;
import squidpony.FakeLanguageGen;
import squidpony.NaturalLanguageCipher;
import squidpony.SquidStorage;
import squidpony.StringKit;
import squidpony.squidmath.CrossHash;
import squidpony.squidmath.StatefulRNG;

import static squidpony.FakeLanguageGen.*;

/**
 * {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms.
 */
public class BabelBobble extends ApplicationAdapter {
    private Stage stage;
    public StatefulRNG rng;
    public FakeLanguageGen lang;
    public NaturalLanguageCipher cipher;
    public VisTextArea currentArea, langArea;
    public SquidStorage storage;
    public Copier copier;
    public BabelBobble(Copier cop)
    {
        copier = cop;
    }
    public static final String flatland = "I call our world Flatland, not because we call it so, but to make its nature clearer " +
            "to you, my happy readers, who are privileged to live in Space.\n" +
            "Imagine a vast sheet of paper on which straight Lines, Triangles, Squares, Pentagons, Hexagons, and other " +
            "figures, instead of remaining fixed in their places, move freely about, on or in the surface, but without " +
            "the power of rising above or sinking below it, very much like shadows - only hard and with luminous edges - " +
            "and you will then have a pretty correct notion of my country and countrymen. Alas, a few years ago, I should " +
            "have said \"my universe\": but now my mind has been opened to higher views of things.",
            mars =  "I have never told this story, nor shall mortal man see this manuscript until after I have passed " +
                    "over for eternity. I know that the average human mind will not believe what it cannot grasp, and so " +
                    "I do not purpose being pilloried by the public, the pulpit, and the press, and held up as a colossal " +
                    "liar when I am but telling the simple truths which some day science will substantiate. Possibly the " +
                    "suggestions which I gained upon Mars, and the knowledge which I can set down in this chronicle will " +
                    "aid in an earlier understanding of the mysteries of our sister planet; mysteries to you, but no " +
                    "longer mysteries to me.",
            oz = "The little girl gave a cry of amazement and looked about her, her eyes growing bigger and bigger at the " +
                    "wonderful sights she saw.\n" +

                    "The cyclone had set the house down very gently — for a cyclone — in the midst of a country of marvelous " +
                    "beauty. There were lovely patches of greensward all about, with stately trees bearing rich and luscious " +
                    "fruits. Banks of gorgeous flowers were on every hand, and birds with rare and brilliant plumage sang " +
                    "and fluttered in the trees and bushes. A little way off was a small brook, rushing and sparkling along " +
                    "between green banks, and murmuring in a voice very grateful to a little girl who had lived so long on " +
                    "the dry, gray prairies.";
    public String cipheredText, currentText;
    public long currentSeed;
    public VisTextField seedField;
    public VisSlider arabicSlider, englishSlider, fantasySlider, frenchSlider, greekSlider, hindiSlider, japaneseSlider,
            lovecraftSlider, russianSlider, somaliSlider, swahiliSlider, randomSlider;

    @Override
    public void create() {
        VisUI.load(SkinScale.X1);
        storage = new SquidStorage("babel");
        currentText = storage.get("data", "text", String.class);
        if(currentText == null || currentText.equals("")) currentText = mars;
        stage = new Stage(new ScreenViewport());

        VisTable root = new VisTable();
        root.setFillParent(true);
        stage.addActor(root);

        seedField = new VisTextField();
        seedField.setWidth(150);

        arabicSlider = new VisSlider(0, 15, 1f, false);
        englishSlider = new VisSlider(0, 15, 1f, false);
        fantasySlider = new VisSlider(0, 15, 1f, false);
        frenchSlider = new VisSlider(0, 15, 1f, false);
        greekSlider = new VisSlider(0, 15, 1f, false);
        hindiSlider = new VisSlider(0, 15, 1f, false);
        japaneseSlider = new VisSlider(0, 15, 1f, false);
        lovecraftSlider = new VisSlider(0, 15, 1f, false);
        russianSlider = new VisSlider(0, 15, 1f, false);
        somaliSlider = new VisSlider(0, 15, 1f, false);
        swahiliSlider = new VisSlider(0, 15, 1f, false);
        randomSlider = new VisSlider(0, 15, 1f, false);

        arabicSlider.setWidth(200);
        englishSlider.setWidth(200);
        fantasySlider.setWidth(200);
        frenchSlider.setWidth(200);
        greekSlider.setWidth(200);
        hindiSlider.setWidth(200);
        japaneseSlider.setWidth(200);
        lovecraftSlider.setWidth(200);
        russianSlider.setWidth(200);
        somaliSlider.setWidth(200);
        swahiliSlider.setWidth(200);
        randomSlider.setWidth(200);
        
        ChangeListener listener = new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                seedField.setText(toSeed());
            }
        };
        arabicSlider.addListener(listener);
        englishSlider.addListener(listener);
        fantasySlider.addListener(listener);
        frenchSlider.addListener(listener);
        greekSlider.addListener(listener);
        hindiSlider.addListener(listener);
        japaneseSlider.addListener(listener);
        lovecraftSlider.addListener(listener);
        russianSlider.addListener(listener);
        somaliSlider.addListener(listener);
        swahiliSlider.addListener(listener);
        randomSlider.addListener(listener);

        rng = new StatefulRNG(); //CrossHash.Lightning.hash64("Let's get babbling!")
        currentArea = new VisTextArea(currentText);
        currentArea.setPrefRows(16);
        currentArea.setWidth(460);
        currentArea.setHeight(450);
        langArea = new VisTextArea();
        langArea.setPrefRows(16);
        langArea.setWidth(460);
        langArea.setHeight(450);
        langArea.setReadOnly(true);
        root.add("Random language cipher demo").colspan(2).top().pad(10).row();
        root.add(currentArea).bottom().fillX().expandX().pad(10);
        root.add(langArea).bottom().fillX().expandX().pad(10).row();
        VisTable tab2 = new VisTable();
        tab2.setWidth(800);
        tab2.add(new VisLabel("Arabic"), arabicSlider); tab2.add().pad(10);tab2.add(new VisLabel("English"), englishSlider);
        tab2.row();
        tab2.add(new VisLabel("'Fantasy'"), fantasySlider); tab2.add().pad(10);tab2.add(new VisLabel("French"), frenchSlider);
        tab2.row();
        tab2.add(new VisLabel("Greek"), greekSlider); tab2.add().pad(10);tab2.add(new VisLabel("Hindi"), hindiSlider);
        tab2.row();
        tab2.add(new VisLabel("Japanese"), japaneseSlider); tab2.add().pad(10);tab2.add(new VisLabel("'Lovecraft'"), lovecraftSlider);
        tab2.row();
        tab2.add(new VisLabel("Russian"), russianSlider); tab2.add().pad(10);tab2.add(new VisLabel("Somali"), somaliSlider);
        tab2.row();
        tab2.add(new VisLabel("Swahili"), swahiliSlider); tab2.add().pad(10);tab2.add(new VisLabel("Random"), randomSlider);
        tab2.row();
        root.add(tab2).colspan(2).fillX().expandX().pad(5).row();

        root.add("Current seed for generating language: ").pad(5);
        String txt = storage.get("data", "seed", String.class);
        if(txt == null || txt.equals(""))
            txt = fixSeed(Long.toString(rng.nextLong(), 36) + Long.toString(rng.nextLong(), 36));
        setFromSeed(txt);
        seedField.setText(txt);
        lang = mixMany();
        cipher = new NaturalLanguageCipher(lang);
        cipheredText = cipher.cipher(currentText);

        langArea.setText(cipheredText);

        root.add(seedField).fillX().expandX().pad(20).row();
        final VisTextButton textButton = new VisTextButton("Randomize Seed");
        textButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
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
                if(copier != null)
                {
                    copier.clear();
                    copier.copy(currentText);
                    copier.copy("\n\n");
                    copier.copy(cipheredText);
                    copier.copy("\n\n");
                    copier.copy(seedField.getText());
                }
            }
        });
        root.add(textButton).pad(10);
        final VisTextButton generateButton = new VisTextButton("Generate");
        generateButton.setColor(Color.PURPLE);
        generateButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String txt = seedField.getText();
                try {
                    setFromSeed(txt);
                    lang = mixMany();
                    cipher = new NaturalLanguageCipher(lang);
                    cipheredText = cipher.cipher(currentText = currentArea.getText());
                    langArea.setText(cipheredText);
                } catch (Exception numberFormatOrArrayOOB) {
                    txt = fixSeed(txt);
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
                if(copier != null)
                {
                    copier.clear();
                    copier.copy(currentText);
                    copier.copy("\n\n");
                    copier.copy(cipheredText);
                    copier.copy("\n\n");
                    copier.copy(seedField.getText());
                }
            }
        });

        root.add(generateButton).pad(10);
        root.pack();
        Gdx.input.setInputProcessor(stage);

        /*VisWindow window = new VisWindow("example window");
        window.add(ozArea);
        window.add("blah blah blah");
        window.add("this is a simple VisUI window").padTop(5f).row();
        window.add(textButton).pad(10f);
        window.pack();
        window.centerWindow();
        stage.addActor(window.fadeIn());
        */
    }
    public String fixSeed(String txt)
    {
        return StringKit.hex(CrossHash.Curlup.chi.hash64(txt))
                + StringKit.hex(CrossHash.Curlup.upsilon.hash(txt))
                + StringKit.hex((short) (CrossHash.Curlup.sigma.hash(txt)));
    }
    public void setFromSeed(String txt)
    {
        currentSeed = Long.parseLong(txt.substring(1, 16), 16) | ((long)Character.digit(txt.charAt(0), 16) << 60);
        rng.setState(currentSeed);
        arabicSlider.setValue(Character.digit(txt.charAt(16), 16));
        englishSlider.setValue(Character.digit(txt.charAt(17), 16));
        fantasySlider.setValue(Character.digit(txt.charAt(18), 16));
        frenchSlider.setValue(Character.digit(txt.charAt(19), 16));
        greekSlider.setValue(Character.digit(txt.charAt(20), 16));
        hindiSlider.setValue(Character.digit(txt.charAt(21), 16));
        japaneseSlider.setValue(Character.digit(txt.charAt(22), 16));
        lovecraftSlider.setValue(Character.digit(txt.charAt(23), 16));
        russianSlider.setValue(Character.digit(txt.charAt(24), 16));
        somaliSlider.setValue(Character.digit(txt.charAt(25), 16));
        swahiliSlider.setValue(Character.digit(txt.charAt(26), 16));
        randomSlider.setValue(Character.digit(txt.charAt(27), 16));
    }
    public String toSeed()
    {
        return StringKit.hex(currentSeed) +
                Character.forDigit((int) arabicSlider.getValue(), 16) +
                Character.forDigit((int) englishSlider.getValue(), 16) +
                Character.forDigit((int) fantasySlider.getValue(), 16) +
                Character.forDigit((int) frenchSlider.getValue(), 16) +
                Character.forDigit((int) greekSlider.getValue(), 16) +
                Character.forDigit((int) hindiSlider.getValue(), 16) +
                Character.forDigit((int) japaneseSlider.getValue(), 16) +
                Character.forDigit((int) lovecraftSlider.getValue(), 16) +
                Character.forDigit((int) russianSlider.getValue(), 16) +
                Character.forDigit((int) somaliSlider.getValue(), 16) +
                Character.forDigit((int) swahiliSlider.getValue(), 16) +
                Character.forDigit((int) randomSlider.getValue(), 16);
    }

    public FakeLanguageGen mixMany()
    {
        return FakeLanguageGen.mixAll(
                FakeLanguageGen.randomLanguage(rng.nextLong()), randomSlider.getValue(),
                ARABIC_ROMANIZED, arabicSlider.getValue(),
                ENGLISH, englishSlider.getValue(),
                FANTASY_NAME, fantasySlider.getValue(),
                FRENCH, frenchSlider.getValue(),
                GREEK_ROMANIZED, greekSlider.getValue(),
                HINDI_ROMANIZED, hindiSlider.getValue(),
                JAPANESE_ROMANIZED, japaneseSlider.getValue(),
                LOVECRAFT, lovecraftSlider.getValue(),
                RUSSIAN_ROMANIZED, russianSlider.getValue(),
                SOMALI, somaliSlider.getValue(),
                SWAHILI, swahiliSlider.getValue()
                ).removeAccents();
        /*
        FakeLanguageGen mixer = FakeLanguageGen.randomLanguage(rng);
        float arabic = arabicSlider.getValue(),
                english = englishSlider.getValue(),
                fantasy = fantasySlider.getValue(),
                french = frenchSlider.getValue(),
                greek = greekSlider.getValue(),
                hindi = hindiSlider.getValue(),
                japanese = japaneseSlider.getValue(),
                lovecraft = lovecraftSlider.getValue(),
                russian = russianSlider.getValue(),
                somali = somaliSlider.getValue(),
                swahili = swahiliSlider.getValue(),
                randomized = randomSlider.getValue(),
                total = arabic + english + fantasy + french + greek + hindi + japanese
                + lovecraft + russian + somali + swahili + randomized;
        if(total == 0)
            return mixer.removeAccents();
        float current = randomized / total;
        if(arabic   > 0)
            mixer = mixer.mix(FakeLanguageGen.ARABIC_ROMANIZED.addModifiers(FakeLanguageGen.Modifier.SIMPLIFY_ARABIC),
                    (arabic / total) / (current += arabic / total));
        if(english   > 0) mixer = mixer.mix(FakeLanguageGen.ENGLISH, (english / total) / (current += english / total));
        if(fantasy   > 0) mixer = mixer.mix(FakeLanguageGen.FANTASY_NAME, (fantasy / total) / (current += fantasy / total));
        if(french    > 0) mixer = mixer.mix(FakeLanguageGen.FRENCH, (french    / total) / (current += french    / total));
        if(greek     > 0) mixer = mixer.mix(FakeLanguageGen.GREEK_ROMANIZED, (greek     / total) / (current += greek     / total));
        if(hindi     > 0) mixer = mixer.mix(FakeLanguageGen.HINDI_ROMANIZED, (hindi     / total) / (current += hindi     / total));
        if(japanese  > 0) mixer = mixer.mix(FakeLanguageGen.JAPANESE_ROMANIZED, (japanese  / total) / (current += japanese  / total));
        if(lovecraft > 0) mixer = mixer.mix(FakeLanguageGen.LOVECRAFT, (lovecraft / total) / (current += lovecraft / total));
        if(russian   > 0) mixer = mixer.mix(FakeLanguageGen.RUSSIAN_ROMANIZED, (russian   / total) / (current += russian   / total));
        if(somali    > 0) mixer = mixer.mix(FakeLanguageGen.SOMALI, (somali    / total) / (current += somali    / total));
        if(swahili   > 0) mixer = mixer.mix(FakeLanguageGen.SWAHILI, (swahili   / total) / (current + swahili   / total));
        return mixer.removeAccents();
        */
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        stage.draw();
    }

    @Override
    public void dispose() {
        VisUI.dispose();
        stage.dispose();
    }
}