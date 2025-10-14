import { Routes, Route, Link } from "react-router-dom";
import Login from "./pages/auth/Login.jsx";
import Register from "./pages/auth/Register.jsx";
import Home from "./pages/home/Home.jsx";
import Header from "./pages/header/Header.jsx";
import Footer from "./pages/footer/Footer.jsx";
import './App.css';

function App() {
  return (
    <div className="app">
      <Header />
      {/* Routes */}
      <div className="content">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
        </Routes>
      </div>
      <Footer />
    </div>
  );
}

export default App;