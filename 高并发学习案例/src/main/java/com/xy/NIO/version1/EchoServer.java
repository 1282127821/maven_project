package com.xy.NIO.version1;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @fileName:EchoServer
 * @author:xy
 * @date:2018/12/9
 * @description:
 */
@SuppressWarnings("all")
public class EchoServer {
    public static void main(String[] args) throws Exception {
        EchoServer echoServer = new EchoServer();
        echoServer.startServer();
    }

    private Selector selector;
    private ExecutorService executorService = Executors.newCachedThreadPool();
    public static Map<Socket, Long> time_stat = new HashMap<Socket, Long>(1024);

    public void startServer() throws Exception {
        selector = SelectorProvider.provider().openSelector();
        ServerSocketChannel ssc = ServerSocketChannel.open();//服务端必须使用ServerSocketChannel
        //必须是非阻塞,否则报错IllegalBlockingModeException
        ssc.configureBlocking(false);
        InetSocketAddress isa = new InetSocketAddress(InetAddress.getLocalHost(), 9999);
        ssc.socket().bind(isa);
        /*SelectionKey.OP_ACCEPT表示几种 状态的监听，
         *这个返回值selectionKey根本及没用到，貌似服务端的必须是OP_ACCEPT，否则没法连接客户端，因为你想想一开始客户端发起连接请求肯定是 可连接才行
         * 还没连接上 就没法说其他几个状态。 直接可连接才会有后续的连接状态  读写状态
         * 而对于客户端来说由于它是发起者，相当于说它默认同意了，所以他只需要监听 OP_CONNECT，或者说 你找别人求婚 只需要别人答应，你肯定默认是答应了
         * 如果一开始就是对读写有兴趣，而对 可接受没兴趣，就没法进行连接，因为selector发现 服务端只对 读写感兴趣就不会通知他了
         2018年12月12日16:15:00新的理解:
* 或者这么理解吧每一个selectionKey(channel+selector 组合主键唯一决定一个selectionKey),都有自己感兴趣的 事件(由于服务端是接受客户端的请求的，所以服务端的这个selectionKey感兴趣必须是
* OP_ACCEPT。) 而当服务端成功接受请求后，就会通过这个服务端 channel拿到客户端的channel(这么理解，既然是客户端主动发起请求肯定可以得到客户端的channel)clientChannel = server.accept();
* 然后将这个channel绑定到同一个selector,生成一个新的 SelectionKey并注册这个sk感兴趣事件， SelectionKey clientKey = clientChannel.register(selector, SelectionKey.OP_READ);
* 但是不管怎么样，服务端的这个sk感兴趣的事件必须是OP_ACCPET否则就会报错,因为如果不是那么他就无法接受客户端请求了。
         */
        SelectionKey selectionKey =
                ssc.register(selector, SelectionKey.OP_ACCEPT);
        long e = 0;
        for (; ; ) {
            /*
             *这个是为了测试 在没有客户端请求的时候唤醒selector.select()阻塞,开辟一个新线程，延迟2s后(目的让主线程阻塞住)，再唤醒,如果后面的
             * System.out.println(select+"？？？？？");能够打印 0？？？？？表示 的确是在没有任何客户端请求，让他唤醒了
             */
//            executorService.execute(new HandleMsg());
            int select = selector.select();// 阻塞,必须有客户端连接才会继续往下运行
            System.out.println("当前线程:"+Thread.currentThread().getName());//证明只有一个线程
            System.out.println(select + "？？？？？");
//        if (select==1){
//            System.out.println(select+"？？？？？");
//            return;
//        }
            Set<SelectionKey> readyKeys =
                    selector.selectedKeys(); // 多线程情况下，可能多个先都调用select()然后selectedKeys()
            Iterator<SelectionKey> iterator = readyKeys.iterator();
            System.out.println(readyKeys.size() + ">>>>>>>证明在做死循环>>>>>>>>>>>>>");
            while (iterator.hasNext()) {
                SelectionKey sk = iterator.next();
                // 必须移除掉，否则会重复处理SelectionKey
                iterator.remove(); // 注意这个remove是 迭代器提供的，而不是集合的
                if (sk.isAcceptable()) { // OP_ACCEPT  //根据状态不同做出对应操作
                    doAccpet(sk);
                } else if (sk.isValid() && sk.isReadable()) {
                    if (!time_stat.containsKey(((SocketChannel) sk.channel()).socket())) {
                        time_stat.put(((SocketChannel) sk.channel()).socket(), System.currentTimeMillis());
                        doRead(sk);
                        //这样就能让 doRead()方法的开辟的那个子线程先执行,也就是sk已经放好了，在执行主线程的下一次selector.select()
                        //这样他肯定能拿到，因为我先放入sk 再执行selector.select()
//                        Thread.sleep(2000);
                    }

                } else if (sk.isValid() && sk.isWritable()) {
                    doWrite(sk);
                    e = System.currentTimeMillis();
                    long b = time_stat.remove(((SocketChannel) sk.channel()).socket());
                    System.out.println("spend:" + (e - b) + " ms");
                }
            }
        }
    }

