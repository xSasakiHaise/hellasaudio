package com.xsasakihaise.hellascontrol.api;

/**
 * Lightweight stub that mimics the pieces of HellasControl required at compile time. The real implementation is provided at
 * runtime on production servers. The stub allows this mod to compile in isolation while still retaining the runtime checks in
 * places where the full API is available.
 */
public final class CoreCheck {
    private CoreCheck() {
    }

    public static void verifyCoreLoaded() {
        // No-op in the development environment.
    }

    public static void verifyEntitled(String modId) {
        // No-op in the development environment.
    }
}
