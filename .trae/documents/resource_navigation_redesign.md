# Resource Navigation Redesign Plan

## Objective
Revamp the "Resource Navigation" (ResourceManagement) page to be more interactive, usable, visually appealing, and elegant.

## Phase 1: Layout & Component Structure
- **Extract Component**: Create a `ResourceCard.vue` component to encapsulate the card logic and styling, keeping the main view clean.
- **Grid Layout**: Use a responsive grid (CSS Grid or Flexbox) that adapts gracefully to different screen sizes.
- **Hero Header**: Replace the simple operation bar with a "Hero" style header section containing:
    -   A prominent Search Bar (real-time filtering).
    -   Elegant Environment Tabs (Pill/Segmented style instead of standard radio buttons).
    -   Primary Action Buttons (Add/Quick Add) styled to fit the theme.

## Phase 2: Visual & Interaction Design (High-end & Elegant)
- **Visual Style**:
    -   **Glassmorphism/Soft UI**: Use subtle shadows, rounded corners (16px), and hover lift effects for cards.
    -   **Typography**: Clean, readable fonts with hierarchy.
    -   **Color Palette**: Use a refined palette for environment tags (e.g., small colored dots or subtle border accents instead of heavy solid tags).
- **Interactions**:
    -   **Hover Actions**: Hide "Edit/Delete" actions by default; reveal them on hover (or put them in a `...` dropdown menu).
    -   **One-Click Copy**: Add a "Copy Link" button on the card.
    -   **Transitions**: Add Vue `<transition-group>` for smooth animations when filtering or adding resources.
    -   **Skeleton Loading**: Replace simple spinners with skeleton screens for a polished loading experience.

## Phase 3: Functionality Enhancements
-   **Search**: Implement client-side fuzzy search for resource name, URL, and description.
-   **Categories**:
    -   Keep the category grouping but improve the section headers.
    -   Consider a "Sticky" side navigation or top anchor links if the list gets too long.
-   **Empty States**: Add a custom illustration or icon for empty states (no results/no resources).

## Implementation Steps
1.  **Refactor**: Extract `ResourceCard` (inline or separate file).
2.  **UI Update**:
    -   Implement the Hero Header with Search and Tabs.
    -   Update Card styling for the "High-end" look.
3.  **Interaction**:
    -   Add "Copy to Clipboard" functionality.
    -   Add animations.
4.  **Verify**: Ensure responsiveness and correct favicon behavior is preserved.

## Proposed File Changes
-   `dga-frontend/src/views/ResourceManagement.vue`: Complete rewrite of the template and style.
