/*
 * Decompiled with CFR 0.152.
 */
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RaceServer
implements AutoCloseable {
    private final ServerSocket listener;
    private final CopyOnWriteArrayList<Socket> sockets = new CopyOnWriteArrayList();
    private final CopyOnWriteArrayList<DataOutputStream> outputs = new CopyOnWriteArrayList();
    private final ArrayBlockingQueue<Input> inputs = new ArrayBlockingQueue(512);
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final RaceWorld world = new RaceWorld();
    private final Thread acceptThread;
    private final Thread tickThread;

    public RaceServer(int port) throws IOException {
        this.listener = new ServerSocket(port, 2, InetAddress.getByName("127.0.0.1"));
        this.acceptThread = Thread.ofPlatform().daemon().name("race-accept").unstarted(this::accept);
        this.tickThread = Thread.ofPlatform().daemon().name("race-tick").unstarted(this::tick);
        this.acceptThread.start();
        this.tickThread.start();
    }

    public int port() {
        return this.listener.getLocalPort();
    }

    private void accept() {
        block3: {
            try {
                for (int id = 0; id < 2 && this.running.get(); ++id) {
                    Socket socket = this.listener.accept();
                    socket.setTcpNoDelay(true);
                    this.sockets.add(socket);
                    DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                    out.writeInt(2);
                    out.writeInt(id);
                    out.flush();
                    this.outputs.add(out);
                    Thread.ofPlatform().daemon().name("race-input-" + id).start(() -> this.read(socket));
                }
            }
            catch (IOException e) {
                if (!this.running.get()) break block3;
                this.fail(e);
            }
        }
    }

    private void read(Socket socket) {
        block4: {
            try {
                DataInputStream in = new DataInputStream(socket.getInputStream());
                while (this.running.get()) {
                    int id = in.readUnsignedByte();
                    int action = in.readUnsignedByte();
                    boolean down = in.readBoolean();
                    if (id > 1 || action >= RaceWorld.Action.values().length) {
                        throw new IOException("Invalid input packet");
                    }
                    if (this.inputs.offer(new Input(id, RaceWorld.Action.values()[action], down))) continue;
                    throw new IOException("Input queue full");
                }
            }
            catch (IOException e) {
                if (!this.running.get()) break block4;
                this.fail(e);
            }
        }
    }

    private void tick() {
        try {
            long next = System.nanoTime();
            while (this.running.get()) {
                Input input;
                boolean ready;
                boolean bl = ready = this.outputs.size() == 2;
                while ((input = this.inputs.poll()) != null) {
                    if (!ready) continue;
                    this.world.input(input.id, input.action, input.down);
                }
                if (ready) {
                    this.world.step();
                }
                for (int id = 0; id < this.outputs.size(); ++id) {
                    this.world.snapshot(id, ready).write(this.outputs.get(id));
                }
                long remaining = (next += 50000000L) - System.nanoTime();
                if (remaining > 0L) {
                    TimeUnit.NANOSECONDS.sleep(remaining);
                    continue;
                }
                next = System.nanoTime();
            }
        }
        catch (IOException e) {
            if (this.running.get()) {
                this.fail(e);
            }
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        catch (RuntimeException e) {
            this.fail(e);
        }
    }

    private void fail(Exception e) {
        System.err.println("Match stopped: " + (e instanceof EOFException ? "a player disconnected" : e.getMessage()));
        this.close();
    }

    @Override
    public void close() {
        if (!this.running.getAndSet(false)) {
            return;
        }
        try {
            this.listener.close();
        }
        catch (IOException iOException) {
            // empty catch block
        }
        for (Socket socket : this.sockets) {
            try {
                socket.close();
            }
            catch (IOException iOException) {}
        }
        this.tickThread.interrupt();
        this.acceptThread.interrupt();
    }

    private record Input(int id, RaceWorld.Action action, boolean down) {
    }
}

