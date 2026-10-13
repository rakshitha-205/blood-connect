const alertsList = document.getElementById("alertsList");
const alertsStatus = document.getElementById("alertsStatus");
const alertsBanner = document.getElementById("alertsBanner");
const alertsToolbar = document.getElementById("alertsToolbar");
const acceptedSection = document.getElementById("acceptedSection");
const acceptedList = document.getElementById("acceptedList");
const alertsListTitle = document.getElementById("alertsListTitle");
let donorAlerts = [];
let activeFilter = "all";
let donorDeferredUntil = null;

const element = (tag, className, text) => {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text != null) node.textContent = text;
  return node;
};

const titleCase = (value) =>
  String(value || "").toLowerCase().replace(/(^|\s)\S/g, (letter) => letter.toUpperCase());

const timeAgo = (isoDate) => {
  const minutes = Math.max(0, Math.round((Date.now() - new Date(isoDate).getTime()) / 60000));
  if (minutes < 1) return "just now";
  if (minutes < 60) return `${minutes} min ago`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${hours} hr${hours === 1 ? "" : "s"} ago`;
  const days = Math.round(hours / 24);
  return `${days} day${days === 1 ? "" : "s"} ago`;
};

const showEmptyState = (icon, title, text, action) => {
  const empty = element("div", "nearby-empty");
  empty.append(element("div", "nearby-empty-icon", icon), element("strong", "", title), element("p", "", text));
  if (action) {
    const link = element("a", "login-btn match-empty-action", action.label);
    link.href = action.href;
    empty.append(link);
  }
  alertsList.replaceChildren(empty);
};

const showBanner = (text) => {
  alertsBanner.textContent = text;
  alertsBanner.hidden = !text;
};

const directionsUrl = (alert) => {
  const url = new URL("https://www.google.com/maps/dir/");
  url.searchParams.set("api", "1");
  url.searchParams.set("destination", alert.latitude != null && alert.longitude != null
    ? `${alert.latitude},${alert.longitude}`
    : `${alert.hospital}, ${alert.location}`);
  return url.toString();
};

const renderAlert = (alert) => {
  const level = String(alert.urgency || "").toLowerCase();
  const card = element("article", "nearby-card alert-card");
  card.dataset.level = level;

  const top = element("div", "nearby-card-top");
  const who = element("div", "nearby-who");
  who.append(element("h3", "nearby-name", alert.hospital), element("p", "nearby-place", alert.location));
  const urgency = element("span", "match-urgency", titleCase(alert.urgency));
  urgency.dataset.level = level;
  top.append(element("span", "nearby-blood", alert.bloodGroup), who, urgency);

  const summary = element("p", "alert-summary",
    `${alert.patientName} needs ${alert.units} unit${alert.units === 1 ? "" : "s"} of ${alert.bloodGroup} blood · ${timeAgo(alert.createdAt)}`);

  const meta = element("div", "nearby-meta");
  meta.append(element("span", `nearby-pill${alert.distanceKm == null ? " is-muted" : ""}`,
    alert.distanceKm == null ? "Distance unavailable" : `${alert.distanceKm} km from you`));
  if (alert.exactMatch) meta.append(element("span", "nearby-pill is-exact", "Exact group"));
  const rating = buildRatingBadge(alert.requesterRating, alert.requesterRatingCount);
  if (rating) meta.append(rating);

  const actions = element("div", "alert-actions");
  const accept = element("button", "tracking-btn is-primary",
    donorDeferredUntil ? `Waiting until ${formatCalendarDate(donorDeferredUntil)}` : "I can donate");
  accept.type = "button";
  accept.disabled = Boolean(donorDeferredUntil);
  accept.addEventListener("click", () => acceptAlert(alert, accept));
  const eligibility = element("a", "tracking-btn", "Check eligibility");
  eligibility.href = ELIGIBILITY_PAGE;
  const directions = element("a", "tracking-btn", "Directions");
  directions.href = directionsUrl(alert);
  directions.target = "_blank";
  directions.rel = "noopener noreferrer";
  const decline = element("button", "tracking-btn is-quiet", "Can't donate");
  decline.type = "button";
  actions.append(accept, ...buildContactButtons(alert.contact, donorGreeting(alert), { callLabel: "Call patient" }),
    directions, eligibility, decline);

  card.append(top, summary);
  if (alert.message) card.append(element("p", "alert-message", `“${alert.message}”`));
  const form = buildDeclineForm(alert, actions);
  decline.addEventListener("click", () => {
    actions.hidden = true;
    form.hidden = false;
    form.querySelector("textarea").focus();
  });
  card.append(meta, actions, form);
  return card;
};

const donorGreeting = (alert) => {
  const name = readCurrentUser().fullName;
  return `Hi, I'm ${name || "a blood donor"} from BloodConnect. I can donate ${alert.bloodGroup} blood for ${alert.patientName} at ${alert.hospital}.`;
};

