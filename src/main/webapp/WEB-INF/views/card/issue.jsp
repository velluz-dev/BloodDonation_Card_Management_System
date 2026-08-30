<%@ include file="../layout/taglibs.jspf" %>
<%-- FR-3: confirm before issuing, so staff see what a reissue will supersede. --%>
<fmt:message var="pageTitle" key="card.issueTitle" scope="request"/>
<c:set var="nav" value="donors" scope="request" />
<fmt:message var="lblIssuePhoto" key="verify.photoAlt">
  <fmt:param value="${donor.fullName}"/>
</fmt:message>
<%@ include file="../layout/header.jspf" %>

<div class="row justify-content-center">
  <div class="col-12 col-lg-7">

    <h1 class="bd-page-title mb-3">
      <c:choose>
        <c:when test="${empty existingCard}"><fmt:message key="card.issueTitle"/></c:when>
        <c:otherwise><fmt:message key="card.reissueTitle"/></c:otherwise>
      </c:choose>
    </h1>

    <div class="card mb-3">
      <div class="card-body">
        <div class="row g-3 align-items-center">
          <div class="col-auto">
            <img src="${ctx}/app/donors/photo?donorId=${donor.donorId}"
                 alt="${fn:escapeXml(lblIssuePhoto)}" class="bdcms-thumb">
          </div>
          <div class="col">
            <div style="font:600 18px/1.2 var(--bd-sans)"><c:out value="${donor.fullName}" /></div>
            <div style="font:400 12.5px/1.5 var(--bd-sans);color:var(--bd-ink-60);margin-top:5px">
              <span class="font-monospace"><c:out value="${donor.nationalId}" /></span> &middot;
              <c:out value="${donor.bloodGroup.label}" /> &middot;
              ${donor.age} <fmt:message key="common.years"/>
            </div>
          </div>
        </div>
      </div>
    </div>

    <c:if test="${not empty existingCard}">
      <div class="alert alert-warning">
        <div class="fw-semibold mb-1"><fmt:message key="card.alreadyHolds"/></div>
        <fmt:message key="card.supersededNote">
          <fmt:param value="${existingCard.cardNo}"/>
          <fmt:param value="${existingCard.issuedOnDisplay}"/>
        </fmt:message>
      </div>
    </c:if>

    <div class="card">
      <div class="card-body">
        <form method="post" action="${ctx}/app/cards/issue?donorId=${donor.donorId}">
          <input type="hidden" name="_csrf" value="${csrfToken}">

          <c:if test="${not empty existingCard}">
            <fieldset class="mb-3">
              <legend class="form-label fs-6"><fmt:message key="card.whyNew"/></legend>
              <div class="form-check">
                <input class="form-check-input" type="radio" name="reason" id="reasonLost"
                       value="lost" checked>
                <label class="form-check-label" for="reasonLost">
                  <fmt:message key="card.lostStolen"/>
                </label>
              </div>
              <div class="form-check">
                <input class="form-check-input" type="radio" name="reason" id="reasonReplace"
                       value="replace">
                <label class="form-check-label" for="reasonReplace">
                  <fmt:message key="card.routine"/>
                </label>
              </div>
            </fieldset>
          </c:if>

          <p class="small text-muted">
            <fmt:message key="card.newNumberNote"/>
          </p>

          <div class="d-flex gap-2">
            <button type="submit" class="btn btn-danger btn-lg">
              <c:choose>
                <c:when test="${empty existingCard}"><fmt:message key="card.issueButton"/></c:when>
                <c:otherwise><fmt:message key="card.issueReplacement"/></c:otherwise>
              </c:choose>
            </button>
            <a href="${ctx}/app/cards?donorId=${donor.donorId}"
               class="btn btn-outline-secondary btn-lg"><fmt:message key="common.cancel"/></a>
          </div>
        </form>
      </div>
    </div>
  </div>
</div>

<%@ include file="../layout/footer.jspf" %>
