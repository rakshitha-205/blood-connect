const byId = (id) => document.getElementById(id);
const profileMessage = byId("profileMessage");

const REQUESTER_ACTIONS = [
  { label: "New emergency request", href: "/emergency-request", primary: true },
  { label: "Track my request", href: TRACKING_TAB, needsRequest: true },
  { label: "View matching donors", href: MATCHES_TAB, needsRequest: true },
  { label: "Find nearby donors", href: "/nearby-donors" },
  { label: "Blood bank stock", href: BLOOD_BANKS_PAGE },
];

const QUICK_ACTIONS = {
  HOSPITAL: REQUESTER_ACTIONS,
  ADMIN: [{ label: "Admin dashboard", href: ADMIN_PAGE, primary: true }],
  DONOR: [
    { label: "My donations", href: DONATIONS_PAGE, primary: true },
    { label: "Requests near me", href: ALERTS_PAGE },
    { label: "Can I donate today?", href: ELIGIBILITY_PAGE },
  ],
  REQUESTER: REQUESTER_ACTIONS,
  BLOOD_BANK: [
    { label: "Update blood stock", href: BANK_DASHBOARD_PAGE, primary: true },
    { label: "View all blood banks", href: BLOOD_BANKS_PAGE },
  ],
};

const RATED_ROLES = ["DONOR", "REQUESTER", "HOSPITAL"];

const renderReview = (review) => {
  const item = document.createElement("li");
  item.className = "review-item";
  const head = document.createElement("div");
  head.className = "review-item-head";
  const who = document.createElement("strong");
  who.textContent = review.reviewerName || "BloodConnect member";
  const when = document.createElement("span");
  when.textContent = new Date(review.createdAt).toLocaleDateString();
  head.append(who, buildStarRow(review.rating), when);
  item.append(head);
  if (review.comment) {
    const comment = document.createElement("p");
    comment.textContent = review.comment;
    item.append(comment);
  }
  return item;
};

const loadReviews = async (role) => {
  if (!RATED_ROLES.includes(role)) return;
  try {
    const reviews = await bloodConnectApi("/api/reviews/me");
    byId("ratingSummary").replaceChildren(buildRatingBadge(reviews.average, reviews.count, "No ratings yet"));
    byId("reviewList").replaceChildren(...reviews.reviews.map(renderReview));
    byId("reviewEmpty").hidden = reviews.reviews.length > 0;
    byId("ratingSection").hidden = false;
  } catch (error) {
    byId("ratingSection").hidden = true;
  }
};

const clearSession = () => {
  localStorage.removeItem("accessToken");
  localStorage.removeItem("currentUser");
};

const showSignedOut = () => {
  byId("profileContent").hidden = true;
  byId("signedOut").hidden = false;
  const accountMenu = document.querySelector(".navbar .account-menu");
  if (accountMenu) {
    const loginLink = document.createElement("a");
    loginLink.className = "login-btn";
    loginLink.href = "/login";
    loginLink.textContent = "Login";
    accountMenu.replaceWith(loginLink);
  }
};

const initialsFor = (text) =>
  text
    .split(/[\s@._-]+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0].toUpperCase())
    .join("");

const showIdentity = (user, displayName) => {
  const name = displayName || user.email.split("@")[0];
  byId("profileName").textContent = name;
  byId("profileAvatar").textContent = initialsFor(name);
  byId("profileEmail").textContent = user.email;
  byId("profileRole").textContent = ROLE_LABELS[user.role] || user.role;
  byId("profileSince").textContent = `Member since ${new Date(user.createdAt).toLocaleDateString(undefined, {
    year: "numeric",
    month: "long",
    day: "numeric",
  })}`;
  byId("profileWelcome").textContent = sessionStorage.getItem("profileWelcome") || "Welcome back";
  sessionStorage.removeItem("profileWelcome");
};

const showAvailability = (available) => {
  const badge = byId("donorAvailability");
  badge.textContent = available ? "Available" : "Unavailable";
  badge.classList.toggle("is-off", !available);
};

const fillDonorForm = (profile) => {
  byId("donorName").value = profile.name;
  byId("donorPhone").value = profile.phone;
  byId("donorBloodGroup").value = profile.bloodGroup;
  byId("donorLocation").value = profile.location;
  byId("donorAvailable").value = String(profile.available);
  byId("donorLatitude").value = profile.latitude ?? "";
  byId("donorLongitude").value = profile.longitude ?? "";
  if (profile.latitude != null && profile.longitude != null) markCoordinatesFor(byId("donorLocation"));
  showAvailability(profile.available);
  showEligibility(profile.deferredUntil);
};

