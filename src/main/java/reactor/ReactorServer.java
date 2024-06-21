package reactor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

public class ReactorServer {
    public static void main(String[] args) throws IOException {
        new Thread(new MainReactor(8080)).start();
    }
}

class MainReactor implements Runnable {
    private final Selector selector;
    private final ServerSocketChannel serverSocketChannel;
    public MainReactor(int port) throws IOException{
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
            while (true){
                selector.select();
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                while (iterator.hasNext()){
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    dispatch(key);
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private void dispatch(SelectionKey key){
        Runnable handler = (Runnable) key.attachment();
        if (handler != null){
            handler.run();
        }
    }

    class Acceptor implements Runnable{

        @Override
        public void run() {
            try{
                SocketChannel socketChannel = serverSocketChannel.accept();
                if (socketChannel != null ){
                    new SubReactor(selector,socketChannel);
                }
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }
}

class SubReactor implements Runnable{

    private final Selector selector;
    private final SocketChannel socketChannel;

    public SubReactor(Selector mainSelector, SocketChannel socketChannel) throws IOException{
        this.selector = Selector.open();
        this.socketChannel = socketChannel;
        socketChannel.configureBlocking(false);
        SelectionKey selectionKey = socketChannel.register(selector, SelectionKey.OP_READ);
        selectionKey.attach(this);
        new Thread(this).start();
    }

    @Override
    public void run() {
        try{
            while (!Thread.interrupted()){
                selector.select();
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                while (iterator.hasNext()){
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    if (key.isReadable()){
                        read(key);
                    }
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private void read(SelectionKey key) throws IOException{
        SocketChannel channel = (SocketChannel)key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int read = channel.read(buffer);
        if (read > 0){
            buffer.flip();
            while (buffer.hasRemaining()){
                System.out.print((char)buffer.get());
            }
            buffer.clear();
            channel.write(ByteBuffer.wrap("ECHO: ".getBytes()));
            buffer.flip();
            channel.write(buffer);
        }else if (read == -1){
            channel.close();
        }
    }
}

