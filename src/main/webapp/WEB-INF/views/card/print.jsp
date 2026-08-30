<%@ include file="../layout/taglibs.jspf" %>
<%--
  FR-3: card issue and print.

  Front and back at real ID-1 size (85.6 x 54 mm), sitting on a darker stage so
  the card reads as a physical object rather than another panel in the page.

  The national ID is deliberately absent from the card: one dropped in the
  street should not hand a finder the donor's identity number. The QR encodes
  the card number and nothing else, for the same reason.
--%>
<fmt:message var="pageTitle" key="card.printTitle" scope="request"/>
<c:set var="nav" value="donors" scope="request" />
<fmt:message var="lblPrintPhoto" key="verify.photoAlt">
  <fmt:param value="${donor.fullName}"/>
</fmt:message>
<fmt:message var="lblQrAlt" key="card.qrAlt">
  <fmt:param value="${card.cardNo}"/>
</fmt:message>
<%@ include file="../layout/header.jspf" %>

<nav aria-label="breadcrumb" class="bdcms-no-print">
  <ol class="breadcrumb">
    <li class="breadcrumb-item"><a href="${ctx}/app/donors"><fmt:message key="common.donors"/></a></li>
    <li class="breadcrumb-item">
      <a href="${ctx}/app/donors/view?donorId=${donor.donorId}"><c:out value="${donor.fullName}" /></a>
    </li>
    <li class="breadcrumb-item active" aria-current="page"><fmt:message key="common.card"/></li>
  </ol>
</nav>

<c:if test="${not card.usable}">
  <div class="alert alert-warning bdcms-no-print">
    <fmt:message var="lblCardStatus" key="${card.status.messageKey}"/>
    <fmt:message key="card.unusableButPrintable">
      <fmt:param value="${lblCardStatus}"/>
    </fmt:message>
  </div>
</c:if>

<div class="row g-4">

  <%-- ---- the stage ---- --%>
  <div class="col-12 col-xl-8">
    <div class="mb-3 bdcms-no-print">
      <h1 class="bd-page-title">
        <fmt:message key="card.preview"><fmt:param value="${donor.fullName}"/></fmt:message>
      </h1>
      <p class="bd-page-sub"><fmt:message key="card.previewSub"/></p>
    </div>

    <div class="bdcms-card-stage">

      <%-- front --%>
      <div class="bdcms-card-face bdcms-card-front">
        <div class="d-flex justify-content-between align-items-start" style="padding-top:6px">
          <div>
            <div class="bdcms-card-centre"><c:out value="${centreName}" /></div>
            <div class="bdcms-card-kind"><fmt:message key="card.kind"/></div>
          </div>
          <div class="d-flex align-items-center gap-2">
            <span class="bdcms-drop" style="width:13px;height:13px" aria-hidden="true"></span>
            <span style="font:500 9px/1 var(--bd-mono);letter-spacing:.12em;color:var(--bd-ink-60)">BDCMS</span>
          </div>
        </div>

        <div class="d-flex gap-3 align-items-end">
          <img src="${ctx}/app/donors/photo?donorId=${donor.donorId}"
               alt="${fn:escapeXml(lblPrintPhoto)}" class="bdcms-card-photo">

          <div style="flex:1;min-width:0">
            <div class="bdcms-card-label"><fmt:message key="common.donor"/></div>
            <div class="bdcms-card-name"><c:out value="${donor.fullName}" /></div>

            <div class="d-flex gap-4 mt-2">
              <div>
                <div class="bdcms-card-label"><fmt:message key="donor.group"/></div>
                <div class="bdcms-card-group"><c:out value="${donor.bloodGroup.label}" /></div>
              </div>
              <div>
                <div class="bdcms-card-label"><fmt:message key="common.issued"/></div>
                <div class="font-monospace" style="font-size:13px;padding-top:5px">
                  ${card.issuedOnDisplay}
                </div>
              </div>
            </div>
          </div>

          <div class="bdcms-card-qr">
            <img src="${ctx}/app/cards/qr?cardNo=${card.cardNo}&size=300"
                 alt="${fn:escapeXml(lblQrAlt)}">
          </div>
        </div>

        <div class="bdcms-card-foot">
          <span class="bdcms-card-no"><c:out value="${card.cardNo}" /></span>
          <span style="font:400 9px/1 var(--bd-mono);color:var(--bd-ink-45)"><fmt:message key="card.validWhileActive"/></span>
        </div>
      </div>

      <%-- back --%>
      <div class="bdcms-card-face bdcms-card-back">
        <div class="bdcms-card-magstripe"></div>

        <div>
          <div class="bdcms-card-label"><fmt:message key="card.conditions"/></div>
          <div class="bdcms-card-terms mt-2">
            <fmt:message key="card.conditionsBody"/>
          </div>
        </div>

        <div class="d-flex justify-content-between align-items-end">
          <div>
            <div class="bdcms-card-label"><fmt:message key="common.centreLabel"/></div>
            <div class="font-monospace" style="font-size:12.5px;margin-top:3px">
              <c:out value="${centreName}" />
            </div>
          </div>
          <div class="text-end">
            <div class="bdcms-card-label"><fmt:message key="card.cardNoShort"/></div>
            <div class="font-monospace" style="font-size:12.5px;margin-top:3px">
              <c:out value="${card.cardNo}" />
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="mt-3 bdcms-no-print"
         style="font:400 12px/1.5 var(--bd-mono);color:var(--bd-ink-50)">
      85.6 &times; 54 mm &middot; 300 dpi &middot; PVC or 300 gsm laminated
    </div>
  </div>

  <%-- ---- issue panel ---- --%>
  <div class="col-12 col-xl-4 bdcms-no-print">
    <div class="card">
      <div class="card-header"><fmt:message key="card.issueDetails"/></div>
      <div class="card-body">
        <div class="bdcms-rail-item">
          <span><fmt:message key="common.donor"/></span><span><c:out value="${donor.fullName}" /></span>
        </div>
        <div class="bdcms-rail-item">
          <span><fmt:message key="common.cardNumber"/></span>
          <span class="font-monospace" style="font-size:12.5px"><c:out value="${card.cardNo}" /></span>
        </div>
        <div class="bdcms-rail-item">
          <span><fmt:message key="common.issued"/></span>
          <span class="font-monospace" style="font-size:12.5px">${card.issuedOnDisplay}</span>
        </div>
        <div class="bdcms-rail-item">
          <span><fmt:message key="common.status"/></span>
          <span class="badge ${card.usable ? 'text-bg-success' : 'text-bg-secondary'}">
            <fmt:message key="${card.status.messageKey}"/>
          </span>
        </div>
      </div>
      <div class="card-footer d-grid gap-2">
        <a href="${ctx}/app/cards/print?cardNo=${card.cardNo}&format=pdf"
           class="btn btn-danger" target="_blank" rel="noopener"><fmt:message key="card.openPdf"/></a>
        <button type="button" class="btn btn-quiet" onclick="window.print()"><fmt:message key="card.printPage"/></button>
        <a href="${ctx}/app/cards?donorId=${donor.donorId}" class="btn btn-quiet"><fmt:message key="card.allCards"/></a>
      </div>
    </div>

    <div class="alert alert-warning mt-3">
      <fmt:message key="card.replacementNote"/>
    </div>

    <div style="font:400 12.5px/1.5 var(--bd-sans);color:var(--bd-ink-50);margin-top:14px">
      <fmt:message key="card.qrNote"/>
    </div>
  </div>
</div>

<%@ include file="../layout/footer.jspf" %>
