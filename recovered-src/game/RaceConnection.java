/*
 * Decompiled with CFR 0.152.
 */
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public final class RaceConnection
implements AutoCloseable {
    private final Socket socket = new Socket();
    private final DataOutputStream out;
    public final int id;
    public volatile RaceSnapshot latest;
    public volatile String error;

    public RaceConnection(int port) throws IOException {
        this.socket.connect(new InetSocketAddress("127.0.0.1", port), 5000);
        this.socket.setTcpNoDelay(true);
        this.socket.setSoTimeout(10000);
        DataInputStream in = new DataInputStream(new BufferedInputStream(this.socket.getInputStream()));
        if (in.readInt() != 2) {
            throw new IOException("Protocol version mismatch");
        }
        this.id = in.readInt();
        this.out = new DataOutputStream(this.socket.getOutputStream());
        Thread.ofPlatform().daemon().name("race-snapshots").start(() -> {
            block3: {
                try {
                    while (!this.socket.isClosed()) {
                        this.latest = RaceSnapshot.read(in);
                    }
                }
                catch (IOException e) {
                    if (this.socket.isClosed()) break block3;
                    this.error = "Connection lost. Restart the demo.";
                }
            }
        });
    }

    public synchronized void send(int target, RaceWorld.Action action, boolean down) {
        if (this.error != null || this.socket.isClosed()) {
            return;
        }
        try {
            this.out.writeByte(target);
            this.out.writeByte(action.ordinal());
            this.out.writeBoolean(down);
            this.out.flush();
        }
        catch (IOException e) {
            this.error = "Connection lost. Restart the demo.";
        }
    }

    public void releaseAll() {
        for (int id = 0; id < 2; ++id) {
            for (RaceWorld.Action action : RaceWorld.Action.values()) {
                this.send(id, action, false);
            }
        }
    }

    @Override
    public void close() {
        try {
            this.socket.close();
        }
        catch (IOException iOException) {
            // empty catch block
        }
    }
}

