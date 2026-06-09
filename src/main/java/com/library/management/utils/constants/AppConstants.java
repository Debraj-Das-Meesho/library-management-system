package com.library.management.utils.constants;

import java.math.BigDecimal;

public final class AppConstants {

    private AppConstants() {}

    // Cache names
    public static final String CACHE_AUTHORS = "authors";
    public static final String CACHE_BOOKS = "books";
    public static final String CACHE_MEMBERS = "members";

    // Cache key expressions (SpEL)
    public static final String CACHE_KEY_ALL = "'all'";
    public static final String CACHE_KEY_AVAILABLE = "'available'";

    // Redis TTL
    public static final long CACHE_TTL_MINUTES = 10L;

    // Borrow defaults
    public static final int DEFAULT_BORROW_DAYS = 14;
    public static final BigDecimal FINE_PER_DAY = new BigDecimal("0.50");
}
