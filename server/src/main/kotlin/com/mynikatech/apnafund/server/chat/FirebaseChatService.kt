package com.mynikatech.apnafund.server.chat

import com.google.cloud.firestore.FieldValue
import com.google.firebase.cloud.FirestoreClient

object FirebaseChatService {

    private val firestore = FirestoreClient.getFirestore()

    fun addMemberToGroup(groupId: Int, firebaseUid: String) {
        firestore
            .collection("groups")
            .document(groupId.toString())
            .update(
                "members",
                FieldValue.arrayUnion(firebaseUid)
            )
    }
}
