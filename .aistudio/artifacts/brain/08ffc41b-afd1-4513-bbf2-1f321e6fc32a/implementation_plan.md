# Implementation Plan: Fix APK Issues & Complete Feature Set

Address the four key problems reported in the APK along with fixing build errors:
1. **Firebase Phone Auth SMS OTP Flow**: Real phone verification with SMS code, verification ID tracking, resend timer, and test fallback.
2. **Camera & Gallery Photo Upload**: Enable device photo gallery selection via `ActivityResultContracts.PickMultipleVisualMedia` and camera photo capture via `ActivityResultContracts.TakePicturePreview`.
3. **Cap Loan Limit to ₹1 Crore**: Restrict all loan sliders, presets, and input fields to a maximum of ₹1 Crore (₹1,00,00,000) with dynamic EMI calculation.
4. **Fresh Location & GPS Auto-Detection**: Remove pre-saved hardcoded locations so listings start fresh, and provide a one-tap "Detect My Location" button with Android location permissions and geocoding.
5. **Compilation Fixes**: Resolve `default_web_client_id` unresolved reference so `:app:compileDebugKotlin` and APK builds pass cleanly.

---

## User Review Required

> [!IMPORTANT]
> **Firebase Phone Auth Requirement**: Firebase Phone Authentication requires the project's SHA-1/SHA-256 fingerprints and Phone Provider enabled in the Firebase Console. We will implement standard Firebase Phone Auth (`PhoneAuthProvider.verifyPhoneNumber`) with automatic verification handling and an instant on-screen test code helper for development testing so the APK works reliably under all network conditions.

---

## Proposed Changes

### 1. Build & Authentication Fixes (`GoogleAuthHelper.kt`, `strings.xml`)
- In `strings.xml`, declare `default_web_client_id` placeholder to ensure the R class symbol resolves even if Google Services plugin did not generate an OAuth web client ID.
- In `GoogleAuthHelper.kt`, retrieve the web client ID gracefully using `resources.getIdentifier` and fallback checking to prevent crashes.

### 2. Real Phone OTP Verification Flow (`PhoneAuthHelper.kt`, `LoginScreen.kt`)
- Create `PhoneAuthHelper.kt` utilizing `FirebaseAuth.getInstance()` and `PhoneAuthProvider`:
  - `verifyPhoneNumber`: Initiates SMS OTP via `OnVerificationStateChangedCallbacks`.
  - Manages `verificationId` and `resendToken`.
  - Verifies entered OTP code with `PhoneAuthProvider.getCredential` and signs in.
- Update `LoginScreen.kt`:
  - Format phone number with country prefix (+91) and validation (10 digits).
  - 6-digit OTP entry field with automatic focus, 30-second resend countdown, and instant verification feedback.
  - Test helper banner allowing 1-tap test OTP entry so testing is never blocked if SMS delivery is delayed.

### 3. Gallery & Camera Photo Upload (`PostPropertyScreen.kt`)
- In `PostPropertyScreen.kt`, implement interactive photo selection dialog:
  - **Gallery**: Uses Android Photo Picker (`ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)`) allowing users to pick up to 5 photos from device storage without needing storage permissions (Play Store policy compliant).
  - **Camera**: Uses `ActivityResultContracts.TakePicturePreview()` to capture live photos directly from the camera.
- Store captured photo Uris / Bitmaps in the listing state.
- Render thumbnail carousel with image preview and delete ("X") badge for each photo.

### 4. Cap Loan Amounts to ₹1 Crore (`LoanScreen.kt`, `LoanLead.kt`)
- Update `LoanScreen.kt`:
  - Set slider maximum value strictly to ₹1,00,00,000 (₹1 Crore) with minimum of ₹5,00,000 (₹5 Lakh).
  - Quick amount filter chips: `₹10 Lakh`, `₹25 Lakh`, `₹50 Lakh`, `₹75 Lakh`, `₹1 Crore (Max)`.
  - Cap EMI calculator principal input at ₹1 Crore.
  - Update `LoanLead.kt` validation to ensure submitted leads cannot exceed ₹1,00,00,000.

### 5. Fresh Location & GPS Auto-Detection (`PostPropertyScreen.kt`, `AndroidManifest.xml`)
- In `AndroidManifest.xml`, declare `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION` permissions.
- In `PostPropertyScreen.kt`:
  - Clear hardcoded default text (`Sector 62, Noida`); start with empty locality fields.
  - Add "Use Current Location" button with location permission launcher (`RequestMultiplePermissions`).
  - Retrieve current latitude and longitude using Android `LocationManager` / `FusedLocationProviderClient` and resolve locality name via Android `Geocoder`.

---

## Verification Plan

### Automated Compilation Check
- Run `compile_applet` to verify that all Kotlin compiler errors (`default_web_client_id`) are eliminated and `:app:compileDebugKotlin` succeeds.

### Manual Verification
1. **Login & OTP**: Test login with phone number; verify OTP screen appears with countdown timer and verification flow.
2. **Post Property & Photos**: Open Post Property, click photo upload button, verify bottom sheet opens with Gallery / Camera options, and selected images display as thumbnails.
3. **Location**: Verify Post Property starts with empty location and clicking "Detect Location" requests permission and populates coordinates/locality.
4. **Loan Screen**: Verify slider stops at ₹1 Crore and cannot be dragged higher.
