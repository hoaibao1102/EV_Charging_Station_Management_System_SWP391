import Button from 'react-bootstrap/Button';
import Col from 'react-bootstrap/Col';
import Form from 'react-bootstrap/Form';
import Row from 'react-bootstrap/Row';
import staff from '../assets/icon/admin/staff.png';


export default function AddStaffForm({onClose}) {
  const handleSubmit = (event) => {
    event.preventDefault();
    onClose();
    const form = event.target;
    const formData = {
      name: form.formBasicName.value,
      phoneNumber: form.formBasicPhone.value,
      birthday: form.formBasicBirthday.value,
      address: form.formBasicAddress.value,
      email: form.formBasicEmail.value,
      passwordHash: form.formBasicPassword.value,
      gender: form.gender.value,
      dateOfBirth: form.formBasicBirthday.value,
    };
    const data = {user :formData, stationId : form.stationID.value};
    console.log("Form Data Submitted: ", data);


    onClose();
  };

  return (
    <Form onSubmit={handleSubmit}>
      <img src={staff} alt="Add Staff" style={{ width: '100px', height: '100px', marginBottom: '20px' }} />
      <Form.Group className="mb-3" controlId="formBasicName">
        <Form.Label>Tên nhân viên</Form.Label>
        <Form.Control type="text" placeholder="Nhập tên đầy đủ" />
      </Form.Group>
      <Form.Group className="mb-3" controlId="formBasicPhone">
        <Form.Label>Số điện thoại</Form.Label>
        <Form.Control type="tel" placeholder="Nhập số điện thoại" />
      </Form.Group>
      <Form.Group className="mb-3" controlId="formBasicBirthday">
        <Form.Label>Ngày sinh</Form.Label>
        <Form.Control type="date" placeholder="Nhập ngày sinh" />
      </Form.Group>
      <Form.Group className="mb-3" controlId="formBasicAddress">
        <Form.Label>Địa chỉ</Form.Label>
        <Form.Control type="text" placeholder="Nhập địa chỉ" />
      </Form.Group>
      <Form.Group className="mb-3" controlId="formBasicEmail">
        <Form.Label>Địa chỉ email</Form.Label>
        <Form.Control type="email" placeholder="Nhập email" />
        <Form.Text className="text-muted">
          Chúng tôi sẽ không bao giờ chia sẻ email của bạn với bất kỳ ai khác.
        </Form.Text>
      </Form.Group>

       <Row className="mb-3">
        <Form.Group as={Col} controlId="gender">
          <Form.Label>Giới tính</Form.Label>
          <Form.Select >
            <option value="" disabled selected>Chọn ...</option>
            <option value="M">Nam</option>
            <option value="F">Nữ</option>
          </Form.Select>
        </Form.Group>

        <Form.Group as={Col} controlId="stationID">
          <Form.Label>Trạm sạc</Form.Label>
          <Form.Select defaultValue="Chọn ...">
            <option value="" disabled selected>Chọn ...</option>
            <option value="station1">Trạm sạc 1</option>
            <option value="station2">Trạm sạc 2</option>
            <option value="station3">Trạm sạc 3</option>

          </Form.Select>
        </Form.Group>

        
      </Row>

      <Form.Group className="mb-3" controlId="formBasicPassword">
        <Form.Label>Mật khẩu</Form.Label>
        <Form.Control type="password" placeholder="Nhập mật khẩu" />
      </Form.Group>
      <Form.Group className="mb-3" controlId="formBasicPasswordConfirm">
        <Form.Label>Nhập lại mật khẩu</Form.Label>
        <Form.Control type="password" placeholder="Nhập lại mật khẩu" />
      </Form.Group>
      <Form.Group className="mb-3" controlId="formBasicCheckbox">
        <Form.Check type="checkbox" label="Check me out" />
      </Form.Group>
      <Button variant="primary" type="submit">
        Submit
      </Button>
    </Form>
  )
}