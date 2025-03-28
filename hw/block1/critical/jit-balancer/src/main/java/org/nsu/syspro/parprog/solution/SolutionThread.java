package org.nsu.syspro.parprog.solution;

import org.nsu.syspro.parprog.UserThread;
import org.nsu.syspro.parprog.external.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.nsu.syspro.parprog.solution.CompilationLevel.*;

/**
 * This thread manages the execution of methods in a Just-In-Time (JIT) compilation system.
 <ul>
 *     <li>Method hotness is tracked using {@link ConcurrentHashMap} with {@link AtomicLong}.</li>
 *     <li>Compilation tasks are submitted to a thread pool (using {@link ExecutorService}).</li>
 *     <li>Processing compilations are tracked using {@link ConcurrentHashMap} with {@link AtomicBoolean}.</li>
 * </ul>
 */
public class SolutionThread extends UserThread {

    private static final long L1_LIMIT = 5000;
    private static final long L2_LIMIT = 10000;

    // Thread pool for running compilation tasks
    private static final ExecutorService compilationPool = Executors.newCachedThreadPool();

    // Global compilation cache
    private static final CompilationCache compilationCache = new CompilationCache();

    // Hotness per method
    private static final ConcurrentHashMap<Long, AtomicLong> hotnessMap = new ConcurrentHashMap<>();

    // Track processing compilations
    private static final ConcurrentHashMap<Long, AtomicBoolean> compilationInProgress = new ConcurrentHashMap<>();

    public SolutionThread(int compilationThreadBound, ExecutionEngine exec, CompilationEngine compiler, Runnable r) {
        super(compilationThreadBound, exec, compiler, r);
    }

    /**
     * This method makes number for how many times method is called go up by one.
     * If method not in map, we put it there and start from zero.
     *
     * @param methodID ID of method
     * @return New hotness
     */
    private long increaseHotness(MethodID methodID) {
        return hotnessMap
                .computeIfAbsent(methodID.id(), k -> new AtomicLong(0))
                .incrementAndGet();
    }

    /**
     * This method is called when we need to run method.
     * It checks hotness, maybe starts compile, and then runs method.
     *
     * @param id ID of method
     * @return Result of execution
     */
    @Override
    public ExecutionResult executeMethod(MethodID id) {
        final long methodID = id.id();
        final CompiledMethod method = compilationCache.getCompiledMethod(methodID);
        final long hotness = increaseHotness(id);

        tryCompile(id, methodID, hotness);

        return execute(id, method);
    }

    /**
     * This method actually runs the method. If method is not compiled, we use interpreter.
     *
     * @param id     ID of method
     * @param method Compiled method (maybe null)
     * @return Result of execution
     */
    private ExecutionResult execute(MethodID id, CompiledMethod method) {
        return (!compilationCache.isCompiled(id, method)) ? exec.interpret(id) : exec.execute(method);
    }

    /**
     * This method looks at hotness and decides if we need to compile method and to which level.
     *
     * @param methodID ID of method
     * @param hotness  How many times method called
     * @return Compilation level we need
     */
    private CompilationLevel determineTargetLevel(long methodID, long hotness) {
        final CompilationLevel currentLevel = compilationCache.getCompilationLevel(methodID);

        if (hotness < L1_LIMIT) return INTERPRET;
        if (hotness == L1_LIMIT && currentLevel == INTERPRET) return L1;
        if (hotness == L2_LIMIT) return L2;

        return INTERPRET;
    }

    /**
     * This class method tries to start compile if needed.
     * If method must be interpreted, it does nothing.
     * If method already compiling, it also does nothing.
     * Otherwise, it starts new compile task in thread pool.
     *
     * @param id       ID of method
     * @param methodID Long ID of method
     * @param hotness  How many times method was run
     */
    private void tryCompile(MethodID id, long methodID, long hotness) {
        CompilationLevel targetLevel = determineTargetLevel(methodID, hotness);

        if (targetLevel == INTERPRET) return;

        // if no other thread was compiling this method -> this thread attempts to
        // compile the method (putting true to compilationInProgress map)
        if (compilationInProgress.putIfAbsent(methodID, new AtomicBoolean(true)) == null) {
            compilationPool.submit(() -> {
                try {
                    CompiledMethod method = compileMethod(id, targetLevel);
                    compilationCache.addCompiledMethod(methodID, method, targetLevel);
                } finally {
                    compilationInProgress.remove(methodID); // Mark as completed
                }
            });
        }
    }

    /**
     * This method compiles method to level we need.
     *
     * @param id          ID of method
     * @param targetLevel Level to compile to
     * @return Compiled method
     */
    private CompiledMethod compileMethod(MethodID id, CompilationLevel targetLevel) {
        return targetLevel == L1 ? compiler.compile_l1(id) : compiler.compile_l2(id);
    }
}
