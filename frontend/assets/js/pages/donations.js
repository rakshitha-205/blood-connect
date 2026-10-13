const donationsStatus = document.getElementById("donationsStatus");

const donationNode = (tag, className, text) => {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text != null) node.textContent = text;
  return node;
};

const unitsText = (units) => `${units} unit${units === 1 ? "" : "s"}`;

const donationDate = (isoDate) =>
  new Date(isoDate).toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" });

const renderDonation = (donation) => {
  const fulfilled = donation.status === "FULFILLED";
  const card = donationNode("article", "nearby-card accepted-card");
  const top = donationNode("div", "nearby-card-top");
  const who = donationNode("div", "nearby-who");
  who.append(donationNode("h3", "nearby-name", donation.hospital), donationNode("p", "nearby-place", donation.location));
  const status = donationNode("span", "accepted-status", fulfilled ? "Donated" : "Waiting");
  status.dataset.status = fulfilled ? "fulfilled" : "open";
  top.append(donationNode("span", "nearby-blood", donation.bloodGroup), who, status);

  const summary = donationNode("p", "alert-summary", fulfilled
    ? `You helped ${donation.patientName} with ${unitsText(donation.units)} of ${donation.bloodGroup} blood`
    : `You accepted ${donation.patientName}'s request for ${unitsText(donation.units)}`);

  const meta = donationNode("div", "nearby-meta");
  meta.append(donationNode("span", "nearby-pill", `📅 ${donationDate(donation.acceptedAt)}`));
  card.append(top, summary, meta);

  if (!fulfilled) {
    const actions = donationNode("div", "alert-actions");
    actions.append(...buildContactButtons(donation.contact,
      `Hi, I'm ${readCurrentUser().fullName || "a blood donor"} from BloodConnect. I accepted the ${donation.bloodGroup} blood request for ${donation.patientName} at ${donation.hospital}.`,
      { callLabel: "Call patient" }));
    card.append(actions);
  }
  return card;
};

const showDonationList = (sectionId, listId, countId, donations) => {
  document.getElementById(sectionId).hidden = !donations.length;
  document.getElementById(countId).textContent = `${donations.length}`;
  document.getElementById(listId).replaceChildren(...donations.map(renderDonation));
};

const showDonationsEmpty = () => {
  const empty = donationNode("div", "nearby-empty");
  const link = donationNode("a", "login-btn match-empty-action", "See requests near me");
  link.href = ALERTS_PAGE;
  empty.append(donationNode("div", "nearby-empty-icon", "🩸"), donationNode("strong", "", "No donations yet"),
    donationNode("p", "", "When you accept a blood request it appears here, and once the patient receives blood it is added to your record."),
    link);
  donationsStatus.replaceChildren(empty);
};

const showDonationEligibility = (deferredUntil) => {
  const note = document.getElementById("donationEligibility");
  note.textContent = deferredUntil
    ? `⏳ You can donate again from ${formatCalendarDate(deferredUntil)}.`
    : "✅ You have no waiting period. Check your eligibility before your next donation.";
  note.hidden = false;
};

const loadDonations = async () => {
  setLoadingStatus(donationsStatus, "Loading your donations...");
  try {
    const [accepted, profile] = await Promise.all([
      bloodConnectApi("/api/donors/me/accepted"),
      bloodConnectApi("/api/donors/me").catch(() => null),
    ]);
    const byNewest = (a, b) => new Date(b.acceptedAt) - new Date(a.acceptedAt);
    const donated = accepted.filter((item) => item.status === "FULFILLED").sort(byNewest);
    const pending = accepted.filter((item) => item.status === "OPEN").sort(byNewest);

    document.getElementById("statDonations").textContent = `${donated.length}`;
    document.getElementById("statUnits").textContent = `${donated.reduce((sum, item) => sum + item.units, 0)}`;
    document.getElementById("statPending").textContent = `${pending.length}`;
    document.getElementById("donationStats").hidden = false;
    if (profile) showDonationEligibility(profile.deferredUntil);

    showDonationList("pendingSection", "pendingList", "pendingCount", pending);
    showDonationList("historySection", "historyList", "historyCount", donated);
    donationsStatus.classList.remove("is-loading");
    donationsStatus.replaceChildren();
    if (!donated.length && !pending.length) showDonationsEmpty();
  } catch (error) {
    donationsStatus.classList.remove("is-loading");
    donationsStatus.textContent = error.message;
  }
};

loadDonations();
