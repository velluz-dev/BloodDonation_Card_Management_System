<%@ include file="../layout/taglibs.jspf" %>
<%--
  The configurable business rules.

  This screen is the maintainability requirement in practice: the minimum
  donation interval is edited here and takes effect on the next eligibility
  check, with no code change and no redeployment.

  The interval is given its own panel rather than sitting as one row in a
  table of key/value pairs, because it is the one setting that decides whether
  a donor is turned away.
--%>
<c:set var="pageTitle" value="System settings" scope="request" />
<c:set var="nav" value="admin" scope="request" />
<%@ include file="../layout/header.jspf" %>

<div class="mb-4">
  <h1 class="bd-page-title">System settings</h1>
  <p class="bd-page-sub">
    Values held in the database rather than in code, so a centre can change them
    without a rebuild
  </p>
</div>

<div class="row g-4">
  <div class="col-12 col-lg-7">

    <%-- ---- the interval, given its own panel ---- --%>
    <div class="bdcms-verdict bdcms-verdict-eligible mb-4">
      <div class="bdcms-verdict-body">
        <div class="bd-eyebrow">Minimum donation interval</div>

        <div class="d-flex align-items-baseline gap-3 mt-2 mb-3">
          <span style="font:600 44px/1 var(--bd-sans);letter-spacing:-.02em">
            <c:out value="${settings[intervalKey]}" default="${defaultInterval}" />
          </span>
          <span style="font:400 17px/1 var(--bd-sans);color:var(--bd-ink-60)">days</span>
        </div>

        <p style="font:400 13.5px/1.55 var(--bd-sans);color:var(--bd-ink-60);max-width:460px">
          The number of days that must pass between one donation and the next.
          <code class="font-monospace">EligibilityService</code> reads this on every
          check, so a change here applies to the very next donor at the counter.
          The proposal's default is ${defaultInterval} days (12 weeks).
        </p>

        <form method="post" action="${ctx}/app/admin/settings" class="row g-2 align-items-end">
          <input type="hidden" name="_csrf" value="${csrfToken}">
          <input type="hidden" name="settingKey" value="${fn:escapeXml(intervalKey)}">
          <div class="col-auto">
            <label for="intervalValue" class="form-label">New interval (days)</label>
            <input type="number" class="form-control font-monospace" id="intervalValue"
                   name="settingValue" value="${fn:escapeXml(settings[intervalKey])}"
                   min="1" max="365" required style="width:140px">
          </div>
          <div class="col-auto">
            <button type="submit" class="btn btn-danger">Update interval</button>
          </div>
        </form>
      </div>
    </div>

    <%-- ---- everything else ---- --%>
    <div class="card">
      <div class="card-header">Other settings</div>
      <div class="table-responsive">
        <table class="table mb-0">
          <thead class="table-light">
            <tr><th>Setting</th><th>Value</th><th class="text-end">Change</th></tr>
          </thead>
          <tbody>
            <c:forEach var="entry" items="${settings}">
              <c:if test="${entry.key ne intervalKey}">
                <tr>
                  <td class="font-monospace" style="font-size:12.5px">
                    <c:out value="${entry.key}" />
                  </td>
                  <td style="font-weight:500"><c:out value="${entry.value}" /></td>
                  <td>
                    <form method="post" action="${ctx}/app/admin/settings"
                          class="row g-1 justify-content-end">
                      <input type="hidden" name="_csrf" value="${csrfToken}">
                      <input type="hidden" name="settingKey" value="${fn:escapeXml(entry.key)}">
                      <div class="col-7">
                        <label class="visually-hidden" for="v-${entry.key}">
                          Value for <c:out value="${entry.key}" />
                        </label>
                        <input type="text" class="form-control form-control-sm" id="v-${entry.key}"
                               name="settingValue" value="${fn:escapeXml(entry.value)}" required>
                      </div>
                      <div class="col-auto">
                        <button type="submit" class="btn btn-sm btn-quiet">Save</button>
                      </div>
                    </form>
                  </td>
                </tr>
              </c:if>
            </c:forEach>
          </tbody>
        </table>
      </div>
    </div>
  </div>

  <div class="col-12 col-lg-5">
    <div class="card">
      <div class="card-header">Why these live in the database</div>
      <div class="card-body" style="font:400 13px/1.6 var(--bd-sans);color:var(--bd-ink-60)">
        <p>
          The donation waiting period is a clinical rule that varies by centre and
          by donation type. Holding it as a constant in the eligibility code would
          mean a recompile and a redeployment every time it changed &mdash; and a
          divergence between centres running different builds.
        </p>
        <p class="mb-0">
          Reports evaluate eligibility against this same value rather than a
          hard-coded 84, so a report and the counter can never disagree.
        </p>
      </div>
    </div>

    <div class="alert alert-warning mt-3">
      <strong>Care with the interval.</strong> Lowering it permits donors to give
      blood sooner, which is a donor-safety decision, not an administrative one.
      Accepted range: 1 to 365 days.
    </div>
  </div>
</div>

<%@ include file="../layout/footer.jspf" %>
