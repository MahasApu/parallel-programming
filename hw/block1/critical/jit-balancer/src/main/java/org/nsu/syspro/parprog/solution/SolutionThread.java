package org.nsu.syspro.parprog.solution;

import org.nsu.syspro.parprog.UserThread;
import org.nsu.syspro.parprog.external.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SolutionThread extends UserThread {

    private final ThreadLocal<Map<MethodID, CompiledMethod>> threadLocalCache = ThreadLocal.withInitial(HashMap::new);
    private final ConcurrentHashMap<MethodID, CompiledMethod> globalCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<MethodID, AtomicInteger> executionCounters = new ConcurrentHashMap<>();
    private final ExecutorService compilationExecutor = Executors.newCachedThreadPool();

    private static final int L1_THRESHOLD = 5000;
    private static final int L2_THRESHOLD = 100000;

    public SolutionThread(int compilationThreadBound, ExecutionEngine exec, CompilationEngine compiler, Runnable r) {
        super(compilationThreadBound, exec, compiler, r);
    }

    @Override
    public ExecutionResult executeMethod(MethodID id) {
        Map<MethodID, CompiledMethod> localCache = threadLocalCache.get();
        CompiledMethod compiledMethod = localCache.get(id);

        if (compiledMethod == null) {
            compiledMethod = globalCache.get(id);
            if (compiledMethod != null) {
                localCache.put(id, compiledMethod);
            }
        }

        if (compiledMethod != null) {
            return exec.execute(compiledMethod);
        }

        ExecutionResult result = exec.interpret(id);
        int count = executionCounters.computeIfAbsent(id, k -> new AtomicInteger()).incrementAndGet();

        if (count == L1_THRESHOLD || count == L2_THRESHOLD) {
            scheduleCompilation(id, count == L2_THRESHOLD ? 2 : 1);
        }

        return result;
    }

    private void scheduleCompilation(MethodID id, int level) {
        compilationExecutor.submit(() -> {
            CompiledMethod compiled = (level == 1) ? compiler.compile_l1(id) : compiler.compile_l2(id);
            globalCache.put(id, compiled);
            threadLocalCache.get().put(id, compiled);
        });
    }
}
