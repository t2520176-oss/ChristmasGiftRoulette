# Windows PowerShell 5.1. Dot-source this file from Start-OurChat.ps1.
function Show-OurChatConfiguration {
    param([string]$ConfigPath, [string]$AppPath)

    Add-Type -AssemblyName System.Windows.Forms
    Add-Type -AssemblyName System.Drawing
    [System.Windows.Forms.Application]::EnableVisualStyles()
    $values = @{}
    $lines = @()
    if (Test-Path -LiteralPath $ConfigPath) {
        $lines = @([System.IO.File]::ReadAllLines($ConfigPath))
        foreach ($line in $lines) {
            if ($line -match '^([A-Z][A-Z0-9_]*)=(.*)$') {
                $values[$Matches[1]] = $Matches[2]
            }
        }
    }

    $form = New-Object System.Windows.Forms.Form
    $form.Text = 'OurChat - Connection setup'
    $form.ClientSize = New-Object System.Drawing.Size(650, 445)
    $form.StartPosition = 'CenterScreen'
    $form.FormBorderStyle = 'FixedDialog'
    $form.MaximizeBox = $false
    $form.MinimizeBox = $false
    $form.Font = New-Object System.Drawing.Font('Segoe UI', 9)
    $intro = New-Object System.Windows.Forms.Label
    $intro.Text = "Connect your own Supabase project. Create two accounts and apply the SQL first.`r`nYou can skip this step to view the app. No working chat is created by skipping."
    $intro.Location = New-Object System.Drawing.Point(20, 15)
    $intro.Size = New-Object System.Drawing.Size(610, 48)
    $form.Controls.Add($intro)

    $fields = @(
        @{ Key = 'NEXT_PUBLIC_SUPABASE_URL'; Label = 'Supabase URL (https://project.supabase.co)' },
        @{ Key = 'NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY'; Label = 'Supabase publishable key (required)' },
        @{ Key = 'SUPABASE_SERVICE_ROLE_KEY'; Label = 'Service-role key (optional; server only)' },
        @{ Key = 'VAPID_SUBJECT'; Label = 'Administrator email (optional; for notifications)' }
    )
    $boxes = @{}
    $top = 72
    foreach ($field in $fields) {
        $label = New-Object System.Windows.Forms.Label
        $label.Text = $field.Label
        $label.Location = New-Object System.Drawing.Point(20, $top)
        $label.Size = New-Object System.Drawing.Size(610, 20)
        $form.Controls.Add($label)
        $box = New-Object System.Windows.Forms.TextBox
        $box.Location = New-Object System.Drawing.Point(20, ($top + 22))
        $box.Size = New-Object System.Drawing.Size(610, 25)
        $box.MaxLength = 4096
        if ($values.ContainsKey($field.Key)) { $box.Text = $values[$field.Key] }
        if ($field.Key -eq 'VAPID_SUBJECT') { $box.Text = $box.Text -replace '^mailto:', '' }
        if ($field.Key -eq 'SUPABASE_SERVICE_ROLE_KEY') { $box.UseSystemPasswordChar = $true }
        $boxes[$field.Key] = $box
        $form.Controls.Add($box)
        $top += 61
    }

    $note = New-Object System.Windows.Forms.Label
    $note.Text = 'Keys stay on this PC. Save does not create accounts or deploy a website.'
    $note.Location = New-Object System.Drawing.Point(20, 324)
    $note.Size = New-Object System.Drawing.Size(610, 35)
    $form.Controls.Add($note)
    $help = New-Object System.Windows.Forms.Button
    $help.Text = 'Setup guide'
    $help.Location = New-Object System.Drawing.Point(20, 382)
    $help.Size = New-Object System.Drawing.Size(120, 32)
    $help.Add_Click({
        try {
            $guide = Join-Path $AppPath 'README.ko.md'
            Start-Process -FilePath (Join-Path $env:WINDIR 'System32\notepad.exe') -ArgumentList ('"' + $guide + '"') | Out-Null
        } catch {
            [void][System.Windows.Forms.MessageBox]::Show('Open README.ko.md in the installed app folder.', 'OurChat')
        }
    })
    $form.Controls.Add($help)
    $skip = New-Object System.Windows.Forms.Button
    $skip.Text = 'Skip / keep settings'
    $skip.Location = New-Object System.Drawing.Point(335, 382)
    $skip.Size = New-Object System.Drawing.Size(150, 32)
    $skip.DialogResult = [System.Windows.Forms.DialogResult]::Cancel
    $form.Controls.Add($skip)
    $save = New-Object System.Windows.Forms.Button
    $save.Text = 'Save and continue'
    $save.Location = New-Object System.Drawing.Point(495, 382)
    $save.Size = New-Object System.Drawing.Size(135, 32)
    $form.Controls.Add($save)
    $form.AcceptButton = $save
    $form.CancelButton = $skip

    $save.Add_Click({
        try {
            $url = $boxes['NEXT_PUBLIC_SUPABASE_URL'].Text.Trim()
            $publicKey = $boxes['NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY'].Text.Trim()
            $serviceKey = $boxes['SUPABASE_SERVICE_ROLE_KEY'].Text.Trim()
            $email = $boxes['VAPID_SUBJECT'].Text.Trim()
            $uri = $null
            if (-not [System.Uri]::TryCreate($url, [System.UriKind]::Absolute, [ref]$uri) -or
                $uri.Scheme -ne 'https' -or $uri.Host -notmatch '^[a-zA-Z0-9-]+\.supabase\.(co|in)$' -or
                $uri.Port -ne 443 -or $uri.UserInfo -or $uri.Query -or $uri.Fragment -or $uri.AbsolutePath -ne '/') {
                throw 'Enter your HTTPS Supabase project URL without a path, query, or credentials.'
            }
            if ($publicKey -notmatch '^[A-Za-z0-9_.-]{10,4096}$') {
                throw 'Enter a valid Supabase publishable key. Spaces, quotes, and line breaks are not accepted.'
            }
            if ($serviceKey -and $serviceKey -notmatch '^[A-Za-z0-9_.-]{10,4096}$') {
                throw 'The optional service-role key contains unsupported characters.'
            }
            if ($email -and ($email.Length -gt 254 -or $email -notmatch '^[A-Za-z0-9_.+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,63}$')) {
                throw 'Enter a valid administrator email address, or leave it blank.'
            }
            $updates = @{
                NEXT_PUBLIC_SUPABASE_URL = $uri.GetLeftPart([System.UriPartial]::Authority)
                NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY = $publicKey
                SUPABASE_SERVICE_ROLE_KEY = $serviceKey
                NEXT_PUBLIC_SITE_URL = 'http://localhost:3000'
                VAPID_SUBJECT = $(if ($email) { 'mailto:' + $email } else { '' })
            }
            $output = New-Object 'System.Collections.Generic.List[string]'
            $written = @{}
            foreach ($line in $lines) {
                if ($line -match '^([A-Z][A-Z0-9_]*)=' -and $updates.ContainsKey($Matches[1])) {
                    $key = $Matches[1]
                    if (-not $written.ContainsKey($key)) {
                        $output.Add($key + '=' + $updates[$key])
                        $written[$key] = $true
                    }
                } else { $output.Add($line) }
            }
            foreach ($key in ($updates.Keys | Sort-Object)) {
                if (-not $written.ContainsKey($key)) { $output.Add($key + '=' + $updates[$key]) }
            }
            $parent = Split-Path -Parent $ConfigPath
            if ($parent -and -not (Test-Path -LiteralPath $parent)) {
                [void][System.IO.Directory]::CreateDirectory($parent)
            }
            $temporary = $ConfigPath + '.new'
            [System.IO.File]::WriteAllLines($temporary, $output, (New-Object System.Text.UTF8Encoding($false)))
            Move-Item -LiteralPath $temporary -Destination $ConfigPath -Force
            $form.DialogResult = [System.Windows.Forms.DialogResult]::OK
            $form.Close()
        } catch {
            [void][System.Windows.Forms.MessageBox]::Show($_.Exception.Message, 'OurChat setup', [System.Windows.Forms.MessageBoxButtons]::OK, [System.Windows.Forms.MessageBoxIcon]::Warning)
        }
    })
    try { return ($form.ShowDialog() -eq [System.Windows.Forms.DialogResult]::OK) }
    finally { $form.Dispose() }
}
