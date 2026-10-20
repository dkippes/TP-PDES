package com.aterrizAR.backend.favorito

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.sql.DriverManager
import java.sql.SQLException

class FavoritoMigrationTest {
    @Test
    fun `migration enforces uniqueness ownership and cascade cleanup`() {
        DriverManager.getConnection("jdbc:h2:mem:favorito_migration;MODE=PostgreSQL").use { connection ->
            connection.createStatement().use { sql ->
                sql.execute("CREATE TABLE comprador (id BIGINT PRIMARY KEY)")
                sql.execute("CREATE TABLE paquete (id BIGINT PRIMARY KEY)")
                val migration = requireNotNull(javaClass.getResource("/db/migration/V5__create_favorito.sql")).readText()
                migration.split(';').filter { it.isNotBlank() }.forEach { sql.execute(it) }
                sql.execute("INSERT INTO comprador VALUES (1), (2)")
                sql.execute("INSERT INTO paquete VALUES (10), (20)")
                sql.execute("INSERT INTO favorito (comprador_id, paquete_id, agregado_en) VALUES (1, 10, CURRENT_TIMESTAMP), (2, 10, CURRENT_TIMESTAMP), (2, 20, CURRENT_TIMESTAMP)")
                assertThrows(SQLException::class.java) {
                    sql.execute("INSERT INTO favorito (comprador_id, paquete_id, agregado_en) VALUES (1, 10, CURRENT_TIMESTAMP)")
                }
                assertThrows(SQLException::class.java) {
                    sql.execute("INSERT INTO favorito (comprador_id, paquete_id, agregado_en) VALUES (99, 10, CURRENT_TIMESTAMP)")
                }
                assertThrows(SQLException::class.java) {
                    sql.execute("INSERT INTO favorito (comprador_id, paquete_id, agregado_en) VALUES (1, 99, CURRENT_TIMESTAMP)")
                }
                sql.execute("DELETE FROM paquete WHERE id = 10")
                sql.executeQuery("SELECT COUNT(*) FROM favorito").use { result ->
                    result.next()
                    assertEquals(1, result.getInt(1))
                }
                sql.execute("DELETE FROM comprador WHERE id = 2")
                sql.executeQuery("SELECT COUNT(*) FROM favorito").use { result ->
                    result.next()
                    assertEquals(0, result.getInt(1))
                }
                sql.executeQuery("SELECT COUNT(*) FROM paquete").use { result ->
                    result.next()
                    assertEquals(1, result.getInt(1))
                }
            }
        }
    }
}
