const PROFILE_ICON =
  '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="8" r="4"/><path d="M4 20c0-4 3.6-6.5 8-6.5s8 2.5 8 6.5"/></svg>';
const ADMIN_ICON =
  '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 3l8 3v6c0 4.5-3.4 8.3-8 9-4.6-.7-8-4.5-8-9V6z"/><path d="M9 12l2 2 4-4"/></svg>';
const LOGOUT_ICON =
  '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M15 17l5-5-5-5"/><path d="M20 12H9"/><path d="M12 20H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h6"/></svg>';

const NAV_ICONS = {
  home: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M3 11l9-7 9 7"/><path d="M5 10v10h14V10"/><path d="M10 20v-6h4v6"/></svg>',
  find: '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="11" cy="11" r="7"/><path d="M20 20l-4-4"/></svg>',
  emergency:
    '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 3s6 6.5 6 11a6 6 0 0 1-12 0c0-4.5 6-11 6-11z"/><path d="M12 11v6M9 14h6"/></svg>',
  alerts:
    '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 8a6 6 0 0 1 12 0c0 7 3 8 3 8H3s3-1 3-8"/><path d="M10 20a2 2 0 0 0 4 0"/></svg>',
  matches:
    '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="9" cy="8" r="3.5"/><path d="M2.5 20c0-3.6 2.9-6 6.5-6s6.5 2.4 6.5 6"/><path d="M16 4.5a3.5 3.5 0 0 1 0 7"/><path d="M18 14c2.2.5 3.5 2.6 3.5 6"/></svg>',
  admin: ADMIN_ICON,
  bank: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M3 9l9-5 9 5"/><path d="M5 9v9M19 9v9M9.5 9v9M14.5 9v9"/><path d="M3 20h18"/></svg>',
  stock: '<svg viewBox="0 0 24 24" aria-hidden="true"><rect x="4" y="3" width="16" height="18" rx="2"/><path d="M8 8h8M8 12h8M8 16h5"/></svg>',
  about: '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="9"/><path d="M12 11v5"/><path d="M12 8h.01"/></svg>',
  eligibility:
    '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 20s-7.5-4.6-7.5-10.2A4.3 4.3 0 0 1 12 7a4.3 4.3 0 0 1 7.5 2.8C19.5 15.4 12 20 12 20z"/><path d="M9 12.5l2 2 4-4"/></svg>',
};

const LINK_ICONS = {
  [HOME_PAGE]: NAV_ICONS.home,
  [NEARBY_DONORS_PAGE]: NAV_ICONS.find,
  [EMERGENCY_PAGE]: NAV_ICONS.emergency,
  [DONATIONS_PAGE]: NAV_ICONS.emergency,
  [ALERTS_PAGE]: NAV_ICONS.alerts,
  [BLOOD_BANKS_PAGE]: NAV_ICONS.bank,
  [BANK_DASHBOARD_PAGE]: NAV_ICONS.stock,
  [ADMIN_PAGE]: NAV_ICONS.admin,
  [ABOUT_PAGE]: NAV_ICONS.about,
  [ELIGIBILITY_PAGE]: NAV_ICONS.eligibility,
};

const LOGO_DROP =
  '<svg class="logo-drop" viewBox="0 0 24 24"><defs><linearGradient id="logoDropFill" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#fb7185"/><stop offset="1" stop-color="#be123c"/></linearGradient></defs><path fill="url(#logoDropFill)" d="M12 2.5s7 7.6 7 12.3a7 7 0 0 1-14 0C5 10.1 12 2.5 12 2.5z"/><path fill="none" stroke="#fff" stroke-opacity="0.7" stroke-width="1.6" stroke-linecap="round" d="M8.6 14.2a3.6 3.6 0 0 0 2.2 3.5"/></svg>';

const MENU_ICON =
  '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 7h16M4 12h16M4 17h16"/></svg>';
const CLOSE_ICON =
  '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 6l12 12M18 6L6 18"/></svg>';
const desktopNav = window.matchMedia("(min-width: 981px)");

const addNavIcons = (links) => {
  links.querySelectorAll(":scope > a:not(.login-btn)").forEach((link) => {
    const label = link.textContent.trim();
    link.dataset.label = label;
    link.innerHTML = `<span class="nav-icon">${LINK_ICONS[link.getAttribute("href")] || NAV_ICONS.home}</span><span class="nav-label">${label}</span>`;
  });
};

