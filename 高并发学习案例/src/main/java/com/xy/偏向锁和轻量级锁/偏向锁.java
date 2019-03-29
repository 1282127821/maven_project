package com.xy.偏向锁和轻量级锁;

import java.util.List;
import java.util.Vector;

/**
 * @fileName:aaa
 * @author:xy
 * @date:2019/3/24
 * @description:
 */
public class 偏向锁 {
    /** 这里要说的是，Vector的add是一个线程安全防方法 不开启偏向锁2800-3000 开启后 2600-2800*/
    public static List<Integer> numberList = new Vector<Integer>();
    public static void main(String[] args) {
        long begin = System.currentTimeMillis();
        int count = 0;
        int startnum = 0;
        while(count<10000000){
            numberList.add(startnum);
            startnum+=2;
            count++;
        }
        long end = System.currentTimeMillis();
        System.out.println(end-begin);
    }
}
