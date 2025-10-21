import Card from 'react-bootstrap/Card';
import classCss from '../assets/css/Main.module.css'

export default function VehicleCard({ vehicle , onDelete}) {   
  return (
    <Card key={vehicle.vehicleId} style={{ width: '18rem' }} >
      <Card.Img variant="top" src={vehicle.imageUrl ? vehicle.imageUrl : 'default-image-url.jpg'} />
      <Card.Body>
        <Card.Title>{vehicle.modelName}</Card.Title>
        <Card.Text>
           Biển số: {vehicle.licensePlate}
          <br />
           Hãng xe: {vehicle.brand}
          <br />
           Năm sản xuất: {vehicle?.year}
          <br />
           Loại cổng sạc: {vehicle.connectorTypeName}
        </Card.Text>
        <button className={classCss.button} onClick={() => onDelete(vehicle)}>Xóa xe khỏi danh sách</button>
      </Card.Body>
    </Card>
  )
}