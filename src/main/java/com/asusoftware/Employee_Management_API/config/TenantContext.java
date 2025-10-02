package com.asusoftware.Employee_Management_API.config;

/**
 * Păstrează tenant-ul curent în contextul firului (ThreadLocal).
 * Setezi la începutul request-ului și îl cureți la final.
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private TenantContext() { }

    /** Setează tenant-ul curent (ex: "acme"). */
    public static void setTenant(String tenant) {
        CURRENT.set(tenant);
    }

    /** Returnează tenant-ul curent sau null dacă nu e setat. */
    public static String getTenant() {
        return CURRENT.get();
    }

    /** Curăță contextul (IMPORTANT de apelat la finalul fiecărui request). */
    public static void clear() {
        CURRENT.remove();
    }

    /**
     * Util pentru try-with-resources:
     * try (TenantContext.scope("acme")) { ... } // la final se curăță automat
     */
    public static AutoCloseable scope(String tenant) {
        final String previous = CURRENT.get();
        CURRENT.set(tenant);
        return () -> {
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        };
    }
}
