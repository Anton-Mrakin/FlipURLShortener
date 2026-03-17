package com.mrakin.usecases;

import com.mrakin.usecases.generator.Base62ShortCodeGenerator;
import com.mrakin.usecases.generator.RandomStringShortCodeGenerator;
import com.mrakin.usecases.generator.Sha256ShortCodeGenerator;
import com.mrakin.usecases.generator.ShortCodeGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ShortCodeGeneratorTest {

    @Test
    void sha256Generator_ShouldBeDeterministic() {
        Sha256ShortCodeGenerator generator = new Sha256ShortCodeGenerator();
        ReflectionTestUtils.setField(generator, "shortCodeLength", 8);

        String url = "https://example.com";
        String code1 = generator.generate(url);
        String code2 = generator.generate(url);

        assertEquals(code1, code2);
        assertEquals(8, code1.length());
    }

    @Test
    void randomStringGenerator_ShouldBeRandom() {
        RandomStringShortCodeGenerator generator = new RandomStringShortCodeGenerator();
        ReflectionTestUtils.setField(generator, "shortCodeLength", 8);

        String url = "https://example.com";
        String code1 = generator.generate(url);
        String code2 = generator.generate(url);

        assertNotEquals(code1, code2);
        assertEquals(8, code1.length());
    }

    @Test
    void base62Generator_WithSeed_ShouldBeDeterministicPerThread() throws InterruptedException {
        Base62ShortCodeGenerator generator = new Base62ShortCodeGenerator();
        ReflectionTestUtils.setField(generator, "shortCodeLength", 8);
        ReflectionTestUtils.setField(generator, "seed", 12345L);

        int threadCount = 2;
        Set<String> results = ConcurrentHashMap.newKeySet();
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    String url = "https://example.com";
                    results.add(generator.generate(url));
                } finally {
                    latch.countDown();
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertEquals(1, results.size(), "Deterministic seed should produce same results across threads");
    }

    @Test
    void base62Generator_NoSeed_ShouldBeRandom() {
        Base62ShortCodeGenerator generator = new Base62ShortCodeGenerator();
        ReflectionTestUtils.setField(generator, "shortCodeLength", 8);

        String url = "https://example.com";
        String code1 = generator.generate(url);
        String code2 = generator.generate(url);

        assertNotEquals(code1, code2);
        assertEquals(8, code1.length());
    }
    
    @Test
    void generators_ShouldThrowExceptionOnInvalidInput() {
        ShortCodeGenerator[] generators = {
            new Sha256ShortCodeGenerator(),
            new RandomStringShortCodeGenerator(),
            new Base62ShortCodeGenerator()
        };
        
        for (ShortCodeGenerator g : generators) {
            assertThrows(IllegalArgumentException.class, () -> g.generate(null));
            assertThrows(IllegalArgumentException.class, () -> g.generate(""));
            assertThrows(IllegalArgumentException.class, () -> g.generate("   "));
        }
    }
}
