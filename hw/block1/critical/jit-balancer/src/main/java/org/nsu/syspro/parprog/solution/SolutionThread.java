package org.nsu.syspro.parprog.solution;

import org.nsu.syspro.parprog.UserThread;
import org.nsu.syspro.parprog.external.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.nsu.syspro.parprog.solution.CompilationLevel.*;

public class SolutionThread extends UserThread {

    private static final long L1_LIMIT = 5000;
    private static final long L2_LIMIT = 10000;

    // Thread pool for running asynchronous compilation tasks
    private static final ExecutorService compilationPool = Executors.newCachedThreadPool();

    // Global compilation cache for storing compiled methods and their levels
    private static final CompilationCache compilationCache = new CompilationCache();


    // Thread-local map to track hotness (invocation counts) of methods in each thread
    private final ThreadLocal<ConcurrentHashMap<Long, AtomicLong>> hotnessMap =
            ThreadLocal.withInitial(ConcurrentHashMap::new);

    public SolutionThread(int compilationThreadBound, ExecutionEngine exec, CompilationEngine compiler, Runnable r) {
        super(compilationThreadBound, exec, compiler, r);
    }

    /**
     * Increment the hotness level for the given methodID in a thread-safe way.
     */
    private long increaseHotness(MethodID methodID) {
        return hotnessMap.get()
                .computeIfAbsent(methodID.id(), k -> new AtomicLong(0))
                .incrementAndGet();
    }

    @Override
    public ExecutionResult executeMethod(MethodID id) {
        final long methodID = id.id();
        final CompiledMethod method = compilationCache.getCompiledMethod(methodID);

        // Increment the hotness level
        final AtomicLong hotness = new AtomicLong(increaseHotness(id));

        // Attempt to compile method if invocation counts (for L1, L2) are reached
        tryCompile(id, methodID, hotness);

        return execute(id, method);
    }

    private ExecutionResult execute(MethodID id, CompiledMethod method) {
        CompilationLevel compilationLevel = compilationCache.getCompilationLevel(id.id());
        return (compilationLevel.equals(INTERPRET)) ? exec.interpret(id) : exec.execute(method);
    }

    /**
     * Determine the target compilation level based on hotness and method state.
     */
    private CompilationLevel determineTargetLevel(long methodID, AtomicLong hotLevel) {
        final CompilationLevel currentLevel = compilationCache.getCompilationLevel(methodID);
        final long hotness = hotLevel.get();

        if (hotness < L1_LIMIT) return INTERPRET;
        else if (hotness == L1_LIMIT && currentLevel.equals(INTERPRET)) return L1;
        else if (hotness == L2_LIMIT) return L2;

        return INTERPRET;
    }

    /**
     * Submit compilation task if needed based on the target level.
     */
    private void tryCompile(MethodID id, long methodID, AtomicLong hotLevel) {
        CompilationLevel targetLevel = determineTargetLevel(methodID, hotLevel);

        if (!targetLevel.equals(INTERPRET)) {
            compilationPool.submit(() -> {
                CompiledMethod code = compileMethod(id, targetLevel);
                compilationCache.addCompiledMethod(methodID, code, targetLevel);
            });
        }
    }

    /**
     * Choose the appropriate compiler method (L1 or L2 (maybe null if method must be interpreted)) based on the target level.
     */
    private CompiledMethod compileMethod(MethodID id, CompilationLevel targetLevel) {
        if (targetLevel.equals(INTERPRET)) return null;
        return targetLevel.equals(L1) ? compiler.compile_l1(id) : compiler.compile_l2(id);
    }
}
