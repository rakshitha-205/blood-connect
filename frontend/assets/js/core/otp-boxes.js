const CODE_LENGTH = 6;

const buildCodeBoxes = (form, codeInput, { autoSubmit = true } = {}) => {
  const container = form.querySelector(".otp-boxes");
  const boxes = Array.from({ length: CODE_LENGTH }, (_, index) => {
    const box = document.createElement("input");
    box.className = "otp-box";
    box.inputMode = "numeric";
    box.autocomplete = index === 0 ? "one-time-code" : "off";
    box.setAttribute("aria-label", `Digit ${index + 1} of ${CODE_LENGTH}`);
    return box;
  });
  container.replaceChildren(...boxes);

  const sync = () => {
    codeInput.value = boxes.map((box) => box.value).join("");
    boxes.forEach((box) => box.classList.toggle("is-filled", Boolean(box.value)));
    container.classList.remove("is-error");
    if (autoSubmit && codeInput.value.length === CODE_LENGTH) form.requestSubmit();
  };

  const fillFrom = (start, digits) => {
    digits.split("").forEach((digit, offset) => {
      if (boxes[start + offset]) boxes[start + offset].value = digit;
    });
    boxes[Math.min(start + digits.length, CODE_LENGTH - 1)].focus();
    sync();
  };

  boxes.forEach((box, index) => {
    box.addEventListener("focus", () => box.select());
    box.addEventListener("input", () => {
      const digits = box.value.replace(/\D/g, "").slice(0, CODE_LENGTH - index);
      box.value = "";
      if (digits) fillFrom(index, digits);
      else sync();
    });
    box.addEventListener("paste", (event) => {
      event.preventDefault();
      const digits = event.clipboardData.getData("text").replace(/\D/g, "").slice(0, CODE_LENGTH - index);
      if (digits) fillFrom(index, digits);
    });
    box.addEventListener("keydown", (event) => {
      if (event.key === "Backspace" && !box.value && index > 0) {
        event.preventDefault();
        boxes[index - 1].value = "";
        boxes[index - 1].focus();
        sync();
      } else if (event.key === "ArrowLeft" && index > 0) {
        boxes[index - 1].focus();
      } else if (event.key === "ArrowRight" && index < CODE_LENGTH - 1) {
        boxes[index + 1].focus();
      }
    });
  });

  return {
    reset: (showError = false) => {
      boxes.forEach((box) => {
        box.value = "";
      });
      sync();
      container.classList.toggle("is-error", showError);
      boxes[0].focus();
    },
    focusFirstEmpty: () => (boxes.find((box) => !box.value) || boxes[0]).focus(),
  };
};
