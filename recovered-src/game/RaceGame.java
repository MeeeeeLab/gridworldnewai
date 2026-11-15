/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.jme3.app.SimpleApplication
 *  com.jme3.font.BitmapText
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
 *  com.jme3.renderer.RenderManager
 *  com.jme3.scene.Geometry
 *  com.jme3.scene.Mesh
 *  com.jme3.scene.Node
 *  com.jme3.scene.Spatial
 *  com.jme3.scene.Spatial$CullHint
 *  com.jme3.scene.shape.Box
 *  com.jme3.scene.shape.Quad
 */
import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapText;
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
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Quad;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class RaceGame
extends SimpleApplication {
    private static final float CELL = 2.5f;
    private static final float TILE = 6.0f;
    private static final ColorRGBA SCOUT = new ColorRGBA(0.12f, 0.9f, 0.87f, 1.0f);
    private static final ColorRGBA TANK = new ColorRGBA(1.0f, 0.56f, 0.22f, 1.0f);
    private final RaceConnection connection;
    private final Runnable onViewReady;
    private final Map<Integer, Consumer<Boolean>> keyHandlers = new HashMap<Integer, Consumer<Boolean>>();
    private boolean readinessReported;
    private final Geometry[] mapTiles = new Geometry[625];
    private final Node enemy = new Node("opponent");
    private final Node effects = new Node("attacks");
    private final Node mini = new Node("minimap");
    private final char[] drawn = new char[625];
    private BitmapText status;
    private BitmapText nav;
    private BitmapText weapon;
    private BitmapText result;
    private BitmapText clock;
    private BitmapText position;
    private BitmapText scoring;
    private BitmapText breakdown;
    private Geometry selfDot;
    private Geometry enemyDot;
    private float mapLeft;
    private float mapBottom;
    private boolean placed;
    private int lastTick = -1;

    public RaceGame(RaceConnection connection, Runnable onViewReady) {
        this.connection = connection;
        this.onViewReady = onViewReady;
    }

    public void simpleRender(RenderManager manager) {
        if (this.placed && !this.readinessReported) {
            this.onViewReady.run();
            this.readinessReported = true;
        }
    }

    public void handleError(String message, Throwable failure) {
        System.err.println("Player " + (this.connection.id + 1) + " window failed: " + message);
        if (failure != null) {
            failure.printStackTrace();
        }
        this.connection.close();
        System.exit(1);
    }

    private ColorRGBA color(int id) {
        return id == 0 ? SCOUT : TANK;
    }

    public void simpleInitApp() {
        this.flyCam.setEnabled(false);
        this.setDisplayFps(false);
        this.setDisplayStatView(false);
        this.setPauseOnLostFocus(false);
        this.inputManager.setCursorVisible(true);
        this.viewPort.setBackgroundColor(new ColorRGBA(0.065f, 0.09f, 0.14f, 1.0f));
        this.cam.setFrustumPerspective(78.0f, (float)this.cam.getWidth() / (float)this.cam.getHeight(), 0.08f, 24.0f);
        AmbientLight ambient = new AmbientLight();
        ambient.setColor(ColorRGBA.White.mult(0.7f));
        this.rootNode.addLight((Light)ambient);
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-1.0f, -3.0f, -2.0f).normalizeLocal());
        this.rootNode.addLight((Light)sun);
        this.rootNode.attachChild((Spatial)this.enemy);
        this.rootNode.attachChild((Spatial)this.effects);
        this.createOpponent();
        this.hud();
        this.controls();
        this.inputManager.addListener((InputListener)((ActionListener)(name, down, tpf) -> {
            if (down) {
                System.out.println("Player " + (this.connection.id + 1) + " requested exit with Esc");
            }
        }), new String[]{"SIMPLEAPP_Exit"});
    }

    private Material material(ColorRGBA color, boolean lit) {
        Material m = new Material(this.assetManager, lit ? "Common/MatDefs/Light/Lighting.j3md" : "Common/MatDefs/Misc/Unshaded.j3md");
        if (lit) {
            m.setBoolean("UseMaterialColors", true);
            m.setColor("Diffuse", color);
            m.setColor("Ambient", color);
        } else {
            m.setColor("Color", color);
        }
        return m;
    }

    private Geometry box(Node parent, String name, float x, float y, float z, float sx, float sy, float sz, ColorRGBA color, boolean lit) {
        Geometry g = new Geometry(name, (Mesh)new Box(sx, sy, sz));
        g.setMaterial(this.material(color, lit));
        g.setLocalTranslation(x, y, z);
        parent.attachChild((Spatial)g);
        return g;
    }

    private Geometry rect(Node parent, float x, float y, float w, float h, ColorRGBA color) {
        Geometry g = new Geometry("panel", (Mesh)new Quad(w, h));
        g.setMaterial(this.material(color, false));
        g.setLocalTranslation(x, y, 0.0f);
        parent.attachChild((Spatial)g);
        return g;
    }

    private BitmapText label(String text, float x, float y, int size, ColorRGBA color) {
        BitmapText t = new BitmapText(this.guiFont);
        t.setText(text);
        t.setSize((float)size);
        t.setColor(color);
        t.setLocalTranslation(x, y, 2.0f);
        this.guiNode.attachChild((Spatial)t);
        return t;
    }

    private void hud() {
        float w = this.cam.getWidth();
        float h = this.cam.getHeight();
        ColorRGBA panel = new ColorRGBA(0.025f, 0.04f, 0.07f, 1.0f);
        ColorRGBA muted = new ColorRGBA(0.55f, 0.65f, 0.72f, 1.0f);
        this.rect(this.guiNode, 16.0f, h - 94.0f, 285.0f, 78.0f, panel);
        this.rect(this.guiNode, 16.0f, h - 94.0f, 3.0f, 78.0f, this.color(this.connection.id));
        this.label(this.connection.id == 0 ? "01 / SCOUT" : "02 / SIEGE", 30.0f, h - 28.0f, 24, this.color(this.connection.id));
        this.label(this.connection.id == 0 ? "MOBILITY  //  EMP PULSE" : "ARMOR  //  RAIL CANNON", 30.0f, h - 59.0f, 12, muted);
        this.clock = this.label("", w - 188.0f, h - 30.0f, 15, ColorRGBA.White);
        this.position = this.label("", 30.0f, h - 81.0f, 12, muted);
        this.mapLeft = w - 190.0f;
        this.mapBottom = h - 270.0f;
        this.rect(this.guiNode, this.mapLeft - 12.0f, this.mapBottom - 37.0f, 176.0f, 210.0f, panel);
        this.label("EXPLORATION", this.mapLeft, this.mapBottom + 162.0f, 14, this.color(this.connection.id));
        this.guiNode.attachChild((Spatial)this.mini);
        for (int y = 0; y < 25; ++y) {
            for (int x = 0; x < 25; ++x) {
                this.mapTiles[y * 25 + x] = this.rect(this.mini, this.mapLeft + (float)x * 6.0f, this.mapBottom + (float)(24 - y) * 6.0f, 5.0f, 5.0f, new ColorRGBA(0.07f, 0.09f, 0.12f, 1.0f));
            }
        }
        this.selfDot = this.rect(this.mini, 0.0f, 0.0f, 5.0f, 5.0f, this.color(this.connection.id));
        this.selfDot.setLocalTranslation(0.0f, 0.0f, 1.0f);
        this.enemyDot = this.rect(this.mini, 0.0f, 0.0f, 5.0f, 5.0f, this.color(1 - this.connection.id));
        this.enemyDot.setLocalTranslation(0.0f, 0.0f, 1.0f);
        this.label("DARK = UNKNOWN / COLOR = TRAIL", this.mapLeft - 3.0f, this.mapBottom - 10.0f, 9, muted);
        this.label("GREEN = SHARED EXIT", this.mapLeft, this.mapBottom - 25.0f, 10, ColorRGBA.Green);
        this.rect(this.guiNode, 16.0f, 16.0f, w - 32.0f, 105.0f, panel);
        this.rect(this.guiNode, 16.0f, 119.0f, w - 32.0f, 2.0f, this.color(this.connection.id));
        this.status = this.label("WAITING FOR BOTH PLAYERS", 30.0f, 104.0f, 16, ColorRGBA.White);
        this.nav = this.label("", 30.0f, 78.0f, 14, this.color(this.connection.id));
        this.weapon = this.label("", 30.0f, 54.0f, 13, muted);
        this.label("P1 W/S drive A/D turn SPACE fire N auto", 30.0f, 34.0f, 11, SCOUT);
        this.label("P2  ARROWS drive/turn  ENTER cannon  M auto", w - 344.0f, 34.0f, 11, TANK);
        this.label("+", w / 2.0f - 6.0f, h / 2.0f + 8.0f, 24, this.color(this.connection.id));
        this.result = this.label("", w / 2.0f - 220.0f, h / 2.0f + 70.0f, 22, ColorRGBA.White);
        this.breakdown = this.label("", w / 2.0f - 220.0f, h / 2.0f + 40.0f, 16, ColorRGBA.White);
        this.rect(this.guiNode, 16.0f, h - 160.0f, 285.0f, 56.0f, panel);
        this.scoring = this.label("", 30.0f, h - 121.0f, 18, this.color(this.connection.id));
        this.label("-10 / cell   |   shortest route, higher rating", 30.0f, h - 146.0f, 11, muted);
        this.rect(this.guiNode, w / 2.0f - 48.0f, 122.0f, 96.0f, 17.0f, this.color(this.connection.id).mult(0.45f));
        this.rect(this.guiNode, w / 2.0f - 12.0f, 139.0f, 24.0f, this.connection.id == 0 ? 30.0f : 60.0f, this.color(this.connection.id).mult(0.7f));
    }

    private void controls() {
        this.bind(0, RaceWorld.Action.FORWARD, 17);
        this.bind(0, RaceWorld.Action.BACK, 31);
        this.bind(0, RaceWorld.Action.TURN_LEFT, 30);
        this.bind(0, RaceWorld.Action.TURN_RIGHT, 32);
        this.bind(0, RaceWorld.Action.FIRE, 57);
        this.bind(0, RaceWorld.Action.NAV, 49);
        this.bind(1, RaceWorld.Action.FORWARD, 200);
        this.bind(1, RaceWorld.Action.BACK, 208);
        this.bind(1, RaceWorld.Action.TURN_LEFT, 203);
        this.bind(1, RaceWorld.Action.TURN_RIGHT, 205);
        this.bind(1, RaceWorld.Action.FIRE, 28);
        this.bind(1, RaceWorld.Action.NAV, 50);
    }

    private void bind(int id, RaceWorld.Action action, int key) {
        String name = id + ":" + String.valueOf((Object)action);
        this.inputManager.addMapping(name, new Trigger[]{new KeyTrigger(key)});
        this.keyHandlers.put(key, down -> this.connection.send(id, action, (boolean)down));
        this.inputManager.addListener((InputListener)((ActionListener)(n, down, tpf) -> this.handleKey(key, down)), new String[]{name});
    }

    public void handleKey(int key, boolean down) {
        Consumer<Boolean> handler = this.keyHandlers.get(key);
        if (handler != null) {
            handler.accept(down);
        }
    }

    public void loseFocus() {
        this.connection.releaseAll();
        super.loseFocus();
    }

    private void createOpponent() {
        this.enemy.attachChild((Spatial)RaceVehicles.create(this.assetManager, 1 - this.connection.id));
    }

    private void terrain(RaceSnapshot s) {
        for (int i = 0; i < this.drawn.length; ++i) {
            char tile = s.known().charAt(i);
            if (tile == this.drawn[i]) continue;
            this.drawn[i] = tile;
            if (tile == '?') continue;
            boolean newlyDiscovered = this.mapTiles[i].getUserData("discovered") == null;
            this.mapTiles[i].setUserData("discovered", (Object)true);
            if (!newlyDiscovered) {
                this.mapTiles[i].setMaterial(this.material(this.color(this.connection.id).mult(0.55f), false));
                continue;
            }
            int x = i % 25;
            int y = i / 25;
            ColorRGBA floor = new ColorRGBA(0.17f, 0.21f, 0.25f, 1.0f);
            if ((x + y) % 2 == 0) {
                floor = floor.mult(0.82f);
            }
            this.box(this.rootNode, "floor", (float)x * 2.5f, -0.08f, (float)y * 2.5f, 1.25f, 0.08f, 1.25f, floor, true);
            if (tile == '#') {
                ColorRGBA wall = x < 12 ? new ColorRGBA(0.19f, 0.32f, 0.4f, 1.0f) : new ColorRGBA(0.39f, 0.29f, 0.22f, 1.0f);
                this.box(this.rootNode, "wall", (float)x * 2.5f, 1.8f, (float)y * 2.5f, 1.24f, 1.8f, 1.24f, wall, true);
                this.box(this.rootNode, "wall trim", (float)x * 2.5f, 0.3f, (float)y * 2.5f, 1.253f, 0.025f, 1.253f, this.color(x < 12 ? 0 : 1).mult(0.5f), false);
            } else if (tile == 'G') {
                this.box(this.rootNode, "exit pad", (float)x * 2.5f, 0.035f, (float)y * 2.5f, 1.1f, 0.035f, 1.1f, ColorRGBA.Green, false);
                this.box(this.rootNode, "exit beacon", (float)x * 2.5f, 2.5f, (float)y * 2.5f, 0.18f, 2.5f, 0.18f, new ColorRGBA(0.3f, 1.0f, 0.4f, 1.0f), false);
            }
            this.mapTiles[i].setMaterial(this.material(tile == '#' ? new ColorRGBA(0.35f, 0.42f, 0.49f, 1.0f) : (tile == 'G' ? ColorRGBA.Green : (tile == ',' ? this.color(this.connection.id).mult(0.55f) : new ColorRGBA(0.15f, 0.23f, 0.29f, 1.0f))), false));
        }
    }

    public void simpleUpdate(float tpf) {
        if (this.connection.error != null) {
            this.status.setText(this.connection.error);
            return;
        }
        RaceSnapshot s = this.connection.latest;
        if (s == null) {
            return;
        }
        if (s.tick() != this.lastTick || !this.placed) {
            this.terrain(s);
            this.lastTick = s.tick();
        }
        Vector3f target = new Vector3f((float)s.x() * 2.5f, 1.4f, (float)s.y() * 2.5f);
        float blend = 1.0f - (float)Math.exp(-12.0f * tpf);
        if (!this.placed) {
            this.cam.setLocation(target);
            this.placed = true;
        } else {
            this.cam.getLocation().interpolateLocal(target, blend);
        }
        Quaternion rotation = new Quaternion().lookAt(new Vector3f((float)RaceWorld.DX[s.facing()], 0.0f, (float)RaceWorld.DY[s.facing()]), Vector3f.UNIT_Y);
        this.cam.getRotation().slerp(rotation, blend);
        this.cam.setRotation(this.cam.getRotation());
        this.cam.setLocation(this.cam.getLocation());
        this.enemy.setCullHint(s.enemyX() < 0 ? Spatial.CullHint.Always : Spatial.CullHint.Inherit);
        if (s.enemyX() >= 0) {
            this.enemy.setLocalTranslation((float)s.enemyX() * 2.5f, 0.0f, (float)s.enemyY() * 2.5f);
            this.enemy.setLocalRotation(new Quaternion().fromAngleAxis((float)(-s.enemyFacing()) * 1.5707964f, Vector3f.UNIT_Y));
        }
        this.mini.setCullHint(s.jammed() > 0 ? Spatial.CullHint.Always : Spatial.CullHint.Inherit);
        this.selfDot.setLocalTranslation(this.mapLeft + (float)s.x() * 6.0f, this.mapBottom + (float)(24 - s.y()) * 6.0f, 1.0f);
        this.enemyDot.setCullHint(s.enemyX() < 0 ? Spatial.CullHint.Always : Spatial.CullHint.Inherit);
        this.enemyDot.setLocalTranslation(this.mapLeft + (float)s.enemyX() * 6.0f, this.mapBottom + (float)(24 - s.enemyY()) * 6.0f, 1.0f);
        this.clock.setText(String.format("RACE  %02d:%02d", s.tick() / 20 / 60, s.tick() / 20 % 60));
        this.position.setText("SECTOR " + s.x() + " / " + s.y() + "   HEADING " + "NESW".charAt(s.facing()));
        this.status.setText((String)(!s.ready() ? "WAITING FOR BOTH PLAYERS" : (s.stunned() > 0 ? "DISABLED  //  " + this.seconds(s.stunned()) + "s" : (s.jammed() > 0 ? "EMP INTERFERENCE  //  MINIMAP OFFLINE" : "REACH THE GREEN BEACON IN THE CENTRAL ARENA"))));
        this.nav.setText((String)(s.navigating() ? "AUTO PILOT ACTIVE  //  1s burst, hits cancel navigation" : (s.navUsed() ? "AUTO PILOT SPENT  //  manual control" : "AUTO PILOT  1 / 1  //  max 1s  //  " + (this.connection.id == 0 ? "N" : "M") + " to activate")));
        this.weapon.setText((this.connection.id == 0 ? "EMP / range 3 cells" : "RAIL / range 8 cells") + "    " + (String)(s.cooldown() == 0 ? "READY" : "cooldown " + this.seconds(s.cooldown()) + "s") + "    HITS " + s.hits() + " / SHOTS " + s.shots());
        this.scoring.setText("STEPS " + s.steps() + "    RATING " + s.score());
        if (s.winner() >= 0) {
            this.result.setText(s.winner() == this.connection.id ? "VICTORY / EXIT REACHED" : "DEFEAT / OPPONENT FINISHED");
            this.breakdown.setText("YOU " + s.steps() + " steps / " + s.score() + " pts\nRIVAL " + s.opponentSteps() + " steps / " + s.opponentScore() + " pts");
        }
        this.effects.detachAllChildren();
        if (s.flash() > 0) {
            this.attackEffect(this.connection.id, s.x(), s.y(), s.facing());
        }
        if (s.enemyFlash() > 0 && s.enemyX() >= 0) {
            this.attackEffect(1 - this.connection.id, s.enemyX(), s.enemyY(), s.enemyFacing());
        }
    }

    private String seconds(int ticks) {
        return String.format("%.1f", Float.valueOf((float)ticks / 20.0f));
    }

    private void attackEffect(int id, int x, int y, int facing) {
        if (id == 0) {
            for (int d = 0; d < 4; ++d) {
                this.box(this.effects, "EMP pulse", (float)x * 2.5f + (float)RaceWorld.DX[d] * 1.4f, 0.9f, (float)y * 2.5f + (float)RaceWorld.DY[d] * 1.4f, 0.16f, 0.6f, 0.16f, SCOUT, false);
            }
        } else {
            for (int distance = 1; distance <= 8; ++distance) {
                char tile;
                int cx = x + RaceWorld.DX[facing] * distance;
                int cy = y + RaceWorld.DY[facing] * distance;
                if (cx >= 0 && cy >= 0 && cx < 25 && cy < 25 && (tile = this.drawn[cy * 25 + cx]) != '#' && tile != '?') {
                    this.box(this.effects, "rail trail", (float)cx * 2.5f, 1.12f, (float)cy * 2.5f, facing % 2 == 1 ? 1.25f : 0.045f, 0.045f, facing % 2 == 0 ? 1.25f : 0.045f, TANK, false);
                    continue;
                }
                break;
            }
        }
    }

    public boolean isViewReady() {
        return this.placed;
    }

    public void verifyView() {
        RaceSnapshot s = this.connection.latest;
        if (this.enemy.getTriangleCount() < 200) {
            throw new AssertionError((Object)"Vehicle model missing");
        }
        if (!this.placed || s == null || s.known().chars().filter(c -> c == 63).count() == 0L) {
            throw new AssertionError((Object)"Missing view/fog");
        }
        if (Math.abs(this.cam.getLocation().y - 1.4f) > 0.01f || this.rootNode.getChild("floor") == null) {
            throw new AssertionError((Object)"First-person scene missing");
        }
        if (this.mini.getChildren().size() != 627) {
            throw new AssertionError((Object)"Minimap missing");
        }
    }

    public void destroy() {
        System.out.println("Player " + (this.connection.id + 1) + " window closed; first frame rendered=" + this.readinessReported);
        this.connection.close();
        super.destroy();
    }
}

