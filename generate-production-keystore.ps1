# Production Keystore Generation Script for FastMediaSorter v2
# WARNING: Keep the generated keystore file and credentials SECURE!
# If lost, you cannot update your app on Google Play Store!

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Production Keystore Generator" -ForegroundColor Cyan
Write-Host "  FastMediaSorter v2" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if production keystore already exists
$keystorePath = "fastmediasorter-production.keystore"
if (Test-Path $keystorePath) {
    Write-Host "WARNING: Production keystore already exists at: $keystorePath" -ForegroundColor Yellow
    Write-Host ""
    $overwrite = Read-Host "Do you want to overwrite it? (yes/NO)"
    if ($overwrite -ne "yes") {
        Write-Host "Aborted. Existing keystore preserved." -ForegroundColor Green
        exit 0
    }
    Write-Host ""
}

# Find keytool
$javaHome = $env:JAVA_HOME
if (-not $javaHome) {
    # Try to find Java in common locations
    $javaLocations = @(
        "$env:ProgramFiles\Java",
        "$env:ProgramFiles\Android\Android Studio\jbr",
        "${env:ProgramFiles(x86)}\Java"
    )
    
    foreach ($location in $javaLocations) {
        if (Test-Path $location) {
            $javaDirs = Get-ChildItem -Path $location -Directory -ErrorAction SilentlyContinue
            if ($javaDirs) {
                $javaHome = $javaDirs[0].FullName
                break
            }
        }
    }
}

if (-not $javaHome) {
    Write-Host "ERROR: Could not find Java installation!" -ForegroundColor Red
    Write-Host "Please set JAVA_HOME environment variable or install Java." -ForegroundColor Yellow
    exit 1
}

$keytool = Join-Path $javaHome "bin\keytool.exe"
if (-not (Test-Path $keytool)) {
    Write-Host "ERROR: keytool not found at: $keytool" -ForegroundColor Red
    exit 1
}

Write-Host "Using Java from: $javaHome" -ForegroundColor Green
Write-Host ""

# Gather information
Write-Host "Enter keystore information:" -ForegroundColor Yellow
Write-Host "(This information will be embedded in the certificate)" -ForegroundColor Gray
Write-Host ""

$alias = Read-Host "Key alias (e.g., fastmediasorter-key)"
if ([string]::IsNullOrWhiteSpace($alias)) {
    $alias = "fastmediasorter-key"
    Write-Host "Using default alias: $alias" -ForegroundColor Gray
}

Write-Host ""
Write-Host "Enter distinguished name information:" -ForegroundColor Cyan

$firstName = Read-Host "First and Last Name (CN)"
$orgUnit = Read-Host "Organizational Unit (OU, e.g., Development)"
$org = Read-Host "Organization (O, e.g., Your Company Name)"
$city = Read-Host "City or Locality (L)"
$state = Read-Host "State or Province (ST)"
$country = Read-Host "Country Code (C, 2-letter, e.g., US)"

# Construct DN
$dname = "CN=$firstName, OU=$orgUnit, O=$org, L=$city, ST=$state, C=$country"

Write-Host ""
Write-Host "Keystore will be created with:" -ForegroundColor Cyan
Write-Host "  File: $keystorePath"
Write-Host "  Alias: $alias"
Write-Host "  DN: $dname"
Write-Host "  Validity: 10000 days (27+ years)"
Write-Host "  Algorithm: RSA 2048-bit"
Write-Host ""

$confirm = Read-Host "Proceed with keystore generation? (yes/no)"
if ($confirm -ne "yes") {
    Write-Host "Aborted." -ForegroundColor Yellow
    exit 0
}

Write-Host ""
Write-Host "Generating production keystore..." -ForegroundColor Cyan
Write-Host "You will be prompted to enter passwords twice (keystore and key)." -ForegroundColor Yellow
Write-Host ""

# Generate keystore
& $keytool -genkeypair `
    -alias $alias `
    -keyalg RSA `
    -keysize 2048 `
    -validity 10000 `
    -keystore $keystorePath `
    -dname $dname

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "  Keystore generated successfully!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Keystore file: $keystorePath" -ForegroundColor Cyan
    Write-Host "Key alias: $alias" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "IMPORTANT: Keep this file and passwords SECURE!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Next steps:" -ForegroundColor Yellow
    Write-Host "1. Update keystore.properties with the credentials"
    Write-Host "2. Add keystore file to .gitignore (already done)"
    Write-Host "3. Back up keystore file to secure location"
    Write-Host "4. Build signed release APK"
    Write-Host ""
    
    # Offer to update keystore.properties
    $updateProps = Read-Host "Update keystore.properties file? (yes/no)"
    if ($updateProps -eq "yes") {
        Write-Host ""
        $storePass = Read-Host "Keystore password" -AsSecureString
        $keyPass = Read-Host "Key password" -AsSecureString
        
        # Convert secure strings to plain text for writing
        $BSTR = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($storePass)
        $storePassPlain = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($BSTR)
        
        $BSTR2 = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($keyPass)
        $keyPassPlain = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($BSTR2)
        
        $propsContent = @"
# Production Keystore Configuration for FastMediaSorter v2
# WARNING: Keep this file secure! Add to .gitignore!
# Do NOT commit this file to version control!

storeFile=$keystorePath
storePassword=$storePassPlain
keyAlias=$alias
keyPassword=$keyPassPlain
"@
        
        Set-Content -Path "keystore.properties" -Value $propsContent -Force
        Write-Host "✓ Updated keystore.properties" -ForegroundColor Green
        Write-Host ""
        Write-Host "WARNING: keystore.properties contains passwords in plain text!" -ForegroundColor Red
        Write-Host "Ensure it is listed in .gitignore and never commit it!" -ForegroundColor Red
    }
    
} else {
    Write-Host ""
    Write-Host "ERROR: Keystore generation failed!" -ForegroundColor Red
    Write-Host "Please check the error messages above." -ForegroundColor Yellow
    exit 1
}
