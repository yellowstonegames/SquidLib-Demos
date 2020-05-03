package com.github.tommyettinger;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.FitViewport;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class WorldViewer extends ApplicationAdapter {
    private FitViewport viewport;
    private PerspectiveCamera camera;
    private CameraInputController cameraController;
    private ModelBatch batch;
    private Environment env;
    private ModelInstance modelInstance;
    private TextureAttribute[] textures;
    private int textureIndex;
    private ColorAttribute ambientLight;
    private DirectionalLight sunLight;
    private DirectionalLight cameraLight;

    @Override
    public void create() {
        // camera
        camera = new PerspectiveCamera();
        camera.fieldOfView = 50f;
        camera.near = .01f;
        camera.far = 100f;
        camera.up.set(Vector3.Y);
        camera.position.set(10, 10, 10);
        camera.lookAt(Vector3.Zero);
        camera.update();
        viewport = new FitViewport(16f, 9f, camera);
        cameraController = new CameraInputController(camera);
        Gdx.input.setInputProcessor(cameraController);

        // environment
        batch = new ModelBatch();
        env = new Environment();
        env.set(ambientLight = new ColorAttribute(ColorAttribute.AmbientLight, Color.DARK_GRAY));
        env.add(sunLight = new DirectionalLight().set(Color.WHITE, new Vector3(1, -3, 1)));
        env.add(cameraLight = new DirectionalLight().set(Color.WHITE, new Vector3(1, 1, 1)));

        // material
        Material material = new Material();
        material.set(ColorAttribute.createDiffuse(Color.WHITE));
        textures = new TextureAttribute[]{
                TextureAttribute.createDiffuse(new Texture(Gdx.files.internal("NASA_Earth_Map.jpg"))),
                TextureAttribute.createDiffuse(new Texture(Gdx.files.internal("EquirectangularWorldMap.png"))),
                TextureAttribute.createDiffuse(new Texture(Gdx.files.internal("GlitchEquirectangularWorldMap.png")))
        };
        textureIndex = (int) (System.currentTimeMillis() >>> 4) % 3;
        material.set(textures[textureIndex]);

        // model
        Model model = createSphere(material);
        modelInstance = new ModelInstance(model);
    }

    private Model createSphere(Material material){
        float sphereSize = 10f;
        int divisionsU = 32;
        int divisionsV = 16;
        return new ModelBuilder()
                .createSphere(sphereSize, sphereSize, sphereSize, divisionsU, divisionsV, material,
                        VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }

    @Override
    public void render() {
        cameraController.update();

        ambientLight.color.set(Color.WHITE).mul(.2f);
        sunLight.color.set(Color.WHITE).mul(.875f);
        cameraLight.color.set(Color.WHITE).mul(.625f);
        cameraLight.direction.set(camera.direction);

        Gdx.gl.glClearColor(0, 0, 0, 0);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        batch.begin(camera);
        batch.render(modelInstance, env);
        batch.end();
    }

}
