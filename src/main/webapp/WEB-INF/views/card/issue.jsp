<%@ include file="../layout/taglibs.jspf" %>
<%-- FR-3: confirm before issuing, so staff see what a reissue will supersede. --%>
<c:set var="pageTitle" value="Issue card" scope="request" />
<c:set var="nav" value="donors" scope="request" />
<%@ include file="../layout/header.jspf" %>

<div class="row justify-content-center">
  <div class="col-12 col-lg-7">

    <h1 class="bd-page-title mb-3">
      <c:choose>
        <c:when test="${empty existingCard}">Issue a donation card</c:when>
        <c:otherwise>Reissue a donation card</c:otherwise>
      </c:choose>
    </h1>

    <div class="card mb-3">
      <div class="card-body">
        <div class="row g-3 align-items-center">
          <div class="col-auto">
            <img src="${ctx}/app/donors/photo?donorId=${donor.donorId}"
                 alt="Photograph of ${fn:escapeXml(donor.fullName)}" class="bdcms-thumb">
          </div>
          <div class="col">
            <div style="font:600 18px/1.2 var(--bd-sans)"><c:out value="${donor.fullName}" /></div>
            <div style="font:400 12.5px/1.5 var(--bd-sans);color:var(--bd-ink-60);margin-top:5px">
              <span class="font-monospace"><c:out value="${donor.nationalId}" /></span> &middot;
              <c:out value="${donor.bloodGroup.label}" /> &middot; ${donor.age} yrs
            </div>
          </div>
        </div>
      </div>
    </div>

    <c:if test="${not empty existingCard}">
      <div class="alert alert-warning">
        <div class="fw-semibold mb-1">This donor already holds an active card</div>
        Card <span class="font-monospace"><c:out value="${existingCard.cardNo}" /></span>,
        issued ${existingCard.issuedOnDisplay}, will be superseded and will stop
        working the moment the new card is issued. The donor's donation history is
        attached to the donor record, so none of it is affected.
      </div>
    </c:if>

    <div class="card">
      <div class="card-body">
        <form method="post" action="${ctx}/app/cards/issue?donorId=${donor.donorId}">
          <input type="hidden" name="_csrf" value="${csrfToken}">

          <c:if test="${not empty existingCard}">
            <fieldset class="mb-3">
              <legend class="form-label fs-6">Why is a new card being issued?</legend>
              <div class="form-check">
                <input class="form-check-input" type="radio" name="reason" id="reasonLost"
                       value="lost" checked>
                <label class="form-check-label" for="reasonLost">
                  The old card was <strong>lost or stolen</strong> - mark it LOST
                </label>
              </div>
              <div class="form-check">
                <input class="form-check-input" type="radio" name="reason" id="reasonReplace"
                       value="replace">
                <label class="form-check-label" for="reasonReplace">
                  Routine replacement (damaged, worn) - mark it INACTIVE
                </label>
              </div>
            </fieldset>
          </c:if>

          <p class="small text-muted">
            A new unique card number is generated and a QR code is produced from it.
            The card is then ready to print and hand to the donor.
          </p>

          <div class="d-flex gap-2">
            <button type="submit" class="btn btn-danger btn-lg">
              <c:choose>
                <c:when test="${empty existingCard}">Issue card</c:when>
                <c:otherwise>Issue replacement card</c:otherwise>
              </c:choose>
            </button>
            <a href="${ctx}/app/cards?donorId=${donor.donorId}"
               class="btn btn-outline-secondary btn-lg">Cancel</a>
          </div>
        </form>
      </div>
    </div>
  </div>
</div>

<%@ include file="../layout/footer.jspf" %>
