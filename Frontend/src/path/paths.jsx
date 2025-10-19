import { PathContext } from "../context/PathContext.js";

export const PathProvider = ({ children }) => {
  const paths = {
    //trong navigate
    home: "/",
    rules: "/rules",
    stations: "/stations",
    bookings: "/bookings",
    profile: "/profile",
    //khác
    login: "/login",
    register: "/register",
    //trong profile
    myVehicle: "/profile/my-vehicle",
    myBookings: "/profile/my-bookings",
    editProfile: "/profile/edit",
    myInformation: "/profile/information",
    notifications: "/profile/notifications",
    chargeHistory: "/profile/charge-history",
  };
  return (
    <PathContext.Provider value={paths}>{children}</PathContext.Provider>
  );
};





