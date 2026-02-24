# Data Source Management Redesign Plan

## Objective
Revamp the "Data Source Management" page (`DataSourceManagement.vue`) to be more professional, user-friendly, and visually aligned with modern data platform standards (e.g., Aliyun DataWorks, AWS Data Glue). The goal is to move away from a simple table view to a more structured and interactive management console.

## Phase 1: Layout & Interaction Design
-   **Card/List Hybrid View**: Instead of a plain table, offer a Card View (default) and List View toggle. Cards provide better visualization of data source types (logos) and status.
-   **Hero Header**: Adopt the same "Hero Header" pattern used in `ResourceManagement.vue` for consistency:
    -   Prominent search bar.
    -   "Create Data Source" primary action.
    -   Stats overview (Total, Active, Error).
-   **Drawer for Forms**: Use a side `Drawer` instead of a `Modal` for adding/editing data sources. This provides more vertical space for complex configurations (connection details, advanced settings).

## Phase 2: Component Structure & Visuals
-   **Data Source Card**:
    -   **Logo**: Dynamic icon based on type (Hive, MySQL, StarRocks, etc.).
    -   **Status Indicator**: Real-time connection status (Healthy/Error/Unknown).
    -   **Quick Actions**: Test Connection, Sync Metadata, Edit, Delete.
    -   **Metadata Info**: Last sync time, table count (if available).
-   **Form Redesign**:
    -   Group fields logically: "Basic Info", "Connection Details", "Advanced Settings".
    -   Password fields should be toggle-able visibility.
    -   "Test Connection" button *inside* the form for immediate validation before saving.

## Phase 3: Backend Enhancements (Minor)
-   Ensure `DataSourceController` supports necessary CRUD and Test Connection endpoints (already exists, just verify).
-   Add `description` field to `DataSourceConfig` entity (currently missing but useful for UI).

## Implementation Steps
1.  **Backend Update**:
    -   Add `description` field to `DataSourceConfig.java`.
    -   Update `DataSourceController.java` if needed (likely auto-handled by JPA).
2.  **Frontend Refactor**:
    -   Rename/Refactor `DataSourceManagement.vue`.
    -   Implement `HeroHeader` pattern.
    -   Implement `DataSourceCard` component (inline or separate).
    -   Replace Modal with `a-drawer`.
    -   Add icons mapping for different DB types.
3.  **Visual Polish**:
    -   Use Ant Design steps/tabs in the drawer if config gets complex.
    -   Add skeleton loading states.

## Proposed File Changes
-   `dga-backend/src/main/java/com/dga/datasource/entity/DataSourceConfig.java`: Add `description` field.
-   `dga-frontend/src/components/DataSourceManagement.vue`: Complete rewrite.

## Mockup Concepts
-   **Header**: "Data Sources" title, Search input, "New Connection" button.
-   **Content**: Grid of cards. Each card has a large logo (e.g., Elephant for Hive), Name, Status dot, and bottom action bar.
-   **Drawer**: Slide-out from right. Step 1: Select Type. Step 2: Connection Config. Step 3: Test & Save.
