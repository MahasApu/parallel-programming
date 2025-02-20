package org.nsu.syspro.parprog.examples;

import org.nsu.syspro.parprog.interfaces.Fork;
import org.nsu.syspro.parprog.interfaces.Philosopher;

import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.atomic.AtomicLong;

public class DefaultPhilosopher implements Philosopher {

    private static final AtomicLong idProvider = new AtomicLong(0);
    public final long id;
    private long successfulMeals;
    private static final int MAX_PHILOSOPHERS_AMOUNT = 5;

    private static final ReentrantLock[] forkLocks = {new ReentrantLock(true),
            new ReentrantLock(true),
            new ReentrantLock(true),
            new ReentrantLock(true),
            new ReentrantLock(true)};

    public DefaultPhilosopher() {
        this.id = idProvider.getAndAdd(1);
        this.successfulMeals = 0;
    }

    @Override
    public long meals() {
        return successfulMeals;
    }

    @Override
    public void countMeal() {
        successfulMeals++;
    }

    private void think() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


    public void onHungry(Fork left, Fork right) {
        int leftFork = (int) left.id() % MAX_PHILOSOPHERS_AMOUNT;
        int rightFork = (int) right.id() % MAX_PHILOSOPHERS_AMOUNT;

        while (true) {
            try {
                if (forkLocks[leftFork].tryLock()) {
                    try {
                        if (forkLocks[rightFork].tryLock()) {
                            eat(left, right);
                            break;
                        }
                    } finally {
                        if (forkLocks[leftFork].isHeldByCurrentThread()) {
                            forkLocks[leftFork].unlock();
                        }
                    }
                }
                think();
            } finally {
                if (forkLocks[rightFork].isHeldByCurrentThread()) {
                    forkLocks[rightFork].unlock();
                }
            }
        }
    }

    @Override
    public String toString() {
        return "DefaultPhilosopher{" +
                "id=" + id +
                '}';
    }
}
