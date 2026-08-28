<%@ include file="../layout/taglibs.jspf" %>
<%--
  FR-2: donor detail and editing.

  Shows only what the donor feature owns. Cards and donation history are reached
  by link because they belong to other features - the same boundary the Java
  packages keep.
--%>
<c:set var="pageTitle" value="${donor.fullName}" scope="request" />
<c:set var="nav" value="donors" scope="request" />
<%@ include file="../layout/header.jspf" %>

<nav aria-label="breadcrumb">
  <ol class="breadcrumb">
    <li class="breadcrumb-item"><a href="${ctx}/app/donors">Donors</a></li>
    <li class="breadcrumb-item active" aria-current="page"><c:out value="${donor.fullName}" /></li>
  </ol>
</nav>

<div class="row g-4">
  <div class="col-12 col-lg-4">
    <div class="card">
      <div class="card-body text-center">
        <img src="${ctx}/app/donors/photo?donorId=${donor.donorId}"
             alt="Photograph of ${fn:escapeXml(donor.fullName)}"
             class="bdcms-portrait mx-auto mb-3">
        <h1 class="h4 mb-1"><c:out value="${donor.fullName}" /></h1>
        <div class="mb-2">
          <span class="badge text-bg-danger fs-6"><c:out value="${donor.bloodGroup.label}" /></span>
          <span class="badge text-bg-light fs-6">${donor.age} yrs</span>
        </div>
        <div class="font-monospace text-muted"><c:out value="${donor.nationalId}" /></div>
        <div class="small text-muted">Registered ${donor.registeredOnDisplay}</div>
      </div>
      <div class="card-footer bg-white d-grid gap-2">
        <a href="${ctx}/app/cards?donorId=${donor.donorId}" class="btn btn-outline-secondary">
          Cards
        </a>
        <a href="${ctx}/app/donations/history?donorId=${donor.donorId}" class="btn btn-outline-secondary">
          Donation history
        </a>
        <c:if test="${not editing}">
          <a href="${ctx}/app/donors/view?donorId=${donor.donorId}&edit=1" class="btn btn-danger">
            Edit details
          </a>
        </c:if>

        <%-- Deleting donor data is an administrator action, and the server
             enforces that independently - this only hides the control. --%>
        <c:if test="${currentUser.admin and not editing}">
          <form method="post" action="${ctx}/app/admin/donors/delete" class="d-grid m-0"
                onsubmit="return confirm('Delete ${fn:escapeXml(donor.fullName)} permanently?\n\nTheir cards and photograph are removed. Verification log entries are kept but detached.\n\nThis cannot be undone.');">
            <input type="hidden" name="_csrf" value="${csrfToken}">
            <input type="hidden" name="donorId" value="${donor.donorId}">
            <button type="submit" class="btn btn-quiet"
                    style="border-color:rgba(176,30,42,.35);color:var(--bd-crimson)">
              Delete donor
            </button>
          </form>
        </c:if>
      </div>
    </div>

    <c:if test="${currentUser.admin}">
      <div class="alert alert-info mt-3" style="font-size:12.5px">
        <strong>Deleting a donor.</strong> Permitted only while they have no
        recorded donations &mdash; history is append-only, because the
        eligibility rule is computed from it. Verification log entries are never
        deleted; they are kept and detached from the record.
      </div>
    </c:if>

    <div class="card mt-3">
      <div class="card-header bg-white fw-semibold">Verification PIN</div>
      <div class="card-body">
        <p class="small mb-3">
          <c:choose>
            <c:when test="${donor.pinSet}">
              A PIN is set. It is asked for as a second factor at every verification.
            </c:when>
            <c:otherwise>
              No PIN is set. Verification relies on the photograph and masked ID alone.
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
                   placeholder="New PIN" autocomplete="off">
          </div>
          <div class="col-5 d-grid">
            <button type="submit" class="btn btn-sm btn-outline-danger">Set</button>
          </div>
          <c:if test="${donor.pinSet}">
            <div class="col-12 d-grid">
              <button type="submit" name="pinMode" value="clear"
                      class="btn btn-sm btn-outline-secondary">Remove PIN</button>
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
          <c:when test="${editing}">Edit donor details</c:when>
          <c:otherwise>Donor details</c:otherwise>
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
                  <label class="form-label">National ID</label>
                  <input type="text" class="form-control font-monospace"
                         value="${fn:escapeXml(donor.nationalId)}" readonly disabled>
                  <div class="form-text">
                    Never editable. It is the key that keeps this record unique
                    across every centre.
                  </div>
                </div>

                <div class="col-12 col-md-6">
                  <label for="fullName" class="form-label">Full name</label>
                  <input type="text" class="form-control ${not empty fieldErrors.fullName ? 'is-invalid' : ''}"
                         id="fullName" name="fullName" value="${fn:escapeXml(donor.fullName)}"
                         maxlength="120" required>
                  <div class="invalid-feedback"><c:out value="${fieldErrors.fullName}" /></div>
                </div>

                <div class="col-12 col-md-4">
                  <label for="dateOfBirth" class="form-label">Date of birth</label>
                  <input type="date" class="form-control ${not empty fieldErrors.dateOfBirth ? 'is-invalid' : ''}"
                         id="dateOfBirth" name="dateOfBirth" value="${donor.dateOfBirthIso}" required>
                  <div class="invalid-feedback"><c:out value="${fieldErrors.dateOfBirth}" /></div>
                </div>

                <div class="col-6 col-md-4">
                  <label for="gender" class="form-label">Gender</label>
                  <select class="form-select" id="gender" name="gender" required>
                    <c:forEach var="g" items="${genders}">
                      <option value="${g}" ${donor.gender eq g ? 'selected' : ''}>
                        <c:out value="${g.label}" />
                      </option>
                    </c:forEach>
                  </select>
                </div>

                <div class="col-6 col-md-4">
                  <label for="bloodGroup" class="form-label">Blood group</label>
                  <select class="form-select" id="bloodGroup" name="bloodGroup" required>
                    <c:forEach var="bg" items="${bloodGroups}">
                      <option value="${bg}" ${donor.bloodGroup eq bg ? 'selected' : ''}>
                        <c:out value="${bg.label}" />
                      </option>
                    </c:forEach>
                  </select>
                </div>

                <div class="col-12 col-md-6">
                  <label for="contactNo" class="form-label">Contact number</label>
                  <input type="tel" class="form-control ${not empty fieldErrors.contactNo ? 'is-invalid' : ''}"
                         id="contactNo" name="contactNo" value="${fn:escapeXml(donor.contactNo)}"
                         maxlength="20" required>
                  <div class="invalid-feedback"><c:out value="${fieldErrors.contactNo}" /></div>
                </div>

                <div class="col-12 col-md-6">
                  <label for="photo" class="form-label">Replace photograph</label>
                  <input type="file" class="form-control" id="photo" name="photo"
                         accept="image/jpeg,image/png">
                </div>

                <div class="col-12">
                  <label for="address" class="form-label">Address</label>
                  <textarea class="form-control" id="address" name="address" rows="2"
                            maxlength="255"><c:out value="${donor.address}" /></textarea>
                </div>
              </div>

              <div class="d-flex gap-2 mt-4">
                <button type="submit" class="btn btn-danger">Save changes</button>
                <a href="${ctx}/app/donors/view?donorId=${donor.donorId}"
                   class="btn btn-outline-secondary">Cancel</a>
              </div>
            </form>
          </c:when>

          <c:otherwise>
            <dl class="row mb-0">
              <dt class="col-sm-4">National ID</dt>
              <dd class="col-sm-8 font-monospace"><c:out value="${donor.nationalId}" /></dd>

              <dt class="col-sm-4">Full name</dt>
              <dd class="col-sm-8"><c:out value="${donor.fullName}" /></dd>

              <dt class="col-sm-4">Date of birth</dt>
              <dd class="col-sm-8">${donor.dateOfBirthDisplay} (${donor.age} years)</dd>

              <dt class="col-sm-4">Gender</dt>
              <dd class="col-sm-8"><c:out value="${donor.gender.label}" /></dd>

              <dt class="col-sm-4">Blood group</dt>
              <dd class="col-sm-8"><c:out value="${donor.bloodGroup.label}" /></dd>

              <dt class="col-sm-4">Contact number</dt>
              <dd class="col-sm-8"><c:out value="${donor.contactNo}" /></dd>

              <dt class="col-sm-4">Address</dt>
              <dd class="col-sm-8"><c:out value="${donor.address}" default="-" /></dd>

              <dt class="col-sm-4">Verification PIN</dt>
              <dd class="col-sm-8">
                <c:choose>
                  <c:when test="${donor.pinSet}"><span class="badge text-bg-success">Set</span></c:when>
                  <c:otherwise><span class="badge text-bg-secondary">Not set</span></c:otherwise>
                </c:choose>
              </dd>

              <dt class="col-sm-4">Registered</dt>
              <dd class="col-sm-8">${donor.registeredOnDisplay}</dd>
            </dl>
          </c:otherwise>
        </c:choose>
      </div>
    </div>
  </div>
</div>

<%@ include file="../layout/footer.jspf" %>
