package com.example.model

enum class ListingType(val label: String) {
    RENT("Rent"),
    SALE("Sale"),
    PLOT("Plot / Land"),
    COMMERCIAL("Commercial")
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
    val propertyType: String,
    val bhk: String,
    val locality: String,
    val city: String = "Noida",
    val distanceInfo: String,
    val price: Double,
    val priceDisplay: String,
    val areaGaj: Int = 0,
    val carpetAreaSqFt: Int = 1100,
    val furnishing: String = "Semi-Furnished",
    val isVerified: Boolean = true,
    val isOwnerListing: Boolean = true,
    val postedBy: String = "Owner",
    val ownerName: String = "Rahul Rajpoot",
    val ownerPhone: String = "9818452198",
    val facing: String = "North-East",
    val floor: String = "3rd of 14 Floors",
    val amenities: List<String> = listOf("Gated Security", "Parking", "Lift", "Power Backup", "24/7 Water"),
    val imageUrl: String = "",
    val description: String = "",
    val viewsCount: Int = 142,
    val inquiriesCount: Int = 8,
    val postedDate: String = "3 days ago",
    val validityDaysRemaining: Int = 27,
    val status: PropertyStatus = PropertyStatus.ACTIVE,
    val lat: Double = 28.6280,
    val lng: Double = 77.3649,
    val isFavorite: Boolean = false
)
