package com.lwy.ocrdemo;

import com.lwy.ocrdemo.utils.Calculator;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
//        assertEquals(4, 2 + 2);
//        System.out.println(Integer.toBinaryString(-16777216));
        String temp = "XXXX年X月X日";
        if (temp.matches("^[a-zA-Z\\d]{4}.{1}[a-zA-Z\\d]{1,2}.[1][a-zA-Z\\d]{1,2}.[1]$")) {
            System.out.println("匹配");
        } else {
            System.out.println("不匹配");
        }
        Pattern pattern = Pattern.compile("^([a-zA-Z\\d]{4}).([a-zA-Z\\d]{1,2}).([a-zA-Z\\d]{1,2}).$");
        Matcher matcher = pattern.matcher(temp);
        if (matcher.find()) {
            System.out.println(String.format("%s年%s月%s日", matcher.group(1), matcher.group(2), matcher.group(3)));
        }

        if ("1992年1月2日 拉".matches(".*([a-zA-Z\\d]{4}.[a-zA-Z\\d]{1,2}.[a-zA-Z\\d]{1,2}.).*")) {
            System.out.println("子匹配");
        }
    }

    @Test
    public void testAvg() throws Exception {
        Calculator calculator = new Calculator();
        int[] array = {1, 19, 25, 35,10};
        for (int i : array) {
            calculator.addDataValue(i);
        }
        System.out.println(calculator.mean());
    }
}