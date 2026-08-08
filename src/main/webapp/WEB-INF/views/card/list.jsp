<%@ include file="../layout/taglibs.jspf" %>
<%--
  FR-3: a donor's cards.

  The table is the reissue trail. Note that donations stay attached to the donor,
  not to the card, so a superseded card losing its ACTIVE status never detaches
  any history.
--%>
<c:set var="pageTitle" value="Cards" scope="request" />
<c:set var="nav" value="donors" scope="request" />
<%@ include file="../layout/header.jspf" %>

<nav aria-label="breadcrumb">
  <ol class="breadcrumb">
    <li class="breadcrumb-item"><a href="${ctx}/app/donors">Donors</a></li>
    <li class="breadcrumb-item">
      <a href="${ctx}/app/donors/view?donorId=${donor.donorId}"><c:out value="${donor.fullName}" /></a>
    </li>
    <li class="breadcrumb-item active" aria-current="page">Cards</li>
  </ol>
</nav>

<div class="d-flex flex-wrap justify-content-between align-items-center mb-3 gap-2">
  <div>
    <h1 class="h3 mb-1">Donation cards</h1>
    <p class="text-muted mb-0"><c:out value="${donor.fullName}" /> &middot;
      <span class="font-monospace"><c:out value="${donor.nationalId}" /></span></p>
  </div>
  <a href="${ctx}/app/cards/issue?donorId=${donor.donorId}" class="btn btn-danger">
    <c:choose>
      <c:when test="${empty activeCard}">Issue first card</c:when>
      <c:otherwise>Reissue card</c:otherwise>
    </c:choose>
  </a>
</div>

<c:if test="${empty activeCard}">
  <div class="alert alert-warning">
    This donor has no active card, so they cannot be verified at the counter.
    Issue one before their next donation.
  </div>
</c:if>

<div class="card">
  <div class="table-responsive">
    <table class="table table-hover align-middle mb-0">
      <thead class="table-light">
        <tr>
          <th>Card number</th>
          <th>Status</th>
          <th>Issued</th>
          <th class="text-end">Actions</th>
        </tr>
      </thead>
      <tbody>
        <c:forEach var="card" items="${cards}">
          <tr class="${card.usable ? 'table-success' : ''}">
            <td class="font-monospace fw-semibold"><c:out value="${card.cardNo}" /></td>
            <td>
              <span class="badge text-bg-${card.status.badgeClass}">
                <c:out value="${card.status.label}" />
              </span>
            </td>
            <td class="small text-nowrap">${card.issuedOnDisplay}</td>
            <td class="text-end text-nowrap">
              <a href="${ctx}/app/cards/print?cardNo=${card.cardNo}"
                 class="btn btn-sm btn-outline-secondary">Print</a>
              <c:if test="${card.usable}">
                <form method="post" action="${ctx}/app/cards" class="d-inline"
                      onsubmit="return confirm('Report this card lost? It will stop working immediately.');">
                  <input type="hidden" name="_csrf" value="${csrfToken}">
                  <input type="hidden" name="cardId" value="${card.cardId}">
                  <input type="hidden" name="donorId" value="${donor.donorId}">
                  <input type="hidden" name="action" value="reportLost">
                  <button type="submit" class="btn btn-sm btn-outline-danger">Report lost</button>
                </form>
                <form method="post" action="${ctx}/app/cards" class="d-inline"
                      onsubmit="return confirm('Deactivate this card?');">
                  <input type="hidden" name="_csrf" value="${csrfToken}">
                  <input type="hidden" name="cardId" value="${card.cardId}">
                  <input type="hidden" name="donorId" value="${donor.donorId}">
                  <input type="hidden" name="action" value="deactivate">
                  <button type="submit" class="btn btn-sm btn-outline-secondary">Deactivate</button>
                </form>
              </c:if>
            </td>
          </tr>
        </c:forEach>
        <c:if test="${empty cards}">
          <tr><td colspan="4" class="text-center text-muted py-5">No card has ever been issued to this donor.</td></tr>
        </c:if>
      </tbody>
    </table>
  </div>
</div>

<%@ include file="../layout/footer.jspf" %>
