<%@ include file="../layout/taglibs.jspf" %>
<%--
  FR-1: staff account management.

  Accounts are disabled, never deleted: every donation and every verification
  attempt names the account responsible, so removing a row would break the audit
  trail the system exists to provide.
--%>
<c:set var="pageTitle" value="Staff accounts" scope="request" />
<c:set var="nav" value="admin" scope="request" />
<%@ include file="../layout/header.jspf" %>

<div class="mb-4">
  <h1 class="bd-page-title">Staff accounts</h1>
  <p class="bd-page-sub">
    Accounts are disabled rather than deleted &mdash; every donation and
    verification names the account responsible
  </p>
</div>

<div class="row g-4">
  <div class="col-12 col-lg-8">
    <div class="card">
      <div class="card-header d-flex justify-content-between align-items-baseline">
        <span>Existing accounts</span>
        <span style="font:400 12px/1 var(--bd-mono);color:var(--bd-ink-50)">
          ${fn:length(users)} account(s)
        </span>
      </div>
      <div class="table-responsive">
        <table class="table table-hover mb-0">
          <thead class="table-light">
            <tr>
              <th>Username</th><th>Name</th><th>Role</th><th>Status</th>
              <th class="text-end">Actions</th>
            </tr>
          </thead>
          <tbody>
            <c:forEach var="u" items="${users}">
              <tr style="${u.active ? '' : 'opacity:.6'}">
                <td class="font-monospace" style="font-weight:500">
                  <c:out value="${u.username}" />
                </td>
                <td><c:out value="${u.fullName}" /></td>
                <td>
                  <span class="badge ${u.admin ? 'text-bg-danger' : 'text-bg-secondary'}">
                    <c:out value="${u.role.label}" />
                  </span>
                </td>
                <td>
                  <c:choose>
                    <c:when test="${u.active}">
                      <span class="d-inline-flex align-items-center gap-2">
                        <span class="bdcms-dot bdcms-dot-green"></span>
                        <span style="font:400 13px/1 var(--bd-sans)">Active</span>
                      </span>
                    </c:when>
                    <c:otherwise>
                      <span class="d-inline-flex align-items-center gap-2">
                        <span class="bdcms-dot bdcms-dot-grey"></span>
                        <span style="font:400 13px/1 var(--bd-sans);color:var(--bd-ink-50)">Disabled</span>
                      </span>
                    </c:otherwise>
                  </c:choose>
                </td>
                <td class="text-end text-nowrap">
                  <form method="post" action="${ctx}/app/admin/users" class="d-inline">
                    <input type="hidden" name="_csrf" value="${csrfToken}">
                    <input type="hidden" name="userId" value="${u.userId}">
                    <input type="hidden" name="action" value="${u.active ? 'disable' : 'enable'}">
                    <button type="submit" class="btn btn-sm btn-quiet">
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
                <td colspan="5" style="background:var(--bd-surface-2)">
                  <form method="post" action="${ctx}/app/admin/users"
                        class="row g-2 align-items-end">
                    <input type="hidden" name="_csrf" value="${csrfToken}">
                    <input type="hidden" name="userId" value="${u.userId}">
                    <input type="hidden" name="action" value="resetPassword">
                    <div class="col-12 col-md-5">
                      <label class="form-label" for="pw-${u.userId}">
                        New password for <c:out value="${u.username}" />
                      </label>
                      <input type="password" class="form-control form-control-sm"
                             id="pw-${u.userId}" name="password"
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
      <div class="card-footer" style="font:400 12.5px/1.5 var(--bd-sans);color:var(--bd-ink-50)">
        Passwords are stored as BCrypt hashes. Nobody &mdash; including an
        administrator &mdash; can read an existing password; it can only be replaced.
      </div>
    </div>
  </div>

  <div class="col-12 col-lg-4">
    <div class="card">
      <div class="card-header">Create an account</div>
      <div class="card-body">
        <form method="post" action="${ctx}/app/admin/users">
          <input type="hidden" name="_csrf" value="${csrfToken}">
          <input type="hidden" name="action" value="create">

          <div class="mb-3">
            <label for="username" class="form-label">Username</label>
            <input type="text" class="form-control font-monospace" id="username"
                   name="username" maxlength="40" required autocomplete="off">
            <div class="form-text">3&ndash;40 letters, digits, dots, hyphens or underscores.</div>
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
            <div class="form-text">
              Administrators additionally reach reports, settings and this screen.
            </div>
          </div>

          <div class="mb-4">
            <label for="password" class="form-label">Initial password</label>
            <input type="password" class="form-control" id="password" name="password"
                   minlength="8" required autocomplete="new-password">
            <div class="form-text">At least 8 characters. Ask the user to change it after signing in.</div>
          </div>

          <button type="submit" class="btn btn-danger w-100">Create account</button>
        </form>
      </div>
    </div>
  </div>
</div>

<%@ include file="../layout/footer.jspf" %>
