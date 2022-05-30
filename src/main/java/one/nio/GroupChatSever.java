package one.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;

/**
 * @Auther: 18517
 * @Date: 2022/5/30 19:50
 * @Description:
 */
//服务器端
public class GroupChatSever {
    private final static int PORT = 6668;//监听端口
    private Selector selector;//选择器
    private ServerSocketChannel serverSocketChannel;

    public GroupChatSever(){
        try{
            selector = Selector.open();//开启选择器
            serverSocketChannel = ServerSocketChannel.open();//开启通道
            serverSocketChannel.configureBlocking(false);//将通道设为非阻塞状态
            serverSocketChannel.socket().bind(new InetSocketAddress(PORT));//通道绑定监听端口
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);//将通道注册到选择器上，事件类型为接收
            listen();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    //对端口进行监听
    public void listen(){
        try {
            while (true){
                //检查注册通道是否有事件发生，检查时长为2秒
                int count = selector.select(2000);
                if (count > 0){//如果注册通道有事件发生则进行处理
                    //获取所有发生事件的通道对应的SelectionKey
                    Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                    while (keyIterator.hasNext()){
                        SelectionKey key = keyIterator.next();
                        if (key.isAcceptable()){//判断该key对应的通道是否需进行接收操作
                            //虽然accept()方法是阻塞的，但是因为对通道进行过判断，
                            //可以确定是有客户端连接的，所以此时调用accept并不会阻塞
                            SocketChannel socketChannel = serverSocketChannel.accept();
                            socketChannel.configureBlocking(false);
                            //接收后，将获取的客户端通道注册到选择器上，事件类型为读
                            socketChannel.register(selector, SelectionKey.OP_READ);
                            System.out.println(socketChannel.getRemoteAddress() + "上线!");
                        }
                        if (key.isReadable()){//判断该key对应的通道是否需进行读操作
                            readFromClient(key);
                        }
                        //注意当处理完一个通道key时，需将它从迭代器中移除
                        keyIterator.remove();
                    }
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    /**
     * 读取客户端发来的消息
     * @param key 需读取的通道对应的SelectionKey
     */
    public void readFromClient(SelectionKey key){
        SocketChannel socketChannel = null;
        try{
            //通过SelectionKey获取对应通道
            socketChannel = (SocketChannel)key.channel();
            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
            int read = socketChannel.read(byteBuffer);
            if (read > 0){
                String message = new String(byteBuffer.array());
                System.out.println("客户端: " + message);
                sendToOtherClient(message, socketChannel);
            }
        }catch (IOException e){
            //这里做了简化，将所有异常都当做是客户端断开连接触发的异常，实际项目中请不要这样做
            try{
                System.out.println(socketChannel.getRemoteAddress() + "下线");
                key.cancel();//将该SelectionKey撤销
                socketChannel.close();//再关闭对应通道
            }catch (IOException e2){
                e2.printStackTrace();
            }
        }
    }

    /**
     * 将客户端发送的消息转发到其他客户端
     * @param message 转发的消息
     * @param from 发送消息的客户端通道
     * @throws IOException
     */
    public void sendToOtherClient(String message, SocketChannel from) throws IOException{
        System.out.println("消息转发中......");
        for (SelectionKey key : selector.keys()){//遍历选择器中所有SelectionKey
            Channel channel = key.channel();//根据SelectionKey获取对应通道
            //排除掉发送消息的通道，将消息写入到其他客户端通道
            if (channel instanceof SocketChannel && channel != from){
                SocketChannel socketChannel = (SocketChannel)channel;
                ByteBuffer byteBuffer = ByteBuffer.wrap(message.getBytes());
                socketChannel.write(byteBuffer);
            }
        }
    }

    public static void main(String[] args) {
        GroupChatSever groupChatSever = new GroupChatSever();
    }
}