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
<fmt:message var="pageTitle" key="verify.title" scope="request"/>
<fmt:message var="lblThisDonor" key="verify.thisDonor"/>
<c:set var="nav" value="verify" scope="request" />
<%@ include file="../layout/header.jspf" %>

<div class="d-flex flex-wrap align-items-end justify-content-between gap-2 mb-4">
  <div>
    <h1 class="bd-page-title"><fmt:message key="verify.title"/></h1>
    <p class="bd-page-sub"><fmt:message key="verify.subtitle"/></p>
  </div>
  <c:if test="${not empty view and view.found}">
    <div class="bd-eyebrow">
      <c:out value="${view.card.cardNo}" /> &middot;
      <fmt:message key="verify.lookedUp"><fmt:param value="${nowTime}"/></fmt:message>
    </div>
  </c:if>
</div>

<%-- ---- step strip ---- --%>
<c:set var="hasCard" value="${not empty view and view.found and view.usable}" />
<div class="bdcms-steps">
  <span class="bd-eyebrow"><fmt:message key="verify.step"/></span>
  <div class="bdcms-step ${hasCard ? 'bdcms-step-done' : 'bdcms-step-now'}">
    <span class="bdcms-step-num">1</span><fmt:message key="verify.step1"/>
  </div>
  <span class="bdcms-step-rule"></span>
  <div class="bdcms-step ${hasCard ? 'bdcms-step-now' : ''}">
    <span class="bdcms-step-num">2</span><fmt:message key="verify.step2"/>
  </div>
  <span class="bdcms-step-rule"></span>
  <div class="bdcms-step ${not empty eligibility ? 'bdcms-step-now' : ''}">
    <span class="bdcms-step-num">3</span><fmt:message key="verify.step3"/>
  </div>
  <span class="bdcms-step-rule"></span>
  <div class="bdcms-step">
    <span class="bdcms-step-num">4</span><fmt:message key="verify.step4"/>
  </div>
</div>

<%-- ---- lookup ---- --%>
<form method="get" action="${ctx}/app/verify" class="bdcms-scanbar mb-4">
  <div style="flex:none">
    <div class="bdcms-scanbar-title"><fmt:message key="verify.cardNumber"/></div>
    <div class="bdcms-scanbar-sub"><fmt:message key="verify.scannerHint"/></div>
  </div>
  <div style="flex:1;min-width:220px">
    <label for="cardNo" class="visually-hidden"><fmt:message key="verify.cardNumber"/></label>
    <input type="text" class="form-control" id="cardNo" name="cardNo"
           value="${fn:escapeXml(cardNo)}" placeholder="BDC-2026-XXXXXXXX"
           autocomplete="off" autofocus maxlength="32" spellcheck="false">
  </div>
  <button type="submit" class="btn btn-danger" style="flex:none;height:48px"><fmt:message key="verify.lookUp"/></button>
  <c:if test="${not empty cardNo}">
    <a href="${ctx}/app/verify" class="btn btn-quiet" style="flex:none;height:48px"><fmt:message key="verify.clear"/></a>
  </c:if>
</form>

<%-- ---- eligibility verdict, when a submission was refused ---- --%>
<c:if test="${not empty eligibility and not eligibility.eligible}">
  <div class="bdcms-verdict bdcms-verdict-refused mb-4">
    <div class="bdcms-verdict-body">
      <div class="d-flex align-items-center gap-3 flex-wrap">
        <span class="bdcms-tag bdcms-tag-refused"><fmt:message key="verify.notEligibleTag"/></span>
        <span style="font:400 13px/1 var(--bd-sans);color:rgba(23,21,15,.55)">
          <fmt:message key="verify.identityConfirmedNotEligible"/>
        </span>
      </div>

      <div class="bdcms-verdict-headline">
        <c:set var="refusedName">
          <c:out value="${view.donor.fullName}" default="${lblThisDonor}"/>
        </c:set>
        <fmt:message key="verify.mayNotDonate">
          <fmt:param value="${refusedName}"/>
        </fmt:message>
      </div>

      <div class="bdcms-metrics mb-3">
        <div class="bdcms-metric">
          <div class="bd-eyebrow"><fmt:message key="verify.lastDonation"/></div>
          <div class="bdcms-metric-value">${eligibility.lastDonationDate}</div>
        </div>
        <div class="bdcms-metric">
          <div class="bd-eyebrow"><fmt:message key="verify.daysElapsed"/></div>
          <div class="bdcms-metric-value bdcms-metric-value-bad">${eligibility.daysSinceLast}</div>
        </div>
        <div class="bdcms-metric">
          <div class="bd-eyebrow"><fmt:message key="verify.required"/></div>
          <div class="bdcms-metric-value">${eligibility.intervalDays}</div>
        </div>
        <div class="bdcms-metric">
          <div class="bd-eyebrow"><fmt:message key="verify.nextEligible"/></div>
          <div class="bdcms-metric-value">${eligibility.nextEligibleDate}</div>
        </div>
      </div>

      <c:set var="pct" value="${eligibility.daysSinceLast * 100 / eligibility.intervalDays}" />
      <div class="bdcms-progress">
        <div class="bdcms-progress-fill bdcms-progress-fill-partial"
             style="width:${pct > 100 ? 100 : pct}%"></div>
      </div>
      <div style="font:400 12.5px/1.5 var(--bd-sans);color:var(--bd-ink-60);margin-top:10px">
        <fmt:message key="${eligibility.explanationKey}">
          <fmt:param value="${eligibility.daysSinceLast}"/>
          <fmt:param value="${eligibility.lastDonationDate}"/>
          <fmt:param value="${eligibility.intervalDays}"/>
          <fmt:param value="${eligibility.nextEligibleDate}"/>
          <fmt:param value="${eligibility.daysRemaining}"/>
        </fmt:message>
      </div>
    </div>
  </div>