    private void doWrite(SelectionKey sk) {
        SocketChannel channel = (SocketChannel) sk.channel();
        EchoClient echoClient = (EchoClient) sk.attachment();
        LinkedList<ByteBuffer> outq = echoClient.getOutputQueue();
        ByteBuffer bb = outq.getLast();
        try {
            int len = channel.write(bb);
            if (len == -1) {
                disconnect(sk);
                return;
            }
            if (bb.remaining() == 0) {
                outq.removeLast();
            }

        } catch (IOException e) {
            System.out.println("错误");
            e.printStackTrace();
            //disconnect(sk);
        }
        /**
         * 这一步看似没用 其实很重要的，因为我们是当 outq 集合有数据的时候才需要 写给客户端
         * 没有数据就没必要执行doWrite,而我们又注册了感兴趣为读，会导致执行doWrite方法,然后就会去拿outq.getLast();拿不到报错
         */
        if (outq.size() == 0) {
//将客户端的那个sk 感兴趣改为OP_READ，没有数据就不用写了，所以要移除 写事件
            sk.interestOps(SelectionKey.OP_READ);
        }
    }

    private void doRead(SelectionKey sk) {
        System.out.println("进来了doRead");
        SocketChannel channel = (SocketChannel) sk.channel();
        ByteBuffer bb = ByteBuffer.allocate(8192);
        int len;
        try {
            len = channel.read(bb);
            System.out.println(len);
            if (len < 0) {
                disconnect(sk);
                return;
            }
        } catch (IOException e) {
            System.out.println("失败");
            e.printStackTrace();
            disconnect(sk);
            return;
        }
        bb.flip();
//      ByteBuffer allocate = ByteBuffer.allocate(100);
//      allocate.put(new String("nihao").getBytes());
//      allocate.flip();//有点类似flush
        executorService.execute(new HandleMsg(sk, bb));
//        new HandleMsg(sk,bb).run();
    }

    private void disconnect(SelectionKey sk) {
        sk.cancel();
        try {
            sk.channel().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void doAccpet(SelectionKey sk) {
        ServerSocketChannel server = (ServerSocketChannel) sk.channel();
        SocketChannel clientChannel;
        try {
            clientChannel = server.accept();
            clientChannel.configureBlocking(false);
            // 将客户端的那个sk 感兴趣改为OP_READ
            SelectionKey clientKey = clientChannel.register(selector, SelectionKey.OP_READ);
            EchoClient echoClient = new EchoClient();
            clientKey.attach(echoClient);
            //
            InetAddress clientAddress = clientChannel.socket().getInetAddress();
            System.out.println("与：" + clientAddress + "连接" + "验证和客户端的是不同同一个clientChannel" + clientChannel);
        } catch (IOException e) {
            System.out.println("失败");
            e.printStackTrace();
        }
    }

    /*
    业务逻辑就是放到HandleMsg处理，比如客户端按照 约定好的协议 发送数据过来，我这里利用反射技术调用 相应的类，这样就可以处理不同的业务
    而HandleMsg这个类，主要作用就是用来接收(发送)请求用的，可以理解为这是一个 boss线程。
     */
    class HandleMsg implements Runnable {
        SelectionKey sk;
        ByteBuffer bb;

        public HandleMsg(SelectionKey sk, ByteBuffer bb) {
            this.sk = sk;
            this.bb = bb;
        }

        public HandleMsg() {

        }

        @Override
        public void run() {
            //attachment()这个好理解，因为我服务的将消息 attach()到了sk，现在相当于从sk拿，注意只有服务端放入哪个sk，对应客户端才可以拿到
            EchoClient echoClient = (EchoClient) sk.attachment();
            echoClient.enqueue(bb);
            //sk.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            //作者解释明明说是注册 写 事件，但是为嘛 写了两个事件(可能是笔误，毕竟前面书上他将事件写成了 时间))，经过测试只需要 写 事件即可
            sk.interestOps(SelectionKey.OP_WRITE);
            selector.wakeup();
//            try {
//                Thread.sleep(2000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }

        }
    }
}

