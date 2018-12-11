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
        EchoServer echoServer=new EchoServer();
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
     */
    SelectionKey selectionKey =
        ssc.register(selector, SelectionKey.OP_ACCEPT);
    long e = 0;
    for (; ; ) {
        int select = selector.select();// 阻塞,必须有客户端连接才会继续往下运行
        System.out.println(select+"？？？？？");
//        if (select==1){
//            System.out.println(select+"？？？？？");
//            return;
//        }
        Set<SelectionKey> readyKeys =
          selector.selectedKeys(); // 多线程情况下，可能多个先都调用select()然后selectedKeys()
      Iterator<SelectionKey> iterator = readyKeys.iterator();
//      System.out.println(readyKeys.size()+">>>>>>>证明在做死循环>>>>>>>>>>>>>");
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
          if (len==-1){
              disconnect(sk);
              return;
          }
          if (bb.remaining()==0){
              outq.removeLast();
          }

      } catch (IOException e) {
      System.out.println("错误");
          e.printStackTrace();
          //disconnect(sk);
      }
      if (outq.size()==0){
          sk.interestOps(SelectionKey.OP_READ);
      }

  }

  private void doRead(SelectionKey sk) {
    SocketChannel channel = (SocketChannel) sk.channel();
    ByteBuffer bb = ByteBuffer.allocate(8192);
    int len;
    try {
      len = channel.read(bb);
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
    executorService.execute(new HandleMsg(sk,bb));
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
      // 修改selector感兴趣的状态，最开始是OP_ACCPET,注意还是那个selector
      SelectionKey clientKey = clientChannel.register(selector, SelectionKey.OP_READ);
      EchoClient echoClient = new EchoClient();
      clientKey.attach(echoClient);
      //
      InetAddress clientAddress = clientChannel.socket().getInetAddress();
      System.out.println("与：" + clientAddress + "连接"+"验证和客户端的是不同同一个clientChannel"+clientChannel);
    } catch (IOException e) {
      System.out.println("失败");
      e.printStackTrace();
    }
  }
/*
业务逻辑就是放到HandleMsg处理，比如客户端按照 约定好的协议 发送数据过来，我这里利用反射技术调用 相应的类，这样就可以处理不同的业务
而HandleMsg这个类，主要作用就是用来接收(发送)请求用的，可以理解为这是一个 boss线程。
 */
    class HandleMsg implements Runnable{
        SelectionKey sk;
        ByteBuffer bb;

        public HandleMsg(SelectionKey sk, ByteBuffer bb) {
            this.sk = sk;
            this.bb = bb;
        }

        @Override
        public void run() {
            //attachment()这个好理解，因为我服务的将消息 attach()到了sk，现在相当于从sk拿，注意只有服务端放入哪个sk，对应客户端才可以拿到
            EchoClient echoClient= (EchoClient) sk.attachment();
            echoClient.enqueue(bb);
            sk.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            selector.wakeup();
        }
    }
}