const acceptAlert = async (alert, button) => {
  setButtonLoading(button, true, "Accepting...");
  try {
    await bloodConnectApi(`/api/donors/me/alerts/${encodeURIComponent(alert.requestId)}/accept`, { method: "POST" });
    donorAlerts = donorAlerts.filter((item) => item.requestId !== alert.requestId);
    renderAlerts();
    showBanner(`Thank you! ${alert.patientName}'s family has been emailed. Call or WhatsApp them below to arrange the donation.`);
    await loadAcceptedRequests();
    acceptedSection.scrollIntoView({ behavior: "smooth", block: "start" });
  } catch (error) {
    setButtonLoading(button, false);
    showBanner(error.message);
  }
};

const ACCEPTED_STATUS = { OPEN: "Open", FULFILLED: "Fulfilled", CANCELLED: "Cancelled" };

const renderAcceptedRequest = (accepted) => {
  const card = element("article", "nearby-card accepted-card");
  const top = element("div", "nearby-card-top");
  const who = element("div", "nearby-who");
  who.append(element("h3", "nearby-name", accepted.hospital), element("p", "nearby-place", accepted.location));
  const status = element("span", "accepted-status", ACCEPTED_STATUS[accepted.status] || titleCase(accepted.status));
  status.dataset.status = String(accepted.status || "").toLowerCase();
  top.append(element("span", "nearby-blood", accepted.bloodGroup), who, status);

  const summary = element("p", "alert-summary",
    `You accepted ${accepted.patientName}'s request for ${accepted.units} unit${accepted.units === 1 ? "" : "s"} · ${timeAgo(accepted.acceptedAt)}`);
  card.append(top, summary);

  const rating = buildRatingBadge(accepted.requesterRating, accepted.requesterRatingCount);
  if (rating) {
    const meta = element("div", "nearby-meta");
    meta.append(rating);
    card.append(meta);
  }

  if (accepted.status === "OPEN") {
    const actions = element("div", "alert-actions");
    const directions = element("a", "tracking-btn", "Directions");
    directions.href = directionsUrl(accepted);
    directions.target = "_blank";
    directions.rel = "noopener noreferrer";
    actions.append(...buildContactButtons(accepted.contact, donorGreeting(accepted), { callLabel: "Call patient" }),
      directions);
    card.append(actions);
  } else if (accepted.status === "FULFILLED") {
    const name = accepted.requesterName || accepted.patientName;
    card.append(accepted.reviewed
      ? element("p", "review-done", `You rated ${name}. Thank you for donating!`)
      : buildReviewForm({ requestId: accepted.requestId, name }));
  }
  return card;
};

const loadAcceptedRequests = async () => {
  try {
    const accepted = await bloodConnectApi("/api/donors/me/accepted");
    acceptedSection.hidden = !accepted.length;
    alertsListTitle.hidden = !accepted.length;
    document.getElementById("acceptedCount").textContent = `${accepted.length}`;
    acceptedList.replaceChildren(...accepted.map(renderAcceptedRequest));
  } catch (error) {
    acceptedSection.hidden = true;
  }
};

const DECLINE_REASONS = [
  "I donated recently",
  "I'm unwell right now",
  "I'm too far away",
  "Not available at this time",
];

