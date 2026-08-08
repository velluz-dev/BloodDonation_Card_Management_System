<%@ include file="../layout/taglibs.jspf" %>
<%-- FR-1: staff account management. Accounts are disabled, never deleted. --%>
<c:set var="pageTitle" value="Staff accounts" scope="request" />
<c:set var="nav" value="admin" scope="request" />
<%@ include file="../layout/header.jspf" %>

<h1 class="h3 mb-1">Staff accounts</h1>
<p class="text-muted">
  Accounts are disabled rather than deleted: every donation and every verification
  attempt names the account responsible, so removing one would break the audit trail.
</p>

<div class="row g-4">
  <div class="col-12 col-lg-8">
    <div class="card">
      <div class="card-header bg-white fw-semibold">Existing accounts</div>
      <div class="table-responsive">
        <table class="table table-hover align-middle mb-0">
          <thead class="table-light">
            <tr><th>Username</th><th>Name</th><th>Role</th><th>Status</th><th class="text-end">Actions</th></tr>
          </thead>
          <tbody>
            <c:forEach var="u" items="${users}">
              <tr class="${u.active ? '' : 'table-secondary'}">
                <td class="font-monospace"><c:out value="${u.username}" /></td>
                <td><c:out value="${u.fullName}" /></td>
                <td>
                  <span class="badge ${u.admin ? 'text-bg-danger' : 'text-bg-secondary'}">
                    <c:out value="${u.role.label}" />
                  </span>
                </td>
                <td>
                  <c:choose>
                    <c:when test="${u.active}"><span class="badge text-bg-success">Active</span></c:when>
                    <c:otherwise><span class="badge text-bg-secondary">Disabled</span></c:otherwise>
                  </c:choose>
                </td>
                <td class="text-end text-nowrap">
                  <form method="post" action="${ctx}/app/admin/users" class="d-inline">
                    <input type="hidden" name="_csrf" value="${csrfToken}">
                    <input type="hidden" name="userId" value="${u.userId}">
                    <input type="hidden" name="action" value="${u.active ? 'disable' : 'enable'}">
                    <button type="submit" class="btn btn-sm btn-outline-secondary">
                      ${u.active ? 'Disable' : 'Enable'}
                    </button>
                  </form>
                  <button type="button" class="btn btn-sm btn-outline-danger"
                          data-bs-toggle="collapse" data-bs-target="#reset-${u.userId}">
                    Reset password
                  </button>
                </td>
              </tr>
              <tr class="collapse" id="reset-${u.userId}">
                <td colspan="5" class="bg-light">
                  <form method="post" action="${ctx}/app/admin/users" class="row g-2 align-items-end">
                    <input type="hidden" name="_csrf" value="${csrfToken}">
                    <input type="hidden" name="userId" value="${u.userId}">
                    <input type="hidden" name="action" value="resetPassword">
                    <div class="col-12 col-md-5">
                      <label class="form-label small">
                        New password for <c:out value="${u.username}" />
                      </label>
                      <input type="password" class="form-control form-control-sm" name="password"
                             minlength="8" required autocomplete="new-password">
                    </div>
                    <div class="col-auto">
                      <button type="submit" class="btn btn-sm btn-danger">Reset</button>
                    </div>
                  </form>
                </td>
              </tr>
            </c:forEach>
          </tbody>
        </table>
      </div>
    </div>
  </div>

  <div class="col-12 col-lg-4">
    <div class="card">
      <div class="card-header bg-white fw-semibold">Create an account</div>
      <div class="card-body">
        <form method="post" action="${ctx}/app/admin/users">
          <input type="hidden" name="_csrf" value="${csrfToken}">
          <input type="hidden" name="action" value="create">

          <div class="mb-3">
            <label for="username" class="form-label">Username</label>
            <input type="text" class="form-control" id="username" name="username"
                   maxlength="40" required autocomplete="off">
          </div>
          <div class="mb-3">
            <label for="fullName" class="form-label">Full name</label>
            <input type="text" class="form-control" id="fullName" name="fullName"
                   maxlength="120" required>
          </div>
          <div class="mb-3">
            <label for="role" class="form-label">Role</label>
            <select class="form-select" id="role" name="role">
              <c:forEach var="r" items="${roles}">
                <option value="${r}"><c:out value="${r.label}" /></option>
              </c:forEach>
            </select>
          </div>
          <div class="mb-3">
            <label for="password" class="form-label">Initial password</label>
            <input type="password" class="form-control" id="password" name="password"
                   minlength="8" required autocomplete="new-password">
            <div class="form-text">
              At least 8 characters. Stored as a BCrypt hash, never in plain text.
            </div>
          </div>

          <button type="submit" class="btn btn-danger w-100">Create account</button>
        </form>
      </div>
    </div>
  </div>
</div>

<%@ include file="../layout/footer.jspf" %>
