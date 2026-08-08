<%@ include file="../layout/taglibs.jspf" %>
<%--
  The configurable business rules.

  This screen is the maintainability requirement in practice: the minimum
  donation interval is edited here and takes effect on the next eligibility
  check, with no code change and no redeployment.
--%>
<c:set var="pageTitle" value="System settings" scope="request" />
<c:set var="nav" value="admin" scope="request" />
<%@ include file="../layout/header.jspf" %>

<h1 class="h3 mb-1">System settings</h1>
<p class="text-muted">
  Values held in the database rather than in code, so a centre can change them
  without a rebuild.
</p>

<div class="row g-4">
  <div class="col-12 col-lg-7">
    <div class="card">
      <div class="card-header bg-white fw-semibold">Current settings</div>
      <div class="table-responsive">
        <table class="table align-middle mb-0">
          <thead class="table-light">
            <tr><th>Setting</th><th>Value</th><th class="text-end">Change</th></tr>
          </thead>
          <tbody>
            <c:forEach var="entry" items="${settings}">
              <tr>
                <td>
                  <div class="font-monospace small"><c:out value="${entry.key}" /></div>
                  <c:if test="${entry.key eq intervalKey}">
                    <div class="small text-muted">
                      Minimum days between whole-blood donations.
                      Proposal default: ${defaultInterval}.
                    </div>
                  </c:if>
                </td>
                <td class="fw-semibold"><c:out value="${entry.value}" /></td>
                <td>
                  <form method="post" action="${ctx}/app/admin/settings" class="row g-1 justify-content-end">
                    <input type="hidden" name="_csrf" value="${csrfToken}">
                    <input type="hidden" name="settingKey" value="${fn:escapeXml(entry.key)}">
                    <div class="col-7">
                      <input type="text" class="form-control form-control-sm" name="settingValue"
                             value="${fn:escapeXml(entry.value)}" required>
                    </div>
                    <div class="col-auto">
                      <button type="submit" class="btn btn-sm btn-outline-danger">Save</button>
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

  <div class="col-12 col-lg-5">
    <div class="card">
      <div class="card-header bg-white fw-semibold">Why these live in the database</div>
      <div class="card-body small">
        <p>
          The donation waiting period is a clinical rule that varies by centre and
          by donation type. Holding it as a constant in the eligibility code would
          mean a recompile and a redeployment every time it changed - and a
          divergence between centres running different builds.
        </p>
        <p class="mb-0">
          <code>EligibilityService</code> reads this value on every check, through a
          short-lived cache. A change here applies to the very next donor at the
          counter.
        </p>
      </div>
    </div>

    <div class="alert alert-warning small mt-3 mb-0">
      <strong>Care with the interval.</strong> Lowering it permits donors to give
      blood sooner, which is a donor-safety decision, not an administrative one.
      Accepted range: 1 to 365 days.
    </div>
  </div>
</div>

<%@ include file="../layout/footer.jspf" %>
