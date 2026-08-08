<%@ include file="../layout/taglibs.jspf" %>
<%-- FR-1: staff sign-in. --%>
<c:set var="pageTitle" value="Sign in" scope="request" />
<%@ include file="../layout/header.jspf" %>

<div class="row justify-content-center align-items-center bdcms-login-row">
  <div class="col-12 col-sm-10 col-md-7 col-lg-5 col-xl-4">

    <div class="text-center mb-4">
      <span class="bdcms-drop bdcms-drop-lg d-inline-block mb-2" aria-hidden="true"></span>
      <h1 class="h4 fw-bold mb-1">Blood Donation Card Management</h1>
      <p class="text-muted small mb-0">Centralised donor, card and eligibility platform</p>
    </div>

    <div class="card shadow-sm">
      <div class="card-body p-4">
        <h2 class="h6 text-uppercase text-muted mb-3">Staff sign in</h2>

        <form method="post" action="${ctx}/login" autocomplete="off" novalidate>
          <input type="hidden" name="_csrf" value="${csrfToken}">
          <c:if test="${not empty next}">
            <input type="hidden" name="next" value="${fn:escapeXml(next)}">
          </c:if>

          <div class="mb-3">
            <label for="username" class="form-label">Username</label>
            <input type="text" class="form-control" id="username" name="username"
                   value="${fn:escapeXml(username)}" required autofocus
                   maxlength="40" autocomplete="username">
          </div>

          <div class="mb-4">
            <label for="password" class="form-label">Password</label>
            <input type="password" class="form-control" id="password" name="password"
                   required autocomplete="current-password">
          </div>

          <button type="submit" class="btn btn-danger w-100 py-2">Sign in</button>
        </form>
      </div>
    </div>

    <p class="text-center text-muted small mt-3 mb-0">
      Sessions end automatically after 20 minutes of inactivity.
    </p>
  </div>
</div>

<%@ include file="../layout/footer.jspf" %>
