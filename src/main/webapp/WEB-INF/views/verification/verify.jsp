<%@ include file="../layout/taglibs.jspf" %>
<%--
  FR-4: the verification screen — the centre of the system.

  Layout follows the mockup. The stored photograph occupies a dark panel down
  the left at full height: a face comparison at a counter takes seconds and must
  not be guesswork, so everything else on this page is subordinate to it.

  The confirmation form posts to the DONATION feature's endpoint, not back here.
  That is deliberate: the reliability requirement says verification, eligibility
  and insertion must succeed or fail together, so all three happen inside one
  transaction, and the donation feature owns that transaction. The link between
  the two features is a form action — a URL — not an import.
--%>
<c:set var="pageTitle" value="Verify donor" scope="request" />
<c:set var="nav" value="verify" scope="request" />
<%@ include file="../layout/header.jspf" %>

<div class="d-flex flex-wrap align-items-end justify-content-between gap-2 mb-4">
  <div>
    <h1 class="bd-page-title">Verify donor &amp; record donation</h1>
    <p class="bd-page-sub">Scan the QR code on the donor's card, or type the card number.</p>
  </div>
  <c:if test="${not empty view and view.found}">
    <div class="bd-eyebrow">
      <c:out value="${view.card.cardNo}" /> &middot; looked up ${nowTime}
    </div>
  </c:if>
</div>

<%-- ---- step strip ---- --%>
<c:set var="hasCard" value="${not empty view and view.found and view.usable}" />
<div class="bdcms-steps">
  <span class="bd-eyebrow">Step</span>
  <div class="bdcms-step ${hasCard ? 'bdcms-step-done' : 'bdcms-step-now'}">
    <span class="bdcms-step-num">1</span>Card scanned
  </div>
  <span class="bdcms-step-rule"></span>
  <div class="bdcms-step ${hasCard ? 'bdcms-step-now' : ''}">
    <span class="bdcms-step-num">2</span>Confirm identity
  </div>
  <span class="bdcms-step-rule"></span>
  <div class="bdcms-step ${not empty eligibility ? 'bdcms-step-now' : ''}">
    <span class="bdcms-step-num">3</span>Eligibility
  </div>
  <span class="bdcms-step-rule"></span>
  <div class="bdcms-step">
    <span class="bdcms-step-num">4</span>Record donation
  </div>
</div>

<%-- ---- lookup ---- --%>
<form method="get" action="${ctx}/app/verify" class="bdcms-scanbar mb-4">
  <div style="flex:none">
    <div class="bdcms-scanbar-title">Card number</div>
    <div class="bdcms-scanbar-sub">A handheld scanner types it and presses Enter</div>
  </div>
  <div style="flex:1;min-width:220px">
    <label for="cardNo" class="visually-hidden">Card number</label>
    <input type="text" class="form-control" id="cardNo" name="cardNo"
           value="${fn:escapeXml(cardNo)}" placeholder="BDC-2026-XXXXXXXX"
           autocomplete="off" autofocus maxlength="32" spellcheck="false">
  </div>
  <button type="submit" class="btn btn-danger" style="flex:none;height:48px">Look up donor</button>
  <c:if test="${not empty cardNo}">
    <a href="${ctx}/app/verify" class="btn btn-quiet" style="flex:none;height:48px">Clear</a>
  </c:if>
</form>

