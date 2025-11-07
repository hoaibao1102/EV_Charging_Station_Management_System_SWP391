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
  bookingDetail: "/bookings/:bookingId",
  bookingQr: "/bookings/:bookingId/qr",
  profile: "/profile",
  myVehicle: "/profile/my-vehicle",
  myBookings: "/profile/my-bookings",
  editProfile: "/profile/edit",
  information: "/profile/information",
  notifications: "/profile/notifications",
  chargeHistory: "/profile/charge-history",
  stationDetail: "/stations/:id",
  chargingSession: "/chargingSession",
  payment: "/payment",

  //admin
  adminDashboard: "/admin",
  userManagement: "/admin/manage-users",
  stationManagement: "/admin/manage-stations",
  modelManagement: "/admin/manage-models",
  chargerManagement: "/admin/manage-chargers",
  accidentReports: "/admin/accident-reports",
  chargingPriceConfiguration: "/admin/charging-price-configuration",
  chargingPointManagement: "/admin/manage-charging-points",

  //staff
  staffDashboard: "/staff",
  manageSessionCharging: "/staff/manage-session-charging",
  manageSessionChargingCreate: "/staff/manage-session-charging/new",
  manageTransaction: "/staff/manage-transaction",
  reportAccidents: "/staff/report-accidents",
  incidents: "/staff/incidents",
};

export default paths;
