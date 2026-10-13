const stage = document.getElementById("requestStage");
const matchList = document.getElementById("matchList");
const matchStatus = document.getElementById("matchStatus");
const trackingMessage = document.getElementById("trackingMessage");
const trackingContent = document.getElementById("trackingContent");
const trackingEmpty = document.getElementById("trackingEmpty");
const MAX_MATCH_SCORE = 100;
const TOP_DONORS = 3;
const REQUEST_TABS = ["new", "matches", "track"];
const STATUS_LABELS = { OPEN: "Open", FULFILLED: "Fulfilled", CANCELLED: "Cancelled" };
const loadedFor = { matches: null, track: null };

const currentRequestId = () => localStorage.getItem("currentRequestId");

const element = (tag, className, text) => {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text != null) node.textContent = text;
  return node;
};

const titleCase = (value) =>
  String(value || "").toLowerCase().replace(/(^|\s)\S/g, (letter) => letter.toUpperCase());

const initialsOf = (name) =>
  name.split(/\s+/).filter(Boolean).slice(0, 2).map((part) => part[0].toUpperCase()).join("") || "?";

const timeAgo = (isoDate) => {
  const minutes = Math.max(0, Math.round((Date.now() - new Date(isoDate).getTime()) / 60000));
  if (minutes < 1) return "just now";
  if (minutes < 60) return `${minutes} min ago`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${hours} hr${hours === 1 ? "" : "s"} ago`;
  const days = Math.round(hours / 24);
  return `${days} day${days === 1 ? "" : "s"} ago`;
};

const buildEmptyState = (icon, title, text, action) => {
  const empty = element("div", "nearby-empty");
  empty.append(element("div", "nearby-empty-icon", icon), element("strong", "", title), element("p", "", text));
  if (!action) return empty;
  const link = element(action.tab ? "button" : "a", "login-btn match-empty-action", action.label);
  if (action.tab) {
    link.type = "button";
    link.addEventListener("click", () => showTab(action.tab));
  } else {
    link.href = action.href;
  }
  empty.append(link);
  return empty;
};

const requestBlocker = () => {
  if (!bloodConnectToken()) {
    return { title: "Log in to see your request", text: "Requests are made from a patient account so donors can reach you.",
      action: { label: "Login", href: LOGIN_PAGE } };
  }
  if (!canRequestBlood(readCurrentUser().role)) {
    return { title: "This is for patient accounts", text: "You're signed in as a donor. See requests that need your blood in Alerts.",
      action: { label: "Open alerts", href: ALERTS_PAGE } };
  }
  if (!currentRequestId()) {
    return { title: "No request yet", text: "Create an emergency request and its matches and progress will appear here.",
      action: { label: "Create request", tab: "new" } };
  }
  return null;
};

/* New request */

if (!bloodConnectToken()) {
  lockBehindLogin(stage, "Log in to request blood", "Requests are made from a patient account so donors can reach you.");
} else if (!canRequestBlood(readCurrentUser().role)) {
  lockBehindLogin(stage, "Patient account needed",
    "You're signed in as a donor. See requests that need your blood in Alerts instead.", false);
}

document.getElementById("useLocation").addEventListener("click", () =>
  captureBloodConnectLocation("latitude", "longitude", "locationMessage"));

document.getElementById("bloodRequestForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const form = event.currentTarget;
  const message = document.getElementById("formMessage");
  const value = (id) => document.getElementById(id).value.trim();
  const submit = document.getElementById("submitRequest");
  setButtonLoading(submit, true, "Finding donors...");
  try {
    message.textContent = "Checking the exact hospital location...";
    await ensureExactLocation("location", "latitude", "longitude");
    message.textContent = "";
    const latitude = value("latitude");
    const longitude = value("longitude");
    const payload = {
      patientName: value("patientName"), bloodGroup: value("bloodGroup"), units: Number(value("units")),
      hospital: value("hospital"), location: value("location"), urgency: value("urgency"),
      contact: value("contact"), message: value("message"),
      latitude: latitude ? Number(latitude) : null, longitude: longitude ? Number(longitude) : null,
    };
    const request = await bloodConnectApi("/api/requests", { method: "POST", body: JSON.stringify(payload) });
    localStorage.setItem("currentRequestId", request.id);
    form.reset();
    showTab("matches");
  } catch (error) {
    message.textContent = error.message;
  } finally {
    setButtonLoading(submit, false);
  }
});

/* Matches */

const directionsUrl = (donor, request) => {
  const url = new URL("https://www.google.com/maps/dir/");
  url.searchParams.set("api", "1");
  url.searchParams.set("destination", donor.latitude != null && donor.longitude != null
    ? `${donor.latitude},${donor.longitude}`
    : donor.location);
  if (request?.latitude != null && request?.longitude != null) {
    url.searchParams.set("origin", `${request.latitude},${request.longitude}`);
  }
  return url.toString();
};

const showMatchesEmpty = (title, text, action) => {
  matchList.replaceChildren(buildEmptyState("🩸", title, text, action));
  removePager(matchList);
};

const showMatchRequest = (request) => {
  document.getElementById("resultBloodGroup").textContent = request.bloodGroup.replace("-", "\u2212");
  document.getElementById("resultPatient").textContent = request.patientName;
  document.getElementById("resultUnits").textContent = `${request.units} unit${request.units === 1 ? "" : "s"}`;
  document.getElementById("resultHospital").textContent = request.hospital;
  document.getElementById("resultLocation").textContent = request.location;
  document.getElementById("resultStatus").textContent = titleCase(request.status);
  const urgency = document.getElementById("resultUrgency");
  urgency.textContent = titleCase(request.urgency);
  urgency.dataset.level = String(request.urgency || "").toLowerCase();
  document.getElementById("matchRequest").hidden = false;
};

const showRanking = (ranking) => {
  const aiRanked = ranking.rankedBy === "AI";
  const tag = document.getElementById("matchSortTag");
  tag.textContent = aiRanked ? "Best match first" : "Nearest first";
  const note = document.getElementById("matchRankingNote");
  note.textContent = aiRanked ? "Ranked by blood group, distance and availability." : "";
  note.hidden = !note.textContent;
};

const renderMatch = (donor, request, rank) => {
  const card = element("article", "nearby-card");

  const top = element("div", "nearby-card-top");
  const who = element("div", "nearby-who");
  who.append(element("h3", "nearby-name", donor.name), element("p", "nearby-place", donor.location));
  top.append(
    element("span", "nearby-rank", `#${rank}`),
    element("div", "nearby-avatar", initialsOf(donor.name)),
    who,
    element("span", "nearby-blood", donor.bloodGroup),
  );

  const score = Math.max(0, Math.min(MAX_MATCH_SCORE, donor.priorityScore));
  const scoreRow = element("div", "match-score");
  const scoreLabel = element("div", "match-score-label");
  scoreLabel.append(element("span", "", "Match score"), element("strong", "", `${score}/${MAX_MATCH_SCORE}`));
  const bar = element("div", "match-score-bar");
  const fill = element("span", "");
  fill.style.width = `${score}%`;
  bar.append(fill);
  scoreRow.append(scoreLabel, bar);

  const meta = element("div", "nearby-meta");
  meta.append(
    element("span", `nearby-pill${donor.distanceKm == null ? " is-muted" : ""}`,
      donor.distanceKm == null ? "Distance unavailable" : `${donor.distanceKm} km away`),
    element("span", `nearby-pill${donor.available ? " is-available" : " is-muted"}`,
      donor.available ? "Available" : "Unavailable"),
  );
  if (donor.bloodGroup === request.bloodGroup) meta.append(element("span", "nearby-pill is-exact", "Exact group"));
  const rating = buildRatingBadge(donor.rating, donor.ratingCount);
  if (rating) meta.append(rating);

  const directions = element("a", "nearby-directions", "Get directions");
  directions.href = directionsUrl(donor, request);
  directions.target = "_blank";
  directions.rel = "noopener noreferrer";

  const footer = element("div", "nearby-card-footer");
  footer.append(meta, directions);
  card.append(top, scoreRow);
  if (donor.aiReason) card.append(element("p", "match-reason", donor.aiReason));
  card.append(footer);
  return card;
};

