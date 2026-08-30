<%@ include file="../layout/taglibs.jspf" %>
<%--
  FR-1: staff account management.

  Accounts are disabled, never deleted: every donation and every verification
  attempt names the account responsible, so removing a row would break the audit
  trail the system exists to provide.
--%>
<fmt:message var="pageTitle" key="users.title" scope="request"/>
<c:set var="nav" value="admin" scope="request" />
<%@ include file="../layout/header.jspf" %>

<div class="mb-4">
  <h1 class="bd-page-title"><fmt:message key="users.title"/></h1>
  <p class="bd-page-sub">
    <fmt:message key="users.subtitle"/>
  </p>
</div>

<div class="row g-4">
  <div class="col-12 col-lg-8">
    <div class="card">
      <div class="card-header d-flex justify-content-between align-items-baseline">
        <span><fmt:message key="users.existing"/></span>
        <span style="font:400 12px/1 var(--bd-mono);color:var(--bd-ink-50)">
          <fmt:message key="users.accounts">
            <fmt:param value="${fn:length(users)}"/>
          </fmt:message>
        </span>
      </div>
      <div class="table-responsive">
        <table class="table table-hover mb-0">
          <thead class="table-light">
            <tr>
              <th><fmt:message key="users.username"/></th>
              <th><fmt:message key="common.name"/></th>
              <th><fmt:message key="users.role"/></th>
              <th><fmt:message key="common.status"/></th>
              <th class="text-end"><fmt:message key="common.actions"/></th>
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
                    <fmt:message key="${u.role.messageKey}"/>
                  </span>
                </td>
                <td>
                  <c:choose>
                    <c:when test="${u.active}">
                      <span class="d-inline-flex align-items-center gap-2">
                        <span class="bdcms-dot bdcms-dot-green"></span>
                        <span style="font:400 13px/1 var(--bd-sans)"><fmt:message key="common.active"/></span>
                      </span>
                    </c:when>
                    <c:otherwise>
                      <span class="d-inline-flex align-items-center gap-2">
                        <span class="bdcms-dot bdcms-dot-grey"></span>
                        <span style="font:400 13px/1 var(--bd-sans);color:var(--bd-ink-50)"><fmt:message key="common.disabled"/></span>
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
                      <c:choose>
                        <c:when test="${u.active}"><fmt:message key="users.disable"/></c:when>
                        <c:otherwise><fmt:message key="users.enable"/></c:otherwise>
                      </c:choose>
                    </button>
                  </form>
                  <button type="button" class="btn btn-sm btn-outline-danger"
                          data-bs-toggle="collapse" data-bs-target="#reset-${u.userId}">
                    <fmt:message key="users.resetPassword"/>
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
                        <fmt:message key="users.newPasswordFor">
                          <fmt:param value="${u.username}"/>
                        </fmt:message>
                      </label>
                      <input type="password" class="form-control form-control-sm"
                             id="pw-${u.userId}" name="password"
                             minlength="8" required autocomplete="new-password">
                    </div>
                    <div class="col-auto">
                      <button type="submit" class="btn btn-sm btn-danger"><fmt:message key="users.reset"/></button>
                    </div>
                  </form>
                </td>
              </tr>
            </c:forEach>
          </tbody>
        </table>
      </div>
      <div class="card-footer" style="font:400 12.5px/1.5 var(--bd-sans);color:var(--bd-ink-50)">
        <fmt:message key="users.hashNote"/>
      </div>
    </div>
  </div>

  <div class="col-12 col-lg-4">
    <div class="card">
      <div class="card-header"><fmt:message key="users.create"/></div>
      <div class="card-body">
        <form method="post" action="${ctx}/app/admin/users">
          <input type="hidden" name="_csrf" value="${csrfToken}">
          <input type="hidden" name="action" value="create">

          <div class="mb-3">
            <label for="username" class="form-label"><fmt:message key="users.username"/></label>
            <input type="text" class="form-control font-monospace" id="username"
                   name="username" maxlength="40" required autocomplete="off">
            <div class="form-text"><fmt:message key="users.usernameHint"/></div>
          </div>

          <div class="mb-3">
            <label for="fullName" class="form-label"><fmt:message key="donor.fullName"/></label>
            <input type="text" class="form-control" id="fullName" name="fullName"
                   maxlength="120" required>
          </div>

          <div class="mb-3">
            <label for="role" class="form-label"><fmt:message key="users.role"/></label>
            <select class="form-select" id="role" name="role">
              <c:forEach var="r" items="${roles}">
                <option value="${r}"><fmt:message key="${r.messageKey}"/></option>
              </c:forEach>
            </select>
            <div class="form-text">
              <fmt:message key="users.roleHint"/>
            </div>
          </div>

          <div class="mb-4">
            <label for="password" class="form-label"><fmt:message key="users.initialPassword"/></label>
            <input type="password" class="form-control" id="password" name="password"
                   minlength="8" required autocomplete="new-password">
            <div class="form-text"><fmt:message key="users.passwordHint"/></div>
          </div>

          <button type="submit" class="btn btn-danger w-100"><fmt:message key="users.createButton"/></button>
        </form>
      </div>
    </div>
  </div>
</div>

<%@ include file="../layout/footer.jspf" %>
