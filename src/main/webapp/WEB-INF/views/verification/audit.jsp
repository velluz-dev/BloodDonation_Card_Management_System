<%@ include file="../layout/taglibs.jspf" %>
<%--
  NFR Auditability: every verification attempt, successful or failed, with the
  staff account and timestamp responsible.

  Failed rows are tinted rather than hidden behind a filter. The attempts worth
  reviewing are precisely the ones that did not go well, so they should be the
  first thing the eye lands on.
--%>
<fmt:message var="pageTitle" key="audit.title" scope="request"/>
<c:set var="nav" value="admin" scope="request" />
<%@ include file="../layout/header.jspf" %>

<div class="mb-4">
  <h1 class="bd-page-title"><fmt:message key="audit.title"/></h1>
  <p class="bd-page-sub">
    <fmt:message key="audit.subtitle"/>
  </p>
</div>

<div class="row g-4">
  <div class="col-12 col-xl-9">

    <div class="card mb-3">
      <div class="card-body">
        <form method="get" action="${ctx}/app/admin/verification-log"
              class="row g-2 align-items-end">
          <div class="col-6 col-md-3">
            <label for="from" class="form-label"><fmt:message key="report.from"/></label>
            <input type="date" class="form-control" id="from" name="from"
                   value="${from}" max="${today}">
          </div>
          <div class="col-6 col-md-3">
            <label for="to" class="form-label"><fmt:message key="report.to"/></label>
            <input type="date" class="form-control" id="to" name="to"
                   value="${to}" max="${today}">
          </div>
          <div class="col-8 col-md-3">
            <label for="donorId" class="form-label"><fmt:message key="audit.donorId"/></label>
            <input type="number" class="form-control font-monospace" id="donorId"
                   name="donorId" value="${donorId}">
          </div>
          <div class="col-4 col-md-auto">
            <button type="submit" class="btn btn-outline-secondary w-100"><fmt:message key="common.filter"/></button>
          </div>
          <c:if test="${not empty from or not empty to or not empty donorId}">
            <div class="col-auto">
              <a href="${ctx}/app/admin/verification-log" class="btn btn-quiet"><fmt:message key="common.reset"/></a>
            </div>
          </c:if>
        </form>
      </div>
    </div>

    <div class="card">
      <div class="table-responsive">
        <table class="table table-hover mb-0">
          <thead class="table-light">
            <tr>
              <th><fmt:message key="dash.when"/></th>
              <th><fmt:message key="common.cardNumber"/></th>
              <th><fmt:message key="common.donor"/></th>
              <th><fmt:message key="common.result"/></th>
              <th><fmt:message key="audit.detail"/></th>
              <th><fmt:message key="audit.staffAccount"/></th>
            </tr>
          </thead>
          <tbody>
            <c:forEach var="e" items="${entries}">
              <tr style="${e.result.success ? '' : 'background:rgba(176,30,42,.035)'}">
                <td class="font-monospace"
                    style="font-size:12.5px;white-space:nowrap;color:var(--bd-ink-60)">
                  ${e.attemptedAtDisplay}
                </td>
                <td class="font-monospace" style="font-size:12.5px">
                  <c:out value="${e.cardNo}" />
                </td>
                <td>
                  <c:choose>
                    <c:when test="${not empty e.donorId}">
                      <a href="${ctx}/app/donations/history?donorId=${e.donorId}">
                        <c:out value="${e.donorName}" />
                      </a>
                    </c:when>
                    <c:otherwise>
                      <span style="color:var(--bd-ink-50)"><fmt:message key="audit.unmatchedCard"/></span>
                    </c:otherwise>
                  </c:choose>
                </td>
                <td>
                  <span class="badge text-bg-${e.result.badgeClass}">
                    <fmt:message key="${e.result.messageKey}"/>
                  </span>
                </td>
                <td style="font-size:12.5px;color:var(--bd-ink-50)">
                  <c:out value="${e.detail}" />
                </td>
                <td class="font-monospace" style="font-size:12.5px;color:var(--bd-ink-60)">
                  <c:out value="${e.staffName}" />
                </td>
              </tr>
            </c:forEach>
            <c:if test="${empty entries}">
              <tr>
                <td colspan="6" class="text-center py-5" style="color:var(--bd-ink-50)">
                  <fmt:message key="audit.noMatch"/>
                </td>
              </tr>
            </c:if>
          </tbody>
        </table>
      </div>

      <div class="card-footer d-flex justify-content-between align-items-center">
        <span style="font:400 12.5px/1 var(--bd-sans);color:var(--bd-ink-50)">
          <fmt:message key="common.page"/> ${page}
        </span>
        <div class="d-flex gap-2">
          <c:if test="${page > 1}">
            <a class="btn btn-sm btn-quiet"
               href="${ctx}/app/admin/verification-log?page=${page - 1}&from=${from}&to=${to}&donorId=${donorId}">
              <fmt:message key="common.previous"/>
            </a>
          </c:if>
          <c:if test="${hasNextPage}">
            <a class="btn btn-sm btn-quiet"
               href="${ctx}/app/admin/verification-log?page=${page + 1}&from=${from}&to=${to}&donorId=${donorId}">
              <fmt:message key="common.next"/>
            </a>
          </c:if>
        </div>
      </div>
    </div>
  </div>

  <%-- ---- outcome tally ---- --%>
  <div class="col-12 col-xl-3">
    <div class="card">
      <div class="card-header"><fmt:message key="audit.outcomes"/></div>
      <div class="card-body d-flex flex-column gap-3">
        <c:forEach var="o" items="${outcomes}">
          <c:set var="tone" value="var(--bd-crimson)" />
          <c:if test="${o.value == 0}"><c:set var="tone" value="var(--bd-ink-50)" /></c:if>
          <c:if test="${o.key.success and o.value > 0}"><c:set var="tone" value="var(--bd-green)" /></c:if>
          <div class="d-flex justify-content-between align-items-baseline gap-2">
            <span style="font:400 13px/1.35 var(--bd-sans);color:var(--bd-ink-60)">
              <fmt:message key="${o.key.messageKey}"/>
            </span>
            <span class="font-monospace"
                  style="font-weight:500;font-size:16px;color:${tone}">${o.value}</span>
          </div>
        </c:forEach>
      </div>
      <div class="card-footer bd-eyebrow">
        ${summaryFrom} &rarr; ${summaryTo}
      </div>
    </div>

    <div class="alert alert-info mt-3">
      <fmt:message key="audit.appendOnly"/>
    </div>
  </div>
</div>

<%@ include file="../layout/footer.jspf" %>
