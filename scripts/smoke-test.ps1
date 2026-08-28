# ---------------------------------------------------------------------------
# End-to-end smoke test against a running instance.
#
#   .\scripts\smoke-test.ps1                 # defaults to http://127.0.0.1:8081/bdcms
#   .\scripts\smoke-test.ps1 -Base http://localhost:9090/bdcms
#
# Requires the application running AND the database freshly seeded:
#
#   mysql ... < db/01_schema.sql
#   mysql ... < db/02_seed.sql
#
# The suite is NOT idempotent - it records a donation and deletes a donor, so
# re-running it without reseeding will report failures that are the system
# behaving correctly (a donor who just donated is no longer eligible).
#
# Windows PowerShell 5.1: Invoke-WebRequest throws on 4xx/5xx and disposes the
# error stream, so Req() catches the exception and reads ErrorDetails.Message.
# ---------------------------------------------------------------------------
param([string]$Base = 'http://127.0.0.1:8081/bdcms')

$s = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$script:pass = 0
$script:fail = 0

function Req {
    param([string]$Path, [string]$Method = 'Get', $Body = $null, [int]$MaxRedirect = 5)
    $a = @{ Uri = "$Base$Path"; Method = $Method; WebSession = $s
            UseBasicParsing = $true; TimeoutSec = 40; MaximumRedirection = $MaxRedirect }
    if ($Body) { $a.Body = $Body }
    try {
        $r = Invoke-WebRequest @a
        [pscustomobject]@{ Status = [int]$r.StatusCode; Content = $r.Content
                           Type = $r.Headers['Content-Type']; Length = $r.RawContentLength
                           Location = $r.Headers['Location'] }
    } catch [Net.WebException] {
        $resp = $_.Exception.Response
        if ($null -eq $resp) {
            return [pscustomobject]@{ Status = -1; Content = $_.Exception.Message
                                      Type = ''; Length = 0; Location = '' }
        }
        $body = ''
        if ($_.ErrorDetails -and $_.ErrorDetails.Message) { $body = $_.ErrorDetails.Message }
        if (-not $body) {
            try { $sr = New-Object IO.StreamReader($resp.GetResponseStream())
                  $body = $sr.ReadToEnd(); $sr.Close() } catch {}
        }
        [pscustomobject]@{ Status = [int]$resp.StatusCode; Content = $body
                           Type = $resp.Headers['Content-Type']; Length = $body.Length
                           Location = $resp.Headers['Location'] }
    }
}

function Token($html) { if ($html -match 'name="_csrf"\s+value="([^"]+)"') { $Matches[1] } }
function Check($name, $cond, $detail) {
    if ($cond) { $script:pass++; "PASS  $name" }
    else       { $script:fail++; "FAIL  $name  --> $detail" }
}
function Today { (Get-Date -Format 'yyyy-MM-dd') }

'=== 1. Access control ==='
$anon = Req '/app/dashboard' 'Get' $null 0
Check 'Anonymous /app/dashboard is redirected' ($anon.Status -eq 302) "status $($anon.Status)"
Check '  ...to login, destination preserved' ($anon.Location -match '/login\?expired=1&next=') "$($anon.Location)"

'=== 2. Login page and CSRF ==='
$login = Req '/login'
$csrf = Token $login.Content
Check 'Login page renders' ($login.Content -match 'One donor' -and $login.Content -match 'Sign in') 'heading missing'
Check 'CSRF token issued' ($null -ne $csrf -and $csrf.Length -gt 20) "token='$csrf'"
$bad = Req '/login' 'Post' @{ username='admin'; password='wrong'; _csrf=$csrf }
Check 'Wrong password rejected (401)' ($bad.Status -eq 401) "status $($bad.Status)"
Check '  ...message does not say which field was wrong' ($bad.Content -match 'Incorrect username or password') 'differs'
$noCsrf = Req '/login' 'Post' @{ username='admin'; password='Admin@123' }
Check 'CsrfFilter blocks POST with no token (403)' ($noCsrf.Status -eq 403) "status $($noCsrf.Status)"

# Demo credentials panel. Present by default; hidden when the build is started
# with -Dbdcms.demo.credentials=false, which this suite does not exercise.
Check 'Demo accounts panel is shown' ($login.Content -match 'Demonstration accounts') 'panel missing'
Check '  ...lists all three seeded accounts' (($login.Content -match 'Admin@123') -and ($login.Content -match 'Staff@123') -and ($login.Content -match 'Nurse@123')) 'accounts missing'
Check '  ...and is labelled as demo-only' ($login.Content -match 'remove before real use') 'no warning'

