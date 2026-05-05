package com.mynikatech.apnafund.server.auth

import com.google.cloud.firestore.FieldValue
import com.google.cloud.firestore.Firestore
import com.google.firebase.cloud.FirestoreClient
import org.slf4j.LoggerFactory

object FirebaseGroupService {

    private val logger = LoggerFactory.getLogger("FirebaseGroupService")

    private val db: Firestore by lazy {
        FirestoreClient.getFirestore()
    }

    private fun groupRef(groupId: Int) =
        db.collection("groups").document(groupId.toString())

    /* ---------------- GROUP CREATE ---------------- */

    fun createGroup(
        groupId: Int,
        groupName: String,
        createdByUserId: Int? = null
    ) {
        try {
            val data = mutableMapOf<String, Any>(
                "name" to groupName,
                "createdAt" to FieldValue.serverTimestamp(),
                "members" to emptyMap<String, Boolean>()
            )

            createdByUserId?.let {
                val uid = "user_$it"
                data["createdBy"] = uid
                data["members"] = mapOf(uid to true)
            }

            val result = groupRef(groupId).set(data).get()
            println("Firestore group created at ${result.updateTime}")

        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
    /* ---------------- ADD MEMBER ---------------- */

    fun addMemberToGroup(groupId: Int, userId: Int) {
        val uid = "user_$userId"
        val docRef = groupRef(groupId)

        try {
            logger.info("Firebase: Updating group={} user={}", groupId, uid)
            val snapshot = docRef.get().get()
            val members = snapshot.get("members")
            when (members) {

                // CASE 1: Old array → convert to map
                is List<*> -> {
                    val newMap = members
                        .filterNotNull()
                        .associate { it.toString() to true }
                        .toMutableMap()

                    newMap[uid] = true

                    docRef.update("members", newMap)

                    logger.info("Firebase: Migrated array → map and added {}", uid)
                }

                // CASE 2: Already map → normal update
                is Map<*, *> -> {
                    docRef.update("members.$uid", true)

                    logger.info("Firebase: Member {} added successfully", uid)
                }

                // CASE 3: Null or missing → create fresh map
                else -> {
                    docRef.update("members", mapOf(uid to true))

                    logger.info("Firebase: Members initialized and added {}", uid)
                }
            }

        } catch (e: Exception) {
            logger.error("Firebase: Failed to add member {}", e.message, e)
            throw e
        }
    }

    /* ---------------- REMOVE MEMBER (optional) ---------------- */

    fun removeMemberFromGroup(groupId: Int, userId: Int) {
        val uid = "user_$userId"
        val docRef = groupRef(groupId)

        try {
            val snapshot = docRef.get().get()
            val members = snapshot.get("members")

            when (members) {

                // Old array → convert to map then remove
                is List<*> -> {
                    val newMap = members
                        .filterNotNull()
                        .map { it.toString() }
                        .filter { it != uid }
                        .associateWith { true }
                        .toMutableMap()

                    docRef.update("members", newMap)
                }

                // Map → normal delete
                is Map<*, *> -> {
                    docRef.update("members.$uid", FieldValue.delete())
                }

                else -> {
                    // nothing to remove
                }
            }

        } catch (e: Exception) {
            logger.error("Firebase: Failed to remove member {}", e.message, e)
            throw e
        }
    }

    fun addMembers(
        groupId: Int,
        userIds: List<Int>
    ) {
        val updates = userIds.associate {
            "members.user_$it" to true
        }

        groupRef(groupId).update(updates)
    }
}
