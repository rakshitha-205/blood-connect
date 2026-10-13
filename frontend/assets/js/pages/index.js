const heroSecondaryButton = document.getElementById("heroSecondaryButton");
const homeRole = bloodConnectToken() ? readCurrentUser().role : "";
const emergencyButtons = document.querySelectorAll(`.emergency-btn[data-href="${EMERGENCY_PAGE}"]`);

if (isBloodBank(homeRole)) {
  heroSecondaryButton.textContent = "📋 Update My Blood Stock";
  heroSecondaryButton.dataset.href = BANK_DASHBOARD_PAGE;
} else if (homeRole === "DONOR") {
  heroSecondaryButton.textContent = "🩸 My Donations";
  heroSecondaryButton.dataset.href = DONATIONS_PAGE;
  emergencyButtons.forEach((button) => {
    button.textContent = "🔔 See Requests Near Me";
    button.dataset.href = ALERTS_PAGE;
  });
  const emergencySection = document.querySelector(".emergency-section");
  emergencySection.querySelector("h2").textContent = "Someone Nearby Needs Blood?";
  emergencySection.querySelector("p").textContent = "See emergency requests that match your blood group and accept one you can help with.";
} else if (canRequestBlood(homeRole)) {
  heroSecondaryButton.hidden = true;
}

if (homeRole === "ADMIN" || isBloodBank(homeRole)) {
  emergencyButtons.forEach((button) => {
    button.hidden = true;
  });
  document.querySelector(".emergency-section").hidden = true;
}

const showHomeStock = (banks) => {
  const totals = Object.fromEntries(BLOOD_GROUPS.map((group) =>
    [group, banks.reduce((sum, bank) => sum + (bank.stock[group] || 0), 0)]));
  const allUnits = Object.values(totals).reduce((sum, units) => sum + units, 0);
  document.getElementById("homeStockTotal").textContent = `${allUnits} units`;
  document.getElementById("homeBankCount").textContent = String(banks.length);
  document.getElementById("homeStockGrid").replaceChildren(...BLOOD_GROUPS.map((group) => {
    const cell = document.createElement("a");
    cell.className = `home-stock-cell${totals[group] ? "" : " is-empty"}`;
    cell.href = `${BLOOD_BANKS_PAGE}?group=${encodeURIComponent(group)}`;
    const name = document.createElement("strong");
    name.textContent = group;
    const units = document.createElement("span");
    units.textContent = `${totals[group]} units`;
    cell.append(name, units);
    return cell;
  }));
};

const loadHomeStock = async () => {
  try {
    showHomeStock(await bloodConnectApi("/api/blood-banks"));
  } catch {
    document.getElementById("homeStockTotal").textContent = "";
    const note = document.createElement("p");
    note.className = "home-stock-note";
    note.textContent = "Live stock is unavailable right now. Open the blood banks page to check again.";
    document.getElementById("homeStockGrid").replaceChildren(note);
  }
};

loadHomeStock();

document.querySelectorAll("[data-href]").forEach((button) => {
  button.addEventListener("click", () => {
    window.location.href = button.dataset.href;
  });
});