const setupMenuToggle = (navbar, links) => {
  const toggle = document.createElement("button");
  toggle.type = "button";
  toggle.className = "nav-toggle";
  toggle.innerHTML = `<span class="nav-toggle-open">${MENU_ICON}</span><span class="nav-toggle-close">${CLOSE_ICON}</span>`;
  navbar.prepend(toggle);

  const backdrop = document.createElement("div");
  backdrop.className = "nav-backdrop";
  document.body.append(backdrop);

  const setMenuOpen = (open) => {
    document.body.classList.toggle("nav-open", open);
    toggle.setAttribute("aria-expanded", open ? "true" : "false");
    toggle.setAttribute("aria-label", open ? "Close menu" : "Open menu");
    toggle.title = open ? "Close menu" : "Open menu";
  };
  setMenuOpen(false);

  toggle.addEventListener("click", () => setMenuOpen(!document.body.classList.contains("nav-open")));
  backdrop.addEventListener("click", () => setMenuOpen(false));
  links.addEventListener("click", (event) => {
    if (event.target.closest("a")) setMenuOpen(false);
  });
  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape" && document.body.classList.contains("nav-open")) setMenuOpen(false);
  });
  desktopNav.addEventListener("change", () => setMenuOpen(false));
};

const ELIGIBILITY_LABEL = "Can I donate?";

const ADMIN_NAV_ITEMS = [
  { href: HOME_PAGE, label: "Home", icon: NAV_ICONS.home },
  { href: ABOUT_PAGE, label: "About", icon: NAV_ICONS.about },
  { href: ELIGIBILITY_PAGE, label: ELIGIBILITY_LABEL, icon: NAV_ICONS.eligibility, topNavOnly: true },
  { href: ADMIN_PAGE, label: "Admin", icon: NAV_ICONS.admin },
];

const BLOOD_BANK_NAV_ITEMS = [
  { href: HOME_PAGE, label: "Home", icon: NAV_ICONS.home },
  { href: ABOUT_PAGE, label: "About", icon: NAV_ICONS.about },
  { href: ELIGIBILITY_PAGE, label: ELIGIBILITY_LABEL, icon: NAV_ICONS.eligibility, topNavOnly: true },
  { href: BANK_DASHBOARD_PAGE, label: "My stock", icon: NAV_ICONS.stock, primary: true },
  { href: BLOOD_BANKS_PAGE, label: "Blood Banks", icon: NAV_ICONS.bank },
];

const REQUESTER_NAV_ITEMS = [
  { href: HOME_PAGE, label: "Home", icon: NAV_ICONS.home },
  { href: ABOUT_PAGE, label: "About", icon: NAV_ICONS.about, topNavOnly: true },
  { href: NEARBY_DONORS_PAGE, label: "Find Donor", icon: NAV_ICONS.find },
  { href: EMERGENCY_PAGE, label: "Emergency", icon: NAV_ICONS.emergency, primary: true },
  { href: ALERTS_PAGE, label: "Alerts", icon: NAV_ICONS.alerts },
  { href: BLOOD_BANKS_PAGE, label: "Blood Banks", icon: NAV_ICONS.bank, topNavOnly: true },
];

const DONOR_NAV_ITEMS = [
  { href: HOME_PAGE, label: "Home", icon: NAV_ICONS.home },
  { href: ABOUT_PAGE, label: "About", icon: NAV_ICONS.about, topNavOnly: true },
  { href: ELIGIBILITY_PAGE, label: ELIGIBILITY_LABEL, icon: NAV_ICONS.eligibility },
  { href: DONATIONS_PAGE, label: "My Donations", icon: NAV_ICONS.emergency, primary: true },
  { href: ALERTS_PAGE, label: "Alerts", icon: NAV_ICONS.alerts },
  { href: BLOOD_BANKS_PAGE, label: "Blood Banks", icon: NAV_ICONS.bank, topNavOnly: true },
];

const ROLE_NAV_ITEMS = {
  ADMIN: ADMIN_NAV_ITEMS,
  BLOOD_BANK: BLOOD_BANK_NAV_ITEMS,
  REQUESTER: REQUESTER_NAV_ITEMS,
  HOSPITAL: REQUESTER_NAV_ITEMS,
  DONOR: DONOR_NAV_ITEMS,
};

const roleNavItems = () => (bloodConnectToken() ? ROLE_NAV_ITEMS[readCurrentUser().role] || null : null);