const buildDeclineForm = (alert, actions) => {
  const form = element("form", "decline-form");
  form.hidden = true;
  const label = element("label", "decline-label", `Tell ${alert.patientName}'s family why you can't donate`);
  const textId = `decline-${alert.requestId}`;
  label.htmlFor = textId;

  const chips = element("div", "decline-chips");
  const text = element("textarea", "decline-text");
  text.id = textId;
  text.rows = 2;
  text.maxLength = 300;
  text.placeholder = "Write a short reason";
  DECLINE_REASONS.forEach((reason) => {
    const chip = element("button", "decline-chip", reason);
    chip.type = "button";
    chip.addEventListener("click", () => {
      text.value = reason;
      text.focus();
    });
    chips.append(chip);
  });

  const message = element("p", "decline-message");
  message.setAttribute("role", "status");
  const buttons = element("div", "alert-actions");
  const send = element("button", "tracking-btn is-primary", "Send reason");
  send.type = "submit";
  const cancel = element("button", "tracking-btn is-quiet", "Back");
  cancel.type = "button";
  cancel.addEventListener("click", () => {
    form.hidden = true;
    actions.hidden = false;
    message.textContent = "";
  });
  buttons.append(send, cancel);

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    const reason = text.value.trim();
    if (!reason) {
      message.textContent = "Please add a reason so the patient knows.";
      text.focus();
      return;
    }
    setButtonLoading(send, true, "Sending...");
    try {
      await bloodConnectApi(`/api/donors/me/alerts/${encodeURIComponent(alert.requestId)}/decline`, {
        method: "POST",
        body: JSON.stringify({ reason }),
      });
      donorAlerts = donorAlerts.filter((item) => item.requestId !== alert.requestId);
      renderAlerts();
      showBanner(`Thanks for letting them know. ${alert.patientName}'s family has been notified.`);
    } catch (error) {
      message.textContent = error.message;
      setButtonLoading(send, false);
    }
  });

  form.append(label, chips, text, message, buttons);
  return form;
};

const renderAlerts = () => {
  const visible = donorAlerts;
  const filtered = visible.filter((alert) =>
    activeFilter === "critical" ? alert.urgency === "CRITICAL"
      : activeFilter === "exact" ? alert.exactMatch
        : true);

  alertsToolbar.hidden = !visible.length;
  alertsStatus.textContent = visible.length
    ? `${visible.length} open request${visible.length === 1 ? "" : "s"} can use your blood.`
    : "";

  if (!visible.length) {
    showEmptyState("🎉", "You're all caught up", "No open requests need your blood group right now. We'll list new ones here.");
    return;
  }
  if (!filtered.length) {
    showEmptyState("🔍", "Nothing in this filter", "Try another filter to see the other alerts.");
    return;
  }
  alertsList.replaceChildren(...filtered.map(renderAlert));
};

const loadDonorAlerts = async () => {
  setLoadingStatus(alertsStatus, "Checking for requests that need your blood...");
  showDonorSkeletons(alertsList);
  try {
    const [profile, alerts] = await withMinimumLoader(Promise.all([
      bloodConnectApi("/api/donors/me"),
      bloodConnectApi("/api/donors/me/alerts"),
    ]));
    hideDonorSkeletons(alertsList);
    clearLoadingStatus(alertsStatus, "");
    donorDeferredUntil = profile.deferredUntil;
    if (donorDeferredUntil) {
      showBanner(`Based on your eligibility check, you can donate again from ${formatCalendarDate(donorDeferredUntil)}. Patients won't be matched with you until then.`);
    } else {
      showBanner(profile.available ? "" : "You are marked unavailable, so patients can't find you. Update your availability on your profile.");
    }
    donorAlerts = alerts;
    renderAlerts();
    loadAcceptedRequests();
  } catch (error) {
    hideDonorSkeletons(alertsList);
    clearLoadingStatus(alertsStatus, "");
    if (/profile not found/i.test(error.message)) {
      showEmptyState("🩸", "Finish your donor profile", "Add your blood group and location so we can alert you about nearby requests.",
        { label: "Complete profile", href: "/profile" });
      return;
    }
    showEmptyState("⚠️", "Alerts could not be loaded", error.message);
  }
};

