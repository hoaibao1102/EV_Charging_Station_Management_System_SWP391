// src/pages/inNavigate/Booking.jsx
import React, { useEffect, useState, useMemo } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { toast } from "react-toastify";
import paths from "../../path/paths.jsx";
import { isAuthenticated } from "../../utils/authUtils.js";
import { stationAPI } from "../../api/stationApi.js";
import "./Booking.css";

// ===== Utility: chuẩn hóa 1 record slot từ API =====
function normalizeSlotRecord(record, pointId, templateBase, templateMap) {
  // API trả về: {slotId, templateId, status, date, pointId}
  const rawSlotId = record?.slotId;

  // Template ID
  const templateId = record?.templateId;

  // Lấy template object từ map (nếu có)
  const template =
    templateMap && templateId != null
      ? templateMap[String(templateId)]
      : undefined;

  // Xác định slotNumber (1..24)
  let slotNumber = undefined;
  if (template && Number.isFinite(Number(template.slotIndex))) {
    slotNumber = Number(template.slotIndex);
  }

  const rawSlotNum = Number(rawSlotId);
  if (slotNumber == null && rawSlotNum && rawSlotNum >= 1 && rawSlotNum <= 24) {
    slotNumber = rawSlotNum;
  }

  if (
    (slotNumber == null || !Number.isFinite(slotNumber)) &&
    templateId != null &&
    Number.isFinite(templateBase)
  ) {
    slotNumber = Number(templateId) - Number(templateBase) + 1;
  }

  if (!slotNumber || !Number.isFinite(slotNumber)) slotNumber = 1;

  // Hàm định dạng giờ từ slot index
  const getTimeRange = (slotIdx) => {
    if (!slotIdx) return { start: "N/A", end: "N/A" };
    const startHour = slotIdx - 1;
    const endHour = slotIdx;
    const formatHour = (h) => `${(h % 24).toString().padStart(2, "0")}:00`;
    return { start: formatHour(startHour), end: formatHour(endHour) };
  };

  // Mặc định lấy range theo slotNumber
  let timeRange = getTimeRange(slotNumber);

  // Nếu template có startTime/endTime thì ưu tiên dùng chúng (ISO string -> 'HH:MM')
  try {
    if (template && template.startTime && template.endTime) {
      const sStr =
        typeof template.startTime === "string" &&
        template.startTime.length >= 16
          ? template.startTime.slice(11, 16)
          : null;
      const eStr =
        typeof template.endTime === "string" && template.endTime.length >= 16
          ? template.endTime.slice(11, 16)
          : null;
      if (sStr && eStr) {
        timeRange = { start: sStr, end: eStr };
      }
    }
  } catch (err) {
    console.warn("⚠️ Error parsing template times", err);
  }

  return {
    id: `${templateId || 1}-${slotNumber}`,
    PointID: record?.pointId || pointId,
    SlotID: slotNumber,
    StartTime: timeRange.start,
    EndTime: timeRange.end,
    Status: record?.status || "available",
    Date: record?.date,
    raw: record,
  };
}

