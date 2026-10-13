const bloodConnectApi = async (path, options = {}) => {
  const headers = new Headers(options.headers || {});
  headers.set("Content-Type", "application/json");
  if (bloodConnectToken()) {
    headers.set("Authorization", `Bearer ${bloodConnectToken()}`);
  }

  const response = await fetch(`${BLOODCONNECT_API_URL}${path}`, {
    ...options,
    headers,
  });
  const body = response.status === 204 ? null : await response.json().catch(() => null);
  if (response.status === 401 && headers.has("Authorization")) {
    sessionExpiredLogin();
  }
  if (response.status === 403 && body?.code === "VERIFICATION_REQUIRED") {
    goToVerification();
  }
  if (!response.ok) {
    throw new Error(body?.detail || body?.message || "The server could not complete the request.");
  }
  return body;
};