const renderDecline = (decline) => {
  const card = element("article", "nearby-card alert-card decline-card");
  const top = element("div", "nearby-card-top");
  const who = element("div", "nearby-who");
  who.append(element("h3", "nearby-name", `${decline.donorName} can't donate`),
    element("p", "nearby-place", timeAgo(decline.createdAt)));
  top.append(element("span", "nearby-blood", decline.donorBloodGroup), who);
  card.append(top, element("p", "alert-message", `“${decline.reason}”`));
  return card;
};

const loadRequesterAlerts = async () => {
  document.getElementById("alertsLead").textContent = "Messages from donors about your blood request.";
  const requestId = localStorage.getItem("currentRequestId");
  if (!requestId) {
    showEmptyState("🔔", "No notifications yet", "Create an emergency request and donor messages about it will appear here.",
      { label: "Create request", href: "/emergency-request" });
    return;
  }
  setLoadingStatus(alertsStatus, "Loading notifications...");
  showDonorSkeletons(alertsList);
  try {
    const path = `/api/requests/${encodeURIComponent(requestId)}`;
    const [request, acceptances, declines] = await withMinimumLoader(Promise.all([
      bloodConnectApi(path),
      bloodConnectApi(`${path}/acceptances`),
      bloodConnectApi(`${path}/declines`),
    ]));
    hideDonorSkeletons(alertsList);
    const total = acceptances.length + declines.length;
    clearLoadingStatus(alertsStatus, total
      ? `${total} notification${total === 1 ? "" : "s"} for ${request.patientName}'s request.`
      : "");

    document.getElementById("acceptedTitle").textContent = "Donors who accepted";
    document.getElementById("acceptedCount").textContent = `${acceptances.length}`;
    acceptedSection.hidden = !acceptances.length;
    acceptedList.replaceChildren(...acceptances.map((donor) => buildAcceptedDonorCard(donor, request)));
    if (acceptances.length) {
      showBanner(request.status === "FULFILLED"
        ? "Your request is fulfilled. Rate the donors who helped so others know who to trust."
        : "Donors have accepted! Call or WhatsApp them to arrange the donation.");
    }

    alertsListTitle.textContent = "Donor replies";
    alertsListTitle.hidden = !acceptances.length || !declines.length;
    if (!declines.length) {
      if (acceptances.length) alertsList.replaceChildren();
      else showEmptyState("🔔", "No notifications yet", "When a donor responds to your request, their message will appear here.");
      return;
    }
    alertsList.replaceChildren(...declines.map(renderDecline));
  } catch (error) {
    hideDonorSkeletons(alertsList);
    clearLoadingStatus(alertsStatus, "");
    if (/not found/i.test(error.message)) {
      localStorage.removeItem("currentRequestId");
      showEmptyState("🔔", "No notifications yet", "Create an emergency request and donor messages about it will appear here.",
        { label: "Create request", href: "/emergency-request" });
      return;
    }
    showEmptyState("⚠️", "Notifications could not be loaded", error.message);
  }
};

document.querySelectorAll(".alerts-filter").forEach((button) => {
  button.addEventListener("click", () => {
    activeFilter = button.dataset.filter;
    document.querySelectorAll(".alerts-filter").forEach((other) => {
      other.classList.toggle("is-active", other === button);
      other.setAttribute("aria-pressed", String(other === button));
    });
    renderAlerts();
  });
});

document.getElementById("refreshAlertsButton").addEventListener("click", loadDonorAlerts);

if (!bloodConnectToken()) {
  showEmptyState("🔔", "Log in to see alerts", "Donors get alerts for nearby emergency requests that match their blood group.",
    { label: "Login", href: "/login" });
} else if (readCurrentUser().role === "DONOR") {
  loadDonorAlerts();
} else if (isAdmin()) {
  showEmptyState("🛡️", "Alerts are for donors and patients", "As an admin, you can see everyone on the admin dashboard.",
    { label: "Open admin dashboard", href: ADMIN_PAGE });
} else if (isBloodBank(readCurrentUser().role)) {
  showEmptyState("🏦", "Alerts are for donors and patients", "Keep your blood stock up to date so patients can find you.",
    { label: "Update blood stock", href: BANK_DASHBOARD_PAGE });
} else {
  loadRequesterAlerts();
}
