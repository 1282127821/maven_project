package com;

import javafx.scene.control.Cell;

import java.util.concurrent.atomic.LongAccumulator;

/**
 * @fileName:Long
 * @author:xy
 * @date:2018/12/16
 * @description:
 */
@SuppressWarnings("all")
public class LongAccumulatorTest {
    public static void main(String[] args) throws InterruptedException {
    /**一个大前提  LongAccumulator只适合 重复使用某一个公式运算的那种  比如重复 x*y  首先计算出初始值和和x的结果，然后将结果和y相乘
     * 其实这个使用起来不难，通过LongAccumulator 的构造方法传递一个对象(一个具体做什么操作的对象)，以及一个基础值(base从多少开始)
     * 然后计算的时候不再是加法，而是通过我们构造指定那个重写的方法，进行操作，function.applyAsLong就是调用我们重写的方法 注意他有两个参数 ，其中第二个参数就是
     * 我们调用accumulate(long x)传递的参数，第一个参数就是 之前计算的结果值
     *
     *
     *        public void accumulate(long x) {
     * //            Striped64.Cell[] as; long b, v, r; int m; Striped64.Cell a;
     * //            if ((as = cells) != null ||
     * //                    (r = function.applyAsLong(b = base, x)) != b && !casBase(b, r)) {
     * //                boolean uncontended = true;
     * //                if (as == null || (m = as.length - 1) < 0 ||
     * //                        (a = as[getProbe() & m]) == null ||
     * //                        !(uncontended =
     * //                                (r = function.applyAsLong(v = a.value, x)) == v ||
     * //                                        a.cas(v, r)))
     * //                    longAccumulate(x, function, uncontended);
     * //            }
     * //        }
     * 然后 我们去拿值的 使用的get()方法：看见没就是首先将base的值赋给result，然后遍历cells将拿到的每个值 调用我们重写的方法进行计算
     * 其实她就是将 细分的结果 整合成最终结果的！
     * 假设我们设定的  初始值是 1
     * 这里太复杂的我不好理解，你就理解为 x*y这个公式的运算，假如一开始只是 base  ，那么就是  1*2*3，然后突然发生了一次 cas那么此后就是
     * cell[0]=base*4  cell[1]=base*5  以此类推，然后 根据数学运算  抽取  base  那么就是 base*(4*5....)
     * 其实和单个线程计算  1*2*3*4*5是一致的！
     * 再来个复杂一点的公式  x*y-10   （太复杂理解不了(#^.^#)），反正按照上面的公式理解就行，它就是也是细分了！
     * 一开始  1*2-10    （1*2-10）*3-10 （(1*2-10）*3-10)*4-10
     * 假设这里出现cas
     * cell[0]=((1*2-10）*3-10)*4-10)*5-10  cell[1]=((1*2-10）*3-10)*4-10)*6-10
     * 最终结果：（理解不了算了...记住结论即可，有的东西拿来用就行，毕竟技术有限）
     * (((1*2-10）*3-10)*4-10)*5-10)*(((1*2-10）*3-10)*4-10)*6-10)-10
     *        public long get() {
     * //            Striped64.Cell[] as = cells; Striped64.Cell a;
     * //            long result = base;
     * //            if (as != null) {
     * //                for (int i = 0; i < as.length; ++i) {
     * //                    if ((a = as[i]) != null)
     * //                        result = function.applyAsLong(result, a.value);
     * //                }
     * //            }
     * //            return result;
     * //        }
     */

    LongAccumulator accumulator =
        new LongAccumulator(
            (x, y) -> {
              return x + y;
            },
            0);
        Thread[] ts=new Thread[1000];
        for (int i = 0; i < 1000;i++) {
            long finalI = i;
            ts[i]=new Thread(()->{
              accumulator.accumulate(finalI);
          });
            ts[i].start();
        }
        for (int i = 0; i < 1000;i++) {
            ts[i].join();//这个就是让所有子线程都执行完毕才打印，否则有的线程未执行完毕而 主线程执行了
        }
        System.out.println(accumulator.get());//499500
    }
}