<%-- ---- eligibility verdict, when a submission was refused ---- --%>
<c:if test="${not empty eligibility and not eligibility.eligible}">
  <div class="bdcms-verdict bdcms-verdict-refused mb-4">
    <div class="bdcms-verdict-body">
      <div class="d-flex align-items-center gap-3 flex-wrap">
        <span class="bdcms-tag bdcms-tag-refused">Not eligible</span>
        <span style="font:400 13px/1 var(--bd-sans);color:rgba(23,21,15,.55)">
          Identity was confirmed &mdash; the waiting period is not complete
        </span>
      </div>

      <div class="bdcms-verdict-headline">
        <c:out value="${view.donor.fullName}" default="This donor" /> may not donate today
      </div>

      <div class="bdcms-metrics mb-3">
        <div class="bdcms-metric">
          <div class="bd-eyebrow">Last donation</div>
          <div class="bdcms-metric-value">${eligibility.lastDonationDate}</div>
        </div>
        <div class="bdcms-metric">
          <div class="bd-eyebrow">Days elapsed</div>
          <div class="bdcms-metric-value bdcms-metric-value-bad">${eligibility.daysSinceLast}</div>
        </div>
        <div class="bdcms-metric">
          <div class="bd-eyebrow">Required</div>
          <div class="bdcms-metric-value">${eligibility.intervalDays}</div>
        </div>
        <div class="bdcms-metric">
          <div class="bd-eyebrow">Next eligible</div>
          <div class="bdcms-metric-value">${eligibility.nextEligibleDate}</div>
        </div>
      </div>

      <c:set var="pct" value="${eligibility.daysSinceLast * 100 / eligibility.intervalDays}" />
      <div class="bdcms-progress">
        <div class="bdcms-progress-fill bdcms-progress-fill-partial"
             style="width:${pct > 100 ? 100 : pct}%"></div>
      </div>
      <div style="font:400 12.5px/1.5 var(--bd-sans);color:var(--bd-ink-60);margin-top:10px">
        <c:out value="${eligibility.explanation}" />
      </div>
    </div>
  </div>
</c:if>

