const ELIG_LANGUAGE = "en";

const ELIG_QUESTIONS = [
  { key: "tattoo", field: "tattooOrPiercingDate" },
  { key: "minorSurgery", field: "minorSurgeryDate" },
  { key: "majorSurgery", field: "majorSurgeryDate" },
  { key: "fever", field: "feverRecoveryDate" },
  { key: "antibiotics", field: "antibioticsLastDoseDate" },
  { key: "malaria", field: "malariaRecoveryDate" },
  { key: "dental", field: "dentalWorkDate" },
  { key: "alcohol", field: "alcoholLast24Hours", yesNoOnly: true },
  { key: "pregnant", field: "pregnantOrBreastfeeding", yesNoOnly: true, femaleOnly: true },
];

const ELIG_TEXT = {
  en: {
    kicker: "Eligibility check",
    title: "Can I donate today?",
    lead: "Answer a few quick questions. We'll tell you if you can donate today, or the date you can.",
    basics: "About you",
    age: "Age (years)",
    weight: "Weight (kg)",
    sex: "Sex",
    choose: "Choose",
    male: "Male",
    female: "Female",
    lastDonation: "Last blood donation (leave empty if never)",
    health: "Recent health",
    yes: "Yes",
    no: "No",
    date: "Date (when it happened or ended)",
    "q.tattoo": "Got a tattoo, body piercing or acupuncture in the last 12 months?",
    "q.minorSurgery": "Had minor surgery in the last 6 months?",
    "q.majorSurgery": "Had major surgery in the last 12 months?",
    "q.fever": "Had a fever or infection in the last 2 weeks?",
    "q.antibiotics": "Taken antibiotics in the last 7 days?",
    "q.malaria": "Had malaria in the last 3 months?",
    "q.dental": "Had dental treatment or a tooth removed in the last 3 days?",
    "q.alcohol": "Had alcohol in the last 24 hours?",
    "q.pregnant": "Pregnant, breastfeeding, or delivered a baby in the last 12 months?",
    noteLabel: "Anything else? Describe it in your own words",
    notePlaceholder: "For example: I had a cold last week and I take blood pressure tablets.",
    submit: "Check my eligibility",
    checking: "Checking...",
    missingBasics: "Please enter your age, weight and sex.",
    pickDate: 'Please choose a date for every "Yes" answer.',
    resultYes: "You can donate today!",
    resultWait: "You can donate again from {date}",
    resultDoctor: "Please talk to a doctor before donating",
    reasonsTitle: "Why",
    noteSkipped: "We couldn't read your note right now, so only your answers were checked.",
    savedOk: "Saved to your profile.",
    savedWait: "Saved to your profile. You won't be matched to requests until then.",
    disclaimer: "This is guidance only. The blood bank does a short health check before every donation.",
    again: "Check again",
    findRequests: "See blood requests near me",
    until: "until {date}",
    fromNote: "from your note",
    "r.AGE_OUT_OF_RANGE": "Age must be between 18 and 65",
    "r.LOW_WEIGHT": "Weight must be at least 45 kg",
    "r.RECENT_DONATION": "Not enough time since your last donation",
    "r.TATTOO_OR_PIERCING": "Recent tattoo, piercing or acupuncture",
    "r.MINOR_SURGERY": "Recent minor surgery",
    "r.MAJOR_SURGERY": "Recent major surgery",
    "r.FEVER_OR_ILLNESS": "Recent fever or infection",
    "r.COLD_OR_FLU": "Recent cold, cough or flu",
    "r.ANTIBIOTICS": "Recent antibiotics",
    "r.ALCOHOL": "Alcohol in the last 24 hours",
    "r.PREGNANT_OR_BREASTFEEDING": "Pregnancy, breastfeeding or recent delivery",
    "r.MALARIA": "Recent malaria",
    "r.DENTAL_WORK": "Recent dental treatment",
    "r.RECENT_VACCINATION": "Recent live vaccine",
    "r.JAUNDICE": "Recent jaundice",
    "r.SERIOUS_CONDITION": "A health condition that needs a doctor's advice",
  },
};

