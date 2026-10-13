const WHATSAPP_ICON =
  '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 3a9 9 0 0 0-7.8 13.5L3 21l4.6-1.2A9 9 0 1 0 12 3z"/><path d="M8.8 8.6c.2-.5.5-.5.8-.5h.5c.2 0 .4 0 .6.4l.8 1.8c.1.2.1.4 0 .6l-.5.7c-.1.2-.1.4 0 .5.6 1 1.4 1.8 2.4 2.4.2.1.4.1.5 0l.7-.8c.2-.2.4-.2.6-.1l1.8.8c.3.1.4.3.4.6 0 .9-.6 1.8-1.5 2-1 .2-2.6 0-4.6-1.6-2-1.6-2.8-3.6-2.9-4.5-.1-.8.2-1.5.4-1.9z"/></svg>';
const PHONE_ICON =
  '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 4h4l2 5-2.5 1.5a11 11 0 0 0 5 5L15 13l5 2v4a2 2 0 0 1-2 2A16 16 0 0 1 3 6a2 2 0 0 1 2-2z"/></svg>';

const connectNode = (tag, className, text) => {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text != null) node.textContent = text;
  return node;
};

const phoneDigits = (phone) => String(phone || "").replace(/\D/g, "");

const whatsappUrl = (phone, message) => {
  const digits = phoneDigits(phone);
  const url = new URL(`https://wa.me/${digits.length === 10 ? `91${digits}` : digits}`);
  if (message) url.searchParams.set("text", message);
  return url.toString();
};

const telUrl = (phone) => `tel:${String(phone || "").replace(/[^\d+]/g, "")}`;

const buildContactButtons = (phone, message, { callLabel = "Call", chatLabel = "WhatsApp" } = {}) => {
  const call = connectNode("a", "contact-btn is-call");
  call.href = telUrl(phone);
  call.innerHTML = PHONE_ICON;
  call.append(connectNode("span", "", callLabel));

  const chat = connectNode("a", "contact-btn is-whatsapp");
  chat.href = whatsappUrl(phone, message);
  chat.target = "_blank";
  chat.rel = "noopener noreferrer";
  chat.innerHTML = WHATSAPP_ICON;
  chat.append(connectNode("span", "", chatLabel));
  return [chat, call];
};

const buildRatingBadge = (average, count, emptyText = "") => {
  if (!count) return emptyText ? connectNode("span", "rating-badge is-empty", emptyText) : null;
  const badge = connectNode("span", "rating-badge");
  badge.title = `${average} out of 5 from ${count} review${count === 1 ? "" : "s"}`;
  badge.append(connectNode("span", "rating-star", "★"), connectNode("strong", "", Number(average).toFixed(1)),
    connectNode("span", "rating-count", `(${count})`));
  return badge;
};

const buildStarRow = (rating) => {
  const row = connectNode("span", "star-row");
  row.setAttribute("aria-label", `${rating} out of 5 stars`);
  for (let star = 1; star <= 5; star += 1) {
    row.append(connectNode("span", star <= rating ? "is-on" : "", "★"));
  }
  return row;
};

const connectTimeAgo = (isoDate) => {
  const minutes = Math.max(0, Math.round((Date.now() - new Date(isoDate).getTime()) / 60000));
  if (minutes < 1) return "just now";
  if (minutes < 60) return `${minutes} min ago`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${hours} hr${hours === 1 ? "" : "s"} ago`;
  const days = Math.round(hours / 24);
  return `${days} day${days === 1 ? "" : "s"} ago`;
};

const buildAcceptedDonorCard = (donor, request) => {
  const card = connectNode("article", "nearby-card accepted-card");
  const top = connectNode("div", "nearby-card-top");
  const who = connectNode("div", "nearby-who");
  const place = donor.distanceKm == null ? donor.location : `${donor.distanceKm} km away · ${donor.location}`;
  who.append(connectNode("h3", "nearby-name", donor.name), connectNode("p", "nearby-place", place));
  top.append(connectNode("span", "nearby-blood", donor.bloodGroup), who,
    connectNode("span", "accepted-tag", "✓ Accepted"));

  const meta = connectNode("div", "nearby-meta");
  meta.append(connectNode("span", "nearby-pill is-available", `Accepted ${connectTimeAgo(donor.acceptedAt)}`));
  const rating = buildRatingBadge(donor.rating, donor.ratingCount, "New donor");
  if (rating) meta.append(rating);
  card.append(top, meta);

  if (request.status !== "CANCELLED") {
    const actions = connectNode("div", "alert-actions");
    actions.append(...buildContactButtons(donor.phone,
      `Hi ${donor.name}, thank you for accepting the ${request.bloodGroup} blood request for ${request.patientName} at ${request.hospital} on BloodConnect.`,
      { callLabel: "Call donor" }));
    card.append(actions);
  }
  if (request.status === "FULFILLED") {
    card.append(donor.reviewed
      ? connectNode("p", "review-done", `You rated ${donor.name}. Thank you!`)
      : buildReviewForm({ requestId: request.id, revieweeId: donor.donorUserId, name: donor.name }));
  }
  return card;
};

const RATING_WORDS = ["", "Poor", "Fair", "Good", "Great", "Excellent"];

const buildReviewForm = ({ requestId, revieweeId, name, onDone }) => {
  const form = connectNode("form", "review-form");
  form.append(connectNode("p", "review-title", `Rate ${name}`));

  let rating = 0;
  const stars = connectNode("div", "star-input");
  stars.setAttribute("role", "radiogroup");
  stars.setAttribute("aria-label", `Rating for ${name}`);
  const word = connectNode("span", "star-word", "Tap a star");
  const buttons = [1, 2, 3, 4, 5].map((value) => {
    const button = connectNode("button", "star-btn", "★");
    button.type = "button";
    button.setAttribute("role", "radio");
    button.setAttribute("aria-label", `${value} star${value === 1 ? "" : "s"}`);
    button.setAttribute("aria-checked", "false");
    button.addEventListener("mouseenter", () => paint(value));
    button.addEventListener("click", () => {
      rating = value;
      paint(value);
    });
    return button;
  });
  const paint = (value) => {
    buttons.forEach((button, index) => {
      button.classList.toggle("is-on", index < value);
      button.setAttribute("aria-checked", String(index + 1 === rating));
    });
    word.textContent = value ? RATING_WORDS[value] : "Tap a star";
  };
  stars.addEventListener("mouseleave", () => paint(rating));
  stars.append(...buttons, word);

  const comment = connectNode("textarea", "review-text");
  comment.rows = 2;
  comment.maxLength = 500;
  comment.placeholder = "Share a few words (optional)";
  comment.setAttribute("aria-label", "Review comment");

  const message = connectNode("p", "review-message");
  message.setAttribute("role", "status");
  const submit = connectNode("button", "tracking-btn is-primary", "Submit review");
  submit.type = "submit";

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (!rating) {
      message.textContent = "Choose 1 to 5 stars first.";
      return;
    }
    setButtonLoading(submit, true, "Saving...");
    try {
      await bloodConnectApi("/api/reviews", {
        method: "POST",
        body: JSON.stringify({ requestId, revieweeId, rating, comment: comment.value.trim() }),
      });
      const done = connectNode("p", "review-done");
      done.append(buildStarRow(rating), connectNode("span", "", `Thanks! You rated ${name}.`));
      form.replaceWith(done);
      if (onDone) onDone(rating);
    } catch (error) {
      message.textContent = error.message;
      setButtonLoading(submit, false);
    }
  });

  form.append(stars, comment, message, submit);
  return form;
};
