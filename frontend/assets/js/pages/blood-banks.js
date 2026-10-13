const bankList = document.getElementById("bankList");
const bankStatus = document.getElementById("bankStatus");
const bankBloodGroup = document.getElementById("bankBloodGroup");

const element = (tag, className, text) => {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text != null) node.textContent = text;
  return node;
};

const unitsText = (units) => `${units} unit${units === 1 ? "" : "s"}`;

const bankDirectionsUrl = (bank) => {
  const url = new URL("https://www.google.com/maps/dir/");
  url.searchParams.set("api", "1");
  url.searchParams.set("destination", bank.latitude != null && bank.longitude != null
    ? `${bank.latitude},${bank.longitude}` : `${bank.name}, ${bank.location}`);
  return url.toString();
};

const showEmptyState = (title, text) => {
  const empty = element("div", "nearby-empty");
  empty.append(element("div", "nearby-empty-icon", "🏦"), element("strong", "", title), element("p", "", text));
  bankList.replaceChildren(empty);
  removePager(bankList);
};

const buildStockChips = (stock, highlight) => {
  const chips = element("div", "bank-chips");
  BLOOD_GROUPS.forEach((group) => {
    const units = stock[group] || 0;
    const chip = element("span", `bank-chip${units ? "" : " is-empty"}${group === highlight ? " is-highlight" : ""}`);
    chip.append(element("strong", "", group), element("span", "", String(units)));
    chips.append(chip);
  });
  return chips;
};

const renderBank = (bank, bloodGroup) => {
  const card = element("article", "nearby-card bank-card");
  const top = element("div", "nearby-card-top");
  const who = element("div", "nearby-who");
  who.append(element("h3", "nearby-name", bank.name), element("p", "nearby-place", bank.location));
  top.append(element("div", "nearby-avatar bank-avatar", "🏦"), who,
    element("span", "bank-total", unitsText(bank.totalUnits)));

  const meta = element("p", "bank-meta",
    [bank.openHours && `🕒 ${bank.openHours}`, `Updated ${connectTimeAgo(bank.updatedAt)}`].filter(Boolean).join("  ·  "));

  const actions = element("div", "alert-actions");
  const directions = element("a", "tracking-btn", "📍 Directions");
  directions.href = bankDirectionsUrl(bank);
  directions.target = "_blank";
  directions.rel = "noopener noreferrer";
  const question = bloodGroup
    ? `Hi, is ${bloodGroup} blood available at ${bank.name}? I found you on BloodConnect.`
    : `Hi, I found ${bank.name} on BloodConnect and would like to check blood availability.`;
  actions.append(...buildContactButtons(bank.phone, question), directions);

  card.append(top, buildStockChips(bank.stock, bloodGroup), meta, actions);
  return card;
};

const searchBanks = async () => {
  const bloodGroup = bankBloodGroup.value;
  const params = new URLSearchParams();
  if (bloodGroup) params.set("bloodGroup", bloodGroup);

  setLoadingStatus(bankStatus, "Checking blood bank stock...");
  showDonorSkeletons(bankList);
  try {
    const banks = await withMinimumLoader(bloodConnectApi(`/api/blood-banks?${params}`));
    hideDonorSkeletons(bankList);
    if (!banks.length) {
      showEmptyState(bloodGroup ? `No ${bloodGroup} stock listed` : "No blood banks yet",
        bloodGroup ? "No registered blood bank has this group in stock right now. Try another group."
          : "Registered blood banks will appear here once they share their stock.");
      clearLoadingStatus(bankStatus, "");
      return;
    }
    renderPaged(bankList, banks, (bank) => renderBank(bank, bloodGroup));
    clearLoadingStatus(bankStatus, `${banks.length} blood bank${banks.length === 1 ? "" : "s"}${bloodGroup ? ` with ${bloodGroup} in stock` : ""}.`);
  } catch (error) {
    hideDonorSkeletons(bankList);
    showEmptyState("Blood banks could not be loaded", error.message);
    clearLoadingStatus(bankStatus, "");
  }
};

bankBloodGroup.addEventListener("change", searchBanks);

const linkedGroup = new URLSearchParams(window.location.search).get("group");
if (BLOOD_GROUPS.includes(linkedGroup)) bankBloodGroup.value = linkedGroup;

searchBanks();
