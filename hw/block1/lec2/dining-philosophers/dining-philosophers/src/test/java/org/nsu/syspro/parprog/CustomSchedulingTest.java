package org.nsu.syspro.parprog;

import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.nsu.syspro.parprog.base.DefaultFork;
import org.nsu.syspro.parprog.base.DiningTable;
import org.nsu.syspro.parprog.examples.DefaultPhilosopher;
import org.nsu.syspro.parprog.helpers.TestLevels;
import org.nsu.syspro.parprog.interfaces.Fork;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CustomSchedulingTest extends TestLevels {

    static final class CustomizedPhilosopher extends DefaultPhilosopher {
        @Override
        public void onHungry(Fork left, Fork right) {
            sleepMillis(this.id * 20);
            System.out.println(Thread.currentThread() + " " + this + ": onHungry");
            super.onHungry(left, right);
        }
    }

    static final class CustomizedFork extends DefaultFork {
        @Override
        public void acquire() {
            System.out.println(Thread.currentThread() + " trying to acquire " + this);
            super.acquire();
            System.out.println(Thread.currentThread() + " acquired " + this);
            sleepMillis(100);
        }
    }

    static final class CustomizedTable extends DiningTable<CustomizedPhilosopher, CustomizedFork> {
        public CustomizedTable(int N) {
            super(N);
        }

        @Override
        public CustomizedFork createFork() {
            return new CustomizedFork();
        }

        @Override
        public CustomizedPhilosopher createPhilosopher() {
            return new CustomizedPhilosopher();
        }
    }

    @EnabledIf("easyEnabled")
    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4, 5})
    @Timeout(2)
    void testDeadlockFreedom(int N) {
        final CustomizedTable table = dine(new CustomizedTable(N), 1);
    }


    static final class CustomizedPhilosopherForFirst extends DefaultPhilosopher {
        public void onHungry(Fork left, Fork right) {
            super.onHungry(left, right);
            if (this.id == 1) {
                sleepMillis(1000);
            }

        }
    }

    static final class CustomizedTableForFirst extends DiningTable<CustomizedPhilosopherForFirst, DefaultFork> {
        public CustomizedTableForFirst(int N) {
            super(N);
        }

        @Override
        public DefaultFork createFork() {
            return new DefaultFork();
        }

        @Override
        public CustomizedPhilosopherForFirst createPhilosopher() {
            return new CustomizedPhilosopherForFirst();
        }
    }

    @EnabledIf("easyEnabled")
    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4, 5})
    @Timeout(5)
    void testSingleSlow(int N) {
        CustomizedTableForFirst table = dine(new CustomizedTableForFirst(N), 1);
        assertTrue(table.maxMeals() >= 1000, "At least one philosopher must eat 1000 times." +
                " But there are only " + table.maxMeals());
    }


    static final class CustomizedPhilosopherForFairness extends DefaultPhilosopher {
        @Override
        public void onHungry(Fork left, Fork right) {
            if (this.id % 2 == 0) {
                sleepMillis(10);
            } else {
                sleepMillis(100);
            }
            super.onHungry(left, right);
        }
    }

    static final class CustomizedTableForFairness extends DiningTable<CustomizedPhilosopherForFairness, DefaultFork> {
        public CustomizedTableForFairness(int N) {
            super(N);
        }

        @Override
        public DefaultFork createFork() {
            return new DefaultFork();
        }

        @Override
        public CustomizedPhilosopherForFairness createPhilosopher() {
            return new CustomizedPhilosopherForFairness();
        }
    }

    @EnabledIf("mediumEnabled")
    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4, 5})
    @Timeout(2)
    void testWeakFairness(int N) {
        final CustomizedTableForFairness table = dine(new CustomizedTableForFairness(N), 1);
        assertTrue(table.minMeals() > 0); // every philosopher eat at least once
    }

}
