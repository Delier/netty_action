package reactor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;

public class SingleReactorPattern {
    public static void main(String[] args) throws IOException{
        new Thread(new Reactor(8080)).start();
    }
}

class Reactor implements Runnable{

    private final Selector selector;

    private final ServerSocketChannel serverSocketChannel;

    public Reactor(int port) throws IOException{
        selector = Selector.open();
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().bind(new InetSocketAddress(port));
        serverSocketChannel.configureBlocking(false);

        SelectionKey selectionKey = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        selectionKey.attach(new Acceptor());

    }

    @Override
    public void run() {
        try{
            while (!Thread.interrupted()){
                selector.select();
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()){
                    dispatch(iterator.next());
                    iterator.remove();
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private void dispatch(SelectionKey key){
        Runnable r = (Runnable)key.attachment();
        if (r != null){
            r.run();
        }
    }

    class Acceptor implements Runnable{

        @Override
        public void run() {
            try{
                SocketChannel socketChannel = serverSocketChannel.accept();
                if (socketChannel != null){
                    new Handler(selector,socketChannel);
                }

            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }
}

class Handler implements Runnable{
    private final SocketChannel socketChannel;
    private final SelectionKey selectionKey;
    private final ByteBuffer buffer = ByteBuffer.allocate(1024);
    private static final int READING = 0, SENDING = 1;
    private int state = READING;

    public Handler(Selector selector,SocketChannel socketChannel) throws IOException{
        this.socketChannel = socketChannel;
        socketChannel.configureBlocking(false);
        selectionKey = socketChannel.register(selector, 0);
        selectionKey.attach(this);
        selectionKey.interestOps(SelectionKey.OP_READ);
        selector.wakeup();
    }


    @Override
    public void run() {
        try{
            if (state == READING){
                read();
            }else if (state == SENDING){
                send();
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private void read() throws IOException{
        int read = socketChannel.read(buffer);
        if (read > 0){
            buffer.flip();
            process();
            state = SENDING;
            selectionKey.interestOps(SelectionKey.OP_WRITE);
        }else if (read == -1){
            socketChannel.close();
        }
    }

    private void send() throws IOException{
        socketChannel.write(buffer);
        if (!buffer.hasRemaining()){
            buffer.clear();
            state = READING;
            selectionKey.interestOps(SelectionKey.OP_READ);
        }
    }

    private void process(){
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        buffer.clear();
        buffer.put(data);
        buffer.flip();
    }
}


