package com.mynikatech.apnafund.server.funds

import com.mynikatech.apnafund.net.dto.AddFundWithDetailsRequest
import com.mynikatech.apnafund.net.dto.CloseFundRequest
import com.mynikatech.apnafund.net.dto.FundDetailsDto
import com.mynikatech.apnafund.net.dto.FundMembersDto
import com.mynikatech.apnafund.net.dto.FundUpdateRequestDto
import com.mynikatech.apnafund.net.dto.FundsDto
import com.mynikatech.apnafund.net.dto.UsersDto
import com.mynikatech.apnafund.server.api.respondError
import com.mynikatech.apnafund.server.api.respondOk
import com.mynikatech.apnafund.server.db.Db.jdbi
import com.mynikatech.apnafund.server.notifications.NotificationService
import com.mynikatech.apnafund.server.users.UsersSql
import com.mynikatech.apnafund.server.util.Converters
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

data class UpdateWithDetailsRequest(
    val fund: FundsDto,
    val details: FundDetailsDto
)

fun Route.fundsRoutes(sql: FundsSql, notificationService: NotificationService,
                      userSql: UsersSql) = route("/funds") {

    // ---- Funds (DTO/basic) ----
    get("get/all") { call.respondOk(sql.getAllFunds()) }

    get("get/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@get call.respondError(HttpStatusCode.BadRequest, "validation", "id required")
        val dto = sql.getFund(id).firstOrNull()
        if (dto != null) call.respondOk(dto)
        else call.respondError(HttpStatusCode.NotFound, "not_found", "Fund not found")
    }

    post("add") {
        val dto = call.receive<FundsDto>()

        if (dto.fundName.isBlank()) {
            return@post call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "fundName required"
            )
        }
        val fundId = jdbi.inTransaction<Int, Exception> { handle ->

            val fundId = sql.addFund(dto)

            // UPSERT FUND MODERATOR ROLE
            userSql.upsertUserRoleByCode(
                dto.moderator,   // make sure this exists in DTO
                "FUND_MODERATOR",
                "ACTIVE"
            )
            fundId
        }

        call.respondOk(fundId, HttpStatusCode.Created)
    }

    put("update/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@put call.respondError(HttpStatusCode.BadRequest, "validation", "id required")
        val dto = call.receive<FundsDto>()
        val ok = sql.updateFund(
            id = id,
            fundName = dto.fundName,
            fundStartDate = dto.fundStartDate,
            fundMaturityDate = dto.fundMaturityDate,
            fundPeriod = dto.fundPeriod,
            depositionFrequency = dto.depositionFrequency,
            moderator = dto.moderator,
            recurringDepositAmount = dto.recurringDepositAmount,
            fundStatus = dto.fundStatus,
            loanInterestRate = dto.loanInterestRate,
            lateFeeRate = dto.lateFeeRate,
            monthlyDepDateBy = dto.monthlyDepDateBy,
            groupId = dto.groupId,
            fundCode = dto.fundCode
        )
        if (!ok) return@put call.respondError(
            HttpStatusCode.NotFound,
            "not_found",
            "Fund not found"
        )
        call.respondOk(Unit, HttpStatusCode.NoContent)
    }

    delete("delete/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@delete call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "id required"
            )
        call.respondOk(sql.deleteFund(id)) // Boolean body, but wrapped in respondOk for consistency
    }

    // ---- Funds (domain queries) ----
    get("get/active") { call.respondOk(sql.getAllActiveFunds()) }

    get("get/by-code") {
        val code = call.request.queryParameters["fundCode"]
            ?: return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "fundCode required"
            )
        call.respondOk(sql.getFundIdByCode(code))
    }

    get("get/active/group/{groupId}") {
        val gid = call.parameters["groupId"]?.toIntOrNull()
            ?: return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "groupId required"
            )
        call.respondOk(sql.getAllActiveFundsForGroup(gid))
    }

    get("get/active/group/{groupId}/{userId}") {
        val gid = call.parameters["groupId"]?.toIntOrNull()
            ?: return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "groupId required"
            )
        val uid = call.parameters["userId"]?.toIntOrNull()
            ?: return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "userId required"
            )
        call.respondOk(sql.getAllActiveFundsForGroupAndModerator(gid, uid))
    }

    get("get/group/{groupId}") {
        val gid = call.parameters["groupId"]?.toIntOrNull()
            ?: return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "groupId required"
            )
        call.respondOk(sql.getAllFundsForGroup(gid))
    }

    // ---- Funds + Details projections ----
    get("get/with-details/all") { call.respondOk(sql.allWithDetails()) }

    get("get/with-details/group/{groupId}") {
        val start = System.currentTimeMillis()
        val gid = call.parameters["groupId"]?.toIntOrNull()
            ?: return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "groupId required"
            )
        val result = sql.allWithDetailsForGroup(gid)
        val stop = System.currentTimeMillis()
        val timeTaken = stop - start
        call.application.log.info("The time to call DB SQL is : $timeTaken")
        call.respondOk(result)
    }

    get("get/with-details/{fundId}") {
        val fid = call.parameters["fundId"]?.toIntOrNull()
            ?: return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "fundId required"
            )
        call.respondOk(sql.getFundWithDetails(fid))
    }

    // ---- Rate of interest ----
    get("roi/get/{fundId}") {
        val fid = call.parameters["fundId"]?.toIntOrNull()
            ?: return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "fundId required"
            )
        call.respondOk(sql.getRateOfInterestForFund(fid))
    }

    // ---- Members ----
    get("members/get/{fundId}") {
        val fid = call.parameters["fundId"]?.toIntOrNull()
            ?: return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "fundId required"
            )
        call.respondOk(sql.getFundMembers(fid))
    }

    get("members/get/with-names/{fundId}") {
        val fundId = call.parameters["fundId"]?.toIntOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, "fundId required")

        call.safeRoute(
            logMessage = "Error fetching fund members with name for fundId=$fundId",
            clientMessage = "Unable to fetch fund members details"
        ) {
            val members = sql.getFundMembersWithNamesForFund(fundId)
            call.respondOk(members)
        }

    }

    post("members/add/one") {
        val body = call.receive<FundMembersDto>()
        val id = sql.addFundMember(body)
        // Fetch full member details for email
        val member = userSql.getUsersBasicByIds(intArrayOf(body.userId))

        val fund = sql.getFund(body.fundId).firstOrNull()
        if (null != fund) {
            CoroutineScope(Dispatchers.IO).launch {
                notificationService.notifyFundMembersAdded(
                    fundId = body.fundId,
                    fundName = fund.fundName,
                    newMembers = member
                )
            }
        }
        call.respondOk(id)
    }

    post("members/add/batch") {

        val items: List<FundMembersDto> = call.receive()

        if (items.isEmpty()) {
            return@post call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "No members provided"
            )
        }

        val json = Json { explicitNulls = false }
        val payload: String =
            json.encodeToString(ListSerializer(FundMembersDto.serializer()), items)

        val ids: List<Int> = sql.addFundMembersBatch(payload)

        // Fetch member details
        val fundMemberIds = items.map { it.userId }.toIntArray()
        val members = userSql.getUsersBasicByIds(fundMemberIds)

        val fundId = items.first().fundId
        val fundDetails = sql.getFund(fundId).firstOrNull()
        if( null != fundDetails) {
            call.application.launch {
                notificationService.notifyFundMembersAdded(
                    fundId = items.first().fundId,
                    fundName = fundDetails.fundName,
                    newMembers = members
                )
            }
        }
        call.respondOk(ids, HttpStatusCode.Created)
    }

    put("update/member") {
        val fm = call.receive<FundMembersDto>()

        sql.updateFundMember(fm)

        call.respondOk(Unit, HttpStatusCode.NoContent)
    }

    delete("members/delete/{fundMemberId}") {
        val fmId = call.parameters["fundMemberId"]?.toIntOrNull()
            ?: return@delete call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "fundMemberId required"
            )
        call.respondOk(sql.removeFundMember(fmId))
    }

    // ---- Details ----
    get("details/get/{fundId}") {
        val fid = call.parameters["fundId"]?.toIntOrNull()
            ?: return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "fundId required"
            )
        val d = sql.getFundDetails(fid).firstOrNull()
        if (d != null) call.respondOk(d) else call.respondError(
            HttpStatusCode.NotFound,
            "not_found",
            "Details not found"
        )
    }

    post("details/insert") {
        val body = call.receive<FundDetailsDto>()
        sql.insertFundDetails(body)
        call.respondOk(Unit, HttpStatusCode.NoContent)
    }

    put("details/update") {
        val body = call.receive<FundDetailsDto>()
        sql.updateFundDetails(body)
        call.respondOk(Unit, HttpStatusCode.NoContent)
    }

    post("details/upsert") {
        val body = call.receive<FundDetailsDto>()
        call.respondOk(sql.upsertDetails(body))
    }

    // ---- Available amounts / eligible members ----
    get("available-amount/get/{fundId}") {
        val fid = call.parameters["fundId"]?.toIntOrNull()
            ?: return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "fundId required"
            )
        call.respondOk(sql.availableAmount(fid))
    }

    get("available-amounts/get/{fundId}") {

        val fid = call.parameters["fundId"]?.toIntOrNull()
            ?: return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "fundId required"
            )

        call.safeRoute(
            logMessage = "Error fetching fund availability for fundId=$fid",
            clientMessage = "Unable to fetch fund availability"
        ) {
            sql.getFundAvailability(fid)
        }
    }

    get("available-members/get") {
        val gid = call.request.queryParameters["groupId"]?.toIntOrNull()
            ?: return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "groupId required"
            )
        val fid = call.request.queryParameters["fundId"]?.toIntOrNull()
            ?: return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "fundId required"
            )

        call.safeRoute(
            logMessage = "Error fetching fund availability for fundId=$fid",
            clientMessage = "Unable to fetch fund availability"
        ) {
            sql.getAvailableFundMembers(gid, fid)
        }
    }

    // ---- Atomic multi-update ----
    put("update/with-details") {
        val body = call.receive<FundUpdateRequestDto>()
        val ok = sql.updateFund(
            id = body.fund.fundId!!,
            fundName = body.fund.fundName,
            fundStartDate = body.fund.fundStartDate,
            fundMaturityDate = body.fund.fundMaturityDate,
            fundPeriod = body.fund.fundPeriod,
            depositionFrequency = body.fund.depositionFrequency,
            moderator = body.fund.moderator,
            recurringDepositAmount = body.fund.recurringDepositAmount,
            fundStatus = body.fund.fundStatus,
            loanInterestRate = body.fund.loanInterestRate,
            lateFeeRate = body.fund.lateFeeRate,
            monthlyDepDateBy = body.fund.monthlyDepDateBy,
            groupId = body.fund.groupId,
            fundCode = body.fund.fundCode
        )
        sql.updateFundDetails(body.fundDetails)
        if (!ok) return@put call.respondError(
            HttpStatusCode.NotFound,
            "not_found",
            "Fund not found"
        )
        call.respondOk(Unit, HttpStatusCode.NoContent)
    }

    get("members/check/{fundId}") {
        val fundId = call.parameters["fundId"]?.toIntOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, "fundId required")
        val userId = call.request.queryParameters["userId"]?.toIntOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, "userId required")
        val exists = sql.checkIfFundMemberAlreadyAdded(userId, fundId)
        call.respondOk(exists)
    }

    post("add/with-details") {
        val req = call.receive<AddFundWithDetailsRequest>()


        if (req.fund.groupId == null) {
            return@post call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "groupId required on fund"
            )
        }

        // One DB call that does both inserts atomically
        val json = Json { explicitNulls = false }

        val fundToSave = req.fund
        val fundDetailsToSave = req.details

        val memberships = mutableListOf<FundMembersDto>()

        // Add creator only if included
        if (!req.excludeCreator) {

            memberships.add(
                FundMembersDto(
                    userId = req.requestorId,
                    fundId = 0, // temp, set later
                    joiningDate = Converters.getCurrentDate(),
                    role =
                        if (fundToSave.moderator == req.requestorId)
                            "PRIMARY_MODERATOR"
                        else
                            "MODERATOR",
                    updatedBy = req.requestorId
                )
            )
        }

        // Add moderator if different
        if (fundToSave.moderator != req.requestorId) {

            memberships.add(
                FundMembersDto(
                    userId = fundToSave.moderator,
                    fundId = 0, // temp, set later
                    joiningDate = Converters.getCurrentDate(),
                    role = "PRIMARY_MODERATOR",
                    updatedBy = req.requestorId
                )
            )
        }

        val totalMembers = memberships.size

        val totalExpectedDeposit =
            fundToSave.recurringDepositAmount *
                    totalMembers *
                    fundToSave.fundPeriod

        val updatedFundDetails = fundDetailsToSave.copy(
            totalExpectedDeposit = totalExpectedDeposit,
            totalExpectedMaturityAmount = totalExpectedDeposit
        )

        val fundJson =
            json.encodeToString(
                FundsDto.serializer(),
                fundToSave
            )

        val detailsJson =
            json.encodeToString(
                FundDetailsDto.serializer(),
                updatedFundDetails
            )

        val newId = jdbi.inTransaction<Int, Exception> { handle ->

            val newFundId =
                sql.addFundWithDetails(
                    fundJson,
                    detailsJson
                )

            memberships.forEach { member ->

                sql.addFundMember(
                    member.copy(fundId = newFundId)
                )
            }

            newFundId
        }

        // Trigger fund created notification (async)
        call.application.launch {

            notificationService.notifyFundCreated(
                fundId = newId,
                fundName = req.fund.fundName,
                groupId = req.fund.groupId
            )
        }

        call.respondOk(newId, HttpStatusCode.Created)
    }

    post("/{fundId}/close") {

        val fundId =
            call.parameters["fundId"]?.toIntOrNull()

        if (fundId == null) {
            call.respondError(
                HttpStatusCode.BadRequest,
                type = "INVALID_ID",
                message = "Invalid fund id"
            )
            return@post
        }

        val request =
            call.receive<CloseFundRequest>()

        try {

            // ---------- CLOSE FUND ----------
            val closed =
                sql.closeFund(
                    fundId,
                    request.closedBy,
                    request.reason
                )

            if (!closed) {
                call.respondError(
                    status = HttpStatusCode.Conflict,
                    type = "FUND_CLOSE_FAILED",
                    message = "Fund already closed or not found"
                )
                return@post
            }

            // ---------- FETCH DETAILS FOR NOTIFICATION ----------
            val fund =
                sql.getFund(fundId).firstOrNull()

            val closedByUser =
                userSql.getUserById(request.closedBy)

            // ---------- TRIGGER NOTIFICATIONS ----------
            if( null != fund) {
                notificationService.notifyFundClosed(
                    fundId = fundId,
                    fundName = fund.fundName,
                    closedByName = closedByUser.fullName, // if required moderator
                    reason = request.reason
                )
            } else
            {
                // no fund to close.
            }

            // ---------- RESPONSE ----------
            call.respond(HttpStatusCode.NoContent)

        } catch (e: Exception) {
            call.application.log.error(e.message)
            call.respondError(
                status = HttpStatusCode.InternalServerError,
                type = "SERVER_ERROR",
                message = "Unable to close fund"
            )
        }
    }
}

inline suspend fun <reified T> ApplicationCall.safeRoute(
    logMessage: String,
    clientMessage: String,
    block: suspend () -> T
) {
    try {
        respondOk(block())
    } catch (e: Exception) {

        application.log.error(logMessage, e)

        respondError(
            HttpStatusCode.InternalServerError,
            "internal",
            clientMessage
        )
    }
}

