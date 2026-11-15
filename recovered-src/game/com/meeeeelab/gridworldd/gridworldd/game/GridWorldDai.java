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
 *  com.jme3.math.Quaternion
 *  com.jme3.math.Vector3f
 *  com.jme3.scene.Geometry
 *  com.jme3.scene.Mesh
 *  com.jme3.scene.Node
 *  com.jme3.scene.Spatial
 *  com.jme3.scene.shape.Box
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
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;

public class GridWorldDai
extends SimpleApplication
implements ActionListener {
    private Node carNode;
    private char[][] grid;
    private int carRow = 0;
    private int carCol = 0;
    private int facingRow = 0;
    private int facingCol = 1;

    public GridWorldDai() {
    }

    public GridWorldDai(AppState ... initialStates) {
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
    }

    public void onAction(String name, boolean isPressed, float tpf) {
        int step;
        if (!isPressed) {
            return;
        }
        if (name.equals("TurnLeft")) {
            int oldRow = this.facingRow;
            this.facingRow = -this.facingCol;
            this.facingCol = oldRow;
            this.updateCamera();
            return;
        }
        if (name.equals("TurnRight")) {
            int oldRow = this.facingRow;
            this.facingRow = this.facingCol;
            this.facingCol = -oldRow;
            this.updateCamera();
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
        this.carNode.setLocalTranslation(x, -0.8f, z);
        this.updateCamera();
        System.out.println("Car: " + this.carRow + ", " + this.carCol);
    }

    private void updateCamera() {
        float x = this.carCol * 2 - 4;
        float z = this.carRow * 2 - 4;
        float eyeY = 0.1f;
        this.cam.setLocation(new Vector3f(x + (float)this.facingCol * 0.25f, eyeY, z + (float)this.facingRow * 0.25f));
        this.cam.lookAt(new Vector3f(x + (float)this.facingCol * 5.0f, eyeY, z + (float)this.facingRow * 5.0f), Vector3f.UNIT_Y);
        float yaw = (float)Math.atan2(this.facingCol, this.facingRow);
        this.carNode.setLocalRotation(new Quaternion().fromAngleAxis(yaw, Vector3f.UNIT_Y));
    }
}