'=== 3. Sign in as admin ==='
$csrf = Token (Req '/login').Content
$ok = Req '/login' 'Post' @{ username='admin'; password='Admin@123'; _csrf=$csrf }
Check 'Login succeeds' ($ok.Status -eq 200) "status $($ok.Status)"
$dash = Req '/app/dashboard'
Check 'Dashboard renders' ($dash.Content -match 'Registered donors') 'stat missing'
Check '  ...admin sees the Reports menu' ($dash.Content -match 'Reports') 'menu missing'
Check '  ...webcam scanner is wired in' ($dash.Content -match 'bdcms-scanner.js') 'scanner script missing'

'=== 4. Donor search (FR-2) ==='
$donors = Req '/app/donors'
Check 'Donor list renders' ($donors.Content -match 'Hein Thu') 'missing donor'
$search = Req '/app/donors?q=Su+Su'
Check 'Search filters correctly' (($search.Content -match 'Su Su Aung') -and -not ($search.Content -match 'Kyaw Zin Latt')) 'filter wrong'

'=== 5. Verification screen (FR-4) ==='
$v = Req '/app/verify?cardNo=BDC-2026-6MPT5RG9'
Check 'Card resolves to the donor' ($v.Content -match 'Min Khant Zaw') 'donor not shown'
Check '  ...national ID is MASKED' ($v.Content -match '901E') 'masked tail missing'
Check '  ...FULL national ID never sent' (-not ($v.Content -match 'S5678901E')) '*** FULL ID LEAKED ***'
Check '  ...PIN demanded (donor set one)' ($v.Content -match 'Donor PIN') 'no PIN field'
Check '  ...explicit confirmation required' ($v.Content -match 'name="confirmed"') 'no confirm box'
$unknown = Req '/app/verify?cardNo=BDC-2026-0000XXXX'
Check 'Unknown card handled cleanly' ($unknown.Content -match 'No card matches that number') 'wrong message'
$lost = Req '/app/verify?cardNo=BDC-2026-1HN6CY4W'
Check 'LOST card refused' ($lost.Content -match 'cannot be used') 'not refused'

'=== 6. Recording a donation (FR-6) ==='
$csrf = Token (Req '/app/verify?cardNo=BDC-2026-6MPT5RG9').Content
$rec = Req '/app/donations/record' 'Post' @{ _csrf=$csrf; cardNo='BDC-2026-6MPT5RG9'; confirmed='on'
                                             pin='1234'; centre='Central Blood Bank'; volumeMl='450'
                                             donationDate=(Today) }
Check 'Donation recorded for an eligible donor' ($rec.Status -eq 200) "status $($rec.Status)"
Check '  ...redirected to the history' ($rec.Content -match 'Donation history') 'not history'
$csrf = Token (Req '/app/verify?cardNo=BDC-2026-4KDQ7M2P').Content
$badPin = Req '/app/donations/record' 'Post' @{ _csrf=$csrf; cardNo='BDC-2026-4KDQ7M2P'; confirmed='on'
                                                pin='9999'; centre='Central Blood Bank'; volumeMl='450'
                                                donationDate=(Today) }
Check 'Wrong PIN refused (400)' ($badPin.Status -eq 400) "status $($badPin.Status)"
Check '  ...with the PIN-specific message' ($badPin.Content -match 'PIN entered does not match') 'differs'
$csrf = Token (Req '/app/verify?cardNo=BDC-2026-4KDQ7M2P').Content
$noConf = Req '/app/donations/record' 'Post' @{ _csrf=$csrf; cardNo='BDC-2026-4KDQ7M2P'
                                                pin='1234'; centre='Central Blood Bank'; volumeMl='450'
                                                donationDate=(Today) }
Check 'Unconfirmed identity refused' ($noConf.Content -match 'must confirm the photograph') 'not refused'

'=== 7. Eligibility (FR-5) ==='
$csrf = Token (Req '/app/verify?cardNo=BDC-2026-9XR3B8VT').Content
$ne = Req '/app/donations/record' 'Post' @{ _csrf=$csrf; cardNo='BDC-2026-9XR3B8VT'; confirmed='on'
                                            pin='1234'; centre='Central Blood Bank'; volumeMl='450'
                                            donationDate=(Today) }
