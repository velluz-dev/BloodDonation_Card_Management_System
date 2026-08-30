<%@ include file="../layout/taglibs.jspf" %>
<%--
  FR-2: donor detail and editing.

  Shows only what the donor feature owns. Cards and donation history are reached
  by link because they belong to other features - the same boundary the Java
  packages keep.
--%>
<c:set var="pageTitle" value="${donor.fullName}" scope="request" />
<c:set var="nav" value="donors" scope="request" />
<fmt:message var="lblNewPin" key="donor.newPin"/>
<fmt:message var="lblViewPhoto" key="verify.photoAlt">
  <fmt:param value="${donor.fullName}"/>
</fmt:message>
<%-- The confirm text carries literal \n sequences from the bundle, which JS
     turns into line breaks. Kept free of apostrophes so it survives being
     embedded in a single-quoted JS string. --%>
<fmt:message var="lblDeleteConfirm" key="donor.deleteConfirm">
  <fmt:param value="${donor.fullName}"/>
</fmt:message>
<%@ include file="../layout/header.jspf" %>

<nav aria-label="breadcrumb">
  <ol class="breadcrumb">
    <li class="breadcrumb-item"><a href="${ctx}/app/donors"><fmt:message key="common.donors"/></a></li>
    <li class="breadcrumb-item active" aria-current="page"><c:out value="${donor.fullName}" /></li>
  </ol>
</nav>

