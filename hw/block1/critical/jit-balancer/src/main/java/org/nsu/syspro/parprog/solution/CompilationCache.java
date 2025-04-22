package org.nsu.syspro.parprog.solution;

import org.nsu.syspro.parprog.external.CompiledMethod;
import org.nsu.syspro.parprog.external.MethodID;

import java.util.concurrent.ConcurrentHashMap;

/**
 * This class stores compiled methods and their compilation levels.
 * It acts like a dictionary where the key is a method ID, and the value is the compiled method.
 *
 * <p> According to the Java Concurrency documentation, {@link ConcurrentHashMap} provides:
 * <ul>
 *     <li> No Global Locking.</li>
 *     <li> Multiple threads can safely read from the map concurrently without blocking each other. </li>
 *     <li> {@code put} on a key provides a happens-before relationship with subsequent non-null retrievals ({@code get}) of that key
 *     (it means that once an update is visible to one thread, all other threads will see the same updated value).</li>
 * </ul>
 */
public class CompilationCache {

    // Map to store compiled methods by ID
    private final ConcurrentHashMap<Long, CompiledMethod> idMap = new ConcurrentHashMap<>();

    // Map to store compilation level of each method
    private final ConcurrentHashMap<Long, CompilationLevel> levelMap = new ConcurrentHashMap<>();

    /**
     * This method adds compiled method to cache with its level.
     * <p>
     * This method is synchronized to ensure that several threads do not write
     * the same method at the same time. Synchronization guarantees that both
     * {@code idMap} and {@code levelMap} are updated together atomically.
     * </p>
     *
     * @param id     ID of method
     * @param method Compiled method
     * @param level  Compilation level (L1 or L2)
     */
    public synchronized void addCompiledMethod(long id, CompiledMethod method, CompilationLevel level) {
        idMap.put(id, method);
        levelMap.put(id, level);
    }

    /**
     * This method gets compiled method from cache.
     * If method is not compiled yet, this returns null.
     *
     * @param id ID of method
     * @return Compiled method or null
     */
    public CompiledMethod getCompiledMethod(long id) {
        return idMap.get(id);
    }

    /**
     * This method gets compilation level of method.
     * If method is not compiled, it returns INTERPRET.
     *
     * @param id ID of method
     * @return Compilation level
     */
    public CompilationLevel getCompilationLevel(long id) {
        return levelMap.getOrDefault(id, CompilationLevel.INTERPRET);
    }

    /**
     * This method checks if method is already compiled.
     * Method is compiled if it is not null and its level is higher than INTERPRET.
     *
     * @param id     Method ID
     * @param method Compiled method (maybe null)
     * @return True if method compiled, false otherwise
     */
    public Boolean isCompiled(MethodID id, CompiledMethod method) {
        return method != null && getCompilationLevel(id.id()) != CompilationLevel.INTERPRET;
    }
}
