# Nova Mart — Design System

The frontend is a retail surface, so the design brief is narrower than "make it
look nice": it has to look like somewhere you would enter card details. This
document records the decisions and, where relevant, what was rejected.

Everything below is defined once in
[`frontend/app/globals.css`](../frontend/app/globals.css). Components reference
tokens and never raw hex values.

---

## 1. The problem with the default look

Generated interfaces converge on a recognisable set of traits: an indigo-to-violet
gradient hero, translucent panels floating over a blurred background, everything
rounded to a pill, a headline making a large claim, and a row of statistics
nobody measured.

Each of those was avoided deliberately, and in one case against a tool's
recommendation: a design-system query for this product returned **"Liquid Glass"**
as the suggested style. It was rejected. Glassmorphism costs text contrast and
GPU time, and on a conversion-focused retail page it competes with the product
photography, which is the thing actually doing the selling.

What premium retail actually does — Aesop, COS, SSENSE — is the opposite:
near-black calls to action on a warm off-white ground, hairline borders instead
of shadows, restrained radii, and colour spent only where it carries meaning.

---

## 2. Colour

A warm neutral base with a near-black primary. Warm rather than blue-grey,
because blue-grey reads as software and warm grey reads as retail.

| Token | Value | Role |
| --- | --- | --- |
| `--color-canvas` | `#faf9f7` | Page background |
| `--color-surface` | `#ffffff` | Cards, panels |
| `--color-sunken` | `#f4f2ee` | Inset areas, table headers |
| `--color-ink` | `#1a1815` | Headings, body, **primary button fill** |
| `--color-ink-soft` | `#57524b` | Body copy (7.5:1 on white) |
| `--color-muted` | `#78716a` | Captions (4.6:1 — still AA) |
| `--color-line` | `#e6e2da` | Hairline borders |
| `--color-line-strong` | `#d3ccc0` | Input borders |
| `--color-accent` | `#9a5b0d` | Discounts and rating stars only (5.3:1) |
| `--color-success` | `#146b3a` | Confirmed, in stock |
| `--color-warning` | `#a15c07` | Low stock, pending |
| `--color-danger` | `#b42318` | Cancelled, destructive |

**Colour is spent, not sprinkled.** The accent appears on a price reduction and
on rating stars, and nowhere else. A page where everything is coloured has no
hierarchy left to express what matters.

**Every semantic colour is paired with a word.** `Badge` never communicates
through colour alone: "Out of stock" says out of stock. Around one man in twelve
cannot reliably separate the red and green ones.

### Light mode only, deliberately

Not an omission. A half-finished dark mode is worse than none, and premium retail
is overwhelmingly light. The tokens are structured so a dark theme means
redefining one block rather than rewriting components — but shipping it would
mean auditing every screen twice, and that scope was better spent elsewhere.

---

## 3. Typography

| | |
| --- | --- |
| Display | **Fraunces** — variable optical serif |
| Interface | **Inter** |

Two families with distinct jobs. Fraunces appears on the wordmark and top-level
headings only; Inter does all the interface work. An editorial serif paired with
a workhorse grotesk is what stops the page reading as another Inter-everywhere
SaaS template — and using it *sparingly* is what stops it reading as a wedding
invitation.

Both are self-hosted via `next/font`, which removes a render-blocking round trip
to Google and stops every page view carrying the visitor's IP to a third party.

Scale, ratio ≈1.25: `12 · 13 · 15 · 16 · 18 · 22 · 28 · 36 · 48`.

Body stays at **16px on mobile**. Below that, iOS Safari zooms the viewport on
input focus, which yanks the layout sideways mid-form.

**Prices use tabular figures.** Proportional numerals make digits shift width as
they change, so a quantity stepper jitters and a column of prices fails to align.

---

## 4. Space, radius, elevation

Spacing follows a 4px rhythm: `4 · 8 · 12 · 16 · 24 · 32 · 48 · 64`.

Radii are restrained — `4 / 6 / 10 / 14`. Everything-is-a-pill is a hallmark of
generated interfaces, and real retail keeps product imagery close to square so
the goods dominate.

**Hairline borders do most of the separating work.** Shadows appear only on
things that genuinely float: menus, dialogs, toasts. A page where every card
casts a shadow has no depth, because depth is relative.

---

## 5. Motion

Fast, few, and always explaining a change.

