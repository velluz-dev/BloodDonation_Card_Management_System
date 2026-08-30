<%@ include file="../layout/taglibs.jspf" %>
<%--
  FR-2: donor registration.

  The duplicate panel is the mockup's central idea made literal: the unique
  constraint is surfaced as a LOOKUP, not an error. A repeat national ID does
  not scold the operator - it hands them the record that already exists, with
  the actions they actually want next.
--%>
<fmt:message var="pageTitle" key="donor.registerButton" scope="request"/>
<fmt:message var="lblAddressHint" key="donor.addressPlaceholder"/>
<fmt:message var="lblPhotoPreview" key="donor.photoPreviewAlt"/>
<c:set var="nav" value="register" scope="request" />
<%@ include file="../layout/header.jspf" %>

<div class="mb-4">
  <h1 class="bd-page-title"><fmt:message key="donor.registerNew"/></h1>
  <p class="bd-page-sub"><fmt:message key="donor.registerSub"/></p>
</div>

<form method="post" action="${ctx}/app/donors/register" enctype="multipart/form-data" novalidate>
  <input type="hidden" name="_csrf" value="${csrfToken}">

  <div class="row g-4">
    <div class="col-12 col-lg-8">

      <%-- ---- national ID, and the duplicate it may collide with ---- --%>
      <div class="card mb-3">
        <div class="card-body">
          <label for="nationalId" class="form-label">
            <fmt:message key="common.nationalId"/> <span style="color:var(--bd-crimson)">*</span>
          </label>
          <input type="text"
                 class="form-control font-monospace ${not empty duplicate ? 'is-invalid' : ''} ${not empty fieldErrors.nationalId ? 'is-invalid' : ''}"
                 id="nationalId" name="nationalId" value="${fn:escapeXml(donor.nationalId)}"
                 maxlength="20" required spellcheck="false"
                 style="font-size:15.5px;letter-spacing:.04em">
          <div class="invalid-feedback"><c:out value="${fieldErrors.nationalId}" /></div>
          <div class="form-text"><fmt:message key="donor.cannotChange"/></div>

          <c:if test="${not empty duplicate}">
            <div class="mt-3" style="padding:18px 20px;border:1px solid rgba(176,30,42,.3);
                        background:rgba(176,30,42,.05);border-radius:var(--bd-r-lg)">
              <div style="font:400 13.5px/1.5 var(--bd-sans);color:var(--bd-ink-75)">
                <fmt:message key="donor.duplicateBody"/>
              </div>

              <div class="d-flex flex-wrap align-items-center gap-3 mt-3"
                   style="padding:14px 16px;background:#fff;border:1px solid var(--bd-line);
                          border-radius:var(--bd-r)">
                <fmt:message var="lblDupPhoto" key="verify.photoAlt">
                  <fmt:param value="${duplicate.fullName}"/>
                </fmt:message>
                <img src="${ctx}/app/donors/photo?donorId=${duplicate.donorId}"
                     alt="${fn:escapeXml(lblDupPhoto)}"
                     class="bdcms-thumb" style="width:54px;height:66px">

                <div style="flex:1;min-width:180px">
                  <div style="font:600 16px/1.2 var(--bd-sans)">
                    <c:out value="${duplicate.fullName}" />
                  </div>
                  <div class="font-monospace" style="font-size:12.5px;color:rgba(23,21,15,.55);margin-top:5px">
                    <c:out value="${duplicate.nationalId}" /> &middot;
                    <fmt:message key="common.registered"/> ${duplicate.registeredOnDisplay}
                  </div>
                </div>

                <div class="text-center px-3"
                     style="border-left:1px solid rgba(23,21,15,.1)">
                  <div style="font:600 20px/1 var(--bd-sans);color:var(--bd-crimson)">
                    <c:out value="${duplicate.bloodGroup.label}" />
                  </div>
                  <div class="bd-eyebrow mt-1"><fmt:message key="donor.group"/></div>
                </div>

                <a href="${ctx}/app/donors/view?donorId=${duplicate.donorId}"
                   class="btn btn-sm btn-outline-secondary"><fmt:message key="donor.openRecord"/></a>
              </div>
            </div>
          </c:if>
        </div>
      </div>

      <%-- ---- demographics ---- --%>
      <div class="card">
        <div class="card-header"><fmt:message key="donor.details"/></div>
        <div class="card-body">
          <div class="row g-3">

            <div class="col-12 col-md-7">
              <label for="fullName" class="form-label">
                <fmt:message key="donor.fullName"/> <span style="color:var(--bd-crimson)">*</span>
              </label>
              <input type="text" class="form-control ${not empty fieldErrors.fullName ? 'is-invalid' : ''}"
                     id="fullName" name="fullName" value="${fn:escapeXml(donor.fullName)}"
                     maxlength="120" required>
              <div class="invalid-feedback"><c:out value="${fieldErrors.fullName}" /></div>
            </div>

            <div class="col-12 col-md-5">
              <label for="dateOfBirth" class="form-label">
                <fmt:message key="common.dateOfBirth"/> <span style="color:var(--bd-crimson)">*</span>
              </label>
              <input type="date" class="form-control ${not empty fieldErrors.dateOfBirth ? 'is-invalid' : ''}"
                     id="dateOfBirth" name="dateOfBirth" value="${donor.dateOfBirthIso}"
                     min="${minDateOfBirth}" max="${maxDateOfBirth}" required>
              <div class="invalid-feedback"><c:out value="${fieldErrors.dateOfBirth}" /></div>
            </div>

            <div class="col-6 col-md-3">
              <label for="bloodGroup" class="form-label">
                <fmt:message key="common.bloodGroup"/> <span style="color:var(--bd-crimson)">*</span>
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
                <fmt:message key="common.gender"/> <span style="color:var(--bd-crimson)">*</span>
              </label>
              <select class="form-select ${not empty fieldErrors.gender ? 'is-invalid' : ''}"
                      id="gender" name="gender" required>
                <option value=""><fmt:message key="donor.choose"/></option>
                <c:forEach var="g" items="${genders}">
                  <option value="${g}" ${donor.gender eq g ? 'selected' : ''}>
                    <fmt:message key="${g.messageKey}"/>
                  </option>
                </c:forEach>
              </select>
              <div class="invalid-feedback"><c:out value="${fieldErrors.gender}" /></div>
            </div>

            <div class="col-12 col-md-6">
              <label for="contactNo" class="form-label">
                <fmt:message key="donor.contactNumber"/> <span style="color:var(--bd-crimson)">*</span>
              </label>
              <input type="tel" class="form-control ${not empty fieldErrors.contactNo ? 'is-invalid' : ''}"
                     id="contactNo" name="contactNo" value="${fn:escapeXml(donor.contactNo)}"
                     maxlength="20" required>
              <div class="invalid-feedback"><c:out value="${fieldErrors.contactNo}" /></div>
            </div>

            <div class="col-12">
              <label for="address" class="form-label"><fmt:message key="common.address"/></label>
              <textarea class="form-control ${not empty fieldErrors.address ? 'is-invalid' : ''}"
                        id="address" name="address" rows="2" maxlength="255"
                        placeholder="${fn:escapeXml(lblAddressHint)}"><c:out value="${donor.address}" /></textarea>
              <div class="invalid-feedback"><c:out value="${fieldErrors.address}" /></div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <%-- ---- photograph and PIN ---- --%>
    <div class="col-12 col-lg-4">
      <div class="card mb-3">
        <div class="card-header"><fmt:message key="donor.photograph"/></div>
        <div class="card-body">
          <img id="photoPreview" class="d-none mb-3"
               alt="${fn:escapeXml(lblPhotoPreview)}"
               style="width:100%;aspect-ratio:3/4;object-fit:cover;border-radius:var(--bd-r);
                      border:1px solid var(--bd-line)">
          <input type="file" class="form-control" id="photo" name="photo"
                 accept="image/jpeg,image/png">
          <div class="form-text mt-2">
            <fmt:message key="donor.photoHint"/>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-header"><fmt:message key="donor.pinOptional"/></div>
        <div class="card-body">
          <label for="pin" class="form-label"><fmt:message key="donor.pinFourDigit"/></label>
          <input type="password" class="form-control bdcms-pin ${not empty fieldErrors.pin ? 'is-invalid' : ''}"
                 id="pin" name="pin" inputmode="numeric" pattern="[0-9]{4}"
                 maxlength="4" autocomplete="off">
          <div class="invalid-feedback"><c:out value="${fieldErrors.pin}" /></div>
          <div class="form-text mt-2">
            <fmt:message key="donor.pinHint"/>
          </div>
        </div>
      </div>
    </div>
  </div>

  <div class="d-flex flex-wrap align-items-center gap-3 mt-4">
    <button type="submit" class="btn btn-danger btn-lg"><fmt:message key="donor.registerButton"/></button>
    <a href="${ctx}/app/donors" class="btn btn-quiet btn-lg"><fmt:message key="common.cancel"/></a>
    <c:if test="${not empty duplicate}">
      <span style="font:400 12.5px/1.4 var(--bd-sans);color:var(--bd-ink-50)">
        <fmt:message key="donor.blockedUntilResolved"/>
      </span>
    </c:if>
  </div>
</form>

<%@ include file="../layout/footer.jspf" %>
