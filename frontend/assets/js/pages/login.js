const LOGIN_ROLES = {
  donor: "DONOR",
  patient: "REQUESTER",
  hospital: "HOSPITAL",
  bloodbank: "BLOOD_BANK",
  admin: "ADMIN",
};

document.getElementById("loginForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const message = document.getElementById("formMessage");
  try {
    const auth = await bloodConnectApi("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({
        email: document.getElementById("email").value.trim(),
        password: document.getElementById("password").value,
      }),
    });
    const expectedRole = LOGIN_ROLES[document.getElementById("userType").value];
    if (auth.user.role !== expectedRole) {
      const label = (ROLE_LABELS[auth.user.role] || auth.user.role).toLowerCase();
      message.textContent = `This account is registered as ${label}. Select the matching account type.`;
      return;
    }
    saveBloodConnectSession(auth);
    window.location.href = needsVerification(auth.user) ? VERIFY_PAGE : pageAfterLogin(auth.user.role);
  } catch (error) {
    message.textContent = error.message;
  }
});

const LOGIN_NOTICES = {
  verified: "Your email is verified. Log in to continue.",
  "verified-profile": "Your email is verified. Log in, then add your donor details on your profile.",
  "password-reset": "Your password has been changed. Log in with your new password.",
  "session-expired": "Your session has expired. Log in again to continue where you left off.",
};
const loginParams = new URLSearchParams(window.location.search);

const pageAfterLogin = (role) => {
  const next = loginParams.get("next") || "";
  const nextPage = next.split("?")[0];
  const safe = next.startsWith("/") && !next.startsWith("//") && !next.startsWith("/\\");
  if (!safe || isAuthPage(nextPage)) return HOME_PAGE;
  const allowed = ROLE_PAGES[role];
  return !allowed || allowed.includes(nextPage) ? next : ROLE_HOME[role];
};
const notice = LOGIN_NOTICES[loginParams.get("notice")];
if (notice) {
  const loginNotice = document.getElementById("loginNotice");
  loginNotice.textContent = notice;
  loginNotice.hidden = false;
}
if (loginParams.get("email")) {
  document.getElementById("email").value = loginParams.get("email");
  document.getElementById("password").focus();
}

document.getElementById("forgotPasswordLink").addEventListener("click", (event) => {
  const email = document.getElementById("email").value.trim();
  if (!email) return;
  event.preventDefault();
  window.location.href = `${FORGOT_PASSWORD_PAGE}?email=${encodeURIComponent(email)}`;
});