export default function Booking() {
  const navigate = useNavigate();
  const location = useLocation();
  const bookingData = location.state;

  const pointId = bookingData?.chargingPoint?.pointId; // trụ sạc đã chọn

  const [availableSlots, setAvailableSlots] = useState([]);
  const [loading, setLoading] = useState(false);
  const [_submitting, setSubmitting] = useState(false);
  const [selectedSlots, setSelectedSlots] = useState([]); // Danh sách slot đã chọn

  const MAX_SLOTS = 3; // Tối đa 3 slot

  // Kiểm tra xem slot mới có liền kề với các slot đã chọn không
  const isSlotAdjacent = (newSlotId, selectedSlots) => {
    if (selectedSlots.length === 0) return true; // Slot đầu tiên luôn hợp lệ

    const selectedSlotIds = selectedSlots
      .map((s) => s.SlotID)
      .sort((a, b) => a - b);
    const newSlot = newSlotId;

    // Kiểm tra xem slot mới có nằm liền kề với dãy đã chọn không
    const min = selectedSlotIds[0];
    const max = selectedSlotIds[selectedSlotIds.length - 1];

    // Slot mới phải là min-1 hoặc max+1
    return newSlot === min - 1 || newSlot === max + 1;
  };

  // Kiểm tra xem danh sách slot có liên tiếp không
  const areSlotsConsecutive = (slots) => {
    if (slots.length <= 1) return true;

    const sortedIds = slots.map((s) => s.SlotID).sort((a, b) => a - b);

    for (let i = 1; i < sortedIds.length; i++) {
      if (sortedIds[i] !== sortedIds[i - 1] + 1) {
        return false;
      }
    }
    return true;
  };

  // Hàm xử lý chọn/bỏ chọn slot
  const handleToggleSlot = (slot) => {
    setSelectedSlots((prev) => {
      const isSelected = prev.some((s) => s.id === slot.id);

      if (isSelected) {
        // Bỏ chọn slot
        const newSelection = prev.filter((s) => s.id !== slot.id);

        // Kiểm tra xem sau khi bỏ chọn, các slot còn lại có còn liên tiếp không
        if (!areSlotsConsecutive(newSelection)) {
          toast.warning("Không thể bỏ chọn slot này vì sẽ tạo khoảng trống!", {
            position: "top-center",
          });
          return prev;
        }

        return newSelection;
      } else {
        // Chọn slot mới
        if (prev.length >= MAX_SLOTS) {
          toast.warning(`Bạn chỉ có thể chọn tối đa ${MAX_SLOTS} khung giờ!`, {
            position: "top-center",
          });
          return prev;
        }

        // Kiểm tra slot mới có liền kề không
        if (!isSlotAdjacent(slot.SlotID, prev)) {
          toast.warning("Bạn chỉ có thể chọn các khung giờ liên tiếp!", {
            position: "top-center",
            autoClose: 2000,
          });
          return prev;
        }

        return [...prev, slot];
      }
    });
  };

  // Kiểm tra slot có được chọn không
  const isSlotSelected = (slotId) => {
    return selectedSlots.some((s) => s.id === slotId);
  };

  // Hàm xác nhận đặt chỗ
  const handleConfirmBooking = () => {
    if (selectedSlots.length === 0) {
      toast.warning("Vui lòng chọn ít nhất 1 khung giờ!", {
        position: "top-center",
      });
      return;
    }

    // Build payload for booking API
    const vehicleId = bookingData?.vehicle?.vehicleId;
    if (!vehicleId) {
      toast.error("Không tìm thấy vehicleId để tạo booking", {
        position: "top-center",
      });
      return;
    }

    // Get slotId from raw record
    const slotIds = selectedSlots
      .map((s) => s.raw?.slotId || s.SlotID)
      .map((v) => Number(v))
      .filter((v) => Number.isFinite(v));

    if (slotIds.length === 0) {
      toast.error("Không có slotId hợp lệ để gửi lên server", {
        position: "top-center",
      });
      return;
    }

    const payload = {
      vehicleId: Number(vehicleId),
      slotIds: slotIds,
    };

    console.log("📅 Booking payload:", payload);

    // Call API
    (async () => {
      try {
        setSubmitting(true);
        const res = await stationAPI.createBooking(payload);
        console.log("✅ Booking response:", res);

        if (!res || res.success === false) {
          console.error("❌ createBooking failed:", res);
          const msg = res?.message || res;
          const text = typeof msg === "string" ? msg : JSON.stringify(msg);
          toast.error(text || "Đặt chỗ thất bại. Vui lòng thử lại.", {
            position: "top-center",
          });
          return;
        }

        // Success
        toast.success("Đặt chỗ thành công!", { position: "top-center" });
        const bookingObj = res.data || res;
        const bookingId = bookingObj?.bookingId;

        // ✅ Lưu maxPowerKW vào sessionStorage để ChargingSession dùng
        const maxPowerKW = bookingData?.chargingPoint?.maxPowerKW || 11.0;

        if (bookingId) {
          try {
            sessionStorage.setItem(
              `booking_${bookingId}_maxPowerKW`,
              JSON.stringify(maxPowerKW)
            );
            console.log(
              `✅ Saved maxPowerKW=${maxPowerKW} for booking #${bookingId}`
            );
          } catch (e) {
            console.warn("Failed to save maxPowerKW to sessionStorage:", e);
          }
        }

        if (bookingId) {
          // Navigate to booking detail/confirmation page and pass booking info
          navigate(`/bookings/${bookingId}`, {
            state: { bookingId, booking: bookingObj },
          });
        } else {
          // Fallback: navigate to bookings list and include booking object in state
          navigate(`/bookings`, { state: { booking: bookingObj } });
        }
      } catch (err) {
        console.error("❌ Lỗi khi tạo booking:", err);
        toast.error("Đặt chỗ thất bại. Vui lòng thử lại.", {
          position: "top-center",
        });
      } finally {
        setSubmitting(false);
      }
    })();
  };

  // ======= Style helpers =======
  const cardStyle = useMemo(
    () => ({
      background: "white",
      padding: "20px",
      borderRadius: "12px",
      marginBottom: "20px",
      boxShadow: "0 2px 8px rgba(0,0,0,0.1)",
    }),
    []
  );

  // ===== Lấy danh sách slot theo PointID và chuẩn hóa (theo logic InstantCharging) =====
  const fetchAvailableSlots = async () => {
    if (!pointId) {
      return;
    }

    try {
      setLoading(true);

      const response = await stationAPI.getAvaila(pointId);
      const rawSlots = Array.isArray(response?.data) ? response.data : [];

      // Lấy unique template IDs
      const uniqueTemplateIds = [...new Set(rawSlots.map((s) => s.templateId))];
      const templateMap = {};

      // Fetch templates song song
      await Promise.all(
        uniqueTemplateIds.map(async (tid) => {
          try {
            const res = await stationAPI.getTemplate(tid);
            if (res?.data) {
              templateMap[String(tid)] = res.data;
            }
          } catch (err) {
            console.warn(`⚠️ Failed to fetch template ${tid}:`, err);
          }
        })
      );

      // Normalize slots
      const normalized = rawSlots.map((record) =>
        normalizeSlotRecord(record, pointId, undefined, templateMap)
      );

      console.log("📊 Total normalized slots:", normalized.length);
      console.log("📊 Sample normalized slot:", normalized[0]);

      // ✅ Filter logic: Lấy slot tương lai của HÔM NAY (giống InstantCharging)
      const now = new Date();
      const year = now.getFullYear();
      const month = String(now.getMonth() + 1).padStart(2, "0");
      const day = String(now.getDate()).padStart(2, "0");
      const todayStr = `${year}-${month}-${day}`;

      const currentHour = now.getHours();

      const currentMinute = now.getMinutes(); // ✅ Mới: Lấy thêm phút

      console.log("📅 Today:", todayStr);
      console.log("⏰ Current time:", `${currentHour}:${currentMinute}`);

      // Filter slots: chỉ hiển thị slot của ngày hôm nay và chưa kết thúc
      const filteredSlots = normalized.filter((slot) => {
        // 1. Filter theo ngày: chỉ lấy slot của ngày hôm nay
        const slotDate = slot.Date;
        if (!slotDate) return false;

        // Extract YYYY-MM-DD từ slot.Date
        let slotDateStr = String(slotDate);
        if (slotDateStr.includes("T")) {
          slotDateStr = slotDateStr.split("T")[0];
        } else if (slotDateStr.includes(" ")) {
          slotDateStr = slotDateStr.split(" ")[0];
        }

        // Nếu không phải ngày hôm nay thì loại bỏ
        if (slotDateStr !== todayStr) {
          return false;
        }

        // 2. ✅ LOGIC MỚI: Filter theo giờ KẾT THÚC (EndTime)
        const slotEndTimeStr = slot.EndTime;
        if (!slotEndTimeStr || slotEndTimeStr === "N/A") {
          return true;
        }

        // Parse giờ kết thúc của slot
        const [endH, endM] = slotEndTimeStr.split(":").map(Number);

        // Hiển thị nếu slot CHƯA kết thúc (EndTime > CurrentTime)
        // Ví dụ: 19:15, Slot kết thúc lúc 20:00 -> 20 > 19 -> OK
        return (
          endH > currentHour || (endH === currentHour && endM > currentMinute)
        );
      });

      // Sắp xếp theo thời gian bắt đầu
      filteredSlots.sort((a, b) => a.StartTime.localeCompare(b.StartTime));

      console.log(
        "✅ Total valid slots (today + future + available):",
        filteredSlots.length
      );
      console.log("✅ Sample valid slot:", filteredSlots[0]);

      setAvailableSlots(filteredSlots);
    } catch (error) {
      console.error("❌ Lỗi khi lấy danh sách slot:", error);
      toast.error("Không thể lấy danh sách slot sạc!", {
        position: "top-center",
      });
    } finally {
      setLoading(false);
    }
  };

  // ===== Kiểm tra điều kiện và gọi API =====
  useEffect(() => {
    if (!isAuthenticated()) {
      toast.warning(
        "Bạn chưa đăng nhập. Vui lòng đăng nhập để có thể đặt chỗ!",
        {
          position: "top-center",
          autoClose: 3000,
        }
      );
      navigate(paths.login);
      return;
    }

    if (!bookingData || !pointId) {
      toast.error("Không có thông tin đặt chỗ. Vui lòng chọn trụ sạc!", {
        position: "top-center",
        autoClose: 3000,
      });
      navigate(-1);
      return;
    }

    // Lưu batteryCapacityKWh vào sessionStorage
    if (bookingData.vehicle?.batteryCapacityKWh != null) {
      sessionStorage.setItem(
        "batteryCapacityKWh",
        bookingData.vehicle.batteryCapacityKWh
      );
      console.log(
        `✅ Saved batteryCapacityKWh=${bookingData.vehicle.batteryCapacityKWh} to sessionStorage`
      );
    }

    fetchAvailableSlots();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [navigate, bookingData, pointId]);

  if (!bookingData) return null;

  return (
    <div style={{ padding: "20px", maxWidth: "1200px", margin: "0 auto" }}>
      <button
        onClick={() => navigate(-1)}
        style={{
          marginBottom: "20px",
          padding: "10px 20px",
          background: "#00BFA6",
          color: "white",
          border: "none",
          borderRadius: "8px",
          cursor: "pointer",
        }}
      >
        ← Quay lại
      </button>

      <h1 style={{ color: "#00BFA6", marginBottom: "30px" }}>Đặt chỗ sạc xe</h1>

      {/* Thông tin trạm */}
      <div style={cardStyle}>
        <h2 style={{ color: "#333", marginBottom: "15px" }}>
          🏢 Thông tin trạm sạc
        </h2>
        <p>
          <strong>Tên trạm:</strong> {bookingData.station?.name}
        </p>
        <p>
          <strong>Địa chỉ:</strong> {bookingData.station?.address}
        </p>
      </div>

      {/* Thông tin trụ sạc */}
      <div style={cardStyle}>
        <h2 style={{ color: "#333", marginBottom: "15px" }}>
          🔋 Thông tin trụ sạc
        </h2>
        <p>
          <strong>Số trụ:</strong> {bookingData.chargingPoint?.pointNumber}
        </p>
        <p>
          <strong>Công suất:</strong> {bookingData.chargingPoint?.maxPowerKW} kW
        </p>
        <p>
          <strong>Trạng thái:</strong> {bookingData.chargingPoint?.status}
        </p>
      </div>

      {/* Thông tin cổng sạc */}
      <div style={cardStyle}>
        <h2 style={{ color: "#333", marginBottom: "15px" }}>
          🔌 Loại cổng sạc
        </h2>
        <p>
          <strong>Tên:</strong> {bookingData.connector?.displayName}
        </p>
        <p>
          <strong>Mã:</strong> {bookingData.connector?.code}
        </p>
        <p>
          <strong>Chế độ:</strong> {bookingData.connector?.mode}
        </p>
        <p>
          <strong>Công suất:</strong> {bookingData.connector?.defaultMaxPowerKW}{" "}
          kW
        </p>
      </div>

      {/* Thông tin xe */}
      <div style={cardStyle}>
        <h2 style={{ color: "#333", marginBottom: "15px" }}>🚗 Xe của bạn</h2>
        <p>
          <strong>Tên xe:</strong> {bookingData.vehicle?.vehicleName}
        </p>
        <p>
          <strong>Hãng:</strong> {bookingData.vehicle?.brand}{" "}
          {bookingData.vehicle?.model}
        </p>
        <p>
          <strong>Biển số:</strong> {bookingData.vehicle?.licensePlate}
        </p>
        <p>
          <strong>Loại cổng sạc:</strong>{" "}
          {bookingData.vehicle?.connectorTypeName}
        </p>
      </div>

      {/* Danh sách slot có sẵn */}
      <div style={cardStyle}>
        <h2 style={{ color: "#333", marginBottom: "15px" }}>
          ⏰ Chọn khung giờ sạc (tối đa {MAX_SLOTS} khung giờ liên tiếp)
        </h2>

        <div
          style={{
            background: "#fff3e0",
            padding: "12px",
            borderRadius: "8px",
            marginBottom: "20px",
            border: "1px solid #ff9800",
          }}
        >
          <p style={{ margin: 0, fontSize: "14px", color: "#f57c00" }}>
            ℹ️ <strong>Lưu ý:</strong> Bạn chỉ có thể chọn các khung giờ liên
            tiếp nhau (không được bỏ trống giữa các khung giờ)
          </p>
        </div>

        {selectedSlots.length > 0 && (
          <div
            style={{
              background: "#e6f9f5",
              padding: "15px",
              borderRadius: "8px",
              marginBottom: "20px",
              border: "2px solid #00BFA6",
            }}
          >
            <p
              style={{
                fontWeight: "600",
                color: "#00BFA6",
                margin: "0 0 10px 0",
              }}
            >
              ✓ Đã chọn {selectedSlots.length}/{MAX_SLOTS} khung giờ
            </p>
            <div style={{ display: "flex", gap: "10px", flexWrap: "wrap" }}>
              {selectedSlots.map((slot) => (
                <span
                  key={slot.id}
                  style={{
                    background: "#00BFA6",
                    color: "white",
                    padding: "5px 12px",
                    borderRadius: "20px",
                    fontSize: "14px",
                  }}
                >
                  {slot.StartTime} - {slot.EndTime}
                </span>
              ))}
            </div>
          </div>
        )}

        {loading ? (
          <p>Đang tải danh sách khung giờ...</p>
        ) : availableSlots.length > 0 ? (
          <>
            <div
              style={{
                display: "grid",
                gridTemplateColumns: "repeat(auto-fill, minmax(250px, 1fr))",
                gap: "15px",
              }}
            >
              {availableSlots.map((slot) => {
                const isSelected = isSlotSelected(slot.id);
                const isAvailable =
                  String(slot.Status ?? "").toLowerCase() === "available";

                // Kiểm tra nếu slot đã qua giờ hiện tại
                const now = new Date(); // Lấy giờ hiện tại
                // ✅ LOGIC MỚI: Check theo EndTime thay vì StartTime để không disable slot hiện tại
                const slotEndTime = new Date(`${slot.Date}T${slot.EndTime}:00`);
                const isPast = slotEndTime <= now;

                const canSelect =
                  selectedSlots.length === 0 ||
                  isSlotAdjacent(slot.SlotID, selectedSlots);

                // Disabled if not available or other selection rules
                const isDisabled =
                  !isAvailable ||
                  (!isSelected && selectedSlots.length >= MAX_SLOTS) ||
                  (!isSelected && selectedSlots.length > 0 && !canSelect);

                return (
                  <div
                    key={slot.id || slot.SlotID}
                    className={`slot-card ${isSelected ? "selected" : ""} ${
                      isDisabled ? "disabled" : ""
                    }`}
                    onClick={() => {
                      if (!isDisabled) {
                        handleToggleSlot(slot);
                      }
                    }}
                  >
                    <div className={`checkbox ${isSelected ? "checked" : ""}`}>
                      {isSelected && "✓"}
                    </div>

                    <p className="slot-time">
                      <strong>
                        ⏰ {slot.StartTime} - {slot.EndTime}
                      </strong>
                    </p>
                    <p className="slot-status">
                      <strong>Trạng thái:</strong>{" "}
                      <span className="available">
                        {String(slot.Status).toLowerCase() === "available"
                          ? "Còn trống"
                          : slot.Status}
                      </span>
                    </p>
                  </div>
                );
              })}
            </div>

            {/* Nút xác nhận đặt chỗ */}
            {selectedSlots.length > 0 && (
              <button
                onClick={handleConfirmBooking}
                style={{
                  marginTop: "30px",
                  width: "100%",
                  padding: "15px",
                  background:
                    "linear-gradient(135deg, #00BFA6 0%, #00897B 100%)",
                  color: "white",
                  border: "none",
                  borderRadius: "10px",
                  fontSize: "18px",
                  fontWeight: "600",
                  cursor: "pointer",
                  transition: "all 0.3s",
                  boxShadow: "0 4px 12px rgba(0, 191, 166, 0.3)",
                }}
                onMouseOver={(e) => {
                  e.currentTarget.style.transform = "translateY(-2px)";
                  e.currentTarget.style.boxShadow =
                    "0 6px 16px rgba(0, 191, 166, 0.4)";
                }}
                onMouseOut={(e) => {
                  e.currentTarget.style.transform = "translateY(0)";
                  e.currentTarget.style.boxShadow =
                    "0 4px 12px rgba(0, 191, 166, 0.3)";
                }}
              >
                🎯 Xác nhận đặt {selectedSlots.length} khung giờ
              </button>
            )}
          </>
        ) : (
          <p style={{ color: "#666" }}>
            Không có khung giờ nào khả dụng cho trụ này.
          </p>
        )}
      </div>
    </div>
  );
}
