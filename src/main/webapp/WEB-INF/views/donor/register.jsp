<%@ include file="../layout/taglibs.jspf" %>
<%--
  FR-2: donor registration.

  The duplicate panel below is the requirement "an attempt to register an
  existing ID is rejected and the existing record is shown instead" made
  literal: the form is redisplayed with what was typed, and the record that
  already exists appears beside it with links to act on it.
--%>
<c:set var="pageTitle" value="Register donor" scope="request" />
<c:set var="nav" value="register" scope="request" />
<%@ include file="../layout/header.jspf" %>

<h1 class="h3 mb-1">Register a new donor</h1>
<p class="text-muted">
  One record per person, keyed by national ID. The unique constraint is enforced
  by the database, so the same person cannot be registered twice at two counters.
</p>

<c:if test="${not empty duplicate}">
  <div class="card border-warning mb-4">
    <div class="card-header bg-warning-subtle fw-semibold">
      This national ID is already registered
    </div>
    <div class="card-body">
      <div class="row g-3 align-items-center">
        <div class="col-auto">
          <img src="${ctx}/app/donors/photo?donorId=${duplicate.donorId}"
               alt="Photograph of ${fn:escapeXml(duplicate.fullName)}"
               class="bdcms-thumb">
        </div>
        <div class="col">
          <div class="fs-5 fw-semibold"><c:out value="${duplicate.fullName}" /></div>
          <div class="text-muted small">
            <c:out value="${duplicate.nationalId}" /> &middot;
            <c:out value="${duplicate.bloodGroup.label}" /> &middot;
            <c:out value="${duplicate.contactNo}" />
          </div>
          <div class="text-muted small">Registered ${duplicate.registeredOnDisplay}</div>
        </div>
        <div class="col-12 col-md-auto d-flex flex-wrap gap-2">
          <a href="${ctx}/app/donors/view?donorId=${duplicate.donorId}"
             class="btn btn-sm btn-outline-secondary">Open record</a>
          <a href="${ctx}/app/cards?donorId=${duplicate.donorId}"
             class="btn btn-sm btn-outline-secondary">Cards</a>
          <a href="${ctx}/app/donations/history?donorId=${duplicate.donorId}"
             class="btn btn-sm btn-outline-secondary">History</a>
        </div>
      </div>
    </div>
  </div>
</c:if>

