const BLOODCONNECT_API_URL = window.BLOODCONNECT_API_URL || "http://localhost:8080";

const HOME_PAGE = "/";
const LOGIN_PAGE = "/login";
const REGISTER_PAGE = "/register";
const VERIFY_PAGE = "/verify";
const FORGOT_PASSWORD_PAGE = "/forgot-password";
const PROFILE_PAGE = "/profile";
const ADMIN_PAGE = "/admin";
const ALERTS_PAGE = "/notifications";
const BLOOD_BANKS_PAGE = "/blood-banks";
const BANK_DASHBOARD_PAGE = "/blood-bank-dashboard";
const ELIGIBILITY_PAGE = "/eligibility";
const ABOUT_PAGE = "/about";
const NEARBY_DONORS_PAGE = "/nearby-donors";
const EMERGENCY_PAGE = "/emergency-request";
const MATCHES_TAB = `${EMERGENCY_PAGE}?tab=matches`;
const TRACKING_TAB = `${EMERGENCY_PAGE}?tab=track`;
const DONATIONS_PAGE = "/donations";

const SHARED_PAGES = [HOME_PAGE, ABOUT_PAGE, BLOOD_BANKS_PAGE, PROFILE_PAGE];
const REQUESTER_PAGES = [...SHARED_PAGES, NEARBY_DONORS_PAGE, EMERGENCY_PAGE, ALERTS_PAGE];
const ROLE_PAGES = {
  REQUESTER: REQUESTER_PAGES,
  HOSPITAL: REQUESTER_PAGES,
  DONOR: [...SHARED_PAGES, ELIGIBILITY_PAGE, DONATIONS_PAGE, ALERTS_PAGE],
  BLOOD_BANK: [...SHARED_PAGES, ELIGIBILITY_PAGE, BANK_DASHBOARD_PAGE],
  ADMIN: [...SHARED_PAGES, ELIGIBILITY_PAGE, ADMIN_PAGE],
};
const ROLE_HOME = {
  REQUESTER: EMERGENCY_PAGE,
  HOSPITAL: EMERGENCY_PAGE,
  DONOR: DONATIONS_PAGE,
  BLOOD_BANK: BANK_DASHBOARD_PAGE,
  ADMIN: ADMIN_PAGE,
};

const ROLE_LABELS = {
  DONOR: "Blood donor",
  REQUESTER: "Patient",
  HOSPITAL: "Hospital",
  ADMIN: "Administrator",
  BLOOD_BANK: "Blood bank",
};
const canRequestBlood = (role) => role === "REQUESTER" || role === "HOSPITAL";
const isBloodBank = (role) => role === "BLOOD_BANK";

const BLOOD_GROUPS = ["A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"];

const PHONE_OTP_REQUIRED = false;
