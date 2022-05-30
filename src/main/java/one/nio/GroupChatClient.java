package one.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Scanner;

/**
 * @Auther: 18517
 * @Date: 2022/5/30 19:52
 * @Description:
 */
//客户端
public class GroupChatClient {
    private final static String SEVER_HOST = "127.0.0.1";//连接的客户端主机
    private final static int SEVER_PORT = 6668;//连接的客户端端口
    private Selector selector;//选择器
    private SocketChannel socketChannel;
    private String username;//储存客户端ip地址

    public GroupChatClient(){
        try {
            selector = Selector.open();//开启选择器
            socketChannel = SocketChannel.open(new InetSocketAddress(SEVER_HOST, SEVER_PORT));//开启通道
            socketChannel.configureBlocking(false);//将通道设为非阻塞
            socketChannel.register(selector, SelectionKey.OP_READ);//将通道注册在选择器上，事件类型为读
            username = socketChannel.getLocalAddress().toString().substring(1);//获取客户端ip地址
            String message = " 进入聊天群!";
            sendMessage(message);
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    //发送消息
    public void sendMessage(String message){
        message = username+": "+message;
        try{
            ByteBuffer byteBuffer = ByteBuffer.wrap(message.getBytes());
            socketChannel.write(byteBuffer);
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    //读取从服务器转发送过来的消息
    public void readMessage(){
        try{
            int read = selector.select();
            if (read > 0){
                Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                while (keyIterator.hasNext()){
                    SelectionKey key = keyIterator.next();
                    if (key.isReadable()){
                        SocketChannel socketChannel = (SocketChannel)key.channel();
                        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                        socketChannel.read(byteBuffer);
                        System.out.println(new String(byteBuffer.array()));
                    }
                    keyIterator.remove();
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        final GroupChatClient groupChatClient = new GroupChatClient();
        //客户端开启一个线程来监听是否有服务器转发来消息
        new Thread(){
            @Override
            public void run() {
                while (true){
                    groupChatClient.readMessage();
                    try {
                        sleep(1000);
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
            }
        }.start();

        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()){
            String message = scanner.nextLine();
            groupChatClient.sendMessage(message);
        }
    }
}