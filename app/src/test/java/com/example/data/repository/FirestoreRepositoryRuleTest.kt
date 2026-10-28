package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.FirestoreRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Test

class FirestoreRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun saveUserProfile_authenticatedUser_succeeds() = runBlocking {
    signInTestUser("alice@test.com")
    val repo = FirestoreRepository(firestore, auth)

    val result = withTimeout(5000L) {
      repo.saveUserProfile(
        fullName = "Alice Sharma",
        phone = "+919818452198",
        email = "alice@test.com"
      )
    }
    assertTrue(result.isSuccess)
  }

  @Test
  fun createProperty_authenticatedUser_succeeds() = runBlocking {
    signInTestUser("alice@test.com")
    val repo = FirestoreRepository(firestore, auth)

    val result = withTimeout(5000L) {
      repo.createProperty(
        title = "Luxury 2 BHK Apartment",
        listingType = "RENT",
        propertyType = "Apartment",
        bhk = "2 BHK",
        locality = "Sector 62, Noida",
        price = 24000.0,
        priceDisplay = "₹24,000 / mo",
        description = "Spacious apartment with lift and parking facility.",
        ownerName = "Alice Sharma",
        ownerPhone = "+919818452198"
      )
    }
    assertTrue(result.isSuccess)
  }

  @Test
  fun createLoanLead_authenticatedUser_succeeds() = runBlocking {
    signInTestUser("alice@test.com")
    val repo = FirestoreRepository(firestore, auth)

    val result = withTimeout(5000L) {
      repo.createLoanLead(
        clientName = "Alice Sharma",
        phoneNumber = "+919818452198",
        loanTypeSelected = "HOME_LOAN",
        requiredAmountInLakhs = 45.0,
        employmentType = "Salaried",
        monthlyIncome = "₹90,000 / month",
        city = "Noida"
      )
    }
    assertTrue(result.isSuccess)
  }
}