const eligText = (key, values = {}) =>
  Object.entries(values).reduce(
    (text, [name, value]) => text.replace(`{${name}}`, value),
    ELIG_TEXT[ELIG_LANGUAGE][key] ?? key,
  );

const eligFormatDate = (isoDate) =>
  new Intl.DateTimeFormat(`${ELIG_LANGUAGE}-IN`, { day: "numeric", month: "long", year: "numeric" })
    .format(new Date(`${isoDate}T00:00:00`));

const eligTodayIso = () => {
  const now = new Date();
  return new Date(now.getTime() - now.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
};

const eligForm = document.getElementById("eligibilityForm");
const eligSex = document.getElementById("eligSex");
const eligNote = document.getElementById("eligNote");
const eligMessage = document.getElementById("eligMessage");
const eligSubmit = document.getElementById("eligSubmit");
const eligResult = document.getElementById("eligResult");

const buildEligQuestions = () => {
  const container = document.getElementById("eligQuestions");
  ELIG_QUESTIONS.forEach((question) => {
    const card = document.createElement("fieldset");
    card.className = "elig-question";
    card.dataset.question = question.key;
    if (question.femaleOnly) card.hidden = true;

    const legend = document.createElement("legend");
    legend.dataset.i18n = `q.${question.key}`;
    card.append(legend);

    const choice = document.createElement("div");
    choice.className = "elig-choice";
    ["no", "yes"].forEach((value) => {
      const option = document.createElement("label");
      const input = document.createElement("input");
      input.type = "radio";
      input.name = `elig-${question.key}`;
      input.value = value;
      input.checked = value === "no";
      const text = document.createElement("span");
      text.dataset.i18n = value;
      option.append(input, text);
      choice.append(option);
    });
    card.append(choice);

    if (!question.yesNoOnly) {
      const dateLabel = document.createElement("label");
      dateLabel.className = "elig-date";
      dateLabel.hidden = true;
      const dateText = document.createElement("span");
      dateText.dataset.i18n = "date";
      const dateInput = document.createElement("input");
      dateInput.type = "date";
      dateInput.max = eligTodayIso();
      dateLabel.append(dateText, dateInput);
      card.append(dateLabel);
    }

    choice.addEventListener("change", () => {
      const yes = choice.querySelector('input[value="yes"]').checked;
      card.classList.toggle("is-yes", yes);
      const dateLabel = card.querySelector(".elig-date");
      if (dateLabel) {
        dateLabel.hidden = !yes;
        if (yes) dateLabel.querySelector("input").focus();
      }
    });
    container.append(card);
  });
};

const applyEligText = () => {
  document.querySelectorAll("[data-i18n]").forEach((element) => {
    element.textContent = eligText(element.dataset.i18n);
  });
  document.querySelectorAll("[data-i18n-placeholder]").forEach((element) => {
    element.placeholder = eligText(element.dataset.i18nPlaceholder);
  });
};

const eligAnswer = (key) => document.querySelector(`input[name="elig-${key}"]:checked`)?.value === "yes";

const buildEligRequest = () => {
  const body = {
    language: ELIG_LANGUAGE,
    age: Number(document.getElementById("eligAge").value),
    weightKg: Number(document.getElementById("eligWeight").value),
    sex: eligSex.value,
    lastDonationDate: document.getElementById("eligLastDonation").value || null,
    note: eligNote.value.trim() || null,
  };
  let missingDate = false;
  ELIG_QUESTIONS.forEach((question) => {
    const card = document.querySelector(`.elig-question[data-question="${question.key}"]`);
    const yes = !card.hidden && eligAnswer(question.key);
    if (question.yesNoOnly) {
      body[question.field] = yes;
      return;
    }
    const date = card.querySelector(".elig-date input").value;
    if (yes && !date) missingDate = true;
    body[question.field] = yes ? date : null;
  });
  return { body, missingDate };
};

const eligElement = (tag, className, text) => {
  const element = document.createElement(tag);
  if (className) element.className = className;
  if (text !== undefined) element.textContent = text;
  return element;
};

const renderEligResult = (result) => {
  const state = result.eligible ? "is-ok" : result.seeDoctor ? "is-doctor" : "is-wait";
  eligResult.className = `elig-result ${state}`;

  const head = eligElement("div", "elig-result-head");
  head.append(
    eligElement("span", "elig-result-icon", result.eligible ? "✓" : result.seeDoctor ? "🩺" : "⏳"),
    eligElement("h2", "", result.eligible ? eligText("resultYes")
      : result.seeDoctor ? eligText("resultDoctor")
        : eligText("resultWait", { date: eligFormatDate(result.deferredUntil) })),
  );
  const parts = [head];

  if (result.reasons.length) {
    parts.push(eligElement("h3", "elig-reasons-title", eligText("reasonsTitle")));
    const list = eligElement("ul", "elig-reasons");
    result.reasons.forEach((reason) => {
      const item = document.createElement("li");
      item.append(eligElement("strong", "", eligText(`r.${reason.code}`)));
      const details = [];
      if (reason.until) details.push(eligText("until", { date: eligFormatDate(reason.until) }));
      if (reason.source === "NOTE") details.push(eligText("fromNote"));
      if (details.length) item.append(eligElement("span", "", details.join(" · ")));
      list.append(item);
    });
    parts.push(list);
  }

  const explanation = eligElement("p", "elig-explanation", result.explanation);
  explanation.lang = result.language;
  parts.push(explanation);
  if (result.noteSkipped) parts.push(eligElement("p", "elig-notice", eligText("noteSkipped")));
  if (result.savedToProfile) {
    parts.push(eligElement("p", "elig-saved", eligText(result.eligible ? "savedOk" : "savedWait")));
  }
  parts.push(eligElement("p", "elig-small", eligText("disclaimer")));

  const actions = eligElement("div", "elig-actions");
  const again = eligElement("button", "ghost-btn", eligText("again"));
  again.type = "button";
  again.addEventListener("click", () => {
    eligResult.hidden = true;
    eligForm.hidden = false;
    eligForm.scrollIntoView({ behavior: "smooth", block: "start" });
  });
  actions.append(again);
  if (result.eligible && readCurrentUser().role === "DONOR" && bloodConnectToken()) {
    const link = eligElement("a", "elig-primary-link", eligText("findRequests"));
    link.href = ALERTS_PAGE;
    actions.append(link);
  }
  parts.push(actions);

  eligResult.replaceChildren(...parts);
};

eligForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  eligMessage.textContent = "";
  const { body, missingDate } = buildEligRequest();
  if (!body.age || !body.weightKg || !body.sex) {
    eligMessage.textContent = eligText("missingBasics");
    return;
  }
  if (missingDate) {
    eligMessage.textContent = eligText("pickDate");
    return;
  }

  setButtonLoading(eligSubmit, true, eligText("checking"));
  try {
    const result = await bloodConnectApi("/api/eligibility/check", { method: "POST", body: JSON.stringify(body) });
    renderEligResult(result);
    eligForm.hidden = true;
    eligResult.hidden = false;
    eligResult.scrollIntoView({ behavior: "smooth", block: "start" });
  } catch (error) {
    eligMessage.textContent = error.message;
  } finally {
    setButtonLoading(eligSubmit, false);
    eligSubmit.textContent = eligText("submit");
  }
});

eligSex.addEventListener("change", () => {
  const pregnantCard = document.querySelector('.elig-question[data-question="pregnant"]');
  pregnantCard.hidden = eligSex.value !== "FEMALE";
});

eligNote.addEventListener("input", () => {
  document.getElementById("eligNoteCount").textContent = String(eligNote.value.length);
});

document.getElementById("eligLastDonation").max = eligTodayIso();
buildEligQuestions();
applyEligText();

if (!bloodConnectToken()) {
  lockBehindLogin(document.getElementById("eligStage"), "Log in to check if you can donate",
    "The eligibility check is for registered BloodConnect members.");
}
