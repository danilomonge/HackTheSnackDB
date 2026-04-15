package de.pecus.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ensures that default seed rows (Brand ID=1, Category ID=1, SubCategory ID=1)
 * exist in the database before any product creation is attempted.
 *
 * ProductManager in the Python frontend always sends idBrand=1, idCategory=1,
 * idSubCategory=1 as foreign-key references; these rows must exist or every
 * product creation will fail with an FK constraint violation.
 *
 * Uses PostgreSQL "INSERT … ON CONFLICT (PK_ID) DO NOTHING" so it is safe to
 * run on every application start-up.
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        seedBrand();
        seedCategory();
        seedSubCategory();
    }

    private void seedBrand() {
        try {
            jdbcTemplate.execute(
                "INSERT INTO BRAND " +
                "(PK_ID, DX_NAME, DX_DESCRIPTION, DN_ACTIVO, DN_USUARIO_CREADOR, DD_FECHA_CREACION, DN_USUARIO_MODIFICADOR, DD_FECHA_MODIFICACION) " +
                "VALUES (1, 'DEFAULT', 'Default Brand', true, 1, NOW(), 1, NOW()) " +
                "ON CONFLICT (PK_ID) DO NOTHING"
            );
            // Ensure the sequence is at least at 1 so auto-generated IDs don't collide
            jdbcTemplate.execute(
                "SELECT setval(pg_get_serial_sequence('BRAND', 'PK_ID'), GREATEST((SELECT MAX(PK_ID) FROM BRAND), 1))"
            );
            log.info("DataInitializer: BRAND seed OK");
        } catch (Exception e) {
            log.warn("DataInitializer: could not seed BRAND — {}", e.getMessage());
        }
    }

    private void seedCategory() {
        try {
            jdbcTemplate.execute(
                "INSERT INTO CATEGORY " +
                "(PK_ID, DX_NAME, DN_ACTIVO, DN_USUARIO_CREADOR, DD_FECHA_CREACION, DN_USUARIO_MODIFICADOR, DD_FECHA_MODIFICACION) " +
                "VALUES (1, 'DEFAULT', true, 1, NOW(), 1, NOW()) " +
                "ON CONFLICT (PK_ID) DO NOTHING"
            );
            jdbcTemplate.execute(
                "SELECT setval(pg_get_serial_sequence('CATEGORY', 'PK_ID'), GREATEST((SELECT MAX(PK_ID) FROM CATEGORY), 1))"
            );
            log.info("DataInitializer: CATEGORY seed OK");
        } catch (Exception e) {
            log.warn("DataInitializer: could not seed CATEGORY — {}", e.getMessage());
        }
    }

    private void seedSubCategory() {
        try {
            jdbcTemplate.execute(
                "INSERT INTO SUBCATEGORY " +
                "(PK_ID, DX_NAME, FK_CATEGORY_ID, DN_ACTIVO, DN_USUARIO_CREADOR, DD_FECHA_CREACION, DN_USUARIO_MODIFICADOR, DD_FECHA_MODIFICACION) " +
                "VALUES (1, 'DEFAULT', 1, true, 1, NOW(), 1, NOW()) " +
                "ON CONFLICT (PK_ID) DO NOTHING"
            );
            jdbcTemplate.execute(
                "SELECT setval(pg_get_serial_sequence('SUBCATEGORY', 'PK_ID'), GREATEST((SELECT MAX(PK_ID) FROM SUBCATEGORY), 1))"
            );
            log.info("DataInitializer: SUBCATEGORY seed OK");
        } catch (Exception e) {
            log.warn("DataInitializer: could not seed SUBCATEGORY — {}", e.getMessage());
        }
    }
}
