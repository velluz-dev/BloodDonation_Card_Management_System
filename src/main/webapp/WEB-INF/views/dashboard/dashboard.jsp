<%@ include file="../layout/taglibs.jspf" %>
<%--
  Shift dashboard.

  Four counters, then the scan panel as the loudest object on the page -
  starting a donation is what staff are here to do - then recent activity, with
  centre settings and today's blood-group mix on a rail.
--%>
<c:set var="pageTitle" value="Dashboard" scope="request" />
<c:set var="nav" value="dashboard" scope="request" />
<c:set var="topTitle" value="${todayLong}" scope="request" />
<c:set var="topSub" scope="request">Signed in as ${fn:escapeXml(currentUser.fullName)} &middot; ${fn:escapeXml(centreName)}</c:set>
<%@ include file="../layout/header.jspf" %>

<div class="row g-4">
  <div class="col-12 col-xxl-9">

    <%-- ---- counters ---- --%>
    <div class="row g-3 mb-4">
      <div class="col-6 col-lg-3">
        <div class="bdcms-stat">
          <div class="bdcms-stat-head">
            <span class="bdcms-stat-badge"><svg class="ic" aria-hidden="true"><use href="#i-users"/></svg></span>
            <span class="bdcms-stat-label">Registered donors</span>
          </div>
          <div class="bdcms-stat-value">${donorCount}</div>
          <div class="bdcms-stat-foot">Total in system</div>
          <svg class="ic bdcms-stat-ghost" aria-hidden="true"><use href="#i-users"/></svg>
        </div>
      </div>

      <div class="col-6 col-lg-3">
        <div class="bdcms-stat">
          <div class="bdcms-stat-head">
            <span class="bdcms-stat-badge"><svg class="ic" aria-hidden="true"><use href="#i-card"/></svg></span>
            <span class="bdcms-stat-label">Active cards</span>
          </div>
          <div class="bdcms-stat-value">${activeCardCount}</div>
          <div class="bdcms-stat-foot">Currently usable at the counter</div>
          <svg class="ic bdcms-stat-ghost" aria-hidden="true"><use href="#i-card"/></svg>
        </div>
      </div>

      <div class="col-6 col-lg-3">
        <div class="bdcms-stat">
          <div class="bdcms-stat-head">
            <span class="bdcms-stat-badge"><svg class="ic" aria-hidden="true"><use href="#i-drop"/></svg></span>
            <span class="bdcms-stat-label">Donations this month</span>
          </div>
          <div class="bdcms-stat-value">${donationsThisMonth}</div>
          <div class="bdcms-stat-foot">Recorded since the 1st</div>
          <svg class="ic bdcms-stat-ghost" aria-hidden="true"><use href="#i-trend"/></svg>
        </div>
      </div>

      <div class="col-6 col-lg-3">
        <div class="bdcms-stat">
          <div class="bdcms-stat-head">
            <span class="bdcms-stat-badge"><svg class="ic" aria-hidden="true"><use href="#i-shield"/></svg></span>
            <span class="bdcms-stat-label">Verifications (7 days)</span>
          </div>
          <div class="bdcms-stat-value">${verificationsThisWeek}</div>
          <div class="bdcms-stat-foot">
            <c:choose>
              <c:when test="${failedVerifications > 0}">
                <span class="bdcms-stat-alert">${failedVerifications} failed</span>
              </c:when>
              <c:otherwise>None failed</c:otherwise>
            </c:choose>
          </div>
          <svg class="ic bdcms-stat-ghost" aria-hidden="true"><use href="#i-shield"/></svg>
        </div>
      </div>
    </div>

    <%-- ---- the scan panel ---- --%>
    <form method="get" action="${ctx}/app/verify" class="bdcms-scanbar mb-4">
      <div class="bdcms-scanbar-head">
        <span class="bdcms-scanbar-icon"><svg class="ic ic-lg" aria-hidden="true"><use href="#i-scan"/></svg></span>
        <div>
          <div class="bdcms-scanbar-title">Start a donation</div>
          <div class="bdcms-scanbar-sub">Scan the card QR or type the number</div>
        </div>
      </div>

      <div class="bdcms-scanfield">
        <label for="cardNo" class="visually-hidden">Card number</label>
        <input type="text" class="form-control" id="cardNo" name="cardNo"
               placeholder="BDC-2026-XXXXXXXX" autocomplete="off" autofocus
               maxlength="32" spellcheck="false">
        <svg class="ic" aria-hidden="true"><use href="#i-search"/></svg>
      </div>

      <button type="submit" class="btn btn-on-crimson">Look up donor</button>
    </form>

    <%-- ---- recent activity ---- --%>
    <div class="bd-section-head">
      <h2 class="bd-section-title">Recent activity</h2>
      <c:if test="${currentUser.admin}">
        <a class="bd-section-link" href="${ctx}/app/admin/verification-log">
          View audit log <svg class="ic ic-sm" aria-hidden="true"><use href="#i-arrow-right"/></svg>
        </a>
      </c:if>
    </div>

    <div class="card mb-4">
      <div class="table-responsive">
        <table class="table table-hover mb-0">
          <thead class="table-light">
            <tr>
              <th>When</th><th>Donor</th><th>Card</th><th>Action</th><th>Result</th><th></th>
            </tr>
          </thead>
          <tbody>
            <c:forEach var="d" items="${recentDonations}">
              <tr>
                <td style="white-space:nowrap">
                  <svg class="ic ic-sm me-1" aria-hidden="true" style="color:var(--bd-faint)"><use href="#i-clock"/></svg>
                  ${d.donationDateDisplay}
                </td>
                <td>
                  <a href="${ctx}/app/donations/history?donorId=${d.donorId}">
                    <c:out value="${d.donorName}" />
                  </a>
                </td>
                <td class="font-monospace" style="font-size:12.5px"><c:out value="${d.cardNo}" /></td>
                <td>Donation</td>
                <td><span class="badge text-bg-success">Recorded</span></td>
                <td class="text-end">
                  <a href="${ctx}/app/donations/history?donorId=${d.donorId}" aria-label="Open donor history">
                    <svg class="ic bd-row-go" aria-hidden="true"><use href="#i-chev-right"/></svg>
                  </a>
                </td>
              </tr>
            </c:forEach>

            <c:forEach var="a" items="${recentAttempts}">
              <c:if test="${not a.result.success}">
                <tr>
                  <td style="white-space:nowrap">
                    <svg class="ic ic-sm me-1" aria-hidden="true" style="color:var(--bd-faint)"><use href="#i-clock"/></svg>
                    ${a.attemptedAtDisplay}
                  </td>
                  <td>
                    <c:choose>
                      <c:when test="${not empty a.donorId}">
                        <a href="${ctx}/app/donations/history?donorId=${a.donorId}">
                          <c:out value="${a.donorName}" />
                        </a>
                      </c:when>
                      <c:otherwise>
                        <span style="color:var(--bd-faint)">&mdash; unmatched &mdash;</span>
                      </c:otherwise>
                    </c:choose>
                  </td>
                  <td class="font-monospace" style="font-size:12.5px"><c:out value="${a.cardNo}" /></td>
                  <td>Verification</td>
                  <td>
                    <span class="badge text-bg-${a.result.badgeClass}">
                      <c:out value="${a.result.label}" />
                    </span>
                  </td>
                  <td></td>
                </tr>
              </c:if>
            </c:forEach>

            <c:if test="${empty recentDonations and empty recentAttempts}">
              <tr>
                <td colspan="6" class="text-center py-5" style="color:var(--bd-muted)">
                  Nothing recorded yet. Scan a card above to begin.
                </td>
              </tr>
            </c:if>
          </tbody>
        </table>
      </div>
    </div>
  </div>

  <%-- ---- right rail ---- --%>
  <div class="col-12 col-xxl-3">
    <div class="bdcms-rail mb-3">
      <div class="bdcms-rail-head">
        <svg class="ic" aria-hidden="true"><use href="#i-gear"/></svg> Centre settings
      </div>

      <div style="padding-bottom:16px;border-bottom:1px solid var(--bd-line-soft)">
        <div style="font:400 13px/1 var(--bd-sans);color:var(--bd-muted)">
          Minimum donation interval
        </div>
        <div class="bdcms-rail-big">${intervalDays} days</div>
        <div style="font:400 12.5px/1.5 var(--bd-sans);color:var(--bd-muted)">
          Configurable per centre &mdash; held in settings, not in code
        </div>
      </div>

      <div class="bd-eyebrow mt-3 mb-3">Donors by blood group</div>
      <div class="d-flex flex-column gap-2">
        <c:forEach var="g" items="${bloodGroupMix}">
          <div class="bdcms-bar-row">
            <span class="bdcms-bar-label ${g.share < 25 ? 'bdcms-bar-label-muted' : ''}">
              <c:out value="${g.label}" />
            </span>
            <div class="bdcms-bar-track">
              <div class="bdcms-bar-fill ${g.share < 25 ? 'bdcms-bar-fill-3' : ''}"
                   style="width:${g.share}%"></div>
            </div>
            <span class="bdcms-bar-value">${g.count}</span>
          </div>
        </c:forEach>
        <c:if test="${empty bloodGroupMix}">
          <div style="font:400 12.5px/1.5 var(--bd-sans);color:var(--bd-muted)">
            No donors registered yet.
          </div>
        </c:if>
      </div>
    </div>

    <c:if test="${lostCardCount > 0}">
      <div class="card">
        <div class="card-body">
          <span class="bdcms-stat-badge mb-3"><svg class="ic" aria-hidden="true"><use href="#i-alert-file"/></svg></span>
          <div style="font:600 15px/1.3 var(--bd-sans);color:var(--bd-ink)">
            ${lostCardCount} card<c:if test="${lostCardCount != 1}">s</c:if> reported lost
          </div>
          <div style="font:400 13px/1.5 var(--bd-sans);color:var(--bd-muted);margin-top:7px">
            Set to INACTIVE. Donation history stays on the donor record.
          </div>
          <a class="btn btn-sm btn-outline-danger mt-3" href="${ctx}/app/donors">View details</a>
        </div>
      </div>
    </c:if>
  </div>
</div>

<%@ include file="../layout/footer.jspf" %>
