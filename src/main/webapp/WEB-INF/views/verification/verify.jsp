<%@ include file="../layout/taglibs.jspf" %>
<%--
  FR-4: the verification screen.

  Layout follows the requirement that the photograph be prominent enough for a
  rapid visual comparison (NFR Usability): the photo is the largest thing on the
  page, and the masked ID sits directly beneath it, so staff look at one place
  while holding the donor's document.

  The confirmation form posts to the DONATION feature's endpoint, not back here.
  That is what lets verification, the eligibility check and the insertion happen
  inside a single transaction, as the reliability requirement demands.
--%>
<c:set var="pageTitle" value="Verify donor" scope="request" />
<c:set var="nav" value="verify" scope="request" />
<%@ include file="../layout/header.jspf" %>

<h1 class="h3 mb-1">Verify donor &amp; record donation</h1>
<p class="text-muted">Scan the QR code on the donor's card, or type the card number.</p>

<div class="card mb-4">
  <div class="card-body">
    <form method="get" action="${ctx}/app/verify" class="row g-2 align-items-end" id="lookupForm">
      <div class="col-12 col-md-7 col-lg-5">
        <label for="cardNo" class="form-label">Card number</label>
        <input type="text" class="form-control form-control-lg font-monospace"
               id="cardNo" name="cardNo" value="${fn:escapeXml(cardNo)}"
               placeholder="BDC-2026-XXXXXXXX" autocomplete="off" autofocus
               maxlength="32" spellcheck="false">
        <div class="form-text">
          A handheld scanner types the number and presses Enter - no extra step needed.
        </div>
      </div>
      <div class="col-auto">
        <button type="submit" class="btn btn-danger btn-lg">Look up</button>
      </div>
      <c:if test="${not empty cardNo}">
        <div class="col-auto">
          <a href="${ctx}/app/verify" class="btn btn-outline-secondary btn-lg">Clear</a>
        </div>
      </c:if>
    </form>
  </div>
</div>

