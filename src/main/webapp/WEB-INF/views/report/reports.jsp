<%@ include file="../layout/taglibs.jspf" %>
<%--
  FR-7: reports.

  The table and the PDF export are rendered from the same Report object, so the
  exported file can never show something different from the screen it came from.
--%>
<fmt:message var="pageTitle" key="report.title" scope="request"/>
<c:set var="nav" value="admin" scope="request" />
<%@ include file="../layout/header.jspf" %>

<div class="d-flex flex-wrap align-items-end justify-content-between gap-3 mb-4">
  <div>
    <h1 class="bd-page-title"><c:out value="${report.title}" /></h1>
    <p class="bd-page-sub"><c:out value="${report.subtitle}" /></p>
  </div>

  <form method="get" action="${ctx}/app/reports" class="d-flex flex-wrap align-items-end gap-2">
    <input type="hidden" name="type" value="${type.slug}">
    <c:if test="${type.dateRanged}">
      <div>
        <label for="from" class="form-label"><fmt:message key="report.from"/></label>
        <input type="date" class="form-control" id="from" name="from" value="${from}" max="${today}">
      </div>
      <div>
        <label for="to" class="form-label"><fmt:message key="report.to"/></label>
        <input type="date" class="form-control" id="to" name="to" value="${to}" max="${today}">
      </div>
      <button type="submit" class="btn btn-quiet"><fmt:message key="report.applyRange"/></button>
    </c:if>
    <a class="btn btn-outline-secondary"
       href="${ctx}/app/reports?type=${type.slug}&from=${from}&to=${to}&format=pdf"><fmt:message key="report.exportPdf"/></a>
  </form>
</div>

<ul class="nav nav-tabs mb-4" style="border-bottom:1px solid var(--bd-line)">
  <c:forEach var="t" items="${types}">
    <li class="nav-item">
      <a class="nav-link ${type eq t ? 'active' : ''}"
         href="${ctx}/app/reports?type=${t.slug}&from=${from}&to=${to}"
         style="font:500 13.5px/1 var(--bd-sans);color:${type eq t ? 'var(--bd-ink)' : 'var(--bd-ink-60)'};
                border:none;border-bottom:2px solid ${type eq t ? 'var(--bd-crimson)' : 'transparent'};
                border-radius:0;padding:11px 16px">
        <fmt:message key="${t.messageKey}"/>
      </a>
    </li>
  </c:forEach>
</ul>

<div class="row g-4">
  <div class="col-12 col-xl-8">
    <div class="card">
      <div class="card-header d-flex justify-content-between align-items-baseline">
        <span><c:out value="${report.title}" /></span>
        <span style="font:400 12px/1 var(--bd-mono);color:var(--bd-ink-50)">
          <fmt:message key="report.rows"><fmt:param value="${report.rowCount}"/></fmt:message>
        </span>
      </div>
      <div class="table-responsive">
        <table class="table table-hover table-sm mb-0">
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
                <c:forEach var="cell" items="${row.cells}" varStatus="s">
                  <td class="${s.index == 0 ? 'font-monospace' : ''}"><c:out value="${cell}" /></td>
                </c:forEach>
              </tr>
            </c:forEach>
            <%-- "empty" is an EL reserved word, so report.empty cannot be read as a
                 property; test the rows directly instead. --%>
            <c:if test="${empty report.rows}">
              <tr>
                <td colspan="${fn:length(report.headers)}" class="text-center py-5"
                    style="color:var(--bd-ink-50)">
                  <fmt:message key="report.noData"/>
                </td>
              </tr>
            </c:if>
          </tbody>
        </table>
      </div>
    </div>
  </div>

  <%-- ---- rail: distribution and verification outcomes ---- --%>
  <div class="col-12 col-xl-4">
    <div class="card mb-3">
      <div class="card-header"><fmt:message key="report.bloodGroupDistribution"/></div>
      <div class="card-body d-flex flex-column gap-2">
        <c:forEach var="g" items="${bloodGroupMix}">
          <div class="bdcms-bar-row">
            <span class="bdcms-bar-label ${g.share < 25 ? 'bdcms-bar-label-muted' : ''}">
              <c:out value="${g.label}" />
            </span>
            <div class="bdcms-bar-track">
              <div class="bdcms-bar-fill ${g.share < 25 ? 'bdcms-bar-fill-3' : ''}"
                   style="width:${g.share}%"></div>
            </div>
            <span class="bdcms-bar-value">${g.count}</span>
          </div>
        </c:forEach>
        <c:if test="${empty bloodGroupMix}">
          <div style="font:400 12.5px/1.5 var(--bd-sans);color:var(--bd-ink-50)">
            <fmt:message key="dash.noDonors"/>
          </div>
        </c:if>
      </div>
    </div>

    <div class="card">
      <div class="card-header"><fmt:message key="report.verificationOutcomes"/></div>
      <div class="card-body d-flex flex-column gap-3">
        <c:forEach var="o" items="${verificationOutcomes}">
          <c:set var="tone" value="var(--bd-crimson)" />
          <c:if test="${o.value == 0}"><c:set var="tone" value="var(--bd-ink-50)" /></c:if>
          <c:if test="${o.key.success and o.value > 0}"><c:set var="tone" value="var(--bd-green)" /></c:if>
          <div class="d-flex justify-content-between align-items-baseline">
            <span style="font:400 13.5px/1 var(--bd-sans);color:var(--bd-ink-60)">
              <fmt:message key="${o.key.messageKey}"/>
            </span>
            <span class="font-monospace" style="font-weight:500;font-size:16px;color:${tone}">
              ${o.value}
            </span>
          </div>
        </c:forEach>
        <c:if test="${empty verificationOutcomes}">
          <div style="font:400 12.5px/1.5 var(--bd-sans);color:var(--bd-ink-50)">
            <fmt:message key="report.noAttempts"/>
          </div>
        </c:if>
      </div>
      <div class="card-footer" style="font:400 12.5px/1.5 var(--bd-sans);color:var(--bd-ink-50)">
        <fmt:message key="report.everyAttemptLogged"/>
        <a href="${ctx}/app/admin/verification-log"><fmt:message key="report.openAuditLog"/></a>.
      </div>
    </div>
  </div>
</div>

<c:if test="${type eq 'DONOR_ACTIVITY'}">
  <div class="mt-3" style="font:400 12.5px/1.5 var(--bd-sans);color:var(--bd-ink-50)">
    <fmt:message key="report.eligibilityNotePre"/>
    <a href="${ctx}/app/admin/settings"><fmt:message key="nav.settings"/></a><fmt:message key="report.eligibilityNotePost"/>
  </div>
</c:if>

<%@ include file="../layout/footer.jspf" %>
