# Navigation Mermaid Diagram

**Last Updated**: January 6, 2026  
**Format**: Mermaid flowchart

```mermaid
graph TD
    A[MainActivity] -->|Click Resource| B[BrowseActivity]
    A -->|Overflow Menu| C[SettingsActivity]
    A -->|About| D[InfoActivity - App]
    A -->|Long-press| E[EditResourceActivity]
    
    B -->|Click File| F[PlayerActivity]
    B -->|Long-press| E
    
    F -->|Overflow Info| G[InfoActivity - File]
    
    style A fill:#4CAF50
    style F fill:#2196F3
```

## Usage

To render this diagram:
1. **GitHub**: Automatically renders in README.md
2. **VS Code**: Install Mermaid extension
3. **Online**: https://mermaid.live/

## Color Legend
- **Green (#4CAF50)**: Entry point (MainActivity)
- **Blue (#2196F3)**: Media viewer (PlayerActivity)
- **Default**: Standard activities
