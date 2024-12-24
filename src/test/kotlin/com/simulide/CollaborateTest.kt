package com.simulide

import com.simulide.domain.OperationType
import com.simulide.plugins.CreateOperationRequest
import com.simulide.plugins.configureCollaboration
import com.simulide.plugins.domain.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.server.util.*
import io.ktor.websocket.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.*
import javax.sql.DataSource
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CollaborateTest {
    private lateinit var inMemoryDb: DataSource
    private lateinit var documentService: DocumentService

    val document = Document(
        name = "Test doc here",
        content = "",
        id = UUID.randomUUID(),
        version = 1
    )


    @Before
    fun setup() {
        inMemoryDb = setupTestDatabase()
        documentService = DocumentService(inMemoryDb)
    }

    @After
    fun teardown() {
        cleanupDatabase(dataSource = inMemoryDb)
    }

    @Test
    fun `test WebSocket collaboration`() = testApplication {
        application {
            configureCollaboration(documentService)
        }

        createDocument(inMemoryDb, document)

        val testClient = createClient {
            install(WebSockets)
        }

        val documentId = document.id
        val endpoint = "/ws/collaborate/$documentId"

        val connections = mutableListOf<WebSocketSession>()
        val numConnections = 3
        val receivedMessages = mutableListOf<String>()

        repeat(numConnections) {
            val session = testClient.webSocketSession(endpoint)
            connections.add(session)
            for (frame in session.incoming) {
                if (frame is Frame.Text){
                    receivedMessages.add(frame.readText())
                }
            }

        }

        val operation = CreateOperationRequest(
            type = OperationType.insert,
            position = 0,
            content = "Hello",
            documentId = documentId.toString(),
            length = 5
        )

        connections.first().send(Frame.Text(jsonConfiguration.encodeToString(operation)))
        delay(3000)

        assertEquals(numConnections-1, receivedMessages.size)

        receivedMessages.forEach {
            val message = jsonConfiguration.decodeFromString<Operation>(it)
            assertEquals(OperationType.insert, message.type)
            assertEquals("Hello", message.content)
            assertEquals(documentId, message.documentId)
            assertEquals(operation.position, message.position)
            assertEquals(operation.length, message.length)
        }


        connections.forEach { it.close() }
    }

}
