<%@ include file="../layout/taglibs.jspf" %>
<%--
  FR-3: a donor's cards.

  The table is the reissue trail. Donations stay attached to the donor, not the
  card, so a superseded card losing ACTIVE never detaches any history.
--%>
<fmt:message var="pageTitle" key="common.cards" scope="request"/>
<fmt:message var="lblConfirmLost" key="card.confirmLost"/>
<fmt:message var="lblConfirmDeactivate" key="card.confirmDeactivate"/>
<c:set var="nav" value="donors" scope="request" />
<%@ include file="../layout/header.jspf" %>

<nav aria-label="breadcrumb">
  <ol class="breadcrumb">
    <li class="breadcrumb-item"><a href="${ctx}/app/donors"><fmt:message key="common.donors"/></a></li>
    <li class="breadcrumb-item">
      <a href="${ctx}/app/donors/view?donorId=${donor.donorId}"><c:out value="${donor.fullName}" /></a>
    </li>
    <li class="breadcrumb-item active" aria-current="page"><fmt:message key="common.cards"/></li>
  </ol>
</nav>

<div class="d-flex flex-wrap align-items-end justify-content-between gap-2 mb-4">
  <div>
    <h1 class="bd-page-title"><fmt:message key="card.title"/></h1>
    <p class="bd-page-sub">
      <c:out value="${donor.fullName}" /> &middot;
      <span class="font-monospace"><c:out value="${donor.nationalId}" /></span>
    </p>
  </div>
  <a href="${ctx}/app/cards/issue?donorId=${donor.donorId}" class="btn btn-danger">
    <c:choose>
      <c:when test="${empty activeCard}"><fmt:message key="card.issueFirst"/></c:when>
      <c:otherwise><fmt:message key="card.reissue"/></c:otherwise>
    </c:choose>
  </a>
</div>

<c:if test="${empty activeCard}">
  <div class="alert alert-warning">
    <fmt:message key="card.noActive"/>
  </div>
</c:if>

<div class="card">
  <div class="table-responsive">
    <table class="table table-hover mb-0">
      <thead class="table-light">
        <tr>
          <th><fmt:message key="common.cardNumber"/></th>
          <th><fmt:message key="common.status"/></th>
          <th><fmt:message key="common.issued"/></th>
          <th class="text-end"><fmt:message key="common.actions"/></th>
        </tr>
      </thead>
      <tbody>
        <c:forEach var="card" items="${cards}">
          <tr>
            <td class="font-monospace" style="font-weight:500;letter-spacing:.04em">
              <c:out value="${card.cardNo}" />
            </td>
            <td>
              <span class="badge text-bg-${card.status.badgeClass}">
                <fmt:message key="${card.status.messageKey}"/>
              </span>
            </td>
            <td class="font-monospace" style="font-size:12.5px;white-space:nowrap;color:var(--bd-ink-60)">
              ${card.issuedOnDisplay}
            </td>
            <td class="text-end text-nowrap">
              <a href="${ctx}/app/cards/print?cardNo=${card.cardNo}"
                 class="btn btn-sm btn-quiet"><fmt:message key="card.print"/></a>
              <c:if test="${card.usable}">
                <form method="post" action="${ctx}/app/cards" class="d-inline"
                      onsubmit="return confirm('${fn:escapeXml(lblConfirmLost)}');">
                  <input type="hidden" name="_csrf" value="${csrfToken}">
                  <input type="hidden" name="cardId" value="${card.cardId}">
                  <input type="hidden" name="donorId" value="${donor.donorId}">
                  <input type="hidden" name="action" value="reportLost">
                  <button type="submit" class="btn btn-sm btn-outline-danger"><fmt:message key="card.reportLost"/></button>
                </form>
                <form method="post" action="${ctx}/app/cards" class="d-inline"
                      onsubmit="return confirm('${fn:escapeXml(lblConfirmDeactivate)}');">
                  <input type="hidden" name="_csrf" value="${csrfToken}">
                  <input type="hidden" name="cardId" value="${card.cardId}">
                  <input type="hidden" name="donorId" value="${donor.donorId}">
                  <input type="hidden" name="action" value="deactivate">
                  <button type="submit" class="btn btn-sm btn-quiet"><fmt:message key="card.deactivate"/></button>
                </form>
              </c:if>
            </td>
          </tr>
        </c:forEach>
        <c:if test="${empty cards}">
          <tr>
            <td colspan="4" class="text-center py-5" style="color:var(--bd-ink-50)">
              <fmt:message key="card.none"/>
            </td>
          </tr>
        </c:if>
      </tbody>
    </table>
  </div>
  <div class="card-footer" style="font:400 12.5px/1.5 var(--bd-sans);color:var(--bd-ink-50)">
    <fmt:message key="card.oneActiveNote"/>
  </div>
</div>

<%@ include file="../layout/footer.jspf" %>
