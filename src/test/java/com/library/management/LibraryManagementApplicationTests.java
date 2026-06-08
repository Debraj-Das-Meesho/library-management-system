package com.library.management;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Smoke test — verifies the main application class is instantiable.
 * Full context integration tests require docker-compose up (postgres + redis).
 */
@DisplayName("Application smoke test")
class LibraryManagementApplicationTests {

    @Test
    @DisplayName("Main application class can be instantiated without errors")
    void applicationClassInstantiates() {
        assertThatNoException().isThrownBy(LibraryManagementApplication::new);
    }
}
