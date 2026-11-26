---
name: ui-design-token-enforcer
description: Supervises React component development and Tailwind CSS styling to enforce the visual design tokens, WCAG 2.2 AA contrast ratios, touch ergonomics, and role-based data visibility established in PRE-UIUX-01-guia-visual.md.
---

# Skill: UI/UX Visual Token & Accessibility Enforcer (`PRE-UIUX-01`)

Activate this skill whenever configuring Tailwind CSS, building React components, or writing frontend tests. Note that [`docs/08-uiux-sin-vistas/PRE-UIUX-01-guia-visual.md`](file:///C:/Users/carlo/Projects/RestaurantTickets/docs/08-uiux-sin-vistas/PRE-UIUX-01-guia-visual.md) defines visual tokens and accessibility constraints without prescribing rigid Figma frames.

---

## 1. Official Brand & Semantic Color Tokens

Use these exact tokens in `tailwind.config.js` / CSS variables:

| Token | Hex | Recommended Usage | Foreground Text |
|---|---|---|---|
| `color-primary-700` | `#14532D` (Dark Nopal Green) | Primary actions, operational headers, navigation | White `#FFFFFF` |
| `color-primary-500` | `#22C55E` (Fresh Green) | Active indicators, secondary accents | Dark `#17211A` |
| `color-secondary-700` | `#B91C1C` (Chile Red) | Secondary brand, errors, destructive actions | White `#FFFFFF` |
| `color-accent-500` | `#F59E0B` (Corn / Amber) | Callouts, order ready alerts, brand highlights | Dark `#1F2937` |
| `color-accent-200` | `#FDE68A` (Light Corn) | Soft warning backgrounds | Dark `#78350F` |
| `color-background` | `#FFF8ED` (Warm Tortilla Cream) | General page background | Dark `#1F2937` |
| `color-surface` | `#FFFFFF` (White) | Cards, modals, tables, content surfaces | Dark `#1F2937` |
| `color-border` | `#D6D3D1` (Stone Gray) | Borders and dividers | N/A |
| `color-text` | `#1F2937` | Primary body text on light backgrounds | N/A |
| `color-text-muted` | `#57534E` | Secondary/helper text on light backgrounds | N/A |

### Semantic Status Palette
- **Success:** Background `#DCFCE7`, Text `#166534` (Confirmed payment, delivered, reconciled close)
- **Info:** Background `#DBEAFE`, Text `#1E40AF` (`EN_PREPARACION`, neutral operational instructions)
- **Warning:** Background `#FEF3C7`, Text `#92400E` (Low inventory threshold, order waiting for pickup)
- **Error:** Background `#FEE2E2`, Text `#991B1B` (Insufficient stock, unconfirmed payment, failed delivery)
- **Neutral:** Background `#F5F5F4`, Text `#44403C` (`PENDIENTE_PAGO`, inactive, empty state)

### Visual Balance Rule
- **70%** Warm Tortilla Cream (`#FFF8ED`) or White (`#FFFFFF`) backgrounds.
- **20%** Nopal Green (`#14532D` / `#22C55E`) for structure and primary actions.
- **10%** Chile Red (`#B91C1C`) and Corn Amber (`#F59E0B`) for warnings, errors, or highlights.

---

## 2. Accessibility, Ergonomics & Operational React Rules (`RNF-016`, `RNF-017`, `T3-01`)

1. **Dual State Encoding (Colorblind Safety):**
   - Never rely solely on red/green color to convey status. Every status badge must include **explicit text** and a **semantic icon**.
2. **WCAG 2.2 AA & Touch Targets (`RNF-017`):**
   - Minimum contrast ratio of **4.5:1** for normal text.
   - Interactive controls in Cashier, Kitchen, Despacho, and Delivery views must have a minimum touch target of **44x44 px** (recommended **48x48 px**).
   - Full keyboard navigation without focus traps; visible `:focus-visible` outlines on all interactive elements.
3. **Form State Preservation on Error (Workshop 3 Requirement):**
   - When the backend returns `400`, `409`, or `422`, the React UI **must preserve the current `Solicitud` form state** so the Cashier can correct fields, adjust items, or refresh stock without re-typing the customer's order.
   - Disable the confirmation button while a request is in flight and retain the same `Idempotency-Key` across network retries of the same confirmation attempt (`RNF-010`).
4. **Kitchen (`COCINA`) Privacy & Readability (`RN-012`, `RN-021`, `RNF-008`):**
   - High-visibility typography readable from a distance.
   - Highlight customer `preferencias` clearly on each line item.
   - **Zero** state-mutation buttons and **zero** monetary amounts, payment references, or delivery addresses rendered.
