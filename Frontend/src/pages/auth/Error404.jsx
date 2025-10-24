import { useState, useEffect } from 'react';
import { Home, ArrowLeft } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useSelector } from 'react-redux';
import paths from '../../path/paths.jsx';
import { selectRole } from '../../redux/slices/authSlice.js';
import classed from '../../assets/css/Main.module.css';

export default function Error404() {
  const [mousePosition, setMousePosition] = useState({ x: 0, y: 0 });
  const navigate = useNavigate();
  const role = useSelector(selectRole); 

  const handleNav = () => {
    if (role === 'ADMIN') {
      console.log("Navigating to admin dashboard");
      navigate(paths.adminDashboard);
    } else if (role === 'STAFF') {
      navigate(paths.staffDashboard);
    } else {
      navigate(paths.home);
    }
  };

  useEffect(() => {
    const handleMouseMove = (e) => {
      setMousePosition({ x: e.clientX, y: e.clientY });
    };
    window.addEventListener('mousemove', handleMouseMove);
    return () => window.removeEventListener('mousemove', handleMouseMove);
  }, []);

  const parallaxX = (mousePosition.x - window.innerWidth / 2) / 50;
  const parallaxY = (mousePosition.y - window.innerHeight / 2) / 50;

  return (
    <div className={classed.modalOverlay}>
      <div className={classed.formContainer}>
      {/* Background animated circles */}
      <div className="absolute inset-0 overflow-hidden">
        <div 
          className="absolute w-96 h-96 rounded-full opacity-10 blur-3xl"
          style={{
            background: 'linear-gradient(135deg, #20a2bb 0%, #1a8299 100%)',
            top: '10%',
            left: '10%',
            transform: `translate(${parallaxX}px, ${parallaxY}px)`,
            transition: 'transform 0.3s ease-out'
          }}
        />
        <div 
          className="absolute w-96 h-96 rounded-full opacity-10 blur-3xl"
          style={{
            background: 'linear-gradient(135deg, #20a2bb 0%, #1a8299 100%)',
            bottom: '10%',
            right: '10%',
            transform: `translate(${-parallaxX}px, ${-parallaxY}px)`,
            transition: 'transform 0.3s ease-out'
          }}
        />
      </div>

      {/* Floating particles */}
      {[...Array(20)].map((_, i) => (
        <div
          key={i}
          className="absolute w-2 h-2 rounded-full opacity-20"
          style={{
            background: '#20a2bb',
            left: `${Math.random() * 100}%`,
            top: `${Math.random() * 100}%`,
            animation: `float ${5 + Math.random() * 5}s ease-in-out infinite`,
            animationDelay: `${Math.random() * 5}s`
          }}
        />
      ))}

      <style>{`
        @keyframes float {
          0%, 100% { transform: translateY(0px) translateX(0px); }
          25% { transform: translateY(-20px) translateX(10px); }
          50% { transform: translateY(-40px) translateX(-10px); }
          75% { transform: translateY(-20px) translateX(10px); }
        }
        
        @keyframes slideIn {
          from { opacity: 0; transform: translateY(30px); }
          to { opacity: 1; transform: translateY(0); }
        }

        @keyframes scaleIn {
          from { opacity: 0; transform: scale(0.8); }
          to { opacity: 1; transform: scale(1); }
        }

        .animate-slide-in {
          animation: slideIn 0.6s ease-out forwards;
        }

        .animate-scale-in {
          animation: scaleIn 0.8s ease-out forwards;
        }
      `}</style>

      <div className="relative z-10 text-center px-6 max-w-4xl">
        {/* Main heading with 3D effect */}
        <div className="mb-12 relative animate-scale-in">
          <h1 
            className="text-5xl sm:text-6xl md:text-7xl font-bold leading-tight select-none"
            style={{
              color: '#20a2bb',
              textShadow: '0 10px 40px rgba(32, 162, 187, 0.4)',
              transform: `perspective(1000px) rotateX(${parallaxY * 0.3}deg) rotateY(${parallaxX * 0.3}deg)`,
              transition: 'transform 0.3s ease-out'
            }}
          >
            TRANG HIỆN<br />KHÔNG KHẢ DỤNG
          </h1>
          <div 
            className="absolute inset-0 text-5xl sm:text-6xl md:text-7xl font-bold leading-tight select-none opacity-10 pointer-events-none"
            style={{
              color: '#20a2bb',
              filter: 'blur(15px)',
              transform: 'scale(1.05)'
            }}
          >
            TRANG HIỆN<br />KHÔNG KHẢ DỤNG
          </div>
        </div>

        {/* Message */}
        <div className="space-y-6 animate-slide-in" style={{ animationDelay: '0.2s', opacity: 0 }}>
          <h2 className="text-2xl sm:text-3xl md:text-4xl font-bold text-gray-800 px-4">
            Rất tiếc, chúng tôi không thể tìm thấy<br className="hidden sm:block" /> trang bạn đang tìm kiếm.
          </h2>
          <p className="text-base sm:text-lg text-gray-600 max-w-2xl mx-auto px-4">
            Bạn vui lòng quay lại trang trước hoặc trở về trang chủ để tiếp tục khám phá.
          </p>
        </div>

        {/* Action Buttons */}
        <div className="flex flex-col sm:flex-row gap-4 justify-center mt-12 animate-slide-in px-4" style={{ animationDelay: '0.4s', opacity: 0 }}>
          <button
            onClick={handleNav}
            className="group px-10 py-4 rounded-full font-semibold text-white text-lg transition-all duration-300 transform hover:scale-105 hover:shadow-2xl flex items-center justify-center gap-3"
            style={{
              background: 'linear-gradient(135deg, #20a2bb 0%, #1a8299 100%)',
              boxShadow: '0 10px 30px rgba(32, 162, 187, 0.3)'
            }}
          >
            <Home className="w-6 h-6 transition-transform group-hover:rotate-12" />
            Về trang chủ
          </button>

          <button
            onClick={() => navigate(-1)}
            className="group px-10 py-4 rounded-full font-semibold text-lg transition-all duration-300 transform hover:scale-105 hover:shadow-xl flex items-center justify-center gap-3"
            style={{
              color: '#20a2bb',
              border: '2px solid #20a2bb',
              background: 'white'
            }}
          >
            <ArrowLeft className="w-6 h-6 transition-transform group-hover:-translate-x-1" />
            Quay lại
          </button>
        </div>

        {/* Decorative line */}
        <div className="mt-16 flex items-center justify-center gap-4 animate-slide-in" style={{ animationDelay: '0.6s', opacity: 0 }}>
          <div className="h-px w-20 bg-gradient-to-r from-transparent via-gray-300 to-transparent"></div>
          <div className="w-2 h-2 rounded-full" style={{ background: '#20a2bb' }}></div>
          <div className="h-px w-20 bg-gradient-to-r from-transparent via-gray-300 to-transparent"></div>
        </div>
      </div>
    </div>
    </div>
    
  );
}