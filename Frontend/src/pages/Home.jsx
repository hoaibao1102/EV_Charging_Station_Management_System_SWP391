import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

const Home = () => {
  const navigate = useNavigate();
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [phone, setPhone] = useState("");
  const [userName, setUserName] = useState("");

  useEffect(() => {
    const loggedIn = localStorage.getItem("isLoggedIn");
    const userPhone = localStorage.getItem("userPhone") || "";
    const name = localStorage.getItem("userName") || "";
    setIsLoggedIn(loggedIn === "true");
    setPhone(userPhone);
    setUserName(name);
  }, []);

  return (
    <div style={{ minHeight: "100vh", display: "flex", flexDirection: "column", justifyContent: "center", alignItems: "center", background: "#f5f5f5" }}>
      <h1>Chào mừng đến với hệ thống quản lý trạm sạc xe điện</h1>
      {isLoggedIn ? (
        <>
          <h2 style={{ marginTop: "32px", color: "#1976d2" }}>Xin chào {userName ? userName : "bạn"}!</h2>
          <h3>Số điện thoại: {phone}</h3>
          <button
            style={{
              padding: "10px 28px",
              fontSize: "16px",
              borderRadius: "6px",
              background: "#d32f2f",
              color: "#fff",
              border: "none",
              cursor: "pointer",
              marginTop: "24px",
            }}
            onClick={() => {
              localStorage.removeItem("isLoggedIn");
              localStorage.removeItem("userPhone");
              localStorage.removeItem("userName");
              setIsLoggedIn(false);
              setPhone("");
              setUserName("");
            }}
          >
            Đăng xuất
          </button>
        </>
      ) : (
        <button
          style={{
            padding: "12px 32px",
            fontSize: "18px",
            borderRadius: "6px",
            background: "#1976d2",
            color: "#fff",
            border: "none",
            cursor: "pointer",
            marginTop: "32px",
          }}
          onClick={() => navigate("/login")}
        >
          Đăng nhập
        </button>
      )}
    </div>
  );
};

export default Home;
