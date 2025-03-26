package org.nsu.syspro.parprog.solution;

import org.nsu.syspro.parprog.external.CompiledMethod;

import java.util.concurrent.ConcurrentHashMap;

public class CompilationCache {
    private final ConcurrentHashMap<Long, CompiledMethod> idMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, CompilationLevel> levelMap = new ConcurrentHashMap<>();

    public void addCompiledMethod(long id, CompiledMethod code, CompilationLevel level) {
        idMap.put(id, code);
        levelMap.put(id, level);
    }

    public CompiledMethod getCompiledMethod(long id) {
        return idMap.get(id);
    }

    public CompilationLevel getCompilationLevel(long id) {
        return levelMap.getOrDefault(id, CompilationLevel.INTERPRET);
    }

}
