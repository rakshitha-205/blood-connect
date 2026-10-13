const byId = (id) => document.getElementById(id);
const bankMessage = byId("bankMessage");
const MAX_UNITS = 10000;

const stockInputs = {};

const buildStockEditor = () => {
  byId("stockEditor").replaceChildren(...BLOOD_GROUPS.map((group) => {
    const cell = document.createElement("div");
    cell.className = "stock-cell";
    const label = document.createElement("label");
    label.className = "stock-group";
    label.textContent = group;
    label.htmlFor = `stock-${group}`;

    const input = document.createElement("input");
    input.id = `stock-${group}`;
    input.type = "number";
    input.min = "0";
    input.max = String(MAX_UNITS);
    input.inputMode = "numeric";
    input.value = "0";
    input.addEventListener("input", () => cell.classList.toggle("is-empty", !Number(input.value)));

    const step = (delta, text) => {
      const button = document.createElement("button");
      button.type = "button";
      button.className = "stock-step";
      button.textContent = text;
      button.setAttribute("aria-label", `${delta > 0 ? "Add" : "Remove"} one unit of ${group}`);
      button.addEventListener("click", () => {
        input.value = String(Math.min(MAX_UNITS, Math.max(0, (Number(input.value) || 0) + delta)));
        input.dispatchEvent(new Event("input"));
      });
      return button;
    };

    const controls = document.createElement("div");
    controls.className = "stock-controls";
    controls.append(step(-1, "−"), input, step(1, "+"));
    cell.append(label, controls);
    cell.classList.add("is-empty");
    stockInputs[group] = input;
    return cell;
  }));
};

const showBank = (bank) => {
  byId("bankTitle").textContent = bank.name;
  byId("bankSubtitle").textContent = [bank.location, bank.openHours].filter(Boolean).join(" · ");
  byId("bankTotal").textContent = `${bank.totalUnits} unit${bank.totalUnits === 1 ? "" : "s"} in stock`;
  byId("bankUpdated").textContent = `Updated ${connectTimeAgo(bank.updatedAt)}`;
  byId("bankName").value = bank.name;
  byId("bankPhone").value = bank.phone;
  byId("bankHours").value = bank.openHours || "";
  byId("bankLocation").value = bank.location;
  byId("bankLatitude").value = bank.latitude ?? "";
  byId("bankLongitude").value = bank.longitude ?? "";
  if (bank.latitude != null && bank.longitude != null) markCoordinatesFor(byId("bankLocation"));
  BLOOD_GROUPS.forEach((group) => {
    stockInputs[group].value = String(bank.stock[group] || 0);
    stockInputs[group].dispatchEvent(new Event("input"));
  });
  byId("stockSection").hidden = false;
};

const saveBank = async (event) => {
  event.preventDefault();
  const button = byId("saveBankButton");
  setButtonLoading(button, true, "Saving...");
  try {
    bankMessage.textContent = "Checking your exact location...";
    await ensureExactLocation("bankLocation", "bankLatitude", "bankLongitude");
    const latitude = byId("bankLatitude").value;
    const longitude = byId("bankLongitude").value;
    const firstSave = byId("stockSection").hidden;
    const bank = await bloodConnectApi("/api/blood-banks/me", {
      method: "PUT",
      body: JSON.stringify({
        name: byId("bankName").value.trim(),
        phone: byId("bankPhone").value.trim(),
        location: byId("bankLocation").value.trim(),
        latitude: latitude ? Number(latitude) : null,
        longitude: longitude ? Number(longitude) : null,
        openHours: byId("bankHours").value.trim(),
      }),
    });
    showBank(bank);
    bankMessage.textContent = firstSave ? "Details saved. Now add your blood stock above." : "Your details were saved.";
    if (firstSave) byId("stockSection").scrollIntoView({ behavior: "smooth", block: "start" });
  } catch (error) {
    bankMessage.textContent = error.message;
  } finally {
    setButtonLoading(button, false);
  }
};

const saveStock = async (event) => {
  event.preventDefault();
  const button = byId("saveStockButton");
  const stock = Object.fromEntries(BLOOD_GROUPS.map((group) =>
    [group, Math.min(MAX_UNITS, Math.max(0, Math.floor(Number(stockInputs[group].value) || 0)))]));
  setButtonLoading(button, true, "Publishing...");
  try {
    const bank = await bloodConnectApi("/api/blood-banks/me/stock", {
      method: "PUT",
      body: JSON.stringify({ stock }),
    });
    showBank(bank);
    bankMessage.textContent = "Stock published. Patients can now see it on the Blood Banks page.";
  } catch (error) {
    bankMessage.textContent = error.message;
  } finally {
    setButtonLoading(button, false);
  }
};

const showGate = (title, text) => {
  byId("bankGateTitle").textContent = title;
  byId("bankGateText").textContent = text;
  byId("bankGate").hidden = false;
};

const loadDashboard = async () => {
  if (!bloodConnectToken()) {
    showGate("Log in as a blood bank", "Registered blood banks can share their live blood stock here.");
    return;
  }
  if (!isBloodBank(readCurrentUser().role)) {
    showGate("This page is for blood banks", "You can still see every blood bank's stock on the Blood Banks page.");
    byId("bankGate").querySelector(".profile-actions").replaceChildren(Object.assign(document.createElement("a"),
      { className: "profile-btn primary", href: BLOOD_BANKS_PAGE, textContent: "View blood banks" }));
    return;
  }
  buildStockEditor();
  byId("bankContent").hidden = false;
  try {
    showBank(await bloodConnectApi("/api/blood-banks/me"));
  } catch (error) {
    const user = readCurrentUser();
    byId("bankName").value = user.fullName || "";
    byId("bankPhone").value = user.phone || "";
    bankMessage.textContent = "Welcome! Add your blood bank details below to start sharing your stock.";
  }
};

byId("bankForm").addEventListener("submit", saveBank);
byId("stockForm").addEventListener("submit", saveStock);
byId("useBankLocation").addEventListener("click", () =>
  captureBloodConnectLocation("bankLatitude", "bankLongitude", "bankLocationMessage", "bankLocation"));

loadDashboard();
