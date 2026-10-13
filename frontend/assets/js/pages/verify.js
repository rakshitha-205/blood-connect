const PENDING_DONOR_PROFILE = "pendingDonorProfile";
const steps = { EMAIL: document.getElementById("emailStep") };
if (PHONE_OTP_REQUIRED) {
  steps.PHONE = document.getElementById("phoneStep");
  steps.PHONE.hidden = false;
  document.getElementById("emailStepNumber").hidden = false;
  document.getElementById("verifySubtitle").textContent =
    "For everyone's safety, confirm your email and phone number once. We'll send a 6‑digit code to each.";
}
const phoneInput = document.getElementById("phoneNumber");
let user = readCurrentUser();

const isDone = (channel) => (channel === "EMAIL" ? user.emailVerified : user.phoneVerified);

const setMessage = (step, text, isError = false) => {
  const message = step.querySelector(".verify-message");
  message.textContent = text;
  message.classList.toggle("is-error", isError);
};

const renderStep = (channel) => {
  const step = steps[channel];
  const done = isDone(channel);
  step.classList.toggle("is-done", done);
  step.querySelector(".verify-badge").hidden = !done;
  step.querySelector(".verify-body").hidden = done;
};

const render = () => {
  document.getElementById("emailDestination").textContent = user.email || "";
  document.getElementById("phoneDestination").textContent = user.phone || "Add your mobile number";
  if (!phoneInput.value && user.phone) phoneInput.value = user.phone;
  Object.keys(steps).forEach(renderStep);
};

const startCooldown = (button, seconds) => {
  let left = seconds;
  button.disabled = true;
  const tick = () => {
    if (left <= 0) {
      button.disabled = false;
      button.textContent = "Resend code";
      return;
    }
    button.textContent = `Resend in ${left}s`;
    left -= 1;
    setTimeout(tick, 1000);
  };
  tick();
};

const goToLogin = (delay, notice = "verified") => {
  setTimeout(() => {
    localStorage.removeItem("accessToken");
    localStorage.removeItem("currentUser");
    window.location.href = `${LOGIN_PAGE}?notice=${notice}`;
  }, delay);
};

const markProgressVerified = () => {
  const current = document.querySelector(".verify-progress .is-current");
  if (!current) return;
  current.classList.replace("is-current", "is-done");
  current.removeAttribute("aria-current");
  current.querySelector(".verify-progress-dot").textContent = "✓";
  const next = current.nextElementSibling;
  next?.classList.add("is-current");
  next?.setAttribute("aria-current", "step");
};

const finish = async () => {
  const done = document.getElementById("verifyDone");
  done.hidden = false;
  markProgressVerified();
  const pending = sessionStorage.getItem(PENDING_DONOR_PROFILE);
  sessionStorage.removeItem(PENDING_DONOR_PROFILE);
  if (pending) {
    try {
      await bloodConnectApi("/api/donors/me", { method: "PUT", body: pending });
    } catch (error) {
      done.querySelector(".verify-done-text").textContent =
        `Your donor details couldn't be saved: ${error.message} You can add them on your profile after logging in.`;
      goToLogin(4000, "verified-profile");
      return;
    }
  }
  goToLogin(1600);
};

const saveUser = (updated) => {
  user = { ...user, ...updated };
  localStorage.setItem("currentUser", JSON.stringify(user));
  render();
  if (!needsVerification(user)) finish();
};

Object.entries(steps).forEach(([channel, step]) => {
  const sendButton = step.querySelector(".verify-send");
  const form = step.querySelector(".verify-form");
  const codeInput = step.querySelector(".verify-code");
  const codeBoxes = buildCodeBoxes(form, codeInput);

  sendButton.addEventListener("click", async () => {
    const body = { channel };
    if (channel === "PHONE") {
      const phone = phoneInput.value.trim();
      if (!phone) {
        setMessage(step, "Enter your mobile number first.", true);
        phoneInput.focus();
        return;
      }
      body.phone = phone;
    }
    setButtonLoading(sendButton, true, "Sending...");
    try {
      const sent = await bloodConnectApi("/api/auth/otp/send", { method: "POST", body: JSON.stringify(body) });
      setButtonLoading(sendButton, false);
      if (channel === "PHONE") {
        user.phone = phoneInput.value.trim();
        document.getElementById("phoneDestination").textContent = sent.destination;
      }
      const hint = step.querySelector(".verify-hint");
      hint.hidden = !sent.devCode;
      hint.textContent = sent.devCode
        ? `Testing mode (no ${channel === "EMAIL" ? "mail server" : "SMS service"} set up): your code is ${sent.devCode}`
        : "";
      setMessage(step, `Code sent to ${sent.destination}. It expires in ${Math.round(sent.expiresInSeconds / 60)} minutes.`);
      form.hidden = false;
      step.classList.add("is-code-sent");
      codeBoxes.reset();
      startCooldown(sendButton, sent.resendInSeconds);
    } catch (error) {
      setButtonLoading(sendButton, false);
      setMessage(step, error.message, true);
    }
  });

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    const submit = form.querySelector(".verify-submit");
    if (submit.disabled) return;
    const code = codeInput.value.trim();
    if (!/^\d{6}$/.test(code)) {
      setMessage(step, "Enter all 6 digits of the code.", true);
      form.querySelector(".otp-box:not(.is-filled)")?.focus();
      return;
    }
    setButtonLoading(submit, true, "Checking...");
    try {
      const updated = await bloodConnectApi("/api/auth/otp/verify", {
        method: "POST",
        body: JSON.stringify({ channel, code }),
      });
      setMessage(step, "");
      saveUser(updated);
    } catch (error) {
      setButtonLoading(submit, false);
      setMessage(step, error.message, true);
      codeBoxes.reset(true);
    }
  });
});

document.getElementById("verifyLogout").addEventListener("click", () => {
  sessionStorage.removeItem(PENDING_DONOR_PROFILE);
  logoutBloodConnect();
});

if (!bloodConnectToken()) {
  window.location.href = LOGIN_PAGE;
} else if (!needsVerification(user)) {
  window.location.href = HOME_PAGE;
} else {
  render();
  bloodConnectApi("/api/auth/me")
    .then((me) => {
      if (needsVerification({ ...user, ...me })) {
        saveUser(me);
        return;
      }
      localStorage.setItem("currentUser", JSON.stringify({ ...user, ...me }));
      window.location.href = HOME_PAGE;
    })
    .catch(() => {});
}
