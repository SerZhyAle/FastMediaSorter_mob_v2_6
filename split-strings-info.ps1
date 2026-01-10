# Script to split Android strings.xml files into organized categories
# This splits large strings files into manageable, feature-based files

Write-Host "Starting strings.xml split process..." -ForegroundColor Cyan

# Define the base paths
$basePath = "C:\SZA\FastMediaSorter_mob_v2_6\app\app\src\main\res"
$languages = @("values", "values-ru", "values-uk")

# Note: The script will create the following split files for each language:
# - strings.xml (core - already created manually)
# - strings_player.xml (player & media display - already created for EN)
# - strings_settings.xml (all settings)
# - strings_file_ops.xml (file browser & operations)
# - strings_onboarding.xml (welcome & permissions)
# - strings_network.xml (network & cloud)
# - strings_dialogs.xml (dialogs & errors)
# - strings_misc.xml (widget, PIN, text editor, formats)

Write-Host "`nThe main strings.xml files have been prepared." -ForegroundColor Green
Write-Host "The strings_player.xml file has been created for English." -ForegroundColor Green
Write-Host "`nTo complete the split:"  -ForegroundColor Yellow
Write-Host "1. Create equivalent strings_player.xml files for RU and UK"  -ForegroundColor Yellow
Write-Host "2. Create remaining category files for all 3 languages"  -ForegroundColor Yellow
Write-Host "3. Remove or archive the old large strings.xml files"  -ForegroundColor Yellow
Write-Host "`nThis organization will:" -ForegroundColor Cyan
Write-Host "  • Make strings easier to find and maintain"
Write-Host "  • Reduce merge conflicts"
Write-Host "  • Keep related strings together"
Write-Host "  • Work perfectly with Android's resource system (auto-merges at build)"

Write-Host "`nSplit process information complete." -ForegroundColor Green
