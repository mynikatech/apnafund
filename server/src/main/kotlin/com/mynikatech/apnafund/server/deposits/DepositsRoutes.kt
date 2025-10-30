package com.mynikatech.apnafund.server.deposits

import com.mynikatech.apnafund.net.dto.DepositsDto
import com.mynikatech.apnafund.net.dto.SaveOrUpdateDepositRequest
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
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** Request body for save-or-update batch then fetch */
data class SaveOrUpdateAllRequest(
    val deposits: List<DepositsDto>,
    val fundId: Int,
    val month: String,
    val year: String
)

fun Route.depositsRoutes(sql: DepositsSql) = route("/deposits") {

    // ---- Reads (with member names DTO) ----

    // GET /deposits/get/with-names/fund/{fundId}
    get("get/with-names/fund/{fundId}") {
        val fid = call.parameters["fundId"]?.toIntOrNull()
            ?: return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "fundId required"
            )
        call.respondOk(sql.getDepositsWithNamesForFund(fid))
    }

    // GET /deposits/get/with-names/fund/{fundId}/{month}/{year}
    get("get/with-names/fund/{fundId}/{month}/{year}") {
        val fid = call.parameters["fundId"]?.toIntOrNull()
            ?: return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "fundId required"
            )
        val month = call.parameters["month"] ?: return@get call.respondError(
            HttpStatusCode.BadRequest,
            "validation",
            "month required"
        )
        val year = call.parameters["year"] ?: return@get call.respondError(
            HttpStatusCode.BadRequest,
            "validation",
            "year required"
        )
        call.respondOk(sql.getDepositsWithNamesForFundMonthYear(fid, month, year))
    }

    // ---- Single deposit DTO ----

    // GET /deposits/get/{id}
    get("get/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@get call.respondError(HttpStatusCode.BadRequest, "validation", "id required")
        val dto = sql.getDeposit(id).firstOrNull()
        if (dto != null) call.respondOk(dto)
        else call.respondError(HttpStatusCode.NotFound, "not_found", "Deposit not found")
    }

    // GET /deposits/get/all
    get("get/all") {
        call.respondOk(sql.getAllDeposits())
    }


    // GET /deposits/get/one/fund/{fundId}/depositor/{depositorId}/{month}/{year}
    get("get/one/fund/{fundId}/depositor/{depositorId}/{month}/{year}") {
        val fid = call.parameters["fundId"]?.toIntOrNull()
            ?: return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "fundId required"
            )
        val did = call.parameters["depositorId"]?.toIntOrNull()
            ?: return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "depositorId required"
            )
        val month = call.parameters["month"] ?: return@get call.respondError(
            HttpStatusCode.BadRequest,
            "validation",
            "month required"
        )
        val year = call.parameters["year"] ?: return@get call.respondError(
            HttpStatusCode.BadRequest,
            "validation",
            "year required"
        )

        val d = sql.getDepositsForFundDepositorMonthYear(did, fid, month, year).firstOrNull()
        if (d != null) call.respondOk(d) else call.respondError(
            HttpStatusCode.NotFound,
            "not_found",
            "Deposit not found"
        )
    }

    // GET /deposits/get/with-names/full/fund/{fundId}/{month}/{year}
    get("get/with-names/full/fund/{fundId}/{month}/{year}") {
        val fid = call.parameters["fundId"]?.toIntOrNull()
            ?: return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "fundId required"
            )
        val month = call.parameters["month"] ?: return@get call.respondError(
            HttpStatusCode.BadRequest,
            "validation",
            "month required"
        )
        val year = call.parameters["year"] ?: return@get call.respondError(
            HttpStatusCode.BadRequest,
            "validation",
            "year required"
        )

        call.respondOk(sql.getAllDepositsForFundForMonthYear(fid, month, year))
    }

    // GET /deposits/get/member?fundId=&depositorId=&month=&year=
    get("get/member") {
        val fid = call.request.queryParameters["fundId"]?.toIntOrNull()
            ?: return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "fundId required"
            )
        val did = call.request.queryParameters["depositorId"]?.toIntOrNull()
            ?: return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "depositorId required"
            )
        val month = call.request.queryParameters["month"]
            ?: return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "month required"
            )
        val year = call.request.queryParameters["year"]
            ?: return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "year required"
            )

        val d = sql.getDepositForMember(fid, did, month, year).firstOrNull()
        if (d != null) call.respondOk(d) else call.respondError(
            HttpStatusCode.NotFound,
            "not_found",
            "Deposit not found"
        )
    }

    // GET /deposits/get/member/fund?fundId=&depositorId=
    get("get/member/fund") {
        val fid = call.request.queryParameters["fundId"]?.toIntOrNull()
            ?: return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "fundId required"
            )
        val did = call.request.queryParameters["depositorId"]?.toIntOrNull()
            ?: return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "depositorId required"
            )
        call.respondOk(sql.getDepositForMemberForFund(fid, did))
    }

    // ---- Creates / bulk ----

    // POST /deposits/add
    post("add") {
        val dto = call.receive<DepositsDto>()
        val id = sql.addDeposit(dto)
        call.respondOk(id, HttpStatusCode.Created)
    }

    // POST /deposits/add/bulk
    post("add/bulk") {
        val items = call.receive<List<DepositsDto>>()
        if (items.isEmpty()) return@post call.respondOk(Unit, HttpStatusCode.NoContent)
        val listOfDepositIds = sql.upsertDeposits(items)
        call.respondOk(listOfDepositIds, HttpStatusCode.Created)
    }

    // ---- Updates / deletes ----

    // PUT /deposits/update/{id}
    put("update/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@put call.respondError(HttpStatusCode.BadRequest, "validation", "id required")
        val dto = call.receive<DepositsDto>()
        val ok = sql.updateDeposit(
            id = id,
            depositorId = dto.depositorId,
            fundId = dto.fundId,
            depositedDate = dto.depositedDate,
            depositAmount = dto.depositAmount,
            depositMonth = dto.depositMonth,
            depositYear = dto.depositYear,
            lateFee = dto.lateFee
        )
        if (!ok) return@put call.respondError(
            HttpStatusCode.NotFound,
            "not_found",
            "Deposit not found"
        )
        call.respondOk(Unit, HttpStatusCode.NoContent)
    }

    // DELETE /deposits/delete/{id}
    delete("delete/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@delete call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "id required"
            )
        val ok = sql.deleteDeposit(id)
        if (!ok) return@delete call.respondError(
            HttpStatusCode.NotFound,
            "not_found",
            "Deposit not found"
        )
        call.respondOk(Unit, HttpStatusCode.NoContent)
    }

    // ---- Save-or-update batch, then fetch ----

    // POST /deposits/save-or-update/fetch
    post("save-or-update/fetch") {
        val req = call.receive<SaveOrUpdateDepositRequest>()
        val json = Json { explicitNulls = false }
        val depositsJson = json.encodeToString(ListSerializer(DepositsDto.serializer()), req.deposits)
        val rows = sql.saveOrUpdateAllDepositsAndFetch(
            fundId = req.fundId,
            month = req.month,
            year = req.year,
            rowsJson = depositsJson
        )
        call.respondOk(rows)
    }
}
