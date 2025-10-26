export default function ModelVehicle({model}) {
  return (
    <div className="vehicle-card">
        <img src={model.imageUrl} alt={model.model} className="vehicle-image" />
        <h3>{model.model}</h3>
        <p>Năm sản xuất: {model.year}</p>
        <p>Loại kết nối: {model.connectorTypeDisplayName}</p>
        <p>Công suất tối đa: {model.connectorDefaultMaxPowerKW} kW</p>
    </div>
  )
}