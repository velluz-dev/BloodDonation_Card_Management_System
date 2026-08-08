<%@ include file="../layout/taglibs.jspf" %>
<%-- FR-2: search, view and manage donor information. --%>
<c:set var="pageTitle" value="Donors" scope="request" />
<c:set var="nav" value="donors" scope="request" />
<%@ include file="../layout/header.jspf" %>

<div class="d-flex flex-wrap justify-content-between align-items-center mb-3 gap-2">
  <div>
    <h1 class="h3 mb-1">Donors</h1>
    <p class="text-muted mb-0">${total} donor(s) match the current filter.</p>
  </div>
  <a href="${ctx}/app/donors/register" class="btn btn-danger">Register donor</a>
</div>

<div class="card mb-3">
  <div class="card-body">
    <form method="get" action="${ctx}/app/donors" class="row g-2 align-items-end">
      <div class="col-12 col-md-6">
        <label for="q" class="form-label">Search</label>
        <input type="text" class="form-control" id="q" name="q" value="${fn:escapeXml(query)}"
               placeholder="Name, national ID or contact number">
      </div>
      <div class="col-8 col-md-3">
        <label for="bloodGroupFilter" class="form-label">Blood group</label>
        <select class="form-select" id="bloodGroupFilter" name="bloodGroup">
          <option value="">All groups</option>
          <c:forEach var="bg" items="${bloodGroups}">
            <option value="${bg}" ${bloodGroup eq bg ? 'selected' : ''}><c:out value="${bg.label}" /></option>
          </c:forEach>
        </select>
      </div>
      <div class="col-4 col-md-auto">
        <button type="submit" class="btn btn-outline-danger w-100">Search</button>
      </div>
      <c:if test="${not empty query or not empty bloodGroup}">
        <div class="col-auto">
          <a href="${ctx}/app/donors" class="btn btn-outline-secondary">Reset</a>
        </div>
      </c:if>
    </form>
  </div>
</div>

<div class="card">
  <div class="table-responsive">
    <table class="table table-hover align-middle mb-0">
      <thead class="table-light">
        <tr>
          <th style="width:64px"></th>
          <th>Name</th>
          <th>National ID</th>
          <th>Group</th>
          <th>Contact</th>
          <th>Registered</th>
          <th class="text-end">Actions</th>
        </tr>
      </thead>
      <tbody>
        <c:forEach var="d" items="${donors}">
          <tr>
            <td>
              <img src="${ctx}/app/donors/photo?donorId=${d.donorId}"
                   alt="Photograph of ${fn:escapeXml(d.fullName)}" class="bdcms-avatar">
            </td>
            <td>
              <a href="${ctx}/app/donors/view?donorId=${d.donorId}" class="fw-semibold">
                <c:out value="${d.fullName}" />
              </a>
              <div class="small text-muted">${d.age} yrs &middot; <c:out value="${d.gender.label}" /></div>
            </td>
            <td class="font-monospace small"><c:out value="${d.nationalId}" /></td>
            <td><span class="badge text-bg-danger"><c:out value="${d.bloodGroup.label}" /></span></td>
            <td class="small"><c:out value="${d.contactNo}" /></td>
            <td class="small text-nowrap">${d.registeredOnDisplay}</td>
            <td class="text-end text-nowrap">
              <a href="${ctx}/app/cards?donorId=${d.donorId}"
                 class="btn btn-sm btn-outline-secondary">Cards</a>
              <a href="${ctx}/app/donations/history?donorId=${d.donorId}"
                 class="btn btn-sm btn-outline-secondary">History</a>
            </td>
          </tr>
        </c:forEach>
        <c:if test="${empty donors}">
          <tr>
            <td colspan="7" class="text-center text-muted py-5">
              No donors match this search.
              <a href="${ctx}/app/donors/register">Register a new donor</a>.
            </td>
          </tr>
        </c:if>
      </tbody>
    </table>
  </div>

  <c:if test="${totalPages > 1}">
    <div class="card-footer bg-white">
      <nav aria-label="Donor pages">
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
