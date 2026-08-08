<%@ include file="../layout/taglibs.jspf" %>
<%--
  FR-7: the three reports.

  The table below and the PDF export are rendered from the same Report object,
  so the exported file can never show something different from the screen it was
  exported from.
--%>
<c:set var="pageTitle" value="Reports" scope="request" />
<c:set var="nav" value="admin" scope="request" />
<%@ include file="../layout/header.jspf" %>

<h1 class="h3 mb-3">Reports</h1>

<ul class="nav nav-tabs mb-3">
  <c:forEach var="t" items="${types}">
    <li class="nav-item">
      <a class="nav-link ${type eq t ? 'active' : ''}"
         href="${ctx}/app/reports?type=${t.slug}&from=${from}&to=${to}">
        <c:out value="${t.title}" />
      </a>
    </li>
  </c:forEach>
</ul>

<div class="card mb-3">
  <div class="card-body">
    <form method="get" action="${ctx}/app/reports" class="row g-2 align-items-end">
      <input type="hidden" name="type" value="${type.slug}">
      <c:if test="${type.dateRanged}">
        <div class="col-6 col-md-3">
          <label for="from" class="form-label">From</label>
          <input type="date" class="form-control" id="from" name="from" value="${from}" max="${today}">
        </div>
        <div class="col-6 col-md-3">
          <label for="to" class="form-label">To</label>
          <input type="date" class="form-control" id="to" name="to" value="${to}" max="${today}">
        </div>
        <div class="col-auto">
          <button type="submit" class="btn btn-outline-danger">Apply range</button>
        </div>
      </c:if>
      <div class="col-auto ms-auto">
        <a class="btn btn-danger"
           href="${ctx}/app/reports?type=${type.slug}&from=${from}&to=${to}&format=pdf">
          Export to PDF
        </a>
      </div>
    </form>
  </div>
</div>

<div class="card">
  <div class="card-header bg-white">
    <div class="fw-semibold"><c:out value="${report.title}" /></div>
    <div class="small text-muted"><c:out value="${report.subtitle}" /></div>
  </div>
  <div class="table-responsive">
    <table class="table table-hover table-sm align-middle mb-0">
      <thead class="table-light">
        <tr>
          <c:forEach var="h" items="${report.headers}">
            <th><c:out value="${h}" /></th>
          </c:forEach>
        </tr>
      </thead>
      <tbody>
        <c:forEach var="row" items="${report.rows}">
          <tr>
            <c:forEach var="cell" items="${row.cells}">
              <td><c:out value="${cell}" /></td>
            </c:forEach>
          </tr>
        </c:forEach>
        <%-- "empty" is an EL reserved word, so report.empty cannot be read as a
             property; test the rows directly instead. --%>
        <c:if test="${empty report.rows}">
          <tr>
            <td colspan="${fn:length(report.headers)}" class="text-center text-muted py-5">
              No data for this report.
            </td>
          </tr>
        </c:if>
      </tbody>
    </table>
  </div>
  <div class="card-footer bg-white small text-muted">
    ${report.rowCount} row(s).
    <c:if test="${type eq 'DONOR_ACTIVITY'}">
      Eligibility is evaluated against the interval currently configured in
      <a href="${ctx}/app/admin/settings">system settings</a>, not a value hard-coded
      in the report.
    </c:if>
  </div>
</div>

<%@ include file="../layout/footer.jspf" %>
