/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  RaceVehicles
 *  com.jme3.app.SimpleApplication
 *  com.jme3.app.state.AppState
 *  com.jme3.app.state.ScreenshotAppState
 *  com.jme3.asset.AssetManager
 *  com.jme3.export.Savable
 *  com.jme3.export.binary.BinaryExporter
 *  com.jme3.export.binary.BinaryImporter
 *  com.jme3.font.BitmapText
 *  com.jme3.light.AmbientLight
 *  com.jme3.light.DirectionalLight
 *  com.jme3.light.Light
 *  com.jme3.material.Material
 *  com.jme3.math.ColorRGBA
 *  com.jme3.math.Vector3f
 *  com.jme3.scene.Geometry
 *  com.jme3.scene.Mesh
 *  com.jme3.scene.Node
 *  com.jme3.scene.Spatial
 *  com.jme3.scene.shape.Box
 *  com.jme3.system.AppSettings
 */
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppState;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.asset.AssetManager;
import com.jme3.export.Savable;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.export.binary.BinaryImporter;
import com.jme3.font.BitmapText;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;

public final class VehicleGarage
extends SimpleApplication {
    private final Node display = new Node("turntable");
    private final Path output;
    private ScreenshotAppState screenshot;
    private int frames;

    VehicleGarage(Path output) {
        this.output = output;
    }

    public static void main(String[] args) {
        VehicleGarage app = new VehicleGarage(args.length == 0 ? null : Path.of(args[0], new String[0]));
        AppSettings settings = new AppSettings(true);
        settings.setTitle("GRID//RIFT | Vehicle workshop");
        settings.setResolution(1100, 720);
        settings.setAudioRenderer(null);
        settings.setSamples(4);
        settings.setFrameRate(60);
        app.setSettings(settings);
        app.setShowSettings(false);
        app.start();
    }

    public void simpleInitApp() {
        this.flyCam.setMoveSpeed(5.0f);
        this.flyCam.setDragToRotate(true);
        this.setDisplayFps(false);
        this.setDisplayStatView(false);
        this.setPauseOnLostFocus(false);
        this.viewPort.setBackgroundColor(new ColorRGBA(0.025f, 0.04f, 0.065f, 1.0f));
        AmbientLight fill = new AmbientLight();
        fill.setColor(ColorRGBA.White.mult(0.55f));
        this.rootNode.addLight((Light)fill);
        DirectionalLight key = new DirectionalLight();
        key.setDirection(new Vector3f(-1.0f, -2.0f, 1.0f).normalizeLocal());
        key.setColor(ColorRGBA.White.mult(1.1f));
        this.rootNode.addLight((Light)key);
        DirectionalLight rim = new DirectionalLight();
        rim.setDirection(new Vector3f(1.0f, -1.0f, -1.0f).normalizeLocal());
        rim.setColor(new ColorRGBA(0.3f, 0.5f, 0.7f, 1.0f));
        this.rootNode.addLight((Light)rim);
        this.rootNode.attachChild((Spatial)this.display);
        for (int id = 0; id < 2; ++id) {
            Node model = RaceVehicles.create((AssetManager)this.assetManager, (int)id);
            if (model.getTriangleCount() < 200) {
                throw new IllegalStateException("Vehicle geometry missing");
            }
            if (this.output != null) {
                this.export(model, id);
            }
            model.setLocalTranslation(id == 0 ? 1.65f : -1.65f, 0.0f, 0.0f);
            this.display.attachChild((Spatial)model);
            Geometry plinth = new Geometry("plinth", (Mesh)new Box(1.3f, 0.08f, 1.55f));
            Material mat = new Material(this.assetManager, "Common/MatDefs/Light/Lighting.j3md");
            mat.setBoolean("UseMaterialColors", true);
            mat.setColor("Diffuse", new ColorRGBA(0.09f, 0.13f, 0.18f, 1.0f));
            mat.setColor("Ambient", ColorRGBA.White.mult(0.15f));
            plinth.setMaterial(mat);
            plinth.setLocalTranslation(model.getLocalTranslation().x, -0.08f, 0.0f);
            this.display.attachChild((Spatial)plinth);
        }
        this.cam.setLocation(new Vector3f(4.6f, 4.1f, -7.8f));
        this.cam.lookAt(new Vector3f(0.0f, 0.45f, 0.0f), Vector3f.UNIT_Y);
        this.text("GRID // RIFT", 35, 675, 30, ColorRGBA.White);
        this.text("VEHICLE WORKSHOP   /   LOW-POLY ORIGINALS", 37, 639, 14, new ColorRGBA(0.5f, 0.66f, 0.77f, 1.0f));
        this.text("01 / SCOUT BUGGY", 65, 105, 23, new ColorRGBA(0.1f, 0.9f, 0.85f, 1.0f));
        this.text("4 wheels / roll cage / roof EMP coil", 65, 73, 14, ColorRGBA.LightGray);
        this.text("02 / SIEGE CRAWLER", 650, 105, 23, new ColorRGBA(1.0f, 0.55f, 0.18f, 1.0f));
        this.text("Armored tracks / turret / rail cannon", 650, 73, 14, ColorRGBA.LightGray);
        this.text("Drag mouse to look | WASD to inspect | Esc to exit", 35, 30, 13, ColorRGBA.LightGray);
        if (this.output != null) {
            this.screenshot = new ScreenshotAppState(String.valueOf(this.output.toAbsolutePath()) + File.separator, "garage");
            this.screenshot.setIsNumbered(false);
            this.stateManager.attach((AppState)this.screenshot);
        }
    }

    private void export(Node model, int id) {
        try {
            Files.createDirectories(this.output, new FileAttribute[0]);
            Path file = this.output.resolve(id == 0 ? "scout-buggy.j3o" : "siege-crawler.j3o");
            BinaryExporter.getInstance().save((Savable)model, file.toFile());
            BinaryImporter importer = BinaryImporter.getInstance();
            importer.setAssetManager(this.assetManager);
            Node restored = (Node)importer.load(file.toFile());
            if (restored.getTriangleCount() != model.getTriangleCount()) {
                throw new IllegalStateException("Model export lost geometry");
            }
            System.out.println("PASS model export/reload: " + String.valueOf(file.getFileName()) + " / " + model.getTriangleCount() + " triangles");
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void text(String value, int x, int y, int size, ColorRGBA color) {
        BitmapText label = new BitmapText(this.guiFont);
        label.setText(value);
        label.setSize((float)size);
        label.setColor(color);
        label.setLocalTranslation((float)x, (float)y, 0.0f);
        this.guiNode.attachChild((Spatial)label);
    }

    public void simpleUpdate(float tpf) {
        if (this.output == null) {
            return;
        }
        ++this.frames;
        if (this.frames == 60) {
            this.screenshot.takeScreenshot();
        }
        if (this.frames == 90) {
            this.stop();
        }
    }
}

