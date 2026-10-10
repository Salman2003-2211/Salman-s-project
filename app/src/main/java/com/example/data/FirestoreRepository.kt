package com.example.data

import android.content.Context
import com.example.R
import com.example.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreRepository(
    val db: FirebaseFirestore = try {
        FirebaseFirestore.getInstance()
    } catch (e: Exception) {
        FirebaseFirestore.getInstance()
    },
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    // Secondary constructor resolving custom database ID from string resources
    constructor(context: Context) : this(
        try {
            val dbId = context.applicationContext.getString(R.string.firestore_database_id)
            if (dbId.isNotBlank() && dbId != "(default)") {
                FirebaseFirestore.getInstance(dbId)
            } else {
                FirebaseFirestore.getInstance()
            }
        } catch (e: Exception) {
            FirebaseFirestore.getInstance()
        }
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: "guest_user_${System.currentTimeMillis()}"
    }

    suspend fun saveUserProfile(
        fullName: String,
        phone: String,
        email: String,
        language: String = "en"
    ): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid"
        return try {
            val data = mapOf(
                "userId" to uid,
                "fullName" to fullName,
                "phoneNumber" to phone,
                "email" to email,
                "language" to language,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun createProperty(
        title: String,
        listingType: String,
        propertyType: String,
        bhk: String,
        locality: String,
        price: Double,
        priceDisplay: String,
        description: String,
        ownerName: String,
        ownerPhone: String
    ): Result<String> {
        val uid = requireUserId()
        val path = "properties"
        return try {
            val data = mapOf(
                "userId" to uid,
                "title" to title,
                "listingType" to listingType,
                "propertyType" to propertyType,
                "bhk" to bhk,
                "locality" to locality,
                "price" to price,
                "priceDisplay" to priceDisplay,
                "description" to description,
                "ownerName" to ownerName,
                "ownerPhone" to ownerPhone,
                "status" to "ACTIVE",
                "createdAt" to FieldValue.serverTimestamp()
            )
            val docRef = db.collection(path).add(data).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun createLoanLead(
        clientName: String,
        phoneNumber: String,
        loanTypeSelected: String,
        requiredAmountInLakhs: Double,
        employmentType: String,
        monthlyIncome: String,
        city: String
    ): Result<String> {
        val uid = requireUserId()
        val path = "loan_leads"
        return try {
            val data = mapOf(
                "userId" to uid,
                "clientName" to clientName,
                "phoneNumber" to phoneNumber,
                "loanTypeSelected" to loanTypeSelected,
                "requiredAmountInLakhs" to requiredAmountInLakhs,
                "employmentType" to employmentType,
                "monthlyIncome" to monthlyIncome,
                "city" to city,
                "status" to "PENDING",
                "createdAt" to FieldValue.serverTimestamp()
            )
            val docRef = db.collection(path).add(data).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun bookPartnerMeeting(
        partnerId: String,
        partnerName: String,
        partnerPhone: String,
        scheduledDate: String,
        timeSlot: String,
        propertyInterest: String
    ): Result<String> {
        val uid = requireUserId()
        val path = "booked_meetings"
        return try {
            val data = mapOf(
                "userId" to uid,
                "partnerId" to partnerId,
                "partnerName" to partnerName,
                "partnerPhone" to partnerPhone,
                "scheduledDate" to scheduledDate,
                "timeSlot" to timeSlot,
                "propertyInterest" to propertyInterest,
                "status" to "CONFIRMED",
                "createdAt" to FieldValue.serverTimestamp()
            )
            val docRef = db.collection(path).add(data).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    /**
     * Saves user submitted lead with Mobile Number, Property Query, and Loan Client Requirements
     * into Firestore collection "property_leads".
     */
    suspend fun savePropertyLead(
        mobileNumber: String,
        propertyQuery: String,
        loanClientRequirements: String,
        clientName: String = ""
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: requireUserId()
        val path = "property_leads"
        return try {
            val data = hashMapOf<String, Any>(
                "mobileNumber" to mobileNumber,
                "propertyQuery" to propertyQuery,
                "loanClientRequirements" to loanClientRequirements,
                "clientName" to clientName,
                "userId" to uid,
                "status" to "PENDING",
                "createdAt" to FieldValue.serverTimestamp()
            )
            val docRef = try {
                db.collection(path).add(data).await()
            } catch (e: Exception) {
                // If custom database failed or not yet provisioned, fallback to default instance
                val defaultDb = FirebaseFirestore.getInstance()
                if (defaultDb != db) {
                    defaultDb.collection(path).add(data).await()
                } else {
                    throw e
                }
            }
            android.util.Log.i("FirestoreRepository", "Successfully saved property_lead: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }
}
