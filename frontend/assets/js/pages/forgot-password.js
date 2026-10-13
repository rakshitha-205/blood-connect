const emailForm = document.getElementById("emailForm");
const codeForm = document.getElementById("codeForm");
const resetForm = document.getElementById("resetForm");
const emailInput = document.getElementById("resetEmail");
const resendButton = document.getElementById("resendButton");
const resetHint = document.getElementById("resetHint");
const resetCodeBoxes = buildCodeBoxes(codeForm, document.getElementById("resetCode"));
const RESET_STEPS = {
  email: {
    form: emailForm,
    title: "Forgot your password?",
    subtitle: "Enter the email you registered with. We'll email you a 6‑digit code to verify it's you.",
  },
  code: { form: codeForm, title: "Check your email" },
  password: { form: resetForm, title: "Choose a new password", subtitle: "Pick something you haven't used here before." },
};
let resetEmail = "";
let verifiedCode = "";
let resendTimer = null;

const showResetMessage = (id, text, isError = false) => {
  const message = document.getElementById(id);
  message.textContent = text;
  message.classList.toggle("is-error", isError);
};

const setResetProgress = (current) => {
  document.querySelectorAll(".verify-progress li").forEach((step, index) => {
    const dot = step.querySelector(".verify-progress-dot");
    step.classList.toggle("is-done", index < current);
    step.classList.toggle("is-current", index === current);
    if (index === current) step.setAttribute("aria-current", "step");
    else step.removeAttribute("aria-current");
    dot.textContent = index < current ? "✓" : String(index + 1);
  });
};

const showResetStep = (name, subtitle) => {
  const step = RESET_STEPS[name];
  Object.values(RESET_STEPS).forEach(({ form }) => {
    form.hidden = form !== step.form;
  });
  setResetProgress(Object.keys(RESET_STEPS).indexOf(name));
  document.getElementById("resetTitle").textContent = step.title;
  document.getElementById("resetSubtitle").textContent = subtitle || step.subtitle;
};

const startResendCooldown = (seconds) => {
  clearInterval(resendTimer);
  let left = seconds;
  const tick = () => {
    resendButton.disabled = left > 0;
    resendButton.textContent = left > 0 ? `Resend in ${left}s` : "Resend code";
    left -= 1;
    if (left < -1) clearInterval(resendTimer);
  };
  tick();
  resendTimer = setInterval(tick, 1000);
};

const showCodeStep = (sent) => {
  showResetStep(
    "code",
    `If an account exists for ${sent.destination}, we've emailed it a 6‑digit code. ` +
      `It expires in ${Math.round(sent.expiresInSeconds / 60)} minutes.`,
  );
  resetHint.hidden = !sent.devCode;
  resetHint.textContent = sent.devCode ? `Testing mode (no mail server set up): your code is ${sent.devCode}` : "";
  showResetMessage("codeMessage", "");
  resetCodeBoxes.reset();
  startResendCooldown(sent.resendInSeconds);
};

const requestResetCode = () =>
  bloodConnectApi("/api/auth/password/forgot", {
    method: "POST",
    body: JSON.stringify({ email: resetEmail }),
  });

emailForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  const email = emailInput.value.trim();
  if (!emailInput.checkValidity() || !email) {
    showResetMessage("emailMessage", "Enter the email address you registered with.", true);
    emailInput.focus();
    return;
  }
  resetEmail = email;
  const button = document.getElementById("sendCodeButton");
  setButtonLoading(button, true, "Sending...");
  try {
    const sent = await requestResetCode();
    showResetMessage("emailMessage", "");
    showCodeStep(sent);
  } catch (error) {
    showResetMessage("emailMessage", error.message, true);
  } finally {
    setButtonLoading(button, false);
  }
});

codeForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  const button = document.getElementById("verifyCodeButton");
  if (button.disabled) return;
  const code = document.getElementById("resetCode").value;
  if (!/^\d{6}$/.test(code)) {
    showResetMessage("codeMessage", "Enter all 6 digits of the code from your email.", true);
    resetCodeBoxes.focusFirstEmpty();
    return;
  }
  setButtonLoading(button, true, "Checking...");
  try {
    await bloodConnectApi("/api/auth/password/verify", {
      method: "POST",
      body: JSON.stringify({ email: resetEmail, code }),
    });
    verifiedCode = code;
    clearInterval(resendTimer);
    document.getElementById("verifiedEmailText").textContent = `${resetEmail} is verified`;
    showResetStep("password");
    showResetMessage("resetMessage", "");
    document.getElementById("newPassword").focus();
  } catch (error) {
    showResetMessage("codeMessage", error.message, true);
    resetCodeBoxes.reset(true);
  } finally {
    setButtonLoading(button, false);
  }
});

resendButton.addEventListener("click", async () => {
  setButtonLoading(resendButton, true, "Sending...");
  try {
    const sent = await requestResetCode();
    setButtonLoading(resendButton, false);
    showCodeStep(sent);
    showResetMessage("codeMessage", "We've sent you a new code.");
  } catch (error) {
    setButtonLoading(resendButton, false);
    showResetMessage("codeMessage", error.message, true);
  }
});

document.getElementById("changeEmailButton").addEventListener("click", () => {
  clearInterval(resendTimer);
  showResetStep("email");
  emailInput.focus();
});

resetForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  const password = document.getElementById("newPassword").value;
  const confirmPassword = document.getElementById("confirmNewPassword").value;

  if (password.length < 10) {
    showResetMessage("resetMessage", "Your new password must be at least 10 characters long.", true);
    document.getElementById("newPassword").focus();
    return;
  }
  if (password !== confirmPassword) {
    showResetMessage("resetMessage", "The two passwords don't match.", true);
    document.getElementById("confirmNewPassword").focus();
    return;
  }

  const button = document.getElementById("resetButton");
  setButtonLoading(button, true, "Changing...");
  try {
    await bloodConnectApi("/api/auth/password/reset", {
      method: "POST",
      body: JSON.stringify({ email: resetEmail, code: verifiedCode, newPassword: password }),
    });
    clearBloodConnectSession();
    resetForm.hidden = true;
    document.getElementById("resetDone").hidden = false;
    setResetProgress(3);
    document.getElementById("resetTitle").textContent = "All set!";
    document.getElementById("resetSubtitle").textContent = "Log in with your new password.";
    setTimeout(() => {
      window.location.href = `${LOGIN_PAGE}?notice=password-reset&email=${encodeURIComponent(resetEmail)}`;
    }, 1800);
  } catch (error) {
    setButtonLoading(button, false);
    if (!/code/i.test(error.message)) {
      showResetMessage("resetMessage", error.message, true);
      return;
    }
    showResetStep("code", "Your code is no longer valid. Ask for a new one below.");
    showResetMessage("codeMessage", error.message, true);
    startResendCooldown(0);
    resetCodeBoxes.reset();
  }
});

const prefilledEmail = new URLSearchParams(window.location.search).get("email");
if (prefilledEmail) emailInput.value = prefilledEmail;
