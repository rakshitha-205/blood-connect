const EYE_OPEN_ICON =
  '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7S2 12 2 12Z"/><circle cx="12" cy="12" r="3"/></svg>';
const EYE_CLOSED_ICON =
  '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M3 3l18 18"/><path d="M10.6 5.1A10.8 10.8 0 0 1 12 5c6.4 0 10 7 10 7a17.6 17.6 0 0 1-3.2 4.1M6.6 6.6A17.4 17.4 0 0 0 2 12s3.6 7 10 7a9.9 9.9 0 0 0 5.4-1.6"/><path d="M9.9 9.9a3 3 0 0 0 4.2 4.2"/></svg>';

const addPasswordToggle = (input) => {
  const wrapper = document.createElement("div");
  wrapper.className = "password-field";
  input.parentNode.insertBefore(wrapper, input);
  wrapper.append(input);

  const toggle = document.createElement("button");
  toggle.type = "button";
  toggle.className = "password-toggle";

  const render = (visible) => {
    input.type = visible ? "text" : "password";
    toggle.innerHTML = visible ? EYE_CLOSED_ICON : EYE_OPEN_ICON;
    toggle.setAttribute("aria-label", visible ? "Hide password" : "Show password");
    toggle.setAttribute("aria-pressed", visible ? "true" : "false");
  };

  toggle.addEventListener("click", () => {
    render(input.type === "password");
    input.focus();
  });

  render(false);
  wrapper.append(toggle);
};

document.querySelectorAll('input[type="password"]').forEach(addPasswordToggle);
