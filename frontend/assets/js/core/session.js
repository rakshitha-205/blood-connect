const bloodConnectToken = () => localStorage.getItem("accessToken");

const readCurrentUser = () => {
  try {
    return JSON.parse(localStorage.getItem("currentUser")) || {};
  } catch {
    return {};
  }
};

const SESSION_EXPIRES_KEY = "sessionExpiresAt";

const saveBloodConnectSession = (authResponse) => {
  if (readCurrentUser().id !== authResponse.user.id) localStorage.removeItem("currentRequestId");
  localStorage.setItem("accessToken", authResponse.accessToken);
  localStorage.setItem("currentUser", JSON.stringify(authResponse.user));
  localStorage.setItem(SESSION_EXPIRES_KEY, String(Date.now() + Number(authResponse.expiresInSeconds) * 1000));
};

const clearBloodConnectSession = () => {
  localStorage.removeItem("accessToken");
  localStorage.removeItem("currentUser");
  localStorage.removeItem(SESSION_EXPIRES_KEY);
};

const logoutBloodConnect = () => {
  clearBloodConnectSession();
  window.location.href = LOGIN_PAGE;
};

const isAuthPage = (page) => [LOGIN_PAGE, REGISTER_PAGE, FORGOT_PASSWORD_PAGE, VERIFY_PAGE].includes(page);

const sessionExpiredLogin = () => {
  clearBloodConnectSession();
  const page = window.location.pathname.replace(/\/+$/, "") || HOME_PAGE;
  if (isAuthPage(page)) {
    window.location.href = LOGIN_PAGE;
    return;
  }
  const next = encodeURIComponent(`${page}${window.location.search}`);
  window.location.href = `${LOGIN_PAGE}?notice=session-expired&next=${next}`;
};

const sessionMillisLeft = () => Number(localStorage.getItem(SESSION_EXPIRES_KEY)) - Date.now();

if (bloodConnectToken() && !(sessionMillisLeft() > 0)) clearBloodConnectSession();
if (bloodConnectToken()) {
  setTimeout(() => {
    if (bloodConnectToken() && !(sessionMillisLeft() > 0)) sessionExpiredLogin();
  }, Math.min(sessionMillisLeft() + 1000, 2147483647));
}

const isAdmin = () => Boolean(bloodConnectToken()) && readCurrentUser().role === "ADMIN";

const currentPage = () => window.location.pathname.replace(/\/+$/, "") || HOME_PAGE;

const needsVerification = (user) => !user.emailVerified || (PHONE_OTP_REQUIRED && !user.phoneVerified);

const goToVerification = () => {
  if (currentPage() !== VERIFY_PAGE) {
    window.location.href = VERIFY_PAGE;
  }
};

if (bloodConnectToken() && needsVerification(readCurrentUser())) goToVerification();

const guardRolePage = () => {
  if (!bloodConnectToken() || needsVerification(readCurrentUser()) || isAuthPage(currentPage())) return;
  const role = readCurrentUser().role;
  const allowed = ROLE_PAGES[role];
  if (allowed && !allowed.includes(currentPage())) window.location.replace(ROLE_HOME[role]);
};

guardRolePage();

const signedInAtLoad = Boolean(bloodConnectToken());
const reloadIfSessionChanged = () => {
  if (Boolean(bloodConnectToken()) !== signedInAtLoad) window.location.reload();
};
window.addEventListener("pageshow", (event) => {
  if (event.persisted) reloadIfSessionChanged();
});
window.addEventListener("storage", (event) => {
  if (event.key === "accessToken" || event.key === null) reloadIfSessionChanged();
});
document.addEventListener("visibilitychange", () => {
  if (document.visibilityState === "visible") reloadIfSessionChanged();
});