const buildAccountMenu = () => {
  const user = readCurrentUser();
  const email = user.email || "";
  const name = user.fullName || localStorage.getItem(`displayName:${email}`) || email.split("@")[0] || "My account";

  const menu = document.createElement("div");
  menu.className = "account-menu";

  const trigger = document.createElement("button");
  trigger.type = "button";
  trigger.className = "account-trigger";
  trigger.setAttribute("aria-label", "Open account menu");
  trigger.setAttribute("aria-haspopup", "menu");
  trigger.setAttribute("aria-expanded", "false");
  trigger.innerHTML = PROFILE_ICON;
  const triggerLabel = document.createElement("span");
  triggerLabel.className = "account-trigger-label";
  const triggerName = document.createElement("strong");
  triggerName.textContent = name;
  const triggerRole = document.createElement("small");
  triggerRole.textContent = ROLE_LABELS[user.role] || "My account";
  triggerLabel.append(triggerName, triggerRole);
  trigger.append(triggerLabel);

  const dropdown = document.createElement("div");
  dropdown.className = "account-dropdown";
  dropdown.setAttribute("role", "menu");
  dropdown.hidden = true;

  const summary = document.createElement("div");
  summary.className = "account-summary";
  const summaryName = document.createElement("strong");
  summaryName.textContent = name;
  const summaryEmail = document.createElement("span");
  summaryEmail.textContent = email;
  summary.append(summaryName, summaryEmail);

  const profileLink = document.createElement("a");
  profileLink.href = PROFILE_PAGE;
  profileLink.className = "account-item";
  profileLink.setAttribute("role", "menuitem");
  profileLink.innerHTML = `${PROFILE_ICON}<span>My profile</span>`;

  const logoutButton = document.createElement("button");
  logoutButton.type = "button";
  logoutButton.className = "account-item danger";
  logoutButton.setAttribute("role", "menuitem");
  logoutButton.innerHTML = `${LOGOUT_ICON}<span>Logout</span>`;
  logoutButton.addEventListener("click", logoutBloodConnect);

  dropdown.append(summary, profileLink);
  if (user.role === "ADMIN") {
    const adminLink = document.createElement("a");
    adminLink.href = ADMIN_PAGE;
    adminLink.className = "account-item";
    adminLink.setAttribute("role", "menuitem");
    adminLink.innerHTML = `${ADMIN_ICON}<span>Admin dashboard</span>`;
    dropdown.append(adminLink);
  }
  dropdown.append(logoutButton);
  menu.append(trigger, dropdown);

  const setOpen = (open) => {
    dropdown.hidden = !open;
    trigger.setAttribute("aria-expanded", open ? "true" : "false");
  };

  trigger.addEventListener("click", () => setOpen(dropdown.hidden));

  if (window.matchMedia("(hover: hover)").matches) {
    let closeTimer;
    menu.addEventListener("mouseenter", () => {
      clearTimeout(closeTimer);
      setOpen(true);
    });
    menu.addEventListener("mouseleave", () => {
      closeTimer = setTimeout(() => setOpen(false), 200);
    });
  }

  if (!user.fullName && bloodConnectToken()) {
    bloodConnectApi("/api/auth/me")
      .then((me) => {
        if (!me?.fullName) return;
        localStorage.setItem("currentUser", JSON.stringify({ ...user, ...me }));
        summaryName.textContent = me.fullName;
        triggerName.textContent = me.fullName;
      })
      .catch(() => {});
  }
  document.addEventListener("click", (event) => {
    if (!menu.contains(event.target)) setOpen(false);
  });
  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape" && !dropdown.hidden) {
      setOpen(false);
      trigger.focus();
    }
  });

  return menu;
};

const showAccountMenuWhenSignedIn = () => {
  const loginLink = document.querySelector(".navbar a.login-btn");
  if (!loginLink || !bloodConnectToken()) return;
  loginLink.replaceWith(buildAccountMenu());
};

const buildBottomNav = () => {
  const items = roleNavItems()?.filter((item) => !item.topNavOnly) || [
    { href: HOME_PAGE, label: "Home", icon: NAV_ICONS.home },
    { href: "/nearby-donors", label: "Find Donor", icon: NAV_ICONS.find },
    { href: "/emergency-request", label: "Emergency", icon: NAV_ICONS.emergency, primary: true },
    { href: ALERTS_PAGE, label: "Alerts", icon: NAV_ICONS.alerts },
  ];

  const nav = document.createElement("nav");
  nav.className = "bottom-nav";
  nav.setAttribute("aria-label", "Main navigation");

  const page = currentPage();
  items.forEach((item) => {
    const link = document.createElement("a");
    link.href = item.href;
    link.innerHTML = `${item.icon}<span>${item.label}</span>`;
    if ((item.matches || [item.href]).includes(page)) link.setAttribute("aria-current", "page");
    nav.append(link);
  });

  return nav;
};

