package com.aterrizAR.backend.valoracion

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.sql.DriverManager
import java.sql.SQLException

class ValoracionMigrationTest {
    @Test
    fun `la migracion conserva contenido previo y limita una valoracion por compra`() {
        DriverManager.getConnection("jdbc:h2:mem:valoracion_migration;MODE=PostgreSQL").use { connection ->
            connection.createStatement().use { sql ->
                sql.execute("CREATE TABLE compra (id BIGINT PRIMARY KEY)")
                sql.execute("INSERT INTO compra VALUES (1), (2)")
                sql.execute("CREATE TABLE valoracion (id BIGINT PRIMARY KEY, puntaje INTEGER NOT NULL, comentario VARCHAR(255) NOT NULL, paquete_id BIGINT NOT NULL)")
                sql.execute("INSERT INTO valoracion VALUES (1, 8, 'Historica', 10)")
                val migration = requireNotNull(javaClass.getResource("/db/migration/V7__valoracion_por_compra.sql")).readText()
                migration.split(';').filter { it.isNotBlank() }.forEach { sql.execute(it) }
                sql.executeQuery("SELECT comentario, compra_id, fecha FROM valoracion WHERE id = 1").use {
                    it.next()
                    assertEquals("Historica", it.getString("comentario"))
                    assertEquals(null, it.getObject("compra_id"))
                    org.junit.jupiter.api.Assertions.assertNotNull(it.getTimestamp("fecha"))
                }
                sql.execute("INSERT INTO valoracion (id, puntaje, paquete_id, compra_id) VALUES (2, 0, 10, 1), (3, 10, 10, 2)")
                listOf(
                    "INSERT INTO valoracion (id, puntaje, paquete_id, compra_id) VALUES (4, 5, 10, 1)",
                    "INSERT INTO valoracion (id, puntaje, paquete_id, compra_id) VALUES (4, 5, 10, 999)",
                    "INSERT INTO valoracion (id, puntaje, paquete_id) VALUES (4, -1, 10)",
                    "INSERT INTO valoracion (id, puntaje, paquete_id) VALUES (4, 11, 10)",
                    "DELETE FROM compra WHERE id = 1",
                ).forEach { statement -> assertThrows(SQLException::class.java) { sql.execute(statement) } }
                sql.execute("DELETE FROM valoracion WHERE compra_id = 1")
                sql.executeQuery("SELECT COUNT(*) FROM compra").use { it.next(); assertEquals(2, it.getInt(1)) }
            }
        }
    }
}
