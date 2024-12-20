package com.simulide.plugins.domain

import com.simulide.domain.OperationType
import com.simulide.plugins.CreateDocumentRequest
import com.simulide.plugins.CreateOperationRequest
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.sql.Connection
import java.util.*
import javax.sql.DataSource

@Serializable
data class Operation(@Contextual val id: UUID, @Contextual val documentId: UUID, val type: OperationType, val position: Int, val content: String?, val version: Int)
@Serializable
data class Document(@Contextual val id: UUID, val name: String, val content: String, val version: Int)

class DocumentService(private val dataSource: DataSource) {

    private fun <T> withConnection(action: (Connection) -> T): T {
        dataSource.connection.use { connection ->
            return action(connection)
        }
    }

    fun getById(id: UUID): Document? {
        return withConnection { connection ->
            connection.prepareStatement(SELECT_DOCUMENT_BY_ID).use { stmt ->
            stmt.setObject(1, id)

                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        return@withConnection Document(
                            id = rs.getObject("id", UUID::class.java),
                            name = rs.getString("name"),
                            content = rs.getString("content"),
                            version = rs.getInt("version")
                        )
                    } else return@withConnection null
                }
            }
        }
    }

    fun applyOperation(documentId: UUID, operation: CreateOperationRequest): Operation {
        return withConnection { connection ->
            val document = getById(documentId) ?: throw IllegalArgumentException("Document not found")

            val updatedContent = when (operation.type) {
                OperationType.insert -> document.content.substring(0, operation.position) +
                        (operation.content ?: "") +
                        document.content.substring(operation.position)
                OperationType.delete -> document.content.substring(0, operation.position) +
                        document.content.substring(operation.position + (operation.content?.length ?: 0))
                else -> throw IllegalArgumentException("Invalid operation type")
            }

            // Update the document and increment the version
            connection.prepareStatement(UPDATE_DOCUMENT).use { stmt ->
                stmt.setString(1, updatedContent)
                stmt.setObject(2, documentId)
                stmt.executeUpdate()
            }

            val appliedOperation = Operation(
                id = UUID.randomUUID(),
                documentId = documentId,
                type = operation.type,
                version = document.version + 1,
                position = operation.position,
                content = operation.content
            )

            // Log the operation
            connection.prepareStatement(CREATE_OPERATION).use { stmt ->
                stmt.setObject(1, appliedOperation.id)
                stmt.setObject(2, documentId)
                stmt.setString(3, appliedOperation.type.toString())
                stmt.setInt(4, appliedOperation.position)
                stmt.setString(5, appliedOperation.content)
                stmt.setInt(6, document.version + 1)
                stmt.executeUpdate()
            }

            return@withConnection appliedOperation
        }
    }

    fun createDocument(document: CreateDocumentRequest): Document {
        return withConnection { connection ->
            val createdDocument = Document(
                id = UUID.randomUUID(),
                name = document.name,
                content = document.content,
                version = 1
            )
            connection.prepareStatement(CREATE_DOCUMENT).use { stmt ->
                stmt.setObject(1, createdDocument.id)
                stmt.setString(2, createdDocument.name)
                stmt.setString(3, createdDocument.content)
                stmt.setInt(4, createdDocument.version)
                stmt.executeUpdate()
            }

            return@withConnection createdDocument

        }
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
           INSERT INTO operations (id, document_id, type, position, content, version, created_at)
           VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
           """

        private const val CREATE_DOCUMENT =
            """
           INSERT INTO documents (id, name, content, version, created_at)
           VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
           """
    }
}
