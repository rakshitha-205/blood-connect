const placeNameFor = async (latitude, longitude) => {
  const params = new URLSearchParams({ format: "jsonv2", lat: latitude, lon: longitude, zoom: "16" });
  const response = await fetch(`https://nominatim.openstreetmap.org/reverse?${params}`, {
    headers: { "Accept-Language": navigator.language || "en" },
  });
  if (!response.ok) throw new Error("Place lookup failed");
  const { address = {}, display_name: displayName = "" } = await response.json();
  const area = address.neighbourhood || address.suburb || address.quarter || address.road;
  const city = address.city || address.town || address.village || address.county;
  const parts = [area, city, address.state].filter(Boolean);
  return parts.length ? [...new Set(parts)].join(", ") : displayName.split(",").slice(0, 3).join(",").trim();
};

const coordinatesFor = async (place) => {
  const params = new URLSearchParams({ format: "jsonv2", limit: "1", q: place });
  const response = await fetch(`https://nominatim.openstreetmap.org/search?${params}`, {
    headers: { "Accept-Language": navigator.language || "en" },
  });
  if (!response.ok) throw new Error("Place lookup failed");
  const [match] = await response.json();
  if (!match) return null;
  return { latitude: Number(match.lat).toFixed(6), longitude: Number(match.lon).toFixed(6) };
};

const markCoordinatesFor = (locationField) => {
  locationField.dataset.coordinatesFor = locationField.value.trim();
};

const ensureExactLocation = async (locationInput, latitudeInput, longitudeInput) => {
  const locationField = document.getElementById(locationInput);
  const latitudeField = document.getElementById(latitudeInput);
  const longitudeField = document.getElementById(longitudeInput);
  const place = locationField.value.trim();
  const hasCoordinates = latitudeField.value && longitudeField.value;
  if (hasCoordinates && locationField.dataset.coordinatesFor === place) return;
  if (!place) throw new Error("Enter a location or use your current location.");

  let coordinates = null;
  try {
    coordinates = await coordinatesFor(place);
  } catch {
    throw new Error("Could not check that location right now. Please use your current location instead.");
  }
  if (!coordinates) {
    throw new Error("We could not find that exact place. Add the area and city, or use your current location.");
  }
  latitudeField.value = coordinates.latitude;
  longitudeField.value = coordinates.longitude;
  markCoordinatesFor(locationField);
};

const captureBloodConnectLocation = (latitudeInput, longitudeInput, messageElement, locationInput = "location") => {
  const message = document.getElementById(messageElement);
  const latitudeField = document.getElementById(latitudeInput);
  const longitudeField = document.getElementById(longitudeInput);
  const locationField = document.getElementById(locationInput);
  if (!navigator.geolocation) {
    message.textContent = "Location is not available in this browser. Please type your location.";
    return;
  }
  message.textContent = "Finding your location...";
  navigator.geolocation.getCurrentPosition(
    async ({ coords }) => {
      const latitude = coords.latitude.toFixed(6);
      const longitude = coords.longitude.toFixed(6);
      latitudeField.value = latitude;
      longitudeField.value = longitude;
      if (!locationField) {
        message.textContent = "Location added.";
        return;
      }
      message.textContent = "Location found. Looking up the area name...";
      try {
        locationField.value = await placeNameFor(latitude, longitude);
      } catch {
        locationField.value = `${latitude}, ${longitude}`;
      }
      markCoordinatesFor(locationField);
      message.textContent = "Location added. You can edit it if needed.";
    },
    () => {
      message.textContent = "Location permission was unavailable. Please type your location.";
    },
    { enableHighAccuracy: true, timeout: 10000, maximumAge: 60000 },
  );
};