const setupNavbar = () => {
  const navbar = document.querySelector(".navbar");
  const links = navbar?.querySelector(".nav-links");
  if (!navbar || !links) return;

  const logo = navbar.querySelector(".logo");
  if (logo) {
    logo.innerHTML = `<span class="logo-mark" aria-hidden="true">${LOGO_DROP}<span class="logo-drip"></span></span><span>Blood<b>Connect</b></span>`;
    logo.setAttribute("aria-label", "BloodConnect home");
  }

  const header = navbar.closest("header");
  const markScrolled = () => header?.classList.toggle("is-scrolled", window.scrollY > 8);
  markScrolled();
  window.addEventListener("scroll", markScrolled, { passive: true });

  const roleItems = roleNavItems();
  if (roleItems) {
    links.querySelectorAll(":scope > a:not(.login-btn)").forEach((link) => link.remove());
    const account = links.querySelector(".login-btn, .account-menu");
    roleItems.forEach((item) => {
      const link = document.createElement("a");
      link.href = item.href;
      link.textContent = item.label;
      if (currentPage() === item.href) link.setAttribute("aria-current", "page");
      links.insertBefore(link, account);
    });
    addNavIcons(links);
    const actions = document.createElement("div");
    actions.className = "nav-actions";
    if (account) actions.append(account);
    navbar.append(actions);
    setupMenuToggle(navbar, links);
    document.body.append(buildBottomNav());
    return;
  }

  links.querySelectorAll(":scope > a:not(.login-btn)").forEach((link) => {
    if (link.getAttribute("href") === currentPage()) link.setAttribute("aria-current", "page");
  });

  let previousLink = links.querySelector('a[href="/emergency-request"]');
  [{ href: ALERTS_PAGE, label: "Alerts" }, { href: BLOOD_BANKS_PAGE, label: "Blood Banks" }]
    .forEach((item) => {
    const link = links.querySelector(`a[href="${item.href}"]`) || document.createElement("a");
    if (!link.href) {
      link.href = item.href;
      link.textContent = item.label;
      if (previousLink) previousLink.after(link);
      else links.insertBefore(link, links.querySelector(":scope > .login-btn, :scope > .account-menu"));
    }
    if (currentPage() === item.href) link.setAttribute("aria-current", "page");
    previousLink = link;
  });

  const homeLink = links.querySelector(`:scope > a[href="${HOME_PAGE}"]`);
  const aboutLink = links.querySelector(`:scope > a[href="${ABOUT_PAGE}"]`);
  if (homeLink && aboutLink) homeLink.after(aboutLink);

  const eligibilityLink = document.createElement("a");
  eligibilityLink.href = ELIGIBILITY_PAGE;
  eligibilityLink.textContent = ELIGIBILITY_LABEL;
  if (currentPage() === ELIGIBILITY_PAGE) eligibilityLink.setAttribute("aria-current", "page");
  const eligibilityAnchor = aboutLink || homeLink;
  if (eligibilityAnchor) eligibilityAnchor.after(eligibilityLink);
  else links.prepend(eligibilityLink);

  addNavIcons(links);
  const actions = document.createElement("div");
  actions.className = "nav-actions";
  const account = links.querySelector(".login-btn, .account-menu");
  if (account) actions.append(account);
  navbar.append(actions);
  setupMenuToggle(navbar, links);
  document.body.append(buildBottomNav());
};

const returnHereAfterLogin = (event) => {
  const link = event.target.closest("a[href]");
  if (!link || link.origin !== window.location.origin || link.pathname !== LOGIN_PAGE || link.search) return;
  link.search = `?next=${encodeURIComponent(`${currentPage()}${window.location.search}`)}`;
};

if (isAuthPage(currentPage())) {
  document.querySelector(".navbar")?.closest("header")?.remove();
} else {
  showAccountMenuWhenSignedIn();
  setupNavbar();
  if (currentPage() !== HOME_PAGE) document.addEventListener("click", returnHereAfterLogin, true);
}
