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
                data["createdBy"] = "user_$it"
                data["members"] = mapOf("user_$it" to true)
            }

            val result = groupRef(groupId).set(data).get()
            println("🔥 Firestore group created at ${result.updateTime}")

        } catch (e: Exception) {
            e.printStackTrace()   // 👈 THIS IS CRITICAL
            throw e               // let your controller return 500
        }
    }
    /* ---------------- ADD MEMBER ---------------- */

    fun addMemberToGroup(groupId: Int, userId: Int) {
        val uid = "user_$userId"

        try {
            logger.info("Firebase: Updating group={} user={}", groupId, uid)

            groupRef(groupId).update(
                "members.$uid", true
            )
            logger.info("Firebase: Member ={} added successfully", uid)

        } catch (e: Exception) {
            logger.error("Firebase: Failed to add member {}", e.message,e)
            throw e
        }
    }

    /* ---------------- REMOVE MEMBER (optional) ---------------- */

    fun removeMemberFromGroup(
        groupId: Int,
        userId: Int
    ) {
        val uid = "user_$userId"

        groupRef(groupId).update(
            "members.$uid", FieldValue.delete()
        )
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
