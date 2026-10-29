package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.example.ui.screens.PropertyPhotoItem
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.tasks.await

object PropertyStorageHelper {
    private const val TAG = "PropertyStorageHelper"
    private val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }

    /**
     * Sanitizes string for clean directory paths in Firebase Storage
     * e.g. "Rahul Rajpoot" -> "Rahul_Rajpoot".
     */
    fun sanitizeSegment(input: String): String {
        return input.trim()
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
            .ifBlank { "user" }
    }

    /**
     * Uploads property photos to the organized user-segregated folder structure:
     * 'properties/{ownerId}_{ownerName}/{purpose}_{propertyId}/{image_filename}.jpg'
     *
     * where purpose is 'RENT', 'LEASE', or 'SELL'.
     */
    suspend fun uploadPropertyPhotos(
        context: Context,
        ownerId: String,
        ownerName: String,
        purpose: String,
        propertyId: String,
        photos: List<PropertyPhotoItem>,
        onProgress: ((uploaded: Int, total: Int) -> Unit)? = null
    ): List<String> {
        if (photos.isEmpty()) return emptyList()

        val safeOwner = sanitizeSegment(ownerName)
        val safeOwnerId = sanitizeSegment(ownerId)
        val safePurpose = purpose.uppercase().trim()
        val downloadUrls = mutableListOf<String>()

        for ((index, photo) in photos.withIndex()) {
            val filename = "photo_${index + 1}_${System.currentTimeMillis()}.jpg"
            val storagePath = "properties/${safeOwnerId}_${safeOwner}/${safePurpose}_${propertyId}/$filename"
            val storageRef = storage.reference.child(storagePath)

            try {
                val metadata = StorageMetadata.Builder()
                    .setContentType("image/jpeg")
                    .setCustomMetadata("ownerId", ownerId)
                    .setCustomMetadata("ownerName", ownerName)
                    .setCustomMetadata("purpose", safePurpose)
                    .setCustomMetadata("propertyId", propertyId)
                    .build()

                val bytes: ByteArray = when {
                    photo.bitmap != null -> {
                        val stream = ByteArrayOutputStream()
                        photo.bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                        stream.toByteArray()
                    }
                    photo.uri != null -> {
                        context.contentResolver.openInputStream(photo.uri)?.use { input ->
                            input.readBytes()
                        } ?: ByteArray(0)
                    }
                    else -> ByteArray(0)
                }

                if (bytes.isNotEmpty()) {
                    storageRef.putBytes(bytes, metadata).await()
                    val url = storageRef.downloadUrl.await().toString()
                    downloadUrls.add(url)
                    Log.d(TAG, "Uploaded photo $index to $storagePath -> $url")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload photo $index to $storagePath: ${e.message}", e)
                // If cloud storage upload fails (e.g. offline/storage emulator not running),
                // fallback to local uri or bitmap representation so listing doesn't break
                if (photo.uri != null) {
                    downloadUrls.add(photo.uri.toString())
                }
            }
            onProgress?.invoke(index + 1, photos.size)
        }

        return downloadUrls
    }

    /**
     * Uploads partner profile photos / visiting cards / RERA certificates to:
     * 'partners/{partnerId}_{partnerName}/{fileName}.jpg'
     */
    suspend fun uploadPartnerDocuments(
        context: Context,
        partnerId: String,
        partnerName: String,
        photos: List<PropertyPhotoItem>
    ): List<String> {
        if (photos.isEmpty()) return emptyList()

        val safePartner = sanitizeSegment(partnerName)
        val safePartnerId = sanitizeSegment(partnerId)
        val downloadUrls = mutableListOf<String>()

        for ((index, photo) in photos.withIndex()) {
            val filename = "doc_${index + 1}_${System.currentTimeMillis()}.jpg"
            val storagePath = "partners/${safePartnerId}_${safePartner}/$filename"
            val storageRef = storage.reference.child(storagePath)

            try {
                val metadata = StorageMetadata.Builder()
                    .setContentType("image/jpeg")
                    .setCustomMetadata("partnerId", partnerId)
                    .setCustomMetadata("partnerName", partnerName)
                    .build()

                val bytes: ByteArray = when {
                    photo.bitmap != null -> {
                        val stream = ByteArrayOutputStream()
                        photo.bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                        stream.toByteArray()
                    }
                    photo.uri != null -> {
                        context.contentResolver.openInputStream(photo.uri)?.use { it.readBytes() } ?: ByteArray(0)
                    }
                    else -> ByteArray(0)
                }

                if (bytes.isNotEmpty()) {
                    storageRef.putBytes(bytes, metadata).await()
                    val url = storageRef.downloadUrl.await().toString()
                    downloadUrls.add(url)
                    Log.d(TAG, "Uploaded partner doc $index to $storagePath -> $url")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload partner doc to $storagePath: ${e.message}", e)
                if (photo.uri != null) {
                    downloadUrls.add(photo.uri.toString())
                }
            }
        }
        return downloadUrls
    }
}
