const phoneStatus = document.querySelector("#phone-status");
let auth, verifier, confirmation, firebase;
async function start() {
  try {
    const response = await fetch("/api/resident/auth/config");
    if (!response.ok) throw Error();
    const config = await response.json();
    if (!config.configured) {
      phoneStatus.textContent =
        "Phone OTP is being connected. Your private flat code works now.";
      document.querySelector("#phone-start").disabled = true;
      return;
    }
    if (document.querySelector("#profile-view").hidden)
      await new Promise((resolve) => {
        const observer = new MutationObserver(() => {
          if (!document.querySelector("#profile-view").hidden) {
            observer.disconnect();
            resolve();
          }
        });
        observer.observe(document.querySelector("#profile-view"), {
          attributes: true,
          attributeFilter: ["hidden"],
        });
      });
    const [app, authSdk] = await Promise.all([
      import("https://www.gstatic.com/firebasejs/11.10.0/firebase-app.js"),
      import("https://www.gstatic.com/firebasejs/11.10.0/firebase-auth.js"),
    ]);
    firebase = authSdk;
    auth = firebase.getAuth(app.initializeApp(config));
    await firebase.setPersistence(auth, firebase.browserSessionPersistence);
    verifier = new firebase.RecaptchaVerifier(auth, "phone-recaptcha", {
      size: "compact",
    });
    await verifier.render();
    phoneStatus.textContent =
      "Use your phone to link this profile, or restore a profile you already linked. Firebase sends the OTP.";
  } catch {
    phoneStatus.textContent =
      "Phone sign-in couldn’t load. Check your connection and try again.";
    document.querySelector("#phone-start").disabled = true;
  }
}
document.querySelector("#phone-start-form").onsubmit = async (e) => {
  e.preventDefault();
  const button = document.querySelector("#phone-start");
  button.disabled = true;
  const number = document
    .querySelector("#phone-number")
    .value.replace(/[ ()-]/g, "");
  try {
    if (!auth || !/^\+[1-9][0-9]{7,14}$/.test(number))
      throw Error("Add your number with the country code, like +91…");
    confirmation = await firebase.signInWithPhoneNumber(auth, number, verifier);
    document.querySelector("#phone-code-form").hidden = false;
    phoneStatus.textContent = "OTP sent. Enter it below.";
  } catch (error) {
    phoneStatus.textContent =
      error.code === "auth/too-many-requests"
        ? "Too many attempts. Wait a little before retrying."
        : error.code === "auth/invalid-phone-number"
          ? "Check the phone number and country code."
          : error.code === "auth/unauthorized-domain" ||
              error.code === "auth/operation-not-allowed"
            ? "Phone login setup is incomplete. The operator needs to enable Phone sign-in and authorise this domain in Firebase."
            : error.message?.startsWith("Add your")
              ? error.message
              : "Couldn’t send the OTP. Check the captcha and try again.";
    if (verifier) {
      try {
        verifier.clear();
        verifier = new firebase.RecaptchaVerifier(auth, "phone-recaptcha", {
          size: "compact",
        });
        await verifier.render();
      } catch {}
    }
  } finally {
    button.disabled = false;
  }
};
document.querySelector("#phone-code-form").onsubmit = async (e) => {
  e.preventDefault();
  const button = e.submitter;
  button.disabled = true;
  try {
    if (!confirmation) throw Error("Request an OTP first.");
    const result = await confirmation.confirm(
      document.querySelector("#phone-code").value.trim(),
    );
    const linked = await heyhoodModuleApi("resident/auth/phone", {
      method: "POST",
      body: JSON.stringify({ idToken: await result.user.getIdToken(true) }),
    });
    heyhoodStoreResident({ ...linked.profile, token: linked.token });
    await firebase.signOut(auth);
    document.querySelector("#phone-code").value = "";
    document.querySelector("#phone-number").value = "";
    document.querySelector("#phone-code-form").hidden = true;
    confirmation = null;
    phoneStatus.textContent = "Phone verified. This device is signed in.";
    window.dispatchEvent(new Event("heyhood-profile-updated"));
  } catch (error) {
    phoneStatus.textContent =
      error.code === "auth/invalid-verification-code"
        ? "That OTP doesn’t match. Try again."
        : error.code === "auth/code-expired"
          ? "That OTP expired. Request a new one."
          : error.code
            ? "Couldn’t verify that OTP. Please retry."
            : error.message;
  } finally {
    button.disabled = false;
  }
};
start();
