<%@ include file="../layout/taglibs.jspf" %>
<%-- Landing page: a read-only summary drawn from every feature. --%>
<c:set var="pageTitle" value="Dashboard" scope="request" />
<c:set var="nav" value="dashboard" scope="request" />
<%@ include file="../layout/header.jspf" %>

<div class="d-flex flex-wrap justify-content-between align-items-center mb-4 gap-2">
  <div>
    <h1 class="h3 mb-1">Dashboard</h1>
    <p class="text-muted mb-0">Welcome back, <c:out value="${currentUser.fullName}" />.</p>
  </div>
  <a href="${ctx}/app/verify" class="btn btn-danger">
    Verify a donor &amp; record a donation
  </a>
</div>

<div class="row g-3 mb-4">
  <div class="col-6 col-lg-3">
    <div class="card bdcms-stat h-100">
      <div class="card-body">
        <div class="bdcms-stat-value">${donorCount}</div>
        <div class="bdcms-stat-label">Registered donors</div>
      </div>
    </div>
  </div>
  <div class="col-6 col-lg-3">
    <div class="card bdcms-stat h-100">
      <div class="card-body">
        <div class="bdcms-stat-value">${activeCardCount}</div>
        <div class="bdcms-stat-label">Active cards</div>
      </div>
    </div>
  </div>
  <div class="col-6 col-lg-3">
    <div class="card bdcms-stat h-100">
      <div class="card-body">
        <div class="bdcms-stat-value">${donationsThisMonth}</div>
        <div class="bdcms-stat-label">Donations this month</div>
      </div>
    </div>
  </div>
  <div class="col-6 col-lg-3">
    <div class="card bdcms-stat h-100 ${failedVerifications > 0 ? 'bdcms-stat-warn' : ''}">
      <div class="card-body">
        <div class="bdcms-stat-value">${failedVerifications}</div>
        <div class="bdcms-stat-label">Failed verifications (7 days)</div>
      </div>
    </div>
  </div>
</div>

<div class="row g-4">
  <div class="col-12 col-xl-7">
    <div class="card h-100">
      <div class="card-header bg-white fw-semibold">Recent donations</div>
      <div class="table-responsive">
        <table class="table table-sm table-hover align-middle mb-0">
          <thead class="table-light">
            <tr>
              <th>Date</th><th>Donor</th><th>Group</th><th>Card</th><th>Verified by</th>
            </tr>
          </thead>
          <tbody>
            <c:forEach var="d" items="${recentDonations}">
              <tr>
                <td class="text-nowrap">${d.donationDateDisplay}</td>
                <td>
                  <a href="${ctx}/app/donations/history?donorId=${d.donorId}">
                    <c:out value="${d.donorName}" />
                  </a>
                </td>
                <td><span class="badge text-bg-light"><c:out value="${d.bloodGroup}" /></span></td>
                <td class="font-monospace small"><c:out value="${d.cardNo}" /></td>
                <td class="small"><c:out value="${d.verifiedByName}" /></td>
              </tr>
            </c:forEach>
            <c:if test="${empty recentDonations}">
              <tr><td colspan="5" class="text-center text-muted py-4">No donations recorded yet.</td></tr>
            </c:if>
          </tbody>
        </table>
      </div>
    </div>
  </div>

  <div class="col-12 col-xl-5">
    <div class="card h-100">
      <div class="card-header bg-white fw-semibold d-flex justify-content-between align-items-center">
        <span>Recent verification attempts</span>
        <c:if test="${currentUser.admin}">
          <a class="small" href="${ctx}/app/admin/verification-log">Full audit log</a>
        </c:if>
      </div>
      <div class="table-responsive">
        <table class="table table-sm table-hover align-middle mb-0">
          <thead class="table-light">
            <tr><th>When</th><th>Card</th><th>Result</th><th>Staff</th></tr>
          </thead>
          <tbody>
            <c:forEach var="a" items="${recentAttempts}">
              <tr>
                <td class="small text-nowrap">${a.attemptedAtDisplay}</td>
                <td class="font-monospace small"><c:out value="${a.cardNo}" /></td>
                <td>
                  <span class="badge text-bg-${a.result.badgeClass}">
                    <c:out value="${a.result.label}" />
                  </span>
                </td>
                <td class="small"><c:out value="${a.staffName}" /></td>
              </tr>
            </c:forEach>
            <c:if test="${empty recentAttempts}">
              <tr><td colspan="4" class="text-center text-muted py-4">No verification attempts yet.</td></tr>
            </c:if>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</div>

<%@ include file="../layout/footer.jspf" %>
