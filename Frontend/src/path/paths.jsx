export const paths = {
  //chung
  login: "/login",
  register: "/register",
  verify: "/verify-otp",
  error: "/error",
  //driver
  home: "/",
  rules: "/rules",
  stations: "/stations",
  booking: "/bookings",
  profile: "/profile",
  myVehicle: "/profile/my-vehicle",
  myBookings: "/profile/my-bookings",
  editProfile: "/profile/edit",
  information: "/profile/information",
  notifications: "/profile/notifications",
  chargeHistory: "/profile/charge-history",
  stationDetail: "/stations/:id", 


  //admin
  adminDashboard: "/admin",
  userManagement: "/admin/manage-users",
  stationManagement: "/admin/manage-stations",
  modelManagement: "/admin/manage-models",
  chargerManagement: "/admin/manage-chargers",
  businessStatistics: "/admin/business-statistics",
  accidentReports: "/admin/accident-reports",
  chargingPriceConfiguration: "/admin/charging-price-configuration",
  chargingPointManagement: "/admin/manage-charging-points",

  //staff
  staffDashboard: "/staff",
  manageSessionCharging: "/staff/manage-session-charging",
  manageTransaction: "/staff/manage-transaction",
  reportAccidents: "/staff/report-accidents",
};

export default paths;
