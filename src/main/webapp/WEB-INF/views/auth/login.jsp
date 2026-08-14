<%@ include file="../layout/taglibs.jspf" %>
<%--
  FR-1: staff sign-in.

  Split screen from the mockups: the dark half carries the product claim, the
  light half the form. Nothing competes with the two fields.

  This page does not use the shared header - it has no navigation bar, because
  there is nowhere to navigate to before signing in.
--%>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta name="robots" content="noindex, nofollow">
  <title>Sign in &middot; BDCMS</title>
  <link rel="stylesheet" href="${ctx}/assets/vendor/bootstrap/bootstrap.min.css">
  <link rel="stylesheet" href="${ctx}/assets/css/bdcms.css">
  <link rel="icon" href="${ctx}/assets/img/favicon.svg" type="image/svg+xml">
</head>
<body class="bdcms">

<div class="bdcms-login">

  <div class="bdcms-login-brand">
    <div class="d-flex align-items-center gap-2">
      <span class="bdcms-drop" aria-hidden="true"></span>
      <span style="font:500 12px/1 var(--bd-mono);letter-spacing:.14em">BDCMS</span>
    </div>

    <div>
      <div class="bdcms-login-claim">One donor.<br>One record.<br>One card.</div>
      <div class="bdcms-login-claim-sub">
        Centralised donor identity, verified at the point of donation.
      </div>
    </div>

    <div class="bdcms-login-foot">
      Blood Donation Card Management System<br>
      Section D &middot; Group 3 &middot; Jakarta EE on Tomcat 10.1
    </div>
  </div>

  <div class="bdcms-login-panel">
    <div class="bdcms-login-form">

      <div class="mb-4">
        <h1 style="font:600 26px/1.2 var(--bd-sans);letter-spacing:-.015em;margin:0">Sign in</h1>
        <p style="font:400 14px/1.5 var(--bd-sans);color:var(--bd-ink-55,rgba(23,21,15,.55));margin:6px 0 0">
          Use your centre-issued staff account.
        </p>
      </div>

      <c:if test="${not empty errorMessage}">
        <div class="alert alert-danger py-2 px-3 mb-3"><c:out value="${errorMessage}" /></div>
      </c:if>
      <c:if test="${not empty infoMessage}">
        <div class="alert alert-info py-2 px-3 mb-3"><c:out value="${infoMessage}" /></div>
      </c:if>

      <form method="post" action="${ctx}/login" autocomplete="off" novalidate>
        <input type="hidden" name="_csrf" value="${csrfToken}">
        <c:if test="${not empty next}">
          <input type="hidden" name="next" value="${fn:escapeXml(next)}">
        </c:if>

        <div class="mb-3">
          <label for="username" class="form-label">Username</label>
          <input type="text" class="form-control font-monospace" id="username" name="username"
                 value="${fn:escapeXml(username)}" required autofocus
                 maxlength="40" autocomplete="username">
        </div>

        <div class="mb-4">
          <label for="password" class="form-label">Password</label>
          <input type="password" class="form-control" id="password" name="password"
                 required autocomplete="current-password">
        </div>

        <button type="submit" class="btn btn-danger w-100 btn-lg">Sign in</button>
      </form>

      <div class="d-flex gap-2 align-items-start mt-4"
           style="padding:13px 15px;background:var(--bd-wash);border-radius:var(--bd-r)">
        <span class="bdcms-dot bdcms-dot-grey" style="width:5px;height:5px;margin-top:7px"></span>
        <div style="font:400 12.5px/1.5 var(--bd-sans);color:rgba(23,21,15,.55)">
          Every sign-in, verification attempt and donation record is written to the
          audit log with your account and timestamp. Sessions end after 20 minutes
          of inactivity.
        </div>
      </div>

    </div>
  </div>
</div>

<script src="${ctx}/assets/vendor/bootstrap/bootstrap.bundle.min.js"></script>
</body>
</html>
