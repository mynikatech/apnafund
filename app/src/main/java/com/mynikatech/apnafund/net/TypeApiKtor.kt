package com.mynikatech.apnafund.net

import com.mynikatech.apnafund.data.model.Type
import com.mynikatech.apnafund.net.dto.TypeDto
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking

class TypeApiKtor(
    private val clientProvider: () -> io.ktor.client.HttpClient = { HttpClientProvider.client }
) : TypeApi {

    private val client get() = clientProvider()

    override suspend fun addType(type: TypeDto) {
        client.post("/types/add") {
            contentType(ContentType.Application.Json)
            setBody(type)
        }.body<Unit>()
    }

    override fun getAllTypes(): List<TypeDto> = runBlocking {
        client.get("/types/get/all").unwrap<List<TypeDto>>()
    }

    override suspend fun updateType(type: TypeDto) {
        val id = requireNotNull(
            (type as? Any)?.let { t ->
                try { Type::class.java.getDeclaredField("typeId").apply { isAccessible = true }.get(t) as Int? }
                catch (_: Exception) { null }
            } ?: (type::class.members.firstOrNull { it.name == "typeId" }?.call(type) as? Int?)
        ) { "typeId required for update" }

        client.put("/types/update/$id") {
            contentType(ContentType.Application.Json)
            setBody(type)
        }.body<Unit>()
    }

    override suspend fun deleteType(type: TypeDto) {
        val id = requireNotNull(
            try { Type::class.java.getDeclaredField("typeId").apply { isAccessible = true }.get(type) as Int? }
            catch (_: Exception) { null }
        ) { "typeId required for delete" }

        client.delete("/types/delete/$id").body<Unit>()
    }
}
