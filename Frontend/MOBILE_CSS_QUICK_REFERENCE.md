# Mobile CSS Quick Reference 🎯

## Common CSS Variable Usage

### Colors

```css
/* Use these variables instead of hardcoded colors */
color: var(--color-text); /* Main text */
color: var(--color-text-secondary); /* Secondary text */
background: var(--color-background); /* Backgrounds */
border-color: var(--color-border); /* Borders */
color: var(--color-primary); /* Brand color */
```

### Spacing

```css
/* Fluid spacing that adapts to screen size */
margin: var(--spacing-xs); /* 4-6px */
margin: var(--spacing-sm); /* 6-8px */
margin: var(--spacing-md); /* 12-18px */
margin: var(--spacing-lg); /* 20-28px */
margin: var(--spacing-xl); /* 24-32px */
```

### Typography

```css
/* Fluid font sizes */
font-size: var(--font-xs); /* 11-13px */
font-size: var(--font-sm); /* 14-16px */
font-size: var(--font-md); /* 15-17px */
font-size: var(--font-lg); /* 26-32px */
font-size: var(--font-xl); /* 28-36px */
```

---

## iOS Zoom Prevention Checklist

When adding new form inputs, ensure:

```css
.your-input {
  /* CRITICAL: Must be >= 16px to prevent iOS auto-zoom */
  font-size: max(16px, clamp(15px, 4vw, 16px));

  /* Also add this global fix */
  -webkit-text-size-adjust: 100%;
}
```

---

## Safe Area Implementation

### For containers with fixed positioning

```css
.your-fixed-element {
  /* Add safe area padding */
  top: max(16px, env(safe-area-inset-top));
  left: max(16px, env(safe-area-inset-left));
  right: max(16px, env(safe-area-inset-right));
  bottom: max(16px, env(safe-area-inset-bottom));
}
```

### For full-screen containers

```css
.your-container {
  padding-top: env(safe-area-inset-top, 0);
  padding-bottom: env(safe-area-inset-bottom, 0);
  padding-left: env(safe-area-inset-left, 0);
  padding-right: env(safe-area-inset-right, 0);
}
```

---

## Touch-Friendly Elements

### Minimum Size Requirements

```css
.your-button {
  /* Apple HIG: minimum 44x44px */
  min-width: 44px;
  min-height: 44px;

  /* Use clamp for fluid sizing */
  width: clamp(44px, 12vw, 56px);
  height: clamp(44px, 12vw, 56px);

  /* Remove tap highlight */
  -webkit-tap-highlight-color: transparent;
}
```

### Touch Feedback

```css
.your-button {
  transition: transform 0.2s ease;
}

/* Use :active instead of :hover for mobile */
.your-button:active {
  transform: scale(0.95);
}
```

---

## Dark Mode Support

### Adding dark mode to new components

```css
.your-component {
  background: var(--color-background);
  color: var(--color-text);
}

/* Specific dark mode overrides if needed */
@media (prefers-color-scheme: dark) {
  .your-component {
    background: rgba(255, 255, 255, 0.05);
    border-color: rgba(255, 255, 255, 0.1);
  }
}
```

---

## Fluid Sizing with clamp()

### Syntax

```css
/* clamp(min, preferred, max) */
font-size: clamp(14px, 4vw, 18px);
```

### Common Patterns

```css
/* Font sizes */
font-size: clamp(11px, 2.5vw, 13px); /* Small text */
font-size: clamp(14px, 3.5vw, 16px); /* Body text */
font-size: clamp(20px, 5vw, 24px); /* Headings */

/* Spacing */
gap: clamp(8px, 2.5vw, 12px);
padding: clamp(12px, 3.5vw, 16px);
margin: clamp(16px, 4vw, 24px);

/* Dimensions */
width: clamp(44px, 12vw, 56px);
border-radius: clamp(8px, 2vw, 12px);
```

---

## Responsive Breakpoints Quick Reference

```css
/* Small phones (iPhone SE) */
@media (max-width: 374px) {
  /* Compact layout */
}

/* Standard phones */
@media (min-width: 375px) and (max-width: 479px) {
  /* Most devices */
}

/* Large phones (Pro Max) */
@media (min-width: 480px) and (max-width: 599px) {
  /* Larger phones */
}

/* Foldables & small tablets */
@media (min-width: 600px) and (max-width: 767px) {
  /* Unfolded devices */
}

/* Tablets */
@media (min-width: 768px) {
  /* iPad and larger */
}

/* Landscape mode */
@media (orientation: landscape) {
  /* Any device in landscape */
}

/* Keyboard open (low height) */
@media (max-height: 600px) {
  /* Adjust spacing when keyboard is open */
}
```

---

## Animation Best Practices

### Use GPU-accelerated properties

```css
.your-animated-element {
  /* ✅ Good: GPU-accelerated */
  transform: translateY(20px);
  opacity: 0;

  /* ❌ Avoid: CPU-intensive */
  top: 20px;
  margin-top: 20px;
}
```

### Smooth easing

```css
.your-element {
  /* Material Design easing */
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

  /* Or use shorthand */
  transition: transform 0.3s ease-out;
}
```

### Respect user preferences

```css
@media (prefers-reduced-motion: reduce) {
  * {
    animation-duration: 0.01ms !important;
    transition-duration: 0.01ms !important;
  }
}
```

---

## Common Patterns