const loadMatches = async () => {
  const blocker = requestBlocker();
  if (blocker) {
    document.getElementById("matchRequest").hidden = true;
    clearLoadingStatus(matchStatus, "");
    showMatchesEmpty(blocker.title, blocker.text, blocker.action);
    return;
  }
  const requestId = currentRequestId();
  loadedFor.matches = requestId;
  setLoadingStatus(matchStatus, "Finding and ranking compatible donors...");
  showDonorSkeletons(matchList);
  removePager(matchList);
  try {
    const [request, ranking] = await withMinimumLoader(Promise.all([
      bloodConnectApi(`/api/requests/${encodeURIComponent(requestId)}`),
      bloodConnectApi(`/api/requests/${encodeURIComponent(requestId)}/matches/ai`),
    ]));
    const matches = ranking.matches;
    hideDonorSkeletons(matchList);
    showMatchRequest(request);
    showRanking(ranking);
    if (!matches.length) {
      clearLoadingStatus(matchStatus, "");
      showMatchesEmpty("No matching donors yet", "No compatible, available donors were found. Check again soon.");
      return;
    }
    clearLoadingStatus(matchStatus, `${matches.length} compatible donor${matches.length === 1 ? "" : "s"} found.`);
    renderPaged(matchList, matches, (donor, index) => renderMatch(donor, request, index + 1));
  } catch (error) {
    loadedFor.matches = null;
    hideDonorSkeletons(matchList);
    clearLoadingStatus(matchStatus, "");
    showMatchesEmpty("Matches could not be loaded", error.message);
  }
};

