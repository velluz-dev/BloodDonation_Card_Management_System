<%@ include file="../layout/taglibs.jspf" %>
<%--
  NFR Auditability: every verification attempt, successful or failed, with the
  staff account and timestamp responsible.
--%>
<c:set var="pageTitle" value="Verification audit log" scope="request" />
<c:set var="nav" value="admin" scope="request" />
<%@ include file="../layout/header.jspf" %>

<h1 class="h3 mb-1">Verification audit log</h1>
<p class="text-muted">
  Every attempt to verify a donor card is recorded here - including the ones that
  failed, which are the ones worth reviewing.
</p>

<div class="card mb-3">
  <div class="card-body">
    <form method="get" action="${ctx}/app/admin/verification-log" class="row g-2 align-items-end">
      <div class="col-6 col-md-3">
        <label for="from" class="form-label">From</label>
        <input type="date" class="form-control" id="from" name="from" value="${from}" max="${today}">
      </div>
      <div class="col-6 col-md-3">
        <label for="to" class="form-label">To</label>
        <input type="date" class="form-control" id="to" name="to" value="${to}" max="${today}">
      </div>
      <div class="col-8 col-md-3">
        <label for="donorId" class="form-label">Donor ID (optional)</label>
        <input type="number" class="form-control" id="donorId" name="donorId" value="${donorId}">
      </div>
      <div class="col-4 col-md-auto">
        <button type="submit" class="btn btn-outline-danger w-100">Filter</button>
      </div>
      <div class="col-auto">
        <a href="${ctx}/app/admin/verification-log" class="btn btn-outline-secondary">Reset</a>
      </div>
    </form>
  </div>
</div>

<div class="card">
  <div class="table-responsive">
    <table class="table table-hover align-middle mb-0">
      <thead class="table-light">
        <tr>
          <th>When</th>
          <th>Card number</th>
          <th>Donor</th>
          <th>Result</th>
          <th>Detail</th>
          <th>Staff account</th>
        </tr>
      </thead>
      <tbody>
        <c:forEach var="e" items="${entries}">
          <tr class="${e.result.success ? '' : 'table-warning'}">
            <td class="small text-nowrap">${e.attemptedAtDisplay}</td>
            <td class="font-monospace small"><c:out value="${e.cardNo}" /></td>
            <td>
              <c:choose>
                <c:when test="${not empty e.donorId}">
                  <a href="${ctx}/app/donations/history?donorId=${e.donorId}">
                    <c:out value="${e.donorName}" />
                  </a>
                </c:when>
                <c:otherwise><span class="text-muted">unknown card</span></c:otherwise>
              </c:choose>
            </td>
            <td>
              <span class="badge text-bg-${e.result.badgeClass}"><c:out value="${e.result.label}" /></span>
            </td>
            <td class="small text-muted"><c:out value="${e.detail}" /></td>
            <td class="small"><c:out value="${e.staffName}" /></td>
          </tr>
        </c:forEach>
        <c:if test="${empty entries}">
          <tr><td colspan="6" class="text-center text-muted py-5">No attempts match this filter.</td></tr>
        </c:if>
      </tbody>
    </table>
  </div>

  <div class="card-footer bg-white d-flex justify-content-between align-items-center">
    <span class="small text-muted">Page ${page}</span>
    <div class="btn-group btn-group-sm">
      <c:if test="${page > 1}">
        <a class="btn btn-outline-secondary"
           href="${ctx}/app/admin/verification-log?page=${page - 1}&from=${from}&to=${to}&donorId=${donorId}">
          Previous
        </a>
      </c:if>
      <c:if test="${hasNextPage}">
        <a class="btn btn-outline-secondary"
           href="${ctx}/app/admin/verification-log?page=${page + 1}&from=${from}&to=${to}&donorId=${donorId}">
          Next
        </a>
      </c:if>
    </div>
  </div>
</div>

<%@ include file="../layout/footer.jspf" %>
