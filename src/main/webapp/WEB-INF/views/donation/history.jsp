<%@ include file="../layout/taglibs.jspf" %>
<%--
  FR-6: a donor's complete chronological donation history, with the current
  eligibility verdict computed from it (FR-5).
--%>
<c:set var="pageTitle" value="Donation history" scope="request" />
<c:set var="nav" value="donors" scope="request" />
<%@ include file="../layout/header.jspf" %>

<nav aria-label="breadcrumb">
  <ol class="breadcrumb">
    <li class="breadcrumb-item"><a href="${ctx}/app/donors">Donors</a></li>
    <li class="breadcrumb-item">
      <a href="${ctx}/app/donors/view?donorId=${donor.donorId}"><c:out value="${donor.fullName}" /></a>
    </li>
    <li class="breadcrumb-item active" aria-current="page">Donation history</li>
  </ol>
</nav>

<div class="row g-4">
  <div class="col-12 col-lg-4">
    <div class="card mb-3">
      <div class="card-body text-center">
        <img src="${ctx}/app/donors/photo?donorId=${donor.donorId}"
             alt="Photograph of ${fn:escapeXml(donor.fullName)}" class="bdcms-thumb mb-2">
        <h1 class="h5 mb-1"><c:out value="${donor.fullName}" /></h1>
        <span class="badge text-bg-danger"><c:out value="${donor.bloodGroup.label}" /></span>
        <div class="small text-muted mt-2">
          <c:choose>
            <c:when test="${not empty activeCard}">
              Active card <span class="font-monospace"><c:out value="${activeCard.cardNo}" /></span>
            </c:when>
            <c:otherwise>
              <span class="text-danger">No active card</span>
            </c:otherwise>
          </c:choose>
        </div>
      </div>
    </div>

    <div class="card border-${eligibility.status.badgeClass}">
      <div class="card-header bg-white fw-semibold">Current eligibility</div>
      <div class="card-body">
        <div class="mb-2">
          <span class="badge text-bg-${eligibility.status.badgeClass} fs-6">
            <c:out value="${eligibility.status.label}" />
          </span>
        </div>
        <p class="small mb-3"><c:out value="${eligibility.explanation}" /></p>

        <dl class="row small mb-0">
          <dt class="col-7">Required interval</dt>
          <dd class="col-5 text-end">${eligibility.intervalDays} days</dd>
          <c:if test="${not eligibility.firstDonation}">
            <dt class="col-7">Days since last</dt>
            <dd class="col-5 text-end">${eligibility.daysSinceLast}</dd>
          </c:if>
          <c:if test="${not eligibility.eligible}">
            <dt class="col-7">Next eligible</dt>
            <dd class="col-5 text-end">${eligibility.nextEligibleDate}</dd>
            <dt class="col-7">Days remaining</dt>
            <dd class="col-5 text-end">${eligibility.daysRemaining}</dd>
          </c:if>
        </dl>
      </div>
      <c:if test="${eligibility.eligible and not empty activeCard}">
        <div class="card-footer bg-white d-grid">
          <a href="${ctx}/app/verify?cardNo=${activeCard.cardNo}" class="btn btn-danger">
            Verify &amp; record a donation
          </a>
        </div>
      </c:if>
    </div>
  </div>

  <div class="col-12 col-lg-8">
    <div class="card">
      <div class="card-header bg-white fw-semibold d-flex justify-content-between">
        <span>Donation history</span>
        <span class="text-muted small">${fn:length(donations)} donation(s)</span>
      </div>
      <div class="table-responsive">
        <table class="table table-hover align-middle mb-0">
          <thead class="table-light">
            <tr>
              <th>Date</th><th>Centre</th><th>Volume</th><th>Card used</th><th>Verified by</th>
            </tr>
          </thead>
          <tbody>
            <c:forEach var="d" items="${donations}">
              <tr>
                <td class="text-nowrap fw-semibold">${d.donationDateDisplay}</td>
                <td><c:out value="${d.centre}" /></td>
                <td>${d.volumeMl} ml</td>
                <td class="font-monospace small"><c:out value="${d.cardNo}" /></td>
                <td class="small"><c:out value="${d.verifiedByName}" /></td>
              </tr>
            </c:forEach>
            <c:if test="${empty donations}">
              <tr>
                <td colspan="5" class="text-center text-muted py-5">
                  No donations recorded. This donor has never donated at any centre
                  using this system.
                </td>
              </tr>
            </c:if>
          </tbody>
        </table>
      </div>
      <div class="card-footer bg-white small text-muted">
        This history is append-only. Records are never edited or deleted, because
        the eligibility rule is computed from them.
      </div>
    </div>
  </div>
</div>

<%@ include file="../layout/footer.jspf" %>
