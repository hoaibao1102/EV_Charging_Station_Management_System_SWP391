// src/pages/inNavigate/Booking.jsx
import React, { useEffect, useState, useMemo } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { toast } from "react-toastify";
import paths from "../../path/paths.jsx";
import { isAuthenticated } from "../../utils/authUtils.js";
import { stationAPI } from "../../api/stationApi.js";

// ===== Utility: chuẩn hóa 1 record slot từ API =====
function normalizeSlotRecord(record, pointId, templateBase, templateMap) {
  // API trả về: {templateid, slotid, status, date, pointid}
  // Cần map slotid sang StartTime/EndTime (mỗi slot = 1 giờ)
  // Hỗ trợ nhiều kiểu tên trường: slotId, slotid, SlotID, slot_id
  const rawSlotId =
    record?.slotid ?? record?.slotId ?? record?.SlotID ?? record?.slot_id;

  // Template ID (nếu cần dùng làm phần id duy nhất)
  const templateId =
    record?.templateid ??
    record?.templateId ??
    record?.TemplateID ??
    record?.template_id;

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
    if (template && (template.startTime || template.start)) {
      const s = template.startTime ?? template.start;
      const e = template.endTime ?? template.end;
      const sStr =
        typeof s === "string" && s.length >= 16 ? s.slice(11, 16) : null;
      const eStr =
        typeof e === "string" && e.length >= 16 ? e.slice(11, 16) : null;
      if (sStr && eStr) {
        timeRange = { start: sStr, end: eStr };
      }
    }
  } catch (err) {
    console.warn("⚠️ Error parsing template times", err);
  }

  return {
    id: `${templateId || 1}-${slotNumber}`,
    PointID: record?.pointid ?? record?.pointId ?? record?.PointID ?? pointId,
    SlotID: slotNumber,
    StartTime: timeRange.start,
    EndTime: timeRange.end,
    Status: record?.status ?? record?.Status ?? "available",
    Date: record?.date ?? record?.Date,
    Price: record?.price ?? record?.Price ?? "50,000",
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
    const vehicleId =
      bookingData?.vehicle?.vehicleId ??
      bookingData?.vehicle?.vehicleID ??
      bookingData?.vehicle?.id;
    if (!vehicleId) {
      toast.error("Không tìm thấy vehicleId để tạo booking", {
        position: "top-center",
      });
      return;
    }

    // Prefer raw slot DB id if available (slotId / slotid / SlotID / slot_id). Fallback to normalized id or SlotID
    const slotIds = selectedSlots
      .map((s) => {
        const r = s.raw ?? {};
        return (
          r?.slotId ??
          r?.slotid ??
          r?.SlotID ??
          r?.slot_id ??
          // If template-based id used as fallback, try numeric part of normalized id
          (typeof s.id === "string" && Number(s.id.split("-")[0])) ??
          s.SlotID
        );
      })
      .map((v) => (v == null ? null : Number(v)))
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
      bookingTime: new Date().toISOString(),
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
          const msg = res?.message ?? res;
          const text = typeof msg === "string" ? msg : JSON.stringify(msg);
          toast.error(text || "Đặt chỗ thất bại. Vui lòng thử lại.", {
            position: "top-center",
          });
          return;
        }

        // Success
        toast.success("Đặt chỗ thành công!", { position: "top-center" });
        const bookingObj = res.data ?? res;
        const bookingId =
          bookingObj?.bookingId ??
          bookingObj?.bookingID ??
          bookingObj?.id ??
          bookingObj?.booking_id;

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

  // ===== Lấy danh sách slot theo PointID và chuẩn hóa =====
  const fetchAvailableSlots = async () => {
    if (!pointId) {
      console.log("⚠️ Không có pointId để lấy slot");
      return;
    }

    try {
      setLoading(true);
      console.log("🔍 Đang lấy slots cho PointID:", pointId);

      const response = await stationAPI.getAvaila(pointId);
      console.log("📦 Raw API Response:", response);
      console.log("📦 Response.data:", response?.data);

      // Axios: dữ liệu ở response.data
      const rows = Array.isArray(response?.data) ? response.data : [];
      console.log("📋 Rows array:", rows);
      console.log("📋 First row example:", rows[0]);

      // Lọc các row cho point hiện tại (hỗ trợ nhiều biến thể tên trường point id)
      const matchedRows = rows.filter((r) => {
        const rowPointId = r?.pointid ?? r?.pointId ?? r?.PointID ?? r?.pointID;
        const match = String(rowPointId) === String(pointId);
        console.log(
          `🔍 Filter: PointID ${rowPointId} === ${pointId}? ${match}`
        );
        return match;
      });

      // Lấy danh sách templateId duy nhất từ matchedRows
      const templateIds = Array.from(
        new Set(
          matchedRows
            .map(
              (r) =>
                r?.templateid ??
                r?.templateId ??
                r?.TemplateID ??
                r?.template_id
            )
            .filter((v) => v != null)
            .map((v) => String(v))
        )
      );

      // Fetch templates in parallel and build a map templateId -> templateData
      const templateMap = {};
      if (templateIds.length > 0) {
        try {
          const promises = templateIds.map((tid) =>
            stationAPI.getTemplate(tid).then((res) => ({ tid, res }))
          );
          const results = await Promise.all(promises);
          results.forEach(({ tid, res }) => {
            if (res && res.data) {
              templateMap[String(tid)] = res.data;
            }
          });
          console.log("🔧 templateMap:", templateMap);
        } catch (err) {
          console.warn("⚠️ Error fetching templates:", err);
        }
      }

      // Nếu backend dùng templateId liên tiếp (ví dụ templateId 97..120 cho 24 slot),
      // ta có thể dùng minTemplateId làm templateBase để tính slotNumber = templateId - base + 1
      const numericTemplateIds = matchedRows
        .map(
          (r) =>
            r?.templateid ?? r?.templateId ?? r?.TemplateID ?? r?.template_id
        )
        .map((v) => (v == null ? NaN : Number(v)))
        .filter((v) => Number.isFinite(v));
      const templateBase =
        numericTemplateIds.length > 0
          ? Math.min(...numericTemplateIds)
          : undefined;
      console.log("🔢 Detected templateBase:", templateBase);

      const normalized = matchedRows.map((r) => {
        const result = normalizeSlotRecord(
          r,
          pointId,
          templateBase,
          templateMap
        );
        console.log("🔄 Normalized record:", result);
        return result;
      });

      setAvailableSlots(normalized);
      console.log("✅ Final Available Slots (normalized):", normalized);
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
                    style={{
                      padding: "15px",
                      border: isSelected
                        ? "3px solid #00BFA6"
                        : "2px solid #e0e0e0",
                      borderRadius: "8px",
                      cursor: isDisabled ? "not-allowed" : "pointer",
                      transition: "all 0.3s",
                      background: isSelected
                        ? "#e6f9f5"
                        : isDisabled
                        ? "#f5f5f5"
                        : "white",
                      opacity: isDisabled ? 0.5 : 1,
                      position: "relative",
                    }}
                    onClick={() => !isDisabled && handleToggleSlot(slot)}
                  >
                    {/* Checkbox icon */}
                    <div
                      style={{
                        position: "absolute",
                        top: "10px",
                        right: "10px",
                        width: "24px",
                        height: "24px",
                        border: isSelected
                          ? "2px solid #00BFA6"
                          : "2px solid #ccc",
                        borderRadius: "4px",
                        background: isSelected ? "#00BFA6" : "white",
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        fontSize: "16px",
                        color: "white",
                      }}
                    >
                      {isSelected && "✓"}
                    </div>

                    {/* Status badge (available / busy / reserved) */}
                    <div
                      style={{
                        position: "absolute",
                        top: "10px",
                        left: "10px",
                        padding: "4px 8px",
                        borderRadius: "12px",
                        background:
                          isAvailable && !isSelected ? "#10b981" : "#9ca3af",
                        color: "white",
                        fontSize: "12px",
                        fontWeight: "700",
                      }}
                    >
                      {isAvailable && !isSelected
                        ? "Còn trống"
                        : String(slot.Status)}
                    </div>

                    {/* Indicator cho slot có thể chọn tiếp theo (moved right to avoid overlap) */}
                    {!isSelected &&
                      canSelect &&
                      selectedSlots.length > 0 &&
                      selectedSlots.length < MAX_SLOTS && (
                        <div
                          style={{
                            position: "absolute",
                            top: "10px",
                            left: "44px",
                            background: "#4CAF50",
                            color: "white",
                            borderRadius: "50%",
                            width: "20px",
                            height: "20px",
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "center",
                            fontSize: "12px",
                            fontWeight: "bold",
                          }}
                          title="Có thể chọn"
                        >
                          ➜
                        </div>
                      )}

                    <p style={{ marginTop: "5px", marginBottom: "8px" }}>
                      <strong>
                        ⏰ {slot.StartTime} - {slot.EndTime}
                      </strong>
                    </p>
                    <p style={{ marginBottom: "8px", fontSize: "14px" }}>
                      <strong>Trạng thái:</strong>{" "}
                      <span
                        style={{
                          color:
                            String(slot.Status).toLowerCase() === "available"
                              ? "#4CAF50"
                              : "#666",
                        }}
                      >
                        {String(slot.Status).toLowerCase() === "available"
                          ? "Còn trống"
                          : slot.Status}
                      </span>
                    </p>
                    <p
                      style={{
                        margin: 0,
                        fontSize: "14px",
                        fontWeight: "600",
                        color: "#00BFA6",
                      }}
                    >
                      💰 {slot.Price ?? "N/A"} VNĐ
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
