import { onCall, HttpsError } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import * as functions from "firebase-functions/v1";
import { initializeApp, getApps } from "firebase-admin/app";
import { getFirestore, FieldValue } from "firebase-admin/firestore";
import { GoogleGenAI } from "@google/genai";

if (!getApps().length) {
  initializeApp();
}

/**
 * Helper to get Firestore instance supporting both named database and default fallback.
 */
function getFirestoreDb() {
  const customDbId =
    process.env.FIRESTORE_DATABASE_ID ||
    "ai-studio-android-spare-08ffc41b-afd1-4513-bbf2-1f321e6fc32a";
  try {
    if (customDbId && customDbId !== "(default)") {
      return getFirestore(customDbId);
    }
  } catch (e) {
    console.warn("Could not connect to custom Firestore database ID, falling back to default:", e);
  }
  return getFirestore();
}

// Securely access Gemini API key defined in Firebase Secrets or environment
const geminiApiKey = defineSecret("GEMINI_API_KEY");

/**
 * Helper to get an initialized GoogleGenAI instance.
 * Reads secret at invocation time to comply with Firebase Functions v2 best practices.
 */
function getAiClient(): GoogleGenAI {
  const apiKey = geminiApiKey.value() || process.env.GEMINI_API_KEY;
  if (!apiKey) {
    throw new HttpsError(
      "failed-precondition",
      "GEMINI_API_KEY secret is not configured in the Cloud Function environment."
    );
  }
  return new GoogleGenAI({ apiKey });
}

/**
 * 1. generatePropertyDescription
 * Generates compelling listing descriptions based on user-provided property attributes.
 */
export const generatePropertyDescription = onCall(
  {
    secrets: [geminiApiKey],
    cors: true,
    maxInstances: 10,
  },
  async (request) => {
    // 1. Enforce Authentication
    if (!request.auth) {
      throw new HttpsError(
        "unauthenticated",
        "Authentication required: only registered users can generate property descriptions."
      );
    }

    const {
      title,
      listingType,
      propertyType,
      bhk,
      locality,
      city,
      priceDisplay,
      furnishing,
      amenities,
      language = "en",
    } = request.data || {};

    if (!title || typeof title !== "string") {
      throw new HttpsError("invalid-argument", "Missing or invalid 'title' parameter.");
    }

    const ai = getAiClient();
    const promptLanguage = language === "hi" ? "Hindi (हिंदी)" : "English";

    const prompt = `You are an expert real estate copywriter. Write an attractive, professional property listing description in ${promptLanguage} for the following property:
- Title: ${title.slice(0, 100)}
- Listing Type: ${listingType || "Sale/Rent"}
- Property Category: ${propertyType || "Residential"}
- BHK Configuration: ${bhk || "N/A"}
- Locality & City: ${locality || ""}, ${city || ""}
- Price: ${priceDisplay || "Negotiable"}
- Furnishing: ${furnishing || "N/A"}
- Amenities: ${Array.isArray(amenities) ? amenities.slice(0, 10).join(", ") : "Standard"}

Guidelines:
- Keep the description between 80 to 180 words.
- Highlight key benefits, prime location convenience, and lifestyle appeal.
- Do not include fabricated contact details or fake URLs.
- Return ONLY the listing description text.`;

    try {
      const response = await ai.models.generateContent({
        model: "gemini-2.5-flash",
        contents: prompt,
      });

      return {
        success: true,
        description: response.text?.trim() || "",
      };
    } catch (err: any) {
      console.error("Gemini API call failed:", err);
      throw new HttpsError(
        "internal",
        "Failed to generate property description with AI Studio: " + (err.message || "Unknown error")
      );
    }
  }
);

/**
 * 2. askRealEstateAdvisor
 * Secure AI Advisor to answer user queries on home loans, property verification, and localities.
 */
export const askRealEstateAdvisor = onCall(
  {
    secrets: [geminiApiKey],
    cors: true,
    maxInstances: 10,
  },
  async (request) => {
    // 1. Enforce Authentication
    if (!request.auth) {
      throw new HttpsError(
        "unauthenticated",
        "Authentication required to consult the AI Real Estate Advisor."
      );
    }

    const { query, conversationHistory = [] } = request.data || {};

    if (!query || typeof query !== "string" || query.trim().length === 0) {
      throw new HttpsError("invalid-argument", "Query text must be a non-empty string.");
    }

    if (query.length > 500) {
      throw new HttpsError("invalid-argument", "Query exceeds maximum allowed limit of 500 characters.");
    }

    const ai = getAiClient();

    const systemInstruction = `You are 'S.P.A.RE AI Advisor', a knowledgeable and helpful Indian real estate and property loan consultant.
You provide clear, accurate guidance regarding:
1. Property buying, selling, and renting guidelines in India.
2. Home loan and Loan Against Property (LAP) processes, documentation, and standard EMI estimation.
3. Property verification, RERA compliance checks, and registry precautions.
Keep your answers concise, practical, and respectful. Never provide legally binding legal/financial guarantees. Always encourage consulting a verified legal advisor or bank representative for final paperwork.`;

    // Format chat messages
    const formattedContents: any[] = [];
    if (Array.isArray(conversationHistory)) {
      for (const msg of conversationHistory.slice(-6)) {
        if (msg.role && msg.text) {
          formattedContents.push({
            role: msg.role === "assistant" ? "model" : "user",
            parts: [{ text: String(msg.text).slice(0, 500) }],
          });
        }
      }
    }
    formattedContents.push({
      role: "user",
      parts: [{ text: query.trim() }],
    });

    try {
      const response = await ai.models.generateContent({
        model: "gemini-2.5-flash",
        contents: formattedContents,
        config: {
          systemInstruction,
          temperature: 0.7,
        },
      });

      return {
        success: true,
        answer: response.text?.trim() || "",
      };
    } catch (err: any) {
      console.error("Gemini AI Advisor failed:", err);
      throw new HttpsError(
        "internal",
        "AI Advisor service temporarily unavailable: " + (err.message || "Unknown error")
      );
    }
  }
);

