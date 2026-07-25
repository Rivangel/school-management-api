package com.escuela;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Contexto de la aplicación")
class SchoolManagementApplicationTests {

    @Test
    @DisplayName("el contexto de Spring arranca con el perfil de test (H2)")
    void contextLoads() {
    }
}