</c:if>

<c:if test="${not empty view and view.found}">
  <div class="row g-4">

    <%-- ---- the identity panel ---- --%>
    <div class="col-12 col-lg-5 col-xxl-4">
      <div class="bdcms-identity">
        <div class="bd-eyebrow">
          <fmt:message key="verify.onFile">
            <fmt:param value="${view.donor.registeredOnDisplay}"/>
          </fmt:message>
        </div>

        <fmt:message var="lblPhotoAlt" key="verify.photoAlt">
          <fmt:param value="${view.donor.fullName}"/>
        </fmt:message>
        <img src="${ctx}/app/donors/photo?donorId=${view.donor.donorId}"
             alt="${fn:escapeXml(lblPhotoAlt)}"
             class="bdcms-verify-photo" id="faceStored" crossorigin="anonymous">

        <div class="d-flex align-items-baseline justify-content-between gap-2">
          <div class="bdcms-identity-name"><c:out value="${view.donor.fullName}" /></div>
          <div class="bdcms-identity-group"><c:out value="${view.donor.bloodGroup.label}" /></div>
        </div>

        <div class="bdcms-identity-meta">
          <fmt:message key="${view.donor.gender.messageKey}"/> &middot;
          ${view.donor.age} <fmt:message key="common.years"/><br>
          <c:out value="${view.maskedId}" />
        </div>

        <div class="d-flex gap-2 mt-auto pt-2">
          <a href="${ctx}/app/donations/history?donorId=${view.donor.donorId}"
             class="btn btn-sm btn-quiet"
             style="border-color:rgba(239,235,227,.28);color:rgba(239,235,227,.75)"><fmt:message key="verify.history"/></a>
          <a href="${ctx}/app/cards?donorId=${view.donor.donorId}"
             class="btn btn-sm btn-quiet"
             style="border-color:rgba(239,235,227,.28);color:rgba(239,235,227,.75)"><fmt:message key="common.cards"/></a>
        </div>
      </div>
    </div>

    <%-- ---- the decision ---- --%>
    <div class="col-12 col-lg-7 col-xxl-8">

      <h2 style="font:600 26px/1.2 var(--bd-sans);letter-spacing:-.015em;margin:0 0 8px">
        <fmt:message key="verify.question"/>
      </h2>
      <p style="font:400 14.5px/1.55 var(--bd-sans);color:var(--bd-ink-60);max-width:560px">
        <fmt:message key="verify.instruction"/>
      </p>

      <div class="row g-3 mb-4">
        <div class="col-12 col-md-6">
          <div class="bdcms-factbox">
            <div class="bd-eyebrow"><fmt:message key="verify.maskedId"/></div>
            <div class="bdcms-masked-id my-2"><c:out value="${view.maskedId}" /></div>
            <div style="font:400 12px/1.4 var(--bd-sans);color:var(--bd-ink-50)">
              <fmt:message key="verify.maskedNote"/>
            </div>
          </div>
        </div>
        <div class="col-12 col-md-6">
          <div class="bdcms-factbox">
            <div class="bd-eyebrow"><fmt:message key="verify.cardStatus"/></div>
            <div class="d-flex align-items-center gap-2 my-2">
              <span class="bdcms-dot ${view.usable ? 'bdcms-dot-green' : 'bdcms-dot-red'}"></span>
              <span style="font:600 19px/1 var(--bd-sans)">
                <fmt:message key="${view.card.status.messageKey}"/>
              </span>
            </div>
            <div style="font:400 12px/1.4 var(--bd-sans);color:var(--bd-ink-50)">
              <fmt:message key="common.issued"/> ${view.card.issuedOnDisplay}
            </div>
          </div>
        </div>
      </div>

      <c:choose>
        <c:when test="${not view.usable}">
          <div class="alert alert-danger">
            <fmt:message var="lblCardStatus" key="${view.card.status.messageKey}"/>
            <fmt:message key="verify.cardUnusable">
              <fmt:param value="${lblCardStatus}"/>
            </fmt:message>
            <a href="${ctx}/app/cards/issue?donorId=${view.donor.donorId}" class="alert-link">
              <fmt:message key="verify.issueReplacement"/></a>
            <fmt:message key="verify.beforeRecording"/>
          </div>
        </c:when>

        <c:otherwise>
          <form method="post" action="${ctx}/app/donations/record" novalidate>
            <input type="hidden" name="_csrf" value="${csrfToken}">
            <input type="hidden" name="cardNo" value="${fn:escapeXml(view.card.cardNo)}">

            <%-- Assisted face check. Advisory only: it never ticks the box
                 below, and the server records its reading without acting on
                 it. See assets/js/bdcms-face.js. --%>
            <div class="bdcms-face" id="faceCheck">
              <div class="d-flex align-items-center justify-content-between gap-2 flex-wrap">
                <div class="d-flex align-items-center gap-2">
                  <svg class="ic" aria-hidden="true" style="color:var(--bd-crimson)"><use href="#i-camera"/></svg>
                  <span style="font:600 14px/1 var(--bd-sans)"><fmt:message key="face.title"/></span>
                  <span class="bdcms-face-tag"><fmt:message key="face.tag"/></span>
                </div>
                <div>
                  <button type="button" class="btn btn-sm btn-quiet" id="faceStart"><fmt:message key="face.compare"/></button>
                  <button type="button" class="btn btn-sm btn-quiet" id="faceStop" hidden><fmt:message key="face.stop"/></button>
                </div>
              </div>

              <div id="faceStage" hidden class="mt-3">
                <video id="faceVideo" playsinline muted></video>
                <div class="bdcms-scan-status" id="faceStatus"></div>
              </div>

              <div class="bdcms-face-result" id="faceResult" hidden></div>

              <div class="bdcms-face-caveat">
                <fmt:message key="face.caveat"/>
              </div>
            </div>

            <input type="hidden" name="faceScore" id="faceScore" value="">

            <div class="bdcms-confirm-box mb-3">
              <div class="form-check d-flex gap-2">
                <input class="form-check-input" type="checkbox" value="on"
                       id="confirmed" name="confirmed" required>
                <label class="form-check-label" for="confirmed">
                  <fmt:message key="verify.confirmLabel"/>
                </label>
              </div>
            </div>

            <%-- The optional second factor from Section 10 of the proposal.
                 Only asked for when this donor set one at registration. --%>
            <c:if test="${view.pinRequired}">
              <div class="bdcms-confirm-box mb-3">
                <div class="d-flex align-items-baseline gap-2 mb-2">
                  <span style="font:600 14px/1 var(--bd-sans)"><fmt:message key="verify.donorPin"/></span>
                  <span style="font:400 12.5px/1 var(--bd-sans);color:var(--bd-ink-50)">
                    <fmt:message key="verify.pinNote"/>
                  </span>
                </div>
                <label for="pin" class="visually-hidden"><fmt:message key="verify.donorPin"/></label>
                <input type="password" class="form-control bdcms-pin" id="pin" name="pin"
                       inputmode="numeric" pattern="[0-9]{4}" maxlength="4"
                       autocomplete="off" required>
              </div>
            </c:if>

            <div class="row g-3 mb-3">
              <div class="col-12 col-md-5">
                <label for="centre" class="form-label"><fmt:message key="common.centreLabel"/></label>
                <input type="text" class="form-control" id="centre" name="centre"
                       value="${fn:escapeXml(defaultCentre)}" maxlength="120" required>
              </div>
              <div class="col-6 col-md-3">
                <label for="volumeMl" class="form-label"><fmt:message key="verify.volume"/></label>
                <input type="number" class="form-control" id="volumeMl" name="volumeMl"
                       value="${defaultVolumeMl}" min="100" max="700" step="10" required>
              </div>
              <div class="col-6 col-md-4">
                <label for="donationDate" class="form-label"><fmt:message key="common.date"/></label>
                <input type="date" class="form-control" id="donationDate" name="donationDate"
                       value="${today}" max="${today}" required>
              </div>
            </div>

            <button type="submit" class="btn btn-danger btn-lg w-100 mb-3">
              <fmt:message key="verify.submit"/>
            </button>

            <div class="d-flex gap-2 align-items-start"
                 style="padding:13px 15px;background:var(--bd-wash);border-radius:var(--bd-r)">
              <span class="bdcms-dot bdcms-dot-grey" style="width:5px;height:5px;margin-top:7px"></span>
              <div style="font:400 12.5px/1.5 var(--bd-sans);color:var(--bd-ink-60)">
                <fmt:message key="verify.transactionNote"/>
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
      <p class="mb-1" style="font:500 14px/1 var(--bd-sans)"><fmt:message key="verify.waiting"/></p>
      <p class="mb-0" style="font:400 12.5px/1.5 var(--bd-sans)">
        <fmt:message key="verify.waitingHint"/>
      </p>
    </div>
  </div>
</c:if>

<%@ include file="../layout/footer.jspf" %>
