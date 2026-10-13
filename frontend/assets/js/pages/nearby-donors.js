const nearbyStatus = document.getElementById("nearbyStatus");
const donorGrid = document.getElementById("donorGrid");
const donorMap = document.getElementById("googleDonorMap");

function googleMapsEmbedUrl(query) {
  const url = new URL("https://www.google.com/maps");
  url.searchParams.set("q", query);
  url.searchParams.set("z", "11");
  url.searchParams.set("output", "embed");
  return url.toString();
}

function googleMapsDirectionsUrl(donor, origin) {
  const destination = donor.latitude != null && donor.longitude != null
    ? `${donor.latitude},${donor.longitude}`
    : donor.location;
  const url = new URL("https://www.google.com/maps/dir/");
  url.searchParams.set("api", "1");
  url.searchParams.set("destination", destination);
  if (origin) url.searchParams.set("origin", origin);
  return url.toString();
}

const element = (tag, className, text) => {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text != null) node.textContent = text;
  return node;
};

const initialsOf = (name) =>
  name.split(/\s+/).filter(Boolean).slice(0, 2).map((part) => part[0].toUpperCase()).join("") || "?";

const addDonorCard = (donor, origin, rank) => {
  const card = element("article", "nearby-card");

  const top = element("div", "nearby-card-top");
  const avatar = element("div", "nearby-avatar", initialsOf(donor.name));
  const who = element("div", "nearby-who");
  who.append(element("h3", "nearby-name", donor.name), element("p", "nearby-place", donor.location));
  top.append(element("span", "nearby-rank", `#${rank}`), avatar, who, element("span", "nearby-blood", donor.bloodGroup));

  const meta = element("div", "nearby-meta");
  const distanceText = donor.distanceKm == null ? "Distance unavailable" : `${donor.distanceKm} km away`;
  meta.append(
    element("span", `nearby-pill${donor.distanceKm == null ? " is-muted" : ""}`, distanceText),
    element("span", "nearby-pill is-available", "Available"),
  );
  const rating = buildRatingBadge(donor.rating, donor.ratingCount);
  if (rating) meta.append(rating);

  const directions = element("a", "nearby-directions", "Get directions");
  directions.href = googleMapsDirectionsUrl(donor, origin);
  directions.target = "_blank";
  directions.rel = "noopener noreferrer";

  const footer = element("div", "nearby-card-footer");
  footer.append(meta, directions);
  card.append(top, footer);
  donorGrid.append(card);
};

const showEmptyState = (title, text) => {
  const empty = element("div", "nearby-empty");
  empty.append(element("div", "nearby-empty-icon", "🩸"), element("strong", "", title), element("p", "", text));
  donorGrid.replaceChildren(empty);
};

async function searchDonors() {
  const bloodGroup = document.getElementById("bloodGroup").value;
  const locationText = document.getElementById("location").value.trim();
  if (!bloodGroup || !locationText) {
    nearbyStatus.textContent = "Choose a blood group and enter a location.";
    return;
  }
  if (!bloodConnectToken()) {
    nearbyStatus.textContent = "Log in to search registered donor profiles.";
    return;
  }
  try {
    setLoadingStatus(nearbyStatus, "Checking the exact location...");
    await ensureExactLocation("location", "latitude", "longitude");
  } catch (error) {
    clearLoadingStatus(nearbyStatus, error.message);
    return;
  }
  const latitude = document.getElementById("latitude").value;
  const longitude = document.getElementById("longitude").value;

  const origin = latitude && longitude ? `${latitude},${longitude}` : locationText;
  donorMap.src = googleMapsEmbedUrl(origin);

  const params = new URLSearchParams({ bloodGroup });
  if (latitude && longitude) {
    params.set("latitude", latitude);
    params.set("longitude", longitude);
  }
  const searchButton = document.getElementById("searchDonorsButton");
  setLoadingStatus(nearbyStatus, "Finding compatible donors near you...");
  showDonorSkeletons(donorGrid);
  setButtonLoading(searchButton, true, "Searching...");
  try {
    const donors = await withMinimumLoader(bloodConnectApi(`/api/donors/nearby?${params}`));
    hideDonorSkeletons(donorGrid);
    donors.forEach((donor, index) => addDonorCard(donor, origin, index + 1));
    if (!donors.length) {
      showEmptyState("No donors found yet", "No compatible donors are available right now. Try another location later.");
    }
    clearLoadingStatus(nearbyStatus,
      `${donors.length} compatible, available donor${donors.length === 1 ? "" : "s"} found.`);
  } catch (error) {
    hideDonorSkeletons(donorGrid);
    showEmptyState("Search could not be completed", error.message);
    clearLoadingStatus(nearbyStatus, error.message);
  } finally {
    setButtonLoading(searchButton, false);
  }
}

window.searchDonors = searchDonors;
document.getElementById("nearbySearchForm").addEventListener("submit", (event) => {
  event.preventDefault();
  searchDonors();
});
document.getElementById("useLocationButton").addEventListener("click", () =>
  captureBloodConnectLocation("latitude", "longitude", "nearbyStatus"));

if (!bloodConnectToken()) {
  lockBehindLogin(document.getElementById("nearbyStage"), "Log in to find donors",
    "Donor details are shared only with registered BloodConnect members.");
}
