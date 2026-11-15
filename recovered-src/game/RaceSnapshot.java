/*
 * Decompiled with CFR 0.152.
 */
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public record RaceSnapshot(int tick, int winner, boolean ready, int x, int y, int facing, boolean navUsed, boolean navigating, int stunned, int cooldown, int jammed, int flash, int enemyX, int enemyY, int enemyFacing, int enemyFlash, int shots, int hits, int steps, int score, int opponentSteps, int opponentScore, String known) {
    public void write(DataOutputStream out) throws IOException {
        out.writeInt(this.tick);
        out.writeInt(this.winner);
        out.writeBoolean(this.ready);
        out.writeInt(this.x);
        out.writeInt(this.y);
        out.writeInt(this.facing);
        out.writeBoolean(this.navUsed);
        out.writeBoolean(this.navigating);
        out.writeInt(this.stunned);
        out.writeInt(this.cooldown);
        out.writeInt(this.jammed);
        out.writeInt(this.flash);
        out.writeInt(this.enemyX);
        out.writeInt(this.enemyY);
        out.writeInt(this.enemyFacing);
        out.writeInt(this.enemyFlash);
        out.writeInt(this.shots);
        out.writeInt(this.hits);
        out.writeInt(this.steps);
        out.writeInt(this.score);
        out.writeInt(this.opponentSteps);
        out.writeInt(this.opponentScore);
        out.writeUTF(this.known);
        out.flush();
    }

    public static RaceSnapshot read(DataInputStream in) throws IOException {
        return new RaceSnapshot(in.readInt(), in.readInt(), in.readBoolean(), in.readInt(), in.readInt(), in.readInt(), in.readBoolean(), in.readBoolean(), in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readUTF());
    }
}