### Glassmorphism Effect

```css
.glass-element {
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
}
```

### Gradient Backgrounds

```css
.gradient-element {
  background: linear-gradient(
    135deg,
    var(--color-primary) 0%,
    var(--color-primary-dark) 100%
  );
}
```

### Shadow Hierarchy

```css
.element {
  /* Small shadow */
  box-shadow: var(--shadow-sm); /* 0 2px 8px rgba(0,0,0,0.1) */

  /* Medium shadow */
  box-shadow: var(--shadow-md); /* 0 4px 16px rgba(0,0,0,0.12) */

  /* Large shadow */
  box-shadow: var(--shadow-lg); /* 0 8px 32px rgba(0,0,0,0.15) */
}
```

### Ripple Effect

```css
.button-with-ripple {
  position: relative;
  overflow: hidden;
}

.button-with-ripple::before {
  content: "";
  position: absolute;
  inset: 0;
  background: rgba(255, 255, 255, 0.2);
  transform: scale(0);
  transition: transform 0.4s ease;
}

.button-with-ripple:active::before {
  transform: scale(1);
}
```

---

## Debugging Tips

### Check if safe-area is working

```css
/* Temporary visual debug */
.register-mobile-wrapper {
  background: red; /* Should not be visible at edges */
}
```

### Check if dark mode is working

```css
/* Add to body temporarily */
body {
  outline: 2px solid red; /* Red in light mode */
}

@media (prefers-color-scheme: dark) {
  body {
    outline: 2px solid green; /* Green in dark mode */
  }
}
```

### Check if clamp() is working

```css
/* Replace clamp with fixed values temporarily */
font-size: 16px; /* Instead of clamp(14px, 4vw, 18px) */
```

---

## Performance Tips

### Reduce Paint/Layout Operations

```css
/* ✅ Good: Only affects composite layer */
.optimized {
  transform: translate3d(0, 0, 0);
  will-change: transform;
}

/* ❌ Avoid: Triggers layout recalculation */
.not-optimized {
  top: 10px;
  left: 20px;
  width: 100px;
}
```

### Efficient Selectors

```css
/* ✅ Good: Specific class */
.register-form-group {
}

/* ❌ Avoid: Deeply nested */
.register-mobile-wrapper .container .form .group input {
}
```

### Minimize Reflows

```css
/* ✅ Good: Batch changes */
.element {
  transform: translateY(10px) scale(0.95);
}

/* ❌ Avoid: Multiple property changes */
.element:active {
  margin-top: 10px;
  width: 95%;
}
```

---

## Common Mistakes to Avoid

### ❌ DON'T: Hardcode pixel values

```css
.bad {
  font-size: 14px;
  padding: 12px;
  margin: 16px;
}
```

### ✅ DO: Use CSS variables and clamp()

```css
.good {
  font-size: var(--font-sm);
  padding: var(--spacing-md);
  margin: clamp(12px, 4vw, 18px);
}
```

---

### ❌ DON'T: Use :hover on mobile

```css
.bad:hover {
  background: blue;
}
```

### ✅ DO: Use :active for mobile

```css
.good:active {
  background: var(--color-primary-light);
  transform: scale(0.97);
}
```

---

### ❌ DON'T: Forget iOS zoom prevention

```css
.bad-input {
  font-size: 14px; /* Will cause zoom on iOS */
}
```

### ✅ DO: Use minimum 16px font-size

```css
.good-input {
  font-size: max(16px, clamp(15px, 4vw, 17px));
}
```

---

### ❌ DON'T: Forget safe-area for fixed elements

```css
.bad-fixed {
  position: fixed;
  top: 0;
  left: 0;
}
```

### ✅ DO: Add safe-area calculations

```css
.good-fixed {
  position: fixed;
  top: env(safe-area-inset-top, 0);
  left: env(safe-area-inset-left, 0);
}
```

---

## Testing Checklist

Before deploying, test on:

1. **Devices**

   - [ ] iPhone SE (smallest modern iPhone)
   - [ ] iPhone 15 Pro Max (largest iPhone)
   - [ ] Samsung Galaxy (popular Android)
   - [ ] iPad Mini (smallest tablet)

2. **Browsers**

   - [ ] Safari iOS
   - [ ] Chrome Mobile
   - [ ] Samsung Internet

3. **Scenarios**

   - [ ] Light mode
   - [ ] Dark mode (Settings > Display > Dark mode)
   - [ ] Landscape orientation
   - [ ] Keyboard open (focus on input)
   - [ ] Safe-area (device with notch)
   - [ ] Touch targets (tap all buttons)
   - [ ] Zoom prevention (focus on inputs)

4. **Accessibility**
   - [ ] Reduced motion (iOS Settings > Accessibility > Motion > Reduce Motion)
   - [ ] Increase contrast (iOS Settings > Accessibility > Display > Increase Contrast)
   - [ ] Large text (iOS Settings > Accessibility > Display > Larger Text)

---

## Support

If you encounter issues:

1. **Check browser console** for CSS errors
2. **Verify CSS variables** are defined in `:root`
3. **Test on actual device** (not just browser DevTools)
4. **Check safe-area** with device that has a notch
5. **Test dark mode** by switching system theme

---

**Last Updated**: ${new Date().toLocaleDateString('vi-VN')}  
**Version**: 1.0.0  
**Compatible with**: iOS 12+, Android 8+