<form method="post" action="${ctx}/app/donors/register" enctype="multipart/form-data" novalidate>
  <input type="hidden" name="_csrf" value="${csrfToken}">

  <div class="row g-4">
    <div class="col-12 col-lg-8">
      <div class="card">
        <div class="card-header bg-white fw-semibold">Donor details</div>
        <div class="card-body">
          <div class="row g-3">

            <div class="col-12 col-md-6">
              <label for="nationalId" class="form-label">National ID <span class="text-danger">*</span></label>
              <input type="text" class="form-control font-monospace ${not empty fieldErrors.nationalId ? 'is-invalid' : ''}"
                     id="nationalId" name="nationalId" value="${fn:escapeXml(donor.nationalId)}"
                     maxlength="20" required spellcheck="false">
              <div class="invalid-feedback"><c:out value="${fieldErrors.nationalId}" /></div>
              <div class="form-text">Cannot be changed after registration.</div>
            </div>

            <div class="col-12 col-md-6">
              <label for="fullName" class="form-label">Full name <span class="text-danger">*</span></label>
              <input type="text" class="form-control ${not empty fieldErrors.fullName ? 'is-invalid' : ''}"
                     id="fullName" name="fullName" value="${fn:escapeXml(donor.fullName)}"
                     maxlength="120" required>
              <div class="invalid-feedback"><c:out value="${fieldErrors.fullName}" /></div>
            </div>

            <div class="col-12 col-md-4">
              <label for="dateOfBirth" class="form-label">Date of birth <span class="text-danger">*</span></label>
              <input type="date" class="form-control ${not empty fieldErrors.dateOfBirth ? 'is-invalid' : ''}"
                     id="dateOfBirth" name="dateOfBirth" value="${donor.dateOfBirthIso}"
                     min="${minDateOfBirth}" max="${maxDateOfBirth}" required>
              <div class="invalid-feedback"><c:out value="${fieldErrors.dateOfBirth}" /></div>
            </div>

            <div class="col-6 col-md-4">
              <label for="gender" class="form-label">Gender <span class="text-danger">*</span></label>
              <select class="form-select ${not empty fieldErrors.gender ? 'is-invalid' : ''}"
                      id="gender" name="gender" required>
                <option value="">Choose...</option>
                <c:forEach var="g" items="${genders}">
                  <option value="${g}" ${donor.gender eq g ? 'selected' : ''}>
                    <c:out value="${g.label}" />
                  </option>
                </c:forEach>
              </select>
              <div class="invalid-feedback"><c:out value="${fieldErrors.gender}" /></div>
            </div>

            <div class="col-6 col-md-4">
              <label for="bloodGroup" class="form-label">Blood group <span class="text-danger">*</span></label>
              <select class="form-select ${not empty fieldErrors.bloodGroup ? 'is-invalid' : ''}"
                      id="bloodGroup" name="bloodGroup" required>
                <option value="">Choose...</option>
                <c:forEach var="bg" items="${bloodGroups}">
                  <option value="${bg}" ${donor.bloodGroup eq bg ? 'selected' : ''}>
                    <c:out value="${bg.label}" />
                  </option>
                </c:forEach>
              </select>
              <div class="invalid-feedback"><c:out value="${fieldErrors.bloodGroup}" /></div>
            </div>

            <div class="col-12 col-md-6">
              <label for="contactNo" class="form-label">Contact number <span class="text-danger">*</span></label>
              <input type="tel" class="form-control ${not empty fieldErrors.contactNo ? 'is-invalid' : ''}"
                     id="contactNo" name="contactNo" value="${fn:escapeXml(donor.contactNo)}"
                     maxlength="20" required>
              <div class="invalid-feedback"><c:out value="${fieldErrors.contactNo}" /></div>
            </div>

            <div class="col-12">
              <label for="address" class="form-label">Address</label>
              <textarea class="form-control ${not empty fieldErrors.address ? 'is-invalid' : ''}"
                        id="address" name="address" rows="2" maxlength="255"><c:out value="${donor.address}" /></textarea>
              <div class="invalid-feedback"><c:out value="${fieldErrors.address}" /></div>
            </div>

          </div>
        </div>
      </div>
    </div>

    <div class="col-12 col-lg-4">
      <div class="card mb-3">
        <div class="card-header bg-white fw-semibold">Photograph</div>
        <div class="card-body">
          <input type="file" class="form-control" id="photo" name="photo"
                 accept="image/jpeg,image/png">
          <div class="form-text">
            JPEG or PNG, up to 2 MB. This is the image staff compare against the
            donor at every future donation, so a clear, front-facing photo matters.
          </div>
          <img id="photoPreview" class="bdcms-thumb mt-3 d-none" alt="Preview of the selected photograph">
        </div>
      </div>

      <div class="card">
        <div class="card-header bg-white fw-semibold">Verification PIN (optional)</div>
        <div class="card-body">
          <label for="pin" class="form-label">Four-digit PIN</label>
          <input type="password" class="form-control bdcms-pin ${not empty fieldErrors.pin ? 'is-invalid' : ''}"
                 id="pin" name="pin" inputmode="numeric" pattern="[0-9]{4}"
                 maxlength="4" autocomplete="off">
          <div class="invalid-feedback"><c:out value="${fieldErrors.pin}" /></div>
          <div class="form-text">
            A second factor asked for at verification. Stored hashed, never in
            plain text, and can be set or removed later.
          </div>
        </div>
      </div>
    </div>
  </div>

  <div class="d-flex gap-2 mt-4">
    <button type="submit" class="btn btn-danger btn-lg">Register donor</button>
    <a href="${ctx}/app/donors" class="btn btn-outline-secondary btn-lg">Cancel</a>
  </div>
</form>

<%@ include file="../layout/footer.jspf" %>
