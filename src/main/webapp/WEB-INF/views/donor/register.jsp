<%@ include file="../layout/taglibs.jspf" %>
<%--
  FR-2: donor registration.

  The duplicate panel is the mockup's central idea made literal: the unique
  constraint is surfaced as a LOOKUP, not an error. A repeat national ID does
  not scold the operator - it hands them the record that already exists, with
  the actions they actually want next.
--%>
<c:set var="pageTitle" value="Register donor" scope="request" />
<c:set var="nav" value="register" scope="request" />
<%@ include file="../layout/header.jspf" %>

<div class="mb-4">
  <h1 class="bd-page-title">Register a new donor</h1>
  <p class="bd-page-sub">Identity and demographics &middot; one record per person, keyed by national ID</p>
</div>

<form method="post" action="${ctx}/app/donors/register" enctype="multipart/form-data" novalidate>
  <input type="hidden" name="_csrf" value="${csrfToken}">

  <div class="row g-4">
    <div class="col-12 col-lg-8">

      <%-- ---- national ID, and the duplicate it may collide with ---- --%>
      <div class="card mb-3">
        <div class="card-body">
          <label for="nationalId" class="form-label">
            National ID number <span style="color:var(--bd-crimson)">*</span>
          </label>
          <input type="text"
                 class="form-control font-monospace ${not empty duplicate ? 'is-invalid' : ''} ${not empty fieldErrors.nationalId ? 'is-invalid' : ''}"
                 id="nationalId" name="nationalId" value="${fn:escapeXml(donor.nationalId)}"
                 maxlength="20" required spellcheck="false"
                 style="font-size:15.5px;letter-spacing:.04em">
          <div class="invalid-feedback"><c:out value="${fieldErrors.nationalId}" /></div>
          <div class="form-text">Cannot be changed after registration.</div>

          <c:if test="${not empty duplicate}">
            <div class="mt-3" style="padding:18px 20px;border:1px solid rgba(176,30,42,.3);
                        background:rgba(176,30,42,.05);border-radius:var(--bd-r-lg)">
              <div style="font:400 13.5px/1.5 var(--bd-sans);color:var(--bd-ink-75)">
                This national ID belongs to an existing donor record. Registration is
                blocked &mdash; open the record instead, and reissue a card if theirs
                was lost.
              </div>

              <div class="d-flex flex-wrap align-items-center gap-3 mt-3"
                   style="padding:14px 16px;background:#fff;border:1px solid var(--bd-line);
                          border-radius:var(--bd-r)">
                <img src="${ctx}/app/donors/photo?donorId=${duplicate.donorId}"
                     alt="Photograph of ${fn:escapeXml(duplicate.fullName)}"
                     class="bdcms-thumb" style="width:54px;height:66px">

                <div style="flex:1;min-width:180px">
                  <div style="font:600 16px/1.2 var(--bd-sans)">
                    <c:out value="${duplicate.fullName}" />
                  </div>
                  <div class="font-monospace" style="font-size:12.5px;color:rgba(23,21,15,.55);margin-top:5px">
                    <c:out value="${duplicate.nationalId}" /> &middot;
                    registered ${duplicate.registeredOnDisplay}
                  </div>
                </div>

                <div class="text-center px-3"
                     style="border-left:1px solid rgba(23,21,15,.1)">
                  <div style="font:600 20px/1 var(--bd-sans);color:var(--bd-crimson)">
                    <c:out value="${duplicate.bloodGroup.label}" />
                  </div>
                  <div class="bd-eyebrow mt-1">Group</div>
                </div>

                <a href="${ctx}/app/donors/view?donorId=${duplicate.donorId}"
                   class="btn btn-sm btn-outline-secondary">Open record</a>
              </div>
            </div>
          </c:if>
        </div>
      </div>

      <%-- ---- demographics ---- --%>
      <div class="card">
        <div class="card-header">Donor details</div>
        <div class="card-body">
          <div class="row g-3">

            <div class="col-12 col-md-7">
              <label for="fullName" class="form-label">
                Full name <span style="color:var(--bd-crimson)">*</span>
              </label>
              <input type="text" class="form-control ${not empty fieldErrors.fullName ? 'is-invalid' : ''}"
                     id="fullName" name="fullName" value="${fn:escapeXml(donor.fullName)}"
                     maxlength="120" required>
              <div class="invalid-feedback"><c:out value="${fieldErrors.fullName}" /></div>
            </div>

            <div class="col-12 col-md-5">
              <label for="dateOfBirth" class="form-label">
                Date of birth <span style="color:var(--bd-crimson)">*</span>
              </label>
              <input type="date" class="form-control ${not empty fieldErrors.dateOfBirth ? 'is-invalid' : ''}"
                     id="dateOfBirth" name="dateOfBirth" value="${donor.dateOfBirthIso}"
                     min="${minDateOfBirth}" max="${maxDateOfBirth}" required>
              <div class="invalid-feedback"><c:out value="${fieldErrors.dateOfBirth}" /></div>
            </div>

            <div class="col-6 col-md-3">
              <label for="bloodGroup" class="form-label">
                Blood group <span style="color:var(--bd-crimson)">*</span>
              </label>
              <select class="form-select font-monospace ${not empty fieldErrors.bloodGroup ? 'is-invalid' : ''}"
                      id="bloodGroup" name="bloodGroup" required
                      style="color:var(--bd-crimson);font-weight:500">
                <option value="">&mdash;</option>
                <c:forEach var="bg" items="${bloodGroups}">
                  <option value="${bg}" ${donor.bloodGroup eq bg ? 'selected' : ''}>
                    <c:out value="${bg.label}" />
                  </option>
                </c:forEach>
              </select>
              <div class="invalid-feedback"><c:out value="${fieldErrors.bloodGroup}" /></div>
            </div>

            <div class="col-6 col-md-3">
              <label for="gender" class="form-label">
                Gender <span style="color:var(--bd-crimson)">*</span>
              </label>
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

            <div class="col-12 col-md-6">
              <label for="contactNo" class="form-label">
                Contact number <span style="color:var(--bd-crimson)">*</span>
              </label>
              <input type="tel" class="form-control ${not empty fieldErrors.contactNo ? 'is-invalid' : ''}"
                     id="contactNo" name="contactNo" value="${fn:escapeXml(donor.contactNo)}"
                     maxlength="20" required>
              <div class="invalid-feedback"><c:out value="${fieldErrors.contactNo}" /></div>
            </div>

            <div class="col-12">
              <label for="address" class="form-label">Address</label>
              <textarea class="form-control ${not empty fieldErrors.address ? 'is-invalid' : ''}"
                        id="address" name="address" rows="2" maxlength="255"
                        placeholder="Block, street, unit, postal code"><c:out value="${donor.address}" /></textarea>
              <div class="invalid-feedback"><c:out value="${fieldErrors.address}" /></div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <%-- ---- photograph and PIN ---- --%>
    <div class="col-12 col-lg-4">
      <div class="card mb-3">
        <div class="card-header">Donor photograph</div>
        <div class="card-body">
          <img id="photoPreview" class="d-none mb-3"
               alt="Preview of the selected photograph"
               style="width:100%;aspect-ratio:3/4;object-fit:cover;border-radius:var(--bd-r);
                      border:1px solid var(--bd-line)">
          <input type="file" class="form-control" id="photo" name="photo"
                 accept="image/jpeg,image/png">
          <div class="form-text mt-2">
            JPEG or PNG, up to 2&nbsp;MB. This is the image staff compare against the
            donor at every future donation, so a clear, front-facing photo matters.
            Face front-on, no headwear covering the face, plain background.
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-header">Verification PIN &mdash; optional</div>
        <div class="card-body">
          <label for="pin" class="form-label">Four-digit PIN</label>
          <input type="password" class="form-control bdcms-pin ${not empty fieldErrors.pin ? 'is-invalid' : ''}"
                 id="pin" name="pin" inputmode="numeric" pattern="[0-9]{4}"
                 maxlength="4" autocomplete="off">
          <div class="invalid-feedback"><c:out value="${fieldErrors.pin}" /></div>
          <div class="form-text mt-2">
            Chosen by the donor and requested on return visits as a second factor.
            Stored hashed, never in plain text, and can be set or removed later.
          </div>
        </div>
      </div>
    </div>
  </div>

  <div class="d-flex flex-wrap align-items-center gap-3 mt-4">
    <button type="submit" class="btn btn-danger btn-lg">Register donor</button>
    <a href="${ctx}/app/donors" class="btn btn-quiet btn-lg">Cancel</a>
    <c:if test="${not empty duplicate}">
      <span style="font:400 12.5px/1.4 var(--bd-sans);color:var(--bd-ink-50)">
        Blocked until the ID conflict is resolved
      </span>
    </c:if>
  </div>
</form>

<%@ include file="../layout/footer.jspf" %>
