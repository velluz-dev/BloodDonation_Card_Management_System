<%@ include file="../layout/taglibs.jspf" %>
<%--
  FR-3: the printable card.

  An on-screen preview of the physical card, at roughly its real size, plus the
  PDF that goes to the card printer. The national ID is deliberately not on the
  card - a card dropped in the street should not hand a finder the donor's
  identity number.
--%>
<c:set var="pageTitle" value="Print card" scope="request" />
<c:set var="nav" value="donors" scope="request" />
<%@ include file="../layout/header.jspf" %>

<div class="d-flex flex-wrap justify-content-between align-items-center mb-3 gap-2">
  <div>
    <h1 class="h3 mb-1">Donation card</h1>
    <p class="text-muted mb-0">
      <c:out value="${donor.fullName}" /> &middot;
      <span class="font-monospace"><c:out value="${card.cardNo}" /></span>
    </p>
  </div>
  <div class="d-flex gap-2">
    <a href="${ctx}/app/cards/print?cardNo=${card.cardNo}&format=pdf"
       class="btn btn-danger" target="_blank" rel="noopener">Open printable PDF</a>
    <a href="${ctx}/app/cards?donorId=${donor.donorId}" class="btn btn-outline-secondary">
      All cards
    </a>
  </div>
</div>

<c:if test="${not card.usable}">
  <div class="alert alert-warning">
    This card is <c:out value="${card.status.label}" /> and cannot be used for
    verification. Printing it is possible for the record, but the donor needs an
    active card.
  </div>
</c:if>

<div class="d-flex justify-content-center">
  <div class="bdcms-card-preview">
    <div class="bdcms-card-header">
      <div class="bdcms-card-title">BLOOD DONATION CARD</div>
      <div class="bdcms-card-centre">Central Blood Bank</div>
    </div>

    <div class="bdcms-card-body">
      <img src="${ctx}/app/donors/photo?donorId=${donor.donorId}"
           alt="Photograph of ${fn:escapeXml(donor.fullName)}" class="bdcms-card-photo">

      <div class="bdcms-card-details">
        <div class="bdcms-card-label">Donor name</div>
        <div class="bdcms-card-value"><c:out value="${donor.fullName}" /></div>

        <div class="bdcms-card-label">Blood group</div>
        <div class="bdcms-card-group"><c:out value="${donor.bloodGroup.label}" /></div>

        <div class="bdcms-card-label">Card number</div>
        <div class="bdcms-card-no"><c:out value="${card.cardNo}" /></div>
      </div>

      <div class="bdcms-card-qr">
        <img src="${ctx}/app/cards/qr?cardNo=${card.cardNo}&size=300"
             alt="QR code encoding card number ${fn:escapeXml(card.cardNo)}">
        <div class="bdcms-card-label">Scan at counter</div>
      </div>
    </div>

    <div class="bdcms-card-footer">
      Issued ${card.issuedOnDisplay} &middot; Present this card at every donation.
    </div>
  </div>
</div>

<p class="text-center text-muted small mt-3">
  The QR code encodes the card number only - no name and no identity number, so a
  photographed card reveals nothing on its own.
</p>

<%@ include file="../layout/footer.jspf" %>
