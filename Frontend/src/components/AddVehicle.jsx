import {
  getAllVehicleBrandsApi,
  getModelsByBrandApi,
} from "../api/driverApi.js";
import { useEffect, useState } from "react";
import { addVehicleApi } from "../api/driverApi.js";
import { toast } from "react-toastify";

export default function AddVehicle() {
  const [step, setStep] = useState(1);
  const [brands, setBrands] = useState([]);
  const [models, setModels] = useState([]);
  const [brand, setBrand] = useState("");
  const [vehicle, setVehicle] = useState({
    modelId: "",
    licensePlate: "",
  });

  const [model, setModel] = useState({
    modelId: "",
    brand: "",
    model: "",
    year: "",
    connectorTypeId: "",
    connectorTypeCode: "",
    connectorTypeDisplayName: "",
    connectorDefaultMaxPowerKW: "",
  });

  const handleAddVehicle = async () => {
    if (!vehicle.licensePlate || !vehicle.modelId) {
      alert("Vui lòng nhập đầy đủ thông tin xe!");
      return;
    } else {
      try {
        const response = await addVehicleApi(vehicle);
        if (response.success) {
          toast.success("Thêm xe thành công!");
          setStep(1);
        } else {
          toast.error(`Thêm xe thất bại!`);
        }
      } catch (error) {
        console.error("Failed to add vehicle:", error);
      }
    }
  };
  useEffect(() => {
    const fetchBrands = async () => {
      const response = await getAllVehicleBrandsApi();
      if (response.success) {
        setBrands(response.data);
      } else {
        console.error("Failed to fetch vehicle brands:", response.message);
      }
    };

    fetchBrands();
  }, []);

  useEffect(() => {
    if (!brand) return;
    const fetchModelsByBrand = async (brand) => {
      const response = await getModelsByBrandApi(brand);
      if (response.success) {
        setModels(response.data);
      } else {
        console.error("Failed to fetch vehicle models:", response.message);
      }
    };

    fetchModelsByBrand();
  }, [brand]);

  return (
    <>
      {/* //STEP 1: CHỌN BRAND XE CÓ TRONG HỆ THỐNG và model xe tương ứng */}
      {step === 1 && (
        <div>
          <h2>Chọn thương hiệu xe</h2>
          <select value={brand} onChange={(e) => setBrand(e.target.value)}>
            <option value="">Chọn thương hiệu</option>
            {brands.map((brand) => (
              <option key={brand} value={brand}>
                {brand}
              </option>
            ))}
          </select>
          {models.length > 0 && (
            <div>
              <h2>Chọn mẫu xe</h2>
              <select value={model} onChange={(e) => setModel(e.target.value)}>
                <option value="">Chọn mẫu xe</option>
                {models.map((model) => (
                  <option key={model.id} value={model.id}>
                    {model.name}
                    onChange=
                    {(e) => setVehicle({ ...vehicle, modelId: e.target.value })}
                  </option>
                ))}
              </select>
            </div>
          )}
        </div>
      )}
      {/* //STEP 2: NHẬP THÔNG TIN XE */}
      {step === 2 && (
        <div>
          <h2>Nhập thông tin xe</h2>
          <label>
            Biển số xe:
            <input
              type="text"
              value={vehicle.licensePlate}
              onChange={(e) =>
                setVehicle({ ...vehicle, licensePlate: e.target.value })
              }
            />
          </label>
          <button onClick={handleAddVehicle}>Thêm xe</button>
        </div>
      )}
    </>
  );
}
