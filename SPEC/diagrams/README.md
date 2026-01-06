# Specification Diagrams

**Purpose**: Reusable visual assets for FastMediaSorter v2 specification documents.

This directory contains diagrams and visual representations referenced throughout the specification files.

---

## Available Diagrams

### 1. [database_er_diagram.md](database_er_diagram.md)
**Entity-Relationship Diagram** for Room DB (version 6)
- **Tables**: resources, network_credentials, favorites, playback_positions, thumbnail_cache, resources_fts
- **Relationships**: FK links, 1:N associations
- **Referenced by**: [26_database_schema.md](../26_database_schema.md)

### 2. [navigation_flow.md](navigation_flow.md)
**Activity Navigation Flow** (ASCII diagram)
- **Activities**: MainActivity → BrowseActivity → PlayerActivity → InfoActivity
- **Actions**: Click, long-press, overflow menu
- **Referenced by**: [33_navigation_graph.md](../33_navigation_graph.md)

### 3. [navigation_mermaid.md](navigation_mermaid.md)
**Mermaid Flowchart** for interactive navigation visualization
- **Format**: Mermaid syntax (renders in GitHub, VS Code with extension)
- **Features**: Color-coded nodes (entry point green, player blue)
- **Referenced by**: [33_navigation_graph.md](../33_navigation_graph.md)

### 4. [project_structure.md](project_structure.md)
**Project Directory Structure** with Clean Architecture layers
- **Layers**: UI → Domain → Data
- **Key directories**: ui/, domain/, data/, di/
- **Referenced by**: [index.md](../index.md)

---

## Usage

To reference a diagram from a specification document:

```markdown
**Full diagram**: [diagrams/database_er_diagram.md](diagrams/database_er_diagram.md)

**Summary**: Brief inline description...
```

---

## Rendering Mermaid Diagrams

### GitHub
Mermaid diagrams render automatically in README.md and other markdown files.

### VS Code
1. Install extension: [Markdown Preview Mermaid Support](https://marketplace.visualstudio.com/items?itemName=bierner.markdown-mermaid)
2. Open markdown file with Ctrl+Shift+V (preview)

### Online
Paste Mermaid code into [mermaid.live](https://mermaid.live/) for live editing and export.

---

## Maintenance

When updating diagrams:
1. **Edit source file** in this directory (e.g., `database_er_diagram.md`)
2. **Update "Last Updated" date** at top of file
3. **Check references** in parent specification documents (use `grep -r "diagrams/" ../`)
4. **Test rendering** in VS Code or GitHub preview

---

**Last Updated**: January 6, 2026  
**Maintained By**: FastMediaSorter Development Team