Check 'Donor inside the waiting period refused (409)' ($ne.Status -eq 409) "status $($ne.Status)"
Check '  ...30 of 84 days reported' ($ne.Content -match 'Only 30 of the required 84 days') 'explanation differs'
Check '  ...next permitted date given' ($ne.Content -match 'may next donate on') 'no date'
$b = Req '/app/donations/history?donorId=3'
Check 'Donor at exactly 84 days is ELIGIBLE' ($b.Content -match 'Eligible') 'not eligible'
$b4 = Req '/app/donations/history?donorId=4'
Check 'Donor at 83 days is NOT ELIGIBLE' ($b4.Content -match 'Not eligible') 'wrong verdict'

'=== 8. Cards, QR and PDF (FR-3) ==='
$cards = Req '/app/cards?donorId=3'
Check 'Card list shows the reissue trail' (($cards.Content -match 'Lost') -and ($cards.Content -match 'Active')) 'trail missing'
$qr = Req '/app/cards/qr?cardNo=BDC-2026-7QJ2FE5S&size=200'
Check 'QR code generated' ($qr.Type -match 'image/png') "type $($qr.Type)"
$cardPdf = Req '/app/cards/print?cardNo=BDC-2026-7QJ2FE5S&format=pdf'
Check 'Printable card PDF generated' (($cardPdf.Type -match 'application/pdf') -and $cardPdf.Length -gt 1000) "type $($cardPdf.Type)"
$photo = Req '/app/donors/photo?donorId=1'
Check 'Photo servlet responds' ($photo.Type -match 'image/') "type $($photo.Type)"

'=== 9. Reports (FR-7) ==='
foreach ($t in 'donations','blood-group','donor-activity') {
    $rep = Req "/app/reports?type=$t"
    Check "Report '$t' renders" ($rep.Status -eq 200) "status $($rep.Status)"
    $pdf = Req "/app/reports?type=$t&format=pdf"
    Check "  ...'$t' exports to PDF" (($pdf.Type -match 'application/pdf') -and $pdf.Length -gt 800) "type $($pdf.Type)"
}

'=== 10. Audit trail and settings ==='
$audit = Req '/app/admin/verification-log'
Check 'Audit log renders' ($audit.Content -match 'Verification audit log') 'missing'
Check '  ...successful verification recorded' ($audit.Content -match 'Verified') 'no success row'
Check '  ...PIN failure recorded' ($audit.Content -match 'PIN incorrect') 'no failure row'
$set = Req '/app/admin/settings'
Check 'Settings shows the configurable interval' ($set.Content -match 'min_donation_interval_days') 'missing'

'=== 11. Admin data management ==='
$dv = Req '/app/donors/view?donorId=1'
Check 'Admin sees the Delete donor control' ($dv.Content -match 'Delete donor') 'missing'
$csrf = Token $dv.Content
$guard = Req '/app/admin/donors/delete' 'Post' @{ _csrf=$csrf; donorId='1' }
Check 'Donor with donations cannot be deleted' ($guard.Content -match 'cannot be deleted') 'guard did not fire'
Check '  ...and still exists' ((Req '/app/donors/view?donorId=1').Content -match 'Hein Thu') 'donor was deleted!'
$noTok = Req '/app/admin/donors/delete' 'Post' @{ donorId='4' }
Check 'Delete without CSRF blocked (403)' ($noTok.Status -eq 403) "status $($noTok.Status)"
Req '/app/admin/donors/delete?donorId=4' | Out-Null
Check 'GET on the delete URL is harmless' ((Req '/app/donors?q=Thida').Content -match 'Thida Win') 'deleted by a GET!'

'=== 12. Role-based access (FR-1) ==='
$s = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$t2 = Token (Req '/login').Content
Req '/login' 'Post' @{ username='staff'; password='Staff@123'; _csrf=$t2 } | Out-Null
$forb = Req '/app/admin/settings'
Check 'STAFF blocked from /app/admin (403)' ($forb.Status -eq 403) "status $($forb.Status)"
$sd = Req '/app/dashboard'
Check '  ...but STAFF can use the dashboard' ($sd.Status -eq 200) "status $($sd.Status)"
$sv = Req '/app/donors/view?donorId=1'
Check '  ...and cannot see the Delete control' (-not ($sv.Content -match 'Delete donor')) 'visible to staff'
$sdel = Req '/app/admin/donors/delete' 'Post' @{ donorId='5' }
Check '  ...and is refused the delete endpoint' ($sdel.Status -eq 403) "status $($sdel.Status)"

''
"================  $($script:pass) passed, $($script:fail) failed  ================"
if ($script:fail -gt 0) { exit 1 }
