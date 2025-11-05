import accidentImg from '../../assets/icon/admin/accident.png';
import classed from '../../assets/css/Main.module.css';
export default function AccidentDetail({ accident, handleClose }) {
  return (
    <div className="add-staff-form">
        <img src={accidentImg} alt="Accident" className="staff-icon" />
        <h1 style={{color:'#20b2aa'}}>Chi tiết báo cáo tai nạn {accident?.title}</h1>
        <h3><strong>Mô tả chi tiết</strong></h3>
        <p>{accident?.description}</p>
        <button className={classed.button} onClick={handleClose}>Đóng</button>

    </div>
  )
}