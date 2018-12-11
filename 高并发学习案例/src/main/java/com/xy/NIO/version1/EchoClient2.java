package com.xy.NIO.version1;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * @fileName:EchoClient
 * @author:xy
 * @date:2018/12/9
 * @description:
 */
public class EchoClient2 {
  public static void main(String[] args) throws IOException {
    EchoClient2 echoClient = new EchoClient2();
    InetAddress localHost = InetAddress.getLocalHost();
    echoClient.init("192.168.130.1", 9999);
    echoClient.working();
  }

  private LinkedList<ByteBuffer> outq;
  private Selector selector;

  public EchoClient2() {
    this.outq = new LinkedList<>();
  }

  public LinkedList<ByteBuffer> getOutputQueue() {
    return outq;
  }

  public void enqueue(ByteBuffer bb) {
    outq.addFirst(bb);
  }

  public void init(String ip, int port) throws IOException {
    SocketChannel channel = SocketChannel.open();
    channel.configureBlocking(false);
    this.selector = SelectorProvider.provider().openSelector();
    channel.connect(new InetSocketAddress(ip, port));
    channel.register(selector, SelectionKey.OP_CONNECT);
  }

  public void working() throws IOException {
    while (true) {
      if (!selector.isOpen()) {
        break;
      }
      selector.select();
      Iterator<SelectionKey> ite = this.selector.selectedKeys().iterator();
      while (ite.hasNext()) {
        SelectionKey key = ite.next();
        ite.remove();
        if (key.isConnectable()) {
          connect(key);
        } else if (key.isReadable()) {
          read(key);
        }
      }
    }
  }

  public void connect(SelectionKey key) throws IOException {
    SocketChannel channel = (SocketChannel) key.channel();
    if (channel.isConnectionPending()) {
      channel.finishConnect();
    }
    channel.configureBlocking(false);
//    try {
//      Thread.sleep(10000);
//    } catch (InterruptedException e) {
//      e.printStackTrace();
//    }
    channel.write(ByteBuffer.wrap(new String("hello server!\r\n").getBytes()));
    channel.register(this.selector, SelectionKey.OP_READ);
  }

  public void read(SelectionKey key) throws IOException {
    SocketChannel channel = (SocketChannel) key.channel();
    ByteBuffer buffer = ByteBuffer.allocate(100);
    channel.read(buffer);
    byte[] data = buffer.array();
    String msg = new String(data).trim();
    System.out.println("客户端收到消息：" + msg);
    System.out.println("验证和客户端的是不同同一个clientChannel"+channel);
    channel.close();
    key.selector().close();
  }
}
