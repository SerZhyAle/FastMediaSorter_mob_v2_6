# Automatic Android strings.xml splitter
# Splits large strings files into organized, feature-based files

param(
    [switch]$DryRun = $false
)

$ErrorActionPreference = "Stop"
$basePath = "C:\SZA\FastMediaSorter_mob_v2_6\app\app\src\main\res"

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Android Strings.xml Auto-Splitter" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# Define string categories with their identifying patterns
$categories = @{
    'player' = @(
        'action_previous', 'action_next', 'action_play', 'action_pause', 'action_rewind', 'action_forward',
        'action_favorite', 'action_translate', 'action_ocr', 'action_google_lens', 'action_lyrics',
        'action_fullscreen', 'exit_fullscreen', 'action_slideshow', 'action_play_pause',
        'file_info', 'info_name', 'info_path', 'info_size', 'info_type', 'info_modified',
        'info_section_', 'info_resolution', 'info_megapixels', 'info_camera', 'info_date_taken',
        'info_aperture', 'info_exposure', 'info_iso', 'info_focal_length', 'info_location',
        'info_flash', 'info_white_balance', 'info_duration', 'info_quality', 'info_bitrate',
        'info_framerate', 'info_rotation', 'info_codec', 'info_date_recorded',
        'info_title', 'info_artist', 'info_album', 'info_year', 'info_track', 'info_genre',
        'info_composer', 'info_sample_rate', 'info_bit_depth', 'info_format',
        'info_pages', 'info_lines', 'info_words', 'info_characters',
        'media_content', 'player', 'error_loading_media', 'error_loading_image', 'album_art',
        'slideshow_', 'touch_zone_', 'zone_', 'lyrics', 'loading_lyrics', 'copy_lyrics',
        'subtitles', 'toggle_subtitles', 'player_settings', 'playback_speed', 'repeat_',
        'edit_image', 'image_preview', 'rotate', 'flip', 'crop', 'aspect_', 'adjust',
        'brightness', 'contrast', 'saturation', 'edit_gif', 'gif_preview', 'frame',
        'loop_mode', 'translation_', 'translate', 'source_language', 'target_language',
        'detecting_text', 'ocr_', 'pdf_tools', 'pdf_', 'extract_pages', 'epub_',
        'open_with', 'set_as_wallpaper', 'extract_audio'
    )
    
    'settings' = @(
        'theme', 'default_view_mode', 'behavior', 'show_hidden_files', 'confirm_delete',
        'confirm_move', 'prevent_sleep', 'enable_favorites', 'enable_safe_mode',
        'network_parallelism', 'default_user', 'default_password', 'export_settings',
        'import_settings', 'grant_', 'storage_permissions', 'show_log', 'cache_size',
        'clear_cache', 'view_logs', 'view_session_logs', 'user_guide', 'privacy_policy',
        'support_images', 'supported_image_formats', 'image_size_limits', 'load_full_size',
        'support_video', 'supported_video_formats', 'video_size_limits', 'show_video_thumbnails',
        'support_audio', 'supported_audio_formats', 'audio_size_limits', 'search_audio_covers',
        'support_text', 'supported_text_formats', 'support_pdf', 'support_epub', 'support_gif',
        'enable_translation', 'enable_google_lens', 'min_size_mb', 'max_size_mb',
        'slideshow_settings', 'slideshow_interval', 'random_order', 'loop_slideshow',
        'touch_zones', 'enable_touch_zones', 'show_zone_overlay', 'video_playback',
        'resume_playback', 'auto_play_videos', 'thumbnail_quality', 'auto_rotate_images',
        'show_exif_data', 'jpeg_compression_quality', 'video_quality', 'hardware_acceleration',
        'seek_increment', 'preview_duration', 'waveform_style', 'background_playback',
        'audio_focus_handling', 'text_encoding', 'pdf_page_cache', 'pdf_render_quality',
        'epub_font_family', 'epub_font_size', 'gif_frame_rate_limit', 'quality_',
        'waveform_', 'audio_focus_', 'encoding_', 'font_', 'small_controls'
    )
    
    'file_ops' = @(
        'files', 'folders', 'no_files', 'select_all', 'deselect_all',
        'sort_by', 'sort_name_', 'sort_date_', 'sort_size_', 'action_sort',
        'filter_by_type', 'select_none', 'action_apply', 'media_type_', 'filter_active',
        'view_list', 'view_grid', 'copy', 'move', 'rename', 'select_destination',
        'favorites', 'add_to_favorites', 'remove_from_favorites', 'no_favorites',
        'added_to_favorites', 'removed_from_favorites', 'action_search', 'search_hint',
        'search_in_document', 'search_case_sensitive', 'search_prev', 'search_next',
        'search_no_results', 'search_result_counter', 'search_in_', 'no_search_results',
        'selected_count', 'files_moved', 'files_copied', 'files_deleted', 'file_deleted',
        'file_restored', 'files_restored', 'error_sharing_file',
        'search_title', 'search_initial_message', 'search_result_info',
        'filter_images', 'filter_videos', 'filter_audio', 'filter_other',
        'cd_search_', 'cd_toggle_filters', 'browse', 'scan'
    )
    
    'onboarding' = @(
        'welcome_title_', 'welcome_description_', 'permissions_required', 'permissions_description',
        'grant_permission', 'skip', 'next', 'previous', 'finish', 'get_started',
        'permission_required_', 'permission_denied_', 'open_settings'
    )
    
    'network' = @(
        'cloud_folder_', 'select_folder', 'add_as_destination', 'scan_subdirectories',
        'no_folders_found', 'breadcrumb_path', 'folder', 'navigate_into_folder',
        'discovering_network_devices', 'select_ssh_key_file', 'add_smb_connection',
        'add_sftp_connection', 'add_ftp_connection', 'add_network_connection',
        'server_address', 'port', 'username', 'password', 'use_ssh_key', 'ssh_key_path',
        'domain_optional', 'share_name', 'test_connection', 'error_name_required',
        'error_server_required', 'error_invalid_port', 'error_username_required',
        'error_share_name_required', 'error_ssh_key_required', 'error_password_required',
        'network_sync_settings', 'enable_background_sync', 'sync_', 'network'
    )
    
    'dialogs' = @(
        'error_unknown', 'error_network', 'error_permission', 'error_file_not_found',
        'error_connection_failed', 'error_authentication', 'confirm_delete_title',
        'confirm_delete_message', 'confirm_delete_permanent', 'delete_confirmation_',
        'delete_single_file_message', 'select_destination_move', 'select_destination_copy',
        'no_destinations', 'confirm_delete_resource_message', 'rename_title', 'hint_new_filename',
        'rename_', 'error_name_', 'delete_move_to_trash', 'delete_permanently',
        'dont_ask_again', 'delete', 'copying_files', 'moving_files', 'deleting_files',
        'files_progress', 'speed_value', 'time_remaining', 'preparing', 'operation_completed',
        'operation_cancelled', 'operation_failed_details', 'error_title', 'show_details',
        'copy_to_clipboard', 'copied_to_clipboard', 'report_error', 'discard_changes',
        'close', 'got_it', 'pick_color', 'preset_colors', 'custom_color', 'hex_color',
        'discover_network_devices', 'searching_network', 'devices_found', 'no_devices',
        'manual_connection', 'pause', 'action_expand', 'action_collapse', 'last_operation',
        'google_lens_not_installed', 'install'
    )
    
    'misc' = @(
        'widget_', 'continue_reading', 'media_type',
        'set_pin_code', 'change_pin_code', 'enter_pin_code', 'pin_code', 'confirm_pin_code',
        'remove_pin', 'pin_', 'enable_pin_protection', 'supported_media_types',
        'media_type_filter_desc', 'edit_text_file', 'action_redo', 'file_truncated',
        'cannot_save_truncated', 'file_saved_successfully', 'save_failed_message',
        'pages_format', 'pixels_format', 'fps_format', 'seconds_format', 'percent_format',
        'no_limit', 'debug_logs', 'search_logs', 'filter_all', 'export_logs',
        'action_dismiss', 'hide_details', 'copy_details'
    )
}

