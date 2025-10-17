import React from "react";
import home from "../../assets/img/home/home.jpg";
import home_lap from "../../assets/img/home/home_lab.jpg";
import xe1 from "../../assets/img/home/xe1.jpg";
import xe2 from "../../assets/img/home/xe2.jpg";
import xe3 from "../../assets/img/home/xe3.jpg"; 
import "./Home.css";

const Home = () => {
  const userName = localStorage.getItem("userName") ? localStorage.getItem("userName") : "bạn";
  return (
    <div className="home-page">
        <img className="hero-img mobile-only img" src={home} alt="Trạm sạc xe điện hiện đại" />
        <img className="hero-img desktop-only img" src={home_lap} alt="Trạm sạc xe điện hiện đại" />
        
          <div className="hero-text">
            <h1 className="hero-title">2025</h1>
            <p className="hero-subtitle">EV CHARGE</p>
          </div>

      {/* Welcome Card */}
      
      <div className="welcome-card">
        <h2 className="welcome-title">Chào mừng {userName} đến với EV Charge</h2>
      

      {/* About Section with Car Gallery */}
      
        
        
        {/* Car Gallery */}
        <div className="car-gallery">
          <div className="car-item">
            <img src={xe1} alt="Xe điện 1" />
          </div>
          <div className="car-item">
            <img src={xe2} alt="Xe điện 2" />
          </div>
          <div className="car-item">
            <img src={xe3} alt="Xe điện 3" />
          </div>
        </div>
        {/* About Text */}
        <h2 className="about-title">VỀ CHÚNG TÔI</h2>
        <div className="about-section">
          
        <p className="about-text">
          Chúng tôi tin rằng tương lai của giao thông là điện hóa - một tương lai sạch hơn, thông minh hơn và bền 
          vững hơn cho các cá nhân người. Những sự biến tâm nhận độ thành hiện thực việc sạc xe điện phải trở nên 
          đơn giản, đáng tin cậy và dễ dàng tiếp cận. Đó là lý do chúng tôi ra đời.
        </p>
      </div>
      </div>
    </div>
  );
};

export default Home;
