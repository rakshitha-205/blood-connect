const adminStatus = document.getElementById("adminStatus");
const adminTable = document.getElementById("adminTable");
const adminSearch = document.getElementById("adminSearch");

const formatDate = (value) =>
  new Date(value).toLocaleDateString(undefined, { year: "numeric", month: "short", day: "numeric" });

const bloodGroupLabel = (group) => (group ? group.replace("-", "\u2212") : "");

const pill = (text, tone) => {
  const span = document.createElement("span");
  span.className = `admin-pill is-${tone}`;
  span.textContent = text;
  return span;
};

const stacked = (main, sub) => {
  const wrap = document.createElement("div");
  const strong = document.createElement("strong");
  strong.textContent = main || "—";
  wrap.append(strong);
  if (sub) {
    const small = document.createElement("small");
    small.textContent = sub;
    wrap.append(small);
  }
  return wrap;
};

const TABS = {
  hospitals: {
    title: "Hospitals",
    path: "/api/admin/hospitals",
    empty: "No hospitals have registered yet.",
    columns: ["Hospital", "Phone", "Requests", "Patients helped", "Joined"],
    search: (row) => [row.name, row.email, row.phone],
    cells: (row) => [
      stacked(row.name, row.email),
      row.phone || "—",
      String(row.requests),
      String(row.helped),
      formatDate(row.createdAt),
    ],
  },
  donors: {
    title: "Donors",
    path: "/api/admin/donors",
    empty: "No donors have registered yet.",
    columns: ["Donor", "Blood group", "Location", "Phone", "Status", "Joined"],
    search: (row) => [row.name, row.email, row.phone, row.bloodGroup, row.location],
    cells: (row) => [
      stacked(row.name, row.email),
      row.bloodGroup ? pill(bloodGroupLabel(row.bloodGroup), "blood") : "—",
      row.location || "—",
      row.phone || "—",
      row.available === null
        ? pill("Profile incomplete", "muted")
        : pill(row.available ? "Available" : "Unavailable", row.available ? "good" : "muted"),
      formatDate(row.createdAt),
    ],
  },
  helped: {
    title: "Patients helped",
    path: "/api/admin/helped",
    empty: "No requests have been marked as fulfilled yet.",
    columns: ["Patient", "Blood group", "Hospital", "Requested by", "Date"],
    search: (row) => [row.patientName, row.bloodGroup, row.hospital, row.location, row.requestedBy, row.requesterEmail],
    cells: (row) => [
      stacked(row.patientName, `${row.units} unit${row.units === 1 ? "" : "s"}`),
      pill(bloodGroupLabel(row.bloodGroup), "blood"),
      stacked(row.hospital, row.location),
      stacked(row.requestedBy, ROLE_LABELS[row.requesterRole] || row.requesterRole),
      formatDate(row.createdAt),
    ],
  },
};

const cache = {};
let activeTab = "hospitals";

const renderTable = () => {
  const tab = TABS[activeTab];
  const query = adminSearch.value.trim().toLowerCase();
  const rows = (cache[activeTab] || []).filter((row) =>
    !query || tab.search(row).some((value) => String(value || "").toLowerCase().includes(query)));

  const headRow = document.createElement("tr");
  tab.columns.forEach((column) => {
    const th = document.createElement("th");
    th.scope = "col";
    th.textContent = column;
    headRow.append(th);
  });
  adminTable.tHead.replaceChildren(headRow);

  const body = adminTable.tBodies[0];
  if (!rows.length) {
    const tr = document.createElement("tr");
    const td = document.createElement("td");
    td.colSpan = tab.columns.length;
    td.className = "admin-empty";
    td.textContent = query ? `Nothing matches "${adminSearch.value.trim()}".` : tab.empty;
    tr.append(td);
    body.replaceChildren(tr);
    return;
  }
  body.replaceChildren(...rows.map((row) => {
    const tr = document.createElement("tr");
    tab.cells(row).forEach((cell, index) => {
      const td = document.createElement("td");
      td.dataset.label = tab.columns[index];
      if (tab.columns[index] === "Phone") td.className = "admin-nowrap";
      td.append(cell);
      tr.append(td);
    });
    return tr;
  }));
};

const showTab = async (name) => {
  activeTab = name;
  document.getElementById("adminListTitle").textContent = TABS[name].title;
  adminSearch.value = "";
  document.querySelectorAll(".admin-stat[data-tab]").forEach((button) => {
    const active = button.dataset.tab === name;
    button.classList.toggle("is-active", active);
    button.setAttribute("aria-selected", active ? "true" : "false");
  });
  if (!cache[name]) {
    adminTable.tBodies[0].replaceChildren();
    adminTable.tHead.replaceChildren();
    setLoadingStatus(adminStatus, "Loading...");
    try {
      cache[name] = await withMinimumLoader(bloodConnectApi(TABS[name].path));
      clearLoadingStatus(adminStatus, "");
    } catch (error) {
      clearLoadingStatus(adminStatus, error.message);
      return;
    }
  }
  if (activeTab === name) renderTable();
};

const loadOverview = async () => {
  const overview = await bloodConnectApi("/api/admin/overview");
  document.getElementById("statHospitals").textContent = overview.hospitals;
  document.getElementById("statDonors").textContent = overview.donors;
  document.getElementById("statHelped").textContent = overview.helped;
};

const showLocked = (title, text, action) => {
  const locked = document.getElementById("adminLocked");
  const empty = document.createElement("div");
  empty.className = "nearby-empty";
  const icon = document.createElement("div");
  icon.className = "nearby-empty-icon";
  icon.textContent = "🛡️";
  const strong = document.createElement("strong");
  strong.textContent = title;
  const p = document.createElement("p");
  p.textContent = text;
  empty.append(icon, strong, p);
  if (action) {
    const link = document.createElement("a");
    link.className = "login-btn match-empty-action";
    link.href = action.href;
    link.textContent = action.label;
    empty.append(link);
  }
  locked.replaceChildren(empty);
  locked.hidden = false;
};

document.querySelectorAll("[data-tab]").forEach((button) => {
  button.addEventListener("click", () => showTab(button.dataset.tab));
});
adminSearch.addEventListener("input", renderTable);

if (!bloodConnectToken()) {
  showLocked("Admins only", "Log in with an admin account to open the dashboard.",
    { label: "Login", href: "/login" });
} else if (!isAdmin()) {
  showLocked("Admins only", "Your account doesn't have access to the admin dashboard.",
    { label: "Go home", href: "/" });
} else {
  document.getElementById("adminContent").hidden = false;
  loadOverview().catch((error) => { adminStatus.textContent = error.message; });
  showTab("hospitals");
}
