/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.jme3.asset.AssetManager
 *  com.jme3.material.Material
 *  com.jme3.math.ColorRGBA
 *  com.jme3.scene.Geometry
 *  com.jme3.scene.Mesh
 *  com.jme3.scene.Node
 *  com.jme3.scene.Spatial
 *  com.jme3.scene.shape.Box
 *  com.jme3.scene.shape.Cylinder
 *  com.jme3.scene.shape.Torus
 */
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;
import com.jme3.scene.shape.Torus;

public final class RaceVehicles {
    private final AssetManager assets;
    private final Node car;
    private final Material paint;
    private final Material rubber;
    private final Material metal;
    private final Material glass;
    private final Material lamp;
    private final Material red;

    private RaceVehicles(AssetManager assets, int role) {
        this.assets = assets;
        this.car = new Node(role == 0 ? "Scout buggy" : "Siege crawler");
        this.paint = this.mat(role == 0 ? new ColorRGBA(0.03f, 0.63f, 0.62f, 1.0f) : new ColorRGBA(0.85f, 0.36f, 0.07f, 1.0f), false);
        this.rubber = this.mat(new ColorRGBA(0.035f, 0.045f, 0.055f, 1.0f), false);
        this.metal = this.mat(new ColorRGBA(0.21f, 0.28f, 0.33f, 1.0f), false);
        this.glass = this.mat(new ColorRGBA(0.025f, 0.08f, 0.12f, 1.0f), false);
        this.lamp = this.mat(new ColorRGBA(0.65f, 0.95f, 1.0f, 1.0f), true);
        this.red = this.mat(new ColorRGBA(1.0f, 0.09f, 0.04f, 1.0f), true);
    }

    public static Node create(AssetManager assets, int role) {
        RaceVehicles v = new RaceVehicles(assets, role);
        if (role == 0) {
            v.scout();
        } else {
            v.tank();
        }
        return v.car;
    }

    private Material mat(ColorRGBA color, boolean glow) {
        Material m = new Material(this.assets, glow ? "Common/MatDefs/Misc/Unshaded.j3md" : "Common/MatDefs/Light/Lighting.j3md");
        if (glow) {
            m.setColor("Color", color);
        } else {
            m.setBoolean("UseMaterialColors", true);
            m.setColor("Diffuse", color);
            m.setColor("Ambient", color);
            m.setColor("Specular", ColorRGBA.White.mult(0.4f));
            m.setFloat("Shininess", 48.0f);
        }
        return m;
    }

    private Geometry part(String name, Mesh mesh, float x, float y, float z, Material m) {
        Geometry g = new Geometry(name, mesh);
        g.setMaterial(m);
        g.setLocalTranslation(x, y, z);
        this.car.attachChild((Spatial)g);
        return g;
    }

    private Geometry box(String name, float x, float y, float z, float a, float b, float c, Material m) {
        return this.part(name, (Mesh)new Box(a, b, c), x, y, z, m);
    }

    private void wheel(float x, float y, float z, float radius, float width) {
        this.part("tire", (Mesh)new Cylinder(2, 16, radius, width, true), x, y, z, this.rubber).rotate(0.0f, 1.5707964f, 0.0f);
        this.part("hub", (Mesh)new Cylinder(2, 8, radius * 0.5f, width + 0.012f, true), x, y, z, this.metal).rotate(0.0f, 1.5707964f, 0.0f);
        this.part("hub cap", (Mesh)new Cylinder(2, 8, radius * 0.19f, width + 0.025f, true), x, y, z, this.paint).rotate(0.0f, 1.5707964f, 0.0f);
    }