/**
 * 3. evaluateLoanEligibility
 * Analyzes loan applications (capped strictly at 1 Crore INR) and returns structured suggestions.
 */
export const evaluateLoanEligibility = onCall(
  {
    secrets: [geminiApiKey],
    cors: true,
    maxInstances: 5,
  },
  async (request) => {
    // 1. Enforce Authentication
    if (!request.auth) {
      throw new HttpsError(
        "unauthenticated",
        "Authentication required to evaluate loan eligibility."
      );
    }

    const {
      loanType,
      requiredAmountInLakhs,
      employmentType,
      monthlyIncome,
      city,
    } = request.data || {};

    const amount = Number(requiredAmountInLakhs);
    if (!amount || isNaN(amount) || amount <= 0 || amount > 100) {
      throw new HttpsError(
        "invalid-argument",
        "Requested loan amount must be between 1 Lakh and 100 Lakhs (Max 1 Crore)."
      );
    }

    const ai = getAiClient();

    const prompt = `Evaluate the loan request for an Indian applicant:
- Loan Type: ${loanType || "Home Loan"}
- Requested Amount: ₹${amount} Lakhs (Max cap: ₹100 Lakhs / ₹1 Crore)
- Employment Status: ${employmentType || "Salaried"}
- Monthly Income: ${monthlyIncome || "Not specified"}
- City: ${city || "Noida / Delhi NCR"}

Return a JSON object with this exact structure:
{
  "eligibilityStatus": "High" | "Medium" | "Low",
  "estimatedTenureYears": number,
  "indicativeInterestRate": string,
  "estimatedMonthlyEmi": string,
  "keyRecommendations": string[],
  "requiredDocuments": string[]
}`;

    try {
      const response = await ai.models.generateContent({
        model: "gemini-2.5-flash",
        contents: prompt,
        config: {
          responseMimeType: "application/json",
          temperature: 0.3,
        },
      });

      const parsed = JSON.parse(response.text || "{}");
      return {
        success: true,
        result: parsed,
      };
    } catch (err: any) {
      console.error("Loan eligibility evaluation failed:", err);
      throw new HttpsError(
        "internal",
        "Loan evaluation failed: " + (err.message || "Unknown error")
      );
    }
  }
);

/**
 * 4. onUserCreated (Auth Trigger)
 * Automatically creates a user profile document in the Firestore database (/users/{userId})
 * whenever a new user signs up via Firebase Authentication (Google, Phone, Email, etc.).
 */
export const onUserCreated = functions.auth.user().onCreate(async (user) => {
  const uid = user.uid;
  console.log(`[onUserCreated] Triggered for user UID: ${uid}`);

  const displayName =
    user.displayName ||
    (user.email ? user.email.split("@")[0] : null) ||
    (user.phoneNumber ? `User ${user.phoneNumber.slice(-4)}` : "New User");

  const phoneNumber = user.phoneNumber || "+910000000000";

  const userProfile = {
    userId: uid,
    fullName: displayName.slice(0, 100),
    phoneNumber: phoneNumber,
    email: user.email || "",
    language: "en",
    createdAt: FieldValue.serverTimestamp(),
    updatedAt: FieldValue.serverTimestamp(),
  };

  const db = getFirestoreDb();
  try {
    const userDocRef = db.collection("users").doc(uid);
    const docSnap = await userDocRef.get();
    if (!docSnap.exists) {
      await userDocRef.set(userProfile);
      console.log(`[onUserCreated] Profile document created in Firestore for UID: ${uid}`);
    } else {
      console.log(`[onUserCreated] Profile document already exists for UID: ${uid}`);
    }
  } catch (primaryErr: any) {
    console.error(`[onUserCreated] Failed to write to primary Firestore instance:`, primaryErr);
    // Fallback to default database instance
    try {
      const defaultDb = getFirestore();
      const fallbackRef = defaultDb.collection("users").doc(uid);
      const snap = await fallbackRef.get();
      if (!snap.exists) {
        await fallbackRef.set(userProfile);
        console.log(`[onUserCreated] Created profile document in fallback (default) database for UID: ${uid}`);
      }
    } catch (fallbackErr) {
      console.error(`[onUserCreated] Fallback creation failed:`, fallbackErr);
      throw fallbackErr;
    }
  }
});
