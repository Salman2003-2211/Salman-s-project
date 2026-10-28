# Bug fixes

## Critical
- Login OTP never arrived on Android 13+: POST_NOTIFICATIONS was declared but never requested at runtime. Now requested before sending the OTP.
- Firestore writes always failed for OTP/demo users (no Firebase session, fake "guest_user_..." uid rejected by rules). An anonymous Firebase session is now created on demand.
- App wrote to the "(default)" Firestore database instead of the configured named database. ViewModel now uses FirestoreRepository(context).
- Editing a listing price added a duplicate listing (postProperty prepended a copy with the same id). Added updateProperty (in place) and keyed the list.
- Auth listener overwrote the user's edited profile on every app start. It now only logs in when not already logged in and ignores anonymous sessions.

## Functional
- Budget filter was stored but never applied (no UI, no logic). Added budget chips + filtering.
- "4+ BHK" filter excluded 4 BHK homes; "Villa" filter excluded "Independent Villa". Fixed.
- "Most Recent" sort compared strings ("1 week ago" < "2 days ago"). Now sorts by parsed age.
- "Posted By" and Amenities filters were local-only and did nothing (amenities were also pre-checked). Now wired to the ViewModel and applied.
- Voice search was a stub that always typed "Rohini Sector 24". Now uses the system speech recognizer.
- My Ads showed every owner listing in the sample data to every user. Now filtered to the signed-in user.
- Post Property: locality/city check used && (either field passed); price silently defaulted to 24,000 / 75 Lakh; apartments got a fake 150 gaj area; camera photos were dropped; wizard stayed on step 7 after posting (easy double post). All fixed.
- Edited price was shown as "₹22000.0"; detail inquiry text showed "₹₹". Fixed with formatPriceDisplay.
- Logout kept the previous user's name/phone in memory and reset the language to English. Fixed.
- Login: Math.random OTP -> SecureRandom, OTP expiry (10 min as promised), mobile must start 6-9, optional email validated.
- saveUserProfile reset createdAt on every save, which firestore.rules reject. Preserved now.
- Lead text fields are truncated to the 500/100/20 char limits the rules enforce.

## Firestore rules
- Operator (admin) could never update another user's lead because isValid*(incoming()) required userId == admin uid. Admin may now change only `status`.
- Admin check now also requires email_verified == true.

## Not changed (needs your decision)
- OTP login is local only (code generated on-device and shown as a notification), so it does not actually verify phone ownership. Use PhoneAuthHelper (already in the project) with Firebase Phone Auth for real verification.
- Listings, meetings and loan applications are held in memory (SampleData) and lost on restart; Firestore helpers for them exist but are unused.
- default_web_client_id in strings.xml is a placeholder, so Google sign-in will not work until replaced.
- Anonymous sign-in must be enabled in the Firebase console.
