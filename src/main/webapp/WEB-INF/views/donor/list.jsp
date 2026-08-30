<%@ include file="../layout/taglibs.jspf" %>
<%-- FR-2: search, view and manage donor information. --%>
<fmt:message var="pageTitle" key="common.donors" scope="request"/>
<fmt:message var="lblSearchPlaceholder" key="donor.searchPlaceholder"/>
<fmt:message var="lblDonorPages" key="donor.pagesNav"/>
<c:set var="nav" value="donors" scope="request" />
<%@ include file="../layout/header.jspf" %>

<div class="d-flex flex-wrap align-items-end justify-content-between gap-2 mb-4">
  <div>
    <h1 class="bd-page-title"><fmt:message key="common.donors"/></h1>
    <p class="bd-page-sub">
      <fmt:message key="donor.matching"><fmt:param value="${total}"/></fmt:message>
    </p>
  </div>
  <a href="${ctx}/app/donors/register" class="btn btn-danger"><fmt:message key="donor.registerButton"/></a>
</div>

<div class="card mb-3">
  <div class="card-body">
    <form method="get" action="${ctx}/app/donors" class="row g-2 align-items-end">
      <div class="col-12 col-md-6">
        <label for="q" class="form-label"><fmt:message key="common.search"/></label>
        <input type="text" class="form-control" id="q" name="q" value="${fn:escapeXml(query)}"
               placeholder="${fn:escapeXml(lblSearchPlaceholder)}">
      </div>
      <div class="col-8 col-md-3">
        <label for="bloodGroupFilter" class="form-label"><fmt:message key="common.bloodGroup"/></label>
        <select class="form-select" id="bloodGroupFilter" name="bloodGroup">
          <option value=""><fmt:message key="donor.allGroups"/></option>
          <c:forEach var="bg" items="${bloodGroups}">
            <option value="${bg}" ${bloodGroup eq bg ? 'selected' : ''}><c:out value="${bg.label}" /></option>
          </c:forEach>
        </select>
      </div>
      <div class="col-4 col-md-auto">
        <button type="submit" class="btn btn-outline-secondary w-100"><fmt:message key="common.search"/></button>
      </div>
      <c:if test="${not empty query or not empty bloodGroup}">
        <div class="col-auto">
          <a href="${ctx}/app/donors" class="btn btn-quiet"><fmt:message key="common.reset"/></a>
        </div>
      </c:if>
    </form>
  </div>
</div>

<div class="card">
  <div class="table-responsive">
    <table class="table table-hover mb-0">
      <thead class="table-light">
        <tr>
          <th style="width:60px"></th>
          <th><fmt:message key="common.name"/></th>
          <th><fmt:message key="common.nationalId"/></th>
          <th><fmt:message key="donor.group"/></th>
          <th><fmt:message key="common.contact"/></th>
          <th><fmt:message key="common.registered"/></th>
          <th class="text-end"><fmt:message key="common.actions"/></th>
        </tr>
      </thead>
      <tbody>
        <c:forEach var="d" items="${donors}">
          <tr>
            <td>
              <fmt:message var="lblRowPhoto" key="verify.photoAlt">
                <fmt:param value="${d.fullName}"/>
              </fmt:message>
              <img src="${ctx}/app/donors/photo?donorId=${d.donorId}"
                   alt="${fn:escapeXml(lblRowPhoto)}" class="bdcms-avatar">
            </td>
            <td>
              <a href="${ctx}/app/donors/view?donorId=${d.donorId}" style="font-weight:600">
                <c:out value="${d.fullName}" />
              </a>
              <div style="font:400 12px/1 var(--bd-sans);color:var(--bd-ink-50);margin-top:4px">
                ${d.age} <fmt:message key="common.years"/> &middot; <fmt:message key="${d.gender.messageKey}"/>
              </div>
            </td>
            <td class="font-monospace" style="font-size:12.5px"><c:out value="${d.nationalId}" /></td>
            <td><span class="badge text-bg-danger"><c:out value="${d.bloodGroup.label}" /></span></td>
            <td class="font-monospace" style="font-size:12.5px"><c:out value="${d.contactNo}" /></td>
            <td class="font-monospace" style="font-size:12.5px;white-space:nowrap;color:var(--bd-ink-60)">
              ${d.registeredOnDisplay}
            </td>
            <td class="text-end text-nowrap">
              <a href="${ctx}/app/cards?donorId=${d.donorId}" class="btn btn-sm btn-quiet"><fmt:message key="common.cards"/></a>
              <a href="${ctx}/app/donations/history?donorId=${d.donorId}"
                 class="btn btn-sm btn-quiet"><fmt:message key="verify.history"/></a>
            </td>
          </tr>
        </c:forEach>
        <c:if test="${empty donors}">
          <tr>
            <td colspan="7" class="text-center py-5" style="color:var(--bd-ink-50)">
              <fmt:message key="donor.noMatch"/>
              <a href="${ctx}/app/donors/register"><fmt:message key="donor.registerNew"/></a>.
            </td>
          </tr>
        </c:if>
      </tbody>
    </table>
  </div>

  <c:if test="${totalPages > 1}">
    <div class="card-footer">
      <nav aria-label="${fn:escapeXml(lblDonorPages)}">
        <ul class="pagination pagination-sm mb-0 justify-content-center">
          <c:forEach var="p" begin="1" end="${totalPages}">
            <li class="page-item ${p eq page ? 'active' : ''}">
              <a class="page-link"
                 href="${ctx}/app/donors?page=${p}&q=${fn:escapeXml(query)}&bloodGroup=${bloodGroup}">
                ${p}
              </a>
            </li>
          </c:forEach>
        </ul>
      </nav>
    </div>
  </c:if>
</div>

<%@ include file="../layout/footer.jspf" %>
