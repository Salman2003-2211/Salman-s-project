// Unit verification for Cloud Functions logic & security gates
const { generatePropertyDescription, askRealEstateAdvisor, evaluateLoanEligibility, onUserCreated } = require("./lib/index");

async function runTests() {
  console.log("=== Testing Cloud Functions Security Gates, Auth Triggers & Logic ===");
  let passed = 0;
  let total = 0;

  // Test 1: Unauthenticated call rejection on generatePropertyDescription
  total++;
  try {
    await generatePropertyDescription.run({ data: { title: "2 BHK Flat" }, auth: null });
    console.error("FAIL: Test 1 allowed unauthenticated request!");
  } catch (err) {
    if (err.code === "unauthenticated" || err.message.includes("Authentication required")) {
      console.log("PASS: Test 1 - Unauthenticated request correctly rejected with 'unauthenticated'.");
      passed++;
    } else {
      console.error("FAIL: Test 1 unexpected error:", err);
    }
  }

  // Test 2: Unauthenticated call rejection on askRealEstateAdvisor
  total++;
  try {
    await askRealEstateAdvisor.run({ data: { query: "How to check RERA?" }, auth: null });
    console.error("FAIL: Test 2 allowed unauthenticated request!");
  } catch (err) {
    if (err.code === "unauthenticated" || err.message.includes("Authentication required")) {
      console.log("PASS: Test 2 - AI Advisor query correctly rejected without auth.");
      passed++;
    } else {
      console.error("FAIL: Test 2 unexpected error:", err);
    }
  }

  // Test 3: Input validation - loan amount over 1 Crore cap
  total++;
  try {
    await evaluateLoanEligibility.run({
      data: { loanType: "Home Loan", requiredAmountInLakhs: 250 }, // 2.5 Crore > 1 Crore cap
      auth: { uid: "test_user_123" }
    });
    console.error("FAIL: Test 3 allowed loan amount over 1 Crore!");
  } catch (err) {
    if (err.code === "invalid-argument" || err.message.includes("Max 1 Crore")) {
      console.log("PASS: Test 3 - Loan cap enforced (rejected request exceeding ₹1 Crore limit).");
      passed++;
    } else {
      console.error("FAIL: Test 3 unexpected error:", err);
    }
  }

  // Test 4: Missing property title validation
  total++;
  try {
    await generatePropertyDescription.run({
      data: { bhk: "3 BHK" }, // missing title
      auth: { uid: "test_user_123" }
    });
    console.error("FAIL: Test 4 allowed missing title!");
  } catch (err) {
    if (err.code === "invalid-argument" || err.message.includes("Missing or invalid 'title'")) {
      console.log("PASS: Test 4 - Schema validation caught missing required 'title'.");
      passed++;
    } else {
      console.error("FAIL: Test 4 unexpected error:", err);
    }
  }

  // Test 5: onUserCreated trigger exports and function signature
  total++;
  try {
    if (typeof onUserCreated === "function" || (onUserCreated && typeof onUserCreated.run === "function")) {
      console.log("PASS: Test 5 - onUserCreated Auth trigger is properly exported as a Cloud Function.");
      passed++;
    } else {
      console.error("FAIL: Test 5 - onUserCreated is not a valid Cloud Function trigger.");
    }
  } catch (err) {
    console.error("FAIL: Test 5 unexpected error:", err);
  }

  console.log(`\nResults: ${passed}/${total} security, auth trigger, and validation tests passed.`);
  process.exit(passed === total ? 0 : 1);
}

runTests();