const showEligibility = (deferredUntil) => {
  const box = byId("donorEligibility");
  const link = document.createElement("a");
  link.className = "elig-check-link";
  link.href = ELIGIBILITY_PAGE;
  if (deferredUntil) {
    const badge = document.createElement("span");
    badge.className = "elig-wait-badge";
    badge.textContent = `⏳ You can donate again from ${formatCalendarDate(deferredUntil)}`;
    link.textContent = "Check again";
    box.replaceChildren(badge, link);
    return;
  }
  link.textContent = "🩺 Can I donate today? Check eligibility";
  box.replaceChildren(link);
};

const loadDonor = async (user) => {
  byId("donorSection").hidden = false;
  try {
    const profile = await bloodConnectApi("/api/donors/me");
    fillDonorForm(profile);
    showIdentity(user, profile.name);
  } catch (error) {
    showIdentity(user);
    profileMessage.textContent = "Add your donor details below so requesters can find you.";
  }
};

const saveDonor = async (event) => {
  event.preventDefault();
  try {
    profileMessage.textContent = "Checking your exact location...";
    await ensureExactLocation("donorLocation", "donorLatitude", "donorLongitude");
    const latitude = byId("donorLatitude").value;
    const longitude = byId("donorLongitude").value;
    const profile = await bloodConnectApi("/api/donors/me", {
      method: "PUT",
      body: JSON.stringify({
        name: byId("donorName").value.trim(),
        phone: byId("donorPhone").value.trim(),
        bloodGroup: byId("donorBloodGroup").value,
        location: byId("donorLocation").value.trim(),
        latitude: latitude ? Number(latitude) : null,
        longitude: longitude ? Number(longitude) : null,
        available: byId("donorAvailable").value === "true",
      }),
    });
    fillDonorForm(profile);
    byId("profileName").textContent = profile.name;
    byId("profileAvatar").textContent = initialsFor(profile.name);
    profileMessage.textContent = "Your donor details were saved.";
  } catch (error) {
    profileMessage.textContent = error.message;
  }
};

const loadRequester = async (user) => {
  showIdentity(user, user.fullName || localStorage.getItem(`displayName:${user.email}`));
  byId("requesterSection").hidden = false;
  const requestId = localStorage.getItem("currentRequestId");
  if (!requestId) {
    byId("requestEmpty").hidden = false;
    return;
  }
  try {
    const request = await bloodConnectApi(`/api/requests/${encodeURIComponent(requestId)}`);
    byId("requestPatient").textContent = request.patientName;
    byId("requestBloodGroup").textContent = request.bloodGroup;
    byId("requestUnits").textContent = String(request.units);
    byId("requestUrgency").textContent = request.urgency;
    byId("requestHospital").textContent = request.hospital;
    byId("requestCreated").textContent = new Date(request.createdAt).toLocaleString();
    const status = byId("requestStatus");
    status.textContent = request.status;
    status.classList.toggle("is-off", request.status !== "OPEN");
    byId("requestDetails").hidden = false;
  } catch (error) {
    localStorage.removeItem("currentRequestId");
    byId("requestEmpty").hidden = false;
  }
};

const renderQuickActions = (role) => {
  const hasRequest = Boolean(localStorage.getItem("currentRequestId"));
  const container = byId("quickActions");
  (QUICK_ACTIONS[role] || [])
    .filter((action) => !action.needsRequest || hasRequest)
    .forEach((action) => {
      const link = document.createElement("a");
      link.className = action.primary ? "profile-btn primary" : "profile-btn";
      link.href = action.href;
      link.textContent = action.label;
      container.append(link);
    });
};

const loadProfile = async () => {
  if (!bloodConnectToken()) {
    showSignedOut();
    return;
  }
  try {
    const user = await bloodConnectApi("/api/auth/me");
    byId("profileContent").hidden = false;
    if (user.role === "DONOR") {
      await loadDonor(user);
    } else if (canRequestBlood(user.role)) {
      await loadRequester(user);
    } else {
      showIdentity(user, user.fullName);
    }
    renderQuickActions(user.role);
    loadReviews(user.role);
  } catch (error) {
    clearSession();
    showSignedOut();
  }
};

byId("donorForm").addEventListener("submit", saveDonor);
byId("useProfileLocation").addEventListener("click", () =>
  captureBloodConnectLocation("donorLatitude", "donorLongitude", "profileLocationMessage", "donorLocation"));

loadProfile();
