const lockBehindLogin = (stage, title, text, showActions = true) => {
  const gate = stage.querySelector(".er-gate");
  gate.querySelector("h2").textContent = title;
  gate.querySelector("p").textContent = text;
  gate.querySelector(".er-gate-actions").hidden = !showActions;
  gate.hidden = false;
  stage.classList.add("is-locked");
  stage.querySelectorAll(".lock-content").forEach((content) => {
    content.inert = true;
    content.setAttribute("aria-hidden", "true");
  });
};

const formatCalendarDate = (isoDate) =>
  new Date(`${isoDate}T00:00:00`).toLocaleDateString("en-IN", { day: "numeric", month: "long", year: "numeric" });

const MIN_LOADER_MS = 700;

const withMinimumLoader = async (promise) => {
  const [result] = await Promise.all([promise, new Promise((resolve) => setTimeout(resolve, MIN_LOADER_MS))]);
  return result;
};

const setLoadingStatus = (element, text) => {
  element.classList.add("is-loading");
  element.innerHTML = '<span class="loader-spinner" aria-hidden="true"></span>';
  element.append(document.createTextNode(text));
};

const clearLoadingStatus = (element, text) => {
  element.classList.remove("is-loading");
  element.textContent = text;
};

const showDonorSkeletons = (container, count = 3) => {
  container.setAttribute("aria-busy", "true");
  container.replaceChildren(
    ...Array.from({ length: count }, () => {
      const card = document.createElement("div");
      card.className = "donor-skeleton";
      card.setAttribute("aria-hidden", "true");
      card.innerHTML =
        '<div class="skeleton-row"><span class="skeleton-line wide"></span><span class="skeleton-badge"></span></div>' +
        '<span class="skeleton-line"></span><span class="skeleton-line"></span><span class="skeleton-line short"></span>';
      return card;
    }),
  );
};

const hideDonorSkeletons = (container) => {
  container.removeAttribute("aria-busy");
  container.querySelectorAll(".donor-skeleton").forEach((card) => card.remove());
};

const PAGE_SIZE = 10;

const pagerFor = (list) => (list.nextElementSibling?.classList.contains("pager") ? list.nextElementSibling : null);

const removePager = (list) => pagerFor(list)?.remove();

const renderPaged = (list, items, renderItem, page = 1) => {
  const pageCount = Math.max(1, Math.ceil(items.length / PAGE_SIZE));
  const current = Math.min(Math.max(1, page), pageCount);
  const start = (current - 1) * PAGE_SIZE;
  const shown = items.slice(start, start + PAGE_SIZE);
  list.replaceChildren(...shown.map((item, index) => renderItem(item, start + index)));
  if (pageCount === 1) {
    removePager(list);
    return;
  }

  let pager = pagerFor(list);
  if (!pager) {
    pager = document.createElement("nav");
    pager.className = "pager";
    pager.setAttribute("aria-label", "Pages");
    list.after(pager);
  }
  const goTo = (target) => {
    renderPaged(list, items, renderItem, target);
    list.scrollIntoView({ behavior: "smooth", block: "start" });
  };
  const pageButton = (label, target, { current: isCurrent = false, ariaLabel } = {}) => {
    const button = document.createElement("button");
    button.type = "button";
    button.className = `pager-btn${isCurrent ? " is-current" : ""}`;
    button.textContent = label;
    if (ariaLabel) button.setAttribute("aria-label", ariaLabel);
    if (isCurrent) button.setAttribute("aria-current", "page");
    button.disabled = isCurrent || target < 1 || target > pageCount;
    button.addEventListener("click", () => goTo(target));
    return button;
  };

  const summary = document.createElement("span");
  summary.className = "pager-summary";
  summary.textContent = `Showing ${start + 1}–${start + shown.length} of ${items.length}`;
  const buttons = document.createElement("div");
  buttons.className = "pager-buttons";
  buttons.append(pageButton("‹ Prev", current - 1, { ariaLabel: "Previous page" }),
    ...Array.from({ length: pageCount }, (_, index) =>
      pageButton(String(index + 1), index + 1, { current: index + 1 === current, ariaLabel: `Page ${index + 1}` })),
    pageButton("Next ›", current + 1, { ariaLabel: "Next page" }));
  pager.replaceChildren(summary, buttons);
};

const setButtonLoading = (button, loading, loadingText) => {
  if (loading) {
    button.dataset.label = button.innerHTML;
    button.disabled = true;
    button.classList.add("is-loading");
    button.innerHTML = `<span class="loader-spinner" aria-hidden="true"></span>${loadingText}`;
    return;
  }
  button.disabled = false;
  button.classList.remove("is-loading");
  if (button.dataset.label) button.innerHTML = button.dataset.label;
};
