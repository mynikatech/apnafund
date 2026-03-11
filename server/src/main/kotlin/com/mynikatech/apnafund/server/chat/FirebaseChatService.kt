package com.mynikatech.apnafund.server.chat

import com.google.cloud.firestore.FieldValue
import com.google.firebase.cloud.FirestoreClient
import com.google.cloud.firestore.SetOptions

object FirebaseChatService {

    private val firestore = FirestoreClient.getFirestore()

    fun addMemberToGroup(groupId: Int, firebaseUid: String) {
        firestore
            .collection("groups")
            .document(groupId.toString())
            .set(
                mapOf(
                    "members" to FieldValue.arrayUnion(firebaseUid)
                ),
                SetOptions.merge()
            )
    }
}
