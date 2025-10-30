package com.mynikatech.apnafund.server.types

import com.mynikatech.apnafund.net.dto.TypeDto
import com.mynikatech.apnafund.server.api.respondError
import com.mynikatech.apnafund.server.api.respondOk
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.typesRoutes(sql: TypesSql) = route("/types") {

    // GET /types/get/all
    get("get/all") {
        call.respondOk(sql.getAllTypes())
    }

    // GET /types/get/{id}
    get("get/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@get call.respondError(HttpStatusCode.BadRequest, "validation", "id required")
        val dto = sql.getType(id).firstOrNull()
        if (dto != null) call.respondOk(dto)
        else call.respondError(HttpStatusCode.NotFound, "not_found", "Type not found")
    }

    // POST /types/add
    post("add") {
        val dto = call.receive<TypeDto>()
        if (dto.typeCode.isNullOrBlank())
            return@post call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "typeCode required"
            )
        val id = sql.addType(dto)
        call.respondOk( id, HttpStatusCode.Created)
    }

    // PUT /types/update/{id}
    put("update/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@put call.respondError(HttpStatusCode.BadRequest, "validation", "id required")
        val dto = call.receive<TypeDto>()
        val ok = sql.updateType(id, dto.typeCode, dto.typeDescription, dto.status)
        if (!ok) return@put call.respondError(
            HttpStatusCode.NotFound,
            "not_found",
            "Type not found"
        )
        call.respondOk(Unit, HttpStatusCode.NoContent)
    }

    // DELETE /types/delete/{id}
    delete("delete/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@delete call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "id required"
            )
        val ok = sql.deleteType(id)
        if (!ok) return@delete call.respondError(
            HttpStatusCode.NotFound,
            "not_found",
            "Type not found"
        )
        call.respondOk(Unit, HttpStatusCode.NoContent)
    }
}
