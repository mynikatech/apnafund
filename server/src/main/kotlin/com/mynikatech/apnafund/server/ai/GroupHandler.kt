package com.mynikatech.apnafund.server.ai

import com.mynikatech.apnafund.net.dto.AIAction
import com.mynikatech.apnafund.net.dto.AIActionItem
import com.mynikatech.apnafund.net.dto.AIContext
import com.mynikatech.apnafund.net.dto.AIEntity
import com.mynikatech.apnafund.net.dto.AIIntentResult
import com.mynikatech.apnafund.net.dto.AIResponse
import com.mynikatech.apnafund.net.dto.AIResponseType
import com.mynikatech.apnafund.net.dto.GroupsDto
import com.mynikatech.apnafund.net.dto.UIActionType
import com.mynikatech.apnafund.server.users.UserFinanceService
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

class GroupHandler(
    private val userFinanceService: UserFinanceService,
    private val responseBuilder: AIResponseBuilder
) {

    fun handle(
        intent: AIIntentResult,
        context: AIContext
    ): AIResponse {

        val groups = getFilteredGroups(context.userId, intent.filters, context)
        val filters = intent.filters

        return when (intent.action) {

            AIAction.LIST -> buildGroupList(groups, filters)

            AIAction.MEMBERS -> buildGroupMembers(groups, filters)

            AIAction.CREATE -> AIResponse(
                reply = "You can add a new group from the Home screen.",
                type = AIResponseType.TEXT,
                actions = listOf(
                    AIActionItem("Add Group", UIActionType.OPEN_ADD_GROUP)
                )
            )

            AIAction.NAVIGATE -> AIResponse(
                reply = "Go to the Home section to add a new group.",
                type = AIResponseType.TEXT,
                actions = listOf(
                    AIActionItem("Open Group", UIActionType.OPEN_GROUPS)
                )
            )

            else -> generic()
        }
    }

    private fun getFilteredGroups(
        userId: Int,
        filters: JsonObject,
        context: AIContext
    ): List<GroupsDto> {

        val allGroups = userFinanceService.getGroupssForUser(userId)

        val groupName = filters["groupName"]?.jsonPrimitive?.contentOrNull
        val scope = filters["scope"]?.jsonPrimitive?.contentOrNull
        val statusFilter = filters["status"]?.jsonPrimitive?.contentOrNull


        val resolvedGroups = when {

            groupName != null -> {
                val inputs = groupName
                    .split(",", "&")
                    .map { it.trim().lowercase() }

                allGroups.filter { group ->
                    inputs.any { input ->
                        group.groupName.lowercase().contains(input) ||
                                group.groupName.lowercase().contains(input)
                    }
                }
            }

            scope == "ALL" -> allGroups

            context.activeGroupId != null -> {
                allGroups.filter { it.groupId == context.activeGroupId }
            }

            else -> allGroups
        }

        // 🔹 STEP 2: Filter (status)
        return resolvedGroups.filter { group ->

            val statusMatch = statusFilter?.let { input ->
                StatusMapper.map(AIEntity.GROUP, input).any {
                    group.status.equals(it, ignoreCase = true)
                }
            } ?: true

            statusMatch
        }
    }

    // --------------------------
    // LIST
    // --------------------------

    private fun buildGroupList(
        groups: List<GroupsDto>,
        filters: JsonObject
    ): AIResponse {

        if (groups.isEmpty()) {
            return responseBuilder.buildEmptyResponse(AIEntity.GROUP, filters)
        }

        return AIResponse(
            reply = "Here are your ${groups.size} groups(s):",
            type = AIResponseType.TABLE,
            data = buildTable(groups),
            actions = listOf(
                AIActionItem("View Groups", UIActionType.OPEN_GROUPS)
            )
        )
    }


    // --------------------------
    // COMMON HELPERS
    // --------------------------

    private fun buildTable(groups: List<GroupsDto>): JsonElement {
        return buildJsonObject {

            put("columns", buildJsonArray {
                add(JsonPrimitive("Group Name"))
                add(JsonPrimitive("Moderator"))
                add(JsonPrimitive("Created Date"))
                add(JsonPrimitive("Status"))

            })

            put("rows", buildJsonArray {
                for (group in groups) {
                    add(buildJsonArray {
                        add(JsonPrimitive(group.groupName))
                        add(JsonPrimitive(group.moderator))
                        add(JsonPrimitive(group.createdDate))
                        add(JsonPrimitive(group.status))
                    })
                }
            })
        }
    }

    private fun buildGroupMembers(
        groups: List<GroupsDto>,
        filters: JsonObject
    ): AIResponse {

        if (groups.isEmpty()) {
            return responseBuilder.buildEmptyResponse(AIEntity.GROUP, filters)
        }

        val members = groups
            .mapNotNull { it.groupId } // remove nulls
            .flatMap { groupId ->
                userFinanceService.getGroupMembersWithNamesForGroup(groupId)
            }

        if (members.isEmpty()) {
            return responseBuilder.buildEmptyResponse(AIEntity.GROUP, filters)
        }

        val groupNames = groups.joinToString { it.groupName }

        return AIResponse(
            reply = "Here are the members of $groupNames:",
            type = AIResponseType.TABLE,
            data = buildJsonObject {
                put("columns", buildJsonArray {
                    add(JsonPrimitive("Name"))
                    add(JsonPrimitive("Joining Date"))
                })
                put("rows", buildJsonArray {
                    members.forEach {
                        add(buildJsonArray {
                            add(JsonPrimitive("${it.firstName} ${it.lastName}"))
                            add(JsonPrimitive(it.joiningDate))

                        })
                    }
                })
            }
        )
    }

    private fun generic() = AIResponse(
        reply = "I didn’t quite get that. Try asking things like:\\n• Show my funds\\n• Show fund members \\n• Show fund summary.\"",
        type = AIResponseType.TEXT
    )
}