<c:if test="${not empty view and view.found}">
  <div class="row g-4">

    <%-- ---- the identity panel ---- --%>
    <div class="col-12 col-lg-5 col-xxl-4">
      <div class="bdcms-identity">
        <div class="bd-eyebrow">On file &mdash; registered ${view.donor.registeredOnDisplay}</div>

        <img src="${ctx}/app/donors/photo?donorId=${view.donor.donorId}"
             alt="Photograph of ${fn:escapeXml(view.donor.fullName)}"
             class="bdcms-verify-photo">

        <div class="d-flex align-items-baseline justify-content-between gap-2">
          <div class="bdcms-identity-name"><c:out value="${view.donor.fullName}" /></div>
          <div class="bdcms-identity-group"><c:out value="${view.donor.bloodGroup.label}" /></div>
        </div>

        <div class="bdcms-identity-meta">
          <c:out value="${view.donor.gender.label}" /> &middot; ${view.donor.age} yrs<br>
          <c:out value="${view.maskedId}" />
        </div>

        <div class="d-flex gap-2 mt-auto pt-2">
          <a href="${ctx}/app/donations/history?donorId=${view.donor.donorId}"
             class="btn btn-sm btn-quiet"
             style="border-color:rgba(239,235,227,.28);color:rgba(239,235,227,.75)">History</a>
          <a href="${ctx}/app/cards?donorId=${view.donor.donorId}"
             class="btn btn-sm btn-quiet"
             style="border-color:rgba(239,235,227,.28);color:rgba(239,235,227,.75)">Cards</a>
        </div>
      </div>
    </div>

    <%-- ---- the decision ---- --%>
    <div class="col-12 col-lg-7 col-xxl-8">

      <h2 style="font:600 26px/1.2 var(--bd-sans);letter-spacing:-.015em;margin:0 0 8px">
        Does the person in front of you match this record?
      </h2>
      <p style="font:400 14.5px/1.55 var(--bd-sans);color:var(--bd-ink-60);max-width:560px">
        Compare the photograph and the masked ID against the identity document
        presented. The donation cannot be recorded until you confirm.
      </p>

      <div class="row g-3 mb-4">
        <div class="col-12 col-md-6">
          <div class="bdcms-factbox">
            <div class="bd-eyebrow">Masked national ID</div>
            <div class="bdcms-masked-id my-2"><c:out value="${view.maskedId}" /></div>
            <div style="font:400 12px/1.4 var(--bd-sans);color:var(--bd-ink-50)">
              First and last four shown only
            </div>
          </div>
        </div>
        <div class="col-12 col-md-6">
          <div class="bdcms-factbox">
            <div class="bd-eyebrow">Card status</div>
            <div class="d-flex align-items-center gap-2 my-2">
              <span class="bdcms-dot ${view.usable ? 'bdcms-dot-green' : 'bdcms-dot-red'}"></span>
              <span style="font:600 19px/1 var(--bd-sans)">
                <c:out value="${view.card.status.label}" />
              </span>
            </div>
            <div style="font:400 12px/1.4 var(--bd-sans);color:var(--bd-ink-50)">
              Issued ${view.card.issuedOnDisplay}
            </div>
          </div>
        </div>
      </div>

      <c:choose>
        <c:when test="${not view.usable}">
          <div class="alert alert-danger">
            This card is <c:out value="${view.card.status.label}" /> and cannot be used.
            <a href="${ctx}/app/cards/issue?donorId=${view.donor.donorId}" class="alert-link">
              Issue a replacement card</a>
            before recording a donation. The donor's history is unaffected.
          </div>
        </c:when>

        <c:otherwise>
          <form method="post" action="${ctx}/app/donations/record" novalidate>
            <input type="hidden" name="_csrf" value="${csrfToken}">
            <input type="hidden" name="cardNo" value="${fn:escapeXml(view.card.cardNo)}">

            <div class="bdcms-confirm-box mb-3">
              <div class="form-check d-flex gap-2">
                <input class="form-check-input" type="checkbox" value="on"
                       id="confirmed" name="confirmed" required>
                <label class="form-check-label" for="confirmed">
                  I have compared the photograph and the masked national ID against
                  the person and the document presented, and confirm this is the
                  registered card holder.
                </label>
              </div>
            </div>

            <%-- The optional second factor from Section 10 of the proposal.
                 Only asked for when this donor set one at registration. --%>
            <c:if test="${view.pinRequired}">
              <div class="bdcms-confirm-box mb-3">
                <div class="d-flex align-items-baseline gap-2 mb-2">
                  <span style="font:600 14px/1 var(--bd-sans)">Donor PIN</span>
                  <span style="font:400 12.5px/1 var(--bd-sans);color:var(--bd-ink-50)">
                    Second factor &middot; returning donor
                  </span>
                </div>
                <label for="pin" class="visually-hidden">Donor PIN</label>
                <input type="password" class="form-control bdcms-pin" id="pin" name="pin"
                       inputmode="numeric" pattern="[0-9]{4}" maxlength="4"
                       autocomplete="off" required>
              </div>
            </c:if>

            <div class="row g-3 mb-3">
              <div class="col-12 col-md-5">
                <label for="centre" class="form-label">Centre</label>
                <input type="text" class="form-control" id="centre" name="centre"
                       value="${fn:escapeXml(defaultCentre)}" maxlength="120" required>
              </div>
              <div class="col-6 col-md-3">
                <label for="volumeMl" class="form-label">Volume collected (ml)</label>
                <input type="number" class="form-control" id="volumeMl" name="volumeMl"
                       value="${defaultVolumeMl}" min="100" max="700" step="10" required>
              </div>
              <div class="col-6 col-md-4">
                <label for="donationDate" class="form-label">Date</label>
                <input type="date" class="form-control" id="donationDate" name="donationDate"
                       value="${today}" max="${today}" required>
              </div>
            </div>

            <button type="submit" class="btn btn-danger btn-lg w-100 mb-3">
              Confirm identity &amp; record donation
            </button>

            <div class="d-flex gap-2 align-items-start"
                 style="padding:13px 15px;background:var(--bd-wash);border-radius:var(--bd-r)">
              <span class="bdcms-dot bdcms-dot-grey" style="width:5px;height:5px;margin-top:7px"></span>
              <div style="font:400 12.5px/1.5 var(--bd-sans);color:var(--bd-ink-60)">
                Either outcome writes to <span class="font-monospace">verification_log</span>
                with your account and the timestamp. Refusals are logged the same way
                as approvals. Verification, the eligibility check and the insert run
                as one transaction &mdash; if any step fails, nothing is recorded.
              </div>
            </div>
          </form>
        </c:otherwise>
      </c:choose>
    </div>
  </div>
</c:if>

<c:if test="${empty view and empty cardNo}">
  <div class="card">
    <div class="card-body text-center py-5" style="color:var(--bd-ink-50)">
      <p class="mb-1" style="font:500 14px/1 var(--bd-sans)">Waiting for a card.</p>
      <p class="mb-0" style="font:400 12.5px/1.5 var(--bd-sans)">
        Scan the QR code on the donor's card, or type the card number above and press
        <kbd>Enter</kbd>.
      </p>
    </div>
  </div>
</c:if>

<%@ include file="../layout/footer.jspf" %>