/* Track */

const showTrackingEmpty = (title, text, action) => {
  trackingEmpty.replaceChildren(buildEmptyState("📋", title, text, action));
  trackingContent.hidden = true;
};

const stepsFor = (request, matchCount, acceptedCount) => {
  const cancelled = request.status === "CANCELLED";
  const fulfilled = request.status === "FULFILLED";
  const hasDonors = matchCount > 0 || acceptedCount > 0;
  const steps = [
    { title: "Request submitted", text: `Sent ${timeAgo(request.createdAt)}.`, state: "done" },
    {
      title: "Finding donors",
      text: hasDonors
        ? `${matchCount} compatible donor${matchCount === 1 ? "" : "s"} found nearby.`
        : "Looking for compatible, available donors.",
      state: hasDonors || fulfilled ? "done" : "current",
    },
    {
      title: "Contact a donor",
      text: "Call a matched donor and arrange the donation at the hospital.",
      state: fulfilled ? "done" : hasDonors ? "current" : "upcoming",
    },
    {
      title: "Blood received",
      text: fulfilled ? "Marked as fulfilled. Thank you for using BloodConnect." : "Mark the request fulfilled once blood is received.",
      state: fulfilled ? "done" : "upcoming",
    },
  ];
  if (cancelled) {
    return [steps[0], { title: "Request cancelled", text: "This request is closed and donors are no longer matched.", state: "cancelled" }];
  }
  return steps;
};

const renderSteps = (request, matchCount, acceptedCount) => {
  const list = document.getElementById("trackingSteps");
  list.replaceChildren(...stepsFor(request, matchCount, acceptedCount).map((step, index) => {
    const item = element("li", `tracking-step is-${step.state}`);
    const marker = element("span", "tracking-step-marker",
      step.state === "done" ? "✓" : step.state === "cancelled" ? "✕" : String(index + 1));
    const body = element("div", "tracking-step-body");
    body.append(element("strong", "", step.title), element("p", "", step.text));
    if (step.state === "current") item.setAttribute("aria-current", "step");
    item.append(marker, body);
    return item;
  }));
};

const renderTopDonors = (request, matches) => {
  const list = document.getElementById("trackingDonors");
  if (request.status !== "OPEN") {
    list.replaceChildren(element("li", "tracking-donors-empty", "Donor matching stops once a request is closed."));
    return;
  }
  if (!matches.length) {
    list.replaceChildren(element("li", "tracking-donors-empty", "No compatible donors yet. Check again soon."));
    return;
  }
  list.replaceChildren(...matches.slice(0, TOP_DONORS).map((donor) => {
    const item = element("li", "tracking-donor");
    const who = element("div", "tracking-donor-who");
    who.append(element("strong", "", donor.name), element("span", "",
      donor.distanceKm == null ? donor.location : `${donor.distanceKm} km away · ${donor.location}`));
    item.append(element("span", "nearby-blood", donor.bloodGroup), who);
    return item;
  }));
};

const renderAccepted = (request, acceptances) => {
  document.getElementById("acceptedCard").hidden = !acceptances.length;
  document.getElementById("acceptedCount").textContent = `${acceptances.length} accepted`;
  document.getElementById("acceptedLead").textContent = request.status === "FULFILLED"
    ? "Blood received. Rate the donors who helped you."
    : "These donors said they can donate. Reach them directly to arrange the donation.";
  renderPaged(document.getElementById("acceptedDonors"), acceptances,
    (donor) => buildAcceptedDonorCard(donor, request));
};

