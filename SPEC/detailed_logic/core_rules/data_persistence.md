# Data Persistence & Stat Management

## 1. Persistence Strategy

- **SharedPreferences**: Used for primitive settings and flags, last sort modes.
- **Room Database**: Stores `Resources`, `Favorites`. Does NOT store file index (files are volatile).
- _TODO: Detail specific data lifecycle._

## 2. Caching

- **UnifiedFileCache**: Stores thumbnails and temporary copies of network files.
