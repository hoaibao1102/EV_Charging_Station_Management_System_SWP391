import { PathContext } from "../context/PathContext.js";

export const PathProvider = ({ children }) => {
  const paths = {
    home: "/",
    rules: "/rules",
    stations: "/stations",
    bookings: "/bookings",
    profile: "/profile",
    login: "/login",
    register: "/register",
  };
  return (
    <PathContext.Provider value={paths}>{children}</PathContext.Provider>
  );
};





