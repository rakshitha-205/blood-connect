const userType = document.getElementById("userType");
const donorFields = document.getElementById("donorFields");
const donorInputs = ["bloodGroup", "location", "availability"];

userType.addEventListener("change", () => {
  const isDonor = userType.value === "donor";
  donorFields.style.display = isDonor ? "block" : "none";
  donorInputs.forEach((id) => { document.getElementById(id).required = isDonor; });
});

document.getElementById("useDonorLocation").addEventListener("click", () =>
  captureBloodConnectLocation("latitude", "longitude", "donorLocationMessage"));

document.getElementById("registerForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const message = document.getElementById("formMessage");
  const value = (id) => document.getElementById(id).value.trim();
  const password = value("password");
  if (password !== value("confirmPassword")) {
    message.textContent = "Passwords do not match.";
    return;
  }
  const isDonor = userType.value === "donor";
  try {
    if (isDonor) {
      message.textContent = "Checking your exact location...";
      await ensureExactLocation("location", "latitude", "longitude");
      message.textContent = "";
    }
    const latitude = value("latitude");
    const longitude = value("longitude");
    const auth = await bloodConnectApi("/api/auth/register", {
      method: "POST",
      body: JSON.stringify({
        name: value("name"), email: value("email"), phone: value("phone"), password,
        role: { donor: "DONOR", hospital: "HOSPITAL", bloodbank: "BLOOD_BANK" }[userType.value] || "REQUESTER",
      }),
    });
    saveBloodConnectSession(auth);
    localStorage.setItem(`displayName:${auth.user.email}`, value("name"));
    if (isDonor) {
      sessionStorage.setItem("pendingDonorProfile", JSON.stringify({
        name: value("name"), phone: value("phone"), bloodGroup: value("bloodGroup"),
        location: value("location"), latitude: latitude ? Number(latitude) : null,
        longitude: longitude ? Number(longitude) : null,
        available: value("availability") === "available",
      }));
    }
    window.location.href = VERIFY_PAGE;
  } catch (error) {
    message.textContent = error.message;
  }
});
