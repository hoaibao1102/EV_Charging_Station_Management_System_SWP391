import { isAuthenticated } from '../../utils/authUtils.js';
import React, { useEffect} from 'react';
import { useNavigate } from 'react-router-dom';
import paths from '../../path/paths.jsx';
import { toast } from 'react-toastify';

export default function Booking() {
  const navigate = useNavigate();

  useEffect(() => {
      if (!isAuthenticated()) {
        toast.warning("Bạn chưa đăng nhập. Vui lòng đăng nhập để có thể đặt chỗ!", {
          position: "top-center",
          autoClose: 3000,
        });
        navigate(paths.login);
        return;
      }
    }, [navigate, paths.login]);
  
  return (
    <>
      
    </>
  )
}