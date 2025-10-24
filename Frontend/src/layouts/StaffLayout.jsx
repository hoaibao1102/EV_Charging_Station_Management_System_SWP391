import StaffNavigate from "../components/navigate/StaffNavigate"
import { Outlet } from "react-router-dom"
export default function StaffLayout() {
  return (
    <>
      <StaffNavigate />
      <main>
        <Outlet />
      </main>
    </>
  )
}