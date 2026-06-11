function selectPaymentMethod(method) {
  document.getElementById("meioPagamento").value = method;

  // Update active tabs
  document
    .querySelectorAll(".payment-card-option")
    .forEach((el) => el.classList.remove("active"));

  const cardForm = document.getElementById("card-form-wrapper");
  const pixInfo = document.getElementById("pix-info-wrapper");
  const boletoInfo = document.getElementById("boleto-info-wrapper");
  const parcelasGroup = document.getElementById("parcelas-group");

  const numCartao = document.getElementById("numCartao");
  const nomeCartao = document.getElementById("nomeCartao");
  const validadeCartao = document.getElementById("validadeCartao");
  const cvvCartao = document.getElementById("cvvCartao");

  if (method === "CARTAO_CREDITO") {
    document.getElementById("tab-credito").classList.add("active");
    cardForm.style.display = "block";
    pixInfo.style.display = "none";
    boletoInfo.style.display = "none";
    parcelasGroup.style.display = "block";

    numCartao.required = true;
    nomeCartao.required = true;
    validadeCartao.required = true;
    cvvCartao.required = true;
  } else if (method === "CARTAO_DEBITO") {
    document.getElementById("tab-debito").classList.add("active");
    cardForm.style.display = "block";
    pixInfo.style.display = "none";
    boletoInfo.style.display = "none";
    parcelasGroup.style.display = "none";

    numCartao.required = true;
    nomeCartao.required = true;
    validadeCartao.required = true;
    cvvCartao.required = true;
  } else if (method === "PIX") {
    document.getElementById("tab-pix").classList.add("active");
    cardForm.style.display = "none";
    pixInfo.style.display = "flex";
    boletoInfo.style.display = "none";

    numCartao.required = false;
    nomeCartao.required = false;
    validadeCartao.required = false;
    cvvCartao.required = false;
  } else if (method === "BOLETO") {
    document.getElementById("tab-boleto").classList.add("active");
    cardForm.style.display = "none";
    pixInfo.style.display = "none";
    boletoInfo.style.display = "flex";

    numCartao.required = false;
    nomeCartao.required = false;
    validadeCartao.required = false;
    cvvCartao.required = false;
  }
}

// Format card fields and update virtual widget dynamically
document.addEventListener("DOMContentLoaded", () => {
  const numCartao = document.getElementById("numCartao");
  const nomeCartao = document.getElementById("nomeCartao");
  const validadeCartao = document.getElementById("validadeCartao");
  const cvvCartao = document.getElementById("cvvCartao");
  const virtualCard = document.getElementById("virtual-card");

  // Card Number formatting
  numCartao.addEventListener("input", (e) => {
    let value = e.target.value.replace(/\D/g, "");
    value = value.match(/.{1,4}/g)?.join(" ") || value;
    e.target.value = value.substring(0, 19);

    document.getElementById("card-num-display").innerText =
      value || "•••• •••• •••• ••••";

    // brand detection
    const brandDisplay = document.getElementById("card-brand-display");
    if (value.startsWith("4")) {
      brandDisplay.innerText = "VISA";
    } else if (value.startsWith("5")) {
      brandDisplay.innerText = "MASTERCARD";
    } else if (value.startsWith("3")) {
      brandDisplay.innerText = "AMEX";
    } else {
      brandDisplay.innerText = "CARD";
    }
  });

  // Name formatting
  nomeCartao.addEventListener("input", (e) => {
    let value = e.target.value.toUpperCase();
    document.getElementById("card-name-display").innerText =
      value || "NOME DO TITULAR";
  });

  // Expiry formatting (MM/AA)
  validadeCartao.addEventListener("input", (e) => {
    let value = e.target.value.replace(/\D/g, "");
    if (value.length > 2) {
      value = value.substring(0, 2) + "/" + value.substring(2, 4);
    }
    e.target.value = value.substring(0, 5);
    document.getElementById("card-expiry-display").innerText =
      value || "MM/AA";
  });

  // CVV formatting
  cvvCartao.addEventListener("input", (e) => {
    let value = e.target.value.replace(/\D/g, "");
    e.target.value = value.substring(0, 4);
    document.getElementById("card-cvv-display").innerText = value || "•••";
  });

  // Card flip events
  cvvCartao.addEventListener("focus", () => {
    virtualCard.classList.add("is-flipped");
  });

  cvvCartao.addEventListener("blur", () => {
    virtualCard.classList.remove("is-flipped");
  });

  // Init select
  selectPaymentMethod("CARTAO_CREDITO");
});

// Pix copier helper
function copyPixKey() {
  const copyText = document.getElementById("pix-key-val");
  copyText.select();
  copyText.setSelectionRange(0, 99999);
  navigator.clipboard.writeText(copyText.value).then(() => {
    const copyBtn = document.getElementById("pix-copy-btn");
    copyBtn.innerText = "Copiado! ✓";
    copyBtn.style.background = "var(--success)";
    setTimeout(() => {
      copyBtn.innerText = "Copiar Código";
      copyBtn.style.background = "var(--primary)";
    }, 2500);
  });
}
