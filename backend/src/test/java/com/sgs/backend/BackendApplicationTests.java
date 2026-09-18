package com.sgs.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Vérifie que le contexte Spring démarre.
 *
 * Depuis la externalisation des secrets (JWT_SECRET, DB_PASSWORD,
 * ADMIN_PASSWORD — plus de valeurs par défaut committées dans
 * application.yaml), le test est hermétique : il tourne sur H2 en
 * mémoire (pas de PostgreSQL requis, viable en CI) et fournit ses
 * propres secrets via @TestPropertySource.
 *
 * La clé JWT ci-dessous fait 48 octets après décodage Base64 : elle
 * satisfait le contrôle fail-fast de JwtUtil.validateSecret().
 */
@SpringBootTest
@TestPropertySource(properties = {
        // H2 en mémoire à la place de PostgreSQL
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        // Hibernate : dialecte explicite (H2) au lieu de la détection JDBC
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        // Pas de scripts SQL d'init (aucun data.sql n'existe, mais par sûreté)
        "spring.sql.init.mode=never",
        // Secrets requis par la nouvelle configuration
        "jwt.secret=YmFja2VuZC10ZXN0LXNlY3JldC1rZXktZm9yLWp3dC1zaWduaW5nLTIwMjY=",
        "app.bootstrap.admin-password=test-admin-password"
})
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
