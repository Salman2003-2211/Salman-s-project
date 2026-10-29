package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.example.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreRepository(
    val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
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

    /**
     * Firestore rules require request.auth != null and userId == request.auth.uid.
     * Users who logged in through the local OTP / demo flow have no Firebase session,
     * so create an anonymous one on demand instead of inventing a fake "guest" uid
     * (which the rules always reject).
     */
    private suspend fun requireUserId(): String {
        auth.currentUser?.uid?.let { return it }
        return auth.signInAnonymously().await().user?.uid
            ?: throw IllegalStateException("Could not establish a Firebase session. Please sign in again.")
    }

    suspend fun saveUserProfile(
        fullName: String,
        phone: String,
        email: String,
        language: String = "en"
    ): Result<Unit> {
        return try {
            val uid = requireUserId()
            val path = "users/$uid"
            val ref = db.collection("users").document(uid)
            val data = hashMapOf<String, Any>(
                "userId" to uid,
                "fullName" to fullName,
                "phoneNumber" to phone,
                "email" to email,
                "language" to language,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            // createdAt must never change after creation (see firestore.rules)
            val existingDoc = ref.get().await()
            if (!existingDoc.exists()) {
                data["createdAt"] = FieldValue.serverTimestamp()
            } else {
                val existingCreatedAt = existingDoc.getTimestamp("createdAt")
                data["createdAt"] = existingCreatedAt ?: FieldValue.serverTimestamp()
            }
            ref.set(data, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users")
            Result.failure(e)
        }
    }

    suspend fun savePropertyListing(property: Property): Result<String> {
        val path = "properties"
        return try {
            val uid = requireUserId()
            val nowMs = System.currentTimeMillis()
            val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000L
            val expiresAtTimestamp = com.google.firebase.Timestamp(java.util.Date(nowMs + thirtyDaysMs))

            val data = hashMapOf<String, Any>(
                "id" to property.id,
                "userId" to uid,
                "ownerId" to uid,
                "ownerName" to property.ownerName,
                "ownerPhone" to property.ownerPhone,
                "listingType" to property.listingType.toFirestoreListingType(),
                "title" to property.title,
                "price" to property.price,
                "priceDisplay" to property.priceDisplay,
                "location" to "${property.locality}, ${property.city}",
                "locality" to property.locality,
                "city" to property.city,
                "description" to property.description,
                "bhk" to property.bhk,
                "propertyType" to property.propertyType,
                "areaGaj" to property.areaGaj,
                "carpetAreaSqFt" to property.carpetAreaSqFt,
                "furnishing" to property.furnishing,
                "imageUrls" to property.imageUrls,
                "imageUrl" to (property.imageUrls.firstOrNull() ?: property.imageUrl),
                "isSold" to property.isSold,
                "status" to "ACTIVE",
                "createdAt" to FieldValue.serverTimestamp(),
                "expiresAt" to expiresAtTimestamp,
                "lat" to property.lat,
                "lng" to property.lng,
                "validityDaysRemaining" to 30
            )

            val docRef = db.collection(path).document(property.id)
            docRef.set(data).await()
            Result.success(property.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    fun listenActiveProperties(): Flow<List<Property>> = callbackFlow {
        val listener = db.collection("properties")
            .whereEqualTo("status", "ACTIVE")
            .whereEqualTo("isSold", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("FirestoreRepository", "listenActiveProperties snapshot error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val nowMillis = System.currentTimeMillis()
                    val propertyList = snapshot.documents.mapNotNull { doc ->
                        docToProperty(doc)
                    }.filter { prop ->
                        !prop.isSold && prop.expiresAt > nowMillis
                    }
                    trySend(propertyList)
                }
            }
        awaitClose { listener.remove() }
    }

    private fun docToProperty(doc: com.google.firebase.firestore.DocumentSnapshot): Property? {
        return try {
            val id = doc.getString("id") ?: doc.id
            val title = doc.getString("title") ?: "Property Listing"
            val listingTypeStr = doc.getString("listingType") ?: "RENT"
            val listingType = ListingType.fromString(listingTypeStr)
            val propertyType = doc.getString("propertyType") ?: "Apartment"
            val bhk = doc.getString("bhk") ?: "2 BHK"
            val locality = doc.getString("locality") ?: doc.getString("location") ?: "Local Area"
            val city = doc.getString("city") ?: "Delhi NCR"
            val price = doc.getDouble("price") ?: 0.0
            val priceDisplay = doc.getString("priceDisplay") ?: formatPriceDisplay(
                price,
                listingType == ListingType.RENT || listingType == ListingType.LEASE
            )
            val areaGaj = doc.getLong("areaGaj")?.toInt() ?: 0
            val carpetAreaSqFt = doc.getLong("carpetAreaSqFt")?.toInt() ?: 1100
            val furnishing = doc.getString("furnishing") ?: "Semi-Furnished"
            val ownerId = doc.getString("ownerId") ?: doc.getString("userId") ?: ""
            val ownerName = doc.getString("ownerName") ?: "Owner"
            val ownerPhone = doc.getString("ownerPhone") ?: ""
            val imageUrl = doc.getString("imageUrl") ?: ""
            @Suppress("UNCHECKED_CAST")
            val imageUrls = (doc.get("imageUrls") as? List<String>) ?: listOfNotNull(imageUrl.ifBlank { null })
            val description = doc.getString("description") ?: ""
            val isSold = doc.getBoolean("isSold") ?: false
            val statusStr = doc.getString("status") ?: "ACTIVE"
            val status = try {
                PropertyStatus.valueOf(statusStr)
            } catch (_: Exception) {
                PropertyStatus.ACTIVE
            }
            val lat = doc.getDouble("lat") ?: 28.6280
            val lng = doc.getDouble("lng") ?: 77.3649
            val createdAtTs = doc.getTimestamp("createdAt")
            val expiresAtTs = doc.getTimestamp("expiresAt")
            val createdAt = createdAtTs?.toDate()?.time ?: System.currentTimeMillis()
            val expiresAt = expiresAtTs?.toDate()?.time ?: (createdAt + 30L * 24 * 60 * 60 * 1000L)

            val daysRemaining = ((expiresAt - System.currentTimeMillis()) / (24 * 60 * 60 * 1000L))
                .toInt()
                .coerceAtLeast(0)

            Property(
                id = id,
                title = title,
                listingType = listingType,
                propertyType = propertyType,
                bhk = bhk,
                locality = locality,
                city = city,
                distanceInfo = "Prime Location",
                price = price,
                priceDisplay = priceDisplay,
                areaGaj = areaGaj,
                carpetAreaSqFt = carpetAreaSqFt,
                furnishing = furnishing,
                isVerified = true,
                isOwnerListing = true,
                postedBy = "Owner",
                ownerId = ownerId,
                ownerName = ownerName,
                ownerPhone = ownerPhone,
                imageUrl = imageUrls.firstOrNull() ?: imageUrl,
                imageUrls = imageUrls,
                description = description,
                viewsCount = doc.getLong("viewsCount")?.toInt() ?: 1,
                inquiriesCount = doc.getLong("inquiriesCount")?.toInt() ?: 0,
                postedDate = "Recently",
                validityDaysRemaining = daysRemaining,
                isSold = isSold,
                status = status,
                lat = lat,
                lng = lng,
                createdAt = createdAt,
                expiresAt = expiresAt
            )
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Failed to parse property doc ${doc.id}: ${e.message}", e)
            null
        }
    }

    /**
     * Records client interest in a property as a lead in 'property_leads'
     * and syncs the favorite into the user's profile document and subcollection.
     */
    suspend fun recordPropertyInterestLead(
        property: Property,
        clientName: String,
        clientPhone: String,
        clientEmail: String = ""
    ): Result<String> {
        val path = "property_leads"
        return try {
            val uid = requireUserId()
            val leadId = "lead_${System.currentTimeMillis()}_${property.id.takeLast(6)}"
            val data = hashMapOf<String, Any>(
                "leadId" to leadId,
                "propertyId" to property.id,
                "propertyTitle" to property.title,
                "propertyPrice" to property.price,
                "propertyType" to property.listingType.toFirestoreListingType(),
                "ownerId" to property.ownerId.ifBlank { "owner" },
                "ownerName" to property.ownerName.ifBlank { "Owner" },
                "clientId" to uid,
                "userId" to uid,
                "clientName" to clientName.ifBlank { "Interested Client" },
                "clientPhone" to clientPhone.ifBlank { "9818452198" },
                "clientEmail" to clientEmail,
                "mobileNumber" to clientPhone.ifBlank { "9818452198" },
                "propertyQuery" to "Interested in ${property.title} (${property.priceDisplay})",
                "loanClientRequirements" to "Interested in purchasing/renting ${property.title}",
                "status" to "NEW_LEAD",
                "timestamp" to FieldValue.serverTimestamp(),
                "createdAt" to FieldValue.serverTimestamp()
            )

            db.collection(path).document(leadId).set(data).await()

            // 1. Sync into user document 'favorites' array
            try {
                db.collection("users").document(uid).update(
                    "favorites", FieldValue.arrayUnion(property.id)
                ).await()
            } catch (_: Exception) {
                try {
                    db.collection("users").document(uid).set(
                        mapOf("favorites" to listOf(property.id)),
                        SetOptions.merge()
                    ).await()
                } catch (_: Exception) {}
            }

            // 2. Also sync into users/{uid}/favorites/{propertyId} subcollection
            try {
                db.collection("users").document(uid)
                    .collection("favorites").document(property.id)
                    .set(
                        mapOf(
                            "propertyId" to property.id,
                            "addedAt" to FieldValue.serverTimestamp(),
                            "title" to property.title,
                            "priceDisplay" to property.priceDisplay,
                            "imageUrl" to property.imageUrl
                        )
                    ).await()
            } catch (_: Exception) {}

            Result.success(leadId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun removePropertyFavorite(propertyId: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            try {
                db.collection("users").document(uid).update(
                    "favorites", FieldValue.arrayRemove(propertyId)
                ).await()
            } catch (_: Exception) {}
            try {
                db.collection("users").document(uid)
                    .collection("favorites").document(propertyId)
                    .delete().await()
            } catch (_: Exception) {}
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadUserFavorites(): List<String> {
        return try {
            val uid = auth.currentUser?.uid ?: return emptyList()
            val doc = db.collection("users").document(uid).get().await()
            @Suppress("UNCHECKED_CAST")
            val favs = doc.get("favorites") as? List<String>
            if (!favs.isNullOrEmpty()) {
                favs
            } else {
                val subDocs = db.collection("users").document(uid).collection("favorites").get().await()
                subDocs.documents.map { it.id }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Registers a dealer/broker partner in 'partners' with status 'PENDING_APPROVAL'.
     */
    suspend fun registerPartner(partner: Partner): Result<String> {
        val path = "partners"
        return try {
            val uid = requireUserId()
            val data = hashMapOf<String, Any?>(
                "id" to partner.id,
                "userId" to uid,
                "name" to partner.name,
                "businessName" to partner.displayBusinessName,
                "phone" to partner.phone,
                "email" to partner.email,
                "areas" to partner.displayAreas,
                "dealTypes" to partner.displayDealTypes,
                "experienceYears" to partner.experienceYears,
                "documentUrls" to partner.documentUrls,
                "status" to "PENDING_APPROVAL",
                "createdAt" to FieldValue.serverTimestamp(),
                "approvedAt" to null,
                "rating" to 5.0,
                "reviewCount" to 0,
                "propertiesListedCount" to 0,
                "dealsClosedCount" to 0,
                "isVerified" to false
            )
            db.collection(path).document(partner.id).set(data).await()
            Result.success(partner.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    /**
     * Listens in real-time to approved partners for client-facing feed.
     */
    fun listenApprovedPartners(): Flow<List<Partner>> = callbackFlow {
        val listener = db.collection("partners")
            .whereEqualTo("status", "APPROVED")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("FirestoreRepository", "listenApprovedPartners error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { docToPartner(it) }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun getMyPartnerApplication(): Partner? {
        return try {
            val uid = auth.currentUser?.uid ?: return null
            val snap = db.collection("partners")
                .whereEqualTo("userId", uid)
                .get()
                .await()
            snap.documents.firstOrNull()?.let { docToPartner(it) }
        } catch (e: Exception) {
            Log.w("FirestoreRepository", "getMyPartnerApplication error: ${e.message}")
            null
        }
    }

    private fun docToPartner(doc: com.google.firebase.firestore.DocumentSnapshot): Partner? {
        return try {
            val id = doc.getString("id") ?: doc.id
            val userId = doc.getString("userId") ?: ""
            val name = doc.getString("name") ?: "Partner"
            val businessName = doc.getString("businessName") ?: doc.getString("agencyName") ?: "Independent Broker"
            val phone = doc.getString("phone") ?: ""
            val email = doc.getString("email") ?: ""
            @Suppress("UNCHECKED_CAST")
            val areas = (doc.get("areas") as? List<String>) ?: (doc.get("operatingAreas") as? List<String>) ?: listOf("Delhi NCR")
            @Suppress("UNCHECKED_CAST")
            val dealTypes = (doc.get("dealTypes") as? List<String>) ?: (doc.get("specializations") as? List<String>) ?: listOf("Residential Specialists")
            val experienceYears = doc.getLong("experienceYears")?.toInt() ?: 1
            @Suppress("UNCHECKED_CAST")
            val documentUrls = (doc.get("documentUrls") as? List<String>) ?: emptyList()
            val status = doc.getString("status") ?: "APPROVED"
            val rating = doc.getDouble("rating") ?: 4.8
            val reviewCount = doc.getLong("reviewCount")?.toInt() ?: 10
            val propertiesListedCount = doc.getLong("propertiesListedCount")?.toInt() ?: 5
            val dealsClosedCount = doc.getLong("dealsClosedCount")?.toInt() ?: 20
            val isVerified = doc.getBoolean("isVerified") ?: (status == "APPROVED")
            val createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis()
            val approvedAt = doc.getTimestamp("approvedAt")?.toDate()?.time

            Partner(
                id = id,
                userId = userId,
                name = name,
                businessName = businessName,
                agencyName = businessName,
                phone = phone,
                email = email,
                operatingAreas = areas,
                areas = areas,
                dealTypes = dealTypes,
                specializations = dealTypes,
                experienceYears = experienceYears,
                documentUrls = documentUrls,
                status = status,
                rating = rating,
                reviewCount = reviewCount,
                propertiesListedCount = propertiesListedCount,
                dealsClosedCount = dealsClosedCount,
                isVerified = isVerified,
                createdAt = createdAt,
                approvedAt = approvedAt
            )
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "docToPartner parsing error ${doc.id}", e)
            null
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
        val path = "properties"
        return try {
            val uid = requireUserId()
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
        val path = "loan_leads"
        return try {
            val uid = requireUserId()
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
        val path = "booked_meetings"
        return try {
            val uid = requireUserId()
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
        val path = "property_leads"
        return try {
            val uid = requireUserId()
            // Keep within the limits enforced by firestore.rules
            val data = hashMapOf<String, Any>(
                "mobileNumber" to mobileNumber.take(20),
                "propertyQuery" to propertyQuery.take(500),
                "loanClientRequirements" to loanClientRequirements.take(500),
                "clientName" to clientName.take(100),
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