<div class="row g-4">
  <div class="col-12 col-lg-4">
    <div class="card">
      <div class="card-body text-center">
        <img src="${ctx}/app/donors/photo?donorId=${donor.donorId}"
             alt="${fn:escapeXml(lblViewPhoto)}"
             class="bdcms-portrait mx-auto mb-3">
        <h1 class="h4 mb-1"><c:out value="${donor.fullName}" /></h1>
        <div class="mb-2">
          <span class="badge text-bg-danger fs-6"><c:out value="${donor.bloodGroup.label}" /></span>
          <span class="badge text-bg-light fs-6">${donor.age} <fmt:message key="common.years"/></span>
        </div>
        <div class="font-monospace text-muted"><c:out value="${donor.nationalId}" /></div>
        <div class="small text-muted"><fmt:message key="common.registered"/> ${donor.registeredOnDisplay}</div>
      </div>
      <div class="card-footer bg-white d-grid gap-2">
        <a href="${ctx}/app/cards?donorId=${donor.donorId}" class="btn btn-outline-secondary">
          <fmt:message key="common.cards"/>
        </a>
        <a href="${ctx}/app/donations/history?donorId=${donor.donorId}" class="btn btn-outline-secondary">
          <fmt:message key="donor.donationHistory"/>
        </a>
        <c:if test="${not editing}">
          <a href="${ctx}/app/donors/view?donorId=${donor.donorId}&edit=1" class="btn btn-danger">
            <fmt:message key="donor.editDetails"/>
          </a>
        </c:if>

        <%-- Deleting donor data is an administrator action, and the server
             enforces that independently - this only hides the control. --%>
        <c:if test="${currentUser.admin and not editing}">
          <form method="post" action="${ctx}/app/admin/donors/delete" class="d-grid m-0"
                onsubmit="return confirm('${fn:escapeXml(lblDeleteConfirm)}');">
            <input type="hidden" name="_csrf" value="${csrfToken}">
            <input type="hidden" name="donorId" value="${donor.donorId}">
            <button type="submit" class="btn btn-quiet"
                    style="border-color:rgba(176,30,42,.35);color:var(--bd-crimson)">
              <fmt:message key="donor.deleteDonor"/>
            </button>
          </form>
        </c:if>
      </div>
    </div>

    <c:if test="${currentUser.admin}">
      <div class="alert alert-info mt-3" style="font-size:12.5px">
        <strong><fmt:message key="donor.deleteDonor"/>.</strong>
        <fmt:message key="donor.deleteNote"/>
      </div>
    </c:if>

    <div class="card mt-3">
      <div class="card-header bg-white fw-semibold"><fmt:message key="donor.verificationPin"/></div>
      <div class="card-body">
        <p class="small mb-3">
          <c:choose>
            <c:when test="${donor.pinSet}">
              <fmt:message key="donor.pinIsSet"/>
            </c:when>
            <c:otherwise>
              <fmt:message key="donor.pinNotSetNote"/>
            </c:otherwise>
          </c:choose>
        </p>
        <form method="post" action="${ctx}/app/donors/view?donorId=${donor.donorId}"
              enctype="multipart/form-data" class="row g-2">
          <input type="hidden" name="_csrf" value="${csrfToken}">
          <input type="hidden" name="action" value="setPin">
          <div class="col-7">
            <input type="password" class="form-control bdcms-pin" name="pin"
                   inputmode="numeric" pattern="[0-9]{4}" maxlength="4"
                   placeholder="${fn:escapeXml(lblNewPin)}" autocomplete="off">
          </div>
          <div class="col-5 d-grid">
            <button type="submit" class="btn btn-sm btn-outline-danger"><fmt:message key="donor.pinSet"/></button>
          </div>
          <c:if test="${donor.pinSet}">
            <div class="col-12 d-grid">
              <button type="submit" name="pinMode" value="clear"
                      class="btn btn-sm btn-outline-secondary"><fmt:message key="donor.removePin"/></button>
            </div>
          </c:if>
        </form>
      </div>
    </div>
  </div>

  <div class="col-12 col-lg-8">
    <div class="card">
      <div class="card-header bg-white fw-semibold">
        <c:choose>
          <c:when test="${editing}"><fmt:message key="donor.editDetails"/></c:when>
          <c:otherwise><fmt:message key="donor.details"/></c:otherwise>
        </c:choose>
      </div>
      <div class="card-body">

        <c:choose>
          <c:when test="${editing}">
            <form method="post" action="${ctx}/app/donors/view?donorId=${donor.donorId}"
                  enctype="multipart/form-data" novalidate>
              <input type="hidden" name="_csrf" value="${csrfToken}">

              <div class="row g-3">
                <div class="col-12 col-md-6">
                  <label class="form-label"><fmt:message key="common.nationalId"/></label>
                  <input type="text" class="form-control font-monospace"
                         value="${fn:escapeXml(donor.nationalId)}" readonly disabled>
                  <div class="form-text">
                    <fmt:message key="donor.idNeverEditable"/>
                  </div>
                </div>

                <div class="col-12 col-md-6">
                  <label for="fullName" class="form-label"><fmt:message key="donor.fullName"/></label>
                  <input type="text" class="form-control ${not empty fieldErrors.fullName ? 'is-invalid' : ''}"
                         id="fullName" name="fullName" value="${fn:escapeXml(donor.fullName)}"
                         maxlength="120" required>
                  <div class="invalid-feedback"><c:out value="${fieldErrors.fullName}" /></div>
                </div>

                <div class="col-12 col-md-4">
                  <label for="dateOfBirth" class="form-label"><fmt:message key="common.dateOfBirth"/></label>
                  <input type="date" class="form-control ${not empty fieldErrors.dateOfBirth ? 'is-invalid' : ''}"
                         id="dateOfBirth" name="dateOfBirth" value="${donor.dateOfBirthIso}" required>
                  <div class="invalid-feedback"><c:out value="${fieldErrors.dateOfBirth}" /></div>
                </div>

                <div class="col-6 col-md-4">
                  <label for="gender" class="form-label"><fmt:message key="common.gender"/></label>
                  <select class="form-select" id="gender" name="gender" required>
                    <c:forEach var="g" items="${genders}">
                      <option value="${g}" ${donor.gender eq g ? 'selected' : ''}>
                        <fmt:message key="${g.messageKey}"/>
                      </option>
                    </c:forEach>
                  </select>
                </div>

                <div class="col-6 col-md-4">
                  <label for="bloodGroup" class="form-label"><fmt:message key="common.bloodGroup"/></label>
                  <select class="form-select" id="bloodGroup" name="bloodGroup" required>
                    <c:forEach var="bg" items="${bloodGroups}">
                      <option value="${bg}" ${donor.bloodGroup eq bg ? 'selected' : ''}>
                        <c:out value="${bg.label}" />
                      </option>
                    </c:forEach>
                  </select>
                </div>

                <div class="col-12 col-md-6">
                  <label for="contactNo" class="form-label"><fmt:message key="donor.contactNumber"/></label>
                  <input type="tel" class="form-control ${not empty fieldErrors.contactNo ? 'is-invalid' : ''}"
                         id="contactNo" name="contactNo" value="${fn:escapeXml(donor.contactNo)}"
                         maxlength="20" required>
                  <div class="invalid-feedback"><c:out value="${fieldErrors.contactNo}" /></div>
                </div>

                <div class="col-12 col-md-6">
                  <label for="photo" class="form-label"><fmt:message key="donor.replacePhoto"/></label>
                  <input type="file" class="form-control" id="photo" name="photo"
                         accept="image/jpeg,image/png">
                </div>

                <div class="col-12">
                  <label for="address" class="form-label"><fmt:message key="common.address"/></label>
                  <textarea class="form-control" id="address" name="address" rows="2"
                            maxlength="255"><c:out value="${donor.address}" /></textarea>
                </div>
              </div>

              <div class="d-flex gap-2 mt-4">
                <button type="submit" class="btn btn-danger"><fmt:message key="donor.saveChanges"/></button>
                <a href="${ctx}/app/donors/view?donorId=${donor.donorId}"
                   class="btn btn-outline-secondary"><fmt:message key="common.cancel"/></a>
              </div>
            </form>
          </c:when>

          <c:otherwise>
            <dl class="row mb-0">
              <dt class="col-sm-4"><fmt:message key="common.nationalId"/></dt>
              <dd class="col-sm-8 font-monospace"><c:out value="${donor.nationalId}" /></dd>

              <dt class="col-sm-4"><fmt:message key="donor.fullName"/></dt>
              <dd class="col-sm-8"><c:out value="${donor.fullName}" /></dd>

              <dt class="col-sm-4"><fmt:message key="common.dateOfBirth"/></dt>
              <dd class="col-sm-8">${donor.dateOfBirthDisplay} (${donor.age} <fmt:message key="common.years"/>)</dd>

              <dt class="col-sm-4"><fmt:message key="common.gender"/></dt>
              <dd class="col-sm-8"><fmt:message key="${donor.gender.messageKey}"/></dd>

              <dt class="col-sm-4"><fmt:message key="common.bloodGroup"/></dt>
              <dd class="col-sm-8"><c:out value="${donor.bloodGroup.label}" /></dd>

              <dt class="col-sm-4"><fmt:message key="donor.contactNumber"/></dt>
              <dd class="col-sm-8"><c:out value="${donor.contactNo}" /></dd>

              <dt class="col-sm-4"><fmt:message key="common.address"/></dt>
              <dd class="col-sm-8"><c:out value="${donor.address}" default="-" /></dd>

              <dt class="col-sm-4"><fmt:message key="donor.verificationPin"/></dt>
              <dd class="col-sm-8">
                <c:choose>
                  <c:when test="${donor.pinSet}"><span class="badge text-bg-success"><fmt:message key="donor.pinSet"/></span></c:when>
                  <c:otherwise><span class="badge text-bg-secondary"><fmt:message key="donor.pinNotSet"/></span></c:otherwise>
                </c:choose>
              </dd>

              <dt class="col-sm-4"><fmt:message key="common.registered"/></dt>
              <dd class="col-sm-8">${donor.registeredOnDisplay}</dd>
            </dl>
          </c:otherwise>
        </c:choose>
      </div>
    </div>
  </div>
</div>

<%@ include file="../layout/footer.jspf" %>
