/**
 * @fileName:浮点数问题
 * @author:xy
 * @date:2018/8/9
 * @description:
 */
package com.xy.浮点数问题;

import java.math.BigDecimal;

/**
 *@fileName:浮点数问题
 *@author:xy
 *@date:2018/8/9
 *@description:
 */
public class 浮点数问题 {

    private BigDecimal b7;

    public static void main(String[] args) {
        /**
         * 奇怪了0.1+0.2出现问题 0.1+1.2不出现 0.1+2.2又出现
         * 0.0100000000000000021==0.0100000000000000022原因在于精度只有0.010000000000000002也就是科学计数法
         * 1.0000000000000002 * 2^-16  也就是转换为科学计数法后面的就会四舍五入
         * 0.0100000000000000021 ！= 0.0100000000000000029  （四舍五入了所以不等，上面末尾1,2都会舍掉）
         * 0.000000100000000000000000000001和0.00000010000000000000000002 长度不一样。但是由于是双精度转换为科学计数法
         * 后长度是一样的，因为两者都超标了。
         * 双精度：保留小数点后15-16位。单精度：保留小数点7-8位。也就是这样记住
         * 单精度表示小数点后面只用一个字节保存数据。双精度表示小数点后面用两个字节保存数据（一个字节8位）巧记也不知道对不对
         *
         * 大概就是转换为科学计数法后，先比较 （由于正宗的IEEE 754 标准使用二进制所以科学计数法只能是0,1）。但是我这里自己总结的
         * 使用十进制，所以（1-10 * 10 ^n）,前面1-10都可能，如果前面不同没必要比较，一定不相等
         * 0.000000100000000000000000000001和0.00000020000000000000000002一定不相等 因为一个是 1.x *10^y  一个是2.x *10^y
         * 还有就是正负数，也不相等，这个就不多说！
         * 而之前说过 0.1f+0.2f =0.3f  之所以这样主要是 转换的二进制然后转回float的时候有效位 都是0 。
         * 也就是应该是 0.030000000  输出做了优化0没必要显示 所以是0.03
         * 0.01000000001f+0.02000000002f =0.03其实也是这个原因。转换二进制的时候单双精度只是看保留小数点后的位数而已。
         * 这也是为嘛double不行，因为取的精度不一样 0.1+0.2 他们转换二进制后会取53位计算。那么最终计算的结果就肯定和
         * 单精度的不一致了。
         * 说了这么多，一句话就是浮点数运算【有可能】导致不准确的结果，注意是有可能，因为有的如果能够正确转换为二进制
         * 那么就可以正确计算。
         * 以后就记住一个原则，任何计算机计算都是转换为二进制计算，然后正常输出就是使用十进制，这时候会把该二进制转换为十进制
         * 按照Java，如果是单精度就是 保留小数点后8位。双精度就是 保留小数点后16 位。
         * 如果你转换为二进制转换不完，那么这个计算基本是有误差的。
         * 只要不运算，只是输出，那么就直接打印【字面量】
         */
        double b=0.4*0.5;
        System.out.println(0.4*0.2);//08000000000000002
        System.out.println(b);//0.2
        double c=2.0014999;
        System.out.println(c);//2.0014999
        System.out.println(0.1f+0.2f);//0.3.
        System.out.println(0.1+0.2);//0.30000000000000004
        System.out.println(1.2+0.1);//1.3
        System.out.println(0.1+2.2);//2.3000000000000003
        System.out.println(0.04f+0.2f);//0.24000001
        System.out.println(0.0100000000000000021==0.0100000000000000029);//false
        System.out.println(0.000000100000000000000000000001==0.00000010000000000000000002);//true
        System.out.println(0.000000100000000000000000000001==0.00000020000000000000000002);//false
        System.out.println(0.001000000011d==0.001000000012d);//false
        System.out.println(0.001000000011f==0.001000000012f);//true
        System.out.println(0.00100001113f==0.00100001112f);//true
        System.out.println(0.01000000001f+0.0200000002f);//0.03
        //0.0000011110101110000101000111101011100001010001111010111  ---0.03
        //0.000001111010111000010100101001011101010001100101011100101---0.03000001
        //0.0000011110110101010111001001100010110010001111100000000001---0.03011111
        //可见只要在一定精度，是可以有一样的二进制的。比如双精度二进制是53位，单精度是23位就是这个道理（具体看我的有道笔记）
        System.out.println("-------------------------BigDecimal---------------------------------");
        BigDecimal b1=new BigDecimal("0.1");
        BigDecimal b2=new BigDecimal("0.2");
        System.out.println(b1.add(b2));
        System.out.println(b1.subtract(b2));
        System.out.println(b1.multiply(b2));
        System.out.println(b1.divide(b2));
        BigDecimal bNum1=new BigDecimal(0.3);
        System.out.println(bNum1);//0.299999999999999988897769753748434595763683319091796875 还是会出现精度问题
        /**
         * 字符串这种数字 计算直接按照字面量进行计算的。数学里面是多少就是多少
         * 但是注意如果是 数字而不是字符串那么还是会出现精度，只不过这个比 double和float准确！！！
         * 所以一般推荐使用字符串传递
         * 加减乘除就是 几个单词 很好记。
         * setScale是对调用它的对象进行小数位数处理，保留5位 多的四舍五入或者保留7位多的直接删除
         * 具体看BigDecimal他的几个常量
         */
        BigDecimal b3=new BigDecimal("0.000119");
        BigDecimal b4=new BigDecimal("0.000115");
        BigDecimal add = b3.add(b4);
        BigDecimal b5 = add.setScale(10, BigDecimal.ROUND_HALF_UP);
        System.out.println(b5);

        BigDecimal b6=new BigDecimal("0.0100");
        BigDecimal b7 = new BigDecimal("0.010");
        BigDecimal b8=new BigDecimal("0.0111");
        System.out.println(b6.compareTo(b7));//返回0表示相等,1表示前者大于后者 -1表示前者小于后者
        System.out.println(b6.compareTo(b8));//返回0表示相等,1表示前者大于后者 -1表示前者小于后者
        System.out.println(b8.compareTo(b6));//返回0表示相等,1表示前者大于后者 -1表示前者小于后者
        System.out.println("optional");
    }
}