<c:if test="${not empty view and view.found}">
  <div class="row g-4">

    <%-- ---- identity panel: photo + masked ID ---- --%>
    <div class="col-12 col-lg-5">
      <div class="card h-100 ${view.usable ? '' : 'border-danger'}">
        <div class="card-header bg-white fw-semibold">Stored record</div>
        <div class="card-body text-center">

          <img src="${ctx}/app/donors/photo?donorId=${view.donor.donorId}"
               alt="Photograph of ${fn:escapeXml(view.donor.fullName)}"
               class="bdcms-verify-photo mb-3">

          <h2 class="h4 mb-1"><c:out value="${view.donor.fullName}" /></h2>

          <div class="mb-3">
            <span class="badge text-bg-danger fs-6"><c:out value="${view.donor.bloodGroup.label}" /></span>
            <span class="badge text-bg-light fs-6"><c:out value="${view.donor.gender.label}" /></span>
            <span class="badge text-bg-light fs-6">${view.donor.age} yrs</span>
          </div>

          <div class="bdcms-masked-id">
            <div class="small text-uppercase text-muted">National ID (masked)</div>
            <div class="fs-4 font-monospace fw-bold"><c:out value="${view.maskedId}" /></div>
            <div class="small text-muted">
              Compare against the identity document the donor is presenting.
            </div>
          </div>

          <dl class="row small text-start mt-3 mb-0">
            <dt class="col-5">Card number</dt>
            <dd class="col-7 font-monospace"><c:out value="${view.card.cardNo}" /></dd>
            <dt class="col-5">Card status</dt>
            <dd class="col-7">
              <span class="badge text-bg-${view.card.status.badgeClass}">
                <c:out value="${view.card.status.label}" />
              </span>
            </dd>
            <dt class="col-5">Issued</dt>
            <dd class="col-7">${view.card.issuedOnDisplay}</dd>
            <dt class="col-5">Contact</dt>
            <dd class="col-7"><c:out value="${view.donor.contactNo}" /></dd>
          </dl>
        </div>
        <div class="card-footer bg-white d-flex flex-wrap gap-2">
          <a href="${ctx}/app/donations/history?donorId=${view.donor.donorId}"
             class="btn btn-sm btn-outline-secondary">Donation history</a>
          <a href="${ctx}/app/cards?donorId=${view.donor.donorId}"
             class="btn btn-sm btn-outline-secondary">Cards</a>
        </div>
      </div>
    </div>

    <%-- ---- confirmation + donation details ---- --%>
    <div class="col-12 col-lg-7">
      <div class="card h-100">
        <div class="card-header bg-white fw-semibold">Confirm identity and record the donation</div>
        <div class="card-body">

          <c:if test="${not view.usable}">
            <div class="alert alert-danger mb-0">
              This card is <c:out value="${view.card.status.label}" /> and cannot be used.
              <a href="${ctx}/app/cards/issue?donorId=${view.donor.donorId}" class="alert-link">
                Issue a replacement card
              </a>
              before recording a donation.
            </div>
          </c:if>

          <c:if test="${not empty eligibility and not eligibility.eligible}">
            <div class="alert alert-warning">
              <div class="fw-semibold mb-1">Donor is not yet eligible</div>
              <c:out value="${eligibility.explanation}" />
            </div>
          </c:if>

          <c:if test="${view.usable}">
            <form method="post" action="${ctx}/app/donations/record" novalidate>
              <input type="hidden" name="_csrf" value="${csrfToken}">
              <input type="hidden" name="cardNo" value="${fn:escapeXml(view.card.cardNo)}">

              <div class="form-check bdcms-confirm-box mb-3">
                <input class="form-check-input" type="checkbox" value="on"
                       id="confirmed" name="confirmed" required>
                <label class="form-check-label" for="confirmed">
                  <strong>I have compared the photograph and the masked national ID
                  against the person and the document presented, and confirm this is
                  the registered card holder.</strong>
                </label>
              </div>

              <c:choose>
                <c:when test="${view.pinRequired}">
                  <div class="mb-3">
                    <label for="pin" class="form-label">
                      Donor PIN <span class="text-danger">*</span>
                    </label>
                    <input type="password" class="form-control bdcms-pin" id="pin" name="pin"
                           inputmode="numeric" pattern="[0-9]{4}" maxlength="4"
                           autocomplete="off" required>
                    <div class="form-text">
                      This donor chose a four-digit PIN at registration. Ask them to enter it.
                    </div>
                  </div>
                </c:when>
                <c:otherwise>
                  <p class="text-muted small">
                    This donor did not set a PIN, so visual confirmation alone applies.
                  </p>
                </c:otherwise>
              </c:choose>

              <hr>

              <div class="row g-3">
                <div class="col-12 col-md-6">
                  <label for="centre" class="form-label">Centre</label>
                  <input type="text" class="form-control" id="centre" name="centre"
                         value="${fn:escapeXml(defaultCentre)}" maxlength="120" required>
                </div>
                <div class="col-6 col-md-3">
                  <label for="volumeMl" class="form-label">Volume (ml)</label>
                  <input type="number" class="form-control" id="volumeMl" name="volumeMl"
                         value="${defaultVolumeMl}" min="100" max="700" step="10" required>
                </div>
                <div class="col-6 col-md-3">
                  <label for="donationDate" class="form-label">Date</label>
                  <input type="date" class="form-control" id="donationDate" name="donationDate"
                         value="${today}" required>
                </div>
              </div>

              <div class="alert alert-secondary small mt-3 mb-3">
                On submission the system verifies identity, checks the waiting period
                and writes the donation as a single transaction - if any step fails,
                nothing is recorded. The attempt is written to the audit log either way.
              </div>

              <button type="submit" class="btn btn-danger btn-lg w-100">
                Confirm identity &amp; record donation
              </button>
            </form>
          </c:if>
        </div>
      </div>
    </div>
  </div>
</c:if>

<c:if test="${empty view and empty cardNo}">
  <div class="card">
    <div class="card-body text-center text-muted py-5">
      <p class="mb-1">Waiting for a card.</p>
      <p class="small mb-0">
        Scan the QR code on the donor's card, or type the card number above and press
        <kbd>Enter</kbd>.
      </p>
    </div>
  </div>
</c:if>

<%@ include file="../layout/footer.jspf" %>
