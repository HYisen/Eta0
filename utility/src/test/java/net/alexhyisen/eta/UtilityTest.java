package net.alexhyisen.eta;

import net.alexhyisen.Utility;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.IntStream;

public class UtilityTest {

    @Test
    public void genCachedMapper() throws InterruptedException {
        var cnt = new AtomicInteger(0);
        Function<Integer, String> old = k -> {
            int count = cnt.addAndGet(1);
            if (count < 5) {
                System.out.println("gen " + count + " : " + k);
            }
            return "value of " + k;
        };
        var neo = Utility.genCachedMapper(new HashMap<>(), old);

        //Cached
        System.out.println(neo.apply(4));
        System.out.println(neo.apply(4));
        System.out.println(neo.apply(4));
        System.out.println(neo.apply(4));
        System.out.println(neo.apply(10));
        assert cnt.get() == 2;

        //Consistent
        var input = 1000;
        assert neo.apply(input).equals(old.apply(input));


        //Benchmark
        int size = 10000;
        ExecutorService es0 = Executors.newFixedThreadPool(4);
        ExecutorService es1 = Executors.newFixedThreadPool(4);
        Utility.stamp("CachedMapperBenchmark");

        IntStream.range(0, size).forEach(v -> es0.submit(() -> old.apply(5)));
        es0.shutdown();
        es0.awaitTermination(10, TimeUnit.SECONDS);
        Utility.stamp("old");

        IntStream.range(0, size).forEach(v -> es1.submit(() -> neo.apply(6)));
        es0.shutdown();
        es0.awaitTermination(10, TimeUnit.SECONDS);
        Utility.stamp("neo");

        //Benchmark Advanced
        ExecutorService es2 = Executors.newFixedThreadPool(4);
        var rand = new Random(17);
        var in = IntStream.range(0, 1000000).map(v -> rand.nextInt(20)).toArray();
        Utility.stamp("initialized");
        Arrays.stream(in).forEach(v -> es2.submit(() -> neo.apply(v)));
        Utility.stamp("finished");
    }
}