    private void scout() {
        this.box("chassis", 0.0f, 0.39f, 0.0f, 0.48f, 0.12f, 0.83f, this.metal);
        this.box("body", 0.0f, 0.55f, 0.0f, 0.48f, 0.13f, 0.77f, this.paint);
        this.box("sloping hood", 0.0f, 0.64f, -0.59f, 0.46f, 0.065f, 0.32f, this.paint).rotate(-0.14f, 0.0f, 0.0f);
        this.box("cabin", 0.0f, 0.84f, 0.05f, 0.38f, 0.25f, 0.35f, this.glass);
        this.box("roof", 0.0f, 1.12f, 0.08f, 0.42f, 0.05f, 0.4f, this.paint);
        this.box("windshield", 0.0f, 0.89f, -0.31f, 0.33f, 0.17f, 0.015f, this.glass).rotate(-0.18f, 0.0f, 0.0f);
        for (int side : new int[]{-1, 1}) {
            this.wheel((float)side * 0.58f, 0.31f, -0.57f, 0.31f, 0.24f);
            this.wheel((float)side * 0.58f, 0.31f, 0.6f, 0.31f, 0.24f);
            for (float z : new float[]{-0.57f, 0.6f}) {
                this.box("fender", (float)side * 0.55f, 0.64f, z, 0.19f, 0.035f, 0.35f, this.metal);
            }
            this.box("roll cage", (float)side * 0.37f, 0.86f, -0.28f, 0.025f, 0.23f, 0.025f, this.metal).rotate(-0.18f, 0.0f, 0.0f);
            this.box("headlamp", (float)side * 0.31f, 0.6f, -0.91f, 0.12f, 0.055f, 0.025f, this.lamp);
            this.box("tail lamp", (float)side * 0.34f, 0.59f, 0.79f, 0.09f, 0.04f, 0.02f, this.red);
            this.box("side stripe", (float)side * 0.485f, 0.54f, 0.0f, 0.012f, 0.025f, 0.47f, this.lamp);
            this.box("spoiler mount", (float)side * 0.3f, 0.83f, 0.71f, 0.025f, 0.17f, 0.03f, this.metal);
        }
        this.box("spoiler", 0.0f, 1.0f, 0.72f, 0.52f, 0.035f, 0.14f, this.paint);
        this.box("front bumper", 0.0f, 0.37f, -0.94f, 0.5f, 0.055f, 0.07f, this.rubber);
        this.part("EMP coil", (Mesh)new Torus(16, 8, 0.035f, 0.17f), 0.0f, 1.23f, 0.1f, this.lamp).rotate(1.5707964f, 0.0f, 0.0f);
        this.box("EMP core", 0.0f, 1.2f, 0.1f, 0.07f, 0.09f, 0.07f, this.metal);
        this.box("antenna", 0.28f, 1.37f, 0.3f, 0.012f, 0.23f, 0.012f, this.metal);
    }

    private void tank() {
        this.box("lower hull", 0.0f, 0.45f, 0.0f, 0.56f, 0.19f, 0.85f, this.metal);
        this.box("armored hull", 0.0f, 0.74f, 0.0f, 0.62f, 0.15f, 0.87f, this.paint);
        this.box("front armor", 0.0f, 0.66f, -0.87f, 0.56f, 0.16f, 0.06f, this.paint).rotate(-0.28f, 0.0f, 0.0f);
        for (int side : new int[]{-1, 1}) {
            int i;
            this.box("track belt bottom", (float)side * 0.73f, 0.11f, 0.0f, 0.2f, 0.07f, 0.96f, this.rubber);
            this.box("track belt top", (float)side * 0.73f, 0.57f, 0.0f, 0.2f, 0.06f, 0.96f, this.rubber);
            for (i = 0; i < 4; ++i) {
                this.wheel((float)side * 0.73f, 0.32f, -0.69f + (float)i * 0.46f, 0.23f, 0.37f);
            }
            for (i = 0; i < 12; ++i) {
                float z = -0.88f + (float)i * 0.16f;
                this.box("track tread", (float)side * 0.73f, 0.64f, z, 0.205f, 0.018f, 0.035f, this.metal);
                this.box("lower tread", (float)side * 0.73f, 0.035f, z, 0.205f, 0.018f, 0.035f, this.metal);
            }
            this.box("track guard", (float)side * 0.73f, 0.71f, 0.0f, 0.23f, 0.045f, 0.98f, this.paint);
            this.box("headlamp", (float)side * 0.47f, 0.83f, -0.9f, 0.085f, 0.045f, 0.025f, this.lamp);
            this.box("rear light", (float)side * 0.47f, 0.73f, 0.89f, 0.07f, 0.035f, 0.02f, this.red);
            this.box("turret armor", (float)side * 0.46f, 1.13f, 0.05f, 0.045f, 0.17f, 0.4f, this.paint);
        }
        this.part("turret bearing", (Mesh)new Cylinder(2, 20, 0.39f, 0.12f, true), 0.0f, 0.96f, 0.08f, this.metal).rotate(1.5707964f, 0.0f, 0.0f);
        this.box("turret", 0.0f, 1.14f, 0.08f, 0.43f, 0.17f, 0.43f, this.paint);
        this.box("hatch", 0.0f, 1.33f, 0.16f, 0.22f, 0.025f, 0.22f, this.metal);
        this.part("gun barrel", (Mesh)new Cylinder(2, 12, 0.09f, 0.94f, true), 0.0f, 1.16f, -0.76f, this.metal);
        this.box("muzzle brake", 0.0f, 1.16f, -1.23f, 0.13f, 0.12f, 0.12f, this.paint);
        this.box("muzzle opening", 0.0f, 1.16f, -1.355f, 0.075f, 0.065f, 0.005f, this.rubber);
        this.box("sight", 0.24f, 1.36f, -0.13f, 0.09f, 0.055f, 0.1f, this.glass);
        this.box("antenna", -0.31f, 1.53f, 0.36f, 0.012f, 0.26f, 0.012f, this.metal);
        for (int i = 0; i < 5; ++i) {
            this.box("engine vent", -0.24f + (float)i * 0.12f, 0.9f, 0.66f, 0.025f, 0.013f, 0.14f, this.rubber);
        }
    }
}

