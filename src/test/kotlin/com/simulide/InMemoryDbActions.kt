package com.simulide

import com.simulide.plugins.domain.Document
import java.util.*
import javax.sql.DataSource

private const val UPDATE_DOCUMENT =
    """
     UPDATE documents
     SET content = ?, version = version + 1, updated_at = CURRENT_TIMESTAMP
     WHERE id = ?
    """
private const val SELECT_DOCUMENT_BY_ID =
    """
     SELECT id, name, content, version
     FROM documents
     WHERE id = ?
    """
private const val CREATE_OPERATION =
    """
     INSERT INTO operations (id, document_id, type, position, content, version, created_at)
     VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
    """

private const val CREATE_DOCUMENT =
    """
     INSERT INTO documents (id, name, content, version, created_at)
     VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
    """

fun createDocument(dataSource: DataSource, document: Document): Document {
     dataSource.connection.use { connection ->
        connection.prepareStatement(CREATE_DOCUMENT).use { stmt ->
            stmt.setObject(1, document.id)
            stmt.setString(2, document.name)
            stmt.setString(3, document.content)
            stmt.setInt(4, document.version)
            stmt.executeUpdate()
        }
    }
    return document
}

fun getDocument(dataSource: DataSource, id: UUID): Document? {
    dataSource.connection.use { connection ->
        connection.prepareStatement(SELECT_DOCUMENT_BY_ID).use { stmt ->
           stmt.setObject(1, id)
           stmt.executeQuery().use { rs ->

               if (rs.next()) {
                   return Document(
                       id = rs.getObject("id", UUID::class.java),
                       name = rs.getString("name"),
                       content = rs.getString("content"),
                       version = rs.getInt("version")
                   )
               } else {
                   return null
               }

           }
        }

    }
}