| Token | Value | Used for |
| --- | --- | --- |
| `--duration-fast` | 120ms | Hover, colour changes |
| `--duration-base` | 180ms | Reveals, dialogs |
| `--duration-slow` | 260ms | Entrances |

The only decorative motion in the product is a 3% image scale on product-card
hover — enough to feel responsive, not enough to be a distraction across a grid
of twelve.

`prefers-reduced-motion` is respected globally. Nothing animated carries meaning
that is lost without it.

---

## 6. Components

Built by hand rather than pulled from a library, so the whole surface shares one
visual language and there is no second set of design decisions to reconcile.

| Component | Decision worth noting |
| --- | --- |
| `Button` | Variants are semantic, not decorative. Disabled during async work — that is what prevents double submission; the spinner only explains why nothing is happening. |
| `Field` | Every input gets a real visible `<label>`. Placeholder-as-label is the most common accessibility failure in modern forms. |
| `Dialog` | Built on native `<dialog>`, which brings focus trapping, the top layer and Escape-to-close from the platform instead of from hand-written focus management that will be subtly wrong. |
| `Toast` | `aria-live="polite"` — announced without stealing focus, which would interrupt whatever the user was typing. |
| `DataTable` | A real `<table>` on desktop, a list of cards below `md`. Not a horizontally scrolling table. |
| `ProductImage` | Skeleton while loading, designed fallback tile on error. A broken-image icon in a product grid reads as a bug. |
| `EmptyState` | Every empty region names what is missing and offers the action that fills it. An empty region with no explanation is indistinguishable from a broken one. |
| `Price` | Struck-through original marked up with `<s>` plus a spoken label, so a screen reader hears a relationship rather than two unexplained numbers. |

---

## 7. Every state, every time

The interface accounts for all of these rather than only the happy path:

| State | Treatment |
| --- | --- |
| Loading | Skeletons shaped like the content, so nothing jumps when data lands |
| Empty | Explanation plus the action that resolves it |
| Error | Human message from the API envelope, plus a retry control |
| Partial | Cart lines that exceed available stock are flagged **before** checkout |
| Disabled | Explained in words — a greyed-out button with no reason reads as broken |
| Unauthorised | Told what is needed and given the route to it |
| Offline | `NETWORK_ERROR` is distinguished from a server error, because the advice differs |

Backend stack traces never reach the interface, because they never leave the
server.

---

## 8. Accessibility

Treated as correctness, not decoration.

- Semantic HTML throughout: `<table>` for tabular data, `<address>` for
  addresses, `<dl>` for label/value pairs, `<nav>` with accessible names
- One focus treatment, defined once, **never removed**. `:focus-visible` keeps it
  off mouse clicks, so there is no reason to reach for `outline: none`
- Skip link as the first tab stop on every page
- All touch targets ≥44px
- Text contrast: body 7.5:1, captions 4.6:1, all interactive states ≥4.5:1
- Errors sit under their own field with `role="alert"` and are linked by
  `aria-describedby`
- Status is never colour alone
- `prefers-reduced-motion` honoured
- Pinch-zoom is **not** disabled — a common copy-paste that makes a site unusable
  for anyone who needs to magnify it

Verified during development: the mobile viewport at 375px has zero horizontal
overflow, and the admin table is genuinely replaced by cards rather than scrolled.

---

## 9. Responsive strategy

Breakpoints `375 / 640 / 768 / 1024 / 1440`, mobile-first.

Layouts are **restructured**, not shrunk:

- Storefront nav becomes a disclosure panel; search moves onto its own line
- Admin tables become card lists
- Account navigation becomes a horizontal scroller
- Product gallery thumbnails reflow to four across
- Page gutters widen with the viewport, which is what makes a desktop layout feel
  composed rather than stretched

---

## 10. Honesty in the interface

A design rule with the same weight as the visual ones: **the interface must not
claim anything the system does not do.**

- Checkout states that the gateway is simulated, before a method is chosen
- The order confirmation says the receipt was *recorded*, not emailed — an
  earlier draft said "we have emailed a receipt", which was false, and was fixed
- The admin notifications page explains that "Sent" means the mock transport
  accepted the message
- The forgotten-password page says password reset is unavailable rather than
  showing a form that silently does nothing
- Deleting a product says "withdrawn from the catalogue", because it is a soft
  delete that keeps historical orders resolvable
- There are no testimonials and no invented statistics anywhere, because this is
  a reference implementation and it has neither
