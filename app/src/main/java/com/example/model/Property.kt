package com.example.model

enum class ListingType(val label: String) {
    RENT("Rent"),
    LEASE("Lease"),
    SALE("Sell"),
    PLOT("Plot / Land"),
    COMMERCIAL("Commercial");

    fun toStoragePurpose(): String = when (this) {
        RENT -> "RENT"
        LEASE -> "LEASE"
        SALE -> "SELL"
        PLOT -> "SELL"
        COMMERCIAL -> "RENT"
    }

    fun toFirestoreListingType(): String = when (this) {
        RENT -> "RENT"
        LEASE -> "LEASE"
        SALE -> "SELL"
        PLOT -> "PLOT"
        COMMERCIAL -> "COMMERCIAL"
    }

    companion object {
        fun fromString(value: String): ListingType = when (value.uppercase().trim()) {
            "RENT" -> RENT
            "LEASE" -> LEASE
            "SALE", "SELL" -> SALE
            "PLOT" -> PLOT
            "COMMERCIAL" -> COMMERCIAL
            else -> RENT
        }
    }
}

enum class PropertyStatus(val label: String) {
    ACTIVE("Active / Live"),
    UNDER_REVIEW("Under Review"),
    EXPIRED("Expired"),
    SOLD("Sold / Rented")
}

data class Property(
    val id: String,
    val title: String,
    val listingType: ListingType,
    val propertyType: String = "Apartment",
    val bhk: String = "2 BHK",
    val locality: String = "",
    val city: String = "Noida",
    val distanceInfo: String = "Near Local Hub",
    val price: Double = 0.0,
    val priceDisplay: String = "",
    val areaGaj: Int = 0,
    val carpetAreaSqFt: Int = 1100,
    val furnishing: String = "Semi-Furnished",
    val isVerified: Boolean = true,
    val isOwnerListing: Boolean = true,
    val postedBy: String = "Owner",
    val ownerId: String = "",
    val ownerName: String = "Rahul Rajpoot",
    val ownerPhone: String = "9818452198",
    val facing: String = "North-East",
    val floor: String = "3rd of 14 Floors",
    val amenities: List<String> = listOf("Gated Security", "Parking", "Lift", "Power Backup", "24/7 Water"),
    val imageUrl: String = "",
    val imageUrls: List<String> = emptyList(),
    val description: String = "",
    val viewsCount: Int = 142,
    val inquiriesCount: Int = 8,
    val postedDate: String = "Just now",
    val validityDaysRemaining: Int = 30,
    val isSold: Boolean = false,
    val status: PropertyStatus = PropertyStatus.ACTIVE,
    val lat: Double = 28.6280,
    val lng: Double = 77.3649,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000L
)

/**
 * Human readable price used on cards, e.g. "₹22,000 / mo", "₹78 Lakh", "₹1.45 Crore".
 * Keeps edited prices consistent with the format used everywhere else in the app.
 */
fun formatPriceDisplay(price: Double, monthly: Boolean): String {
    val locale = java.util.Locale.US
    return when {
        monthly -> "₹${String.format(locale, "%,d", price.toLong())} / mo"
        price >= 10_000_000.0 -> {
            val crore = price / 10_000_000.0
            "₹${trimTrailingZeros(String.format(locale, "%.2f", crore))} Crore"
        }
        else -> {
            val lakh = price / 100_000.0
            "₹${trimTrailingZeros(String.format(locale, "%.2f", lakh))} Lakh"
        }
    }
}

private fun trimTrailingZeros(value: String): String =
    if (value.contains('.')) value.trimEnd('0').trimEnd('.') else value
