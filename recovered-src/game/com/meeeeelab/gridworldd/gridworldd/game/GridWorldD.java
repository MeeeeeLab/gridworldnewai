/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.jme3.app.SimpleApplication
 *  com.jme3.app.state.AppState
 *  com.jme3.input.controls.ActionListener
 *  com.jme3.input.controls.InputListener
 *  com.jme3.input.controls.KeyTrigger
 *  com.jme3.input.controls.Trigger
 *  com.jme3.light.AmbientLight
 *  com.jme3.light.DirectionalLight
 *  com.jme3.light.Light
 *  com.jme3.material.Material
 *  com.jme3.math.ColorRGBA
 *  com.jme3.math.FastMath
 *  com.jme3.math.Quaternion
 *  com.jme3.math.Vector3f
 *  com.jme3.scene.Geometry
 *  com.jme3.scene.Mesh
 *  com.jme3.scene.Node
 *  com.jme3.scene.Spatial
 *  com.jme3.scene.shape.Box
 *  com.jme3.scene.shape.Quad
 */
package com.meeeeelab.gridworldd.gridworldd.game;

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppState;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.InputListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.Trigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Quad;

public class GridWorldD
extends SimpleApplication
implements ActionListener {
    private Node carNode;
    private char[][] grid;
    private int carRow = 0;
    private int carCol = 0;
    private int facingRow = 0;
    private int facingCol = 1;
    private Vector3f moveTarget;
    private boolean moving = false;
    private boolean turning = false;
    private float currentYaw = 1.5707964f;
    private float targetYaw = 1.5707964f;
    private Node minimapCarNode;
    private float minimapLeft;
    private float minimapBottom;
    private final float minimapCellSize = 24.0f;
    private final float minimapPadding = 8.0f;

    public GridWorldD() {
    }

    public GridWorldD(AppState ... initialStates) {
        super(initialStates);
    }

    public void simpleInitApp() {
        this.viewPort.setBackgroundColor(new ColorRGBA(0.75f, 0.84f, 0.9f, 1.0f));
        Box b = new Box(0.95f, 1.0f, 0.95f);
        Material mat = new Material(this.assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        ColorRGBA wallColor = new ColorRGBA(0.35f, 0.55f, 0.75f, 1.0f);
        mat.setColor("Diffuse", wallColor);
        mat.setColor("Ambient", wallColor);
        mat.setColor("Specular", ColorRGBA.Black);
        this.grid = new char[][]{{'A', '.', '.', '.', '.'}, {'.', '#', '#', '.', '#'}, {'.', '.', '.', '#', '.'}, {'.', '.', '.', '.', '.'}, {'.', '.', '.', '.', 'G'}};
        for (int row = 0; row < this.grid.length; ++row) {
            for (int col = 0; col < this.grid[row].length; ++col) {
                float x = col * 2 - 4;
                float z = row * 2 - 4;
                if (this.grid[row][col] == '#') {
                    Geometry wall = new Geometry("Wall", (Mesh)b);
                    wall.setMaterial(mat);
                    wall.setLocalTranslation(x, 0.0f, z);
                    this.rootNode.attachChild((Spatial)wall);
                    continue;
                }
                if (this.grid[row][col] != 'A' && this.grid[row][col] != 'G') continue;
                Box markerBox = new Box(0.75f, 0.03f, 0.75f);
                Geometry marker = new Geometry("Marker", (Mesh)markerBox);
                Material markerMat = new Material(this.assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
                if (this.grid[row][col] == 'A') {
                    markerMat.setColor("Color", new ColorRGBA(0.95f, 0.6f, 0.25f, 1.0f));
                } else {
                    markerMat.setColor("Color", new ColorRGBA(0.25f, 0.75f, 0.45f, 1.0f));
                }
                marker.setMaterial(markerMat);
                marker.setLocalTranslation(x, -0.96f, z);
                this.rootNode.attachChild((Spatial)marker);
            }
        }
        this.carNode = new Node("Car");
        Box bodyBox = new Box(0.55f, 0.2f, 0.75f);
        Geometry body = new Geometry("Body", (Mesh)bodyBox);
        Material bodyMat = new Material(this.assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        bodyMat.setColor("Color", new ColorRGBA(0.95f, 0.55f, 0.25f, 1.0f));
        body.setMaterial(bodyMat);
        this.carNode.attachChild((Spatial)body);
        Box cabinBox = new Box(0.35f, 0.16f, 0.33f);
        Geometry cabin = new Geometry("Cabin", (Mesh)cabinBox);
        Material cabinMat = new Material(this.assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        cabinMat.setColor("Color", ColorRGBA.White);
        cabin.setMaterial(cabinMat);
        cabin.setLocalTranslation(0.0f, 0.27f, 0.0f);
        this.carNode.attachChild((Spatial)cabin);
        this.carNode.setLocalTranslation(-4.0f, -0.8f, -4.0f);
        this.moveTarget = this.carNode.getLocalTranslation().clone();
        this.rootNode.attachChild((Spatial)this.carNode);
        Box floorBox = new Box(5.0f, 0.1f, 5.0f);
        Geometry floor = new Geometry("Floor", (Mesh)floorBox);
        Material floorMat = new Material(this.assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        floorMat.setColor("Color", ColorRGBA.LightGray);
        floor.setMaterial(floorMat);
        floor.setLocalTranslation(0.0f, -1.1f, 0.0f);
        this.rootNode.attachChild((Spatial)floor);
        AmbientLight ambient = new AmbientLight();
        ambient.setColor(ColorRGBA.White.mult(0.5f));
        this.rootNode.addLight((Light)ambient);
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-1.0f, -2.0f, -1.0f).normalizeLocal());
        sun.setColor(ColorRGBA.White.mult(1.0f));
        this.rootNode.addLight((Light)sun);
        this.flyCam.setEnabled(false);
        this.inputManager.addMapping("Forward", new Trigger[]{new KeyTrigger(17), new KeyTrigger(200)});
        this.inputManager.addMapping("Backward", new Trigger[]{new KeyTrigger(31), new KeyTrigger(208)});
        this.inputManager.addMapping("TurnLeft", new Trigger[]{new KeyTrigger(30), new KeyTrigger(203)});
        this.inputManager.addMapping("TurnRight", new Trigger[]{new KeyTrigger(32), new KeyTrigger(205)});
        this.inputManager.addListener((InputListener)this, new String[]{"Forward", "Backward", "TurnLeft", "TurnRight"});
        this.updateCamera();
        this.createMinimap();
    }

    public void onAction(String name, boolean isPressed, float tpf) {
        int step;
        if (!isPressed || this.moving || this.turning) {
            return;
        }
        if (name.equals("TurnLeft")) {
            int oldRow = this.facingRow;
            this.facingRow = -this.facingCol;
            this.facingCol = oldRow;
            this.targetYaw += 1.5707964f;
            this.turning = true;
            return;
        }
        if (name.equals("TurnRight")) {
            int oldRow = this.facingRow;
            this.facingRow = this.facingCol;
            this.facingCol = -oldRow;
            this.targetYaw -= 1.5707964f;
            this.turning = true;
            return;
        }
        if (name.equals("Forward")) {
            step = 1;
        } else if (name.equals("Backward")) {
            step = -1;
        } else {
            return;
        }
        int nextRow = this.carRow + this.facingRow * step;
        int nextCol = this.carCol + this.facingCol * step;
        if (nextRow < 0 || nextRow >= this.grid.length || nextCol < 0 || nextCol >= this.grid[nextRow].length || this.grid[nextRow][nextCol] == '#') {
            System.out.println("Blocked at: " + nextRow + ", " + nextCol);
            return;
        }
        this.carRow = nextRow;
        this.carCol = nextCol;
        float x = this.carCol * 2 - 4;
        float z = this.carRow * 2 - 4;
        this.moveTarget.set(x, -0.8f, z);
        this.moving = true;
    }

    public void simpleUpdate(float tpf) {
        if (this.moving) {
            Vector3f remaining = this.moveTarget.subtract(this.carNode.getLocalTranslation());
            float maxStep = 4.0f * tpf;
            if (remaining.length() <= maxStep) {
                this.carNode.setLocalTranslation(this.moveTarget);
                this.moving = false;
                System.out.println("Arrived: " + this.carRow + ", " + this.carCol);
            } else {
                this.carNode.move(remaining.normalizeLocal().multLocal(maxStep));
            }
        }
        if (this.turning) {
            float remainingAngle = this.targetYaw - this.currentYaw;
            float maxTurn = (float)Math.PI * tpf;
            if (FastMath.abs((float)remainingAngle) <= maxTurn) {
                this.currentYaw = this.targetYaw;
                this.turning = false;
            } else {
                this.currentYaw += Math.signum(remainingAngle) * maxTurn;
            }
        }
        this.updateCamera();
        this.updateMinimap();
    }

    private void updateCamera() {
        Vector3f position = this.carNode.getLocalTranslation();
        float forwardX = FastMath.sin((float)this.currentYaw);
        float forwardZ = FastMath.cos((float)this.currentYaw);
        float eyeY = 0.1f;
        this.cam.setLocation(new Vector3f(position.x + forwardX * 0.25f, eyeY, position.z + forwardZ * 0.25f));
        this.cam.lookAt(new Vector3f(position.x + forwardX * 5.0f, eyeY, position.z + forwardZ * 5.0f), Vector3f.UNIT_Y);
        this.carNode.setLocalRotation(new Quaternion().fromAngleAxis(this.currentYaw, Vector3f.UNIT_Y));
    }

    private void createMinimap() {
        float mapWidth = (float)this.grid[0].length * 24.0f + 16.0f;
        float mapHeight = (float)this.grid.length * 24.0f + 16.0f;
        this.minimapLeft = (float)this.cam.getWidth() - mapWidth - 20.0f;
        this.minimapBottom = (float)this.cam.getHeight() - mapHeight - 20.0f;
        this.addMinimapQuad("MinimapBackground", mapWidth, mapHeight, new ColorRGBA(0.1f, 0.14f, 0.2f, 1.0f), this.minimapLeft, this.minimapBottom, 0.0f);
        for (int row = 0; row < this.grid.length; ++row) {
            for (int col = 0; col < this.grid[row].length; ++col) {
                ColorRGBA color = this.grid[row][col] == '#' ? new ColorRGBA(0.3f, 0.4f, 0.53f, 1.0f) : (this.grid[row][col] == 'A' ? new ColorRGBA(0.95f, 0.62f, 0.25f, 1.0f) : (this.grid[row][col] == 'G' ? new ColorRGBA(0.25f, 0.72f, 0.46f, 1.0f) : new ColorRGBA(0.82f, 0.86f, 0.89f, 1.0f)));
                float cellX = this.minimapLeft + 8.0f + (float)col * 24.0f;
                float cellY = this.minimapBottom + 8.0f + (float)(this.grid.length - 1 - row) * 24.0f;
                this.addMinimapQuad("MinimapCell_" + row + "_" + col, 22.0f, 22.0f, color, cellX + 1.0f, cellY + 1.0f, 1.0f);
            }
        }
        this.minimapCarNode = new Node("MinimapCarMarker");
        Geometry marker = new Geometry("MinimapCarArrow", (Mesh)new Quad(7.0f, 14.0f));
        Material markerMat = new Material(this.assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        markerMat.setColor("Color", new ColorRGBA(0.1f, 0.16f, 0.23f, 1.0f));
        marker.setMaterial(markerMat);
        marker.setLocalTranslation(-3.5f, -7.0f, 0.0f);
        this.minimapCarNode.attachChild((Spatial)marker);
        this.guiNode.attachChild((Spatial)this.minimapCarNode);
        this.updateMinimap();
    }

    private void addMinimapQuad(String name, float width, float height, ColorRGBA color, float x, float y, float z) {
        Geometry quad = new Geometry(name, (Mesh)new Quad(width, height));
        Material material = new Material(this.assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setColor("Color", color);
        quad.setMaterial(material);
        quad.setLocalTranslation(x, y, z);
        this.guiNode.attachChild((Spatial)quad);
    }

    private void updateMinimap() {
        if (this.minimapCarNode == null || this.carNode == null) {
            return;
        }
        Vector3f position = this.carNode.getLocalTranslation();
        float col = (position.x + 4.0f) / 2.0f;
        float row = (position.z + 4.0f) / 2.0f;
        float markerX = this.minimapLeft + 8.0f + (col + 0.5f) * 24.0f;
        float markerY = this.minimapBottom + 8.0f + ((float)this.grid.length - row - 0.5f) * 24.0f;
        this.minimapCarNode.setLocalTranslation(markerX, markerY, 2.0f);
        float screenDirX = FastMath.sin((float)this.currentYaw);
        float screenDirY = -FastMath.cos((float)this.currentYaw);
        float angle = FastMath.atan2((float)screenDirY, (float)screenDirX) - 1.5707964f;
        this.minimapCarNode.setLocalRotation(new Quaternion().fromAngleAxis(angle, Vector3f.UNIT_Z));
    }
}