function Get-StringCategory {
    param([string]$stringName)
    
    foreach ($category in $categories.Keys) {
        foreach ($pattern in $categories[$category]) {
            if ($stringName -like "*$pattern*") {
                return $category
            }
        }
    }
    return 'core'
}

function Split-StringsFile {
    param(
        [string]$languageFolder,
        [string]$language
    )
    
    $sourceFile = Join-Path $basePath "$languageFolder\strings.xml"
    
    if (-not (Test-Path $sourceFile)) {
        Write-Host "  Skipping $language - source file not found" -ForegroundColor Yellow
        return
    }
    
    Write-Host "`n  Processing $language..." -ForegroundColor White
    
    # Read and parse the file
    [xml]$xml = Get-Content $sourceFile -Encoding UTF8
    
    # Create category buckets
    $buckets = @{}
    foreach ($cat in $categories.Keys) {
        $buckets[$cat] = @()
    }
    $buckets['core'] = @()
    
    # Categorize each string
    foreach ($string in $xml.resources.string) {
        $name = $string.name
        $category = Get-StringCategory $name
        
        # Store the entire XML node
        $buckets[$category] += $string
    }
    
    # Also handle string-array
    foreach ($array in $xml.resources.'string-array') {
        $buckets['core'] += $array
    }
    
    Write-Host "    Categorized: Core=$($buckets['core'].Count), Player=$($buckets['player'].Count), Settings=$($buckets['settings'].Count)" -ForegroundColor Gray
    
    # Create split files
    foreach ($category in $buckets.Keys) {
        if ($buckets[$category].Count -eq 0) {
            continue
        }
        
        $fileName = if ($category -eq 'core') { 'strings.xml' } else { "strings_$category.xml" }
        $outputFile = Join-Path $basePath "$languageFolder\$fileName"
        
        if ($DryRun) {
            Write-Host "    [DRY RUN] Would create: $fileName ($($buckets[$category].Count) strings)" -ForegroundColor Cyan
            continue
        }
        
        # Create new XML
        $newXml = New-Object System.Xml.XmlDocument
        $xmlDeclaration = $newXml.CreateXmlDeclaration("1.0", "utf-8", $null)
        $newXml.AppendChild($xmlDeclaration) | Out-Null
        
        $resources = $newXml.CreateElement("resources")
        $newXml.AppendChild($resources) | Out-Null
        
        # Add comment header
        $comment = $newXml.CreateComment(" $category strings for FastMediaSorter ")
        $resources.AppendChild($comment) | Out-Null
        
        # Add all strings in this category
        foreach ($stringNode in $buckets[$category]) {
            $imported = $newXml.ImportNode($stringNode, $true)
            $resources.AppendChild($imported) | Out-Null
        }
        
        # Save with proper formatting
        $settings = New-Object System.Xml.XmlWriterSettings
        $settings.Indent = $true
        $settings.IndentChars = "    "
        $settings.NewLineChars = "`r`n"
        $settings.Encoding = [System.Text.UTF8Encoding]::new($false)
        
        $writer = [System.Xml.XmlWriter]::Create($outputFile, $settings)
        try {
            $newXml.Save($writer)
            Write-Host "    ✓ Created: $fileName ($($buckets[$category].Count) strings)" -ForegroundColor Green
        }
        finally {
            $writer.Close()
        }
    }
}

# Main execution
Write-Host "Splitting strings for all languages...`n" -ForegroundColor Cyan

$languages = @(
    @{Folder = 'values'; Name = 'English'},
    @{Folder = 'values-ru'; Name = 'Russian'},
    @{Folder = 'values-uk'; Name = 'Ukrainian'}
)

foreach ($lang in $languages) {
    Split-StringsFile -languageFolder $lang.Folder -language $lang.Name
}

if ($DryRun) {
    Write-Host "`n[DRY RUN] No files were modified. Run without -DryRun to apply changes." -ForegroundColor Yellow
}
else {
    Write-Host "`n========================================" -ForegroundColor Green
    Write-Host "✓ Strings split completed successfully!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    
    Write-Host "`nNext steps:" -ForegroundColor Cyan
    Write-Host "1. Review the generated files" -ForegroundColor White
    Write-Host "2. Run build to verify: .\gradlew.bat assembleDebug" -ForegroundColor White
    Write-Host "3. If build succeeds, you can optionally backup the old large strings.xml files" -ForegroundColor White
}

Write-Host ""
