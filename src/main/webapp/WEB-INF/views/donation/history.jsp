<%@ include file="../layout/taglibs.jspf" %>
<%--
  FR-6: donor record and donation history, with the FR-5 eligibility verdict
  computed from it.

  Layout follows the mockup: the donor and their cards sit on a rail down the
  left, the history and the verdict fill the rest. History belongs to the donor,
  not to the card — which is why reissuing a card never detaches a row here.
--%>
<fmt:message var="pageTitle" key="history.title" scope="request"/>
<fmt:message var="lblHistPhoto" key="verify.photoAlt">
  <fmt:param value="${donor.fullName}"/>
</fmt:message>
<c:set var="nav" value="donors" scope="request" />
<%@ include file="../layout/header.jspf" %>

<nav aria-label="breadcrumb">
  <ol class="breadcrumb">
    <li class="breadcrumb-item"><a href="${ctx}/app/donors"><fmt:message key="common.donors"/></a></li>
    <li class="breadcrumb-item">
      <a href="${ctx}/app/donors/view?donorId=${donor.donorId}"><c:out value="${donor.fullName}" /></a>
    </li>
    <li class="breadcrumb-item active" aria-current="page"><fmt:message key="history.title"/></li>
  </ol>
</nav>

<div class="row g-4">

  <%-- ---- donor rail ---- --%>
  <div class="col-12 col-lg-4 col-xxl-3">
    <div class="bdcms-rail">
      <div class="d-flex align-items-start gap-3">
        <img src="${ctx}/app/donors/photo?donorId=${donor.donorId}"
             alt="${fn:escapeXml(lblHistPhoto)}" class="bdcms-thumb">
        <div>
          <div style="font:600 21px/1.2 var(--bd-sans);letter-spacing:-.01em">
            <c:out value="${donor.fullName}" />
          </div>
          <div class="font-monospace" style="font-size:12.5px;line-height:1.6;color:rgba(23,21,15,.55);margin-top:6px">
            <c:out value="${donor.nationalId}" />
          </div>
          <span class="badge text-bg-danger mt-2" style="font-size:12px">
            <c:out value="${donor.bloodGroup.label}" />
          </span>
        </div>
      </div>

      <div class="mt-3 pt-3" style="border-top:1px solid rgba(23,21,15,.1)">
        <div class="bdcms-rail-item">
          <span><fmt:message key="common.dateOfBirth"/></span><span class="font-monospace">${donor.dateOfBirthDisplay}</span>
        </div>
        <div class="bdcms-rail-item">
          <span><fmt:message key="common.contact"/></span><span class="font-monospace"><c:out value="${donor.contactNo}" /></span>
        </div>
        <div class="bdcms-rail-item">
          <span><fmt:message key="common.registered"/></span><span class="font-monospace">${donor.registeredOnDisplay}</span>
        </div>
        <div class="bdcms-rail-item">
          <span><fmt:message key="donor.totalDonations"/></span><span class="font-monospace">${fn:length(donations)}</span>
        </div>
      </div>

      <div class="bd-eyebrow mt-3 mb-2"><fmt:message key="common.card"/></div>
      <c:choose>
        <c:when test="${not empty activeCard}">
          <div class="d-flex align-items-center justify-content-between gap-2"
               style="padding:12px 14px;background:#fff;border:1px solid var(--bd-line);border-radius:var(--bd-r)">
            <div>
              <div class="font-monospace" style="font-weight:500;font-size:13px">
                <c:out value="${activeCard.cardNo}" />
              </div>
              <div style="font:400 11.5px/1 var(--bd-sans);color:var(--bd-ink-50);margin-top:4px">
                <fmt:message key="common.issued"/> ${activeCard.issuedOnDisplay}
              </div>
            </div>
            <span class="badge text-bg-success"><fmt:message key="common.active"/></span>
          </div>
        </c:when>
        <c:otherwise>
          <div style="font:400 12.5px/1.5 var(--bd-sans);color:var(--bd-crimson)">
            <fmt:message key="history.noActiveCardNote"/>
          </div>
        </c:otherwise>
      </c:choose>

      <div class="d-grid gap-2 mt-3">
        <a href="${ctx}/app/cards?donorId=${donor.donorId}" class="btn btn-sm btn-outline-secondary">
          <fmt:message key="history.allCardsReissue"/>
        </a>
        <a href="${ctx}/app/donors/view?donorId=${donor.donorId}&edit=1" class="btn btn-sm btn-quiet">
          <fmt:message key="donor.editDetails"/>
        </a>
      </div>
    </div>
  </div>

  <%-- ---- history ---- --%>
  <div class="col-12 col-lg-8 col-xxl-9">

    <div class="d-flex flex-wrap align-items-end justify-content-between gap-2 mb-3">
      <h1 class="bd-page-title"><fmt:message key="history.title"/></h1>
      <c:if test="${eligibility.eligible and not empty activeCard}">
        <a href="${ctx}/app/verify?cardNo=${activeCard.cardNo}" class="btn btn-danger">
          <fmt:message key="history.verifyAndRecord"/>
        </a>
      </c:if>
    </div>

    <%-- ---- the verdict ---- --%>
    <div class="bdcms-verdict ${eligibility.eligible ? 'bdcms-verdict-eligible' : 'bdcms-verdict-refused'} mb-4">
      <div class="bdcms-verdict-body">
        <div class="d-flex align-items-center gap-3 flex-wrap">
          <span class="bdcms-tag ${eligibility.eligible ? 'bdcms-tag-eligible' : 'bdcms-tag-refused'}">
            <fmt:message key="${eligibility.status.messageKey}"/>
          </span>
          <span style="font:400 13px/1.4 var(--bd-sans);color:rgba(23,21,15,.55)">
            <fmt:message key="${eligibility.explanationKey}">
          <fmt:param value="${eligibility.daysSinceLast}"/>
          <fmt:param value="${eligibility.lastDonationDate}"/>
          <fmt:param value="${eligibility.intervalDays}"/>
          <fmt:param value="${eligibility.nextEligibleDate}"/>
          <fmt:param value="${eligibility.daysRemaining}"/>
        </fmt:message>
          </span>
        </div>

        <div class="bdcms-metrics mt-3">
          <div class="bdcms-metric">
            <div class="bd-eyebrow"><fmt:message key="verify.lastDonation"/></div>
            <div class="bdcms-metric-value">
              <c:choose>
                <c:when test="${eligibility.firstDonation}">&mdash;</c:when>
                <c:otherwise>${eligibility.lastDonationDate}</c:otherwise>
              </c:choose>
            </div>
          </div>
          <div class="bdcms-metric">
            <div class="bd-eyebrow"><fmt:message key="verify.daysElapsed"/></div>
            <div class="bdcms-metric-value ${eligibility.eligible ? 'bdcms-metric-value-good' : 'bdcms-metric-value-bad'}">
              <c:choose>
                <c:when test="${eligibility.firstDonation}">&mdash;</c:when>
                <c:otherwise>${eligibility.daysSinceLast}</c:otherwise>
              </c:choose>
            </div>
          </div>
          <div class="bdcms-metric">
            <div class="bd-eyebrow"><fmt:message key="verify.required"/></div>
            <div class="bdcms-metric-value">${eligibility.intervalDays}</div>
          </div>
          <div class="bdcms-metric">
            <div class="bd-eyebrow"><fmt:message key="verify.nextEligible"/></div>
            <div class="bdcms-metric-value">
              <c:choose>
                <c:when test="${eligibility.eligible}"><fmt:message key="common.today"/></c:when>
                <c:otherwise>${eligibility.nextEligibleDate}</c:otherwise>
              </c:choose>
            </div>
          </div>
        </div>

        <c:if test="${not eligibility.firstDonation}">
          <c:set var="pct" value="${eligibility.daysSinceLast * 100 / eligibility.intervalDays}" />
          <div class="bdcms-progress mt-3">
            <div class="bdcms-progress-fill ${eligibility.eligible ? '' : 'bdcms-progress-fill-partial'}"
                 style="width:${pct > 100 ? 100 : pct}%"></div>
          </div>
        </c:if>
      </div>
    </div>

    <div class="card">
      <div class="card-header d-flex justify-content-between align-items-baseline">
        <span><fmt:message key="history.allDonations"/></span>
        <span style="font:400 12px/1 var(--bd-mono);color:var(--bd-ink-50)">
          <fmt:message key="history.records">
            <fmt:param value="${fn:length(donations)}"/>
          </fmt:message>
        </span>
      </div>
      <div class="table-responsive">
        <table class="table table-hover mb-0">
          <thead class="table-light">
            <tr>
              <th><fmt:message key="common.date"/></th>
              <th><fmt:message key="common.centreLabel"/></th>
              <th><fmt:message key="common.volume"/></th>
              <th><fmt:message key="history.cardUsed"/></th>
              <th><fmt:message key="common.verifiedBy"/></th>
            </tr>
          </thead>
          <tbody>
            <c:forEach var="d" items="${donations}">
              <tr>
                <td class="font-monospace" style="white-space:nowrap;font-weight:500">
                  ${d.donationDateDisplay}
                </td>
                <td><c:out value="${d.centre}" /></td>
                <td class="font-monospace" style="font-size:12.5px">${d.volumeMl} ml</td>
                <td class="font-monospace" style="font-size:12.5px"><c:out value="${d.cardNo}" /></td>
                <td class="font-monospace" style="font-size:12.5px;color:var(--bd-ink-60)">
                  <c:out value="${d.verifiedByName}" />
                </td>
              </tr>
            </c:forEach>
            <c:if test="${empty donations}">
              <tr>
                <td colspan="5" class="text-center py-5" style="color:var(--bd-ink-50)">
                  <fmt:message key="history.none"/>
                </td>
              </tr>
            </c:if>
          </tbody>
        </table>
      </div>
      <div class="card-footer" style="font:400 12.5px/1.5 var(--bd-sans);color:var(--bd-ink-50)">
        Append-only. Records are never edited or deleted, because the eligibility
        rule is computed from them.
      </div>
    </div>
  </div>
</div>

<%@ include file="../layout/footer.jspf" %>
