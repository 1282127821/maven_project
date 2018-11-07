package com.xy.threadlocal;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * @fileName:StrongRefenceTest
 * @author:xy
 * @date:2018/11/4
 * @description:
 */
public class StrongRefenceTest {
    private Product product=new Product("iPhone");
    private Product product2=new Product("小米");
    WeakHashMap<Product, Integer> weakHashMap= new WeakHashMap<>();
    Map<Product, Integer> map = new HashMap<>();
    @Test
    public void test(){

        map.put(product, 111);
        product=null;
        System.gc();
        /*
        对象仍然存在！,说明虽然product=null但是 map内部还是有该对象，也就是product=null仅仅断开了该引用与对象的联系，而没有触发该对象回收操作
        本来我们product=null;一般是会触发 他所引用的对象的垃圾回收操作的
         */
        map.keySet().forEach(System.out::println);
    }

  /**
   * 弱引用与软引用的区别在于：只具有弱引用的对象拥有更短暂的生命周期。如果一个对象只具有弱引用，只要垃圾回收器在内存空间检测到了，
   * 不管当前内存空间足够与否，都会回收对应的内存。不过，由于垃圾回收器是一个优先级很低的线程，因此不一定会很快发现那些只具有弱引用的对象。
   */
  @Test
  public void weakRef_2() {

//      weakHashMap.put(product, 111);
      weakHashMap.put(product2, 222);

        product2 = null;// 关键,这个是标记他需要被回收
         System.gc();//关键，这个是立刻触发gc操作，因为 垃圾回收器 是不定时运行，所以手动触发gc

      weakHashMap.keySet().forEach(System.out::println);// 不gc()的话，还能访问对象，否则null。
System.out.println("branches");
System.out.println("branches");
System.out.println("master");
    }
}
