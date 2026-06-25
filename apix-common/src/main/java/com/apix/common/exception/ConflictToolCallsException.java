package com.apix.common.exception;

import java.util.Set;

/**
 * Conflict tool calls detected.
 * 对标 Python: ConflictToolCalls
 */
public class ConflictToolCallsException extends ApixException {

    private final Set<String> conflictTools;

    public ConflictToolCallsException(String message) {
        super(message);
        this.conflictTools = Set.of();
    }

    public ConflictToolCallsException(String message, Set<String> conflictTools) {
        super(message + " [conflictTools=" + conflictTools + "]");
        this.conflictTools = conflictTools;
    }

    public Set<String> getConflictTools() {
        return conflictTools;
    }
}
