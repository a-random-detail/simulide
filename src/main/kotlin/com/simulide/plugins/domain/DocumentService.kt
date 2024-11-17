package com.simulide.plugins.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.util.UUID

data class Operation(val documentId: UUID, val type: String, val position: Int, val content: String?, val version: Int)
data class Document(val id: UUID, val name: String, val content: String, val version: Int)

class DocumentService(private val dbConnection: Connection) {

    suspend fun getById(id: UUID): Document? = withContext(Dispatchers.IO) {
        return@withContext dbConnection.prepareStatement(Companion.SELECT_DOCUMENT_BY_ID).use { stmt ->
            stmt.setObject(1, id)
            val resultSet = stmt.executeQuery()

            if (resultSet.next()) {
                Document(
                    id = resultSet.getObject("id") as UUID,
                    name = resultSet.getString("name"),
                    content = resultSet.getString("content"),
                    version = resultSet.getInt("version")
                )
            } else {
                null
            }
        }
    }

    suspend fun applyOperation(documentId: UUID, operation: Operation): Document {
        // Fetch the current document state
        val document = dbConnection.prepareStatement(SELECT_DOCUMENT_BY_ID).use { stmt ->
            stmt.setObject(1, documentId)
            val resultSet = stmt.executeQuery()
            if (resultSet.next()) {
                resultSet.getString("content") to resultSet.getInt("version")
            } else {
                throw IllegalArgumentException("Document not found")
            }
        }

        val (currentContent, currentVersion) = document

        // Validate the operation's version
        if (operation.version != currentVersion) {
            throw IllegalStateException("Version mismatch: expected $currentVersion, got ${operation.version}")
        }

        // Apply the operation
        val updatedContent = when (operation.type) {
            "insert" -> currentContent.substring(0, operation.position) +
                    (operation.content ?: "") +
                    currentContent.substring(operation.position)
            "delete" -> currentContent.substring(0, operation.position) +
                    currentContent.substring(operation.position + (operation.content?.length ?: 0))
            else -> throw IllegalArgumentException("Invalid operation type")
        }

        // Update the document and increment the version
        dbConnection.prepareStatement(Companion.UPDATE_DOCUMENT).use { stmt ->
            stmt.setString(1, updatedContent)
            stmt.setObject(2, documentId)
            stmt.executeUpdate()
        }

        // Log the operation
        dbConnection.prepareStatement(CREATE_OPERATION).use { stmt ->
            stmt.setObject(1, documentId)
            stmt.setString(2, operation.type)
            stmt.setInt(3, operation.position)
            stmt.setString(4, operation.content)
            stmt.setInt(5, currentVersion + 1)
            stmt.executeUpdate()
        }

        // Return the updated document
        return Document(
            id = documentId,
            name = "", // Name is omitted in this example; retrieve if necessary
            content = updatedContent,
            version = currentVersion + 1
        )
    }

    companion object {
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
           INSERT INTO operations (document_id, type, position, content, version, created_at)
           VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
           """
    }
}
