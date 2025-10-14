# 🎨 EV Charging Station - Reusable Components Guide

## 📋 Tổng quan
File `Main.module.css` chứa tất cả các style có thể tái sử dụng cho toàn bộ ứng dụng EV Charging Station. Các component được thiết kế theo hệ thống design system nhất quán với màu sắc chủ đạo là teal (#2dd4bf).

## 🎯 Cách sử dụng

### 1. Import CSS Module
```jsx
import styles from '../pages/Main.module.css';
```

### 2. Sử dụng các Input Components

#### Basic InputField
```jsx
import InputField from '../components/InputField';

<InputField
  label="Email"
  type="email"
  name="email"
  value={email}
  onChange={handleChange}
  placeholder="your-email@example.com"
  icon="📧"
  required
/>
```

#### InputField với Error/Success States
```jsx
<InputField
  label="Password"
  type="password"
  error={hasError}
  errorMessage="Password is required"
  success={isValid}
  successMessage="Password is strong"
/>
```

### 3. Sử dụng Button Components

#### Basic Button
```jsx
import Button from '../components/Button';

<Button onClick={handleClick}>
  Click Me
</Button>
```

#### Button Variants
```jsx
<Button variant="primary">Primary</Button>
<Button variant="secondary">Secondary</Button>
<Button variant="outline">Outline</Button>
<Button variant="danger">Danger</Button>
<Button variant="success">Success</Button>
```

#### Button Sizes
```jsx
<Button size="small">Small</Button>
<Button size="medium">Medium</Button>
<Button size="large">Large</Button>
<Button fullWidth>Full Width</Button>
```

#### Loading Button
```jsx
<Button loading={isLoading} disabled>
  Submit
</Button>
```

### 4. Sử dụng Card Components

```jsx
import Card from '../components/Card';

<Card 
  title="Card Title"
  footerContent={
    <Button>Action</Button>
  }
>
  <p>Card content goes here</p>
</Card>
```

### 5. Layout Classes

```jsx
// Container
<div className={styles.container}>
  Content with max-width and auto margins
</div>

// Flex layouts
<div className={styles.flexCenter}>Centered content</div>
<div className={styles.flexBetween}>Space between content</div>
<div className={styles.flexColumn}>Column layout</div>

// Grid layouts
<div className={`${styles.grid} ${styles.gridTwoColumns}`}>
  <div>Item 1</div>
  <div>Item 2</div>
</div>
```

### 6. Form Classes

```jsx
<div className={styles.formGroup}>
  <label className={styles.formLabel}>Label</label>
  <input className={styles.input} />
  <div className={styles.formError}>Error message</div>
</div>
```

### 7. Utility Classes

```jsx
<div className={styles.textCenter}>Centered text</div>
<div className={styles.marginBottom}>With margin bottom</div>
<div className={styles.paddingAll}>With padding</div>
<div className={styles.hidden}>Hidden element</div>
```

## 🎨 Color Variables

Các màu sắc được định nghĩa trong CSS variables:

```css
--primary-color: #2dd4bf    /* Teal chính */
--primary-dark: #0891b2     /* Teal đậm */
--secondary-color: #64748b   /* Xám */
--success-color: #10b981     /* Xanh lá */
--error-color: #ef4444       /* Đỏ */
--warning-color: #f59e0b     /* Vàng */
```

## 📱 Responsive Design

Tất cả component đều được thiết kế responsive:
- Desktop: Auto-width với min-width
- Mobile (≤768px): Full width với padding phù hợp

## 🚀 Best Practices

1. **Consistency**: Luôn sử dụng các component có sẵn thay vì tạo style mới
2. **Reusability**: Tận dụng CSS variables cho màu sắc và kích thước
3. **Accessibility**: Các component đã bao gồm aria-labels và proper form structure
4. **Performance**: CSS Module giúp tránh xung đột class và tối ưu bundle size

## 📁 File Structure

```
src/
├── components/
│   ├── InputField.jsx     # Reusable input component
│   ├── Button.jsx         # Reusable button component
│   └── Card.jsx          # Reusable card component
├── pages/
│   ├── Main.module.css   # Main CSS module với tất cả styles
│   └── demo/
│       └── ComponentsDemo.jsx  # Demo page để test components
```

## 🔧 Customization

Để tùy chỉnh theme, chỉ cần thay đổi CSS variables trong `:root`:

```css
:root {
  --primary-color: #your-color;
  --border-radius: 20px;
  /* ... other variables */
}
```

## 💡 Examples

Xem file `ComponentsDemo.jsx` để có ví dụ đầy đủ về cách sử dụng tất cả components.

---

Bây giờ bạn có thể sử dụng các component này trong tất cả các trang khác của ứng dụng! 🎉