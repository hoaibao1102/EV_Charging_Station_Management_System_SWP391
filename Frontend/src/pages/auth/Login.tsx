
import "../../assets/css/login.css";
import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
// import { jwtDecode } from "jwt-decode";
import { ToastContainer, toast } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";
import axios from "axios";

// Cấu hình axios để luôn gửi credentials (cookies) nếu backend cần
axios.defaults.withCredentials = true;
const Login = () => {
	const [form, setForm] = useState({
		phone: "",
		password: "",
	});
	const [loading, setLoading] = useState(false);
	const navigate = useNavigate();

	const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
		setForm({ ...form, [e.target.name]: e.target.value });
	};

	const handleSubmit = async (e: React.FormEvent) => {
		e.preventDefault();
		const trimmedPhone = form.phone.trim();
		const trimmedPassword = form.password.trim();

		if (!trimmedPhone || !trimmedPassword) {
			toast.error("Vui lòng nhập đầy đủ số điện thoại và mật khẩu!", {
				position: "top-center",
				autoClose: 2500,
				theme: "colored",
			});
			return;
		}
		const phoneRegex = /^0[0-9]{9}$/;
		if (!phoneRegex.test(trimmedPhone)) {
			toast.error("Số điện thoại không hợp lệ!", {
				position: "top-center",
				autoClose: 2500,
				theme: "colored",
			});
			return;
		}
		if (trimmedPassword.length < 5) {
			toast.error("Mật khẩu phải có ít nhất 5 ký tự!", {
				position: "top-center",
				autoClose: 2500,
				theme: "colored",
			});
			return;
		}

		setLoading(true);
		try {
			const response = await axios.post(
				"http://localhost:8080/api/users/login",
				{
					phoneNumber: trimmedPhone,
					password: trimmedPassword,
				},
				{
					// Nếu backend yêu cầu credentials/cookies
					withCredentials: true,
					headers: {
						"Content-Type": "application/json",
					},
				}
			);
			if (response.data && response.data.message && response.data.message.toLowerCase().includes("thành công")) {
				toast.success("Đăng nhập thành công!", {
					position: "top-center",
					autoClose: 2000,
					theme: "colored",
				});
				// Lưu trạng thái đăng nhập và thông tin người dùng
				localStorage.setItem("isLoggedIn", "true");
				localStorage.setItem("userPhone", trimmedPhone);
				localStorage.setItem("userId", response.data.data.userId);
				localStorage.setItem("userName", response.data.data.firstName || "");
				setTimeout(() => {
					navigate("/");
				}, 2000);
			} else {
				toast.error(response.data.message || "Đăng nhập thất bại!", {
					position: "top-center",
					autoClose: 2500,
					theme: "colored",
				});
			}
		} catch (error: any) {
			// Hiển thị chi tiết lỗi trả về từ server (nếu có)
			if (error.response) {
				const status = error.response.status;
				const msg = error.response.data?.message || error.message;
				toast.error(`Lỗi server (${status}): ${msg}`, {
					position: "top-center",
					autoClose: 3500,
					theme: "colored",
				});
			} else {
				toast.error("Lỗi kết nối đến server!", {
					position: "top-center",
					autoClose: 2500,
					theme: "colored",
				});
			}
		} finally {
			setLoading(false);
		}
	};

	return (
		<div className="login-page-wrapper">
			<ToastContainer position="top-center" autoClose={2500} theme="colored" />
			<div className="login-container">
				<div className="left-section">
					<h1>Hệ thống quản lý trạm sạc xe điện</h1>
					<p>Giải pháp quản lý và theo dõi trạm sạc thông minh</p>
					<div className="illustration"></div>
				</div>
				<div className="right-section">
					<div className="right-content">
						<div className="form-header">
							<h2>Đăng nhập</h2>
							<p>Chào mừng bạn trở lại!</p>
						</div>
						<form onSubmit={handleSubmit}>
							<div className="form-group">
								<label htmlFor="phone">Số điện thoại</label>
								<input
									type="tel"
									id="phone"
									name="phone"
									placeholder="Nhập số điện thoại"
									value={form.phone}
									onChange={handleChange}
								/>
							</div>
							<div className="form-group">
								<label htmlFor="password">Mật khẩu</label>
								<input
									type="password"
									id="password"
									name="password"
									placeholder="Nhập mật khẩu"
									value={form.password}
									onChange={handleChange}
								/>
							</div>
							<div className="forgot-password">
								<a
									href="#"
									onClick={(e) => {
										e.preventDefault();
										navigate("/forgot-password");
									}}
								>
									Quên mật khẩu?
								</a>
							</div>
							<button type="submit" className="login-btn" disabled={loading}>
								{loading ? "Đang đăng nhập..." : "Đăng nhập"}
							</button>
							<div className="register-link">
												<span
													style={{ cursor: "pointer", color: "#007bff" }}
													onClick={() => navigate("/register")}
												>
													Chưa có tài khoản?
												</span>
							</div>
						</form>
					</div>
				</div>
			</div>
		</div>
	);
};

export default Login;
