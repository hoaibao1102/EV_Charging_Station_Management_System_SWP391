import Form from "react-bootstrap/Form";
import { transferStaffApi } from "../../api/admin.js";
import "./AddStaffForm.css";
import Button from "react-bootstrap/Button";

export default function SelectStationForm({
  onClose,
  onAddSuccess,
  staff,
  staffsStationData,
  stations,
}) {
  if (!staff) return null;

  const currentStationEntry = staffsStationData.find(
    (s) =>
      (staff.staffId && s.staffId === staff.staffId) ||
      (staff.userId && s.userId === staff.userId)
  );

  const currentStationId = currentStationEntry?.stationId;

  const currentStationName = stations.find(
    (station) => station.stationId === currentStationId
  )?.stationName;

  const handleTransferStaff = async (e) => {
    e.preventDefault();
    const formData = new FormData(e.target);
    const selectedStation = formData.get("station");

    if (!selectedStation) {
      alert("Vui lòng chọn một trạm sạc.");
      return;
    }

    const staffIdToSend =
      staff.staffId ||
      staffsStationData.find((s) => s.userId === staff.userId)?.staffId;

    if (!staffIdToSend) {
      alert("Không xác định được ID nhân viên để chuyển công tác.");
      return;
    }

    console.log("Staff ID to send:", staffIdToSend);
    console.log("Selected Station ID:", selectedStation);

    const response = await transferStaffApi(staffIdToSend, selectedStation);
    if (response.success) {
      alert("Chuyển công tác thành công");
      handleClose();
    } else {
      alert("Chuyển công tác thất bại");
    }
  };

  const handleClose = () => {
    onClose();
    onAddSuccess();
  };

  return (
    <>
      <Form onSubmit={handleTransferStaff} className="add-staff-form">
        <h2>
          Chuyển công tác nhân viên {staff.name}
          {currentStationName &&
            ` (hiện công tác ở trạm ${currentStationName})`}
        </h2>

        <Form.Group className="mb-3" controlId="station">
          <Form.Select aria-label="select station" name="station" required>
            <option value="">Chọn trạm sạc mới</option>
            {stations
              .filter(
                (station) =>
                  station.stationId != currentStationId &&
                  station.status !== "INACTIVE"
              )
              .map((station) => (
                <option key={station.stationId} value={station.stationId}>
                  {station.stationName}
                </option>
              ))}
          </Form.Select>
          <Form.Text className="text-muted">
            Thay đổi trạm làm việc sẽ áp dụng cho nhân viên này ngay lập tức.
          </Form.Text>
        </Form.Group>

        <div className="form-button-group mt-3">
          <Button variant="primary" type="submit" className="me-2">
            XÁC NHẬN CHUYỂN CÔNG TÁC
          </Button>
          <Button
            variant="primary"
            type="button"
            className="me-2"
            onClick={handleClose}
          >
            Trở về
          </Button>
        </div>
      </Form>
    </>
  );
}
