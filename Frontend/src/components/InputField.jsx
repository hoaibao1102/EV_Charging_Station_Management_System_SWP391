import React from 'react';
import styles from '../pages/Main.module.css';

const InputField = ({ 
  type = "text", 
  placeholder, 
  value, 
  onChange, 
  name, 
  icon,
  required = false 
}) => {
  return (
    <div className={styles.inputContainer}>
      <div className={styles.inputWrapper}>
        {icon && <div className={styles.inputIcon}>{icon}</div>}
        <input
          className={styles.input}
          type={type}
          placeholder={placeholder}
          value={value}
          onChange={onChange}
          name={name}
          required={required}
        />
      </div>
    </div>
  );
};

export default InputField;