const renderTrackedRequest = (request, matches, acceptances) => {
  document.getElementById("trackedBloodGroup").textContent = request.bloodGroup.replace("-", "\u2212");
  document.getElementById("trackedPatient").textContent = request.patientName;
  document.getElementById("trackedCreatedAt").textContent =
    `Submitted ${new Date(request.createdAt).toLocaleString()}`;
  document.getElementById("trackedUnits").textContent = `${request.units} unit${request.units === 1 ? "" : "s"}`;
  document.getElementById("trackedHospital").textContent = request.hospital;
  document.getElementById("trackedLocation").textContent = request.location;
  const urgency = document.getElementById("trackedUrgency");
  urgency.textContent = titleCase(request.urgency);
  urgency.dataset.level = String(request.urgency || "").toLowerCase();
  const status = document.getElementById("trackedStatus");
  status.textContent = STATUS_LABELS[request.status] || titleCase(request.status);
  status.dataset.status = String(request.status || "").toLowerCase();

  renderSteps(request, matches.length, acceptances.length);
  renderAccepted(request, acceptances);
  renderTopDonors(request, matches);
  document.getElementById("trackingActions").hidden = request.status !== "OPEN";
  trackingEmpty.replaceChildren();
  trackingContent.hidden = false;
};

const loadTrackedRequest = async () => {
  const blocker = requestBlocker();
  if (blocker) {
    clearLoadingStatus(trackingMessage, "");
    showTrackingEmpty(blocker.title, blocker.text, blocker.action);
    return;
  }
  const requestId = currentRequestId();
  loadedFor.track = requestId;
  setLoadingStatus(trackingMessage, "Loading your request...");
  try {
    const path = `/api/requests/${encodeURIComponent(requestId)}`;
    const [request, matches, acceptances] = await withMinimumLoader(Promise.all([
      bloodConnectApi(path),
      bloodConnectApi(`${path}/matches`),
      bloodConnectApi(`${path}/acceptances`),
    ]));
    clearLoadingStatus(trackingMessage, "");
    renderTrackedRequest(request, matches, acceptances);
  } catch (error) {
    loadedFor.track = null;
    clearLoadingStatus(trackingMessage, "");
    showTrackingEmpty("Request could not be loaded", error.message);
  }
};

const updateStatus = async (button, status, loadingText) => {
  setButtonLoading(button, true, loadingText);
  try {
    await bloodConnectApi(`/api/requests/${encodeURIComponent(currentRequestId())}/status`, {
      method: "PATCH",
      body: JSON.stringify({ status }),
    });
    loadedFor.matches = null;
    await loadTrackedRequest();
  } catch (error) {
    trackingMessage.textContent = error.message;
  } finally {
    setButtonLoading(button, false);
  }
};

document.getElementById("refreshRequestButton").addEventListener("click", loadTrackedRequest);

document.getElementById("fulfilledRequestButton").addEventListener("click", (event) => {
  if (window.confirm("Mark this request as fulfilled? Donors will stop being matched.")) {
    updateStatus(event.currentTarget, "FULFILLED", "Saving...");
  }
});

document.getElementById("cancelRequestButton").addEventListener("click", (event) => {
  if (window.confirm("Cancel this blood request? This cannot be undone.")) {
    updateStatus(event.currentTarget, "CANCELLED", "Cancelling...");
  }
});

/* Tabs */

const showTab = (tab) => {
  REQUEST_TABS.forEach((name) => {
    const active = name === tab;
    const button = document.getElementById(`tab-${name}`);
    button.setAttribute("aria-selected", String(active));
    button.tabIndex = active ? 0 : -1;
    document.getElementById(`panel-${name}`).hidden = !active;
  });
  const url = new URL(window.location.href);
  if (tab === "new") url.searchParams.delete("tab");
  else url.searchParams.set("tab", tab);
  window.history.replaceState(null, "", url);

  const requestId = currentRequestId();
  if (tab === "matches" && (!requestId || loadedFor.matches !== requestId)) loadMatches();
  if (tab === "track" && (!requestId || loadedFor.track !== requestId)) loadTrackedRequest();
};

document.querySelectorAll("[data-tab]").forEach((button) => {
  button.addEventListener("click", () => showTab(button.dataset.tab));
});

document.querySelector(".request-tabs").addEventListener("keydown", (event) => {
  const step = { ArrowRight: 1, ArrowLeft: -1 }[event.key];
  if (!step) return;
  const current = REQUEST_TABS.indexOf(document.activeElement?.dataset.tab);
  if (current < 0) return;
  const next = REQUEST_TABS[(current + step + REQUEST_TABS.length) % REQUEST_TABS.length];
  showTab(next);
  document.getElementById(`tab-${next}`).focus();
});

const startTab = new URLSearchParams(window.location.search).get("tab");
showTab(REQUEST_TABS.includes(startTab) ? startTab : "new");
